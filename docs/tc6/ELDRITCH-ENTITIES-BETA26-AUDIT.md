# Registered Eldritch and greater Crimson entities — 0.42 preparation

This is a source/bytecode implementation audit, not an executed gameplay pass.
The accepted 0.41 release is not evidence for these new runtimes.

Authority: released `Thaumcraft-1.12.2-6.1.BETA26.jar`, SHA256
`9425f8643581b27ff8845b087c8bc6fc10425a32942f1a3f0e265ce6b38f7b5f`.
Recovered source commit `954022bb777b7546281fb36df8522f0ba6b43f81` was
cross-checked against `javap -c -p` of the seven released entity classes.
The local diagnostic dump is `work/eldritch042-released.javap.txt` in the
workspace root. Original class names are `EntityEldritchCrab`,
`EntityInhabitedZombie`, `EntityThaumcraftBoss`, `EntityEldritchWarden`,
`EntityEldritchGolem`, `EntityCultistLeader`, `EntityCultistPortalGreater`.

## Access and registration

All original registered IDs remain. Inhabited Zombie uses a typed vanilla
Zombie subclass, native Zombie renderer/armor and original `czombie.png`.
The crab and four bosses retain the existing renderer-compatible IDs while
executing actual native AI/combat. The seeking `golem_orb` was already native
in the accepted golemancy implementation: it is reused unchanged, including
red/nonred damage, finite lifetime, distance-squared steering, deflection,
original sprite/impact and deliberately unpersisted red/target values.
The original `special_item` now also reuses the already-native
`FocusExchangeItemEntity` implementation under the original ID, keeping the
existing `focus_exchange_item` compatibility registration. Boss/greater-portal/
Giant pearl rewards use its exact compensated gravity, positive-Y damping and
explosion immunity, original .1Y/zeroXZ initial motion and default pickup delay.
The helper respects Forge captured-drop ownership and does not insert a reward
that is already owned by the current death-drop collection. Bags remain ordinary
physical item drops. No second SpecialItem simulation is introduced.

No TC4 dungeon, Outer Lands dimension, or invented natural boss chain is added.
BETA26 registers these dormant encounter entities but does not provide an
active natural access route. Commands/eggs do not establish survival access.
The original inhabited Zombie spacing predicate is retained, but no arbitrary
Overworld spawn list is invented.

## Crab and inhabited Zombie

Crab: 20HP, 4damage, speed .3, 6XP, arthropod/Poison immunity, original
float/leap/melee/wander/retaliation/player/cultist goals. Helmet supplies5armor;
half-health damage removes it and emits five original chest-item fragments.
The original speed construction quirk remains: helmet setters do not recalculate
the initial attribute, while helmet removal explicitly sets .3. Its original
.275 branch is not silently turned into an invented live armor-speed toggle.
Hard spawn always helmets; other difficulties use .33. The original hard-only
Spider group effects and `helm` persistence remain. An airborne bare crab
above a living target can ride it, attack every10..19ticks and dismount at .2.
Only a recent player hit permits the original Ender Pearl loot roll.

Inhabited Zombie: native Zombie inheritance, 30HP, 5damage, no reinforcements
and no villager conversion callback; original crimson armor spawn chances.
Its overridden immediate death phase releases exactly one helmeted crab at eye
height, emits20original explosion particles, handles credited XP and removes
the Zombie. Normal death/equipment/flesh drops are intentionally suppressed.
The server owns child insertion, with a one-shot guard against duplicate calls.

## Common boss and Warden

