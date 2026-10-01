# Adventurer's Packs ✦

Upgradable backpacks with mob-charm powers, for Minecraft Java Edition 26.3 on Forge (66.0.8 or newer).

## Backpacks

| Pack | Space | Charms it can power |
|---|---|---|
| **Leather Satchel** | 3 rows (27 slots) | 1 |
| **Explorer's Pack** | 4 rows (36 slots) | 2 |
| **Hero's Pack** | 5 rows (45 slots) | 3 |
| **Legendary Pack** | 6 rows (54 slots, a full double chest) | 5 |

- **Right-click** a pack to open it.
- **Upgrading:** use a **Smithing Table**. Everything inside the pack comes along.
- Packs can't go inside other packs.
- The pack you're holding can't be moved while it's open.

## Mob charms

Craft a charm, put it **inside** your backpack, and you get that mob's power as long as the pack is anywhere in your inventory.

| Charm | Power | Recipe (shapeless, all with 1 Gold Ingot) |
|---|---|---|
| 🕷️ **Spider Charm** | Climb walls. Sneak to hang on. | Spider Eye + 2 String |
| 🟩 **Slime Charm** | No fall damage | 3 Slimeballs |
| 🐔 **Chicken Charm** | Float gently down when falling. Sneak to drop normally. | 2 Feathers + Egg |
| 🐰 **Rabbit Charm** | Speed + higher jumps | Rabbit's Foot + Rabbit Hide |
| 🔥 **Blaze Charm** | Fire and lava can't burn you | Blaze Rod + Blaze Powder |
| 🦑 **Squid Charm** | Breathe underwater | 3 Ink Sacs |
| 🌙 **Phantom Charm** | Night vision | 2 Phantom Membranes |
| 🟪 **Enderman Charm** | Sneak + right-click your pack to open your Ender Chest from anywhere | 2 Ender Pearls + Obsidian |
| 🦊 **Fox Charm** | Dropped items within 6 blocks fly to you | 2 Sweet Berries + Glow Berries |
| 🛡️ **Golem Charm** | Take less damage (Resistance) | Iron Block + Poppy |

**How charm slots work:** a pack only powers as many charms as its tier allows. The **first** charms in the pack (top-left first) are the active ones. Hover over a pack to see which charms are on. Duplicate charms don't stack.

**Several packs:** only your **highest-tier** pack's charms count.

## Recipes

**Leather Satchel** (crafting table)
```
Leather  String   Leather
Leather  Chest    Leather
Leather  Leather  Leather
```

**Upgrades** (Smithing Table: template, then base, then addition)

| Upgrade | Template | Base | Addition |
|---|---|---|---|
| Explorer's Pack | Chest | Leather Satchel | Iron Block |
| Hero's Pack | Chest | Explorer's Pack | Diamond Block |
| Legendary Pack | Chest | Hero's Pack | Netherite Ingot |

## Building the .jar

Same as the other mods. Upload everything in this folder to a new GitHub repository, including the `.github` and `gradle` folders. Open the **Actions** tab and wait for the green checkmark. Then download **adventurepacks-mod** from Artifacts and put the .jar in your CurseForge `mods` folder.

## Changing things

- Names and descriptions are in `src/main/resources/assets/adventurepacks/lang/en_us.json`.
- Pack sizes and charm limits are in `AdventurePacks.java` (look for `pack("leather_satchel", 1, 3, 1, ...)`. The numbers are tier, rows and charms).
- Recipes are in `src/main/resources/data/adventurepacks/recipe/`.
