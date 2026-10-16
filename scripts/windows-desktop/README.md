# Isolated Windows client QA

The `-SmokeTest Generator` profile uses only `run/generator-smoke` for the
paid TC6 Vis Generator study and25vis bench recipe, native Survival placement,
six output faces, actual Forge Energy pushes and redstone pause/resume.
Its exact-owned-world opt-in Chest FE capability is explicitly a QA receiver
fixture, not vanilla Chest behavior. It requests1600x1000 frames with the same
hidden non-input desktop and exact-JVM checks below. Selecting the profile is
not evidence of a passed run; actual results belong to the versioned report.

The `-SmokeTest Lamps` profile uses only `run/lamps-smoke` for paid TC6
Growth/Fertility research, two native lamp infusions, real crop ticks and
animal breeding. It requests1600x1000 frames with the same hidden non-input
desktop, exact-JVM and unchanged-input-desktop checks below.

The `-SmokeTest Condenser` profile uses only `run/condenser-smoke` for the
paid TC6 cleanup research and recipes, connected lattices, physical transport,
native Flux/Vitium production, filter cleanup and redstone scenes. It requests
1600x1000 captures and retains the exact hidden non-input JVM guarantees below.

From the project, run `scripts/run_client_smoke_isolated.ps1 -SmokeTest EssentiaProduction`
or `-SmokeTest Infusion` / `-SmokeTest TheoryComplete` / `-SmokeTest ThaumonomiconComplete`.
The `-SmokeTest Thaumometer` profile runs native scanning/container/aura checks in `run/thaumometer-smoke` with the same hidden-desktop guarantees.
The `-SmokeTest Rift` profile runs natural rift generation, actual scans/research,
paid infusion/throw/collapse and physical stabilizer checks in `run/rift-smoke`.
The `-SmokeTest Bore` profile uses only `run/bore-smoke` for the Arcane Bore's
paid research/infusion, actual placer, live tool menu and native mining scenes.
Its preparation export is allowlisted separately; it retains the same hidden
desktop, exact-JVM and unchanged-input-desktop requirements below.
The `-SmokeTest Mirror` profile uses only `run/mirror-smoke` for the paid
lamp/mirror chain, native bindings and item/hand/essentia transfer, actual light
and six mounted orientations. It uses the same separately allowlisted exact
Java17 JVM and never activates or switches the input desktop.
The old `run_theory_smoke_isolated.ps1` forwards to the TheoryComplete profile.
The wrapper prepares the selected JavaExec task without executing its game launch
action, writes a Java17 argument file, and launches the final JVM on a uniquely
named non-input Win32 desktop. The desktop is chosen in `CreateProcessW` before
Minecraft creates a window. The helper retains its desktop handle until that
exact JVM exits. No desktop switch, window activation or global input is used.

The wrapper waits for completion and requires a successful preparation build,
the capture success marker, direct JVM exit0, a positive count of windows
owned by that JVM on the named desktop, and the client smoke/renderer-audit
success markers without a smoke failure marker. Logs alone do not prove visual QA;
review the smoke screenshots and assertions separately.

`-PrepareOnly` performs preparation/export without launching Minecraft. Optional
`-RunTag` selects a unique evidence prefix; existing evidence paths are rejected.
`-JavaHome` defaults to the existing Adoptium Java17 installation. The actual
client executable comes from Gradle's selected Java launcher and is checked as
Java17. The init script is pinned to Gradle8.8/ForgeGradle6.0.54; unexpected APIs
fail before the replaced run action can execute.

All capture JSON, argument files, environment overrides, preparation/client logs,
desktop status and run summary files go to the project's ignored `validation/`
directory with a fresh run prefix. Only environment differences from the Gradle
process's inherited environment are exported; inherited values are not dumped.
Differences are applied to the child environment block without changing the
caller's environment. The Java argument-file generator quotes every original
argument, escapes backslashes/quotes, writes no BOM and rejects non-ASCII
arguments pending local Java17 code-page verification.

Only the selected `run/theory-complete-smoke/options.txt` or
`run/infusion-smoke/options.txt` / `run/essentia-production-smoke/options.txt` / `run/thaumonomicon-complete-smoke/options.txt` receives these fixture settings:

- `soundCategory_master:0.0`
- `fullscreen:false`
- `maxFps:30`

Other options are preserved. Other run directories and manual gameplay saves
are not inspected or modified. The helper uses `NUL` standard input and a file
for stdout/stderr. `EnumDesktopWindows` and `GetWindowThreadProcessId` provide
read-only proof of window placement; they never activate or move a window.

This Win32 desktop shares CPU, RAM and GPU with the user's game. It is not a
Task View virtual desktop, a VM or a security sandbox. If GLFW/WGL initialization
fails, inspect the log and stop; switching desktops is not a fallback. Do not
use Computer Use inputs because they activate their target. The selected smoke
hooks supply automated interaction and screenshot capture instead.

The three helpers are intentionally separated:

The Infernal profile uses only `run/infernal-smoke`, property
`thaumcraft.infernalSmokeTest` and fresh `infernal-client-<RunTag>` evidence.
Its native interactions and captures run on the same verified non-input desktop;
ordinary gameplay saves and global keyboard/mouse input remain untouched.

- `capture-javaexec.init.gradle`: ForgeGradle preparation and final command dump.
- `New-JavaArgumentFile.ps1`: safe Java argument-file and environment delta export.
- `Start-IsolatedJava.ps1`: native named-desktop launch and exact-JVM lifetime.

# Pattern Crafter and Dioptra

`run_client_smoke_isolated.ps1 -SmokeTest Pattern` prepares the opt-in
`patternSmokeTest` JVM under `run/pattern-smoke` on the same verified hidden,
non-input desktop. Actual pass/fail and reviewed captures belong to the
versioned validation report; selecting this profile alone is not proof.

# Grapple Gun

`run_client_smoke_isolated.ps1 -SmokeTest Grapple` prepares the opt-in
`grappleSmokeTest` JVM under `run/grapple-smoke` on the same verified hidden,
non-input desktop. The driver operates only its owned QA world; actual native
pass/fail, source fingerprints and reviewed captures belong to the versioned
validation report. Selecting this profile alone is not evidence of completion.
