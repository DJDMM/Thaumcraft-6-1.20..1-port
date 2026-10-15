# Dioptra and Arcane Pattern Crafter: released BETA26 progression

This records the0.36 implementation contract and prepared checks. It is not a
claim that a native server/client run has passed. The final run IDs, logs,
resource fingerprints, screenshots and fixture limits belong in VALIDATION.md
and the0.36 artifact report after actual execution.

## Primary authority

Original JAR: `Thaumcraft-1.12.2-6.1.BETA26.jar`, SHA-256
`9425f8643581b27ff8845b087c8bc6fc10425a32942f1a3f0e265ce6b38f7b5f`.
The exact research JSON was read from this archive; the classes below were
disassembled directly with Java17 javap. Source commit
`954022bb777b7546281fb36df8522f0ba6b43f81` is a readable companion only.

| Released entry | SHA-256 |
|---|---|
| `assets/thaumcraft/research/artifice.json` | `615ac0c944aa19bf6bb3bf5f02158eb99577aa979bf0767dfd3aef0badc7834c` |
| `thaumcraft/common/config/ConfigRecipes.class` | `487d32f43df9277fa845c129bc549da0126f34902a2d5089f9d97d204eb0f4e2` |
| `thaumcraft/api/capabilities/IPlayerKnowledge$EnumKnowledgeType.class` | `e6bd216ee50f7b236a37f0cbb00d05303368dfcd264db5dd848795e35cca8be1` |
| `thaumcraft/common/lib/network/playerdata/PacketSyncProgressToServer.class` | `99a99813a54773ced22db9b5ddafd5fbd51a070e01e2e0aad41365a73313b7f2` |
| `thaumcraft/common/lib/research/ResearchManager.class` | `0ee9b7926ac6fe02f6b82df69212927193cf889e3c8e844948e7ca6e8ca6b243` |

`ConfigRecipes` bytecode5190–5334 confirms Dioptra and8411–8584 confirms
Pattern Crafter, including exact component identities, quantities and primal
aspects. Knowledge type initialization at4–33 fixes THEORY progression32 and
OBSERVATION progression16. `PacketSyncProgressToServer`476–480 multiplies the
negative requested amount by that progression before payment.
`ResearchManager`998–1000 grants5 earned XP per successful advance.

## Original studies and strict prerequisite chain

Both are ARTIFICE entries with two stages, no siblings and no Warp. Stage1
contains the payment. Stage2 is the concluding recipe page, completed after the
first stage pays. Starting grants5 XP, finishing grants5 XP. Replay grants none.

| Key | Strict completed parent | Original discoveries | Original knowledge |
|---|---|---|---|
| `DIOPTRA` | `BASEARTIFICE` | `!sensus`, `!auram` | OBSERVATION;AUROMANCY;1 =16 raw |
| `ARCANEPATTERNCRAFTER` | `DIOPTRA` | `!fabrico` | THEORY;GOLEMANCY;1 + THEORY;ARTIFICE;1 + THEORY;AUROMANCY;1 =32 raw each,96 total |

Neither study obtains/consumes an item. A recipe output, aspect hover or starting
the precursor does not complete the parent. DIOPTRA's original ARTIFICE graph
coordinate[-2,3] and REVERSE meta flag remain; Pattern's coordinate is[-2,5].
The needed Dioptra prerequisite is implemented in the same tranche: it is never
silently skipped, automatically completed or replaced with a new unlock cost.
The normal book retains current-stage visibility, the physical book requirement,
expected-stage replay guard, alive/non-spectator checks and exact server payment.
Its recipe page is revealed only after payment. The original workbench's bare
research gate deliberately accepts an already started study even before the
concluding book page is available.

## Original arcane crafts

Both are shaped arcane workbench recipes, fresh one-item outputs,50vis each.
Neither is an infusion and neither inherits component NBT.

