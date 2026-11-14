# Development

Build and run every test suite with:

```shell
./gradlew --no-daemon build runAllGameTests
```

`runAllGameTests` includes the Fabric server, Fabric client and NeoForge server suites. Vein Miner has no client-side production code.

The server GameTests live in `common/src/gametest`. `fabric/src/gametest` and `neoforge/src/gametest` register them and supply each loader's stand-in for another mod's break listener. Three NeoForge-only tests cover another mod refusing or releasing the original break. The Fabric client GameTest's film (staging, captions and shots) lives in `fabric/src/recording`. After the film, the client GameTest checks spawn protection on its dedicated server, the only server type that has spawn protection.

`:fabric:test` runs the unit tests. `VeinSearchTest` checks the vein search in `VeinSearch` with examples and jqwik properties: a vein never holds more blocks than its cap, never asks for a block whose 3x3 chunk area is not loaded, and is the whole connected region when the cap leaves room. `:fabric:pitest` runs mutation tests on `VeinSearch` against them; it is not part of `check`, and the report is written to `fabric/build/reports/pitest`.

BrainageLib 2.0.0 and its test fixtures (`brainagelib-gametest`) are not on Maven Central. CI clones BrainageLib beside the checkout, as `../BrainageLib`, and publishes it to its local repository: `build.yml` at commit `d783aa0a761aa6523fbf3ed291d7b5ca288fbffc`, `release.yml` at tag `v2.0.0`. A local build needs the same:

```shell
git clone --branch v2.0.0 https://github.com/brainage04/BrainageLib.git ../BrainageLib
../BrainageLib/gradlew -p ../BrainageLib publishAllPublicationsToLocalRepository
```
