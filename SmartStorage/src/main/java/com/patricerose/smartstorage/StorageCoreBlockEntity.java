package com.patricerose.smartstorage;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Remembers which chests are linked to this core. */
public class StorageCoreBlockEntity extends BlockEntity {
    private final List<BlockPos> linked = new ArrayList<>();

    /** Lets hoppers and pipes push items into the core; they get sorted into the network. */
    private final LazyOptional<IItemHandler> input = LazyOptional.of(() -> new CoreInput(this));

    public StorageCoreBlockEntity(BlockPos pos, BlockState state) {
        super(SmartStorage.STORAGE_CORE_BE.get(), pos, state);
    }

    public int linkedCount() {
        return linked.size();
    }

    /** Links the block if it isn't linked, unlinks it if it is. Returns true if it is now linked. */
    public boolean toggleLink(BlockPos pos) {
        BlockPos p = pos.immutable();
        boolean nowLinked;
        if (linked.remove(p)) {
            nowLinked = false;
        } else {
            linked.add(p);
            nowLinked = true;
        }
        setChanged();
        return nowLinked;
    }

    public List<IItemHandler> getHandlers() {
        return StorageNetwork.handlers(getLevel(), linked);
    }

    private long lastSparkle = -100;

    public ItemStack insert(ItemStack stack, boolean simulate) {
        if (getLevel() == null || getLevel().isClientSide()) return stack;
        ItemStack leftover = StorageNetwork.insert(getHandlers(), stack, simulate);
        if (!simulate && leftover.getCount() < stack.getCount()) sparkle();
        return leftover;
    }

    /** Magic glyphs swirl up from the hearth when it files something away (at most twice a second). */
    private void sparkle() {
        if (!(getLevel() instanceof ServerLevel serverLevel)) return;
        long now = serverLevel.getGameTime();
        if (now - lastSparkle < 10) return;
        lastSparkle = now;
        BlockPos p = getBlockPos();
        serverLevel.sendParticles(ParticleTypes.ENCHANT, p.getX() + 0.5, p.getY() + 1.1, p.getZ() + 0.5,
                12, 0.3, 0.2, 0.3, 0.6);
        serverLevel.playSound(null, p, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.4F, 1.4F);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("Linked", BlockPos.CODEC.listOf(), List.copyOf(linked));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        linked.clear();
        input.read("Linked", BlockPos.CODEC.listOf()).ifPresent(linked::addAll);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return input.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        input.invalidate();
    }

    /** A one-slot "mailbox": anything put in is sorted straight into the linked chests. */
    private record CoreInput(StorageCoreBlockEntity core) implements IItemHandler {
        @Override public int getSlots() { return 1; }
        @Override public @NotNull ItemStack getStackInSlot(int slot) { return ItemStack.EMPTY; }
        @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return core.insert(stack, simulate);
        }
        @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) { return ItemStack.EMPTY; }
        @Override public int getSlotLimit(int slot) { return 64; }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return true; }
    }
}
