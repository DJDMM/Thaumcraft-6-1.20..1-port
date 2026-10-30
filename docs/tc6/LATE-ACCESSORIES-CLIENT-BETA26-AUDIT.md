# Late accessories native client verification

0.42 selected native client route accepted in `late-accessories-042-client5`
after two actual full-image reviews. Final server and release gates remain
separate; this is not overall 0.42 acceptance.
Original behavior is defined by BETA26 `ItemVerdantCharm`, `ItemCloudRing`,
`ItemCuriosityBand`, `ItemCharmUndying` and `PlayerEvents.pickupXP/onDeath`.

`LateAccessoriesClientSmokeTest` is enabled only by
`thaumcraft.lateAccessoriesSmokeTest` in a new owned Survival world. Root
launches it through the hidden non-input desktop helper using the dedicated
`run/late-accessories-smoke` directory. No manual save, desktop switching or
global input is used.

The14 prepared scenes exercise actual native routes:

1. Original three NBT-typed Verdant item forms in the real inventory.
2. Real Curios inventory-button and C2S equip, initial three cure inputs.
3. First native Wither priority interval and40→20charge.
4. Native Poison then Flux Taint intervals, ending at5charge.
5. Life charm native one-health heal for5charge,20→15.
6. Sustenance native air then food,10→9→8charge, in a sealed water fixture.
7. Physically equipped Curiosity band in third person with original worn art.
8. One real100XP orb's natural player contact:50XP payment and one raw theory
   point; no direct pickup event or knowledge award is called.
9. Physical Undying charm in the native Curios GUI.
10. Actual fatal1000damage through native damage/death callbacks, consuming
    that one charm and producing original health/effects/entity-state35.
11. Physical Cloud ring in the native Curios GUI.
12. Actual local first-jump input, released key, then native key binding click,
    client prediction and real C2S authoritative extra impulse.
13. A third native key click in the same flight adds no new upward impulse.
14. Actual landing, without motion/clock setters.

All six initial gear items, initial charges40/20/10, later one-time raw cure,
health19, air25, food19 and the100XP orb are explicit scenario inputs. They
do not establish paid research, crafting or survival acquisition. Once a
scenario begins, its effects/charge/health/air/food/velocity/timers/RNG are
not reset to obtain a frame. Curios clicks physically conserve replaced
items; only the Undying charm is consumed. The other five physical items and
all final charm charges are checked again at completion. A glass basin keeps
the water input from interfering with the later real jump.

The native screenshot marker says that actual pixels require review. Renderer
draw counts,14PNG existence and gameplay assertions do not prove appearance
or visibility. Root and `/root/qa_ledger_042` each actually viewed all14 latest
1600x1000 PNGs and the independent review sheet before accepting the selected
route. Their SHA-bound reviews are
`validation/late-accessories-042-client5-root-visual-review.json` and
`validation/late-accessories-042-client5-independent-visual-review.json`.
The native JVM exited0, PID16292, with16 owned hidden windows and unchanged
Default input desktop. No multiplayer, complete survival or audio claim follows.

## Retained failures and selected capture correction

Client1 stopped after12 captures on an unexpected extra airborne impulse.
Client2's read-only native LivingJumpEvent and state observations proved a
production defect: Entity equality uses the shared numeric client/server ID,
so one WeakHashMap aliased distinct LocalPlayer and ServerPlayer objects.
Their alternating level guards rearmed used flight state. Separate weak maps
per logical side fix the alias, preserving original motion, ownership and
one-flight guards. Client3 passed the strict one-client/one-server impulse
checks, but its initial mesh and overlapping activation captures required
better fixture readiness. Its record remains preserved.

Client4 completed14 captures and native model/type guards but was visually
rejected for the intended double-jump frame: the emitted white bamf was
visible only later in the landing image. No production FX change followed.
Client5 uses an initial -45-degree third-person camera before the first jump,
records a particle identity baseline before the native second click, then
waits for new native bamf particles aged6-12 in the actual world frame.
A CPU-only sink reads the original particle draw's vertices with that same
event's camera, partial tick, pose and projection matrices. At least two
vertices must project above the HUD with nonzero visible alpha. It submits
no GPU buffer and changes no effect, particle age, RNG, motion or flight flag.

In client5 the actual capture shows white clouds below the airborne player's
feet; native draw evidence records age6 and vertex alpha199-213. Strict third
click rejection remains active, with one accepted Cloud impulse on each side
and no intervening landing. The final image includes ordinary native landing
hurt; the original ring does not grant fall immunity.

The earlier apparent similarity of all three charm colors was investigated
without changing textures. All three released overlays are original11-frame
animations; later frames can converge visually. Client5 records every physical
client stack's full NBT, type0/1/2, actual unclamped predicate and resolved quad
texture IDs, plus final server type/count/charge guards. All pass. There is no
remaining demonstrated type-loss or substituted-texture defect in this route.
Earlier logs, full PNGs and rejected reviews remain retained. These bounded
results do not replace the final server, packaging or other-profile gates.
