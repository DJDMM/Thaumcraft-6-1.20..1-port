# Vis Generator client and native smoke contract — BETA26

Scope0.37.0,2026-10-16. This is the implementation and verification contract.
Actual executed results and reviewed captures belong to VALIDATION.md and the
final0.37 artifact report. No client or screenshot pass is claimed here in advance.

## Released authority

Original`Thaumcraft-1.12.2-6.1.BETA26.jar`,SHA256
`9425f8643581b27ff8845b087c8bc6fc10425a32942f1a3f0e265ce6b38f7b5f`;
readable reference commit`954022bb777b7546281fb36df8522f0ba6b43f81`.
Primary released disassemblies are retained outside the project in
`work/generator-037-block-client-javap.txt`,
`work/generator-037-dispatcher-javap.txt` and
`work/generator-037-generic-javap.txt`. Relevant source classes are
`common/blocks/devices/BlockVisGenerator`,
`common/tiles/devices/TileVisGenerator`,
`client/fx/FXDispatcher`,
`client/fx/particles/FXGeneric` and`client/fx/ParticleEngine`.

There is no original inventory/menu,energy bar,developer label or player message.
No interface from TC4/5 or another energy mod is inferred. Six original facing
models and both enabled states remain ordinary baked blockstate models. The
existing item model is a normal baked parent,so no custom item renderer is added.
Both enabled states use the same original model and texture; redstone does not
introduce a separate off texture.

## Original random-display spark

The actual block`animateTick`calls`GeneratorClientEffects.animate`. It obtains
the physical client block entity's native Forge ENERGY capability on its actual
facing and requires`getEnergyStored()>0`. It does not test enabled state. Thus
a charged generator disabled by redstone still sparks,as in released BETA26.
No particle is emitted merely because a catalogue entry exists or a QA flag is set.

Offsets are Gaussian×.1 on axes perpendicular to the facing and signed .1 on
the facing axis. Source spark scale is .66+randomFloat,red .65+randomFloat×.1,
green1,blue1,alpha.8. `FXDispatcher.spark`uses lifetime5..9,16×16 atlas,
start8/24/40,eight age-selected frames,random horizontal flip and constant scale.
Motion,gravity,wind and rotation are zero. `FXGeneric.setRBGColorF`sets both
initial and final colors equally,so no invented fade to black is added.

Original ParticleEngine admission uses particle setting0/1/2,one additional
rejection level below30 FPS,`random.nextInt(3)`and budgets2000/1000/500.
The generator's bounded particle list preserves those settings and removes its
oldest spark when full. The released globally shared particle budget is represented
by this module's local list; modern particle scheduling/RNG streams are an explicit
engine adaptation. The effect is cleared on client world changes.

Rendering uses the existing original atlas and layer0 additive pipeline:
SRC_ALPHA/ONE,alpha>1/255,native lightmap/fog,no depth writes. It reuses the
previously audited CondenserSparkShader. There is no see-through text shader,
fabricated beam,generated image or replacement atlas. Atlas SHA256 is
`3c31a3bed7a007ef4d671745bd627112c79d1ac70049e23194a1454929f17000`,
byte-identical to the released JAR. Existing`textures/blocks/vis_generator.png`
is retained; no new media file or player locale is needed.

Read-only counters expose actual positive-energy display callbacks,admitted and
drawn sparks,facing mask,and powered positive samples/admitted sparks. Neither
the counters nor QA causes a random-display callback or resets generator energy.
The source sync quirk is retained: charge and empty boundaries sync energy;
intermediate pushes do not add an invented per-tick packet stream.

## Twelve-scene owned native client route

Opt-in`thaumcraft.generatorSmokeTest`,profile`Generator`,directory
`run/generator-smoke`,prefix`tc6-generator-`. The root alone may run the verified
hidden non-input Windows desktop launcher. This driver neither activates a
window nor switches desktops or sends global keyboard/mouse input.

The driver creates a fresh uniquely named owned Survival world. Its twelve names
are authoritative in`GeneratorClientSmokeTest.IMAGES`:native Potentia scan,
paid research,paid arcane bench,DOWN/UP/NORTH/SOUTH/WEST/EAST Survival placements,
powered pause,resume and final physical teardown. Screenshots are1600×1000.
After ordinary camera readiness,the effect scenes wait for an actual naturally
live spark at render completion; no particle is generated solely for a photograph.

