# Growth and Fertility lamps: BETA26 client contract

Primary authority is released `Thaumcraft-1.12.2-6.1.BETA26.jar`, SHA-256
`9425f8643581b27ff8845b087c8bc6fc10425a32942f1a3f0e265ce6b38f7b5f`.
Readable source is pinned at `954022bb777b7546281fb36df8522f0ba6b43f81`.
`TileLampGrowth`, `TileLampFertility`, `PacketFXBlockMist`,
`FXDispatcher.drawBlockMistParticles`, `FXGeneric` and the released lamp
models form the reference. Released tile and packet bytecode independently
confirms color4259648, radius32, the delayed prior-plant-state test and the
fertility eligible300-tick clock. This document describes implemented
contracts and planned QA; it does not establish a passed native run.

Both lamps use their existing original baked blocks/items. There is no
original TESR, moving lamp body, ray, special GUI or persistent aura halo.
The six support-facing variants and enabled/off texture variants remain
native block states. Enabled emits15 and disabled0. Core server tests
cover all six supports; this client scenario uses real DOWN placement and
does not claim six-angle photographic coverage.

The four growth/fertility lamp PNGs, both animated-on `.mcmeta` files and
particle atlas were compared byte-for-byte with the released JAR. All
seven match; the shared atlas SHA-256 is
`3c31a3bed7a007ef4d671745bd627112c79d1ac70049e23194a1454929f17000`.

Growth tests the previously selected physical plant on its next active
update. Only a changed block/state sends the original green block-mist
packet; selecting a plant or merely paying Herba is not an effect trigger.
The original nearest-player32 gate and same-dimension strict32-radius
recipient distribution are retained. The modern packet includes its
dimension and is receive-only, so no client can request a plant change or
payment. Its common packet class resides beside client classes but imports
no client types; `DistExecutor` isolates the actual client effect handler.

Each native packet creates eight puffs inside the current physical block's
bounding box, color4259648 (`#40ff40`). Motion is Gaussian.01 horizontally
and randomFloat*.075 vertically; lifetime50..74. Original grid16/frame56
and the byte-identical `textures/misc/particles.png` are reused. The alpha
keys are0,.5,.4,.3,.2,.1,0 and scale5->1. Gravity.1 gives.004 downward
acceleration per tick; slowdown1, lunar wind.001 and original signed
one-degree rotation conversion remain. RGB stays constant, as the original
three-argument setter sets destination RGB too. Collision, loaded-world
light and camera orientation use native1.20 APIs. The existing original
ParticleEngine-layer0 shader retains SRC_ALPHA/ONE, GL_GREATER1/255,
depth test and no depth writes. Native rendering does not pay fuel, choose
crops or change server state. The4096 active-particle bound and world-change
cleanup are explicit modern guards.

Fertility has no custom lamp particles. The original lamp puts a qualifying
pair into ordinary animal love; native animal event18 makes hearts. Actual
vanilla breeding AI subsequently makes the child. An instantaneous child,
forced AI tick or invented magic beam would not preserve that route.

## Owned hidden-client scenario

Opt-in `thaumcraft.lampsSmokeTest` is Dist.CLIENT only and creates an owned
Survival world under `run/lamps-smoke`. Its15 planned actual1600x1000 PNGs
use prefix `tc6-lamps-`:

1. Actual Thaumometer offhand C2S scan of one raw typed Herba crystal.
2. The same original route discovers Desiderium from a raw typed crystal.
3. Paid LAMPGROWTH:32 raw Artifice Theory, one physical bone meal,+10XP.
4. Paid LAMPFERTILITY:32 raw Artifice Theory, wheat and carrot,+10XP.
5. Two native bench result clicks:100vis/Aer2/Ignis2 and real original
   components yield two Arcane Lamps, preserved for central ingredients.
6. Ordinary matrix ticks consume a paid central Arcane Lamp, six original
   peripherals and65 typed essence, producing one Growth Lamp.
7. The second paid central lamp/six peripherals/65 essence produce Fertility.
8. Survival BlockItem payment places the Growth Lamp on its physical tube.
9. Native Herba jar/tube draw, actual delayed scheduled crop state progress,
   and original green mist. No direct driver bonemeal/grow/tick/emit call.
10. Survival BlockItem payment places Fertility on its physical tube.
11. Native Desiderium draw puts two physical adult cows into love; an
    unchanged Netty observer counts real animal event18 packets.
12. Actual vanilla BreedGoal produces one baby. Source jar+in-flight tube+
    remaining charges must account for exactly five Desiderium for the pair,
    preserving the original possibility of negative transient charges.
13. Two native C2S redstone blocks pause work. Sixty actual world ticks first
    settle already scheduled growth/refill; the subsequent observation spans
    at least320 actual world ticks. Crops and fertility active count freeze;
    original powered refilling remains permitted.
14. Survival harvest of Growth's redstone resumes its existing counter,
    crop progress and new native mist without resetting state.
15. Survival harvest conserves exactly one physical paid item for each lamp.

Explicit raw fixtures are the loaded bedrock arena, bench, native altar,
six pedestals,32 symmetric candles,148 typed altar essence (130 paid,18
remaining), predecessor ARCANELAMP/INFUSION research, raw Theory and study
materials, recipe materials/crystals,2500vis in each prepared loaded chunk,
80 age0 wheat on hydrated farmland, two adult cows with full native AI,
fence, two typed40-unit source jars and their physical tubes, two redstone
blocks and ordinary pickaxes for native interactions. The two final
lamps, their central lamps, target studies/aspects, love and child are never
supplied. RandomTickSpeed0 is a declared fixture: crop progress must follow
the lamp's original scheduled callback, using the explicit modern one-tick
crop randomTick adapter, rather than ambient random crop ticks. The driver
never calls that adapter, force-ticks a tile/animal, resets a queue, charges,
reserve, count, delay, postplacement aura or operation RNG. It uses native
held-slot packets and conservative stow/take operations for paid items.
No overlay or developer label is painted into ordinary gameplay.

These prepared scenes are not proof of a complete Survival acquisition
path, natural animal spawning, multiplayer, reload, every orientation or
audio. Native pass/fail logs, fingerprints, exact final captures and actual
two-reviewer observations belong in VALIDATION/artifact report only after
the root runs and reviews them. No final0.35 result is claimed here.

## Фактически завершённый QA0.35

`lamps-035-server3` прошёл1422/1422; `lamps-035-client2`15/15, оба nativeexit0.
Два проверяющих просмотрели все15полных1600×1000PNG и оба листа.
Реальные130эссенции/100vis/4кристалла/64rawTheory/20XP, 320тиков паузы,
native рост при randomTickSpeed0, сердца/AI-рождение и физическая разборка
связаны с финальными logs/SHA. Подготовленные условия, принятый modern1tick
bridge, source quirks, ранние попытки и ограничения сохранены в
[VALIDATION.md](../../VALIDATION.md) и
[artifact-report0.35](../../validation/artifact-report-0.35.json).
