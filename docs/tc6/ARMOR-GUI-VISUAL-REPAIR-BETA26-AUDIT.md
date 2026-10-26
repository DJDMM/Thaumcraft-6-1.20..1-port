# Armor and GUI visual repair — 0.41.0

Primary reference is the released TC6 6.1.BETA26 JAR, SHA256
`9425f8643581b27ff8845b087c8bc6fc10425a32942f1a3f0e265ce6b38f7b5f`.
Recovered source commit954022bb777b7546281fb36df8522f0ba6b43f81
supplies readable methods. Fresh released bytecode is retained locally at
`work/armor-gui-041-original-javap.txt` (eight original classes).

## Original armor state and motion

`LegacyArmorGeometry.configure` restores all12 omitted Fortress shoulder and
side-panel conditions. Top/first shoulder and second side panels need two
pieces; second/third shoulder and third side panels need three. Existing
book/flaps, scroll/ornaments, masks and goggles retain their original rules.

`ModelRobe`, `ModelKnightArmor` and `ModelLeaderArmor` render methods confirm
`a=cos(swing*.6662)*1.4*amount`, `b=cos(swing*.6662+PI)*1.4*amount`,
`c=min(a,b)`. Robe front offsets are-.1047198/-.3316126; rear offsets are
.1047198/.2268928 against-c. Knight uses the same front offsets and
cloak-c/2+.1396263/.3069452/.4465716. Leader uses separate a/b leg cloth
and the same cloak formula. Adult original heads render at1.01 scale.

The modern armor hook applies these independent named child rotations using
the entity's native interpolated walk animation before Forge copies parent
limb pose. It does not manufacture movement. The source model geometry and
textures remain original; state/formula checks do not certify their pixels.

## Focal Manipulator

The physical BER adds the original camera-facing atlas halo per synchronized
aspect: base(.5,1.3,.5), radius.4, bob sin((ticks+i*10)/12)*.02+.02,
half-size.175, alpha.66, frame320+viewEntityTicks%16, 64x64 grid. The shader
declares additive blend and GL_GREATER1/255; depth is tested and only color is
written. It uses the BER main output rather than a particle framebuffer that
may be cleared after the block-entity stage.

The original two ray fans preserve seed187+num*num and random call order:
rotation first, height second, width third. Focus age-zero EntityItem bob
and hoverStart rotation are retained. Original image confirm, rounded graph
connectors, scrollbar tracks/knobs and arrow-only range/list spinners replace
the previous Minecraft widgets/primitive bars. The old crafting/payment
route is unchanged.

### Native editor input compatibility

An independent source/native-base review found two concrete input regressions
in the new Focal widget integration. The released `GuiSliderTC` updates its
value continuously while held. The mapped Minecraft1.20.1
`AbstractContainerScreen.mouseDragged` instead handles inventory/touchscreen
dragging and unconditionally returns true without calling the superclass
widget-drag dispatcher. Merely supplying `OriginalSlider.onDrag` therefore
allowed a single click jump but never a real screen drag.

`FocalManipulatorScreen.mouseDragged` now routes only a native captured
left-button drag to its focused, active, visible `OriginalSlider`; all other
drags retain the container's inventory behavior. The existing native release
path is retained: `AbstractContainerScreen.mouseReleased` first invokes
`Screen.mouseReleased`, and `ContainerEventHandler.mouseReleased` clears the
screen's dragging state before dispatching a child's release. The fix adds no
custom lingering capture or alternate input channel.

The released Focal click method also sends every editable click to its name
field before handling graph/palette regions. The port's custom early returns
had bypassed native widget dispatch, leaving the name field focused after a
graph click. Those editable clicks now blur a previously focused name field
when outside it, through the native `setFocused(null)` lifecycle. Typing and
the inventory-close key consequently follow the clicked graph rather than
remaining captured by the old name field.

Raw released and mapped-native bytecode is retained in validation:

- `visual041-gui-original-input-javap.txt`: released Focal/Research GUIs and
  original slider/spinner classes; the Focal name-field click is at bytecode
  offset87 in its click method.
