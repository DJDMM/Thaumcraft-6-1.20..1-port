# Original world FX core — BETA26 repair for 0.41.0

This is a source/JAR contract and implementation audit. It is **not a successful
client run or a visual parity verdict**. Fresh native server checks and actual
pixel/visual review of the final client captures are separate requirements.
The previous audit and the failed/partial probe evidence remain valid history.

## Authority

Primary: released `Thaumcraft-1.12.2-6.1.BETA26.jar`, SHA256
`9425f8643581b27ff8845b087c8bc6fc10425a32942f1a3f0e265ce6b38f7b5f`.
The recovered source at commit
`954022bb777b7546281fb36df8522f0ba6b43f81` is a reading aid.

Critical classes were independently disassembled with Java17 `javap -p -c`:
`FXDispatcher`, `FXGeneric`, `ParticleEngine`, `FXBoreParticles`,
`FXVisSparkle`, `FXSmokeSpiral`, `FXWispEG`, `FXShieldRunes`, `PacketFXShield`,
`TileNitor` and `EntityEvents`. The inherited particle scale and packed-light
behavior were checked against the official Minecraft1.12.2 client JAR, SHA256
`8ada07da5ee77dad3527bd7278fbd05ee1fc8a597813b216a871a2d7d64cc64f`,
using the existing pinned MCP mapping `btf` (Particle) and `buk` (BufferBuilder).

Scratch bytecode transcripts live in workspace `work/`:

| Transcript | SHA256 |
| --- | --- |
| `fx041-core-javap.txt` | `bd84025f945f2e00b2ebab2b44b329080869d89525be517e0eda68cb9f8071be` |
| `fx041-custom-javap.txt` | `0813dff4254581dbd63b56ce30d898d635bad746bd2bfeb84ee4aac5641ed86b` |
| `fx041-particle1122-javap.txt` | `5a1253312358e570ae4a5731cab609737bfac0712eb6845eb72263e25853f858` |
| `fx041-buffer1122-javap.txt` | `e50d81908bd73407be048ddce75c55eb510dfeac497d59240b4dd74c87082a5d` |

## Transport, lifetime and compositing

`FxDispatcher` transports the source dimension, effect name, double precision
origin/vector, RGB, scale, data/entity targets, actual item texture identity,
generic NBT program and delay. The physical `ItemStack` is copied; an effect
never owns or consumes the ingredient. The only channel direction is S2C.
Common-side classes use a distribution guard before accessing client classes.
Client dimension changes discard old particles, delayed work and shield actors.

`FxParticle` ports the original Generic atlas selection, loop/inc behavior,
late final frames, precomputed alpha/scale keys, gravity, drag, random movement,
moon-phase wind, rotation, angled geometry and RGB interpolation. Original
`dr/dg/db` are initially zero: a Generic program that never calls
`setRBGColorF` fades its original white toward black. This matters for burst
and flyer smoke. An explicit RGB also sets the same ending RGB. Nitor core
explicitly uses white and therefore stays white. Rotation retains the actual
released conversion and `2π * rotationSpeed` tick formula.

The four world layers retain their order3→0:

| Layer | RGB blend | Depth test | Depth writes |
| --- | --- | --- | --- |
| 0 | SRC_ALPHA, ONE | enabled | disabled |
| 1 | SRC_ALPHA, ONE_MINUS_SRC_ALPHA | enabled | disabled |
| 2 | SRC_ALPHA, ONE | explicitly disabled | disabled |
| 3 | SRC_ALPHA, ONE_MINUS_SRC_ALPHA | explicitly disabled | disabled |

The dedicated modern shaders retain the released alpha threshold
`GL_GREATER, 1/255`; the vanilla entity/text alpha cutoff would remove weak
edge pixels. Additive and ordinary-alpha shaders have separate blend JSON;
shader JSON is not allowed to overwrite the selected blend accidentally.
Depth-free layers use an explicit disable/restore state shard because Forge's
nominal no-depth shard does not disable an already enabled depth test.

Nitor/Wisp's original delayed API queues even delay0 until the next client
START tick, preserving insertion order. Active limits are2000/1000/500 per
layer according to particle setting, with original low-FPS additional
probabilistic suppression below30FPS. World FX do not add the original
screen-overlay layers4/5; their existence is not evidence that those separate
GUI paths are ported through this engine.

