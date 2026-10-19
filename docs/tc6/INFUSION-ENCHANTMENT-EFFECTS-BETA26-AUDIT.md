# Eight tool infusion enchantments: released TC6 BETA26

This is the 0.40 runtime audit. The primary authority is the released
`Thaumcraft-1.12.2-6.1.BETA26.jar`, SHA-256
`9425f8643581b27ff8845b087c8bc6fc10425a32942f1a3f0e265ce6b38f7b5f`.
Recovered source at commit `954022bb777b7546281fb36df8522f0ba6b43f81`
is a navigation aid, not the authority when its decompilation differs.

The eight entries here have research `INFUSIONENCHANTMENT`. This does not
activate the unrelated enum entries with `?`, `IEARMOR` or `IETAINT` gates.
Recipe/research eligibility and altar payment have a separate progression
audit; this document covers what an actually enchanted physical item does.

## Primary checks

`javap -c -p` was read for released
`thaumcraft.common.lib.events.ToolEvents`, its deferred `ToolEvents$1`,
and `thaumcraft.api.aspects.AspectHelper`.
The runtime source aid also includes `EnumInfusionEnchantment`,
`EntityFollowingItem`, `BlockUtils`, `Utils`, and `ModConfig`.
The official Minecraft 1.12.2 client and MCP stable-39 SRG mapping establish
that `World.func_175699_k` is `getLight`: it delegates to chunk combined light
with subtraction **zero**, not block light alone or time-darkened brightness.

Modern Forge 47.4.10's mapped `IForgeItemStack.onBlockStartBreak` default
and `ServerPlayerGameMode.destroyBlock` were read to locate the narrow
post-approval item hook. Its existing default delegates to the actual
item's `onBlockStartBreak`. The native game-mode call occurs after
`ForgeHooks.onBlockBreakEvent` has completely returned and rejected any veto.

## Physical vanilla-item integration

The original handlers operate on held `infench` NBT globally, not only on
Thaumcraft item subclasses. The old port's item-only custom harvest hooks
therefore left legitimately infused vanilla tools without their effects.

A narrowly scoped Forge coremod prepends one call to the **non-obfuscated**
default method `net.minecraftforge.common.extensions.IForgeItemStack.onBlockStartBreak`.
The descriptor passed to `ToolMining.beforeVanillaBlockStart` is
`(Object, BlockPos, Player) -> boolean`. Uninfused items immediately use the
original default. Existing TC tool subclasses also use the original default,
and consequently their established item override exactly once. An actually
infused ordinary stack can enter the same custom physical harvest after the
complete native central approval. No BreakEvent cancellation/interception,
scheduled second mining action, repeated central protection event, replacement
vanilla registry item, new dependency or expanded access transformer is used.

This is a 1.20.1 integration adaptation, not a claim that the released TC6 JAR
contained this coremod. Development loader discovery and the eventual packaged
coremod resource presence must be verified separately by the root run.

The real `Block.playerDestroy` path retains native loot tables, vanilla
Fortune/Silk Touch and Forge global loot modifiers. Only newly emitted nearby
ItemEntity contents inside the bounded synchronous harvest are refined or
marked for homing. Every additional/far target passes its own fully completed
native break event, loaded chunk, world border, interaction, harvest and
unbreakable checks. A callback replacing the physical main-hand tool cannot
bill the replacement or continue spending a detached stack. Existing
BlockEntity-bearing blocks retain ordinary native harvesting and are excluded
from this custom area/far path; this is not complete area support for arbitrary
third-party containers.

The central approval retains its actual native BreakEvent until the
post-dispatch item hook consumes it. Its final XP value includes changes made
by later LOWEST subscribers; an early snapshot would silently lose those
changes. Unconsumed approvals are cleared at server END/logout. Burrowing
retains a separate released quirk: it emits no block XP, including when the
furthest target is the clicked ore. Released `breakFurthestBlock` bytecode
offset43 calls `harvestBlockSkipCheck`; that helper passes a literal true
skip-event flag, and `harvestBlock` offsets22–27 select literal zero XP. The
modern full far protection callback is retained without inventing an XP
reward that this original branch omitted.

## Released effects

