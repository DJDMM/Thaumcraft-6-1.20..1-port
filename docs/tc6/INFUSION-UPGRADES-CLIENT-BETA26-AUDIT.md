# Infusion upgrades: native client contract

Scope: original **Thaumcraft 6.1.BETA26**, Minecraft1.20.1/Forge, stage0.40.
This document describes the opt-in client scenario. It does **not** report
a successful run. Actual source fingerprints, logs, screenshots, failures,
reviews and exit codes belong to `VALIDATION.md` and the final artifact report.

The released JAR is the authority. Research and recipe definitions are audited
in `INFUSION-UPGRADES-PROGRESSION-BETA26-AUDIT.md`; global effects are audited
separately. This client does not import mechanics from TC4 or TC5.

## Isolation and evidence

`InfusionUpgradesClientSmokeTest` is a `Dist.CLIENT` subscriber guarded by
`thaumcraft.infusionUpgradesSmokeTest`. It creates a uniquely named Survival
world, guards ownership before work, and stops its own Minecraft instance.
Only the checked hidden non-input Windows desktop launcher may run it.
There is no window activation, desktop switch or global keyboard/mouse input.
Sounding's sneak action changes only this client's `KeyMapping` state.

Native screenshots are1600x1000. The driver refuses to overwrite an existing
capture. Scene names and logging markers describe QA evidence; they are never
drawn as gameplay labels. It uses the ordinary Thaumonomicon, inventory,
Curios menu and world renderer, with no test gallery or developer HUD.
Paid tool, runic and worn helmet inventory frames hover the actual native slot
so its real item tooltip is visible. Only the owned Java `MouseHandler`
`xpos/ypos` fields are changed, using actual native slot/screen coordinates;
the prior values are restored on close/failure. This never moves an OS/GLFW
cursor or sends input to another window. Tooltip text and pixels still come
from ordinary inventory rendering and the production item tooltip hooks.

## Payments and physical ownership

The three studies use real research C2S requests and strict prepared parents:

| Study | Native payment checked | Other transition checked |
| --- | --- | --- |
| INFUSIONENCHANTMENT |64 raw Theory Infusion |+10 earned XP |
| RUNICSHIELDING |32 raw Theory Infusion and one amber |+10 earned XP |
| FORTRESSMASK |32 raw Theory Infusion |+10 XP; permanent Warp1 and normal Warp1 |

The malformed original placeholder item requirement is ignored by the normal
research parser. The fixture does not invent or pay a repaired placeholder.

Four ordinary arcane altars are40blocks apart, with symmetric physical
pedestals, candles and stabilizers. Two batches run four enchantments in
parallel. Every altar starts with real C2S caster clicks and then advances
through ordinary world/block-entity ticks. No matrix state, counter, stability,
recipe queue or payment field is injected during a craft.

| Infusion | Target | Essentia at initial level |
| --- | --- | ---: |
| Collector |vanilla diamond axe |180 |
| Destructive |vanilla diamond pickaxe |450 |
| Burrowing |vanilla diamond axe |230 |
| Sounding |vanilla diamond pickaxe |100 |
| Refining |vanilla diamond pickaxe |140 |
| Arcing |vanilla diamond sword |100 |
| Essence |vanilla diamond sword |100 |
| Lamplight |vanilla diamond pickaxe |100 |
| Runic0→1 |Fortress chestplate |80 |
| Runic0→1 |mundane ring |80 |
| Fortress goggles |same physical Fortress helmet |80 |
| Grinning Devil mask |same helmet |180 |
| Angry Ghost mask |same helmet |180 |
| Sipping Fiend mask |same helmet |180 |

Total contract: **14 native infusions,2180essentia,40physical reagents**.
Initial source jars contain the aggregate original costs for each altar plus
one surplus per distinct aspect at that altar. A251-unit source is split into
legal jars. Exact typed source balances and each reagent/container remainder
are checked after payment; the final expected surplus is one of each initial
altar/aspect pair. Milk's physical empty bucket is conserved.

The eight tool inputs are unenchanted vanilla items. No infusion tag is given
to them by the driver. The runic armor and ring start without `TC.RUNIC`.
The helmet starts with damage17 and unrelated `ownerMemory:Silverwood` NBT.
All four modifications preserve its current unrelated NBT and current damage,
including real wear caused by the native Angry Ghost damage scene.

Each output is taken through the real pedestal C2S interaction and physical
ItemEntity pickup. Paid tools remain owned throughout their effect scenes.
The same helmet is equipped by ordinary armor Quick Move C2S after each
mutation, then physically unequipped before the next infusion. The paid
chestplate uses the native armor menu, and the paid ring uses the actual
Curios inventory button and noncosmetic ring slot C2S.

