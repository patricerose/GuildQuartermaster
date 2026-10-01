package com.patricerose.smartstorage;

import net.minecraft.ChatFormatting;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Guild Quartermaster: a wireless, auto-sorting storage network with a cozy fantasy look.
 *
 *  Arcane Hearth          (storage_core)      - the brain. Chests are linked to it wirelessly.
 *  Guild Ledger           (storage_terminal)  - a desk you right-click to search and take out items.
 *  Scrying Orb            (wireless_terminal) - a handheld ledger that works up to 256 blocks from the hearth.
 *  Quartermaster's Quill  (storage_linker)    - the tool used to connect chests and ledgers to a hearth.
 */
@Mod(SmartStorage.MODID)
public final class SmartStorage {
    public static final String MODID = "smartstorage";

    /** How far (in blocks) a linked chest can be from its core. */
    public static final int LINK_RANGE = 64;
    /** How far (in blocks) the Wireless Terminal can be from its core. */
    public static final int WIRELESS_RANGE = 256;

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MODID);
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, MODID);

    // ---- Data stored on items: which core a Linker / Wireless Terminal points at ----
    public static final RegistryObject<DataComponentType<GlobalPos>> LINKED_CORE =
            DATA_COMPONENTS.register("linked_core", () -> DataComponentType.<GlobalPos>builder()
                    .persistent(GlobalPos.CODEC)
                    .networkSynchronized(GlobalPos.STREAM_CODEC)
                    .build());

    // ---- Blocks ----
    public static final RegistryObject<Block> STORAGE_CORE = BLOCKS.register("storage_core",
            () -> new StorageCoreBlock(BlockBehaviour.Properties.of()
                    .setId(BLOCKS.key("storage_core"))
                    .mapColor(MapColor.METAL)
                    .strength(3.0F, 6.0F)
                    .sound(SoundType.STONE)
                    .lightLevel(state -> 12)));

    public static final RegistryObject<Block> STORAGE_TERMINAL = BLOCKS.register("storage_terminal",
            () -> new StorageTerminalBlock(BlockBehaviour.Properties.of()
                    .setId(BLOCKS.key("storage_terminal"))
                    .mapColor(MapColor.METAL)
                    .strength(3.0F, 6.0F)
                    .sound(SoundType.WOOD)
                    .lightLevel(state -> 5)));

    // ---- Block entities (the part of a block that remembers data) ----
    public static final RegistryObject<BlockEntityType<StorageCoreBlockEntity>> STORAGE_CORE_BE =
            BLOCK_ENTITIES.register("storage_core", () -> new BlockEntityType<>(StorageCoreBlockEntity::new, null) {
                @Override
                public boolean isValid(BlockState state) {
                    return state.is(STORAGE_CORE.get());
                }
            });

    public static final RegistryObject<BlockEntityType<StorageTerminalBlockEntity>> STORAGE_TERMINAL_BE =
            BLOCK_ENTITIES.register("storage_terminal", () -> new BlockEntityType<>(StorageTerminalBlockEntity::new, null) {
                @Override
                public boolean isValid(BlockState state) {
                    return state.is(STORAGE_TERMINAL.get());
                }
            });

    // ---- The terminal screen ----
    public static final RegistryObject<MenuType<TerminalMenu>> TERMINAL_MENU =
            MENUS.register("terminal", () -> IForgeMenuType.create((id, inventory, data) -> new TerminalMenu(id, inventory)));

    // ---- Items ----
    public static final RegistryObject<Item> STORAGE_CORE_ITEM = ITEMS.register("storage_core",
            () -> new BlockItem(STORAGE_CORE.get(), new Item.Properties().setId(ITEMS.key("storage_core"))));

    public static final RegistryObject<Item> STORAGE_TERMINAL_ITEM = ITEMS.register("storage_terminal",
            () -> new BlockItem(STORAGE_TERMINAL.get(), new Item.Properties().setId(ITEMS.key("storage_terminal"))));

    public static final RegistryObject<Item> STORAGE_LINKER = ITEMS.register("storage_linker",
            () -> new LinkerItem(new Item.Properties().setId(ITEMS.key("storage_linker")).stacksTo(1)));

    public static final RegistryObject<Item> WIRELESS_TERMINAL = ITEMS.register("wireless_terminal",
            () -> new WirelessTerminalItem(new Item.Properties().setId(ITEMS.key("wireless_terminal")).stacksTo(1)));

    public SmartStorage(FMLJavaModLoadingContext context) {
        var modBusGroup = context.getModBusGroup();
        BLOCKS.register(modBusGroup);
        ITEMS.register(modBusGroup);
        BLOCK_ENTITIES.register(modBusGroup);
        MENUS.register(modBusGroup);
        DATA_COMPONENTS.register(modBusGroup);

        FMLCommonSetupEvent.getBus(modBusGroup).addListener(event -> ModNetwork.init());
        BuildCreativeModeTabContentsEvent.BUS.addListener(SmartStorage::addToCreativeTab);
        ItemTooltipEvent.BUS.addListener(SmartStorage::addTooltip);
    }

    private static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(STORAGE_CORE_ITEM);
            event.accept(STORAGE_TERMINAL_ITEM);
        }
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(STORAGE_LINKER);
            event.accept(WIRELESS_TERMINAL);
        }
    }

    private static void addTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        var lines = event.getToolTip();
        boolean isLinker = stack.getItem() instanceof LinkerItem;
        boolean isWireless = stack.getItem() instanceof WirelessTerminalItem;
        if (!isLinker && !isWireless) return;

        GlobalPos core = stack.get(LINKED_CORE.get());
        if (core == null) {
            lines.add(Component.translatable(isLinker ? "tooltip.smartstorage.linker.none" : "tooltip.smartstorage.wireless.none")
                    .withStyle(ChatFormatting.RED));
        } else {
            lines.add(Component.translatable("tooltip.smartstorage.core_at",
                    core.pos().getX(), core.pos().getY(), core.pos().getZ()).withStyle(ChatFormatting.AQUA));
        }
        if (isLinker) {
            lines.add(Component.translatable("tooltip.smartstorage.linker.help1").withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable("tooltip.smartstorage.linker.help2").withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable("tooltip.smartstorage.linker.help3").withStyle(ChatFormatting.GRAY));
        } else {
            lines.add(Component.translatable("tooltip.smartstorage.wireless.help", WIRELESS_RANGE).withStyle(ChatFormatting.GRAY));
        }
    }
}
