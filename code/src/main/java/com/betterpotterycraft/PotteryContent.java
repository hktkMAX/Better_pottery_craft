package com.betterpotterycraft;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.WrittenBookItem;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.util.StringRepresentable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

public final class PotteryContent {
    public static final Map<String, Item> ITEMS = new LinkedHashMap<>();
    public static final Map<String, Block> BLOCKS = new LinkedHashMap<>();
    public static final String[] COLORS = {"plain", "iron_red", "iron_brown", "copper_green", "lapis_blue", "amethyst_purple", "charcoal_black"};
    public static final String[] SHAPES = {"storage_jar", "planter", "flower_vase"};
    public static final String[] STAGES = {"unfired", "bisque", "glaze_ready", "finished"};

    public enum Shape implements StringRepresentable {
        STORAGE_JAR("storage_jar"), PLANTER("planter"), FLOWER_VASE("flower_vase");
        private final String name;
        Shape(String name) { this.name = name; }
        @Override public String getSerializedName() { return name; }
        public static Shape byName(String name) {
            for (Shape value : values()) if (value.name.equals(name)) return value;
            return STORAGE_JAR;
        }
    }

    public enum Color implements StringRepresentable {
        PLAIN("plain"), IRON_RED("iron_red"), IRON_BROWN("iron_brown"), COPPER_GREEN("copper_green"),
        LAPIS_BLUE("lapis_blue"), AMETHYST_PURPLE("amethyst_purple"), CHARCOAL_BLACK("charcoal_black");
        private final String name;
        Color(String name) { this.name = name; }
        @Override public String getSerializedName() { return name; }
        public static Color byName(String name) {
            for (Color value : values()) if (value.name.equals(name)) return value;
            return PLAIN;
        }
    }

    public static final EnumProperty<Shape> SHAPE = EnumProperty.create("shape", Shape.class);
    public static final EnumProperty<Color> COLOR = EnumProperty.create("color", Color.class);
    public static final BooleanProperty WORKING = BooleanProperty.create("working");
    public static final BooleanProperty FUELED = BooleanProperty.create("fueled");
    public static final BooleanProperty GLAZED_FIRE = BooleanProperty.create("glazed_fire");
    private static final AttachmentType<Boolean> GUIDE_GIVEN = AttachmentRegistry.createPersistent(id("guide_given"), Codec.BOOL);

    private PotteryContent() {
    }

    public static void register() {
        registerBlock("pottery_wheel", PotteryWheelBlock::new);
        registerBlock("kiln", PotteryKilnBlock::new);

        registerItem("pottery_guide");
        registerItem("pottery_clay");
        for (String color : COLORS) {
            if (!color.equals("plain")) {
                registerItem("pottery_clay_" + color);
                registerItem("glaze_" + color);
            }
        }
        registerItem("glaze_base");

        for (String stage : STAGES) {
            for (String shape : SHAPES) {
                for (String color : COLORS) {
                    registerItem(vesselId(stage, shape, color));
                }
            }
        }

        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output -> {
            ITEMS.values().forEach(output::accept);
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(output -> {
            BLOCKS.values().forEach(block -> output.accept(block.asItem()));
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (!handler.player.getAttachedOrElse(GUIDE_GIVEN, false)) {
                handler.player.getInventory().add(new ItemStack(item("pottery_guide")));
                handler.player.setAttached(GUIDE_GIVEN, true);
            }
        });
    }

    private static Item registerItem(String path) {
        Identifier id = id(path);
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
        Item.Properties properties = new Item.Properties().setId(key);
        Item registered;
        if (path.equals("pottery_guide")) {
            properties.component(DataComponents.WRITTEN_BOOK_CONTENT, guideContent());
            registered = new WrittenBookItem(properties);
        } else {
            registered = new Item(properties);
        }
        Registry.register(BuiltInRegistries.ITEM, id, registered);
        ITEMS.put(path, registered);
        return registered;
    }

    private static void registerBlock(String path, Function<BlockBehaviour.Properties, Block> factory) {
        Identifier id = id(path);
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id);
        BlockBehaviour.Properties properties = BlockBehaviour.Properties.of().strength(2.0F).setId(blockKey);
        Block block = factory.apply(properties);
        Registry.register(BuiltInRegistries.BLOCK, id, block);
        BLOCKS.put(path, block);

        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);
        Item blockItem = Registry.register(BuiltInRegistries.ITEM, id,
                new BlockItem(block, new Item.Properties().setId(itemKey)));
        ITEMS.put(path, blockItem);
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(BetterPotteryCraft.MOD_ID, path);
    }

    public static String vesselId(String stage, String shape, String color) {
        return stage + "_" + shape + "_" + color;
    }

    public static Item item(String path) {
        return ITEMS.get(path);
    }

    private static WrittenBookContent guideContent() {
        String[] pages = {"guide.page1", "guide.page2", "guide.page3", "guide.page4", "guide.page5", "guide.page6"};
        return new WrittenBookContent(Filterable.passThrough("陶艺指南"), "Better Pottery Craft", 0,
                java.util.Arrays.stream(pages).map(key -> Filterable.<Component>passThrough(Component.translatable(key))).toList(), true);
    }
}
