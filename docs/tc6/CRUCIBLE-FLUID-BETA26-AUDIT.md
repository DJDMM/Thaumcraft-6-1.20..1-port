# Crucible fluid — released BETA26 rendering audit

This records an implementation correction and original asset/bytecode checks.
It is not a completed native client run or visual acceptance. Fresh captures of
the final renderer, including its interaction with the original froth, still
require inspection. Earlier failed, rejected and interrupted captures remain
historical evidence.

## Authority

Released `Thaumcraft-1.12.2-6.1.BETA26.jar`, SHA256
`9425f8643581b27ff8845b087c8bc6fc10425a32942f1a3f0e265ce6b38f7b5f`,
is the behavioral authority. Java17 `javap -c -p` of
`thaumcraft.client.renderers.tile.TileCrucibleRenderer`,
`thaumcraft.client.lib.UtilsFX` and
`thaumcraft.common.tiles.crafting.TileCrucible` confirms the formulas, state
gate, geometry, UVs, tint, blend and ordinary packed-light argument below.
Recovered source at commit `954022bb777b7546281fb36df8522f0ba6b43f81`
was used as a reading aid.

Raw transcript: `validation/visual041-crucible-fluid-javap.txt`, SHA256
`9c878b79b2bb6b027c02a3527a57d4ccedb5a7a7fbe74d3f1e1665f613aa4eda`.
Its copy in workspace `work/` has identical bytes. Machine-readable provenance
is in `validation/visual-041-crucible-fluid-original-assets.json`.

## Confirmed defect

The previous port baked an inset water quad at constant local Y `12/16=.75`
whenever blockstate `filled=true`, with modern biome water tint. It therefore
could not follow real tank volume or the dissolved aspect volume, and did not
apply the original aspect-quantity tint. The original particle program already
used the real fluid height, so the displayed liquid and particle origins could
disagree. The blue original water texture also differs from modern grayscale
water; substituting the latter while merely removing biome tint would make
clean water gray.

## Exact original contract

`renderEntityAt` draws liquid only when the actual tank contains water. Heat and
the boiling state do not gate the surface. At capacity 1000 mB, let `w` be water
and `n` the total dissolved aspect quantity (`aspects.visSize()`):

```
base = .3F + .5F * (w / 1000F)
height = base + (n / 500F) * (1 - base)
if height > 1: height = 1.001F
if height == 1: height = .9999F
```

The implementation uses the already matching
`MachineFxPrograms.fluidHeight(w, aspects)`, which the original froth and boil
programs also use. No particle size, color, gravity, lifetime or clock changes
are part of this correction.

`TileCrucibleRenderer.renderFluid` translates `(0,height,1)`, then rotates
90 degrees about negative X. `UtilsFX.renderQuadFromIcon`, at scale 1, supplies
this full quad, including the original orientation of the animated water icon:

| Local vertex | U | V |
| --- | --- | --- |
| `(0,height,1)` | sprite max U | sprite max V |
| `(1,height,1)` | sprite min U | sprite max V |
| `(1,height,0)` | sprite min U | sprite min V |
| `(0,height,0)` | sprite max U | sprite min V |

The normal becomes `(0,1,0)`. The geometry remains a full 1×1 quad; the opaque
crucible shell hides the parts below its walls. It is not cropped to the mouth
and is not raised above the original height to expose particles.

The tint depends on quantity, not individual aspect colors or biome color:

```
recolor = n / 500F
if recolor > 0: recolor = .5F + recolor / 2
if recolor > 1: recolor = 1
RGB = (1 - recolor/3, 1 - recolor, 1 - recolor/2)
opacity = 1
```

`UtilsFX` enables ordinary `SRC_ALPHA, ONE_MINUS_SRC_ALPHA` blending and retains
normal depth behavior. The caller supplies the crucible block's world packed
light, not an emissive/fullbright constant. The native renderer consequently
uses the normal BE packed-light argument, an upward normal, ordinary alpha
blending and depth-tested/depth-writing entity-translucent geometry. This is a
modern rendering API adaptation, not a claim that modern and legacy directional
lighting or GPU filtering are byte-identical.

## Original animated water resource

The icon used by BETA26 is vanilla Minecraft 1.12.2 `water_still`. Its original
16×512 PNG contains 32 blue frames and alpha values 168..226. It is not the
modern grayscale sprite that needs biome tint. The exact PNG and its animation
metadata are copied from the already pinned official Minecraft 1.12.2 client
JAR, SHA256
`8ada07da5ee77dad3527bd7278fbd05ee1fc8a597813b216a871a2d7d64cc64f`:

| Original entry | Port resource | SHA256 |
| --- | --- | --- |
| `assets/minecraft/textures/blocks/water_still.png` | `assets/thaumcraft/textures/blocks/crucible_water.png` | `2fef429515591b671525477e886da265595027b288b771cac185fa331b0c52ba` |
| `assets/minecraft/textures/blocks/water_still.png.mcmeta` | `assets/thaumcraft/textures/blocks/crucible_water.png.mcmeta` | `e9b8025fde1b09b0a4d2a38427b04f98f28a593045be6d177eb2b29164c872b0` |

The metadata retains `frametime:2`; no pixels, alpha or animation settings were
edited. The existing block-atlas directory source (`blocks`, prefix `blocks/`)
stitches the relocated sprite `thaumcraft:blocks/crucible_water`. This added
vanilla compatibility resource must be distinguished from original TC6 asset
parity counts.

## Integration and validation boundary

`AlchemyClientEvents` now registers `CrucibleRenderer` for the actual
`AlchemyModule.CRUCIBLE_TILE`. The renderer reads the existing synchronized tank
and aspect data on every frame. Its empty-water gate follows the original
renderer. The obsolete baked fluid model and its multipart entry and biome tint
handler are removed; the crucible shell, block properties, server tank,
crafting, aspect consumption, synchronization and particle programs are unchanged.

Static checks verified JSON syntax, exact original resource bytes and the
geometry/formula against released bytecode. Compilation, native server
regression and actual final client visual acceptance belong to the root QA run;
this audit does not predeclare their results. In particular, a correctly
registered surface does not prove that a camera can see the very small original
froth over the opaque front rim.
