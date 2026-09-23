# cc: Shared
A Fabric and NeoForge mod for CC: Tweaked that adds a **shared folder** every
computer can read from and write to. And the reason is that I am too lazy to use floppy disks or whatever.

## Features

- **Shared mount** — every computer (pocket, turtle, command, normal) gets a
  `/shared` directory mounted automatically when its filesystem is created.
  Backed by the world save at `computercraft/shared/`, so it persists across
  restarts and is shared across every computer in the same world.
- **1 GB write limit** — the mount is a `WritableMount`, so computers can both
  read and write.
- **Lua globals** — an autorun script exposes `shared` and `shared_package`
  inside every computer, so scripts can use:

  ```lua
  local mymodule = shared("mymodule")          -- loads /shared/mymodule.lua
  local nested   = shared("lib.utils")          -- loads /shared/lib/utils.lua
  local pkg      = shared_package               -- the raw package table
  ```

  Search paths are `/shared/?.lua` and `/shared/?/init.lua`, matching CC's
  usual module conventions.

## Requirements

Fabric Loader `0.18.0+` on every supported Minecraft version.

| Minecraft | Fabric API      | NeoForge       | CC: Tweaked |
|-----------|-----------------|----------------|-------------|
| 1.21.1    | 0.115.6+1.21.1  | 21.1.230+      | 1.117.1     |
| 1.21.11   | 0.141.5+1.21.11 | 21.11.45+      | 1.117.1     |
| 26.1.2    | 0.155.2+26.1.2  | 26.1.2.109+    | 1.120.0     |
| 26.2      | 0.161.0+26.2    | 26.2.0.88+     | 1.120.2     |
| 26.3      | 0.160.5+26.3    | 26.3.0.0-beta+ | 1.120.2     |

> **Note on 1.21.10:** Not supported — CC: Tweaked has no build for that
> Minecraft version.

> **Note on 26.3:** Is broken but this mod still works.

## Usage

Inside any computer, open the Lua REPL and try:

```lua
-- List what's in the shared folder
for _, name in ipairs(fs.list("/shared")) do
    print(name)
end

-- Load a module someone dropped in /shared/lib/logger.lua
local logger = shared("lib.logger")
logger.info("hello from another computer")
```

To drop files into the shared folder from outside the game, edit the world
save directly:

```
saves/<world>/computercraft/shared/
```

Files written there are visible to every computer the next time they read.

## Building

```
./gradlew buildAndCollect
```

### Usage
- Use `"Set active project to ..."` Gradle tasks to update the Minecraft
  version available in `src/` classes.
- Use `buildAndCollect` Gradle task to store mod releases in `build/libs/`.