- `visual041-gui-native-input-javap.txt`: mapped native container/widget input,
  plus the BE dispatcher and vertex consumer. The container drag method ends
  at offset347 without a superclass drag call; release calls Screen at
  offset5. The same proof confirms world packed light goes through the BE
  renderer argument and native `uv2(int)` splits low block/high sky components.
- `visual041-gui-native-focus-javap.txt`: native focus changes explicitly blur
  the old widget before installing a new one, including a null target.

The opt-in ArmorVisual fixture keeps exactly the same19 captured images and
the same paid six-node focus. Before capturing the Focal view, it now checks a
real name-field click followed by a graph click and verifies blur. It then
checks a real visible palette slider through screen
`mouseClicked -> mouseDragged -> mouseReleased`, comparing the post-click and
post-drag values and asserting dragging is cleared. A temporary incomplete
eight-node graph with three nested trajectory splits is created through the
existing editor calls and accepted by the real revisioned EDIT route. Its
real nonzero visible vertical/horizontal ranges permit the same drag checks.
Each scroll position is restored through real clicks; the temporary subtree
is then replaced through editor calls with the original six-node paid graph.
No draft/scroll/visibility/dragging setter creates the checked result. The
temporary graph never starts or pays a focus plan. Opt-in native logs record
eligibility, ranges, click/drag/restored values, coordinates and release state.

These new checks are pending native execution. No successful runtime result is
claimed from the bytecode review or the fixture's new assertions alone.

The same review confirmed two further editor input defects. First, the confirm
button previously retained the old `containerTick` enabled state after a name
or graph/settings change. A second confirm event in the same tick could send
START before the dirty draft's EDIT, paying for the old server graph. Every
dirty transition now immediately disables confirm; its callback also rejects
dirty/pending state, and the tick refresh rechecks pending after sending EDIT.
The native fixture makes a real setting change and clicks the confirm location
before the next tick, requiring no START request/pending transition or draft
mutation. The later acknowledged edit and actual paid start remain required.

Second, palette wheel changes were clamped only during rendering. Wheel-up
from index0 followed by a palette click before the next render could index
`parts.get(-1)`. Released `GuiFocalManipulator.handleMouseInput` checks both
bounds immediately. The port now clamps on each wheel event. The fixture
wheels beyond both ends without an intervening render, verifies the bounds,
then really clicks the first eligible part. The modern port's extra Shift-wheel
graph shortcut is retained but clamped to the same min/max as the original
horizontal slider; ordinary graph wheel already had vertical bounds. A zero
vertical wheel delta leaves the values unchanged. The Shift shortcut itself is
source-reviewed, not a native injected-key test; no global input is sent.

## Research desk

See `THEORY-GUI-BETA26-AUDIT.md`. The repair restores the full sheet drawing
and frame formulas, native atlas sparkle layer, original aid-icon behavior,
and cosmetic persisted previous-card seeds. `TheoryVisualPersistenceGameTests`
checks accepted-selection history, unchanged rejected selection and old-save
roundtrips. No test or client pass is claimed before root records execution.

## Required native review

`ArmorVisual` / `thaumcraft.armorVisualSmokeTest` defines19 full1600x1000
native captures: three Fortress partial sets, twelve front/rear walking phases
across Robe/Knight/Leader, two real Focal editor views and two physical paid
working-table halo phases. Walk-phase gates execute at RenderTick.END against
the actual captured interpolated frame. Reflection records actual rendered
armor mesh state/formulas; no limb, motion, beam, halo or active clock setter
creates the result. A long owned floor corridor keeps native walking on set.

Fixtures supply raw armor, greater focus, completed parent research,100XP,
eight crystals of each required aspect and initial500 aura. They do not prove
paid armor/research progression. Native inventory clicks insert the focus;
native editor/settings/start packets must produce a paid physical table.

`TheoryComplete` separately covers22 actual paid/negative/reload/GUI-scale
scenes. Root and an independent reviewer must inspect all actual files,
including seeded sheet faces, clipping, cloth silhouette and both halo phases.
Only recorded screenshots and an actual pixel review close visual findings.
No new native QA was run by this implementation agent.
