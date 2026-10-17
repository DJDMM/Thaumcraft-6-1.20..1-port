# Accessories: released BETA26 research, recipes and acquisition

Scope0.39.0,2026-10-16. This records the primary audit, implementation and
prepared progression checks. It does not claim a passed native server or
client run. Actual logs, source snapshots, exits, reviewed screenshots,
final counts and earlier failures belong in VALIDATION.md and the final
artifact report after execution.

## Released evidence

Primary: `Thaumcraft-1.12.2-6.1.BETA26.jar`, SHA256
`9425f8643581b27ff8845b087c8bc6fc10425a32942f1a3f0e265ce6b38f7b5f`.
Research JSON was read directly from that archive. Original ConfigRecipes,
ModConfig, ConfigEntities and ItemTCBase bytecode were inspected using
Java17 javap. Recovered source commit
`954022bb777b7546281fb36df8522f0ba6b43f81` is a readable companion only.
No TC4/5 jewelry, recharge or focus-pouch mechanic is imported.

| Original archive entry | SHA256 |
|---|---|
| `assets/thaumcraft/research/auromancy.json` | `654e16833b2b8715af74adf3a26b5bc68d874f559c38a56b170628f8a75da604` |
| `assets/thaumcraft/research/infusion.json` | `54d76698e0f842396e992fd9dec26a18b74f593cf765bcf6eb76211660e85913` |
| `thaumcraft/common/config/ConfigRecipes.class` | `487d32f43df9277fa845c129bc549da0126f34902a2d5089f9d97d204eb0f4e2` |
| `thaumcraft/common/config/ConfigItems.class` | `03ca145d619bcd808b1412ec7b2c8278400bc705afa691aa26a6d574f5b741d7` |
| `thaumcraft/common/items/baubles/ItemBaubles.class` | `ef4800f7cd3e7b94445de0d1e44843f5753c6958ea3157c650f30d7c745b13c5` |
| `thaumcraft/common/items/baubles/ItemAmuletVis.class` | `93276e1a0fe8daa432e52c4c53fd42fac379b08aedfbcb01bee6e01018ab710f` |
| `thaumcraft/common/items/casters/ItemFocusPouch.class` | `290df85bf39f75c9a4e0c88fcbb0c7ba877c488d17510c25ef9fea4863e602fd` |
| `thaumcraft/common/config/ModConfig.class` | `3998e471055d26d5f9ae9d18cb47fcb0ce02f99e4904ed6957f3788b00158c5a` |
| `thaumcraft/common/config/ConfigEntities.class` | `319bdd6970e54d6015b85aaaa2fceb8d53f0beba8a1afcdd5b56c9710f084894` |
| `thaumcraft/common/lib/research/ResearchManager.class` | `0ee9b7926ac6fe02f6b82df69212927193cf889e3c8e844948e7ca6e8ca6b243` |
| `thaumcraft/api/capabilities/IPlayerKnowledge$EnumKnowledgeType.class` | `e6bd216ee50f7b236a37f0cbb00d05303368dfcd264db5dd848795e35cca8be1` |

ConfigRecipes initializeArcaneRecipes offsets5061–5170 fix FocusPouch25vis,
null crystal cost, exact mundane belt metadata2, seven leather and a plain
vanilla gold ingot. initializeInfusionRecipes1477–1617 fixes VisAmulet,
instability6 and all200 essentia/six components. initializeNormalRecipes
2682–3132 registers the six ordinary jewelry shapes in baubles_stuff; these
are normal shaped Ore Dictionary recipes, not research-gated recipes.
The literal historical IDs spell `babule`, and are retained unchanged.
Raw knowledge uses the already audited THEORY32 and OBSERVATION16 units;
successful study advances each earn5 XP.

## Original studies and scope

FOCUSPOUCH is AUROMANCY at[-2,-1], strictly parented by completed
BASEAUROMANCY. Its first stage pays OBSERVATION;AUROMANCY;1 and
OBSERVATION;ARTIFICE;1, or16+16 raw observations. Its second stage shows
`thaumcraft:FocusPouch`.

VISAMULET is AUROMANCY at[-5,4], strictly parented by completed
RECHARGEPEDESTAL and INFUSION. Its first stage pays THEORY;AUROMANCY;2,
or64 raw theory. Its second stage shows `thaumcraft:VisAmulet`.

Both have exactly two stages, no siblings, no required aspect scan, no
obtain/craft specimen and no Warp. They finish at stored stage3, with5 XP
at start and5 XP at paid completion. Wrong knowledge categories/types,
already started parents, missing books, spectator/stale requests and
replays must not pay or invent progress. Original bare started recipe
gates remain distinct from strict completed parents and book conclusions.

Neither branch requires unsupported Eldritch or Warp progression. Adding
these two keys yields105/148 canonical entries. The already registered
VisAmulet infusion becomes one additional accessible path:31/56, with
56 registered original infusions unchanged. BASEELDRITCH, CURIOSITYBAND,
VOIDSEERPEARL and other late branches remain closed.

## Six ordinary recipes and loot-only forms

The BASEINFUSION chapter displays `thaumcraft:baubles_stuff`; that display
does not add a payment gate to vanilla crafting. Each recipe creates one
fresh item with no Vis, crystals or special XP award.

