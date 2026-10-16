# Grapple Gun client contract — released BETA26

Scope0.38.0,2026-10-16. This is an implementation and verification contract.
Actual executed results,source receipts and reviewed screenshots must be recorded
in VALIDATION.md and the final0.38 report. No client pass is claimed in advance.

## Primary visual authority

Released`Thaumcraft-1.12.2-6.1.BETA26.jar`,SHA256
`9425f8643581b27ff8845b087c8bc6fc10425a32942f1a3f0e265ce6b38f7b5f`,
and pinned source commit`954022bb777b7546281fb36df8522f0ba6b43f81`.
Primary classes:

- `client/renderers/entity/projectile/RenderGrapple` and
  `client/renderers/models/entity/ModelGrappler`.
- `common/items/tools/ItemGrappleGun` and
  `common/entities/projectile/EntityGrapple`.
- `com/sasmaster/glelwjgl/java/CoreGLE`,especially`gen_polycone`,
  `extrusion_angle_join`and`draw_segment_color`.

The native renderer uses the original static hook cuboids from the existing
LegacyGrapplerModel. It interpolates yaw minus90 and pitch on the original axes.
Original grappler texture and original rope texture are retained. There is no
invented entity model,GUI,energy bar,beam or developer overlay.

The original hook glow is a camera-facing square with six looping atlas frames:
U=(1+tick%6)/32,V=7/32,cell1/32,half-size.5,scale
`1+sin(tick/5)*.2+.2`,white alpha.21,brightness220 and additive blend.
The native render type provides depth testing,lightmap and fog with no depth
write. It reuses the previously audited additive shader and original particle
atlas rather than inventing a particle effect or issuing a QA-only emission.

Rope samples reproduce released`calcPoints`:interpolated owner/hook positions,
original hand-dependent first-person offset,eye-height/2.6,owner-height/2,
length×5,integer step count,the original three sine waves and amplitude weighting,
plus the support point and repeated final endpoint. Amplitude comes from actual
entity flight/pull ticks. Source GLE uses four sides,radius.025,join style1042
(angle joins,caps,path-edge normals),texture slice2 and phase length-pointCount.
Modern buffers replace immediate OpenGL GLE with a bounded four-sided tube and
angle-bisector joins. Degenerate tangents and malformed paths beyond4096 steps
are guarded; exact old GLE floating-point tessellation equivalence is not claimed.
The core centerline,scale,texture and ordinary visible rope are preserved.

The native whole tube samples the hook's packed light, so it can darken when
the anchor reaches solid stone, as observed in both final hand-pull captures.
Released RenderGrapple assigns220 to the separate glow vertex lightmap, then
draws GLE without an explicit fullbright global-light override. A fullbright
rope is therefore not inferred from the glow's attributes. Exact fixed-function
GLE lighting and per-segment illumination equivalence are not claimed.

The existing item predicate reads original byte`loaded==1`;the two original item
models and reequip behavior remain. The field is a visual flag,not an ammunition
or cooldown gate. Native inventory ticking clears it after the owner's hook is
gone. Offhand rope offsets and actual replacement hooks use the same mechanism.

## Eleven-scene native route

Opt-in`thaumcraft.grappleSmokeTest`,profile`Grapple`,directory
`run/grapple-smoke`,PNG prefix`tc6-grapple-`. Only root may launch the verified
hidden non-input desktop profile. The driver never activates a window,switches
desktops or sends global keyboard/mouse input. Normal gameplay never enables it.

Authoritative names live in`GrappleClientSmokeTest.IMAGES`:paid study,tip bench,
spool bench,gun bench,native pedestal recharge,mainhand flight,mainhand pull,
mainhand release,offhand pull,replacement hook and final release. The flight
capture is requested promptly while the actual client hook is flying;it does
not wait long enough to turn flight into a fabricated stationary scene.

The actual study C2S pays32 raw ARTIFICE theory and32 BASICS theory,one actual
valid singular legacy nugget metadata10 (mapped rare-earth nugget),and earns
10 XP. BASEARTIFICE and RECHARGEPEDESTAL are explicit completed parent fixtures.
There is no invented aspect-scan requirement. Raw knowledge is supplied once
and exact before/after debits are checked without setting the target study.

Three actual native bench transactions pay25+25+75=125 Vis and the original
Terra1/Aqua1/Aer1/Ignis1. The final gun consumes the actual paid tip and spool
taken from the player inventory;component copies are never supplied to substitute
for them. Ordinary materials,including the simple mechanism,are explicit fixtures.
This route does not claim the mechanism's separate earlier manufacturing recipe.
Every bench must end empty with exactly one newly earned physical output.

