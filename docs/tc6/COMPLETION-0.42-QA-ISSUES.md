# Pending 0.42 native verification

Accepted 0.41 is preserved. This ledger prevents repeating completed work or
mistaking an implementation census for gameplay acceptance. No 0.42 release
has been accepted, packaged or pushed.

## First full server run

`completion-042-server1` completed 1781 tests, with 18 required failures;
native exit 18, Gradle exit 1. Exact log and pre-run fingerprints are in
`validation/`. Its world was archived from the previous QA run before launch;
manual saves were not touched. Random champion joins were disabled through
a declared detached memory-only original option; explicit champion tests
remain active, with config bytes preserved.

| Finding | Initial diagnosis / correction; later native results are recorded below |
| --- | --- |
| Goo evaporation | Modern same-block onPlace rescheduled it; block-owned random/neighbor callbacks and explicit AIR removal also required. Exact finite math/cadence retained. |
| Original living factories | Original SpellBat ID was still a catalogue shell although paid spells were native at a compatibility ID. Both IDs now share the guarded native runtime. |
| Barrier paving arcane payment | Test put a 2×2 recipe into consecutive 3×3 slots. Correct row-stride fixture. |
| Late mob scans | Test looked up typed Zombies in the VisualMob factory map. Query the actual registry; retain ordinary-Zombie negative controls. |
| Temporary Warp decay | Old test assumed the newly active random event scheduler could never debit its counter. Initial zero-activity fixture isolates decay; separate Warp tests retain event coverage. |
| Curse Sap roster | Same typed Inhabited Zombie registry lookup issue; original Giant tentacle also now carries its IEldritchMob marker. |
| Siphon migration | Fixture set disabled state without redstone, so native onPlace correctly recomputed it. Use an actual initial redstone source. |
| Cultist equipment drops | Distant entity sections were still HIDDEN despite successful insertion. Wait native readiness; retain exact physical count/Vanishing/source ownership checks. |
| Pech negative scan | New native Pech correctly grants its own scan. It must still grant neither wand nor Firebat facts. |
| Portal minion payment | Same HIDDEN section race; separately witness one exact original 5–9 debit and one native visible minion. |
| Paving study | Original invalid arcane_stone item craft descriptor is ignored by released parsing; remove the invented display alias, preserve valid costs. |
| Undead immunity | Same typed Zombie registry lookup issue, with exact native health retained. |
| Void golem material | Old test expected even completed Void study to remain unimplemented. Verify completed manufacture and retain started-study rejection. |
| Twelve paid infusions | Fast consecutive raw fixtures inherited the real-time source retry after adding a new sixth jar. Use independent initial native altars, without changing active clocks/caches or production costs. |
| Cultist follow range | Native finalizeSpawn adds a random range modifier. Assert exact original base 32 and the actual native modifier contribution separately. |
| Sprayer target | Distant target section was not query-visible. Native ticking/query readiness was added, but Server2 still fails the combined charge/healing assertion; visibility alone does not explain the remaining failure. |
| Portal overlap damage | Native detached-player initial spawn protection blocked the fixture's damage. Only the initial test player protection counter is set to zero; production damage remains unchanged. This test passes in Server2. |
| Infusion census | Historical expected set contained only 44. Exact original set now contains all 56; no recipe is inferred merely from research count. |

These initial diagnoses do not themselves establish passed native execution.
Server1 evidence is retained; the following run records the actual rerun result.

## Second full server run

`completion-042-server2` completed **1784 tests: 1782 passed and two required
tests failed**, with native exit **2**, Gradle exit **1**, in **10m 37s**.
The exact log, exit receipt, archived QA-world receipt and 4814 pre-run source
fingerprints remain in `validation/`. This is a rejected release gate, not a
successful 0.42 build. Manual saves were not touched.

The only reported failures are:

- `primeSeedEventReplacesRiftAndPaysIntegerHalfPollution`: the old assertion
  combined event outcome with rift removal, so the log cannot identify the
  rejected eligibility gate. Its Gaussian candidate lies outside the owned
  3x3 template without prepared body chunks. The pending fixture repair owns
  an upper vertical arena, tickets and waits for the original seed-spacing
  query chunks, and separately asserts eligibility, event outcome, removal
  and payment. It preserves the initial 92173 sample, exact native placement,
  150 health, boost 21, integer +10 Flux and zero stability cost. No production
  rift/seed change is justified by this log; the new fixture awaits Server3.
