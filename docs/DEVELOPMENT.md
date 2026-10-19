# Development

Run builds and tests through the workspace's `heavy` wrapper:

```shell
heavy status
heavy -- ./gradlew --no-daemon build runAllGameTests -Pfabricmoddingconventions.devClientOptions=/nonexistent
```

`runAllGameTests` includes the Fabric server, Fabric client and NeoForge server suites. Vein Miner has no client-side production code.

The legacy `veinSize` setting migrates to all three category limits the first time an older config is loaded.

## Test selection

Keep tests of observable production behaviour and distinct loader integrations. Remove a redundant test only when a named test on the same loader covers all of its assertions. Remove tests that only echo constants, mocks or test-runner wiring. Cross-loader counterparts are not duplicates.

The October 2026 review removed the Fabric environment/metadata checks and the default-tag literal check. NeoForge's `connectedOreVeinMinesOnNeoForge` was covered by `connectedOreVeinDropsRespectTelekinesisCompatibility`, which also checks the drops.

## Coverage

Shared server scenarios cover every activation mode, both mining switches, incorrect tools, personal command persistence, empty personal filters, denied selections, admin permissions, category-limit validation and config reload on Fabric and NeoForge. Existing tests cover drops, ore/tree families, adjacency, costs, tool protection and the shared help registry; the Fabric client scenario checks connected mining through a dedicated-server session.

Remaining gaps include client-side presentation on NeoForge, third-party tool callbacks, full inventories and experience overflow with Telekinesis installed, custom datapack families, selection pagination/size limits and crash-time filesystem failures. There are no mod gamerules or production client rendering hooks.

Queued leaf decay is discarded when fast decay is disabled or its multiplier returns to one. Due leaves are left intact when random ticks are disabled or the leaves are no longer decaying. Shutdown clears both leaf work and pending global-whitelist confirmations. Shared synchronous regressions exercise these cases on both loaders, with a forced one-tick leaf delay and restored config/gamerules.
