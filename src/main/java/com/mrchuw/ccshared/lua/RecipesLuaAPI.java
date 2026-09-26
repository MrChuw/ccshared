package com.mrchuw.ccshared.lua;

import com.mrchuw.ccshared.data.RecipeIndex;
import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import java.util.Optional;

public class RecipesLuaAPI implements ILuaAPI {

    @Override
    public String[] getNames() {
        return new String[] { "recipes", "cc_recipes" };
    }

    @LuaFunction
    public final Object get(String id) {
        return RecipeIndex.get(id);
    }

    @LuaFunction
    public final Object crafting(String id) {
        return RecipeIndex.crafting(id);
    }

    @LuaFunction
    public final Object smelting(String id) {
        return RecipeIndex.smelting(id);
    }

    @LuaFunction
    public final Object stonecutting(String id) {
        return RecipeIndex.stonecutting(id);
    }

    @LuaFunction
    public final Object smithing(String id) {
        return RecipeIndex.smithing(id);
    }

    @LuaFunction
    public final Object custom(String id) {
        return RecipeIndex.custom(id);
    }

    @LuaFunction
    public final Object fuel(String id) {
        return RecipeIndex.fuel(id);
    }

    @LuaFunction
    public final Object fuels(IArguments args) throws LuaException {
        String group = args.optString(0, null);
        Object value = args.get(1);
        return RecipeIndex.fuelsByGroup(group, value);
    }

    @LuaFunction
    public final Object brewing() {
        return RecipeIndex.brewing();
    }

    @LuaFunction
    public final Object itemTag(String tag) {
        return RecipeIndex.itemTag(tag);
    }

    @LuaFunction
    public final Object list(String kind) {
        return RecipeIndex.list(kind);
    }

    @LuaFunction
    public final Object search(String kind, String pattern) {
        return RecipeIndex.search(kind, pattern);
    }

    @LuaFunction
    public final Object smithingById(String id) {
        return RecipeIndex.smithingById(id);
    }
}