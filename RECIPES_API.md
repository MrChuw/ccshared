# `recipes` — CC: Shared Recipe API

In-computer Lua library that exposes the server's recipe data (crafting,
smelting, stonecutting, smithing, custom mod machines, brewing, fuels) and item tags.

Registered by the **CC: Shared** addon via `ComputerCraftAPI.registerAPIFactory`.
Available on every computer as a global named `recipes` (alias: `cc_recipes`).

The index is built once per server boot (`SERVER_STARTING`) and is read-only
after that. No disk I/O, no reload command needed for datapack recipes —
just restart the server.

---

## Quick start

```lua
local recipes_list = recipes.get("minecraft:crafting_table")
if recipes_list then
    for i, r in ipairs(recipes_list) do
        print(r.output.name, "x" .. r.output.count)
        for slot, item in pairs(r.grid) do
            print("  slot " .. slot .. " <- " .. tostring(item))
        end
    end
end

```

---

## Types returned

Every lookup returns either `nil` (not found) or an array of tables (since there can be multiple recipes that result in the same item). The shape depends on the recipe kind.

**Important:** Many fields like `input`, `template`, or grid values can return either a string (e.g. `"minecraft:iron_pickaxe"`) or a list of strings if the recipe accepts multiple specific items (e.g. `["minecraft:iron_pickaxe", "minecraft:iron_sword"]`) instead of a tag.

### Crafting (`crafting`, `craft`, `get`)

```lua
[
    {
        type   = "craft",
        id     = "minecraft:oak_planks",
        output = { name = "minecraft:oak_planks", count = 4, components = "{...}" },
        grid   = {
            ["1"] = "minecraft:oak_log",       -- or "#minecraft:planks" for tags
            ["4"] = "minecraft:oak_log",
        },
    }
]

```

`grid` uses keys `"1"` through `"9"` laid out like a crafting table:

```
 1 2 3
 4 5 6
 7 8 9

```

Missing slots are simply not present in `grid`.

### Cooking (`smelting`, `smelt`) — also blasting, smoking, campfire

```lua
[
    {
        type        = "smelting" | "blasting" | "smoking" | "campfire",
        id          = "minecraft:glass",
        input       = "#minecraft:smelts_to_glass",  -- or ["minecraft:iron_ore", "minecraft:deepslate_iron_ore"]
        output      = { name = "minecraft:glass", count = 1 },
        cookingtime = 200,     -- ticks
        experience  = 0.1,     -- XP awarded per item
    }
]

```

### Stonecutting

```lua
[
    {
        type   = "stonecutting",
        id     = "minecraft:brick_stairs_from_bricks_stonecutting",
        input  = "minecraft:bricks",
        output = { name = "minecraft:brick_stairs", count = 1 },
    }
]

```

### Smithing — transforms and trims

Transforms and trims are keyed by **output item** or **recipe id**.

```lua
-- transform (netherite upgrade)
[
    {
        type     = "smithing_transform",
        id       = "minecraft:netherite_axe_smithing",
        template = "minecraft:netherite_upgrade_smithing_template",
        base     = "minecraft:diamond_axe",
        addition = "minecraft:netherite_ingot",
        result   = { name = "minecraft:netherite_axe", count = 1 },
    }
]

```

### Custom Mod Machines (`custom`)

Recipes from modded machines (like *Create* crushing, *Mekanism* smelting, etc.) will be dynamically extracted using generic matching.

```lua
[
    {
        type       = "create:milling",
        id         = "create:milling/cobblestone",
        inputs     = { "minecraft:cobblestone" },
        output     = { name = "minecraft:gravel", count = 1 },
        fluids_in  = { { name = "minecraft:water", amount = 1000 } },
        fluids_out = { { name = "create:honey", amount = 250 } }
    }
]

```

### Brewing (`brewing`)

Returns a flat list (Not grouped by item):

```lua
{
    { type = "brewing", from = "water", ingredient = "minecraft:nether_wart", to = "awkward" },
    { type = "brewing", from = "awkward", ingredient = "minecraft:glowstone_dust", to = "thick" },
    -- ...
}

```

### Fuels (`fuel` / `fuels`)

Individual item:

```lua
{
    type          = "fuel",
    item          = "minecraft:coal",
    burn_ticks    = 1600,
    items_smelted = 8.0,   -- burn_ticks / 200
}

```

Grouped collections:

```lua
{
    ["smelted"] = {
        ["1.5"] = {
            ["minecraft:acacia_fence"] = {
                items_smelted = 1.5,
                type          = "fuel",
                item          = "minecraft:acacia_fence",
                burn_ticks    = 300,
            },
            -- ...
        },
        ["4"] = {
            ["minecraft:acacia_hanging_sign"] = {
                items_smelted = 4,
                type          = "fuel",
                item          = "minecraft:acacia_hanging_sign",
                burn_ticks    = 800,
            },
            -- ...
        }
    },
    ["ticks"] = {
        ["300"] = { ... },
        ["800"] = { ... }
    }
}

```

