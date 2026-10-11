# Arcane Bore: released client presentation and isolated QA

Authority is Thaumcraft **6.1.BETA26** for Minecraft1.12.2, original JAR
SHA256 `9425f8643581b27ff8845b087c8bc6fc10425a32942f1a3f0e265ce6b38f7b5f`
and source commit `954022bb777b7546281fb36df8522f0ba6b43f81`.
This document specifies the client implementation and fixtures; it does not
declare a completed client run. Actual evidence is recorded separately.
`javap -c -p` against the released JAR independently confirms175/232,
the original English property strings and colours,0.5scale, source beam
length5,0.4opacity and no-depth-write operations used below.

## Original GUI

`GuiArcaneBore` uses `gui_arcanebore.png`, image175×232 and no foreground
title. The one pickaxe is at80,29; main inventory starts8,84 with3×9slots,
hotbar8,142. There is no fuel inventory. The original empty lower parchment
is retained. The health strip is39×6 at68,59, UV192,48; damage+1>=maximum
shows the16×16 crossed-tool overlay at80,29, UV240,0.

At124,18,Z505,scale0.5 the released English text shows Width1+radius×2,
Depth, Speed+stone digging speed and, when applicable, Other properties,
Refining/Fortune/Silk Touch with original ordering, offsets and colours.
These are original player statistics, not development labels. `BoreScreen`
reads the actual synchronized owned construct and native menu slot. The
port bounds the native menu by alive owner/world/distance/window context;
the release's unbounded `canInteractWith=true` is not copied.

## Original model, animation and beam

`ModelArcaneBore` has11cuboids and64×32UVs. Five opaque tripod parts render
first (`leg2`, `tripod`, `leg3`, `leg4`, `leg1`). The six-cuboid base/head
tree renders separately with source-alpha blending. Tripod world body yaw
is0; only the base aims with yaw/pitch. `LegacyArcaneBoreModel` retains
original geometry/pivots/UVs, while `BoreRenderers` uses a native translucent
head layer and the original0.5shadow.

`RenderArcaneBore` draws only for `clientDigging && isActive && validInventory`.
The muzzle starts(0.5,0.075,0), uses the original clockwise Z rotation at
pitch and Y rotation at−(headYaw+90), then adds eye height. The node is the
original32×32atlas frame96+age%32, size0.5, green(0,1,0.4),opacity0.8,
light210. The source beam deliberately stays **five blocks long**, even if
the selected block is further away. Three panels rotate60degrees each,
half-width0.15, green(0,1,0.4),opacity0.4/light200, original UV scroll and
five-degrees-per-day-time-tick spin (`WorldProvider.getWorldTime` becomes
native `getDayTime`, not total `getGameTime`). Native state objects preserve SRC_ALPHA/ONE,
depth test, no culling and no depth writes, restoring state afterwards.
The beam is a visual indicator; it neither selects nor harvests a block.

`PacketFXBoreDig` schedules `delay` emissions from a captured non-air block,
each with loop `i < 50F/delay`, one-quarter sparkle versus three-quarter
original block-sprite debris. The port captures each synchronized target
epoch/state/delay and uses the same countdown/ratio, original texture UVs,
colours, scale/lifetime and attraction/damping/noise. Sparkle initial lifetime
uses the eye, debris uses the feet before subsequent attraction uses the eye,
matching the original constructors/setTarget order. Removal/world change
clears transient schedules; malformed-scene particle count is bounded8192.
Native sprite rendering and entity data replace the legacy particle/network
APIs. The actual client counters observe draws/emissions only.

## Original media equality

The following resource bytes were compared directly with the released JAR:

| Resource | SHA256 |
|---|---|
| `textures/gui/gui_arcanebore.png` | `0b2ea01e46629b2289a5fb305b687b4008078100310bc32b1994b80e401bb890` |
| `textures/entity/arcanebore.png` | `a2b3a59c55afe619933d73847a7a8db236ce616ffba815f6d2adb9afe4255aa7` |
| `textures/misc/beam1.png` | `f56bfe8f53af35d06b1c5d6f0979ddcb6791af7c5becdb07aa156d073b909567` |
| `textures/misc/auranodes.png` | `3b46124e4a48422bbb64d7948707c3d4422e9399970b7637e255726e5156bda4` |
| `textures/misc/particles.png` | `3c31a3bed7a007ef4d671745bd627112c79d1ac70049e23194a1454929f17000` |

## Opt-in isolated native driver

`BoreClientSmokeTest` runs only with `thaumcraft.boreSmokeTest=true`. The
hidden non-input desktop launcher owns a new timestamped world. It never
switches/activates desktops, uses global input or modifies manual saves.
No gallery, development caption or substitute renderer is added to gameplay.

Ten intended captures are paid research, original book recipe, active native
infusion, paid physical output, original pickaxe GUI, redstone pause, actual
native beam, real vanilla chest output, enchanted GUI and enchanted native
beam. Success requires all ten fresh image files and exact native facts:

- Ordinary C2S ARCANEBORE pays32raw Golemancy Theory+16Artifice Observation+
  16Auromancy Observation and receives10XP(start5+completion5).
- Physical native altar spends a basic central, eight original peripheral
  components and200typed essentia, creating one fresh `turret_bore`. Five
  supplied jars start at28/28/103/28/28 and must finish3each. Original bare
  recipe research predicate and instability4 remain.
- Actual pedestal interaction and native ItemEntity pickup retrieve that
  output; actual survival placer consumes it and creates one owned valid
  `arcane_bore`, HP50, NoAI=false and player-facing NORTH.
- Native owner/menu C2S quick move pays a raw diamond pickaxe to the one
  real tool slot, Width7/Depth24/Speed4. Unpowered native world ticks40+
  must not harvest, while ordinary aura callbacks recharge.
- An explicit redstone fixture powers the block below; only ordinary native
  world ticks search/mine/harvest/insert. The rendered source beam and pulled
  particles must actually occur. Settled native wall removal equals physical
  cobblestone in an adjacent real chest, without duplicate loose drops.
- Native GUI withdraws the old tool and pays a raw enchanted one. Diamond
  EfficiencyIII/FortuneIII/SilkI with original infench DestructiveI/BurrowingI/
  SoundingIV/RefiningIV yields Width11/Depth56/Speed7/Fortune4/Refining4/Silk.
  Subsequent ordinary mining must create real Silk Touch stone in the chest.

Explicit fixtures are predecessor studies/raw knowledge, loaded arena/bedrock
floor, completed physical altar/candle pairs, raw basic central/components/
essentia/vis, chest/252stone wall, redstone and raw plain/enchanted tools.
Neither ARCANEBORE nor the finished Bore is supplied. This is not full
survival progression, a proof of enchanting progression or independent-client
multiplayer QA. No direct tick, harvest, target or activation setter is used.
Markers are `THAUMCRAFT_BORE_CLIENT_SMOKE_IMAGE`, exact fact markers,
`THAUMCRAFT_BORE_CLIENT_FACTS`, `THAUMCRAFT_BORE_RENDER_AUDIT_OK` and
`THAUMCRAFT_BORE_CLIENT_SMOKE_OK`. Final facts are recorded after power
removal and20+ordinary native ticks, with wall-air equal to physical
cobblestone+Silk Touch stone and zero duplicate loose drops. Both original
pause ticks and final settled pause ticks are reported separately.
Definitions/compilation alone do not establish success; fresh native exit0,
log facts and review of every actual screenshot are required.