| Entry | Rank | Runtime behavior |
| --- | --- | --- |
| COLLECTOR | 1 | Non-sneaking eligible harvests keep physical loot and home it toward the harvester. Player-caused living drops also home, including new Essence crystals, regardless of sneaking. It never inserts a synthetic copy into inventory. |
| DESTRUCTIVE | 1 | Non-sneaking successful eligible central harvest attempts eight effective neighbors in the plane of the last clicked face. Each successful physical block pays ordinary one-block tool durability. Sneaking uses only the ordinary central action. |
| BURROWING | 1 | Non-sneaking effective logs/ores use the original greedy, increasing-distance search: log reach2, ore reach1, X/Z limits24 and Y48. Exactly one far block is harvested, not the whole tree/vein. Sneaking mines the clicked block. |
| SOUNDING | 1–4 | Sneak-use a block with the actual infused hand: one five-durability use, original wandfail sound, authoritative dimension/position/rank packet and existing original grouped ore-ripple rendering. It is not a Thaumometer scan or knowledge reward. |
| REFINING | 1–4 | Every emitted stack gets the original world-RNG roll, including unmapped stacks. Original default conversion chance is `(1+rank)*0.125`, inclusive boundary. A successful mapped conversion preserves physical stack quantity. |
| ARCING | 1–4 | A real player attack searches around the primary target using horizontal `1.5+rank` and vertical `1+rank/2` expansion. It damages at most rank eligible additional living mobs for half the actual equipped attack damage, with the inherited attack call, retaliation enchant effects, original knockback and wind sound. |
| ESSENCE | 1–5 | Player-caused living drops can add one-unit typed crystals drawn without replacement from that victim's actual aspect pool. Starting probability is rank/5. Level5 always enters when the pool is nonempty, but the released increment quirk determines the count. |
| LAMPLIGHT | 1 | A non-sneaking successful eligible harvest schedules a check after physical removal. If the mined location remains real air and combined **raw sky/block** light is below10, it gets the existing persistent invisible glimmer, light15. There is no extra vis, item or durability price. |

Collector preserves the existing original age20 homing acceleration and
ordinary item pickup/overflow. Its original type10 selects the blue effect;
it is not a ten-tick delay. Modern expiry/logout/world-change cleanup restores
the actual item entity's previous gravity/physics flags.

Refining retains the released original iron/gold ore, cinnabar and quartz
conversions and optional tagged metals. Modern raw iron/gold/copper and
deepslate equivalents are explicit 1.20 adaptations to changed native drops.
All default TC6 special-mining scale factors for these mappings are1. The
port does not add arbitrary Essence crystals to mining loot or add a cost
for refining/collector/arcing beyond the physical action's ordinary cost.

## ESSENCE decompilation error

The recovered source prints an unconditional for-loop step
`1 + world.rand.nextInt(2)`. That is **not the released bytecode**.
Offsets500–549 in `ToolEvents.livingDrops` instead perform:

1. Increment progress by1 when a crystal was emitted.
2. Refresh the remaining aspect array.
3. Draw `nextInt(rank)`.
4. **Only if that draw is zero**, add `1+nextInt(2)` to progress.
5. Return to the condition `progress < rank`.

Rank5 can therefore produce four or five crystals; blindly porting the
decompiled for-loop incorrectly caps it lower. The implementation follows
this conditional released increment and removes one selected aspect unit
per physical crystal. The original global victim aspect definition is never
mutated. Crystal spawn height is victim Y+eye height; Collector applies to
the same physical newly added drops.

The released player-victim special case is also retained: Humanus4 plus
three aspect selections of15 from the aspect insertion order, using a
separate `java.util.Random(playerName.hashCode())`. Other victims use the
port's existing native aspect registry, including its typed powered-creeper
adaptation. This does not invent aspects for unregistered new-version mobs.

## Modern safety and presentation limits

Original Burrowing skips the far protection recheck and charges before
that harvest; original Destructive also charges before neighbor vetoes.
The modern port deliberately pays only for accepted physical targets and
fully dispatches each far/neighbor break event. This protection/conservation
adaptation is explicit and preserves the existing port's guarded behavior.
Greedy search additionally has a finite4096 bound and does not load chunks.
Existing nearby-leaf delayed ticks remain the port's modern bounded foliage
adaptation rather than scheduling every block in the original 7³ cube.

