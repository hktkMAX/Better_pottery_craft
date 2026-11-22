package com.betterpotterycraft;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

public final class PotteryWheelBlock extends Block {
    public PotteryWheelBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(PotteryContent.SHAPE, PotteryContent.Shape.STORAGE_JAR)
                .setValue(PotteryContent.COLOR, PotteryContent.Color.PLAIN)
                .setValue(PotteryContent.WORKING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PotteryContent.SHAPE, PotteryContent.COLOR, PotteryContent.WORKING);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (state.getValue(PotteryContent.WORKING)) return InteractionResult.SUCCESS;
        PotteryContent.Shape[] shapes = PotteryContent.Shape.values();
        PotteryContent.Shape next = shapes[(state.getValue(PotteryContent.SHAPE).ordinal() + 1) % shapes.length];
        level.setBlock(pos, state.setValue(PotteryContent.SHAPE, next), Block.UPDATE_ALL);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                           net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        if (state.getValue(PotteryContent.WORKING)) return InteractionResult.CONSUME;

        for (String color : PotteryContent.COLORS) {
            String clayId = color.equals("plain") ? "pottery_clay" : "pottery_clay_" + color;
            if (PotteryContent.item(clayId) != stack.getItem()) continue;

            if (!level.isClientSide()) {
                PotteryContent.Color selectedColor = PotteryContent.Color.byName(color);
                BlockState working = state.setValue(PotteryContent.COLOR, selectedColor).setValue(PotteryContent.WORKING, true);
                level.setBlock(pos, working, Block.UPDATE_ALL);
                level.scheduleTick(pos, this, 80);
                if (!player.isCreative()) stack.shrink(1);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(PotteryContent.WORKING)) return;
        String itemId = PotteryContent.vesselId("unfired", state.getValue(PotteryContent.SHAPE).getSerializedName(),
                state.getValue(PotteryContent.COLOR).getSerializedName());
        ItemEntity output = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                new ItemStack(PotteryContent.item(itemId)));
        level.addFreshEntity(output);
        level.setBlock(pos, state.setValue(PotteryContent.WORKING, false), Block.UPDATE_ALL);
    }
}
