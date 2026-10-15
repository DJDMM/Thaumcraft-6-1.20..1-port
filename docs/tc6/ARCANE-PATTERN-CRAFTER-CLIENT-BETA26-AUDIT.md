# Arcane Pattern Crafter and Dioptra client audit — released BETA26

Client scope0.36.0,2026-10-15. This document records implementation and intended
verification. No native client or screenshot pass is claimed before final evidence
is recorded in VALIDATION.md and the0.36 artifact report.

## Primary authority

Released `Thaumcraft-1.12.2-6.1.BETA26.jar`,SHA256
`9425f8643581b27ff8845b087c8bc6fc10425a32942f1a3f0e265ce6b38f7b5f`,
and the pinned reference commit`954022bb777b7546281fb36df8522f0ba6b43f81`.
Both original tile renderers were inspected with Java17`javap -c -p`; original
Java is used only where it agrees with released bytecode. Relevant classes:

- `client/renderers/tile/TilePatternCrafterRenderer`,
  `common/tiles/crafting/TilePatternCrafter`,
  `common/blocks/crafting/BlockPatternCrafter`.
- `client/renderers/tile/TileDioptraRenderer`,
  `common/tiles/devices/TileDioptra`,
  `common/blocks/devices/BlockDioptra`.
- `client/lib/UtilsFX` and`client/lib/TexturedQuadTC` supply UVs, geometry,
  color rounding, vertex ordering and alpha quantization.

No TC4/5 pattern interface or invented menu is introduced. The original physical
knob cycles ten dies. The Dioptra toggles Vis/Flux when used. There is no wrench,
ghost-slot menu, developer overlay, status label or added player message.

## Pattern hardware and original events

Native world rendering consumes the actual tile`type`and`rot`. It uses the
original center(.5,.75,.5),EAST90/WEST270/NORTH180/SOUTH0 yaw, selector Z90
rotation, half-block front offset and .5-wide die. Original10×1 UVs are retained,
including integer V repetition. A .0001-block depth offset avoids modern z-fighting.
The two gears remain at X±.2,Y-.40625,Z.05,scale(.5,.5,1),with opposite
`-rot%360`/`rot%360` rotations. Source rotation is discrete; no partial-tick
interpolation is invented. The unused source ModelBoreBase allocation is not drawn.
The16×16 sprite extrusion includes .1 thickness and original half-texel side UVs.

The common native tile retains client event1→rotTicks10,original rp acceleration,
.8 decay,rot accumulation and clack cadence/volume. Animation is triggered by the
actual server crafting event. The renderer does not synthesize this event or reset
tile state. Pattern items keep their original builtin/entity display transforms;
the native item extension renders the baked body plus neutral mode0/rotation0.
The historical catalogue preview delegates to the same neutral hardware helper.

## Dioptra graph

The native tile supplies169 actual cached chunk samples. Rendering builds exactly
144 top quads,48 fading edge quads and four translucent side screens. Heights use
signed byte/96,original .99 XZ scale and top Y1.001. Native production sanitizes
external grid data to0..64; the renderer neither samples nor modifies aura.
Original Vis/Flux time colors,red×.8 top tint,rounded Java Color bytes, .9 top
alpha,edge0→.9 fades, .8 side alpha and integer brightness`200+sin*15`/220
are preserved. Original client counter reset is retained by the common tile.

Modern render state uses SRC_ALPHA/ONE,ordinary depth test and depth writes,
culling for correctly flipped graph surfaces and no culling for side screens.
The native ordinary text shader provides original .1 alpha cutoff,world fog and
lightmap; the see-through text shader is not used. No fabricated beam or new
shader artwork is added. Original enlarged block-entity bounds cover the graph.

These four texture payloads match released JAR bytes exactly:

| Resource | SHA256 |
| --- | --- |
| textures/blocks/pattern_crafter_modes.png | 5c9e49ca27b4e1adf2236fcb23cc9f4f8185fdc776fa765bf842523c6713c117 |
| textures/misc/gear_brass.png | c392b7d86e050412987a88c0579ab41aa8a4571a897ccfdea85b5bafc458b211 |
| textures/misc/gridblock.png | a07750c21fccfe6e14ccfd3c21c2d6671c89b355467c71de25c03a045b3bbe90 |
| textures/models/dioptra_side.png | 7ab7b5604c0dfef5666e448b57565ce4cf4d2e9a12661edbbf227fbbb6914413 |