| Original/native recipe ID | Result metadata / modern item | Original grid | Inputs |
|---|---|---|---|
| `babuleamulet` |0 / `baubles_amulet_mundane`|` S `, `S S`, ` I `|string3, ingotBrass1|
| `babulering` |1 / `baubles_ring_mundane`|`NNN`, `N N`, `NNN`|nuggetBrass8|
| `babulegirdle` |2 / `baubles_girdle_mundane`|` L `, `L L`, ` I `|leather3, ingotBrass1|
| `babuleamuletfancy` |4 / `baubles_amulet_fancy`|` S `, `SGS`, ` I `|string3, gemDiamond1, ingotGold1|
| `babuleringfancy` |5 / `baubles_ring_fancy`|`NGN`, `N N`, `NNN`|nuggetGold7, gemDiamond1|
| `babulegirdlefancy` |6 / `baubles_girdle_fancy`|` L `, `LGL`, ` I `|leather3, gemDiamond1, ingotGold1|

Ore Dictionary inputs map to Forge string/leather/ingot/nugget/gem tags;
they are not narrowed to a single addon implementation. Brass already has
the original metallurgy and ingot/nugget conversion routes; leather,
string, gold and diamond have ordinary vanilla sources. All six normal
recipes belong to `thaumcraft:baubles_stuff`.

Metadata3 apprentice ring has no released recipe or canonical research.
Found Vis Amulet metadata0 likewise has no recipe. Do not invent either
craft or transform a found amulet into the infusion's required central.
Released ModConfig registers these actual weighted loot-bag entries:

- Common bags: mundane metadata0/1/2, weight10 each.
- Uncommon bags: fancy metadata4/5/6, weight5 each; found Vis Amulet weight6.
- Rare bags: apprentice ring metadata3, weight5; found Vis Amulet weight6.

The existing LootBagItem tables already retain those target entries.
Original ConfigEntities also contains the found amulet in the magician
Pech reward table and apprentice ring in the arcane Pech table, weight5
each. Those original tables are not a claim that complete Pech trade AI
or every natural bag-acquisition route is already implemented. ModConfig's
unused local rareLoot array alone is not proof of live chest generation.

## Pouch and crafted Vis Amulet payment

`thaumcraft:arcane/focuspouch` mirrors original FocusPouch:
25vis, no primal crystals, rows `LGL`, `LBL`, `LLL`.
L is leather, G is exactly ordinary vanilla gold ingot, B is exactly one
mundane belt/girdle metadata2. This is not the mundane ring, fancy belt,
apprentice ring or generic bauble. The full belt-plus-pouch chain consumes
ten leather, one brass ingot and one gold ingot, plus25vis.

The existing `thaumcraft:infusion/visamulet` is kept unchanged:
instability6, Auram50/Potentia100/Vacuos50,200ess total. Central is exactly
one mundane amulet metadata0. Its six unordered reagents are one Vis
Resonator and five exact typed crystals: Aer,Ignis,Aqua,Terra,Ordo. There
is no Perditio crystal. Output is one fresh crafted Vis Amulet metadata1;
it replaces rather than upgrades a found amulet and inherits no central NBT.

The ordinary central costs string3/brass1. Vis Resonator is an existing
earlier recipe; five typed primal crystals and existing essentia production
complete the material routes. Silverwood logs/saplings and Shimmerleaf
already expose Auram in the native aspect data; Potentia/Vacuos are ordinary
existing aspects and can also be supplied as typed essentia. Availability
of recipes/aspects does not certify a whole survival playthrough.

## Prepared native progression checks

AccessoriesProgressionGameTests checks both strict research graphs,
current105/148/31of56 census, exact raw payments/book rows/20 XP in total,
no invented physical/aspect requirements, atomic rejection/replay/save,
six normal CraftingMenu result-slot payments without research, every wrong
normal ingredient, released book grids and absence of loot-only crafts.
It also pays an actually crafted mundane belt into a native pouch result,
checks no-crystal25vis, unknown/short-Vis/wrong-belt/blocked-output refusal,
native recipe network synchronization, and exact Vis Amulet gate/typed
reagents/200ess/fresh output/network/book contract.

The owned altar check first pays the central through the normal crafting
menu, refuses an unknown Vis Amulet gate without partial debit, pays the
actual study, activates an original matrix with ordinary caster callbacks,
and runs native block-entity callbacks to consume the six physical reagents
and exactly200 prepared essentia: initial server fixture209
(Auram53/Potentia103/Vacuos53), leaving9 total,3 in each jar. The separate
client fixture starts at203 and leaves3 total; these are not the same totals.
Sixteen candle pairs
establish literal-cost stable operation through idle native callbacks.
Further callbacks must not duplicate output/payment. Each exact placed
position and old block-entity data is restored in finally, and the temporary
owner registration is removed. No broad terrain clearing or hidden timer,
stability, output, charge or RNG setter substitutes for the process.

These local block-entity callbacks prove payment semantics, not elapsed
world time; the separately executed client must establish real native
world-tick behavior. Raw parents/knowledge/ingredients, fixture positions,
candles and jars are prepared inputs. This suite does not certify Curios
equip effects, amulet target ordering, pouch container ownership, focus
selection, multiplayer, reload or audio by itself. Those common/client
contracts and actual runtime evidence are separate. Native client QA must
remain on the verified hidden non-input desktop, preserve manual saves,
avoid developer gameplay labels and deliver actual reviewed screenshots
beside each finished build.

## Фактически завершённый QA0.39

`accessories-039-server2` прошёл1628/1628; `accessories-039-client2`14/14, оба native exit0.
Два проверяющих просмотрели все14полных1600×1000PNG и оба листа.
Настоящие study/recipe payments, Curios equip, pouch/source ownership,
radial C2S, worn recharge/render и точные native observations/SHA
содержатся в [artifact-report0.39](../../validation/artifact-report-0.39.json).
Server altar209→9 и client203→3 — отдельные подготовленные маршруты.
Ранние отказы, фикстуры, современные адаптации и границы —
[VALIDATION.md](../../VALIDATION.md); полного survival не заявляется.
