# Vein Miner

Vein Miner is a server-side Fabric mod that mines connected veins of whitelisted blocks when a player breaks a block while sneaking.

## Configuration

Server configuration is written to `config/vein_miner.json` after the server starts.

Operators can manage the config in game with `/veinminer`:

- `/veinminer toggle`
- `/veinminer vein_size <amount>`
- `/veinminer leaf_decay_speed_multiplier <amount>`
- `/veinminer whitelist add <block>`
- `/veinminer whitelist remove <block>`
- `/veinminer whitelist list`
- `/veinminer load_from_disk`

## Telekinesis compatibility

When [Telekinesis](https://github.com/brainage04/Telekinesis) is installed, drops from both the original block and every connected block mined by Vein Miner go directly into the breaking player's inventory.
Normal loot-table behavior is preserved, and any overflow still spawns in the world.
Vein Miner continues to work normally when Telekinesis is absent.

## Releases

Releases are created from annotated tags named `vX.Y.Z`.
The tag message becomes the GitHub release body and the Modrinth changelog.
