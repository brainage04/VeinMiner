This repository contains a server-side only modding template called VeinMiner. Specifications:
- Minecraft version: 1.21.11
- Modloader: Fabric
- Mappings: Mojmap (official Mojang mappings)
- Build system: Gradle
- JDK: Eclipse Temurin 21.0.10

Constraints:
- Keep changes minimal and idiomatic for Fabric.
- Prefer Mojmap names consistent with the project.
- Don’t introduce new dependencies unless necessary.

Definition of done:
- `./gradlew build` passes
- The mod launches and exits gracefully in dev client. Use the following commands on the following OSes:
Linux/macOS: `timeout 1m ./gradlew runClient --no-daemon`
Windows:
```powershell
$proc = Start-Process -PassThru -NoNewWindow -FilePath ".\gradlew.bat" -ArgumentList "runClient","--no-daemon"
if (-not $proc.WaitForExit(60000)) { $proc.Kill($true) }
exit $proc.ExitCode
```
- Briefly summarize what you changed and where.

Proceed autonomously: edit files, run Gradle tasks, fix issues until done.
If something is ambiguous, make a reasonable assumption and state it.

todo:
improve definition of done
deep dive into performance of core vein mining logic (how much more can it be improved?)