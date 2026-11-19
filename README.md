# Vein Miner

Vein Miner is a server-side Fabric and NeoForge mod for Minecraft 26.3 that mines connected ore and tree blocks when a player breaks a block.

## Features

- Global and per-player block and block category matching (`#vein_miner:ores` and `#vein_miner:trees)` - those tags include `#c:ores` and `#minecraft:logs` respectively

* Works with directly adjacent faces (6), faces + edges (18), or faces + edges + corners (3x3 - 26)
* Groups normal + deepslate ore variants and log + wood variants
* Durability and exhaustion costs
* Fast leaf decay
* Accounts for spawn protection, world border, unloaded chunks and statistics correctly

## Requirements

- Fabric Loader >=0.19.5 w/ Fabric API or NeoForge >=26.3.0.99
- BrainageLib >=2.0.0
- Java >=25

## Commands

### Player commands

`/veinminer` shows global and personal state plus relevant help:

- `/veinminer help`
- `/veinminer toggle`
- `/veinminer enable`
- `/veinminer disable`
- `/veinminer mode while_sneaking`
- `/veinminer mode while_not_sneaking`
- `/veinminer mode always`
- `/veinminer mode never`
- `/veinminer whitelist`
- `/veinminer whitelist enable`
- `/veinminer whitelist disable`
- `/veinminer whitelist add <block>`
- `/veinminer whitelist remove <block>`
- `/veinminer whitelist list [page]`
- `/veinminer whitelist clear`

Adding the first personal block enables the personal whitelist filter. Clearing it leaves an empty filter enabled, which intentionally prevents all vein mining until the player disables the filter or adds another allowed block.

### Operator commands

Operator-only controls are under `/veinminer admin:`

- `/veinminer admin`
- `/veinminer admin enable|disable|toggle`
- `/veinminer admin reload`
- `/veinminer admin default_mode while_sneaking|while_not_sneaking|always|never`
- `/veinminer admin adjacency faces|faces_edges|faces_edges_corners`
- `/veinminer admin limit ore|tree|other <blocks>`
- `/veinminer admin durability cost <points>`
- `/veinminer admin durability minimum_remaining <points>`
- `/veinminer admin durability protect_tool enable|disable`
- `/veinminer admin exhaustion <amount>`
- `/veinminer admin better_ores enable|disable`
- `/veinminer admin better_trees enable|disable`
- `/veinminer admin fast_leaf_decay enable|disable`
- `/veinminer admin fast_leaf_decay multiplier <amount>`
- `/veinminer admin selection blocks allow add|remove <block>`
- `/veinminer admin selection blocks allow list [page]`
- `/veinminer admin selection blocks allow clear`
- `/veinminer admin selection blocks deny add|remove <block>`
- `/veinminer admin selection blocks deny list [page]`
- `/veinminer admin selection tags allow add|remove <tag-id>`
- `/veinminer admin selection tags allow list [page]`
- `/veinminer admin selection tags deny add|remove <tag-id>`
- `/veinminer admin selection tags deny list [page]`

### BrainageLib commands

- `/servermods help` - prints the help lists for all mods which depend on BrainageLib
- `/servermods config` - (operators only) prints the settings command for each mod which depends on BrainageLib

## Config

Global config is stored in `config/vein_miner.json`.

Per-player config is stored per world in `<world>/vein-miner-players.json`. Removing a block from the global policy does not erase it from player lists - it just remains inactive unless an operator allows it again.

## Compatibility

When [Telekinesis](https://github.com/brainage04/Telekinesis) is installed, drops and experience from both the original block and every connected block go directly to the breaking player. Inventory overflow is dropped at that player's feet. Vein Miner continues to use normal world drops and experience orbs when Telekinesis is absent.

## Documentation

- [Development](docs/DEVELOPMENT.md) - builds and tests
- [Release automation](docs/RELEASE.md) - release workflow
- [Modrinth publishing](docs/MODRINTH.md) - optional Modrinth publishing setup
