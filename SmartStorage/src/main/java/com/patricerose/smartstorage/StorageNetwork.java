package com.patricerose.smartstorage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;

/**
 * The sorting brain. Works on any storage block: chests, barrels, shulker boxes
 * and most modded containers.
 *
 * Where does a new item go? In this order:
 *   1. Topped up onto matching stacks that already exist ("carrots with carrots")
 *   2. Into a chest that already holds that item
 *   3. Into a chest that holds items of the same kind ("swords with swords")
 *   4. Into a completely empty chest (that chest now "belongs" to this item)
 *   5. Anywhere there is space
 */
public final class StorageNetwork {
    private StorageNetwork() {}

    /** One kind of item in storage and how many there are in total. */
    public record Entry(ItemStack prototype, long count) {}

    // ---------------------------------------------------------------- categories

    private record Category(String name, TagKey<Item> tag) {}

    private static TagKey<Item> tag(String namespace, String path) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(namespace, path));
    }

    private static Category cat(String path) {
        return new Category(path, tag("minecraft", path));
    }

    private static Category common(String path) {
        return new Category("c:" + path, tag("c", path));
    }

    /** Checked top to bottom; the first match decides an item's "kind". */
    private static final List<Category> CATEGORIES = List.of(
            cat("swords"), cat("axes"), cat("pickaxes"), cat("shovels"), cat("hoes"),
            cat("head_armor"), cat("chest_armor"), cat("leg_armor"), cat("foot_armor"),
            cat("arrows"), common("tools"),
            common("ores"), common("raw_materials"), common("ingots"), common("gems"), common("nuggets"),
            common("dusts"), common("dyes"), common("seeds"),
            cat("logs"), cat("planks"), cat("wool"), cat("wool_carpets"), cat("saplings"), cat("leaves"),
            cat("small_flowers"), cat("beds"), cat("candles"), cat("terracotta"), cat("boats"),
            cat("stairs"), cat("slabs"), cat("walls"), cat("fences"), cat("doors"), cat("trapdoors"),
            cat("stone_bricks"), cat("sand"), cat("coals"), common("crops")
    );

    /** Returns a name for what "kind" of item this is, or null if it has no special kind. */
    public static String category(ItemStack stack) {
        for (Category c : CATEGORIES) {
            if (stack.is(c.tag())) return c.name();
        }
        if (stack.has(DataComponents.FOOD)) return "food";
        if (stack.has(DataComponents.POTION_CONTENTS)) return "potions";
        if (stack.is(Items.ENCHANTED_BOOK)) return "enchanted_books";
        return null;
    }

    // ---------------------------------------------------------------- finding storage

    /** Turns linked block positions into inventories we can read and write. */
    public static List<IItemHandler> handlers(Level level, List<BlockPos> positions) {
        List<IItemHandler> result = new ArrayList<>();
        if (level == null) return result;
        for (BlockPos pos : positions) {
            BlockEntity be = level.getBlockEntity(pos);
            IItemHandler handler = handlerFor(be);
            if (handler != null) result.add(handler);
        }
        return result;
    }

    /** Can this block entity hold items? Returns its inventory, or null. */
    public static IItemHandler handlerFor(BlockEntity be) {
        if (be == null || be instanceof StorageCoreBlockEntity || be instanceof StorageTerminalBlockEntity) return null;
        // Each half of a double chest is its own Container, so nothing gets counted twice.
        if (be instanceof Container container) return new InvWrapper(container);
        return be.getCapability(ForgeCapabilities.ITEM_HANDLER, null).resolve().orElse(null);
    }

    // ---------------------------------------------------------------- putting items in

    /**
     * Sorts a stack into storage. Returns whatever did NOT fit.
     * With simulate = true nothing actually moves; it only reports what would be left over.
     */
    public static ItemStack insert(List<IItemHandler> handlers, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack remaining = stack.copy();
        Set<Long> used = new HashSet<>();

        // 1. Top up existing matching stacks
        for (int h = 0; h < handlers.size(); h++) {
            IItemHandler handler = handlers.get(h);
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack inSlot = handler.getStackInSlot(slot);
                if (!inSlot.isEmpty() && ItemStack.isSameItemSameComponents(inSlot, remaining)) {
                    used.add(key(h, slot));
                    remaining = handler.insertItem(slot, remaining, simulate);
                    if (remaining.isEmpty()) return ItemStack.EMPTY;
                }
            }
        }

        // Work out what each chest holds
        String kind = category(remaining);
        List<Integer> sameItem = new ArrayList<>();
        List<Integer> sameKind = new ArrayList<>();
        List<Integer> emptyChests = new ArrayList<>();
        List<Integer> others = new ArrayList<>();
        for (int h = 0; h < handlers.size(); h++) {
            IItemHandler handler = handlers.get(h);
            boolean hasItem = false, hasKind = false, empty = true;
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack inSlot = handler.getStackInSlot(slot);
                if (inSlot.isEmpty()) continue;
                empty = false;
                if (ItemStack.isSameItem(inSlot, remaining)) hasItem = true;
                else if (kind != null && kind.equals(category(inSlot))) hasKind = true;
            }
            if (hasItem) sameItem.add(h);
            else if (hasKind) sameKind.add(h);
            else if (empty) emptyChests.add(h);
            else others.add(h);
        }

        // 2-5. Fill empty slots, best chests first
        for (List<Integer> group : List.of(sameItem, sameKind, emptyChests, others)) {
            for (int h : group) {
                IItemHandler handler = handlers.get(h);
                for (int slot = 0; slot < handler.getSlots(); slot++) {
                    if (used.contains(key(h, slot))) continue;
                    if (!handler.getStackInSlot(slot).isEmpty()) continue;
                    used.add(key(h, slot));
                    remaining = handler.insertItem(slot, remaining, simulate);
                    if (remaining.isEmpty()) return ItemStack.EMPTY;
                }
            }
        }
        return remaining;
    }

    private static long key(int handler, int slot) {
        return ((long) handler << 32) | (slot & 0xFFFFFFFFL);
    }

    // ---------------------------------------------------------------- taking items out

    /** Takes up to {@code amount} of an item out of storage. */
    public static ItemStack extract(List<IItemHandler> handlers, ItemStack prototype, int amount) {
        ItemStack result = ItemStack.EMPTY;
        int wanted = Math.min(amount, prototype.getMaxStackSize());
        for (IItemHandler handler : handlers) {
            for (int slot = 0; slot < handler.getSlots() && wanted > 0; slot++) {
                ItemStack inSlot = handler.getStackInSlot(slot);
                if (inSlot.isEmpty() || !ItemStack.isSameItemSameComponents(inSlot, prototype)) continue;
                ItemStack got = handler.extractItem(slot, wanted, false);
                if (got.isEmpty()) continue;
                if (result.isEmpty()) result = got;
                else result.grow(got.getCount());
                wanted -= got.getCount();
            }
            if (wanted <= 0) break;
        }
        return result;
    }

    // ---------------------------------------------------------------- listing contents

    /** Adds up everything in storage, one entry per kind of item. */
    public static List<Entry> contents(List<IItemHandler> handlers) {
        Map<Integer, List<long[]>> countsByHash = new HashMap<>();
        Map<Integer, List<ItemStack>> protosByHash = new HashMap<>();
        for (IItemHandler handler : handlers) {
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack stack = handler.getStackInSlot(slot);
                if (stack.isEmpty()) continue;
                int hash = ItemStack.hashItemAndComponents(stack);
                List<ItemStack> protos = protosByHash.computeIfAbsent(hash, k -> new ArrayList<>());
                List<long[]> counts = countsByHash.computeIfAbsent(hash, k -> new ArrayList<>());
                boolean found = false;
                for (int i = 0; i < protos.size(); i++) {
                    if (ItemStack.isSameItemSameComponents(protos.get(i), stack)) {
                        counts.get(i)[0] += stack.getCount();
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    protos.add(stack.copyWithCount(1));
                    counts.add(new long[]{stack.getCount()});
                }
            }
        }
        List<Entry> entries = new ArrayList<>();
        for (var e : protosByHash.entrySet()) {
            List<long[]> counts = countsByHash.get(e.getKey());
            for (int i = 0; i < e.getValue().size(); i++) {
                entries.add(new Entry(e.getValue().get(i), counts.get(i)[0]));
            }
        }
        return entries;
    }
}