Sounding's actual requested hand is used instead of the released ambiguous
active-hand fallback. One acknowledged modern consumed use prevents a
second-hand retry and a duplicate five-durability bill. Existing TC elemental
pick has no second independent Sounding override. Native earlier cancellation,
use denial, spectator/permission checks reject the effect. Like the original
Forge attack/use handlers, this is not a universal transaction guarantee for
arbitrary later foreign attack/use listeners after LOWEST priority.

Lamplight drains queued checks at native server END after at least one full
world tick, with no forced chunk, and with loaded-position/owner/permission
validation before retaining or placing a request. Invalid worlds and non-air
positions are discarded. A native placement veto restores
our still-owned glimmer to prior air; a foreign callback replacement is not
overwritten. A successful ordinary harvest remains paid if optional light
placement is rejected. It uses raw sky light even at night; a dark cave/roof
is needed rather than an outdoor night-only condition. No light meter/debug
label is added to player UI.

Existing original grouped Sounding atlas/ripple parameters remain intact.
Collector/Arcing retain the prior modern particle feedback; exact original
following-item/slash renderers and every audio/camera angle are not newly
claimed by this runtime work.

## Verification contract

`InfusionEnchantmentEffectsGameTests` defines **32** new required tests.
They invoke actual `ServerPlayerGameMode.destroyBlock`, use-item, attack,
death/drop and native Forge callback paths. Tests cover:

- Ordinary pick/axe/shovel physical area mining, cold uninfused path, late
  LOWEST central veto, far/neighbor veto, container preservation, callback tool
  replacement, shift/ineffective bypass, and TC-subclass no-double-call.
- Actual coal ore and physical XP orbs retain a later LOWEST eleven-XP
  mutation exactly once; a separate far-ore test preserves Burrowing's
  zero-XP quirk with native loot, durability and two protection callbacks.
- Physical collector flags/ordinary pickup boundary/logout cleanup; both
  Sounding hands with five paid durability and actual S2C bytes; actual
  Arcing damage/rank/veto; actual death Essence alongside ordinary loot.
- Fixed pre-input RNG fixtures over real ore harvests checking native
  Silk Touch/raw-iron/quartz quantity conservation, partial conversion,
  typed crystals/victim pool conservation, rank5 four/five regression,
  original name-seeded player pool and canceled/offhand/nonplayer exclusions.
- Four tests wait actual elapsed world ticks for native lighting and deferred
  Lamplight: dark paid harvest/light15, raw outdoor skylight threshold,
  sneaking bypass and native light-placement veto.

The four elapsed-light fixtures hold native region tickets and wait for
actual block/entity ticking and the appropriate sky/roof lighting. They have
idempotent cleanup and a pre-timeout watchdog, including placement-listener
removal. This fixes preparation weaknesses exposed by server1; it is not a
claim that a native rerun has passed. Server2 exposed an actual modern
integration error: the same-END query could read the removed opaque block's
stale zero light, place an outdoor glimmer, then report raw15 when the strict
outdoor test checked three ticks later. Requests now retain their harvest
world tick and wait until a later world tick's END. The strict outdoor, dark,
sneaking and veto checks remain intact; the final native rerun must establish
their actual outcome. This minimum native cycle is an explicit adaptation to
modern threaded lighting, not a claim of identical 1.12 scheduling or universal
thread-completion guarantees.

Raw infused items, arena terrain/entities, initial RNG seeds, existing aspect
definitions and untracked network-recording players are declared fixtures;
these checks are not paid survival progression, multiplayer, persistence,
original audio listening or all-mod compatibility proof. Final source hashes,
actual server/client runs, packaged bridge check, reviewed native screenshots
and final pass/failure facts belong to the root validation report. This audit
does not promote a compile success to a native gameplay pass.

## Фактически завершённый QA0.40

`infusion-upgrades-040-server3` прошёл1673/1673; `infusion-upgrades-040-client4`24/24, оба native exit0.
Два проверяющих просмотрели все24 полных1600×1000PNG и оба листа.
Фактические платежи, физические предметы, потребители и точные native
observations/SHA — в [artifact-report0.40](../../validation/artifact-report-0.40.json).
Фикстуры, ранние отказы, современные адаптации и пределы —
[VALIDATION.md](../../VALIDATION.md); полного survival/WarpEvents не заявляется.
