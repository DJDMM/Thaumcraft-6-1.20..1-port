# Sounding through terrain: BETA26 and Forge47.4.10

This 0.40.1 patch corrects ore-marker depth handling. It adds no research,
recipe, scan reward, range increase or extra durability cost. The original
authority is the released `Thaumcraft-1.12.2-6.1.BETA26.jar`, SHA-256
`9425f8643581b27ff8845b087c8bc6fc10425a32942f1a3f0e265ce6b38f7b5f`.
Recovered source at commit `954022bb777b7546281fb36df8522f0ba6b43f81`
is a navigation aid. Native QA results belong in the final artifact report;
the implementation and verification contract below are not themselves a pass.

## Original released behavior

Read-only `javap -c -p` on released `PacketFXScanSource` confirms a cube
of radius `4 + rank * 4`, groups connected through all26 neighboring
positions with the same block state, and one particle at each group's mean
block center. The delay is integer truncation of `distance * 3`. The effect
lasts44 ticks, loops atlas frames240 through254, uses grid16 and scale9,
giving the original `FXGeneric.draw` half-width `0.1 * 9 = 0.9`.
Its alpha keys are0,1,0.8,0. Dark colors with meanRGB below0.25 use layer3;
the others use layer2. The current atlas is byte-identical to the released
`assets/thaumcraft/textures/misc/particles.png`.

Original `ParticleEngine.onRenderWorldLast` sets depth writes false and,
for both layer2 and layer3, explicitly calls `GlStateManager.disableDepth()`.
Layer2 blends SRC_ALPHA/ONE; layer3 blends SRC_ALPHA/ONE_MINUS_SRC_ALPHA.
It calls `enableDepth()` after each of these layers and restores depth
writes after the particle loop. Seeing the ore indication through ordinary
opaque terrain is therefore essential behavior, not an optional enhancement.
Released `javap` confirms the SRG calls directly: depth-mask false at68,
`func_179097_i` (disableDepth) at189 for layer2 and204 for layer3,
`func_179126_j` (enableDepth) at468 for either layer, and depth-mask true
at478. These are released instructions, not only recovered source comments.

## Confirmed modern integration defect

The exact local mapped artifact inspected with Java17 `javap -c -p` is:

```
net/minecraftforge/forge/1.20.1-47.4.10_mapped_official_1.20.1/
forge-1.20.1-47.4.10_mapped_official_1.20.1.jar
SHA256 86c89ba73afaa2dbc075f87b5c30758ef64ec2d2da126452fd22000aa7f598a6
```

`RenderStateShard$DepthTestStateShard.lambda$new$0(int)` compares its
function with519 (`GL_ALWAYS`). When it is519, the method returns without
calling either `RenderSystem.enableDepthTest()` or `depthFunc()`. Its
clear lambda likewise does nothing for519. Consequently the standard
`NO_DEPTH_TEST` shard inherits the current depth state; its name does not
guarantee an explicit disable.

In the same artifact, `ParticleEngine.render(..., Frustum)` calls
`RenderSystem.enableDepthTest()` at bytecode offset4. Its final sequence
at318–329 restores depth writes, disables blending and turns off the
light layer, without disabling depth testing. `LevelRenderer.renderLevel`
invokes that render at2601 and dispatches `AFTER_PARTICLES` at2604 in one
graphics branch; the other does so at2740 and2743. The ore effect is drawn
at this event. Using the no-op standard shard therefore leaves terrain
depth testing enabled, hiding the indicator behind the very rock it
should penetrate. The separate color-only write mask prevented writes,
but did not fix the test.

## Localized correction

Both ore render types now use `THROUGH_TERRAIN`, a dedicated
`DepthTestStateShard`. Its setup explicitly disables depth testing and its
clear explicitly enables it, matching the original particle layers and
the enabled state of this modern event. The constant is initialized before
the two render-type constants. State changes occur during the effect's own
render-type flush rather than wrapping unrelated shared-buffer submissions.
The existing shader, atlas, full-bright color, blends, billboards, dimensions,
grouping, delay, lifetime, range, packet, hand handling and five-durability
payment are unchanged. This patch does not generalize depth-state changes
to unrelated effects without separate evidence.

