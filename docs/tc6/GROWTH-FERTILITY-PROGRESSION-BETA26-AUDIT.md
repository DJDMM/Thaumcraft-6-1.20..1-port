# Growth and fertility lamps: BETA26 progression and paid recipes

This audit describes the original TC6 BETA26 progression and the0.35 implementation.
It does not certify a successful build or native QA run. Final run IDs, failures,
screenshots and limitations belong in `VALIDATION.md` and the artifact report.

## Primary evidence

The authority is `Thaumcraft-1.12.2-6.1.BETA26.jar`, SHA-256
`9425f8643581b27ff8845b087c8bc6fc10425a32942f1a3f0e265ce6b38f7b5f`.
These exact entries were read or disassembled from that release:

| Original entry | SHA-256 |
|---|---|
| `assets/thaumcraft/research/artifice.json` | `615ac0c944aa19bf6bb3bf5f02158eb99577aa979bf0767dfd3aef0badc7834c` |
| `common/config/ConfigRecipes.class` | `487d32f43df9277fa845c129bc549da0126f34902a2d5089f9d97d204eb0f4e2` |
| `api/capabilities/IPlayerKnowledge$EnumKnowledgeType.class` | `e6bd216ee50f7b236a37f0cbb00d05303368dfcd264db5dd848795e35cca8be1` |
| `common/lib/network/playerdata/PacketSyncProgressToServer.class` | `99a99813a54773ced22db9b5ddafd5fbd51a070e01e2e0aad41365a73313b7f2` |
| `common/lib/research/ResearchManager.class` | `0ee9b7926ac6fe02f6b82df69212927193cf889e3c8e844948e7ca6e8ca6b243` |

Decompiled source at commit `954022bb777b7546281fb36df8522f0ba6b43f81`
is a readable companion. `ConfigRecipes` bytecode offsets4101–4252 and
4259–4404 confirm the two recipes, including ordered constructor arguments,
typed primal crystals, costs and instability. `EnumKnowledgeType.<clinit>`
offsets4–15 establishes THEORY progression32. The original server payment
multiplies the requested knowledge amount by `getProgression()` before debit.
`ResearchManager.completeResearch` grants5 earned XP per successful advance;
these two-stage entries advance once to start and once to complete.

## Original research

Both entries are in ARTIFICE. They require **completed** ARCANELAMP and INFUSION,
have two stages and no siblings or warp. Stage1 holds all payments. Stage2 is
the concluding recipe page and is skipped into completion after stage1 pays.

| Key | Knowledge | Discovery | Physical obtain | Concluding recipe |
|---|---|---|---|---|
| `LAMPGROWTH` | THEORY;ARTIFICE;1 =32 raw | `!herba` | one `minecraft:dye;1;15` | `thaumcraft:LampGrowth` |
| `LAMPFERTILITY` | THEORY;ARTIFICE;1 =32 raw | `!desiderium` | one wheat and one carrot | `thaumcraft:LampFertility` |

The discovery is retained. Physical specimens are consumed from main inventory,
not armor or offhand. Growth's old dye metadata15 is **bone meal** in1.20.1;
modern white dye, ink sac and bone do not substitute for it. The mapping is
shared by the real server payment and detached book display. No observation
cost, extra unlock item, knowledge category or manufactured discovery is added.
Start grants5 XP, completion grants5 XP; replayed requests pay and grant nothing.
Current-stage normal book visibility reveals the original recipe only after
the player completes the paid stage. Recipe matching retains the original bare
`isResearchKnown` gate: an already started entry is sufficient for crafting,
although its second book chapter is still hidden until completion.

## Original infusion recipes

Both are ordinary replacement infusions, instability4, one ordinary arcane lamp
in the center, one fresh output. The result does not inherit the center's NBT.
Every peripheral entry pays one physical item; order is not a matching constraint.