- `sprayerPullsEachOriginalAspectFromPhysicalJarThenSpraysOnlyOnPowerEdge`:
  native target-query visibility now passes, but the combined one-charge /
  full instant-Healing assertion still fails. The source now separates
  actual neighbor signal, enabled state, charge debit and target health
  diagnostics. The remaining cause and correction are pending; neither
  successful firing nor an exclusively fixture-related cause is claimed.

All other required tests in this run passed, including the corrected portal
overlap fixture and the new native Pech gather regression. Passing isolated
tests does not establish full survival, visual or multiplayer acceptance.

## Pech client failure

`mobs-042-client1` crashed after its first wild-Pech capture with an actual
`ConcurrentModificationException` in `GoalSelector.tickRunningGoals`.
Gathering a valued pearl tamed the Pech and immediately replaced combat goals
inside that same goal iterator. `PechEntity` now defers that replacement until
the current native entity tick has finished; changes outside goal execution
remain immediate. Original taming chance, physical payment and loot are not
changed.

`nativeGatherGoalTamesWithoutMutatingTheRunningGoalIterator`, the twelfth mob
test, passed in Server2 through actual world ticks and physical pearls. This
server regression does not replace the still-pending fresh Mobs client run
and full screenshot review. The rejected client log, crash and lone capture
are retained.

## Warp clients

`warp-042-client1` failed during actual registry initialization: Forge invoked
MobEffect.initializeClient before subclass kind was assigned. Icon kind now
resolves at rendering time.

`warp-042-client2` produced 15 frames and native exit 0 but was rejected:
the first control was captured before native chunk meshes settled, blur/HDR
went dark because PostChain lacked the native renderer's GL preconditions,
and its completion marker omitted the runner's required space. No visual
acceptance follows from its native counter.

`warp-042-client3` completed 15 frames with native exit 0, 16 owned hidden
windows and unchanged input desktop. Both reviewers viewed all 15 full PNGs.
The first root review accepted the captured route while explicitly withholding
release acceptance; that original SHA-bound JSON remains unchanged.
Independent review subsequently rejected black horizontal horizon bands in
Hunger-active and Mist-active. The run is therefore rejected despite its
completion marker and earlier root route assessment.

The diagnosed copy pass alpha-composited native horizon pixels with zero
alpha onto a cleared target, losing their valid sky RGB. A replacement
ONE/ZERO copy using native blit shaders is now in source, together with
before/after native RGBA diagnostics; its claimed correction still requires
a completed fresh client, actual pixel comparisons and full image reviews.
The source keeps the original colors and effect parameters. Earlier fixes
made blur/HDR visible, but do not close this later horizon finding.

Potion chains run after the hand, before HUD; depth mist stays before the
hand. A failed mist shader no longer suppresses vanilla fog. Sun Scorned may
leave its original real fire after the potion expires; that is separate from
post-chain cleanup and prevents treating those hand pixels as clean controls.

`warp-042-client4` was interrupted between sessions during spawn preparation
at 36%, with no completion marker and no captured PNGs. On resumption no Java
process remained. Its original running receipts and partial log are preserved;
`warp-042-client4-interruption.json` records the later observation without
rewriting them. This run supplies no acceptance evidence. A fresh run is
required rather than continuing to poll the departed process.

GLSL150 HDR adaptation explicitly guards the undefined zero-chroma division;
defined colored-pixel math and original bloom parameters remain unchanged.
Original boss-name templates have modern resource aliases for released stale
Thaumcraft prefixes; the missing Tainted modifier label is localized. These
are declared presentation adaptations, not byte-identical original strings
at the previously absent keys.

## Warp5 limited route acceptance

`warp-042-client5` completed **15 captures**, native exit **0**, with **16**
owned hidden windows and input desktop **Default -> Default**. Root and the
independent `/root/warp5_review_042` reviewer each actually viewed all 15 full
1600x1000 PNGs and the review sheet. Their SHA-bound records are
`validation/warp-042-client5-root-visual-review.json` and
`validation/warp-042-client5-independent-visual-review.json`; both accept the
captured route and explicitly withhold release acceptance.