## Concrete original programs and late corrections

| Effect | Released contract retained |
| --- | --- |
| Nitor flames | Every client tile tick; jitter source `(.5,.45,.5) ± gaussian*.025`; velocity gaussianXZ*.0025 and randomY*.06. Frames264–271, life10–14, loop, alpha.66, scale3–4→.05, randomXZ.0025. |
| Nitor core | First tick then every10; source `(.5,.49,.5)`, cell457, life10, white, scale1→`1+gaussian*.1`→1, layer1, randomXYZ.0002. |
| Wisp particles | Frames264–271, life10–14, loop, alpha.5, scale1–1.25→.05, wind.00025, randomXZ.0025 and original delayed insertion. |
| Burst | Grid16, frames208–238 across31ticks, original supplied scale and default white→black Generic color behavior. |
| Fire mote / Alumentum | Cell7, life16 with one-in-six extra age, original velocity/drag, scale and opacity shrink, spin, packed light255. Ordinary fire randomly selects full-size additive or one-third-size alpha layer; Alumentum always uses the original alpha layer. |
| Vent | Original normalized heading with Gaussian inaccuracy, initial scale.05–.15 times multiplier, expansion×1.15, Y+.0025, drag.85, cells1–5, original `.4*(max-size)/max` vertex alpha. |
| Sword smoke spiral | Original two-turn spherical path/radius/start/minY, life20–29, cells1–5, alpha.66 times pre-increment age fade. Inherited vanilla1.12 particle scale is1–2, not a fixed1. |
| Infusion fragments | Actual source quarter-sprite, original minU-first orientation, item tint*.6 and the data2 block overload's untinted(.6,.6,.6) quirk described below; item/block initial-velocity distinction, source-dependent life, homing/clamp/shrink/noise and alpha layer. **Vertex alpha is1**, even though the original dispatcher calls `setAlphaF(.3)`. Released renderer hardcodes all four vertex alpha values. |
| Infusion sparkle | Frames256–259, original purple or XP-green RGB, size.5–1, sine bob, packed light240 and original homing. Original 64/fancy or32/fast distance expiry is retained; block fragments do not inherit the item-only distance rule. |
| Focal vis sparkle | Integer input blocks get source random[0,1) XYZ and target `.4 + random*.2` XYZ before custom homing. Frames512–527, packed light240, alpha.5, original size ramp and sine bob. The last argument is literal RGB, not an aspect/color selector. |
| Matrix block runes | Caller supplies the already centered source; no second+.5 offset. Original surface orientation, life3×duration, quarter-height offset, random glyph and scale, alpha/2 and packed light240. |
| Arcing slash | Original FXPlane movement, random Y rotation, grid32 row10/cells22–30, size growth, alpha/2 and packed light240. |
| Collector follow | Original crucibleBubble: cell64, life15–24, scale.3–.6, RGB(.33,.33,1), randomXYZ.002, gravity−.001, final65/66/66; client previous item coordinates, actual item bob and independent XYZ jitter. It is not a Nitor-core particle. |
| Dark spirit | Two FXWispEG per dispatch, original dark RGB, frames192–204, alpha.2×agefade×distancefade, target XZ movement, alpha layer and light240. Its inherited vanilla scale1–2 multiplied by.4 is retained. |

Custom collision boxes retain the source constructors: default Particle .2,
Vent .02, Spiral/Rune/Slash/dark spirit .01, Generic .1. Light240 and255 are
original packed-light values, not a blanket modern fullbright replacement.
The old SHORT BufferBuilder lightmap branch writes its second argument first;
this confirms that the modern packed words retain the original UV2 ordering.

### Bamf

Sound and flair are independent flags. Sound is original `poof`, volume.4,
pitch `1+gaussian*.05`. The primary layer1 smoke uses original cells123–127,
life20–34, clamped RGB Gaussian variation, alpha1→.1, scale3→4–7, slowdown.7
and signed spin. Direction contributes*.1 to primary velocity.

Flair additionally creates the missing wispy-mote loop before the flash:
original RGB.25–1, input age15–24 with the original integer `age/2`
extension, frames512–527/loop, alpha0/.6/.6/0, scale1→.5, wind.001,
gravity−.01 and randomXZ.0025. The flash uses grid16/cell77, life10–14,
RGB(1,.9,1), alpha1→0, scale10–12→0 and Gaussian rotation.