Common boss: purple darkening bar,50XP, .95knockback resistance,40follow range,
24block home on spawn, permanent no-distance-despawn, spawn-timer AI freeze
and invulnerability, underwater breathing, web immunity, heal1/30ticks.
Incoming raw damage above35 triggers200ticks of anger with the original
damage-derived Regeneration/Strength/Haste amplifiers and is capped at35.
The released aggro iteration has incorrect `hei`/`ld` indexing; the port
preserves it rather than claiming an improved aggro algorithm. Its five
50HP/.5damage multiplayer modifiers and proportional health adjustment remain.
Bosses receive no ordinary +100HP/*3 champion buffs. Merely registering one
does not force an original champion roll that BETA26 never performed.
Ordinary boss deaths drop a pearl and rare bag; the Praetor overrides this to
the rare bag plus ordinary equipment drops. Home persists; anger, spawn timer,
aggro map, magazine and frenzy are deliberately transient as in the release.

Warden:400HP/10damage/.33speed/4armor,150tick emergence and264absorption field,
separate blue notched bar,1field point/25unhurt ticks up to baseHP*.66. The
first successful hit exhausting absorption starts the once-only150tick
invulnerable field frenzy. It uses the original home teleport/event,
expanding timed Sap rings, four foot Sap positions and original block arcs.
The original teleport probe is not recomputed inside its failed-attempt loop;
that counterintuitive released quirk is retained. Ranged choice is80%native
Eldritch Orb and20%sonic (line of sight), including arm callbacks, knockback,
400tick Wither/Weakness and3..5temporary player Warp. Drown/Wither are rejected.
The fifteen original titles and title byte persist.

## Golem, Praetor and greater portal

Golem:400HP/10damage/.3speed/6armor, fire immunity,100spawn ticks with heal2
per tick. Raw incoming damage strictly greater than current health triggers
its headless transition before the common35damage cap; it returns false,
explodes without terrain damage and starts the second100tick heal phase.
Headless state persists. Melee has10tick cadence, .75attribute damage,
.2vertical impulse and additional headless knockback. Its magazine recharges
one/tick to150; attacks debit15..19 and reuse real nonred seeking orbs. Charging
prevents firing. Original rare sparks, vent, arcing callbacks, fragile-block
clearing and attack/model timers remain. The original100vs150 spawn-message
condition and unchanged block probe in the client arcing search are preserved.

Praetor:150HP/5damage/.32speed,40XP, original equipment/enchantment rolls and
fifteen saved titles; original cultist team/retaliation. Nearby cultists in the
original8block cuboid receive60tick RegenerationII, including its corner
positions. The ranged minimum16/maximum24 attack uses actual red seeking orbs.

Greater portal:500HP/5armor/30XP, stationary/fire/potion/fall immunity, red
notched darkening boss bar and fullbrightness original renderer. Stage0 starts
at200ticks: four correctly rotated crimson banners appear at160 remaining;
original subsequent intro crate probabilities and block arcs remain. Nearby
players within48 advance the original minion timing/waves and stage12Praetor.
Cultists receive32block home. Later minions cost5..9portalHP after successful
native insertion. It heals1/tick before stage12, deals8indirect magic contact
damage, and dies with a strength2 non-destructive explosion and pearl.
Only `stage` persists; the200counter restarts on reload as in BETA26.

## Original effects and modern seams

`EldritchArcClient` ports actual `FXArc`, not the distinct `FXBolt`: original
ballistic velocity with .115gravity and .25noise, maximum50intermediate points,
`beamh.png`, two crossing .125wide strips, original strip triangle diagonal,
3tick fade, light220, additive blend/depth-test/no-depth-write, and original
delayed atlas sparks. Modern rendering expands strips into explicit triangles
through the existing native particle target/shader. Degenerate/nonfinite input
and finite cache size are guarded; dimension-bound S2C dispatch is limited to
nearby players and packet handling runs on the client thread.

Server authority replaces original client-side ghost block writes, and unloaded
chunk checks avoid generating terrain during effects. Forge insertion vetoes
cannot consume finite portal health. Catalogue-shell NBT migration removes
its old forced NoAI state; genuine newly saved NoAI is preserved where the
original entity permits it. Hidden native rendering and actual pixel review
remain required; resource identity or a counter alone cannot accept visibility.

Nine new `EldritchGameTests` cover helmet break/immunity/save, one-shot death
replacement/no loot, damage cap/home/transience, spawn field/frenzy, headless
magazine/save, cuboid support/red native orb, four-banner timing and saved-stage
boss/minion health debit, and physical SpecialItem reward ownership/fullNBT/
gravity/explosion immunity. They are implementation fixtures, not a full survival
run, and have not yet been executed at this audit's creation.

The separate opt-in `thaumcraft.eldritchSmokeTest` driver requests17native
captures in its new owned world. It uses raw actors, initial comparison NoAI,
original spawn/equipment callbacks, high-health Cow targets and declared native
fixture strikes. Crab armor is broken by a real damage pipeline; inhabited
death releases its actual child. Boss clocks advance naturally, with no timer,
stage, magazine, frenzy, RNG or particle injection. The Warden's shield is
removed by spaced real hits, the Golem phase uses its raw lethal-hit callback,
and all ranged captures require owned native AI projectiles. The greater portal
actually runs through its thirteen stages to the spawned Praetor; it is never
advanced by a test setter. Arc captures additionally require a currently live
arc and a real draw, rather than an accumulated effect receipt alone. These
requested scenes remain unexecuted until the root's hidden native QA and two
actual image reviews; their marker names do not themselves constitute a pass.

Before its first actor capture the driver now waits at least60 actual world
ticks and60 client ticks, the received native arena floor, three compiled
floor/corner meshes and read-only skylight readiness. Waiting advances the
ordinary world; it neither pauses bosses nor edits any active clock.

The three ranged scenes use a declared side camera instead of lining the
camera up behind the caster. Only owned native projectiles aged at least3
ticks, still moving and physically outside their caster's bounding box may
qualify; the same conditions are checked again in the actual capture frame.
No projectile is slowed, moved, spawned or aged for visibility, and no
projection-matrix helper is used. A qualifying frame still requires actual
independent PNG review. Each ranged wait remains bounded at400 world ticks;
every20 ticks without qualification logs the actual client/server actor,
target, owned projectiles and, for the Golem, native magazine/spawn state.
These are fixture/read-only capture changes awaiting root's native execution,
not an accepted runtime or visual result.