| Recipe | Four essentia costs,65 total | Six original peripheral constructor arguments |
|---|---|---|
| `LampGrowth` | Herba20, Lux15, Victus15, Instrumentum15 | gold ingot; bone meal; Terra crystal; gold ingot; bone meal; Terra crystal |
| `LampFertility` | Bestia20, Lux15, Victus15, Desiderium15 | gold ingot; wheat; Ignis crystal; gold ingot; carrot; Ignis crystal |

Each typed crystal is `crystal_essence` with exactly one corresponding Aspects
entry of amount1. This is distinct from the world vis-crystal items used in
arcane workbenches. The existing two infusion JSON files already encode the
released recipes and modern IDs correctly and remain unchanged. Their recipes
were included in the archived56 before0.35; no new duplicate recipe is added.

The precursor Arcane Lamp remains the original arcane50vis, Aer1, Ignis1,
four iron plates and one amber block. Its native paid craft is exercised as
part of each new altar test; the resulting physical output becomes the center.

## Implementation and census

`ResearchProgression` enables only LAMPGROWTH and LAMPFERTILITY.
`LegacyResearchItems` adds only the explicit legacy white-dye mapping.
This yields99 canonical implemented entries out of148, preserving the previous97.
Enabling these two previously archived, now operational infusion paths yields
30/56 playable paths. The registered recipe count remains56. DIOPTRA,
ARCANEPATTERNCRAFTER and BASEELDRITCH remain unfinished; none is granted by
studying or scanning either lamp. Existing canonical-census and unsupported
branch assertions are updated only for the two newly enabled entries.

## Verification scope prepared for the root run

`LampProgressionGameTests` contains15 native GameTest methods:

- Every strict parent, original graph,99-entry census,56-recipe nonduplication
  and still unavailable later devices.
- Missing discovery, raw theory31, every missing physical obtain and rejected
  modern substitutes preserve all other resources.
- Exact32-theory/item/5+5XP payments, detached book rows and concluding recipe,
  no observation or warp, retained discovery, main-inventory-only matching,
  replay/save and physical-book/current-stage/spectator guards.
- A real held Thaumometer callback acquires Herba and Desiderium from supplied
  typed physical crystals; a read-only hover gives no discovery and the scan
  consumes no specimen or lamp study.
- Both bare-started recipe gates, four exact aspect prices, six exact peripheral
  entries, wrong centers/primal amounts/multiplicity, fresh output and recipe
  network/plan ownership.
- Two native world-ticked altar runs, one for each lamp. Each first pays50vis,
  one Aer and one Ignis plus four iron plates/amber for the physical precursor;
  an unknown lamp research cannot spend it. Paid study then starts a real caster
  altar at instability4 with65 essentia, consumes six real pedestal components,
  replaces the paid center with one fresh lamp and retains exactly3 units in
  every source jar. A later100-tick observation rejects duplicate output/debit.

Altar fixtures are at absoluteY250/280, clear a bounded loaded area and load
neighbor chunks. Sixteen original candle pairs supply native gain1.6 and the
tests wait95 real world ticks for stability25; they do not reset active craft
progress, supply buffer values or drive the block-entity tick manually. Both
parents, raw knowledge, aspect discovery in payment-only tests, raw materials,
the initial100vis, source jars with price+3, and the arena are explicit fixtures.
The two physical scanner specimens are also supplied. These tests establish
paid native routes when they pass; they do not establish an ordinary survival
playthrough, natural acquisition of every precursor, multiplayer or client
visual behavior. Root performs the fresh full server and hidden client runs.

## Фактически завершённый QA0.35

`lamps-035-server3` прошёл1422/1422; `lamps-035-client2`15/15, оба nativeexit0.
Два проверяющих просмотрели все15полных1600×1000PNG и оба листа.
Реальные130эссенции/100vis/4кристалла/64rawTheory/20XP, 320тиков паузы,
native рост при randomTickSpeed0, сердца/AI-рождение и физическая разборка
связаны с финальными logs/SHA. Подготовленные условия, принятый modern1tick
bridge, source quirks, ранние попытки и ограничения сохранены в
[VALIDATION.md](../../VALIDATION.md) и
[artifact-report0.35](../../validation/artifact-report-0.35.json).
