# Flux Condenser: original client and isolated QA contract

Primary authority is the released Thaumcraft 6.1.BETA26 JAR, SHA-256
`9425f8643581b27ff8845b087c8bc6fc10425a32942f1a3f0e265ce6b38f7b5f`.
Readable source is pinned at `954022bb777b7546281fb36df8522f0ba6b43f81`.
`TileCondenser.update`, `FXDispatcher.spark`, `FXGeneric` and
`ParticleEngine` were checked against released bytecode where necessary.
No TC4, TC5, addon mechanics or AI textures are used.

## Original baked models

The original condenser has eight cuboids, the lattice arm two, and each
clean/dirty lattice core one. The existing port's four block model files
retain identical elements, UV coordinates, textures and ambient-occlusion
settings. Six multipart arms use the original NORTH/SOUTH/EAST/WEST/UP/DOWN
rotations. The dirty core uses different UVs from the same lattice PNG.
Enabled/disabled condenser models use `condenser.png`/`condenser_off.png`.
There is no original condenser TESR, special inventory animation or GUI.

The three block PNGs and original `textures/misc/particles.png` were checked
byte-for-byte against the released JAR. Static item models remain original.
Native block states and physical world topology select connected arms.

## Original sparks

An enabled physical client tile emits only when its calculated lattice
weight is positive, synced input essentia is positive, a clean lattice
exists, and its transient tick counter is divisible by
`max(3, interval / 50)`. One clean lattice is selected randomly. Its center
receives one spark of scale `4.5 + randomFloat`, independent RGB components
`.33 + randomFloat * .66`, and constant alpha `.8`.

`FXDispatcher.spark` uses lifetime `5 + nextInt(5)`, grid16, eight animation
frames starting at `8 + nextInt(3) * 16`, fixed scale and random horizontal
flip. Frame selection is `start + int(min(8 * age / lifetime, 7))`.
Velocity, gravity and rotation are zero. FXGeneric's three-argument RGB
setter also sets its destination RGB to that same color: this spark does
**not** fade to black. Its single alpha/scale keys also remain constant.

ParticleEngine layer0 is SRC_ALPHA/ONE, with depth test and no depth writes.
The modern particle uses the original 1024×1024 sheet and native light,
camera orientation, lifecycle and render-state restoration. No new sound,
server payment or aura mutation occurs during particle rendering.
The MOD-registered `condenser_spark` shader preserves released
`GL_GREATER, 1/255` alpha acceptance, rather than the vanilla text shader's
0.1 cutoff. Its shader JSON explicitly selects SRC_ALPHA/ONE, matching the
RenderType blend state, so shader application cannot restore ordinary
translucent blending. Native lightmap and fog, LEQUAL depth testing and no
depth writes remain. This corrects faint original sprite pixels without
changing emission cadence, original atlas, particle color, size or RNG.

Client discovery inspects only already-loaded chunks; cached tiles are
discarded when detached, removed, unloaded or replaced. World changes clear
particles and counters. A bounded4096 particle list is a modern guard.
Reservoir sync preserves the client transient tick counter instead of
resetting it every time a five-tick input update arrives.

Modern `LevelChunk.setBlockState` bypasses `Block.onPlace` on the client.
Actual isolated client2 diagnostics found all four correct physical lattice
states and a ticking, synced condenser (`ticks162`, `essentia19`) while its
initial empty topology stayed at weight0/interval0/clean-empty and no sparks
were emitted. The released lattice `processUpdate` directly invalidates its
condenser's `latticeCount`; there is no special original condenser client
block-event override to copy. As an explicit modern visual adaptation, the
existing20-tick loaded discovery reruns the unchanged original topology DFS
for each current live client tile before spark eligibility. Placement,
clean/dirty changes, disconnection and chunk updates therefore reach visual
topology within20 client ticks without changing either reservoir or the
transient tick count. The cache also drops tiles outside the current discovery
radius of three chunks on each horizontal axis; walking does not accumulate
an unbounded trailing tile cache. The observer neither mutates physical
blocks nor invokes server input, production, aura, reward or paid callbacks.

## Prepared native client scenario

Opt-in `thaumcraft.condenserSmokeTest` is restricted to Dist.CLIENT and the
owned `run/condenser-smoke` world. Fourteen actual1600×1000 captures use the
`tc6-condenser-` prefix:

1. Native Thaumometer C2S scan of a physical FluxRift, earning the original
   `f_toomuchflux` and `!FluxRift` facts.
2. Paid FLUXCLEANUP through ordinary research requests: Observation32
   Auromancy/16 Artifice/16 Basics, Theory64 Auromancy, one physical phial
   containing10 Vitium, consumed without returning an empty phial; +10XP.
3. Four real filter crafts:60vis/Aqua4, yielding eight filters.
4. Four real lattice crafts:400vis/Terra12/Aer12, consuming four of those
   paid filters.
5. One real condenser craft:500vis/Aer5/Aqua5/Perditio5. All nine crafts
   cost960vis and43 crystals total. Finished devices are not gifted.
6. Actual Survival BlockItem placement of the paid condenser, empty stores.
7. Four Survival lattice placements: original vertical weighted3.10,
   interval554 and cost6, with physical connected down/up arms. Visual
   connectivity includes the condenser beneath the first lattice; the
   source's weighted side count only includes clean/dirty lattice blocks,
   so the first lattice contributes.85 rather than.70. The root contributes
   another.85, two intermediate lattices.70 each, and the leaf contributes0.
