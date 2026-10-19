# Infused ordinary tools: Forge 47.4.10 bridge

This is a modern integration layer for Minecraft 1.20.1, not code claimed to
exist in the released Thaumcraft 6 JAR. The original effects inspect the held
stack's infusion data globally. An ordinary Minecraft item cannot acquire a
new Java override when an altar changes its stack NBT.

## Exact boundary

The locally resolved, official-mapped Forge 47.4.10 binary was inspected with
Java 17 `javap -c -p`. `ServerPlayerGameMode.destroyBlock` first waits for
`ForgeHooks.onBlockBreakEvent` to finish, including every listener's veto.
Only an accepted central break reaches the public Forge stack hook:

```text
net.minecraftforge.common.extensions.IForgeItemStack.onBlockStartBreak
(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;)Z
```

`META-INF/coremods.json` registers `coremods/infusion_tools.js`. Its method
transformer prepends one call to the mod's public static method:

```text
thaumcraft.equipment.tools.ToolMining.beforeVanillaBlockStart
(Ljava/lang/Object;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;)Z
```

A true result returns true from the Forge hook. A false result runs its
original method body, which delegates to the actual item's existing hook.
Uninfused stacks and TC tool subclasses take that original path. This keeps
the established TC item override and prevents running its effects twice.
Additional area/far targets still require their own complete native approval.

The transformer checks the exact nonstatic method signature and rejects a
duplicate injected invocation. It loads ASM classes and Forge's `ASMAPI`,
without loading the mod's item/tool classes during transformation. Stack
frames and maximum stack are maintained for the inserted branch. The existing
access transformer is unchanged and no extra runtime dependency is introduced.

## Loader and evidence

The API was checked against the locally resolved official Forge CoreMods
5.2.4 binary, SHA-256
`db32dc040af2eea1216a145a3f5fc6bc45d006740fe696da5a9a445006167c9f`.
Forge's [CoreMods repository](https://github.com/MinecraftForge/CoreMods)
documents its JavaScript/ASM integration; the installed binary, rather than
the repository's current branch, supplies this port's exact API.

The native startup marker is
`THAUMCRAFT_INFUSION_TOOL_BRIDGE_APPLIED`. A marker alone does not establish
working gameplay. The new common GameTests use actual
`ServerPlayerGameMode.destroyBlock` with infused ordinary picks, axes and
shovels, central and neighbor vetoes, physical drops, durability and the TC
subclass bypass. The opt-in client additionally uses ordinary Survival C2S
mining with tools physically paid for at the altar.

Final run tags, pass/failure counts, source fingerprints and packaged resource
and public-method checks belong to `validation/artifact-report-0.40.json`
after QA completes. Development native execution and structural verification
of a reobfuscated JAR are distinct checks; neither is represented as a separate
standalone production installation launch.

## Scope

The Forge interface, its public hook name and the referenced Minecraft class
names are stable between this version's official-mapped development binary
and SRG distribution. The mod's public bridge method retains its own name.
The packaged check must verify both resources and that exact public method
descriptor. This is specific to Forge 47.4.10 / Minecraft 1.20.1, rather than a
claim of compatibility with arbitrary Forge versions or third-party coremods.
Other mods' later attack/use callbacks and arbitrary container area-mining
support remain limited as described in the effects audit.

## Фактически завершённый QA0.40

`infusion-upgrades-040-server3` прошёл1673/1673; `infusion-upgrades-040-client4`24/24, оба native exit0.
Два проверяющих просмотрели все24 полных1600×1000PNG и оба листа.
Фактические платежи, физические предметы, потребители и точные native
observations/SHA — в [artifact-report0.40](../../validation/artifact-report-0.40.json).
Фикстуры, ранние отказы, современные адаптации и пределы —
[VALIDATION.md](../../VALIDATION.md); полного survival/WarpEvents не заявляется.