The actual Thaumometer C2S scans a supplied typed Potentia crystal. The study
uses completed REDSTONERELAY as an explicit precursor fixture. It pays exactly
32 raw BASICS theory,32 raw ARTIFICE theory,one redstone block and one piston,
and earns10 XP through native advancement. Before/after knowledge balances are
used,without clearing any knowledge earned through the scan. The native bench
pays25 Vis,Ignis1 and Ordo1,four planks,redstone dust,piston,two rare-earth
nuggets and a Vis Resonator. Both study and craft need their own physical piston.

Only that one paid generator is reused through six placements and six native
Survival harvests. Five intervening physical ItemEntity drops are picked up by
the real player before reuse; no item is copied or granted to replace the paid
device. Source block drops do not preserve stored FE; each genuine dismantling
and placement follows that original behavior. Placement finds its actual facing
by discovering the adjacent receiving capability in original direction order.
No direct FACING write,BlockEntityTag energy fixture or wrench is used.

## Explicit Forge Energy recipient fixture

Vanilla Minecraft has no FE-consuming chest or ordinary FE consumer. The smoke
uses `AttachCapabilitiesEvent<BlockEntity>`to provide one real IEnergyStorage
on each of six supplied exact native chest instances at known coordinates.
The entire listener is Dist.CLIENT,opt-in,and attaches only at those coordinates;
its provider returns a capability only on the correct face,in the exact owned
integrated-server world. Other worlds,clients,positions and normal gameplay do
not receive this energy capability. Invalidation follows the native chest.
There is no registered QA block,item,recipe,screen or gameplay consumer.

Each fixture has200000 capacity,accepts actual native pushes of0..20 FE,records
the accepted balance and call tick,and rejects a second non-simulated offer in
the same world tick. Simulations do not increment its balance. Native generator
callbacks alone pay this recipient; the driver never calls the capability to
manufacture a transfer or resets an active recipient balance. Each facing must
accept at least200 FE. This proves an ordinary truthful Forge capability route,
not compatibility with every external energy mod or dishonest callback handler.

After the sixth placement,the driver waits for a naturally high positive charge,
places a real redstone block through C2S,and records source energy and recipient
FE. Both must remain unchanged for at least80 actual world ticks. Original
powered charged sparks must continue. The player then physically harvests the
redstone block and the recipient must gain at least200 further FE through native
20-FE pushes. Final teardown leaves exactly one physical paid device,with every
placed generator absent. Receiver ledgers remain available for final assertions.

Fixtures are the precursor study,raw theory,typed scanning crystal,study and
recipe materials,tools,bench,six raw chests,stone supports,bedrock arena and
initial500 Vis in16 loaded chunks before any consumer exists. There are no
post-consumer aura,energy,tick,count,RNG or recipient resets and no direct tile
tick/recharge/craft call. Preparing these inputs does not prove uninterrupted
ordinary Survival material acquisition.

Required final evidence:12 ordered fresh native PNG,all full frames and both
sheets actually reviewed by root and an independent reviewer,hidden JVM native
exit0,matching source fingerprints,`THAUMCRAFT_GENERATOR_RENDER_AUDIT_OK`and
`THAUMCRAFT_GENERATOR_CLIENT_SMOKE_OK: 12 scenes`. Logs bind actual accepted FE,
native recipient calls,pause duration,positive-energy samples and spark counts.
This route does not establish multiplayer,client reload,audio capture,all camera
angles or a full ordinary Survival playthrough.

## Фактически завершённый QA0.37

`generator-037-server1` прошёл1534/1534; `generator-037-client2`12/12, оба nativeexit0.
Два проверяющих просмотрели все12полных1600×1000PNG и оба листа.
Реальные Potentia scan/64Theory+2physicalitems/10XP/25visIgnisOrdo craft,
один предмет6Survivalplacements/5nativepickups/6harvests,6faces/13820FE,
81тиков паузы/chargedpowered sparks/resume связаны с финальными logs/SHA.
ChestFEcapabilities являются явной opt-in фикстурой; ordinary vanillaChest
не потребляет FE. Source quirks, современные guards, подготовленные условия,
ранние попытки и границы — [VALIDATION.md](../../VALIDATION.md) и
[artifact-report0.37](../../validation/artifact-report-0.37.json).