Curly smoke retains the independent flair-dependent loop, direction*.025,
source offset five times initial velocity, seeded optional angles, life
`25+nextInt(20+20*seed)`, grid16/cell60–63, source RGB→(.1,0,.1),
alpha.75→0, scale5→10–14 and original signed2–4 rotation. RNG choices retain
the method-local source order where represented; random streams are modern
client/particle streams and are not claimed to reproduce an unseeded1.12
recording frame for frame.

### Runic hit hemisphere

`LivingHurtEvent` publishes the original32-block shield hit before absorption
is consumed, only with positive **integer** absorbed charge and a registered
Runic state. Actual attacker ID determines yaw/pitch. Generic−1 creates both
hemispheres; fall−2 and falling-block−3 select the original side. Modern
`falling_anvil` maps to the old shared falling-block damage cause.

The renderer uses the original `hemis.obj` and15hemis/15ripple textures,
original8–11tick life, animation frame, target center/height, rotation,
additive blend, no culling/depth writes, light220, fade and source RGB.
This change does not invent champion/eldritch combat behavior where the
corresponding mechanics are absent.

## Nitor native block identity and assets

Base Nitor and the sixteen original colors now use an invisible,
non-colliding light15 block with the original .33→.66 selection cube and a
client-ticked native Nitor tile. Existing saved catalogue tiles migrate on
their actual world ticker without replacing the physical colored block.
Original two-layer item presentation is retained; no glowing cube substitute
is rendered in the world.

A direct zip-byte comparison performed during this audit matched **32/32**
core assets: particles atlas, hemis OBJ and all thirty shield PNG. The atlas
SHA256 is `3c31a3bed7a007ef4d671745bd627112c79d1ac70049e23194a1454929f17000`;
hemis OBJ is `cab67fcdcf7d952336438aaad043e33a665c6db928172ad4b68d4ddb39f41ad9`.
Byte identity establishes provenance, not correct on-screen geometry or blend.

## Late source/JAR and modern pipeline corrections

### Data2 block-fragment color

Released `FXDispatcher.drawInfusionParticles2` constructs the block overload of
`FXBoreParticles` and then calls `getObjectColor` with the destination matrix
position. For this instability path, the state at that position differs from
`blockInstance`. The method enters the item-color branch with its uninitialized
`itemInstance`; the caught exception preserves the constructor's original
RGB(.6,.6,.6). The port therefore keeps data2 fragments untinted gray while
retaining their physical block/item sprite. The data1 item overload still uses
the actual item tint multiplied by.6. This is a released-program quirk, not a
general rule that all block particles should ignore their colors. The existing
`FXDispatcher`/`FXBoreParticles` bytecode transcripts above contain this path.

### Untextured additive ray fans

`FxShaders.rayFan()` registers `thaumcraft:original_ray_fan` with the exact
`DefaultVertexFormat.POSITION_COLOR` input. Its three resources live at
`src/main/resources/assets/thaumcraft/shaders/core/original_ray_fan.{json,vsh,fsh}`.
The shader transforms Position, carries vertex Color, and outputs
`vertexColor * ColorModulator`. It has no texture sampler, lightmap, fog or
alpha-discard operation. Transparent ray tips remain valid geometry.

`FxRenderTypes.rayFan(boolean cull)` uses that shader and QUADS; callers express
each original fan triangle by repeating its fourth vertex. The state uses
SRC_ALPHA/ONE addition, LEQUAL depth testing, color writes without depth writes,
MAIN_TARGET, no texture/lightmap/overlay, and the caller's original culling.
The shader JSON also declares SRC_ALPHA/ONE, so applying a shader cannot silently
replace the intended blend with ordinary alpha. Focal Manipulator rays and
Infusion Matrix halo rays both request `rayFan(true)`.

The vanilla `rendertype_lightning` shader JSON has an ordinary-alpha blend
override. Reusing its program while selecting an additive RenderType therefore
does not preserve the original fan blend. The dedicated program removes that
state conflict. Source agreement alone does not establish the final visible ray
shape; actual native client frames remain required.

### Focus Mine lightmap and rotation adaptation