The single fresh paid gun begins with0 charge. An actual uncharged C2S use must
make no hook and cost no charge,while retaining the original use/sound behavior.
Actual C2S inserts that gun onto a supplied native recharge pedestal. Native
world ticks supply100 charge in increments of at most5,through its ordinary
100-whole-Vis recharge route. The driver's END observer records the100 increments
and actual aura before/after;aggregate aura can also undergo native diffusion.
The driver never sets charge,invokes recharge/tick directly or supplies charged
NBT. A second empty-hand C2S retrieves it as a real ItemEntity,which the player
must pick up natively before shooting.

Three accepted native shots are exercised:mainhand,offhand,and offhand refire
while the prior hook remains active. Their literal one-charge costs leave97.
Main and offhand shots must genuinely fly,hit the prepared stone wall,enter
pulling,and move the native owner at least.75 blocks with at least.2 upward
rise. Server END observation records maxima without writing position or velocity.
The same paid physical stack moves between hands only after the first hook is
released. Active hook and owner-motion state are never synthesized.

Both releases are native C2S crouch actions. Native hooks must disappear on both
sides,and ordinary inventory ticking must reset loaded to0. The replacement
trial refires from the owner's current native position with no teleport;the old
entity must be removed,one new offhand hook must remain,and charge must be97.
No direct hook discard,forced hit,owner velocity or active-hook teleport is used.

## Fixtures and proof boundaries

The uniquely named owned Survival world contains a bedrock floor,a static stone
anchor wall at Z24,raw workbench and recharge pedestal,raw precursor knowledge,
theories,study nugget,ordinary recipe materials and primal crystals. Initial
5000 Vis/zero Flux in30 already loaded chunks is supplied before consumers.
The high explicit aura fixture prevents short payment checks being altered by
lunar regeneration and does not imply natural acquisition or balance. No active
aura,charge,hook,player-motion or RNG reset occurs. Teleports only prepare camera
and launch positions while there is no active hook;actual flight and pull camera
movement follows the native player.

Required final proof is11 fresh ordered1600×1000 native PNG,all full frames and
both sheets actually viewed by root and an independent reviewer,matching executed
source receipts,hidden owned JVM native exit0,and both
`THAUMCRAFT_GRAPPLE_RENDER_AUDIT_OK`and
`THAUMCRAFT_GRAPPLE_CLIENT_SMOKE_OK: 11 scenes`.
Logs bind actual study/bench/pedestal payments,empty use,three shots,native
movement,old-hook replacement,loaded reset and actual hook/rope/glow draw counts.
This route does not prove multiplayer,client reload,entity-hit/water/portal
variants,audible sound capture,all camera angles or an uninterrupted ordinary
Survival playthrough. Common native GameTests cover separate behavior boundaries.

## First client attempt and driver correction

The first hidden attempt produced five native frames and completed the paid
study,all three bench transactions,and native pedestal charging. It stalled at
main-flight phase2 after retrieval: the server observer already read100 charge
from the player's actual held gun,but the client readiness check called
`Inventory.contains(new ItemStack(grappleGun))`. The actual mapped1.20.1
`Inventory.contains(ItemStack)` bytecode calls `ItemStack.isSameItemSameTags`,so
the charged stack cannot match the fresh untagged probe. This was a QA readiness
error; it is not evidence of failed native pickup or a gameplay defect.

After the verified owned first JVM was stopped,the sole driver correction uses
the native main-inventory stream and item identity for readiness. The existing
server assertions still require an empty pedestal,exactly one physical gun and
100 charge before launching. It neither copies/moves the dropped ItemEntity nor
invokes pickup or changes charge. This change requires a fresh hidden client
run; the stopped first attempt is not a client pass.

## Фактически завершённый QA0.38

`grapple-038-server2` прошёл1559/1559; `grapple-038-client2`11/11, оба native exit0.
Два проверяющих просмотрели все11полных1600×1000PNG и оба листа.
Реальные64Theory/nugget1/10XP/3bench125vis4crystals/paidTipSpoolConsumed,
nativepedestal100charge/physicalpickup/3shots97/mainoffmovement/refire/2shift
связаны с final logs/SHA. Современная GLE-tessellation адаптация,
подготовленные условия, ранние отказы и границы — [VALIDATION.md](../../VALIDATION.md)
и [artifact-report0.38](../../validation/artifact-report-0.38.json).
