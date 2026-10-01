package com.patricerose.smartstorage;

import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The Wireless Terminal.
 *  - Right-click a Storage Core with it to pair it.
 *  - Right-click anywhere to open your storage, as long as you are within range of the core.
 */
public class WirelessTerminalItem extends Item {
    public WirelessTerminalItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!(level.getBlockEntity(context.getClickedPos()) instanceof StorageCoreBlockEntity)) {
            return InteractionResult.PASS; // not a core: fall through to normal right-click (opens the terminal)
        }
        if (!level.isClientSide()) {
            context.getItemInHand().set(SmartStorage.LINKED_CORE.get(),
                    GlobalPos.of(level.dimension(), context.getClickedPos().immutable()));
            LinkerItem.tell(context.getPlayer(), Component.translatable("message.smartstorage.wireless_linked"));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        ItemStack stack = player.getItemInHand(hand);
        GlobalPos core = stack.get(SmartStorage.LINKED_CORE.get());

        if (core == null) {
            LinkerItem.tell(player, Component.translatable("message.smartstorage.wireless_not_linked"));
            return InteractionResult.FAIL;
        }
        if (!core.dimension().equals(level.dimension())) {
            LinkerItem.tell(player, Component.translatable("message.smartstorage.wrong_dimension"));
            return InteractionResult.FAIL;
        }
        double maxDistSq = (double) SmartStorage.WIRELESS_RANGE * SmartStorage.WIRELESS_RANGE;
        if (player.distanceToSqr(Vec3.atCenterOf(core.pos())) > maxDistSq) {
            LinkerItem.tell(player, Component.translatable("message.smartstorage.out_of_range", SmartStorage.WIRELESS_RANGE));
            return InteractionResult.FAIL;
        }
        if (!(level.getBlockEntity(core.pos()) instanceof StorageCoreBlockEntity)) {
            LinkerItem.tell(player, Component.translatable("message.smartstorage.core_missing"));
            return InteractionResult.FAIL;
        }
        if (player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel serverLevel) {
            TerminalMenu.open(serverPlayer, serverLevel, core.pos(),
                    p -> p.level() == serverLevel && p.distanceToSqr(Vec3.atCenterOf(core.pos())) <= maxDistSq);
        }
        return InteractionResult.SUCCESS;
    }
}
