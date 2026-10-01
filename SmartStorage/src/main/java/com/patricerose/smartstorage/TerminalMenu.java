package com.patricerose.smartstorage;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * The terminal screen's logic. It looks like a big chest:
 *  - Rows 1-5: everything in storage (45 items per page)
 *  - Row 6: buttons (previous page, sort, info, deposit, next page)
 *  - Below: your own inventory
 *
 * Nothing in the top part is a real item; clicks are turned into "take out" or "put in" actions.
 */
public class TerminalMenu extends ChestMenu {
    public static final int VIEW_SLOTS = 45;
    public static final int TOTAL_TOP_SLOTS = 54;

    private static final int PREV = 45, SORT = 47, INFO = 49, DEPOSIT = 51, NEXT = 53;

    private final SimpleContainer view;
    private final @Nullable ServerLevel level;   // null on the client
    private final @Nullable BlockPos corePos;
    private final Predicate<Player> validity;

    private String search = "";
    private int page = 0;
    private boolean sortByName = false;
    private int ticks = 0;
    private List<StorageNetwork.Entry> shown = List.of();

    /** Client side: an empty copy that gets filled in by the server. */
    public TerminalMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(TOTAL_TOP_SLOTS), null, null, p -> true);
    }

    private TerminalMenu(int id, Inventory inventory, SimpleContainer view, @Nullable ServerLevel level,
                         @Nullable BlockPos corePos, Predicate<Player> validity) {
        super(SmartStorage.TERMINAL_MENU.get(), id, inventory, view, 6);
        this.view = view;
        this.level = level;
        this.corePos = corePos;
        this.validity = validity;
        refresh();
    }

    /** Opens the terminal for a player (server side). */
    public static void open(ServerPlayer player, ServerLevel level, BlockPos corePos, Predicate<Player> validity) {
        player.openMenu(new SimpleMenuProvider(
                (id, inventory, p) -> new TerminalMenu(id, inventory, new SimpleContainer(TOTAL_TOP_SLOTS), level, corePos.immutable(), validity),
                Component.translatable("gui.smartstorage.terminal")));
    }

    // ------------------------------------------------------------------ storage access

    private @Nullable StorageCoreBlockEntity core() {
        if (level == null || corePos == null) return null;
        return level.getBlockEntity(corePos) instanceof StorageCoreBlockEntity core ? core : null;
    }

    private List<IItemHandler> handlers() {
        StorageCoreBlockEntity core = core();
        return core == null ? List.of() : core.getHandlers();
    }

    private ItemStack insert(ItemStack stack) {
        StorageCoreBlockEntity core = core();
        return core == null ? stack : core.insert(stack, false);
    }

    /** Called by the search box (through a network packet). */
    public void setSearch(String text) {
        String cleaned = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
        if (cleaned.length() > 50) cleaned = cleaned.substring(0, 50);
        search = cleaned;
        page = 0;
        refresh();
    }

    // ------------------------------------------------------------------ building the view

    private void refresh() {
        if (level == null) return;

        List<StorageNetwork.Entry> all = new ArrayList<>(StorageNetwork.contents(handlers()));
        long totalItems = 0;
        for (var e : all) totalItems += e.count();
        int totalTypes = all.size();

        if (!search.isEmpty()) {
            all.removeIf(e -> !matches(e.prototype(), search));
        }
        Comparator<StorageNetwork.Entry> byName = Comparator.comparing(e -> e.prototype().getHoverName().getString().toLowerCase(Locale.ROOT));
        if (sortByName) {
            all.sort(byName);
        } else {
            all.sort(Comparator.comparingLong(StorageNetwork.Entry::count).reversed().thenComparing(byName));
        }

        int pages = Math.max(1, (all.size() + VIEW_SLOTS - 1) / VIEW_SLOTS);
        page = Math.max(0, Math.min(page, pages - 1));
        int from = page * VIEW_SLOTS;
        shown = all.subList(from, Math.min(all.size(), from + VIEW_SLOTS));

        for (int i = 0; i < VIEW_SLOTS; i++) {
            view.setItem(i, i < shown.size() ? displayStack(shown.get(i)) : ItemStack.EMPTY);
        }

        // Button row
        for (int i = VIEW_SLOTS; i < TOTAL_TOP_SLOTS; i++) {
            view.setItem(i, button(new ItemStack(Items.PURPLE_STAINED_GLASS_PANE), Component.literal(" ")));
        }
        view.setItem(PREV, button(new ItemStack(Items.FEATHER), Component.translatable("gui.smartstorage.prev")));
        view.setItem(NEXT, button(new ItemStack(Items.FEATHER), Component.translatable("gui.smartstorage.next")));
        view.setItem(SORT, button(new ItemStack(Items.COMPASS), Component.translatable(
                sortByName ? "gui.smartstorage.sort_name" : "gui.smartstorage.sort_count"),
                Component.translatable("gui.smartstorage.sort_help")));
        view.setItem(DEPOSIT, button(new ItemStack(Items.CAKE), Component.translatable("gui.smartstorage.deposit"),
                Component.translatable("gui.smartstorage.deposit_help")));

        List<Component> info = new ArrayList<>();
        info.add(Component.translatable("gui.smartstorage.info_types", totalTypes));
        info.add(Component.translatable("gui.smartstorage.info_items", format(totalItems)));
        StorageCoreBlockEntity core = core();
        info.add(Component.translatable("gui.smartstorage.info_chests", core == null ? 0 : core.linkedCount()));
        if (!search.isEmpty()) info.add(Component.translatable("gui.smartstorage.info_search", search));
        view.setItem(INFO, button(new ItemStack(Items.WRITABLE_BOOK),
                Component.translatable("gui.smartstorage.page", page + 1, pages), info.toArray(new Component[0])));
    }

    private static boolean matches(ItemStack stack, String search) {
        if (search.startsWith("@")) {
            String mod = BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace();
            return mod.contains(search.substring(1));
        }
        return stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(search);
    }

    private static ItemStack displayStack(StorageNetwork.Entry entry) {
        ItemStack stack = entry.prototype().copyWithCount((int) Math.min(entry.count(), entry.prototype().getMaxStackSize()));
        ItemLore lore = stack.getOrDefault(DataComponents.LORE, ItemLore.EMPTY);
        stack.set(DataComponents.LORE, lore.withLineAdded(grayLine(
                Component.translatable("gui.smartstorage.in_storage", format(entry.count())))));
        return stack;
    }

    private static ItemStack button(ItemStack stack, Component name, Component... lines) {
        stack.set(DataComponents.ITEM_NAME, name.copy().withStyle(ChatFormatting.YELLOW));
        if (lines.length > 0) {
            List<Component> styled = new ArrayList<>();
            for (Component line : lines) styled.add(grayLine(line));
            stack.set(DataComponents.LORE, new ItemLore(styled));
        }
        return stack;
    }

    private static Component grayLine(Component c) {
        return c.copy().withStyle(style -> style.withItalic(false).withColor(ChatFormatting.GRAY));
    }

    private static String format(long n) {
        return NumberFormat.getIntegerInstance(Locale.US).format(n);
    }

    // ------------------------------------------------------------------ clicks

    @Override
    public void clicked(int slotId, int button, ContainerInput input, Player player) {
        // Top part (storage view and buttons): handled by the server only
        if (slotId >= 0 && slotId < TOTAL_TOP_SLOTS) {
            if (level != null && player instanceof ServerPlayer serverPlayer) {
                clickTop(slotId, button, input, serverPlayer);
            }
            return;
        }
        // Shift-click in your own inventory: send that stack into storage
        if (input == ContainerInput.QUICK_MOVE && slotId >= TOTAL_TOP_SLOTS && slotId < slots.size()) {
            if (level != null) {
                Slot slot = slots.get(slotId);
                if (slot.hasItem()) {
                    slot.set(insert(slot.getItem().copy()));
                    refresh();
                }
            }
            return;
        }
        super.clicked(slotId, button, input, player);
    }

    private void clickTop(int slotId, int button, ContainerInput input, ServerPlayer player) {
        if (slotId < VIEW_SLOTS) {
            ItemStack carried = getCarried();
            if (!carried.isEmpty()) {
                // Holding something: put it into storage (left click = all, right click = one)
                if (input == ContainerInput.PICKUP) {
                    if (button == 0) {
                        setCarried(insert(carried.copy()));
                    } else {
                        ItemStack leftover = insert(carried.copyWithCount(1));
                        if (leftover.isEmpty()) carried.shrink(1);
                        setCarried(carried.isEmpty() ? ItemStack.EMPTY : carried);
                    }
                }
                refresh();
                return;
            }
            if (slotId >= shown.size()) return;

            StorageNetwork.Entry entry = shown.get(slotId);
            int max = entry.prototype().getMaxStackSize();
            if (input == ContainerInput.QUICK_MOVE) {
                // Shift-click: a full stack straight into your inventory
                ItemStack got = StorageNetwork.extract(handlers(), entry.prototype(), max);
                if (!got.isEmpty()) {
                    player.getInventory().add(got);
                    if (!got.isEmpty()) got = insert(got);            // inventory full: put it back
                    if (!got.isEmpty()) {
                        player.level().addFreshEntity(new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), got));
                    }
                }
            } else if (input == ContainerInput.PICKUP) {
                // Left click: a full stack. Right click: half a stack.
                int available = (int) Math.min(entry.count(), max);
                int amount = button == 0 ? available : Math.max(1, (available + 1) / 2);
                setCarried(StorageNetwork.extract(handlers(), entry.prototype(), amount));
            }
            refresh();
            return;
        }

        switch (slotId) {
            case PREV -> { page--; chime(player, SoundEvents.BOOK_PAGE_TURN, 1.0F); }
            case NEXT -> { page++; chime(player, SoundEvents.BOOK_PAGE_TURN, 1.0F); }
            case SORT -> { sortByName = !sortByName; page = 0; }
            case DEPOSIT -> {
                depositInventory(player);
                chime(player, SoundEvents.AMETHYST_BLOCK_CHIME, 1.2F);
            }
            default -> { }
        }
        refresh();
    }

    /** A little sound just for the player using the ledger. */
    private static void chime(ServerPlayer player, SoundEvent sound, float pitch) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, 0.6F, pitch);
    }

    /** Moves everything in the main inventory (not the hotbar or armor) into storage. */
    private void depositInventory(Player player) {
        Inventory inv = player.getInventory();
        for (int i = 9; i < 36; i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty()) inv.setItem(i, insert(stack.copy()));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY; // handled in clicked()
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack carried, Slot slot) {
        return slot.container != view && super.canTakeItemForPickAll(carried, slot);
    }

    @Override
    public boolean canDragTo(Slot slot) {
        return slot.container != view && super.canDragTo(slot);
    }

    @Override
    public void broadcastChanges() {
        // Refresh once a second so changes from hoppers or other players show up
        if (level != null && ++ticks >= 20) {
            ticks = 0;
            refresh();
        }
        super.broadcastChanges();
    }

    @Override
    public boolean stillValid(Player player) {
        if (level == null) return true;
        return validity.test(player) && core() != null;
    }
}
