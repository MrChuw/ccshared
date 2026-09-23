-- SPDX-License-Identifier: MPL-2.0

local expect = require("cc.expect").expect

local function make_searchpath(root)
    return function(name)
        expect(1, name, "string")
        local module = name:gsub("%.", "/")
        local patterns = { root .. "/?.lua", root .. "/?/init.lua" }
        local errors = {}
        for _, pattern in ipairs(patterns) do
            local path = pattern:gsub("%?", module)
            if fs.exists(path) and not fs.isDir(path) then
                return path
            end
            errors[#errors + 1] = "no file '" .. path .. "'"
        end
        return nil, table.concat(errors, "\n  ")
    end
end

local function preload(shared)
    return function(name)
        if shared.preload[name] then
            return shared.preload[name]
        end
        return nil, "no field shared.preload['" .. name .. "']"
    end
end

local function from_file(shared)
    return function(name)
        local path, err = shared.searchpath(name)
        if not path then return nil, err end
        local fn, load_err = loadfile(path)
        if not fn then return nil, load_err end
        return fn, path
    end
end

local function make_shared(root)
    expect(1, root, "string")
    local shared = {}
    local sentinel = {}
    shared.loaded = {}
    shared.preload = {}
    shared.searchpath = make_searchpath(root)
    local loaders = { preload(shared), from_file(shared) }
    local function require_shared(name)
        expect(1, name, "string")
        if shared.loaded[name] == sentinel then
            error("loop or previous error loading shared module '" .. name .. "'", 0)
        end
        if shared.loaded[name] ~= nil then
            return shared.loaded[name]
        end
        local err = "shared module '" .. name .. "' not found:"
        for _, loader in ipairs(loaders) do
            local result = table.pack(loader(name))
            if result[1] then
                shared.loaded[name] = sentinel
                local value = result[1](name, table.unpack(result, 2, result.n))
                if value == nil then value = true end
                shared.loaded[name] = value
                return value
            end
            err = err .. "\n  " .. result[2]
        end
        error(err, 2)
    end
    return require_shared, shared
end

return { make = make_shared }
