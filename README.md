# Guild Quartermaster ✦

A cozy, magical storage guild: a wireless, auto-sorting storage mod for Minecraft Java Edition 26.3 on Forge (66.0.8 or newer).

## What's in it

| Thing | What it does |
|---|---|
| **Arcane Hearth** (block) | The heart of your storage. Chests link to it wirelessly, with no cables, from up to 64 blocks away. Enchanting glyphs drift around it, and it chimes and sparkles when it files something away. |
| **Quartermaster's Quill** (tool) | Enlists chests and ledgers into the guild. |
| **Guild Ledger** (block) | A wooden desk with an open ledger. Right-click it to see, search and take out everything in storage. |
| **Scrying Orb** (item) | Peer into the guild stores from afar: anywhere within 256 blocks of the hearth, in the same dimension. |

## Setting it up

1. Place an **Arcane Hearth**.
2. Hold the **Quartermaster's Quill** and right-click the hearth. That selects it.
3. Right-click each chest or barrel you want in the system. Click a linked one again to unlink it.
   - **Double chests:** link both halves.
4. Place a **Guild Ledger** and right-click it with the Quill to connect it.
5. For the **Scrying Orb**, right-click the hearth with it once to pair it.

## How sorting works

When an item goes into storage, it looks for a place in this order:

1. **The same item:** carrots go on top of your existing carrots.
2. **A chest that already has that item.**
3. **A chest with the same kind of item:** swords with swords, pickaxes with pickaxes, food with food, logs with logs, ores with ores, and so on.
4. **An empty chest.** That chest becomes the new home for this item.
5. **Anywhere with space**, as a last resort.

Tip: to set up "a chest for X," put one X in an empty linked chest. Everything like it will go there.

## Ways to put items in

- **Cake button** ("Hand everything to the Quartermaster") in the ledger: sorts your whole inventory except your hotbar.
- **Shift-click** an item in your inventory while the ledger is open.
- **Click an item** you're holding on the ledger's top area. Left-click puts in all of them, right-click puts in one.
- **Hoppers:** point a hopper into the Arcane Hearth and everything that flows in gets sorted. This works well with a mob farm or a crop farm.

## The Guild Ledger

- **Search box** (top right): type to filter. Type `@modname` to show only items from one mod.
- **Left-click** an item: take a full stack.
- **Right-click** an item: take half a stack.
- **Shift-click** an item: send a full stack straight to your inventory.
- Hover over an item to see how many you have in total ("In the guild stores: 1,234").
- The bottom row has **Feathers** to turn pages, a **Compass** to change the order (most first or A to Z), a **Book** with item totals and the linked chest count and the **Cake** to deposit your inventory.
- The first Escape leaves the search box and the second closes the screen.

## Renaming things

All names and messages are in `src/main/resources/assets/smartstorage/lang/en_us.json`. For example, change `"Scrying Orb"` to `"Satchel of Holding"` there and nothing else needs to change.

## Recipes

**Arcane Hearth**
```
Stone Bricks  Amethyst Shard  Stone Bricks
Ender Pearl   Chest           Ender Pearl
Stone Bricks  Gold Ingot      Stone Bricks
```

**Guild Ledger**
```
Gold Nugget  Book    Gold Nugget
Planks       Chest   Planks
Planks       Planks  Planks
```
(any kind of wooden planks)

**Quartermaster's Quill** (diagonal, bottom-left to top-right)
```
                          Feather
           Amethyst Shard
Ink Sac
```

**Scrying Orb**
```
Glass           Amethyst Shard  Glass
Amethyst Shard  Eye of Ender    Amethyst Shard
Glass           Guild Ledger    Glass
```

## Building the .jar

Same as the XP Canisters mod. Upload this folder to GitHub and open the **Actions** tab. When the build finishes, download **smartstorage-mod** from the Artifacts section. Inside is `smartstorage-26.3-1.0.0.jar`. Put it in your CurseForge profile's `mods` folder.

## Changing the numbers

In `SmartStorage.java`:
- `LINK_RANGE = 64` sets how far chests can be from the hearth.
- `WIRELESS_RANGE = 256` sets how far the Scrying Orb reaches.
