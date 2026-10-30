# Late accessories, released BETA26

Implementation record for 0.42. The selected late-accessory native client
route is accepted below; final server and release gates remain separate.
Baseline: released `Thaumcraft-1.12.2-6.1.BETA26.jar`, pinned by
`reference-manifest.json`; original `ItemVerdantCharm`, `ItemCloudRing`,
`ItemCuriosityBand` and `PlayerEvents` are the behavioral references.

The three Verdant forms share one original NBT `type` item and 200 `tc.charge`.
Every 20 wearer ticks, only the first affordable operation succeeds: Wither
20, Poison 10, Flux Taint 5; type 1 heals one health for 5; type 2 restores
air below 100 to 300 for 1, then food by 1 with saturation modifier .3 for 1.
Actual noncosmetic Curios storage owns the physical item. Detached copies do
not spend its charge. Existing original item predicates/textures select forms.

The Curiosity head slot halves the picked-up unit by integer division before
vanilla XP/Mending. The original single random float chooses Theory below
`.05 * paid`, otherwise Observation below `.2 * paid`, one raw unit in an
original category. The original worn sprite is copied unchanged and uses the
existing original strip extrusion with its separate helmet offset.

Modern XP orbs contain multiple equal-value units. A pending pickup records
the pre-touch count, lends vanilla the reduced value, then restores the
remaining units at player-tick end (server-tick end is a fallback). Repeated
touches settle their previous transaction first. Knowledge is credited only
after the native count decreases; cancelled pickups restore the original
value and do not credit knowledge. This merge/cancellation handling is an
explicit modern adaptation, not a claim about original cancelled-event quirks
or arbitrary third-party rewrites of an orb during an event.

The Undying charm consumes one actual equipped charm through LivingDeath,
restores one health, clears effects and grants Regeneration II 900 ticks and
Absorption II 100 ticks, with the native totem status/stat/criterion. It does
not gain the modern vanilla totem's Fire Resistance. An already cancelled
event does not consume a charm; an inventory-only copy cannot rescue death.

Cloud uses the actual native jump input, original zero jump-delay guard and
one additional airborne jump. Vertical motion is .75 plus Jump Boost, sprint
adds the original horizontal .2 impulse, fall distance resets and the Forge
jump hook runs. The native client predicts; a sender-owned C2S request checks
the real equipped ring, life/spectator/water/ground and per-flight state.
Landing with native jump delay zero rearms it. No client motion/UUID is accepted.
The released white upward bamf and sweep sound use their original parameters.

The native diagnostic `late-accessories-042-client2` exposed a real integrated
server defect: Entity equality compares entity IDs, so one shared WeakHashMap
aliased LocalPlayer and ServerPlayer with the same ID166 but different object
identities. The server replaced the client's stored level, permitting another
airborne jump on the next client click. Read-only LivingJumpEvent observations
recorded two .75 impulses on each logical side without an intervening landing.
Client and server now use separate weak maps; both tryJump and grounded select
the appropriate map. Worn ownership, original motion and the dimension guard
are retained. The client regression also requires exactly one native impulse
per logical side before and after the third click. The selected completed
client verification is recorded below; earlier failing logs and PNGs remain
preserved. The final full server gate is not inferred from a client result.

`LateAccessoryGameTests` covers native slot validation, actual charge priority,
insufficient charge, NBT forms, merged XP/native Mending/cancellation, physical
death ownership and per-flight jumps. The opt-in hidden-client driver supplies
separate actual rendering and gameplay evidence. Initial gear/charge/ailments,
actor positions and worlds are declared fixtures; no full survival, multiplayer
or arbitrary-mod compatibility acceptance follows from those checks.

## Selected native client acceptance

`late-accessories-042-client5` completed14 captures with JVM exit0, PID16292,
16 owned hidden windows and Default input desktop unchanged. Root and an
independent reviewer each actually viewed all14 full1600x1000 PNGs and the
review sheet. Their SHA-bound records are
`validation/late-accessories-042-client5-root-visual-review.json` and
`validation/late-accessories-042-client5-independent-visual-review.json`.
The original three Verdant forms/native HUD, physical Curios equipment,
worn Curiosity art, XP contact, actual Undying rescue, white Cloud bamf below
airborne feet, strict third-click rejection and native landing are visible.

After client2 proved the shared-map defect, the runtime's separate logical
side maps prevent another impulse: later native checks retain exactly one
accepted Cloud jump per side during the same flight. Client3/4 capture
limitations are retained in the client audit. Only fixture mesh readiness,
natural Totem-animation expiry, an initial camera angle and read-only native
particle draw/age/projection gates changed afterward; no production FX or
physics was changed to obtain client5's frame.

Client/server type0/1/2, actual model predicates, resolved original overlay
texture IDs, final physical counts and charges remain correct. Similar red
appearances in earlier frames are compatible with the released11-frame
animations, whose PNG and metadata bytes remain unchanged. No further type
or texture repair is supported by this evidence.

This accepts the declared client scenarios only. Raw gear and scenario inputs
do not establish survival acquisition, paid progression, every animation
frame, all angles, multiplayer or audio. Overall0.42 release acceptance and
the final full server/build/package checks remain outside this result.
