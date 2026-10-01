package com.patricerose.smartstorage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** The Arcane Hearth (storage core). Right-click it with an empty hand to see how many chests are linked. */
public class StorageCoreBlock extends Block implements EntityBlock {
    public StorageCoreBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StorageCoreBlockEntity(pos, state);
    }

    /** Floating enchanting glyphs drift around the hearth all the time (only visible to players nearby). */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        for (int i = 0; i < 2; i++) {
            level.addParticle(ParticleTypes.ENCHANT,
                    pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 1.6,
                    pos.getY() + 0.6 + random.nextDouble() * 1.2,
                    pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 1.6,
                    (random.nextDouble() - 0.5) * 0.5, -0.4, (random.nextDouble() - 0.5) * 0.5);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof StorageCoreBlockEntity core) {
            player.sendOverlayMessage(Component.translatable("message.smartstorage.core_info",
                    core.linkedCount(), SmartStorage.LINK_RANGE));
        }
        return InteractionResult.SUCCESS;
    }
}
