package com.patricerose.smartstorage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * The Quartermaster's Quill (storage linker).
 *  - Right-click a Storage Core to select it.
 *  - Right-click a chest/barrel to link it to (or unlink it from) the selected core.
 *  - Right-click a Storage Terminal to connect it to the selected core.
 *  - Sneak + right-click the air to clear the selection.
 */
public class LinkerItem extends Item {
    public LinkerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        BlockPos pos = context.getClickedPos();
        BlockEntity be = level.getBlockEntity(pos);

        // Select a core
        if (be instanceof StorageCoreBlockEntity) {
            stack.set(SmartStorage.LINKED_CORE.get(), GlobalPos.of(level.dimension(), pos.immutable()));
            tell(player, Component.translatable("message.smartstorage.core_selected"));
            return InteractionResult.SUCCESS;
        }

        GlobalPos selected = stack.get(SmartStorage.LINKED_CORE.get());
        if (selected == null) {
            tell(player, Component.translatable("message.smartstorage.select_core_first"));
            return InteractionResult.FAIL;
        }
        if (!selected.dimension().equals(level.dimension())) {
            tell(player, Component.translatable("message.smartstorage.wrong_dimension"));
            return InteractionResult.FAIL;
        }
        if (!(level.getBlockEntity(selected.pos()) instanceof StorageCoreBlockEntity core)) {
            stack.remove(SmartStorage.LINKED_CORE.get());
            tell(player, Component.translatable("message.smartstorage.core_missing"));
            return InteractionResult.FAIL;
        }

        // Connect a terminal
        if (be instanceof StorageTerminalBlockEntity terminal) {
            terminal.setCorePos(selected.pos());
            tell(player, Component.translatable("message.smartstorage.terminal_linked"));
            return InteractionResult.SUCCESS;
        }

        // Link or unlink a chest
        if (StorageNetwork.handlerFor(be) != null) {
            if (!pos.closerThan(selected.pos(), SmartStorage.LINK_RANGE)) {
                tell(player, Component.translatable("message.smartstorage.too_far", SmartStorage.LINK_RANGE));
                return InteractionResult.FAIL;
            }
            boolean nowLinked = core.toggleLink(pos);
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(nowLinked ? ParticleTypes.ENCHANT : ParticleTypes.SMOKE,
                        pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, nowLinked ? 20 : 6, 0.3, 0.3, 0.3, 0.5);
                serverLevel.playSound(null, pos, nowLinked ? SoundEvents.AMETHYST_BLOCK_CHIME : SoundEvents.BOOK_PAGE_TURN,
                        SoundSource.BLOCKS, 0.8F, nowLinked ? 1.5F : 0.8F);
            }
            tell(player, Component.translatable(nowLinked ? "message.smartstorage.linked" : "message.smartstorage.unlinked",
                    core.linkedCount()));
            return InteractionResult.SUCCESS;
        }

        tell(player, Component.translatable("message.smartstorage.not_storage"));
        return InteractionResult.FAIL;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && stack.has(SmartStorage.LINKED_CORE.get())) {
            if (!level.isClientSide()) {
                stack.remove(SmartStorage.LINKED_CORE.get());
                tell(player, Component.translatable("message.smartstorage.selection_cleared"));
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    static void tell(Player player, Component message) {
        if (player != null) player.sendOverlayMessage(message);
    }
}
