# World/entity presentation repairs after 0.40.1

Authority: released Thaumcraft 6.1.BETA26 JAR,
SHA256 `9425f8643581b27ff8845b087c8bc6fc10425a32942f1a3f0e265ce6b38f7b5f`.
Recovered source is a reading aid. Original shader resources are also retained
byte-for-byte in `assets/thaumcraft/shader/ender.frag` and `ender.vert`.
This document records implementation and the targeted capture contract; it does
not assert that a new native run or visual review has passed.

## Rift material and depth

`FluxRiftShader` registers two modern core programs sharing the original
fragment equations: three SRC_ALPHA/ONE outer shells and one
SRC_ALPHA/ONE_MINUS_SRC_ALPHA core. Conversion is limited to GLSL150 inputs,
explicit int-to-float casts, `texture()`/fragment output and the modern
ModelViewMat/ProjMat interface. Sixteen layers, background color, seeded
rotations, spherical UV mapping, time drift, polar fade and colors remain the
released `ender.frag` equations. Time is the whole camera-entity tick count;
yaw/pitch are the original player angles, in radians with negative pitch.
Original TC6 uses the vanilla end-portal image. Local1.12.2 and1.20.1 client
resources have different compressed PNG bytes but **identical decoded RGBA
pixels**; the modern vanilla resource therefore retains the original art.

`CatalogEffectGeometry` retains its original spine/radius/polycone math.
Pass zero explicitly disables depth testing only with `hasGoggles`, restoring
enabled depth afterward. Other passes explicitly use LEQUAL. Outer shells do
not write depth; the core does. Each pass is flushed in release order, so
shared buffer ordering cannot render the core before its additive shells.

## Wisp

`VisualEntityRenderers` uses separate additive sprite shaders with alpha
threshold1/255 after modulation, no depth writes and the original packed
light220. The three release frames/scales/colors/alphas and UV orientation are
preserved: particles512+tick%16/.4/white/1; particles320+tick%16/.75/white/.25;
auranodes800+tick%16/.75/aspect/.5. Dead Wisps are not rendered.

`WispEntity` calls the shared original dispatcher for the initial size10 burst,
half-probability jittered ambient particles and death burst atY+.449999988.
Actual AI attack emits an arcBolt from Wisp position to target position,
width.6, within32blocks, replacing the straight native dust line. Damage,
counter cadence, movement and target selection remain their existing paths.
Modern client death status3 is the bridge to the original remote onDeath
visual, rather than assuming modern LivingEntity invokes die on both sides.

## Existing catalogue presentation, without inventing boss AI

- Guardian: released distance-squared alpha.6 within256; fade to zero at1024
  normally or576 on Hard; full alpha in the reserved Outer Lands ID. The
  custom modern entity fragment retains native light/overlay with the original
  low alpha threshold. This does not add the old dimension or hostile AI.
- Warden: original status18 starts150ticks of vertical emergence. Render offset
  in world coordinates is `-height*spawnTimer/150`. The modern scale callback
  receives an already inverted model-Y transform, so its local translation is
  positive. Probe2 exposed the incorrect opposite sign as a floating Warden;
  the corrected native frames still require review. Its ordinary ticks emit original wisps and the
  original33 smoke spirals while emerging. Guardian/Warden named states remain
  presentation state; this does not manufacture a combat/loot lifecycle.
- Pech: both held hands render from their original animated arm transforms,
  retaining left/right handedness, bow offset, sneaking and item transforms.
- Portals: lesser ACTIVE is synchronized and persists under original `active`
  tag; inactive lesser portal has no sprite. Its activeCounter advances only
  while active. Greater uses entity age. Both retain50tick growth, original
  health wobble, hurt72degree/pulse36degree modulation and status16 pulse10.
  Frame timing retains System.nanoTime/50ms and reversed16frame atlas UVs.
  These render states do not add missing portal spawner AI.

## Golems

GolemOrb has no extra flame/end-rod trail; its native impact uses the original
burst. Damage, steering, owner and expiry are unchanged. Flyer ticks use the
original dispatcher flight particles at the original movement/ground cadence.
Status5..9 use their separate original generic atlas programs, not native
HAPPY_VILLAGER/ENCHANT replacements. Shared FX implementation is audited
separately.

## Native capture contract

Opt-in `thaumcraft.worldVisualSmokeTest`, owned directory
`run/world-visual-smoke`; `WorldVisualClientSmokeTest` defines23 native1600x1000
captures. Root alone launches via the verified hidden non-input desktop.

- Rift: exposed and matched opaque-bedrock hidden/goggles/restored views in
  Fancy, then matched hidden/goggles/restored views in Fabulous. All wall blocks
  must stay bedrock. Raw rift starts at seed187,size40,stability100 and is not
  continuously reset. Shader time, animation and depth state are never set by
  the driver.
- Wisp: ambient; actual AI20tick attack/S2C; actual lethal hit/death status.
  Read-only received counts locate short effect timing; they do not certify
  visible pixels.
- Guardian near/far alpha; Warden original presentation event18 and two
  naturally elapsed emergence phases; Pech physical bow and apple in both hands.
- Lesser inactive/growing/full; greater growing and actual hurt plus original
  event16 pulse; orb flight and native block collision; raw hovering Flyer
  client-tick particles.

All actors, initial materials, arena, creative flight, initial target, catalogue
activation/status messages and raw Flyer design are explicit fixtures. This
does not repeat paid survival progression, prove boss AI, or establish all
angles, shader packs, audio or multiplayer. No driver directly supplies an FX,
sets an active FX age, or changes production render flags. Final actual native
results, source fingerprints, PNG hashes, pixel controls and independent
full-image review must be recorded by the release report before closing these
visual findings.
