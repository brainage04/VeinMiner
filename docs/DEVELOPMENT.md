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