The latter two original PNGs were absent from the old visual catalogue and are
copied unchanged. Original media census therefore increases from668 to670 PNG.

## Opt-in native client contract

Property`thaumcraft.patternSmokeTest`,profile`Pattern`,working directory
`run/pattern-smoke`,prefix`tc6-pattern-`. The root alone runs the verified hidden
non-input desktop launcher; this driver never switches desktops,activates windows
or sends global input. Its native C2S actions belong only to its new owned world.
The normal mod does not enable the driver or show its technical log text.

The bounded23-scene list is authoritative in`PatternClientSmokeTest.IMAGES`:
three native Thaumometer aspect scans,paid Dioptra study/bench,Survival Dioptra
placement with actual Vis graph,native toggle to Flux,paid Pattern study/bench,
Survival Pattern placement,ten actual die views,redstone pause/resume and both
devices' native Survival teardown.

Payment assertions preserve scan-earned observation: before/after knowledge
balances must debit exactly16 raw AUROMANCY observation and32 raw theory in each
of GOLEMANCY/ARTIFICE/AUROMANCY. Both studies earn10 XP. Both physical bench
outputs must consume50 Vis each and their original ingredients/crystals:
Aer1/Aqua1 for Dioptra,Terra1/Aqua1/Ordo1 for Pattern. The actual paid BlockItems
are then placed; no target study,device,aspect or output is supplied directly.

Source chest ingredients enter through native C2S chest quick-move. Nine original
die shapes have ordinary vanilla examples:hay,planks,pressure plate,sticks,
crafting table,slabs,trapdoors,doors and chest. Vertical-three planks have no native
vanilla recipe; its three paid raw inputs and stored power must remain unchanged
for80 actual world ticks. The three preserved planks carry into the next six-item
recipe with only three further supplied planks. No artificial QA recipe is added.
The resumed ring recipe produces a second chest,for ten actual crafts and24
physical output items. A server END observer records native five-Vis refills and
one-power debits without changing consumer state. Idle refill after reaching zero
is allowed; the resume payment is observed on its actual tick rather than requiring
its transient balance to persist. Redstone must preserve input8,output and power
for80 world ticks before actual Survival redstone-block harvesting resumes work.

Explicit fixtures are BASEARTIFICE and earlier predecessor studies,raw knowledge,
typed aspect crystals,recipe materials,tools,bench,empty chests,owned bedrock arena
and varied initial aura in30 loaded chunks before either consumer exists. Original
resources may exist in other naturally loaded chunks. No active consumer count,
power,type,grid,RNG or aura is reset; no tile tick or craft method is called.
Preparing these fixtures does not prove an uninterrupted survival playthrough.

Successful final evidence must include all23 fresh1600×1000 PNGs,actual reviews
of every full frame and both sheets by two reviewers,hidden owned JVM native exit0,
`THAUMCRAFT_PATTERN_RENDER_AUDIT_OK` and
`THAUMCRAFT_PATTERN_CLIENT_SMOKE_OK: 23 scenes`. No multiplayer,client reload,
audio capture,all-angle or fully acquired survival claim follows from this route.

## Фактически завершённый QA0.36

`pattern-036-server4` прошёл1488/1488; `pattern-036-client1`23/23, оба nativeexit0.
Два проверяющих просмотрели все23полных1600×1000PNG и оба листа.
Настоящие3scan/2study/2bench/100vis5crystals/96Theory16Observation20XP,
nativeDioptraVisFlux,10dies/10crafts/24outputs, 80тиков паузы,
продолжение и физическая разборка связаны с финальными logs/SHA.
Фикстуры, source quirks, современное loaded-only/владение, ранние попытки
и пределы доказательства сохранены в [VALIDATION.md](../../VALIDATION.md)
и [artifact-report0.36](../../validation/artifact-report-0.36.json).