The former Hunger/Mist black horizon strips are absent in these frames.
Actual before/after GPU RGBA samples corroborate the copy correction:
Hunger's horizon changes from `(176,203,255,0)` to `(176,161,207,0)` rather
than black; Mist's horizon remains `(176,203,255,0)`. Alpha is preserved in
all six relevant pairs. Mist changes a distant chart sample from
`(16,6,15,255)` to `(118,132,168,255)`, while the hand remains clear after
world processing. The independent record retains the raw samples and their
event phases. Native potion particles and stable arena shadows remain;
these captures do not establish whole-image equivalence, universal GL-state
restoration, every angle, random event frequency, survival or multiplayer.

Warp5 and the Server3 pre-run snapshots have identical production and
resource fingerprints; their only differing file is
`src/main/java/thaumcraft/research/LateDevicesGameTests.java`.
This limited source relationship does not predeclare Server3's final result
or acceptance of a final packaged build. Earlier rejected runs and reviews
remain preserved.

## Server3 finding awaiting Server4

Server3 exposed a nondeterministic catalogue assertion:
`allOriginalEntitiesExistAndLivingFactoriesUseNativeGameplay` expected the
Thaumic Slime preview's size-four dimensions from every new gameplay entity.
The released constructor randomly chooses sizes 2, 4 or 8. Only the test
branch now checks that original size set and actual width/height formula
`0.51000005 * size`; other entity dimension assertions remain unchanged.
No runtime size, RNG or production behavior is changed. This test correction
awaits fresh Server4 execution. Server3 was still running when this finding
was first recorded; its completed result follows.

## Completed Server3 result

`completion-042-server3` completed **1784 tests: 1783 passed and one required
test failed**, with native exit **1**, Gradle exit **1**, in **11m 2s**.
`validation/completion-042-server3.log` and
`validation/completion-042-server3-run.json` retain the exact result.
The sole failure is the catalogue Slime dimension fixture described above.
Both the Prime Seed event and the Sprayer physical-aspect / native power-edge
tests passed in this run; their earlier rejected results remain preserved.
The catalogue-only test correction still needs Server4, so this is not a
passed final server gate or a 0.42 release acceptance.

A fresh `mobs-042-client2` run is being launched from the current 4815-file
source snapshot after the Pech goal-iterator fix. Its completion and image
review are pending; no Mobs client acceptance follows from Server3.

## Source resource checks

`validation/resources-0.42-second.log` records validation of **2946 JSON**
files and local model/texture/translation references.
`validation/media-0.42-source-parity.json` records byte equality against the
pinned original JAR for **710 PNG, 7 JPG and 111 OGG** shared media files,
with no mismatches. These are source-tree checks; they do not establish
packaged-JAR resource parity, runtime use of every asset or final release
acceptance.

## Mobs and late accessories follow-up

Mobs2 completed24/native0 but both reviewers rejected its capture route:
returned trade items obscured the camera, the Cleric projectile was hidden
at its caster, the Giant tentacle was still emerging and the Bottle's flight
was not distinguishable. All24 full PNGs were actually viewed by root and an
independent reviewer; their hashed reports are retained. The opt-in driver
now adjusts only initial fixtures, cameras and read-only visibility gates.
No corrected Mobs3 execution has yet been accepted.

LateAccessories1 stopped after12 images on an unexpected extra cloud jump.
Root viewed those12 full PNGs; no14-scene pass is implied. Diagnostic client2
reproduced the bug and recorded native .75 LivingJumpEvent impulses twice on
both logical sides while still airborne. Same-ID client/server Entity equality
aliased their shared WeakHashMap entry, replacing the stored level each time.
The runtime now keeps separate logical-side maps, preserving original physics
and charge/ownership guards. At that initial diagnosis Client3 was still
running; its later capture history and accepted follow-up are recorded below.
The production correction requires a fresh final server gate and does not
constitute a 0.42 release acceptance.

## Late accessories client5 limited acceptance

`late-accessories-042-client3` completed the native14-scene route and strict
one-impulse checks, but initial terrain readiness and overlapping Totem
activation required capture improvements. Client4 completed14 and validated
the actual model/type guards; its planned Cloud double-jump frame was still
rejected because white bamf appeared only later at landing. Both full-frame
reviews and earlier failures remain retained rather than overwritten.

`late-accessories-042-client5` completed **14 captures / JVM exit0**, PID16292,
with16 owned hidden windows and input desktop **Default -> Default**. Root
and `/root/qa_ledger_042` independently actually viewed every latest full PNG
and the independent sheet. Their SHA-bound JSON reviews accept this selected
route only, with `releaseAcceptance=false`.

