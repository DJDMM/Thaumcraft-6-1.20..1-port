# Crimson ecology: released TC6 BETA26

Authority: `Thaumcraft-1.12.2-6.1.BETA26.jar`, SHA256
`9425f8643581b27ff8845b087c8bc6fc10425a32942f1a3f0e265ce6b38f7b5f`.
Reference navigation commit `954022bb777b7546281fb36df8522f0ba6b43f81`.
This document records implementation and required native checks. It does not
declare a completed0.42 build or QA pass.

## Lesser portal

The original `EntityCultistPortalLesser` remains at
`thaumcraft:cultist_portal_lesser` with its existing original sprite renderer.
It has100HP,4armor,1knockback resistance and10XP; it is immobile, unpushable,
immune to fire, fall damage and potion addition, and persists without
distance despawn. Peaceful removes the monster. Every10ticks an inactive
portal checks for the nearest eligible player within32 and starts its
original growth animation and craftstart sound.

The stage counter starts100. The active `stagecounter-- <= 0` predicate
therefore checks on the101st active server update; subsequent checks reset
to50..99. A player must remain within32 and in sight. Nearby native cultists
in the32-block inflated cubic bounds count against the original2/4/6
Easy/Normal/Hard cap. It creates a Knight when the next float exceeds.33,
otherwise a Cleric, with the original triangular X/Z offsets and Y+.25.
A successful physical insertion emits the20 original white-to-red atlas
particles and wandfail sound, then deals5..9 out-of-world damage to the
portal. Rejected modern Forge insertion cannot charge health or leave an
orphan. This transactional callback behavior is an explicit modern
hardening; the old `World.spawnEntity` return value was ignored.

Player overlap with squared distance<3 deals4 indirect-magic damage.
Death makes the original1.5-strength entity-damaging, terrain-preserving
explosion. Reentrant callbacks cannot repeat it. Save data persists only
`active`; the counter100, growth and pulse restart on load as in BETA26.

## Cultists

Knight30HP and Cleric24HP share original speed.3, range32, base attack4,
10XP, no item pickup, team exclusions, melee/swimming/door/home/wander/watch
goals and retaliation that alerts nearby cultists. Native instances undo the
visual base's old NoAI and artificial persistence. Genuine named/persistent
native saves are retained; catalogue saves migrate without false persistence.

Knights use original crimson plate/boots, iron sword,1% ordinary or5% Hard
rare weapon branch, one-fifth Void sword/robe helmet, otherwise Thaumium
sword and optional bare head. Held-weapon enchantment uses the original
local-difficulty expression. Clerics use original robe equipment and10%/30%
boots. Their2..24 ranged route uses the existing native red Golem Orb when
the random float exceeds.66, otherwise three vanilla SmallFireballs, with
original direction, leading, speed, spread and sounds. The original melee
fallback remains.

Ritualist and Home coordinates persist. Home also synchronizes for the
original client-side ritual gaze. The original altar block is not active
in BETA26; no TC4 ring, portal or dungeon is fabricated. Modern door
navigation uses native OpenDoorGoal because the old1.12 VillageDoorInfo/
EntityAIRestrictOpenDoor system has no1.20 equivalent.

The renderer base deliberately suppresses vanilla equipment death drops.
Native cultists restore the actual1.20 Mob loop: original armor chance.05,
normal main-hand chance, recent-hit/guaranteed conditions, Looting, Vanishing,
native random wear and clearing transferred equipment. The original
cultist loot table separately drops gold and a1.25%+.01×Looting Crimson
Rites roll.

The original `EntityEvents.livingDrops` extra book roll is separate:
a real nonfake player kill gives1/4 before `!CrimsonCultist@2`,1/20 after
that stage, or1/50 while an exact-NBT unmodified book is in mainInventory.
Offhand, armor and renamed/NBT-modified books do not meet the original
`InventoryUtils.getPlayerSlotFor` predicate. The physical drop never grants
research or knowledge. Its original curio consumer remains authoritative.

## New-chunk world generation

The feature runs once per new overworld chunk through the normal feature
pipeline; it does not retrofit chunk-loads or modify manual saves.
Superflat and disabled `generateStructure` skip it. X/Z each use chunk origin
+8+uniform[-4,4]; the modern motion-blocking surface represents the original
precipitation surface. The first1/100 branch attempts the original mound.
Only when that roll fails is the independent1/500 Lesser Portal roll made.
An invalid mound does not fall through to a portal.

The original ground/rock/sand/snow material gate is represented by the
extensible `thaumcraft:crimson_portal_ground` tag because1.20 removed the
old Material classification. Entity creation/finalizeSpawn uses the real
native portal, while WorldGenRegion queues its protochunk entity rather
than mutating ServerLevel from a generation worker.

`WorldGenMound.java` contains2441 literal layout writes before its two
random treasure blocks. All2441 are retained in exact original order in
`data/thaumcraft/crimson/mound_blocks.json`, with the pinned reference
source SHA `3ee825d23bf44cf40816681ba4f3853b301bdc67278a1a8c738c977529dea824`.
Default stair states remain distinct from explicit metadata0. The19×16×19
plan, five original surface predicates, two crate/urn rolls and their
rarities, one-third trapped chest/TNT, simple-dungeon chest loot and
Skeleton/Zombie spawners are retained. Entire bounds are checked before
writing; unloaded chunks, build boundaries and existing block entities
veto it. No generation creates extra chunks on demand.

All six crate/urn IDs now use actual fragile partial shapes, original
hardness.15/no tool requirement, original urn sound and LootBagItem's
already-ported original reward selector. Silk Touch preserves the block;
ordinary drops make1+tier+random[0,2] rolls and ignore Fortune. The legacy
per-block independent Random becomes the native level RandomSource; exact
old-world RNG history is not claimed.

## Verification contract

`CrimsonGameTests` covers native attributes/armor/team/persistence migration,
real minion insertion and health debit, cancelled insertion, portal
stationary/fire/potion/collision behavior, scan-gated exact inventory book
rarity and fake-player rejection, physical equipment drops/Vanishing,
actual mound/chest/spawners/treasure, loot tiers/Silk Touch and an actual
world-ticked portal activation/spawn sequence. The latter owns a separate
ticketed high arena and waits for real entity-ticking readiness; tight
manual entity loops do not stand in for AI ticks. Every fixture restores
terrain/block entities and removes its owned actors and drops.

The native/server/client result belongs in the root's final version report
only after execution and actual screenshot review. Prepared knowledge,
actors, terrain and direct callback tests do not establish natural world
discovery, full survival, multiplayer, audio output, every camera angle or
a complete remainder of the mod.
