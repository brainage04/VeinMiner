# Development

Run builds and tests through the workspace's `heavy` wrapper:

```shell
heavy status
heavy -- ./gradlew --no-daemon build runAllGameTests
```

`runAllGameTests` includes the Fabric server, Fabric client and NeoForge server suites. Vein Miner has no client-side production code.

The server GameTests live in `common/src/gametest`; `fabric/src/gametest` and `neoforge/src/gametest` only register them, apart from three NeoForge-only tests of another mod refusing or releasing a break. The Fabric client GameTest's film (staging, captions and shots) lives in `fabric/src/recording`.
