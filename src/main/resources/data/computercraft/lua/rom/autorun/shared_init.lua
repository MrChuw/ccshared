-- Ensure the module was loaded
local make_shared = require("cc.shared").make

-- Inject into the global environment (_G)
_G.shared, _G.shared_package = make_shared("/shared")