| Output | Grid rows | Exact key | Primal payment |
|---|---|---|---|
| Dioptra | `APA` / `IGI` / `AAA` | A=arcane stone(5); P=Vis Resonator(1); G=Thaumometer(1); I=iron plate(2) | Aer1,Aqua1 |
| Pattern Crafter | `VH ` / `GCG` / ` W ` | V=Vis Resonator(1); H=hopper(1); G=simple mechanism(2); C=workbench(1); W=Greatwood plank(1) | Aqua1,Terra1,Ordo1 |

Original `plateIron` becomes existing `forge:plates/iron`. Original ore-dictionary
`workbench` becomes an additive `forge:workbench` item tag containing the actual
vanilla crafting table, keeping the original extension point. Greatwood is an
exact block identity, not a broad planks tag; the Vis Resonator differs from the
Essentia Resonator and Morphic Resonator. Native IDs are `arcane/dioptra` and
`arcane/patterncrafter`; original detached book IDs remain `thaumcraft:dioptra`
and `thaumcraft:patterncrafter`. Live synchronized recipes let normal book views
find these original pages; pinned reference data is not altered or used as a
manufacturing registry.

## Census and prepared native checks

`ResearchProgression` adds only DIOPTRA and ARCANEPATTERNCRAFTER:101/148 canonical
implemented keys, preserving99 from0.35. Existing56 archived infusion recipes
remain and their operational subset stays30/56. Neither study unlocks later
equipment/Eldritch content. The thirteen old exact census assertions become101;
their unsupported-branch lists drop only the two legitimately implemented keys.
No raw legacy item mapping or broad payment rule is changed.

`PatternCrafterProgressionGameTests` has16 native GameTest methods covering:

- Exact released graph, two additions only, strict completed parents, later
  unsupported studies and unchanged infusion recipe count.
- Each missing discovery, Dioptra raw observation15 and each Pattern theory31
  balance reject atomically. Wrong type/category cannot substitute.
- Exact16/32+32+32 raw payment, no specimen cost,5+5 earned XP, no Warp,
  normal book rows/conclusion, stale/replayed requests and saved completion.
- Physical book, expected stage and spectator guards.
- Three real held Thaumometer callbacks acquire Sensus/Auram/Fabrico from
  supplied typed crystals; read-only hover grants nothing and consumes nothing.
- Both live synchronized recipes/pinned displays, research gates, typed primal
  crystals, exact grids, blank cells, fresh detached outputs and serialization.
- Both native result-menu clicks pay50vis and their real ingredients/crystals;
  unknown research, missing primal,49vis, wrong/mixed crystal and blocked output
  preserve physical balances. Their paid outputs record real crafting facts.
- One complete native chain scans all three aspects, pays Dioptra, crafts it,
  then pays and crafts Pattern Crafter. Dioptra is never granted in this chain.

Payment-isolation tests supply original completed precursors intentionally.
The full chain supplies only completed BASEARTIFICE. Raw knowledge, typed scan
specimens, ordinary recipe materials, initial50vis per craft and the bounded
workbench fixture at absoluteY235 are supplied test conditions. Existing scan
observation rewards are retained. These are not a complete ordinary survival
playthrough, natural acquisition of all precursor materials, multiplayer,
dedicated-client synchronization or visual/audio evidence. Root alone runs the
fresh full native server, hidden non-input client and final packaging.

## Фактически завершённый QA0.36

`pattern-036-server4` прошёл1488/1488; `pattern-036-client1`23/23, оба nativeexit0.
Два проверяющих просмотрели все23полных1600×1000PNG и оба листа.
Настоящие3scan/2study/2bench/100vis5crystals/96Theory16Observation20XP,
nativeDioptraVisFlux,10dies/10crafts/24outputs, 80тиков паузы,
продолжение и физическая разборка связаны с финальными logs/SHA.
Фикстуры, source quirks, современное loaded-only/владение, ранние попытки
и пределы доказательства сохранены в [VALIDATION.md](../../VALIDATION.md)
и [artifact-report0.36](../../validation/artifact-report-0.36.json).
