# Vein Miner

Vein Miner is a server-side Fabric and NeoForge mod that mines connected ore and tree blocks when a player breaks one eligible block. Vanilla clients can join without installing the mod.

## Requirements

- Minecraft 26.2
- Fabric Loader 0.19.3 or newer with Fabric API, or NeoForge 26.2.0.23-beta or newer
- BrainageLib 1.0.0 or newer
- Java 25 or newer

## Migrating from the Fabric-only release

- Install exactly one Vein Miner JAR matching the server loader: the Fabric JAR requires Fabric API; the NeoForge JAR requires NeoForge. Do not install both variants together.
- Remove the previous Vein Miner JAR before switching loaders. The mod ID remains `vein_miner`, and its `config/vein_miner.json` configuration and world `vein-miner-players.json` player settings retain their existing paths.
- This is server-side on both loaders; vanilla clients do not need Vein Miner or its dependencies. Install the matching loader-specific BrainageLib dependency on the server.
- Repository builds run `./gradlew build` and collect both production loader artifacts in the root `build/libs` directory.

## Behavior

- Mining is enabled server-wide by default.
- Each player also has a persistent personal toggle and activation mode. The default mode is `while_sneaking`.
- The built-in selection policy allows `#vein_miner:ores` and `#vein_miner:trees`. Those tags include `#c:ores` and `#minecraft:logs`, respectively.
- Operators can add exact blocks or block tags and can deny exact blocks or tags. Denials take precedence over allowances.
- A player's optional personal whitelist can only narrow the server policy; it cannot enable a server-denied block.
- Ore, tree, and other blocks have separate traversal limits.
- Adjacency can use faces only, faces and edges, or all 26 neighboring positions.
- Better ore mining joins matching normal and deepslate ore variants. Better tree mining joins logs, wood, stems, and hyphae from the same wood family, including stripped variants.
- Extra blocks use configurable durability and exhaustion costs. Tool protection stops traversal before an extra block would cross the configured remaining-durability floor.
- Fast leaf decay has its own enable switch and multiplier; it is independent of connected tree mining.

Vanilla loot tables, block entities, mining statistics, and tool checks are preserved for every mined block.

## Player commands

`/veinminer` shows the effective global and personal state plus relevant help.

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

## Operator commands

Operator-only controls are under `/veinminer admin`.

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

Tag arguments use identifiers without `#`, for example `c:ores` or `minecraft:logs`.

## Configuration files

Server policy is stored in `config/vein_miner.json`. Changes made through operator commands are saved immediately. Invalid numeric values and malformed identifiers are corrected to bounded defaults during reload, with warnings in the server log.

The legacy `veinSize` setting migrates to all three category limits the first time an older config is loaded.

Persistent player preferences are stored per world in `<world>/vein-miner-players.json`. Removing a block from the global policy does not erase it from player lists; it remains inactive unless an operator allows it again.

## Telekinesis compatibility

When [Telekinesis](https://github.com/brainage04/Telekinesis) is installed, drops and experience from both the original block and every connected block go directly to the breaking player. Inventory overflow is dropped at that player's feet. Vein Miner continues to use normal world drops and experience orbs when Telekinesis is absent.

## Shared server help

Vein Miner registers with BrainageLib's combined first-join notice. Players can run `/servermods help`; operators can also run `/servermods config`.

## Development

```shell
./gradlew test runGameTest
```

Release automation is documented in [docs/RELEASE.md](docs/RELEASE.md). Optional Modrinth publishing is documented in [docs/MODRINTH.md](docs/MODRINTH.md).
