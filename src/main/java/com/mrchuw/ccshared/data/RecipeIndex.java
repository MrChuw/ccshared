package com.mrchuw.ccshared.data;

import com.mrchuw.ccshared.CCShared;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
//? if <26.3 {
/*import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;
*///?}
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.ShapedRecipe;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class RecipeIndex {

    private static volatile Map<String, List<Map<String, Object>>> CRAFTING = Map.of();
    private static volatile Map<String, List<Map<String, Object>>> SMELTING = Map.of();
    private static volatile Map<String, List<Map<String, Object>>> STONECUTTING = Map.of();
    private static volatile Map<String, List<Map<String, Object>>> SMITHING = Map.of();
    private static volatile Map<String, List<Map<String, Object>>> CUSTOM = Map.of();
    private static volatile Map<String, Map<String, Object>> SMITHING_BY_ID = Map.of();
    private static volatile List<Map<String, Object>> BREWING = List.of();
    private static volatile Map<String, Map<String, Object>> FUELS = Map.of();
    private static volatile Map<String, Map<String, Map<String, Object>>> FUELS_BY_TICKS = Map.of();
    private static volatile Map<String, Map<String, Map<String, Object>>> FUELS_BY_SMELTED = Map.of();
    private static volatile Map<String, List<String>> ITEM_TAGS = Map.of();

    public static synchronized void rebuild(MinecraftServer server) {
        try {
            RegistryAccess regs = server.registryAccess();
            Map<String, List<String>> tags = buildTags(regs);
            Map<String, List<Map<String, Object>>> crafting = new HashMap<>();
            Map<String, List<Map<String, Object>>> smelting = new HashMap<>();
            Map<String, List<Map<String, Object>>> stonecutting = new HashMap<>();
            Map<String, List<Map<String, Object>>> smithing = new HashMap<>();
            Map<String, List<Map<String, Object>>> custom = new HashMap<>();
            Map<String, Map<String, Object>> smithingById = new HashMap<>();

            buildRecipes(regs, server.getRecipeManager(), crafting, smelting, stonecutting, smithing, custom, smithingById);
            List<Map<String, Object>> brewing = buildBrewing(server);
            Map<String, Map<String, Object>> fuels = buildFuels(server);

            Map<String, Map<String, Map<String, Object>>> byTicks = new LinkedHashMap<>();
            Map<String, Map<String, Map<String, Object>>> bySmelted = new LinkedHashMap<>();

            for (Map.Entry<String, Map<String, Object>> entry : fuels.entrySet()) {
                String itemId = entry.getKey();
                Map<String, Object> fuelData = entry.getValue();

                Object ticksObj = fuelData.get("burn_ticks");
                if (ticksObj != null) {
                    byTicks.computeIfAbsent(String.valueOf(ticksObj), k -> new LinkedHashMap<>())
                            .put(itemId, new LinkedHashMap<>(fuelData));
                }

                Object smeltedObj = fuelData.get("items_smelted");
                if (smeltedObj instanceof Number n) {
                    double d = n.doubleValue();
                    String key = (d == (long) d) ? String.valueOf((long) d) : String.valueOf(d);
                    bySmelted.computeIfAbsent(key, k -> new LinkedHashMap<>())
                            .put(itemId, new LinkedHashMap<>(fuelData));
                }
            }

            ITEM_TAGS = Map.copyOf(tags);
            CRAFTING = Map.copyOf(crafting);
            SMELTING = Map.copyOf(smelting);
            STONECUTTING = Map.copyOf(stonecutting);
            SMITHING = Map.copyOf(smithing);
            CUSTOM = Map.copyOf(custom);
            SMITHING_BY_ID = Map.copyOf(smithingById);
            BREWING = List.copyOf(brewing);
            FUELS = Map.copyOf(fuels);
            FUELS_BY_TICKS = Map.copyOf(byTicks);
            FUELS_BY_SMELTED = Map.copyOf(bySmelted);

            CCShared.LOGGER.info(
                    "[cc: Shared] Recipe index: {} craft, {} smelt, {} stonecut, {} smith, {} custom, {} brew, {} fuel, {} tags",
                    CRAFTING.size(), SMELTING.size(), STONECUTTING.size(),
                    SMITHING.size(), CUSTOM.size(), BREWING.size(), FUELS.size(), ITEM_TAGS.size());
        } catch (Throwable t) {
            CCShared.LOGGER.error("[cc: Shared] Failed to build RecipeIndex", t);
        }
    }

    public static Object fuelsByGroup(String group, Object filterValue) {
        if (group == null || group.isBlank()) {
            Map<String, Object> full = new LinkedHashMap<>();
            full.put("ticks", FUELS_BY_TICKS);
            full.put("smelted", FUELS_BY_SMELTED);
            return full;
        }

        String g = group.toLowerCase(Locale.ROOT).trim();
        Map<String, Map<String, Map<String, Object>>> targetMap = switch (g) {
            case "tick", "ticks", "burn_ticks" -> FUELS_BY_TICKS;
            case "smelted", "items_smelted", "smelt" -> FUELS_BY_SMELTED;
            default -> null;
        };

        if (targetMap == null) return Map.of();
        if (filterValue == null) return targetMap;

        String key;
        if (filterValue instanceof Number n) {
            double d = n.doubleValue();
            key = (d == (long) d) ? String.valueOf((long) d) : String.valueOf(d);
        } else {
            key = filterValue.toString().trim();
            try {
                double d = Double.parseDouble(key);
                if (d == (long) d) key = String.valueOf((long) d);
            } catch (NumberFormatException ignored) {}
        }

        Map<String, Map<String, Object>> res = targetMap.get(key);
        return res != null ? res : Map.of();
    }

    public static List<Map<String, Object>> get(String id) {
        List<Map<String, Object>> r = CRAFTING.get(id);
        if (r != null) return r;
        r = SMELTING.get(id);
        if (r != null) return r;
        r = STONECUTTING.get(id);
        if (r != null) return r;
        r = SMITHING.get(id);
        if (r != null) return r;
        return CUSTOM.get(id);
    }

    public static List<Map<String, Object>> crafting(String id) { return CRAFTING.get(id); }
    public static List<Map<String, Object>> smelting(String id) { return SMELTING.get(id); }
    public static List<Map<String, Object>> stonecutting(String id) { return STONECUTTING.get(id); }
    public static List<Map<String, Object>> smithing(String id) { return SMITHING.get(id); }
    public static List<Map<String, Object>> custom(String id) { return CUSTOM.get(id); }
    public static Map<String, Object> fuel(String id) { return FUELS.get(id); }
    public static List<Map<String, Object>> brewing() { return BREWING; }
    public static Map<String, Object> smithingById(String id) { return SMITHING_BY_ID.get(id); }

    public static List<String> itemTag(String tag) {
        if (tag == null) return null;
        if (!tag.startsWith("#")) tag = "#" + tag;
        List<String> direct = ITEM_TAGS.get(tag);
        if (direct != null) return direct;
        if (!tag.contains(":")) return ITEM_TAGS.get("#minecraft:" + tag.substring(1));
        return null;
    }

    public static List<String> list(String kind) {
        return switch (kind == null ? "" : kind.toLowerCase(Locale.ROOT)) {
            case "craft", "crafting" -> sorted(CRAFTING.keySet());
            case "smelt", "smelting" -> sorted(SMELTING.keySet());
            case "stonecut", "stonecutting" -> sorted(STONECUTTING.keySet());
            case "smith", "smithing" -> sorted(SMITHING.keySet());
            case "custom" -> sorted(CUSTOM.keySet());
            case "fuel" -> sorted(FUELS.keySet());
            case "tag" -> sorted(ITEM_TAGS.keySet());
            default -> List.of();
        };
    }

    public static List<String> search(String kind, String pattern) {
        String p = pattern == null ? "" : pattern.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String id : list(kind)) {
            if (id.toLowerCase(Locale.ROOT).contains(p)) out.add(id);
        }
        return out;
    }

    private static List<String> sorted(Set<String> set) {
        List<String> l = new ArrayList<>(set);
        Collections.sort(l);
        return l;
    }

    private static String cleanId(String id) {
        return id.startsWith("minecraft:") ? id.substring("minecraft:".length()) : id;
    }

    private static String itemId(ItemStack s) {
        if (s == null || s.isEmpty()) return null;
        return BuiltInRegistries.ITEM.getKey(s.getItem()).toString();
    }

    private static Object invokeAny(Object obj, String... methods) {
        if (obj == null) return null;
        for (String m : methods) {
            try { return obj.getClass().getMethod(m).invoke(obj); }
            catch (Throwable ignored) {}
        }
        return null;
    }

    private static Object getFieldAny(Object obj, String... fields) {
        if (obj == null) return null;
        Class<?> cls = obj.getClass();
        while (cls != null) {
            for (String fName : fields) {
                try {
                    Field f = cls.getDeclaredField(fName);
                    f.setAccessible(true);
                    Object v = f.get(obj);
                    if (v != null) return v;
                } catch (Throwable ignored) {}
            }
            cls = cls.getSuperclass();
        }
        return null;
    }

    private static Identifier keyLocation(Object resourceKey) {
        Object r = invokeAny(resourceKey, "location", "identifier", "getLocation");
        if (r instanceof Identifier rl) return rl;
        if (resourceKey == null) return null;
        String s = resourceKey.toString();
        int idx = s.indexOf('/');
        if (idx >= 0) s = s.substring(idx + 1);
        try { return Identifier.parse(s.replaceAll("[\\[\\]\\s]", "")); }
        catch (Throwable ignored) { return null; }
    }

    private static Identifier holderLocation(Object holder) {
        Object opt = invokeAny(holder, "unwrapKey");
        if (opt instanceof Optional<?> o && o.isPresent()) return keyLocation(o.get());
        return keyLocation(invokeAny(holder, "key"));
    }

    private static String holderRecipeId(RecipeHolder<?> h) {
        Identifier loc = keyLocation(h.id());
        return loc != null ? loc.toString() : h.id().toString();
    }

    private static Object getHolderSet(Ingredient ing) {
        Object res = invokeAny(ing, "values");
        return res != null ? res : getFieldAny(ing, "values", "items", "itemStacks");
    }

    private static boolean isNamedHolderSet(Object holderSet) {
        if (holderSet == null) return false;
        if (holderSet.getClass().getName().contains("Named")) return true;
        return invokeAny(holderSet, "key") != null || getFieldAny(holderSet, "key") != null;
    }

    private static Identifier extractTagKey(Object namedSet) {
        Object key = invokeAny(namedSet, "key");
        if (key == null) key = getFieldAny(namedSet, "key");
        if (key == null) return null;
        Identifier loc = keyLocation(key);
        if (loc != null) return loc;
        String s = key.toString();
        int idx = s.lastIndexOf('/');
        if (idx >= 0) s = s.substring(idx + 1).replaceAll("[\\]\\s]", "");
        try { return Identifier.parse(s); } catch (Throwable ignored) { return null; }
    }

    private static List<Object> ingredientHolders(Ingredient ing) {
        if (ing == null) return List.of();
        Object r = invokeAny(ing, "getItems", "getStacks", "getMatchingStacks");
        if (r == null) r = getFieldAny(ing, "items", "itemStacks");

        if (r instanceof ItemStack[] arr) return new ArrayList<>(List.of(arr));
        if (r instanceof Item[] arr) return new ArrayList<>(List.of(arr));
        if (r instanceof java.util.Collection<?> col) return new ArrayList<>(col);

        Object holderSet = getHolderSet(ing);
        if (holderSet == null) return List.of();

        if (holderSet.getClass().isArray()) {
            List<Object> out = new ArrayList<>();
            int len = java.lang.reflect.Array.getLength(holderSet);
            for (int i = 0; i < len; i++) {
                Object v = java.lang.reflect.Array.get(holderSet, i);
                Object items = invokeAny(v, "getItems");
                if (items instanceof java.util.Collection<?> c) out.addAll(c);
                else out.add(v);
            }
            return out;
        }

        try {
            Object stream = invokeAny(holderSet, "stream");
            if (stream != null) {
                Object list = invokeAny(stream, "toList");
                if (list instanceof List<?> l) return new ArrayList<>(l);
            }
        } catch (Throwable ignored) {}

        List<Object> holders = new ArrayList<>();
        try {
            java.util.Iterator<?> it = (java.util.Iterator<?>) invokeAny(holderSet, "iterator");
            while (it != null && it.hasNext()) holders.add(it.next());
            if (!holders.isEmpty()) return holders;
        } catch (Throwable ignored) {}

        return holders;
    }

    private static String holderItemId(Object holder) {
        if (holder == null) return null;
        Object item = holder instanceof Item ? holder : invokeAny(holder, "value", "getItem");
        if (item == null) item = getFieldAny(holder, "item", "value");
        if (item instanceof Item it) {
            Identifier id = BuiltInRegistries.ITEM.getKey(it);
            return id != null ? id.toString() : null;
        }
        return null;
    }

    private static Object ingredientData(Ingredient ing) {
        if (ing == null) return null;
        Object holderSet = getHolderSet(ing);
        if (holderSet != null && isNamedHolderSet(holderSet)) {
            Identifier tagLoc = extractTagKey(holderSet);
            if (tagLoc != null) return "#" + tagLoc;
        }
        List<Object> holders = ingredientHolders(ing);
        if (holders.isEmpty()) return null;

        List<String> ids = new ArrayList<>();
        for (Object h : holders) {
            String id = h instanceof ItemStack st ? itemId(st) : holderItemId(h);
            if (id != null && !ids.contains(id)) ids.add(id);
        }

        if (ids.isEmpty()) return null;
        return ids.size() == 1 ? ids.get(0) : ids;
    }

    private static ItemStack recipeResult(Recipe<?> r, RegistryAccess regs) {
        Object result = invokeAny(r, "getResultItem", "getResult", "result", "getOutput", "output");
        if (result == null) {
            for (String m : new String[]{"getResultItem", "getResult", "result", "getOutput", "output"}) {
                try {
                    result = r.getClass().getMethod(m, RegistryAccess.class).invoke(r, regs);
                    if (result != null) break;
                } catch (Throwable ignored) {}
                try {
                    result = r.getClass().getMethod(m, HolderLookup.Provider.class).invoke(r, regs);
                    if (result != null) break;
                } catch (Throwable ignored) {}
            }
        }
        if (result == null) result = getFieldAny(r, "result", "output");
        return convertToStack(result, regs);
    }

    private static ItemStack convertToStack(Object obj, RegistryAccess regs) {
        if (obj == null) return ItemStack.EMPTY;
        if (obj instanceof ItemStack s) return s;
        for (String m : new String[]{"create", "toStack", "make", "resolve", "asStack"}) {
            try {
                Object v = obj.getClass().getMethod(m, RegistryAccess.class).invoke(obj, regs);
                if (v instanceof ItemStack s) return s;
            } catch (Throwable ignored) {}
            try {
                Object v = obj.getClass().getMethod(m, HolderLookup.Provider.class).invoke(obj, regs);
                if (v instanceof ItemStack s) return s;
            } catch (Throwable ignored) {}
            Object v = invokeAny(obj, m);
            if (v instanceof ItemStack s) return s;
        }
        try {
            Object itemObj = invokeAny(obj, "item");
            Object countObj = invokeAny(obj, "count");
            Item item = itemObj instanceof Item it ? it : (itemObj instanceof Holder<?> h && h.value() instanceof Item i ? i : null);
            if (item != null && countObj instanceof Integer count) return new ItemStack(item, count);
        } catch (Throwable ignored) {}
        return ItemStack.EMPTY;
    }

    private static List<Ingredient> recipeIngredients(Recipe<?> r) {
        Object pattern = invokeAny(r, "pattern");
        if (pattern == null) pattern = getFieldAny(r, "pattern");
        if (pattern != null) {
            Object pIngs = invokeAny(pattern, "ingredients");
            if (pIngs == null) pIngs = getFieldAny(pattern, "ingredients");
            if (pIngs instanceof List<?> list) {
                List<Ingredient> out = new ArrayList<>();
                for (Object o : list) {
                    out.add(coerceIngredient(o));
                }
                return out;
            }
        }

        Object result = invokeAny(r, "getIngredients", "getInputs", "ingredients");
        if (result == null) result = getFieldAny(r, "ingredients", "inputs");
        if (result instanceof List<?> list) {
            List<Ingredient> out = new ArrayList<>();
            for (Object o : list) {
                out.add(coerceIngredient(o));
            }
            return out;
        }
        return List.of();
    }

    private static Ingredient getIngredientByName(Recipe<?> r, String... names) {
        Object v = invokeAny(r, names);
        if (v == null) v = getFieldAny(r, names);
        return coerceIngredient(v);
    }

    private static Ingredient coerceIngredient(Object v) {
        if (v == null) return null;
        if (v instanceof Ingredient ing) return ing;
        if (v instanceof Optional<?> opt) return opt.isPresent() ? coerceIngredient(opt.get()) : null;
        Object inner = invokeAny(v, "ingredient", "getIngredient", "value");
        if (inner == null) inner = getFieldAny(v, "ingredient", "value");
        return inner instanceof Ingredient ing ? ing : null;
    }

    private static String getComponentsData(ItemStack s) {
        try {
            Object comps = invokeAny(s, "getComponentsPatch", "getComponents");
            if (comps != null) {
                Object isEmpty = invokeAny(comps, "isEmpty");
                if (Boolean.TRUE.equals(isEmpty)) return null;
                String str = comps.toString();
                String clean = str.replaceAll("\\s+", "");
                if (clean.isEmpty() || clean.equals("[]") || clean.equals("{}") || clean.equals("DataComponentPatch.EMPTY")) {
                    return null;
                }
                return str;
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static Object extractSingleFluid(Object obj) {
        if (obj == null) return null;
        try {
            Object fluid = invokeAny(obj, "getFluid", "fluid", "getFluidType", "getVariant");
            if (fluid != null) {
                Identifier loc = null;
                if (fluid instanceof net.minecraft.world.level.material.Fluid f) {
                    loc = BuiltInRegistries.FLUID.getKey(f);
                } else {
                    loc = holderLocation(fluid);
                }
                if (loc != null) {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("name", loc.toString());
                    Object amt = invokeAny(obj, "getAmount", "amount");
                    if (amt instanceof Number n) map.put("amount", n.longValue());
                    return map;
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static Object extractFluidData(Object obj) {
        if (obj == null) return null;
        if (obj instanceof java.util.Collection<?> col) {
            List<Object> out = new ArrayList<>();
            for (Object o : col) {
                Object f = extractSingleFluid(o);
                if (f != null) out.add(f);
            }
            return out.isEmpty() ? null : out;
        }
        Object single = extractSingleFluid(obj);
        return single != null ? List.of(single) : null;
    }

    private static int cookingTime(AbstractCookingRecipe r) {
        Object v = invokeAny(r, "getCookingTime", "cookingTime", "getCookTime");
        return v instanceof Integer i ? i : 200;
    }

    private static float cookingExperience(AbstractCookingRecipe r) {
        Object v = invokeAny(r, "getExperience", "experience");
        return v instanceof Number n ? n.floatValue() : 0f;
    }

    private static void putFuelEntry(Map<String, Map<String, Object>> out, String id, int ticks) {
        if (id == null || ticks <= 0) return;
        Map<String, Object> e = new LinkedHashMap<>();
        e.put("type", "fuel");
        e.put("item", id);
        e.put("burn_ticks", ticks);
        e.put("items_smelted", ticks / 200.0);
        out.put(id, e);
    }

    private static Map<String, List<String>> buildTags(RegistryAccess regs) {
        Map<String, List<String>> out = new HashMap<>();
        HolderLookup.RegistryLookup<Item> items = regs.lookupOrThrow(Registries.ITEM);
        items.listTags().forEach(named -> {
            List<String> list = new ArrayList<>();
            for (Holder<Item> h : named) {
                Identifier loc = holderLocation(h);
                if (loc != null) list.add(loc.toString());
            }
            Identifier tagLoc = keyLocation(named.key());
            if (tagLoc != null) out.put("#" + tagLoc, list);
        });
        return out;
    }

    private static void buildRecipes(RegistryAccess regs, RecipeManager rm,
                                     Map<String, List<Map<String, Object>>> crafting,
                                     Map<String, List<Map<String, Object>>> smelting,
                                     Map<String, List<Map<String, Object>>> stonecutting,
                                     Map<String, List<Map<String, Object>>> smithing,
                                     Map<String, List<Map<String, Object>>> custom,
                                     Map<String, Map<String, Object>> smithingById) {
        for (RecipeHolder<?> holder : rm.getRecipes()) {
            Recipe<?> r = holder.value();
            try {
                Identifier key = BuiltInRegistries.RECIPE_SERIALIZER.getKey(r.getSerializer());
                if (key == null) continue;
                String path = key.getPath();
                switch (path) {
                    case "crafting_shaped" -> addGridRecipe(regs, holder, r, "craft", crafting, ((ShapedRecipe) r).getWidth());
                    case "crafting_shapeless" -> addGridRecipe(regs, holder, r, "craft", crafting, 0);
                    case "smelting", "blasting", "smoking", "campfire_cooking" -> addSingleInputRecipe(regs, holder, r, path, smelting, true);
                    case "stonecutting" -> addSingleInputRecipe(regs, holder, r, "stonecut", stonecutting, false);
                    case "smithing_transform", "smithing_trim" -> addSmithing(regs, holder, smithing, smithingById, path);
                    default -> addGenericRecipe(regs, holder, r, path, custom);
                }
            } catch (Throwable ignored) {}
        }
    }

    private static Map<String, Object> baseRecipeMap(String type, RecipeHolder<?> h, ItemStack result) {
        Map<String, Object> e = new LinkedHashMap<>();
        e.put("type", type);
        e.put("id", holderRecipeId(h));
        if (result != null && !result.isEmpty()) {
            Map<String, Object> output = new LinkedHashMap<>();
            output.put("name", itemId(result));
            output.put("count", result.getCount());
            String comp = getComponentsData(result);
            if (comp != null) output.put("components", comp);
            e.put("output", output);
        }
        return e;
    }

    private static void addGridRecipe(RegistryAccess regs, RecipeHolder<?> h, Recipe<?> r,
                                      String type, Map<String, List<Map<String, Object>>> out, int width) {
        ItemStack result = recipeResult(r, regs);
        String rid = itemId(result);
        if (rid == null) return;
        Map<String, Object> e = baseRecipeMap(type, h, result);
        Map<String, Object> grid = new LinkedHashMap<>();

        if (width > 0 && r instanceof ShapedRecipe sr) {
            Object pattern = invokeAny(sr, "pattern");
            if (pattern == null) pattern = getFieldAny(sr, "pattern");

            List<String> rows = null;
            Map<?, ?> keyMap = null;

            if (pattern != null) {
                Object patternData = invokeAny(pattern, "data");
                if (patternData == null) patternData = getFieldAny(pattern, "data");
                Object target = patternData != null ? patternData : pattern;

                Object rObj = invokeAny(target, "rows", "pattern");
                if (rObj == null) rObj = getFieldAny(target, "rows", "pattern");
                if (rObj instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof String) {
                    rows = (List<String>) list;
                }

                Object kObj = invokeAny(target, "key");
                if (kObj == null) kObj = getFieldAny(target, "key");
                if (kObj instanceof Map<?, ?> map) {
                    keyMap = map;
                }
            }

            if (rows != null && keyMap != null) {
                for (int y = 0; y < rows.size() && y < 3; y++) {
                    String row = rows.get(y);
                    for (int x = 0; x < row.length() && x < 3; x++) {
                        char c = row.charAt(x);
                        if (c == ' ') continue;
                        Object val = keyMap.get(c);
                        if (val == null) val = keyMap.get(String.valueOf(c));
                        Ingredient ing = coerceIngredient(val);
                        Object data = ingredientData(ing);
                        if (data != null) {
                            grid.put(String.valueOf(y * 3 + x + 1), data);
                        }
                    }
                }
            } else {
                List<Ingredient> ing = recipeIngredients(r);
                int height = (ing.size() + width - 1) / width;
                for (int y = 0; y < height && y < 3; y++) {
                    for (int x = 0; x < width && x < 3; x++) {
                        int idx = y * width + x;
                        if (idx < ing.size()) {
                            Object s = ingredientData(ing.get(idx));
                            if (s != null) {
                                grid.put(String.valueOf(y * 3 + x + 1), s);
                            }
                        }
                    }
                }
            }
        } else {
            List<Ingredient> ing = recipeIngredients(r);
            for (int i = 0; i < ing.size() && i < 9; i++) {
                Object s = ingredientData(ing.get(i));
                if (s != null) {
                    grid.put(String.valueOf(i + 1), s);
                }
            }
        }

        e.put("grid", grid);
        out.computeIfAbsent(rid, k -> new ArrayList<>()).add(e);
    }

    private static void addSingleInputRecipe(RegistryAccess regs, RecipeHolder<?> h, Recipe<?> r,
                                             String kind, Map<String, List<Map<String, Object>>> out, boolean isCooking) {
        ItemStack result = recipeResult(r, regs);
        String rid = itemId(result);
        if (rid == null) return;
        Map<String, Object> e = baseRecipeMap(kind, h, result);
        Ingredient input = getIngredientByName(r, "ingredient", "input", "getIngredient");
        if (input == null) {
            List<Ingredient> list = recipeIngredients(r);
            if (!list.isEmpty()) input = list.get(0);
        }
        if (input != null) {
            Object s = ingredientData(input);
            if (s != null) e.put("input", s);
        }
        if (isCooking && r instanceof AbstractCookingRecipe cr) {
            e.put("cookingtime", cookingTime(cr));
            e.put("experience", (double) cookingExperience(cr));
        }
        out.computeIfAbsent(rid, k -> new ArrayList<>()).add(e);
    }

    private static void addSmithing(RegistryAccess regs, RecipeHolder<?> h,
                                    Map<String, List<Map<String, Object>>> out,
                                    Map<String, Map<String, Object>> byId,
                                    String typeId) {
        Recipe<?> r = h.value();
        String recipeId = holderRecipeId(h);
        ItemStack res = recipeResult(r, regs);

        Map<String, Object> e = baseRecipeMap(typeId, h, res);
        Object outputData = e.remove("output");
        if (outputData != null) e.put("result", outputData);

        Ingredient template = getIngredientByName(r, "template", "getTemplate");
        Ingredient base = getIngredientByName(r, "base", "baseIngredient", "getBase");
        Ingredient addition = getIngredientByName(r, "addition", "additionIngredient", "getAddition");

        if (template == null || base == null || addition == null) {
            List<Ingredient> list = recipeIngredients(r);
            if (list.size() > 0 && template == null) template = list.get(0);
            if (list.size() > 1 && base == null) base = list.get(1);
            if (list.size() > 2 && addition == null) addition = list.get(2);
        }

        if (template != null) e.put("template", ingredientData(template));
        if (base != null) e.put("base", ingredientData(base));
        if (addition != null) e.put("addition", ingredientData(addition));

        String key = (!res.isEmpty() && itemId(res) != null) ? itemId(res) : recipeId;
        out.computeIfAbsent(key, k -> new ArrayList<>()).add(e);
        byId.put(recipeId, e);
    }

    private static void addGenericRecipe(RegistryAccess regs, RecipeHolder<?> h, Recipe<?> r,
                                         String path, Map<String, List<Map<String, Object>>> out) {
        ItemStack result = recipeResult(r, regs);
        String rid = itemId(result);
        Map<String, Object> e = baseRecipeMap(path, h, result);

        List<Ingredient> ing = recipeIngredients(r);
        if (!ing.isEmpty()) {
            List<Object> inputs = new ArrayList<>();
            for (Ingredient i : ing) {
                Object data = ingredientData(i);
                if (data != null) inputs.add(data);
            }
            if (!inputs.isEmpty()) e.put("inputs", inputs);
        }

        Object fluidIn = extractFluidData(invokeAny(r, "getFluidIngredients", "getFluidInputs", "fluids", "fluid"));
        if (fluidIn == null) fluidIn = extractFluidData(getFieldAny(r, "fluidIngredients", "fluidInputs", "fluids", "fluid"));
        if (fluidIn != null) e.put("fluids_in", fluidIn);

        Object fluidOut = extractFluidData(invokeAny(r, "getFluidResults", "getFluidOutputs", "fluidResults"));
        if (fluidOut == null) fluidOut = extractFluidData(getFieldAny(r, "fluidResults", "fluidOutputs"));
        if (fluidOut != null) e.put("fluids_out", fluidOut);

        if (rid == null) {
            rid = e.containsKey("fluids_out") ? "fluid_output" : holderRecipeId(h);
        }
        out.computeIfAbsent(rid, k -> new ArrayList<>()).add(e);
    }

    private static List<Map<String, Object>> buildBrewing(MinecraftServer server) {
        List<Map<String, Object>> out = new ArrayList<>();
        try {
            //? if <26.3 {
            /*PotionBrewing brewing = server.potionBrewing();
            var potionRegistry = server.registryAccess().lookupOrThrow(Registries.POTION);
            String[] ingredients = {
                    "minecraft:nether_wart", "minecraft:redstone", "minecraft:glowstone_dust",
                    "minecraft:fermented_spider_eye", "minecraft:gunpowder", "minecraft:dragon_breath",
                    "minecraft:sugar", "minecraft:rabbit_foot", "minecraft:glistering_melon_slice",
                    "minecraft:spider_eye", "minecraft:pufferfish", "minecraft:magma_cream",
                    "minecraft:golden_carrot", "minecraft:blaze_powder", "minecraft:ghast_tear",
                    "minecraft:phantom_membrane", "minecraft:turtle_helmet", "minecraft:breeze_rod",
                    "minecraft:slime_ball", "minecraft:cobweb", "minecraft:stone"
            };

            Set<String> seen = new HashSet<>();
            List<Holder.Reference<Potion>> potions = potionRegistry.listElements().toList();

            for (Holder.Reference<Potion> fromRef : potions) {
                Identifier fromLoc = holderLocation(fromRef);
                if (fromLoc == null) continue;
                String fromId = fromLoc.toString();
                ItemStack fromStack = PotionContents.createItemStack(Items.POTION, fromRef);

                for (String ingId : ingredients) {
                    try {
                        Item ing = BuiltInRegistries.ITEM.getOptional(Identifier.parse(ingId)).orElse(null);
                        if (ing == null || ing == Items.AIR) continue;

                        ItemStack result = brewing.mix(new ItemStack(ing), fromStack);
                        if (result.isEmpty()) continue;

                        PotionContents contents = result.get(DataComponents.POTION_CONTENTS);
                        if (contents == null || contents.potion().isEmpty()) continue;

                        Identifier toLoc = holderLocation(contents.potion().get());
                        if (toLoc == null) continue;

                        String toId = toLoc.toString();
                        if (toId.equals(fromId) || !seen.add(fromId + "|" + ingId + "|" + toId)) continue;

                        Map<String, Object> e = new LinkedHashMap<>();
                        e.put("type", "brewing");
                        e.put("from", cleanId(fromId));
                        e.put("ingredient", ingId);
                        e.put("to", cleanId(toId));
                        out.add(e);
                    } catch (Throwable ignored) {}
                }
            }
            *///?} else {
            Set<String> seen = new HashSet<>();
            for (RecipeHolder<?> holder : server.getRecipeManager().getRecipes()) {
                Recipe<?> r = holder.value();
                Identifier key = BuiltInRegistries.RECIPE_SERIALIZER.getKey(r.getSerializer());
                if (key == null) continue;
                String path = key.getPath();
                if (path.contains("brewing")) {
                    Object inputObj = getFieldAny(r, "input");
                    Object reagentObj = getFieldAny(r, "reagent");
                    Object outputObj = getFieldAny(r, "output", "result");

                    if (inputObj != null && reagentObj != null && outputObj != null) {
                        String fromId = null;
                        Object potionsOpt = invokeAny(inputObj, "potions");
                        if (potionsOpt == null) potionsOpt = getFieldAny(inputObj, "potions");
                        if (potionsOpt instanceof Optional<?> opt && opt.isPresent()) {
                            Object pred = opt.get();
                            Object setOpt = invokeAny(pred, "potions");
                            if (setOpt == null) setOpt = getFieldAny(pred, "potions");
                            Object holderSet = setOpt instanceof Optional<?> sOpt && sOpt.isPresent() ? sOpt.get() : setOpt;
                            if (holderSet instanceof Iterable<?> it) {
                                var iterator = it.iterator();
                                if (iterator.hasNext()) {
                                    Identifier fromLoc = holderLocation(iterator.next());
                                    if (fromLoc != null) fromId = fromLoc.toString();
                                }
                            }
                        }

                        if (fromId == null) {
                            Object ingObj = invokeAny(inputObj, "ingredient");
                            if (ingObj == null) ingObj = getFieldAny(inputObj, "ingredient");
                            Ingredient inputIng = coerceIngredient(ingObj != null ? ingObj : inputObj);
                            Object inStr = ingredientData(inputIng);
                            if (inStr != null) {
                                fromId = inStr instanceof List<?> l && !l.isEmpty() ? l.get(0).toString() : inStr.toString();
                            }
                        }

                        Ingredient reagentIng = coerceIngredient(reagentObj);
                        Object reagentData = ingredientData(reagentIng);
                        String reagentStr = reagentData instanceof List<?> l && !l.isEmpty() ? l.get(0).toString() : (reagentData != null ? reagentData.toString() : null);

                        String toId = null;
                        Object potionOpt = invokeAny(outputObj, "potion");
                        if (potionOpt instanceof Optional<?> opt && opt.isPresent()) {
                            Identifier toLoc = holderLocation(opt.get());
                            if (toLoc != null) toId = toLoc.toString();
                        }

                        if (toId == null) {
                            ItemStack resStack = convertToStack(outputObj, server.registryAccess());
                            if (!resStack.isEmpty()) {
                                try {
                                    PotionContents potContents = resStack.get(DataComponents.POTION_CONTENTS);
                                    if (potContents != null && potContents.potion().isPresent()) {
                                        Identifier toLoc = holderLocation(potContents.potion().get());
                                        if (toLoc != null) toId = toLoc.toString();
                                    }
                                } catch (Throwable ignored) {}
                                if (toId == null) {
                                    toId = itemId(resStack);
                                }
                            }
                        }

                        if (fromId != null && reagentStr != null && toId != null) {
                            String cleanedFrom = cleanId(fromId);
                            String cleanedTo = cleanId(toId);
                            if (!cleanedTo.equals(cleanedFrom) && seen.add(cleanedFrom + "|" + reagentStr + "|" + cleanedTo)) {
                                Map<String, Object> e = new LinkedHashMap<>();
                                e.put("type", "brewing");
                                e.put("from", cleanedFrom);
                                e.put("ingredient", reagentStr);
                                e.put("to", cleanedTo);
                                out.add(e);
                            }
                        }
                    }
                }
            }
            //?}
        } catch (Throwable t) {
            CCShared.LOGGER.warn("[cc: Shared] brewing unavailable: {}", t.toString());
        }
        return out;
    }

    private static Map<String, Map<String, Object>> buildFuels(MinecraftServer server) {
        Map<String, Map<String, Object>> out = new HashMap<>();
        try {
            //? if <26.3 {
            /*Method fuelValuesM;
            try { fuelValuesM = server.getClass().getMethod("fuelValues"); }
            catch (NoSuchMethodException ignored) { return out; }

            Object fuelValues = fuelValuesM.invoke(server);
            Method burn = fuelValues.getClass().getMethod("burnDuration", ItemStack.class);

            var items = server.registryAccess().lookupOrThrow(Registries.ITEM);
            items.listElements().forEach(holder -> {
                try {
                    int ticks = (int) burn.invoke(fuelValues, new ItemStack(holder.value()));
                    Identifier loc = holderLocation(holder);
                    if (loc != null) putFuelEntry(out, loc.toString(), ticks);
                } catch (Throwable ignored) {}
            });
            *///?} else {
            var items = server.registryAccess().lookupOrThrow(Registries.ITEM);
            Object intProvidersRegistry = null;
            try {
                intProvidersRegistry = server.registryAccess().lookupOrThrow(Registries.CONTEXT_INT_PROVIDER);
            } catch (Throwable ignored) {}
            final Object finalIntProviders = intProvidersRegistry;

            items.listElements().forEach(holder -> {
                try {
                    ItemStack stack = new ItemStack(holder.value());
                    Object cookingFuel = stack.get(DataComponents.COOKING_FUEL);

                    if (cookingFuel != null) {
                        int ticks = 0;
                        Object burnTime = invokeAny(cookingFuel, "burnTime");

                        if (burnTime != null) {
                            String simpleName = burnTime.getClass().getSimpleName();

                            if (simpleName.equals("Constant")) {
                                Object val = invokeAny(burnTime, "value");
                                if (val instanceof Integer i) ticks = i;

                            } else if (simpleName.equals("Reference")) {
                                Object keyObj = invokeAny(burnTime, "key");
                                if (keyObj instanceof net.minecraft.resources.ResourceKey<?> key) {
                                    if (finalIntProviders != null) {
                                        try {
                                            Method getM = finalIntProviders.getClass().getMethod("get", net.minecraft.resources.ResourceKey.class);
                                            Object optObj = getM.invoke(finalIntProviders, key);
                                            if (optObj instanceof Optional<?> opt && opt.isPresent()) {
                                                Object providerHolder = opt.get();
                                                Object providerValue = invokeAny(providerHolder, "value");
                                                ticks = extractFuelTicks(providerValue, 0);
                                            }
                                        } catch (Throwable ignored) {}
                                    }

                                    if (ticks <= 0) {
                                        String keyStr = key.toString();
                                        if (keyStr.contains("time_wood_blocks")) ticks = 300;
                                        else if (keyStr.contains("time_wood_items")) ticks = 200;
                                        else if (keyStr.contains("time_wood_saplings")) ticks = 100;
                                        else if (keyStr.contains("time_coal_block")) ticks = 16000;
                                        else if (keyStr.contains("time_coal")) ticks = 1600;
                                        else if (keyStr.contains("time_lava_bucket")) ticks = 20000;
                                        else if (keyStr.contains("time_kelp_block")) ticks = 4000;
                                        else if (keyStr.contains("time_blaze_rod")) ticks = 2400;
                                        else if (keyStr.contains("time_bamboo_block")) ticks = 300;
                                        else if (keyStr.contains("time_bamboo")) ticks = 50;
                                        else if (keyStr.contains("time_scaffolding")) ticks = 400;
                                    }
                                }
                            }
                        }

                        if (ticks <= 0) {
                            ticks = extractFuelTicks(cookingFuel, 0);
                        }

                        Identifier loc = holderLocation(holder);
                        if (loc != null) putFuelEntry(out, loc.toString(), ticks);
                    }
                } catch (Throwable ignored) {}
            });
            //?}
        } catch (Throwable t) {
            CCShared.LOGGER.warn("[cc: Shared] fuels unavailable: {}", t.toString());
        }
        return out;
    }

    private static int extractFuelTicks(Object obj, int depth) {
        if (obj == null || depth > 5) return 0;
        if (obj instanceof Integer i && i > 0) return i;

        try {
            Object burnTimeObj = invokeAny(obj, "burnTime", "uses");
            if (burnTimeObj != null && burnTimeObj.getClass().getSimpleName().equals("Constant")) {
                Object val = invokeAny(burnTimeObj, "value", "amount");
                if (val instanceof Integer i && i > 0) return i;
            }

            for (Field f : obj.getClass().getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
                f.setAccessible(true);
                Object val = f.get(obj);
                if (val instanceof Integer i && i > 0) return i;
            }

            for (Field f : obj.getClass().getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
                f.setAccessible(true);
                int nested = extractFuelTicks(f.get(obj), depth + 1);
                if (nested > 0) return nested;
            }
        } catch (Throwable ignored) {}

        return 0;
    }

    private RecipeIndex() {}
}