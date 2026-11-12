# Development

Build and run every test suite with:

```shell
./gradlew --no-daemon build runAllGameTests
```

`runAllGameTests` includes the Fabric server, Fabric client and NeoForge server suites. Vein Miner has no client-side production code.

The server GameTests live in `common/src/gametest`. `fabric/src/gametest` and `neoforge/src/gametest` register them and supply each loader's stand-in for another mod's break listener. Three NeoForge-only tests cover another mod refusing or releasing the original break. The Fabric client GameTest's film (staging, captions and shots) lives in `fabric/src/recording`. After the film, the client GameTest checks spawn protection on its dedicated server, the only server type that has spawn protection.

CI and release builds take BrainageLib from the repository variable `PREPARE_SIBLINGS` (for example `BrainageLib@<full commit sha>`), which `build.yml` and `release.yml` both read. Both workflows build against the same BrainageLib commit. Leave the variable empty to build against the published BrainageLib.
