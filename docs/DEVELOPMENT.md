# Development

Run builds and tests through the workspace's `heavy` wrapper:

```shell
heavy status
heavy -- ./gradlew --no-daemon build runAllGameTests -Pfabricmoddingconventions.devClientOptions=/nonexistent
```

`runAllGameTests` includes the Fabric server, Fabric client and NeoForge server suites. Vein Miner has no client-side production code.

The legacy `veinSize` setting migrates to all three category limits the first time an older config is loaded.