## What the old0.40 proof missed

The delivered0.40 full `native-sounding` image does not show an obvious
ore ring. Its prepared diamond group has centroid(.5,111.5,8.5); the crouched
player at(.5,112,13.5), yaw180 and pitch20 looks approximately toward that
point. It cannot be excused simply as all markers being outside the view.
The old client check `visibleRipples()` tested only delay and particle age,
not framebuffer output, occlusion or visible pixels. Durability payment and
S2C effect creation can pass while terrain hides every ring. Thus the old
24-scene run remains evidence for those native actions, but its visual
review was a false positive for through-terrain Sounding. Its files and
report must remain as historical evidence; they are not fresh patch proof.

## Required targeted verification

The patch requires a fresh full1673-test native server run and a distinct
opt-in hidden-desktop client run. The targeted client compares unchanged
before/active/after views with multiple genuinely buried ore groups under
Fancy and Fabulous graphics. Actual native uses must pay five durability
each, preserve target/ore terrain and create the ordinary S2C effect.
Root and an independent reviewer must inspect every final full-resolution
frame and both contact sheets, including actual colored rings over opaque
terrain and their disappearance after expiry. A separately recorded pixel
analysis must bind the exact native image hashes and the three ore regions
against before/after controls in each graphics mode. The excluded-range ore
is outside the camera view: its exclusion is checked in native particle facts,
not claimed as a visible negative pixel region;
particle-list membership alone cannot satisfy it.

The final report records actual frame count, uses, durability, graphics
modes, pixel measurements, source hashes and native exits. Prepared terrain,
raw infused tools, camera positions and graphics selection are explicit QA
fixtures. This focused patch run does not repeat the fourteen paid altar
routes, establish full survival progression, multiplayer, arbitrary shaders,
every camera angle, audio parity or full WarpEvents. Those broader0.40 facts
retain their original report and scope.

## Fresh server run exposed an existing fixture assumption

The first fresh0.40.1 server run exposed a probabilistic assumption in the
existing `fourNativeHelmMutationsPreserveSamePhysicalHelmAndActivateAllOriginalConsumers`
test, rather than a Sounding production failure. Its world was Easy. The
fixture supplied a mob-attack damage amount12 and expected Angry Ghost to
apply Wither unconditionally. In the exact mapped1.20.1 `Player.hurt`,
offsets100–115 perform Easy scaling `min(amount / 2 + 1, amount)` before
the call to `LivingEntity.hurt` at150. Input12 therefore reaches the
`LivingHurtEvent` as7. The preserved mask predicate is
`nextFloat() < originalAmount / 10`, giving only0.7 in that fixture.

Only the test's raw incoming attack amount is corrected:18 on Easy,12
otherwise. The native post-difficulty amount is then10 on Easy,12 on Normal
or18 on Hard, making the unchanged predicate guaranteed without setting RNG
or changing production mask behavior. Its native hurt call and Wither80-tick
assertion are retained. This does not alter Sounding's cost, age, delay or
render behavior. A fresh full server rerun is required after the fixture
source change. Actual completed outcomes, exits, source snapshots and the
first run's retained failure history belong to the final artifact report;
this explanation does not assert the later run has passed.

## Фактическое исправление0.40.1

Сигнал Сонара через камень исправлен. `sounding-0401-server2`1673/1673 и
`sounding-0401-client2`8/8 прошли с native0. Все8полныхPNG и оба листа
просмотрены двумя проверяющими; шесть проекций скрытой руды имеют
проверенные реальные пиксели до/во время/после импульса.
Точные SHA, подготовленные условия и ограничения —
[отчёт0.40.1](../../validation/artifact-report-0.40.1.json).
Прежняя0.40проверка возраста частиц не доказывала их видимость.
