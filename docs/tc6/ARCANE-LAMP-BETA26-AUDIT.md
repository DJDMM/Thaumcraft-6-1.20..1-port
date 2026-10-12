# Arcane lamp prerequisite: TC6 BETA26

Authority is the released `Thaumcraft-1.12.2-6.1.BETA26.jar`, SHA256
`9425f8643581b27ff8845b087c8bc6fc10425a32942f1a3f0e265ce6b38f7b5f`,
and source commit `954022bb777b7546281fb36df8522f0ba6b43f81`.
Original `TileLampArcane`, `BlockLamp`, `BlockTCDevice`, `BlockEffect`
and `ConfigRecipes` were read. Released lamp bytecode separately confirms
total world time, six nextInt(16) calls, +/-15 triangular offsets,
precipitation-height+4, Y5, block-light<11 and source-ignored LOS.
This audit records implementation/test definitions, not completed QA.

ARCANELAMP requires completed BASEARTIFICE,16raw Artifice observation
and discovered Lux through the original !lux aspect predicate.
Its shaped arcane recipe costs50vis/Aer1/Ignis1, rows ` I `/`IAI`/` I `,
amber block in the centre and four iron plates. Mirror glass costs50vis/
Aqua1/Ordo1, shapeless quicksilver plus original glass pane equivalents.
Amber compression uses four gemAmber in2x2, not nine; decompression
returns four amber. Modern Forge tags retain addon equivalents.

The lamp faces its support (clicked face opposite), original fixed box
(.25,.125,.25)..(.75,.875,.75), metal sound/hardness1. Discarded original
withProperty returns leave default DOWN/disabled; placement starts disabled,
native onPlace updates enabled to inverse neighboring redstone power.
Enabled emits15, disabled0. Original support check rejects air, not every
non-sturdy block. Support removal drops one lamp and removes its tile.
Powering an enabled lamp/removing it clear glimmers in inclusive -15..15
cube, preserving ordinary terrain. Cleanup may remove another lamp's nearby
glimmers as in the release; no invented per-lamp ownership.

Each total-world-time multiple of5 when unpowered tries one candidate;
each axis is nextInt16-nextInt16. Clamp above precipitation height+4,
then bottom margin. Accept only air/non-glimmer/blocklight<11 and collision
LOS ignoring source's own shape, ignoring fluid ray hits. No reroll/fuel/vis.
Original glimmer is invisible, noncolliding/air/replaceable/unpickable light15,
does not randomly expire and has no player obtainable loot.
The original schedules random ticks whose glimmer callback does nothing;
the port omits this no-op scheduling explicitly, preserving persistent light.

Modern safeguards use getChunkNow for candidate/ray/cleanup with no tickets,
five-block bottom margin minBuildHeight+5 for negative-Y worlds, top/border
bounds. MOTION_BLOCKING height is the modern precipitation equivalent.
Cleanup visits only loaded cells. Clamping can move a high source's target
outside its own cleanup cube, an original consequence retained.

Eight LampGameTests cover glimmer properties, native empty-section storage,
palette serialization/removal, six support placements,
signal/cleanup bounds, removal/ordinary terrain, physical LOS/air/fluid,
bottom/precipitation/no-load bounds, and migration preserving paid native
mirror queue. Prepared chunk readiness precedes each fixed trial.
The shared64/200 migration queue repairs missing/catalogue tiles, refreshes
clients and preserves existing native lamps/mirrors. Actual paid lamp/native
world light are planned in hidden Mirror client; evidence goes in VALIDATION
and artifact report after execution, not inferred from test registration.

Modern storage adaptation: Glimmer must be non-air in the 1.20.1 section
palette, like vanilla LightBlock. Mapped LevelChunk.setBlockState drops a
new isAir state in a hasOnlyAir section; getBlockState also returns ordinary
AIR for sections counted empty. The first full native mirror server exposed
this as missing light outside the lamp's own nonempty section. Properties.of
without air/copyAIR preserves placement, light15, native serialization and
removal. It remains invisible, replaceable, noncolliding, without loot/clone.
The lamp explicitly excludes glimmer as support to preserve the original air
support rule. This is an explicit compatibility adaptation, not original
isAir identity parity. The empty top-section regression places the real block,
checks section occupancy and world lookup/light, writes the native palette,
and removes it back to an empty section.

Финальный срез0.32:1211/1211native server и20/20hidden client прошли;
все20полных PNG и лист реально просмотрены двумя проверяющими.
Подготовленные условия, исходные counters и ограничения — в
[отчёте0.32](../../validation/artifact-report-0.32.json) и VALIDATION.md.
