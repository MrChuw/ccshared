-- dump_categories.lua
-- Usage: dump_categories [kind] [limit]
--   kind  = craft | smelt | stonecut | smith | custom | fuel | fuels_grouped | tag | brew | all  (default: all)
--   limit = how many entries per category (default: 50; use 0 for all)
--
-- Output: /shared/dump/<kind>.json
--         /shared/dump/index.json  (summary with totals)

local args = { ... }
local kindArg = args[1]
local limit   = tonumber(args[2] or 50) or 50
local target  = (kindArg or "all"):lower()

local OUT = "/shared/dump"

local function ensureDir(path)
    if not fs.exists(path) then
        fs.makeDir(path)
    end
end

local function writeJson(path, data)
    local f = fs.open(path, "w")
    if not f then
        print("Failed to open " .. path .. " for writing")
        return false
    end
    f.write(textutils.serializeJSON(data, { compact = false }))
    f.close()
    return true
end

local function takeFirst(list, n)
    if not list then return {} end
    if n <= 0 then return list end
    local out = {}
    for i = 1, math.min(n, #list) do out[i] = list[i] end
    return out
end

local function pick(recipe, keys)
    if not recipe then return nil end
    local out = {}
    for _, k in ipairs(keys) do
        if recipe[k] ~= nil then out[k] = recipe[k] end
    end
    return out
end

local function collectCraft(limitN)
    local ids = recipes.list("craft")
    local out = {}
    for _, id in ipairs(takeFirst(ids, limitN)) do
        local rList = recipes.crafting(id)
        if type(rList) == "table" then
            out[id] = {}
            for _, r in ipairs(rList) do
                table.insert(out[id], pick(r, {"type", "id", "output", "grid"}))
            end
        end
    end
    return out
end

local function collectSmelt(limitN)
    local ids = recipes.list("smelt")
    local out = {}
    for _, id in ipairs(takeFirst(ids, limitN)) do
        local rList = recipes.smelting(id)
        if type(rList) == "table" then
            out[id] = {}
            for _, r in ipairs(rList) do
                table.insert(out[id], pick(r, {"type", "id", "input", "output", "cookingtime", "experience"}))
            end
        end
    end
    return out
end

local function collectStonecutting(limitN)
    local ids = recipes.list("stonecut")
    local out = {}
    for _, id in ipairs(takeFirst(ids, limitN)) do
        local rList = recipes.stonecutting(id)
        if type(rList) == "table" then
            out[id] = {}
            for _, r in ipairs(rList) do
                table.insert(out[id], pick(r, {"type", "id", "input", "output"}))
            end
        end
    end
    return out
end

local function collectSmith(limitN)
    local ids = recipes.list("smith")
    local out = {}
    for _, id in ipairs(takeFirst(ids, limitN)) do
        local rList = recipes.smithing(id)
        if type(rList) == "table" then
            out[id] = {}
            for _, r in ipairs(rList) do
                table.insert(out[id], pick(r, {"type", "id", "template", "base", "addition", "result"}))
            end
        end
    end
    return out
end

local function collectCustom(limitN)
    local ids = recipes.list("custom")
    local out = {}
    for _, id in ipairs(takeFirst(ids, limitN)) do
        local rList = recipes.custom(id)
        if type(rList) == "table" then
            out[id] = {}
            for _, r in ipairs(rList) do
                table.insert(out[id], pick(r, {"type", "id", "inputs", "output", "fluids_in", "fluids_out"}))
            end
        end
    end
    return out
end

local function collectFuel(limitN)
    local ids = recipes.list("fuel")
    local out = {}
    for _, id in ipairs(takeFirst(ids, limitN)) do
        local r = recipes.fuel(id)
        if r then
            out[id] = pick(r, {"type", "item", "burn_ticks", "items_smelted"})
        end
    end
    return out
end

local function collectFuelsGrouped(limitN)
    local data = recipes.fuels()
    if type(data) ~= "table" then return {} end
    if limitN <= 0 then return data end

    local out = {}
    for groupName, groups in pairs(data) do
        out[groupName] = {}
        local count = 0
        for valKey, items in pairs(groups) do
            if count >= limitN then break end
            out[groupName][valKey] = items
            count = count + 1
        end
    end
    return out
end

local function collectTags(limitN)
    local ids = recipes.list("tag")
    local out = {}
    for _, id in ipairs(takeFirst(ids, limitN)) do
        local items = recipes.itemTag(id) or {}
        out[id] = items
    end
    return out
end

local function collectBrewing(limitN)
    local list = recipes.brewing() or {}
    return takeFirst(list, limitN)
end

local function countEntries(data)
    if type(data) ~= "table" then return 0 end
    if data[1] ~= nil then return #data end
    local count = 0
    for _ in pairs(data) do count = count + 1 end
    return count
end

local function dumpAll()
    ensureDir(OUT)

    print("Writing dumps to " .. OUT .. " ...")

    local n = limit
    local results = {}

    local jobs = {
        { name = "crafting",       fn = collectCraft         },
        { name = "smelting",       fn = collectSmelt         },
        { name = "stonecutting",   fn = collectStonecutting  },
        { name = "smithing",       fn = collectSmith         },
        { name = "custom",         fn = collectCustom        },
        { name = "fuels",          fn = collectFuel          },
        { name = "fuels_grouped",  fn = collectFuelsGrouped  },
        { name = "item_tags",      fn = collectTags          },
        { name = "brewing",        fn = collectBrewing       },
    }

    for _, job in ipairs(jobs) do
        local data = job.fn(n)
        local path = fs.combine(OUT, job.name .. ".json")
        writeJson(path, data)

        local count = countEntries(data)
        results[job.name] = { file = path, count = count }
        print(string.format("  -> %-20s %d entries", job.name .. ".json", count))
    end

    local index = {
        generated = os.date("!%Y-%m-%dT%H:%M:%SZ"),
        limit     = limit,
        counts    = {},
    }
    for k, v in pairs(results) do index.counts[k] = v.count end
    writeJson(fs.combine(OUT, "index.json"), index)

    print("Done.")
end

local function dumpOne(kind)
    ensureDir(OUT)

    local map = {
        craft         = { name = "crafting",      fn = collectCraft         },
        crafting      = { name = "crafting",      fn = collectCraft         },
        smelt         = { name = "smelting",      fn = collectSmelt         },
        smelting      = { name = "smelting",      fn = collectSmelt         },
        stonecut      = { name = "stonecutting",  fn = collectStonecutting  },
        stonecutting  = { name = "stonecutting",  fn = collectStonecutting  },
        smith         = { name = "smithing",      fn = collectSmith         },
        smithing      = { name = "smithing",      fn = collectSmith         },
        custom        = { name = "custom",        fn = collectCustom        },
        fuel          = { name = "fuels",         fn = collectFuel          },
        fuels         = { name = "fuels",         fn = collectFuel          },
        fuels_grouped = { name = "fuels_grouped", fn = collectFuelsGrouped  },
        grouped_fuel  = { name = "fuels_grouped", fn = collectFuelsGrouped  },
        tag           = { name = "item_tags",     fn = collectTags          },
        tags          = { name = "item_tags",     fn = collectTags          },
        brew          = { name = "brewing",       fn = collectBrewing       },
        brewing       = { name = "brewing",       fn = collectBrewing       },
    }

    local job = map[kind]
    if not job then
        print("Unknown kind: " .. kind)
        print("Use: all | craft | smelt | stonecut | smith | custom | fuel | fuels_grouped | tag | brew")
        return
    end

    local data = job.fn(limit)
    local path = fs.combine(OUT, job.name .. ".json")
    writeJson(path, data)

    local count = countEntries(data)
    print(string.format("Wrote %s (%d entries, limit=%d)", path, count, limit))
end

if target == "all" then
    dumpAll()
else
    dumpOne(target)
end