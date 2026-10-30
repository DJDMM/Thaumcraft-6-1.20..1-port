# Native ordinary mobs and client verification, BETA26

Implementation notes for pending0.42. This is not an executed QA pass.
Authority: released `Thaumcraft-1.12.2-6.1.BETA26.jar`, SHA256
`9425f8643581b27ff8845b087c8bc6fc10425a32942f1a3f0e265ce6b38f7b5f`,
with pinned recovered source at `954022bb777b7546281fb36df8522f0ba6b43f81`.

## Production routes

Brainy and Giant Brainy retain native Zombie inheritance, armor/equipment,
vanilla animation and original `bzombie.png`. Giant anger is deliberately
not persisted; rejected hits still raise it, collision growth waits for the
anger>1 tick branch, and the original anger0 tick sets2attack damage.
The existing golem progression listener owns brain drops; no second roll is
introduced here.

Firebat has original hanging/waking, native steering/chase and explosive
melee with original fire/explosion immunity, powder drops and saved hanging
state. Pech has physical9slot pack, valued-item gathering/taming, anger,
three weapon/variant routes and original five-slot trade GUI and dice sound.
Ordered trade lists retain duplicate experience-bottle weights, original
forager-list insertion mistakes, ignored empty choices and the obsolete
damage-only potion data instead of inventing useful potion NBT.

Native Pech focus projectiles use the existing projectile implementation and
shared effect calculations with no surrogate ServerPlayer. The five default
packages and nested boolean choice distribution match `EntityPech`.
Released `FocusMediumRoot.generateSourceVector` bytecode explicitly subtracts
`0.10000000149011612D` from eye height before target direction and muzzle
offset; review identified that correction for the shared implementation.
No NPC Earth player-only terrain harvesting is granted.

Modern adaptations are the explicit biome tags and Forge biome modifier,
ownership-checked native menus, typed entity registration, native render
callbacks and bounded loaded-world projectile persistence. Original inactive
biomes are not converted into an invented survival dungeon chain.

## Prepared server and client evidence

`MobsGameTests` defines11 native checks. Besides entity attributes/migration,
anger, Firebat damage/NBT, pack conservation, tables, one-viewer trade,
ground-return conservation and wand consumable knowledge, three checks cover
NPC package boundaries, actual projectile ownership/reload/drag/gravity and
shared living damage versus player-only block harvesting. They have not been
reported as executed here.

`MobsClientSmokeTest` is opt-in with `thaumcraft.mobsSmokeTest` and an owned
new world in the separate Mobs launch directory. It prepares24 actual native
captures: Pech real dropped-item collection and physical pack, native GUI/dice
trade, native AI arrow/focus, Firebat hanging/flight, Brainy/Giant anger,
lesser portal activation/minions, knight/cleric combat, five raw taint actors,
contained finite-goo heights and real survival Bottle of Taint use/impact.

The trade offer has a raw QA-only identity tag so a legitimately returned
pack pearl cannot be mistaken for the remaining two offer items. A native
trade may untame the Pech and close the menu; the driver accepts either the
live GUI or original physical ground return. It never forces a retame or
rerolls a rejected outcome. Native dice clicks go through `PechScreen`.
Projectile captures begin from an actual client-owned projectile and confirm
its owner on the server, avoiding a stale server-only ID. A later naturally
fired projectile may be observed if an earlier one already impacted; the
driver never changes RNG or injects a projectile/effect for a frame.

Raw actors, initial gear, target health, arena, comparative NoAI actors,
seed and goo states are declared fixtures, not paid research or natural
world discovery. Marker counts and renderer callbacks are not visual
acceptance. Full PNGs and selected sheets still require actual independent
review after root runs the hidden non-input launcher. No native pass,
multiplayer, full survival, audio or all-angle rendering claim is made here.

## Rejected client2 frames and pending capture corrections

The actual `mobs-042-client2` run completed24captures/nativeexit0 after the
Pech AI goal-iterator correction. Root and an independent reviewer viewed
all24full1600x1000PNG. Its counter is not visual acceptance: scene3 was
obscured by native returned items very close to the camera; scene15's
clienttick1fireball was hidden at its caster's face; scene20 showed only
the Giant tentacle's early emerging tip; scene22's Bottle was not visually
distinguishable. Native sunlight fire also obscured the angry Giant Zombie.
Rejected frames, logs and hashed independent review are preserved in
`validation/mobs-042-client2-independent-visual-review.json`.

The opt-in driver now prepares an opaque local Zombie roof before its
first native tick, without clearing active fire or granting immunity. A
native untame/physical-return trade moves only the fixture camera away
from the drop origin, retaining all physical items and the actual outcome.
Ranged combat uses a side camera and waits for native projectiles aged at
least3ticks, moving outside their caster's physical and projected bounds.
Read-only projection checks use the actual completed world-frame matrix,
camera position/rotation, viewport margins and unobstructed native block
ray. They are capture gates, not a substitute for actual image review.
The Giant tentacle waits its unchanged height*10 emergence duration before
the settled capture, without editing age, size or animation clocks.

The Bottle fixture places its raw cow farther along the natural trajectory.
Only after the real use packet has spawned the owned physical Bottle does
the camera move to its declared side position; native speed, gravity,
throw rotation parameters, projectile age, random spread, survival debit
and impact remain unchanged. No particles, projectiles, RNG rerolls or
active state are injected for visibility. These source corrections await
root's fresh hidden client run and two full image reviews; no rerun pass
is asserted here.

`mobs-042-client3` reached the first four captures but did not witness a
qualifying scene4shot before it was stopped. Its previous heartbeat lacked
actual shot rejection reasons, so neither a projection fault nor a combat
fault is established from that run. The opt-in driver now bounds each
ranged capture to400native worldticks and records every20ticks the actual
frame projection/camera, client actor/target, all owned client projectiles,
their NDC/motion/age and exact rejecting gate. A queued server-thread
read separately records actual target health, actor AI/target/position and
physical owned projectiles. No combat or production renderer was changed.
Fresh diagnostic execution is still required; these logs prevent another
unexplained20minute wait rather than relaxing the visibility requirements.

Read-only native bytecode then identified the actual scene4capture defect:
`Camera.setRotation` stores `rotationYXZ(-yaw,pitch,0)` with forward+Z and
left+X. Applying only its inverse left every forward point at positiveZ,
which produces negative OpenGL clip.w. The opt-in capture projector now
converts that camera basis to OpenGL by negating localX and localZ. This
equals the actual GameRenderer view rotationX(pitch)*rotationY(yaw+180).
No production camera, renderer, projectile or combat behavior changed.
`validation/mobs-042-camera-bytecode.txt` and the corresponding GameRenderer
dump record the native authority. A standalone Java17/JOML1.10.5 harness
compared432representative angle/point fixtures and72forward-sign checks:
maxviewerror9.536743e-6, maxcliperror8.583069e-6, tolerance2e-4.
`validation/mobs-042-camera-basis-cpu.json` records that limited CPU pass;
actual fresh native images remain required. Bounded shot diagnostics stay
enabled for the next run.
