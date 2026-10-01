package com.patricerose.smartstorage;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

/** Remembers which Storage Core this terminal shows. */
public class StorageTerminalBlockEntity extends BlockEntity {
    private @Nullable BlockPos corePos;

    public StorageTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(SmartStorage.STORAGE_TERMINAL_BE.get(), pos, state);
    }

    public @Nullable BlockPos getCorePos() {
        return corePos;
    }

    public void setCorePos(BlockPos pos) {
        this.corePos = pos.immutable();
        setChanged();
    }

    public @Nullable StorageCoreBlockEntity getCore() {
        if (corePos == null || getLevel() == null) return null;
        return getLevel().getBlockEntity(corePos) instanceof StorageCoreBlockEntity core ? core : null;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (corePos != null) output.store("Core", BlockPos.CODEC, corePos);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        corePos = input.read("Core", BlockPos.CODEC).orElse(null);
    }
}
