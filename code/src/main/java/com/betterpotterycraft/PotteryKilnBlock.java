package com.betterpotterycraft;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

public final class PotteryKilnBlock extends Block {
    public PotteryKilnBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(PotteryContent.SHAPE, PotteryContent.Shape.STORAGE_JAR)
                .setValue(PotteryContent.COLOR, PotteryContent.Color.PLAIN)
                .setValue(PotteryContent.WORKING, false)
                .setValue(PotteryContent.FUELED, false)
                .setValue(PotteryContent.GLAZED_FIRE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PotteryContent.SHAPE, PotteryContent.COLOR, PotteryContent.WORKING,
                PotteryContent.FUELED, PotteryContent.GLAZED_FIRE);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                           net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        if (state.getValue(PotteryContent.WORKING)) return InteractionResult.CONSUME;

        if (!state.getValue(PotteryContent.FUELED) && (stack.is(Items.COAL) || stack.is(Items.CHARCOAL))) {
            if (!level.isClientSide()) {
                level.setBlock(pos, state.setValue(PotteryContent.FUELED, true), Block.UPDATE_ALL);
                if (!player.isCreative()) stack.shrink(1);
            }
            return InteractionResult.SUCCESS;
        }

        if (!state.getValue(PotteryContent.FUELED)) return InteractionResult.PASS;
        String itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        for (String shape : PotteryContent.SHAPES) {
            for (String color : PotteryContent.COLORS) {
                String unfired = PotteryContent.vesselId("unfired", shape, color);
                String glazed = PotteryContent.vesselId("glaze_ready", shape, color);
                boolean secondFiring = itemId.equals(glazed);
                if (!itemId.equals(unfired) && !secondFiring) continue;
                if (!level.isClientSide()) {
                    BlockState working = state.setValue(PotteryContent.SHAPE, PotteryContent.Shape.byName(shape))
                            .setValue(PotteryContent.COLOR, PotteryContent.Color.byName(color))
                            .setValue(PotteryContent.WORKING, true)
                            .setValue(PotteryContent.FUELED, false)
                            .setValue(PotteryContent.GLAZED_FIRE, secondFiring);
                    level.setBlock(pos, working, Block.UPDATE_ALL);
                    level.scheduleTick(pos, this, 200);
                    if (!player.isCreative()) stack.shrink(1);
                }
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(PotteryContent.WORKING)) return;
        String stage = state.getValue(PotteryContent.GLAZED_FIRE) ? "finished" : "bisque";
        String itemId = PotteryContent.vesselId(stage, state.getValue(PotteryContent.SHAPE).getSerializedName(),
                state.getValue(PotteryContent.COLOR).getSerializedName());
        level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                new ItemStack(PotteryContent.item(itemId))));
        level.setBlock(pos, state.setValue(PotteryContent.WORKING, false).setValue(PotteryContent.GLAZED_FIRE, false), Block.UPDATE_ALL);
    }
}