## Native consumers

- Runic shielding observes ordinary production wall-clock ticks. The paid
  chestplate and paid Curios ring restore two absorption points by spending
  exactly two aura vis. No injected timer, absorption or charge is used.
- Collector mines one physical log using a paid vanilla axe; the log follows
  and enters the player's real inventory. Original ordinary durability1 is
  checked.
- Destructive mines a real3x3stone wall using a paid vanilla pickaxe and checks
  all nine blocks plus nine durability. Burrowing selects the farthest of
  three physical logs using a paid vanilla axe, retaining the lower two and
  paying one durability.
- Refining mines ordinary iron ore through actual Survival C2S until a physical
  native cluster appears, with a bound of32ores. No RNG is forced. A bound
  being exhausted is a failure, not an excuse to grant a cluster.
- Arcing uses a paid vanilla sword and actual attack C2S against a primary
  controlled cow, then verifies native secondary cow damage. Essence uses its
  independently paid sword and real cow deaths until a typed aspect crystal
  appears, bounded by160actual attacks. No RNG or target health is reset.
- Sounding uses a real sneaking block-use C2S with the paid vanilla pickaxe,
  checks exactly five durability and an unchanged target, and captures native
  server-to-client ore ripples during their visible interval.
- Lamplight mines a real stone in a physically enclosed dark room. Combined
  raw sky/block brightness is below10; night alone is not substituted. The
  native END effect after a full world tick places a light15glimmer, with only ordinary mining
  durability1 and no extra vis payment.
- Angry Ghost is verified through a real controlled vanilla zombie attack12:
  its original probability boundary guarantees Wither80 without an RNG seed.
  A separate controlled zombie attack60 wounds the fully armored player before
  combat, so Sipping Fiend can be observed through real `LivingHurtEvent`
  before/after health readings. If ordinary recovery restores full health before
  a mask heal is observed, another real controlled attack is permitted only
  after native invulnerability frames expire. The entire route allows at most
  seven such native wounds, including the first. It stops only after an actual
  Sipping heal and typed Essence output are observed, or fails at its bounds.
  `NATIVE_SIPPING` records the actual `controlledNativeWounds` count; the evidence
  helper independently binds it to1..7. No player health, hunger, RNG or timer
  setter forces the effect. These attack values and bounded controlled actors
  are explicit fixtures, not ordinary zombie statistics.

## Scene and marker contract

The24scenes are the three studies, eight paid enchantments, two runic
outputs, four worn helmet modifications, native Curios/runic shielding,
Collector, Destructive/Burrowing, Refining, Arcing/Essence, Sounding and
Lamplight. Each capture comes from its actual client framebuffer.

Required logs use the `THAUMCRAFT_INFUSION_UPGRADES_` prefix:
`STUDY_PAID`×3, `ALTAR_PAID`×14, one marker for each native consumer,
`CLIENT_FACTS`, `RENDER_AUDIT_OK`, `CLIENT_SMOKE_OK` and24
`CLIENT_SMOKE_IMAGE` paths. The final facts include actual paid essentia,
native shield credits, mask effects and bounded random-effect attempts.
The success marker is emitted only after final ownership/resource assertions
and all screenshots have been saved.

## Prepared conditions and limits

Prepared strict parents,128raw Infusion theory, research amber, vanilla tool
inputs, component materials, previous-stage Fortress pieces and mundane ring,
four stable altars, initial typed essentia, initial aura, loaded arena chunks,
mining targets, enclosed room and controlled no-AI vanilla actors are fixtures.
Camera/player positioning uses owned-server teleports; the driver does not
claim ordinary walking, exploration or resource acquisition.

No active aura, charge, absorption, health, matrix counter, target clock or RNG
setter is used. Native shielding, damage, healing, tool wear and item changes
remain observable gameplay consequences. This scenario is not full Survival
progression, independent multiplayer, save/reload, every modded tool/Curios
configuration, all viewing angles, audio parity or full WarpEvents proof.
Grinning Devil's integrated severity consumer is covered by server contracts;
the client does not manufacture a full WarpEvent merely to claim one.

## Retained first automatic client failure

`infusion-upgrades-040-client1` is a failed attempt, despite Minecraft exiting
normally with native code0. The opt-in `CLIENT_SMOKE_FAILED` marker records an
actual NullPointerException in the scenario's essentia-source inspection; neither
CLIENT_SMOKE_OK nor RENDER_AUDIT_OK was emitted. PID28080 owned16 hidden-desktop
windows and the input desktop remained Default. No manual process stop occurred.
The structured `validation/infusion-upgrades-040-client1-state.json` binds the
original log, native run/desktop summaries, launcher capture, source snapshot and
four original1600x1000screenshots. Its executed driver SHA256 is
`43a65ecb6231878d601e43418fd129e3694cc6bc07fe24cf04d14d111cf48950`.
The partial log contains three paid studies and only one verified Collector
infusion180ess/two reagents; it does not establish the remaining route.