The released `RenderFocusMine.renderEntityAt` always renders its original
`ModelGrappler` and `textures/entity/mine.png`. It has no owner, goggles or armed
visibility gate. The original core is3/16 of a block across and the prongs5/16;
its native model can be small in a distant frame. Arming starts a40-tick counter;
after it reaches zero, client effect particles emit on every fifth entity tick.

The released renderer sends FLOAT lightmap coordinates(61680,0) through
`OpenGlHelper`, then interpolates both previous/current yaw and pitch. The actual
Minecraft1.12 `EntityRenderer` (`buq.i`, mapped `func_180436_i`) sets the lightmap
texture matrix to scale1/256 with an8-unit translation, linear filtering and
GL_CLAMP(10496). Thus the original large positive light coordinate uses the
saturated block-light edge, with sky light0.

Modern `DefaultVertexFormat.NEW_ENTITY` stores UV2 as signed SHORTs.
`BufferBuilder.vertex` writes `(short)(packedLight & 65535)`; passing61680 makes
the first component -3856. The actual local1.20.1 shader
`assets/minecraft/shaders/core/rendertype_entity_translucent.vsh` reads
`texelFetch(Sampler2, UV2 / 16, 0)`, yielding an out-of-range negative texel
coordinate(-241,0). That shader does not use the clamping
`minecraft_sample_lightmap` helper. An undefined/out-of-range sample can blacken
the otherwise correctly submitted Mine mesh.

The port adapts this intent to valid modern packed light240: block15/sky0.
Pitch now uses `Mth.lerp(partial, entity.xRotO, entity.getXRot())` before the
original Z-axis rotation; the dispatcher supplies interpolated yaw for Y.
This establishes valid modern light coordinates and the released rotation
formula. It does not claim byte-identical legacy GL_CLAMP border interpolation
or identical behavior across old GPU drivers.

The following evidence was copied verbatim into project validation; paths are
relative to this document:

| Evidence | SHA256 |
| --- | --- |
| [Released Mine, FollowingItem and Crucible disassembly](../../validation/fx041-mine-froth-following-javap.txt) | `08cf196e2642fd850806cb3fecbb034e63e78f60d8705e2a376685b8dcf5ebc7` |
| [Local1.20 NEW_ENTITY/BufferBuilder disassembly](../../validation/fx041-mine-modern-light-javap.txt) | `33f56b5f3ec3f356aa872564a72ad97fe6b1d77ddb3bb1fb7c99eb6967e85c29` |
| [Official1.12 EntityRenderer lightmap disassembly](../../validation/fx041-mine-old-entityrenderer-javap.txt) | `8714b8f986eae687d2f59f66a8572a8eded6ef44f8a8ab35df0202d015c4eabb` |

The modern shader text was read directly from the configured local
`C:/Users/user/.gradle/caches/forge_gradle/minecraft_repo/versions/1.20.1/client.jar`.
The three bytecode files document the released behavior and actual local vertex
storage, not successful final visual QA.

### Native timing boundaries used for visual review

Released `TileCrucible.drawEffects` emits froth every client tick when heat>150
and water is present, even with no aspects. Its original froth has4–6 ticks of
life, scale.2–.4, and downward gravity.1. Aspect-colored ambient bubbles have
their separate one-in-six/aspect-present condition. A frame containing only a
new age0 froth particle cannot establish the visible heated-surface effect;
review requires naturally aged geometry at the actual fluid height.

`EntityFollowingItem` starts its homing acceleration counter at20 and decreases
it toward1;20 is not its item lifespan. It follows until within.5 of its target,
then clears the target and returns to ordinary item motion. Type10 emits the
blue15–24-tick `crucibleBubble` program while following. Native visual fixtures
must account for natural corpse removal, collection and bubble expiry rather
than keeping particles or items artificially alive.

## Acceptance boundaries

`FxCoreGameTests` adds three native checks: actual placement/save-reload of
all17Nitor block identities and bounds; actual world-tick migration of a saved
catalogue Nitor tile; complete S2C round-trip of dimension, vectors, targets,
physical item/NBT and original Generic keyframes. Those checks are prepared
source until the root's fresh native server run passes. They do not establish
visible particles, shader output, shield orientation, audio or multiplayer
delivery. The final build report must bind its actual source hashes, server
result, native client callbacks and reviewed full images separately.
