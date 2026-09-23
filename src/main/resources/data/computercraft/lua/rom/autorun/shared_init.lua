-- Garante que o módulo foi carregado
local make_shared = require("cc.shared").make

-- Injeta no ambiente global (_G)
_G.shared, _G.shared_package = make_shared("/shared")