# Vis Generator: released BETA26 study and arcane crafting

This records the0.37 progression implementation and prepared checks. It does
not certify a successful native server/client execution. Final run IDs, raw
observations, snapshots, reviewed screenshots and limits belong in the final
artifact report and VALIDATION.md after the root actually runs them.

## Primary evidence

Original released JAR: `Thaumcraft-1.12.2-6.1.BETA26.jar`, SHA256
`9425f8643581b27ff8845b087c8bc6fc10425a32942f1a3f0e265ce6b38f7b5f`.
The exact artifice JSON was read from that archive. Original classes below
were inspected through Java17 javap, not compiled from recovered source.
Source commit `954022bb777b7546281fb36df8522f0ba6b43f81` is a readable companion.

| Released entry | SHA256 |
|---|---|
| `assets/thaumcraft/research/artifice.json` | `615ac0c944aa19bf6bb3bf5f02158eb99577aa979bf0767dfd3aef0badc7834c` |
| `thaumcraft/common/config/ConfigRecipes.class` | `487d32f43df9277fa845c129bc549da0126f34902a2d5089f9d97d204eb0f4e2` |
| `thaumcraft/common/config/ConfigItems.class` | `03ca145d619bcd808b1412ec7b2c8278400bc705afa691aa26a6d574f5b741d7` |
| `thaumcraft/api/capabilities/IPlayerKnowledge$EnumKnowledgeType.class` | `e6bd216ee50f7b236a37f0cbb00d05303368dfcd264db5dd848795e35cca8be1` |
| `thaumcraft/common/lib/network/playerdata/PacketSyncProgressToServer.class` | `99a99813a54773ced22db9b5ddafd5fbd51a070e01e2e0aad41365a73313b7f2` |
| `thaumcraft/common/lib/research/ResearchManager.class` | `0ee9b7926ac6fe02f6b82df69212927193cf889e3c8e844948e7ca6e8ca6b243` |

ConfigRecipes9201–9364 fixes the VISGENERATOR gate,25vis, Ignis/Ordo1,
three ordered grid rows and all constructor operands. ConfigItems253–262
assigns `rareearth` to nuggets metadata10: it is neither a vis crystal nor a
broad metal nugget. EnumKnowledgeType initialization4–15 fixes THEORY32;
PacketSyncProgressToServer476–480 multiplies the requested negative amount
by that raw progression. ResearchManager998–1000 grants5 earned XP on each
successful advance, giving5 at start and5 at completion for this entry.

## Original research

VISGENERATOR is in ARTIFICE at[2,-3], has exactly the strict completed parent
REDSTONERELAY, two stages, no siblings and no Warp. Starting opens stage1;
completing its requirements enters the original concluding recipe page and
finishes at stored stage3. An already started Relay does not satisfy the parent.

Stage1 requires:

- THEORY;BASICS;1:32 raw theory.
- THEORY;ARTIFICE;1:32 raw theory.
- The retained actual `!potentia` discovery.
- One physical vanilla redstone block and one physical ordinary piston.

Both physical specimens are consumed from main inventory; armor/offhand do
not substitute. Theory and both items are checked together before payment.
No observation, extra knowledge category, scan specimen consumption, redstone
dust substitute, sticky piston or new discovery cost is added. A pure hover
does not discover Potentia or complete the study. Native Thaumometer callbacks
can acquire the aspect independently of study state. Replayed expected-stage
requests never repay theory/items or grant a second10XP.

The normal book keeps original current-stage visibility, exact requirement
rows, physical book/expected-stage/alive/non-spectator guards. Its recipe page
appears after the paid stage completes. The original arcane workbench uses a
bare known/started research gate, so an already started generator study can
craft even while that concluding normal book page remains unavailable.

## Original arcane recipe

The recipe is ordinary shaped arcane crafting,25vis, Ignis1 and Ordo1, one fresh
Vis Generator output. It is not an infusion, and component NBT is not copied.

| Grid | Key |
|---|---|
| `WSW` | W=`plankWood`, four total |
| `EPE` | E=rare earth nugget metadata10, two total; P=one ordinary piston |
| `WRW` | S=`dustRedstone`, one; R=one Vis Resonator |

Ore dictionary plankWood maps to existing additive `minecraft:planks`, allowing
mixed wood species and the two TC planks. dustRedstone maps to existing
`forge:dusts/redstone`. E maps exactly to `thaumcraft:nugget_rareearth` and R
exactly to `thaumcraft:vis_resonator`, preserving the distinction from Essentia
Resonator and Morphic Resonator. The live native recipe ID is
`thaumcraft:arcane/visgenerator`; the original book ID remains
`thaumcraft:VisGenerator`, normalized by the existing book recipe resolver.
The pinned original display JSON and legacy research data remain unchanged.

Research consumes its own piston; manufacturing consumes another piston.
Therefore the complete paid study plus device craft pays two distinct physical
pistons overall. The redstone block is a study specimen; the later recipe uses
one separate redstone dust. Neither item is silently recycled between steps.

## Implementation and prepared native checks

Only VISGENERATOR is added to ResearchProgression, yielding102/148 canonical
implemented keys while preserving101 from0.36. No GRAPPLEGUN/Eldritch or other
late equipment gate is enabled. Registered infusion recipes remain56 and their
operational subset stays30/56, because this addition is an arcane recipe.
The fourteen old canonical-census assertions become102; no unrelated payment
or unsupported-branch requirement is relaxed. LegacyResearchItems needs no new
mapping for the two ordinary vanilla study specimens.

VisGeneratorProgressionGameTests contains14 prepared native GameTest methods:

- Original strict Relay parent, two stages/no Warp/siblings,102-entry census,
  unchanged56 infusions and still unsupported later equipment/Eldritch.
- Missing Potentia, either raw theory31 and each missing/offhand/worn physical
  specimen reject all other resources atomically; wrong category/type cannot pay.
- Exact32+32 raw theory, one redstone block/piston,5+5XP, retained discovery,
  normal book rows/conclusion, detached item templates, replay and saved completion.
- Actual book, expected stage and spectator guards.
- Native held Thaumometer scan earns Potentia from a supplied typed crystal;
  pure hover grants nothing and the actual scan consumes no specimen or study.
- Exact native recipe, original pinned book grid and recipe-network roundtrip,
  Ignis1/Ordo1,25vis and detached fresh output.
- Native result-menu clicks reject unknown research,24vis, either missing
  crystal, wrong/mixed aspect crystal, blocked result and stale sticky-piston
  preview; original physical balances remain unchanged.
- Every wrong component is rejected, while mixed vanilla/Greatwood planks
  satisfy the original dictionary group. Paid native crafting spends exactly
  one of every occupied slot, two rare-earth nuggets, two primal crystals and
 25vis, recording its real crafting fact without inheriting input NBT.
- One integrated native route scans Potentia, pays the original study and then
  the real workbench recipe, retaining independent scan rewards.

Completed Relay, raw theory, typed scan crystal, original study/recipe materials,
25initial bench vis and a bounded workbench at absoluteY235 are supplied fixtures.
Payment-isolation tests use an explicit target aspect fact; the integrated
route earns it through the actual scanner. The target study and manufactured
device are not granted in the integrated route. The tests do not establish a
full ordinary survival playthrough, natural acquisition of all precursor
materials, FE interoperability, generator mechanics or client/audio appearance.
Those device behaviors have separate core/client audits and native evidence.
Root alone runs the fresh full server, verified hidden non-input client and
final packaging; definitions here are not QA success claims.

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