### Item tags (`itemTag`)

Returns a plain array of item ids:

```lua
{ "minecraft:oak_planks", "minecraft:spruce_planks", ... }

```

---

## API reference

### `recipes.get(id)`

Returns **any** recipe list whose output matches `id`, searching in order:
crafting → smelting → stonecutting → smithing → custom. `nil` if none.

* `id` — item id string (e.g. `"minecraft:iron_pickaxe"`)

### `recipes.crafting(id)` / `recipes.smelting(id)` / `recipes.stonecutting(id)` / `recipes.smithing(id)` / `recipes.custom(id)`

Same as `get` but restricted to one kind. Returns an array of recipes or `nil` if not found.

`smithing(id)` matches either the output item (transforms) or the recipe id
(trims). For a strict recipe-id lookup use `smithingById`.

### `recipes.smithingById(id)`

Looks up a smithing recipe list by its **recipe id**, regardless of kind. Useful
for trims, which have no distinct output item.

* `id` — recipe id string (e.g. `"minecraft:netherite_axe_smithing"`)

### `recipes.fuel(id)`

Fuel information for the given item id. `nil` if the item is not a fuel.

### `recipes.fuels([group, [value]])`

Returns fuel data grouped by burn efficiency (`"ticks"` ou `"smelted"`). Pre-indexed in memory for instant queries.

* **No arguments**: Returns the full table containing both groups: `{ ticks = { ... }, smelted = { ... } }`.
* **Group only**: Filters by group type.
* `recipes.fuels("ticks")` — returns all fuels grouped by tick count (`{ ["300"] = { ... }, ["800"] = { ... } }`).
* `recipes.fuels("smelted")` — returns all fuels grouped by items smelted (`{ ["1.5"] = { ... }, ["4"] = { ... } }`).


* **Group + Value filter**: Returns only the items mapped to that specific duration or smelt count.
* `recipes.fuels("ticks", 300)` — returns map of items that burn for 300 ticks.
* `recipes.fuels("smelted", 1.5)` — returns map of items that smelt 1.5 items.


Accepts numbers or strings for the value argument.

### `recipes.brewing()`

Returns the full list of brewing recipes (small — usually).

### `recipes.itemTag(tag)`

Returns the array of item ids belonging to `tag`.

Accepts all of these forms:

* `"minecraft:planks"`
* `"#minecraft:planks"`
* `"#planks"` (shorthand for `#minecraft:planks`)

`nil` if the tag doesn't exist.

### `recipes.list(kind)`

Returns a **sorted array of ids** for the given kind. Use it to iterate.

`kind` accepts: `"craft"` / `"crafting"`, `"smelt"` / `"smelting"`,
`"stonecut"` / `"stonecutting"`, `"smith"` / `"smithing"`, `"custom"`, `"fuel"`,
`"tag"`.

For smithing, the list contains output ids for transforms and recipe ids
for trims. Invalid kinds return an empty list.

### `recipes.search(kind, pattern)`

Filters `recipes.list(kind)` down to ids that **contain** `pattern`
(case-insensitive).

```lua
recipes.search("craft", "stick")     -- every craftable id containing "stick"
recipes.search("tag", "planks")      -- every tag containing "planks"

```

---

## Tags vs. items vs lists

The API returns **`"#tag"`** strings when the recipe itself declared a tag
(`"tag": "minecraft:planks"`), and a plain item id when the recipe declared
a specific item (`"item": "minecraft:iron_ingot"`).

If a recipe accepts multiple explicit items without using a tag (like smelting any pickaxe to an iron nugget), the API returns a **list of item ids**: `{"minecraft:iron_pickaxe", "minecraft:iron_sword", ...}`.

This means:

* `oak_door` returns `#minecraft:planks` — the recipe genuinely accepts any plank.
* `iron_nugget` smelting returns `{"minecraft:iron_pickaxe", "minecraft:iron_sword", ...}` — it accepts specific items explicitly.

To expand a tag into items, use `recipes.itemTag("#...")`:

```lua
local planks = recipes.itemTag("#minecraft:planks")
-- { "minecraft:oak_planks", "minecraft:spruce_planks", ... }

```

---

## Data Components (NBT)

Starting in 1.20.5+, item data components (like enchantments, effects, patterns) are embedded as a raw string format representation from the game's internal data in the `output.components` field.
For example, suspicious stews will output their specific component data: `{minecraft:suspicious_stew_effects=>...}`.
If an output item has no specific custom component, this field will simply be missing.

---

## Notes

* All lookups are pure reads from an in-memory index. They are cheap and
  safe to call in tight loops.
* Recipes are grouped in arrays. You must iterate `ipairs(recipes.get(id))` to read them.
* The index is immutable for the lifetime of the server. Adding mods or
  datapack recipes requires a server restart.
* `recipes` and `cc_recipes` are the same object.