Its Cloud image clearly shows original white bamf below airborne feet.
Native same-frame draw evidence records new particles at age6 and actual
vertex alpha199-213. The driver observes original draw vertices with the
actual event camera/pose/projection and waits for visible bounds; only the
initial camera and capture readiness changed. It injects no FX and changes
no particle age, RNG, momentum or flight state. Third-click native checks
still retain one accepted impulse per logical side while airborne, followed
by actual landing. Ordinary landing hurt is expected; no fall immunity is
claimed.

Physical client/server Verdant type0/1/2, unclamped predicates and actual
resolved original texture IDs pass through the final native guards. The
suspected all-red appearance was original11-frame overlay animation, not a
demonstrated NBT-loss defect; original media bytes remain unchanged. The
compiled initial floor, original sprites, worn band, native HUD/Curios,
XP contact and actual Undying rescue are legible in the accepted captures.

Reports and independent sheet are under
`validation/late-accessories-042-client5-{root,independent}-visual-review.json`
and `validation/late-accessories-042-client5-independent-sheet.png`.
Raw gear/charges/scenario inputs limit this to selected native conditions;
it does not establish paid acquisition, full survival, multiplayer, audio,
all angles or overall0.42 release acceptance. Final full server, other
profile and packaged-artifact gates remain separate.

## Remaining native gates, 30 October

Mobs4 and Eldritch1 also completed24 and17 PNG respectively with native exit0,
16 owned hidden windows and unchanged input desktop. Root and a distinct
reviewer actually viewed all full frames and selected sheets. With Warp5
and LateAccessories5, these70 selected frames have bounded acceptance;
their per-run reports retain occlusion, fixture and random-trade limits.
They need not be repeated for unrelated opt-in driver changes.

LateDevices2 progressed through Sprayer and produced13 PNG. Root viewed the
six specified new full frames; the independent reviewer viewed all13. The
unfolded Urn path, original Sprayer GUI, physical charge, red Vent2 and
held-power no-repeat were visible. The exact owned JVM was stopped at Travel:
its raw NoAI Cow cannot apply native LivingEntity travel physics. Initial
default AI and bounded client/server position/effect diagnostics correct that
fixture. Observers receive mob attribute changes rather than the rider-only
active effect list; native server effect assertions remain authoritative.

Green sections of the Urn's blue stream exposed a real vertex packing defect.
The original texture is grayscale. Original blue246 under brightness1.1 can
reach270; GL clamps floating color, whereas byte packing wraps270 to14.
WaterStreams now clamps only at that packing boundary, with two new server
regressions. Texture, UVs, original brightness, geometry and stream physics
are retained. Fresh LateDevices/native server and final source-freeze
verification remain required; no0.42 build has yet been released.

LateDevices3 stopped during preparation because a diagnostic Player import
was missing. Native Minecraft was never launched and its evidence is kept.
The import and the observer-only Travel guard are being corrected before
the next uniquely tagged run.

## Final native and build result

Server4 completed1786/1786 with native/Gradle exit0 in10m42s. The two new
Urn color regressions passed, as did the corrected randomized Slime fixture.
LateDevices4 completed17/native0/PID47772/16hidden windows/unchanged input
desktop. Both reviewers actually viewed all17 full PNG and the sheet: full
blue Urn path, original Spa/Sprayer GUI, physical charges, Vent2, native
Travel attributes and Barrier player passage/powered runes are visible.
Root and independent records retain faint/lingering Rune and fixture limits.

All five selected clients now have87 full PNG with two actual reviews each,
and all five sheets/current preview were actually viewed twice. Final
build passed24s; 2946 JSON references validated. All4817 source/resource/helper
fingerprints match final server/LateDevices/build; the four earlier clients
have the exact inactive-feature/test/driver exceptions in the source proof.
The first build snapshot request used a disallowed filename and wrote
nothing; a later snapshot matched the immutable server freeze on all4817
files, with no source edits since that freeze. Timing evidence is retained.

This closes the native verification issues above. Final archive CRC, JAR
resource-byte checks and Git delivery are recorded separately in the
artifact report; full survival, multiplayer, audio, all angles and arbitrary
third-party compatibility remain outside this acceptance.