8. Native jar/tube input and live original sparks, without direct pull,
   particle emission, queue or tile-tick calls from the driver.
9. Actual aura Flux debit and typed output through the bottom tube to a jar:
   six paid input essentia and one aura Flux produce one Vitium. The existing
   physical scanner is reused for a read-only aura meter while native
   Goggles show the jar's synchronized contents. Goggles are an explicit raw
   equipment fixture equipped only after all nine paid bench crafts, so
   their discount cannot reduce the960vis payment. No Goggles craft is
   claimed, no jar scan awards facts, and no overlay is painted by QA.
10. Actual Vitium-source intake causes native lattice clogging. Its held
    Vitium unit is not consumed by the original dirty-making branch. A real
    pre-close observation verifies9 Vitium in the source jar and1 in the
    open tube. A real caster request closes the physical tube arm before
    cleanup. After that closure, original jar suction32 may pull the unit
    back from tube suction31: either9+1 or10+0 must conserve all10 Vitium,
    with matching aspects and the EAST arm closed. One physical
    redstone block is then placed through C2S before filters are applied, so
    ordinary later production trials cannot introduce another random clog
    during the finite paid-cleanup sequence. No random branch is forced.
11. Original dirty-filter interaction consumes one paid filter per cleaned
    lattice. The unforced50% crystal branch may produce zero; actual world
    and inventory ownership is observed, never guaranteed or invented.
12. That physical redstone block and620 actual world ticks prove production,
    input payment and aura debit pause. Existing output may still extract.
13. Survival redstone harvest resumes production through real world ticks.
14. Survival condenser harvest detaches the network; exactly one paid core
    and four paid lattice drops remain physically owned.

The Rift entity, its seed0x54433634/size10/stability100 and arena are explicit target
fixtures; the scan itself uses the native protocol. This does not prove
natural rift generation. Only the owned fixture Rift is removed after the
scan. Predecessor research, raw knowledge, recipe ingredients/crystals,
raw native bench, loaded arena/temporary high-placement footing,
Aer80 and Vitium10 source jars, transport tubes and empty output jar are
declared fixtures. All25 initially loaded aura chunks receive2500vis and
10Flux at fixed lunar phase0 **before** any target device is placed or
crafted. The fixture Rift may consume its own aura through ordinary ticks.
After its actual scan/removal and the paid bench sequence, uniform Flux10
is restored once before the first paid condenser placement, with no target
consumer present. The bench's paid vis budget is retained. No production
reservoir, timer, aura or output is reset after the condenser exists.
Filter ingredients in lattice recipes are taken from paid craft outputs.
The temporary owned footing is exactly the column x3/z3/y112..115.
Before a different interaction height or return to the ordinary capture
camera, the driver removes only still-bedrock cells of that raw column,
then rebuilds the requested standing height. It never changes target blocks,
paid stacks, timers, reservoirs, aura or RNG during this fixture cleanup.
Actual client3 visual review found its earlier retained tall column obscured
the lattice/input frames and obstructed the lower filter interaction footing;
those partial captures are failed-run evidence, not delivery screenshots.

The driver observes source jars plus in-flight native tube units, input
store, output store/tube/jar and aura to check conservation. Waits are
bounded and report physical state; a failed trial is not retried as success.
Before the live-spark capture it also waits for the actual client tile's
four-node clean topology, weight3.10, interval554 and cost6. Frozen clogged
and cleaned captures require its cached clean list to equal the physical
native clean lattice states. Teardown waits for at least nine quiet client
ticks and expiry of remaining native sparks while requiring no new emission
from the removed condenser. These assertions only read native state; the
driver never invokes topology invalidation, `triggerCheck` or an emitter.
The original atlas is byte-identical to the released JAR (SHA-256
`3c31a3bed7a007ef4d671745bd627112c79d1ac70049e23194a1454929f17000`).
Its grid16 frames30/31 are fully transparent, so active/drawn counters alone
do not prove a visible spark in a selected photograph. The opt-in driver
only reads the existing private particle list and native age/lifetime/frame
fields; a spark capture waits for a naturally spawned alpha-bearing middle
frame and logs the actual frame/age/source. It changes no particle, emitter,
RNG, color, size, lifetime or tick. Each final full PNG still requires actual
visual review; a logged draw is not substituted for a visible photograph.
Actual client4 failed an over-specific post-close9+1 assertion. Its saved
owned chunk contained the original valid10-in-jar/0-in-tube backflow state;
`condenser-034-client4-clog-state.json` preserves the exact observation and
released suction proof. The corrected assertion accepts only those two
physically conserved distributions and records actual final quantities.
Native client QA uses only the verified hidden non-input Win32 desktop
launcher. No desktop activation, global input or manual-save alteration is
allowed. Screenshots retain ordinary gameplay interfaces and original
research text; technical labels appear only in opt-in logs and this document.

This is an implementation and fixture contract, not passed evidence.
Final exit codes, exact actual counts, hashes and two actual visual reviews
are recorded after completion in VALIDATION and the artifact report.
Full survival progression, multiplayer, client reload, all angles and audio
are outside this prepared client proof; server tests cover saved reservoirs.

## Фактически завершённый QA0.34

condenser-034-server7 1369/1369 и condenser-034-client514/14, оба nativeexit0. Два проверяющих
просмотрели все14полных1600×1000PNG и оба листа. Финальные fingerprints,
реальные наблюдения, fixtures/ранние failures/ограничения сохранены в
[VALIDATION.md](../../VALIDATION.md) и
[artifact-report0.34](../../validation/artifact-report-0.34.json).