The exact cause is fixture geometry: altar1 at(20,114,-20) had its typed source
jar index5 at(14,112,-12), while the prepared iron-ore grid also wrote that block.
This was an opt-in scenario source-jar replacement before use, not a failed
production essentia mechanic. The ore grid now occupies X=-12..-5 and cannot
overlap those jars. A pure preflight after all terrain fixtures checks every
recorded jar block entity and aggregate initial typed balance; source inspection
also asserts the exact missing position/type instead of dereferencing null.
These changes affect only the opt-in Dist.CLIENT driver. Core/resource/test/helper
fingerprints remain bound separately to the final native server execution.

The four native frames were copied, byte-verified and recorded before removing
only their exact owned QA screenshot paths for the next unique run. Raw logs and
the failed world's isolated QA save remain retained. The replacement
client2 driver SHA256 is
`1f795d097f98a549e63d288bfb9fbae5af4950337142a50fbfac1c463847cd97`.
This correction and a new launch do not claim that client2 has passed; its final
native observations, frames, reviews and exact source bindings are still required.

## Retained manually stopped second client

`infusion-upgrades-040-client2` was explicitly stopped after its heartbeat remained
at scene15 phase7. Three paid studies, thirteen paid infusions/2000essentia,
fifteen actual full screenshots and the native Angry Ghost Wither effect had been
recorded. It emitted neither a failure marker nor either success marker. This is
a stopped incomplete attempt, not an automatic failure or a passed native run.

The opt-in mask scenario unconditionally entered its exact-position readiness
gate before every phase. The real controlled zombie hit knocked the wearer away;
the subsequent phase7 capture could not satisfy that gate. The correction only
limits this gate to phases0..3 before the physical output pickup. Native item
wear, worn mask tags, actual damage, Wither and knockback remain observable.
No production mechanic is changed by this driver correction.

The stop receipt binds PID29724 to the exact Java17 executable, unique captured
argument file and owned hidden desktop. Native termination code4294967295 is
retained; sixteen owned windows were observed and the input desktop stayed
Default. `validation/infusion-upgrades-040-client2-state.json` binds the original
log/run/desktop/stop bytes and executed driver SHA256
`1f795d097f98a549e63d288bfb9fbae5af4950337142a50fbfac1c463847cd97`.
All fifteen original1600x1000frames were copied and byte-verified before removing
only their exact logged QA screenshot paths for the next unique attempt. The
original log and isolated QA world remain retained. A further launch must supply
its own complete native observations, frames, reviews and source bindings.

## Retained manually stopped third client

`infusion-upgrades-040-client3` completed all fourteen paid infusions/2180essentia,
the actual Curios/armor two-credit shield payment and native Collector,
Destructive, Burrowing and Refining observations. Refining produced a physical
cluster after two ordinary ore actions. The driver then remained at scene20
phase5 without a failure marker or either success marker; later combat,
Sounding and Lamplight observations are not established by this incomplete run.

After native Refining succeeded, the callback incremented its attempt index.
The next client invocation calculated the next ore position and entered that
position gate before returning the already observed success. The player remained
at the just-mined ore. Only the guarded opt-in driver now returns its actual
phase5 result before computing a future target; native mining, physical cluster
output, durability, RNG and all payment checks are retained.

The exact owned hidden PID29572 was explicitly stopped, preserving native exit
code4294967295 and an unchanged Default input desktop. The original log, run,
desktop, exact Java17/argument-file stop receipt and executed driver SHA256
`c53c2fb5edf77313185d3bb26bb97a58b15dcc05bfc972cc10fef7e471168da1`
are bound by `validation/infusion-upgrades-040-client3-state.json`. All twenty
original1600x1000frames were copied and byte-verified before removing only their
exact logged QA screenshot paths. This retained driver stall is not a gameplay
failure or a native pass. The next attempt requires its own complete evidence
and a fresh actual review of all twenty-four frames and both overview sheets.

## Фактически завершённый QA0.40

`infusion-upgrades-040-server3` прошёл1673/1673; `infusion-upgrades-040-client4`24/24, оба native exit0.
Два проверяющих просмотрели все24 полных1600×1000PNG и оба листа.
Фактические платежи, физические предметы, потребители и точные native
observations/SHA — в [artifact-report0.40](../../validation/artifact-report-0.40.json).
Фикстуры, ранние отказы, современные адаптации и пределы —
[VALIDATION.md](../../VALIDATION.md); полного survival/WarpEvents не заявляется.
