# Grapple Gun: released BETA26 research and three arcane recipes

Scope0.38.0,2026-10-16. This is an independent source/bytecode audit and
the native progression verification contract. It is not a claim that the
current server or client run has passed. Exact final run IDs, snapshots,
observations, reviewed screenshots and limitations belong in VALIDATION.md
and the final0.38 artifact report after actual executions finish.

## Released authority

The primary is the released `Thaumcraft-1.12.2-6.1.BETA26.jar`, SHA256
`9425f8643581b27ff8845b087c8bc6fc10425a32942f1a3f0e265ce6b38f7b5f`.
The exact JSON was read directly from the archive, and the original class
files were inspected using Java17 javap. The recovered source at commit
`954022bb777b7546281fb36df8522f0ba6b43f81` is a readable companion only.
No TC4/5 research, recipe, ammunition or durability mechanic is inferred.

| Released entry | SHA256 |
|---|---|
| `assets/thaumcraft/research/artifice.json` | `615ac0c944aa19bf6bb3bf5f02158eb99577aa979bf0767dfd3aef0badc7834c` |
| `thaumcraft/common/config/ConfigRecipes.class` | `487d32f43df9277fa845c129bc549da0126f34902a2d5089f9d97d204eb0f4e2` |
| `thaumcraft/common/config/ConfigItems.class` | `03ca145d619bcd808b1412ec7b2c8278400bc705afa691aa26a6d574f5b741d7` |
| `thaumcraft/common/items/ItemTCBase.class` | `0edb72aadc72f5d800b4d6687c51f9918c72e8ffdf892fd5896a7b5128a20898` |
| `thaumcraft/common/lib/research/ResearchManager.class` | `0ee9b7926ac6fe02f6b82df69212927193cf889e3c8e844948e7ca6e8ca6b243` |
| `thaumcraft/api/capabilities/IPlayerKnowledge$EnumKnowledgeType.class` | `e6bd216ee50f7b236a37f0cbb00d05303368dfcd264db5dd848795e35cca8be1` |
| `thaumcraft/common/lib/network/playerdata/PacketSyncProgressToServer.class` | `99a99813a54773ced22db9b5ddafd5fbd51a070e01e2e0aad41365a73313b7f2` |

Released ConfigRecipes offsets8587–8721 establish Tip,8724–8855 Spool,
and8858–9028 Gun, including each grid, exact input constructor, gate and
crystal/Vis price. ConfigItems offsets191–265 and ItemTCBase constructor4–9
resolve the research specimen identity below. ResearchManager's
required_item branch calls parseJsonOreList; its parseJSONtoItemStack
offsets144–164 use the actual item registry lookup and count/metadata.
EnumKnowledgeType initialization fixes THEORY32; the original progress
packet multiplies the negative knowledge payment by that raw progression.
ResearchManager grants5 earned XP on each successful advance.

## Research and the singular nugget ID

GRAPPLEGUN is ARTIFICE at[2,3], with REVERSE display metadata. Its parents
are exactly BASEARTIFICE and RECHARGEPEDESTAL, both strictly completed.
It has two original stages, no siblings, no Warp and no required aspect
discovery/craft proof. Starting enters stage1 and earns5 XP. Stage1 pays:

- `THEORY;ARTIFICE;1`:32 raw theory.
- `THEORY;BASICS;1`:32 raw theory.
- `thaumcraft:nugget;1;10`:one physical rare-earth nugget in main inventory.

The singular `nugget` is valid. ConfigItems passes the literal name
`nugget` to ItemTCBase, whose constructor calls setRegistryName with that
same name; the Java field holding it happens to be named ItemsTC.nuggets.
Array index10 is `rareearth`. The released research parser therefore
obtains a real metadata10 stack; this is not one of the obsolete metal
or leather IDs that the original silently discards. The already pinned
LegacyResearchItems manifest maps this exact identity to
`thaumcraft:nugget_rareearth`. No speculative spelling repair is needed.

Paid completion earns another5 XP, retains no study specimen, and ends
at stored stage3. The conclusion contains exactly GrappleGunTip,
GrappleGunSpool and GrappleGun in that order. Bare started GRAPPLEGUN is
the original workbench recipe gate; canonical parent completion and the
normal book's paid concluding chapter remain separate checks. A supplied
finished gun never grants either parent, the research or its payment.

## Exact original manufacturing chain

All three recipes are ordinary shaped arcane recipes and create one fresh
item. None is an infusion or carries charge/loaded NBT into its result.

| Original ID / native ID | Vis | Crystals | Ordered rows |
|---|---:|---|---|
| `thaumcraft:GrappleGunTip` / `thaumcraft:arcane/grappleguntip` |25|Terra1|`BRB`, `RHR`, `BRB`|
| `thaumcraft:GrappleGunSpool` / `thaumcraft:arcane/grapplegunspool` |25|Aqua1|`SHS`, `SGS`, `SSS`|
| `thaumcraft:GrappleGun` / `thaumcraft:arcane/grapplegun` |75|Aer1,Ignis1|`  S`, `TII`, ` BW`|

Tip uses B=plateBrass four times, R=exact ItemsTC.nuggets/meta10 four
times, H=ordinary vanilla tripwire hook once. Spool uses S=string seven
times, H=ordinary tripwire hook once and G=exact simple arcane mechanism
once. Gun uses S=the paid spool, T=the paid tip, I=plateIron twice,
B=plateBrass once and W=plankWood once. Empty cells remain empty.

Original plateBrass, plateIron, string and plankWood are Ore Dictionary
inputs. Their modern equivalents are `forge:plates/brass`,
`forge:plates/iron`, `forge:string` and `minecraft:planks`. They must not
be narrowed to a single manufacturer's plate or vanilla string while
the book accepts tag equivalents. Rare-earth nuggets, tripwire hooks,
mechanism, tip and spool are exact source-item inputs. Addon items must
actually join the corresponding tag; no universal third-party recipe
compatibility is claimed from one vanilla/TC fixture.

The three target benches together pay125 Vis and four crystals:
Aer1/Ignis1/Aqua1/Terra1. Study plus tip uses five rare-earth nuggets in
total. Mundane target components total brass plates5, iron plates2,
tripwire hooks2, string7, one plank and one simple mechanism, with the
intermediate tip and spool physically consumed by the final gun craft.

The mechanism is an earlier BASEARTIFICE recipe,10 Vis/Ignis1/Aqua1;
manufacturing it as well makes135 Vis and six crystals for that longer
chain. Do not count supplied mechanism stock as a fourth paid craft.
Existing ordinary recipes make brass/iron plates from three ingots;
the earlier brass metallurgy, simple-mechanism recipe and non-silk
RareEarthOreLootModifier provide existing material paths. These source
paths do not themselves certify a complete ordinary survival playthrough.

## Native checks and an identified fixture error

The initial six GrappleGunProgressionGameTests definitions cover strict
parents/103-key census, atomic missing-theory/offhand refusal, exact
64-theory/one-specimen/10-XP payment and replay/save, pinned book grids,
three native paid menu crafts with actual intermediate ownership, and
unknown-research/short-Vis/missing-crystal/blocked-cursor refusal.
Parents, raw balances, mundane ingredients and temporary aura are explicit
fixtures. Native ArcaneWorkbenchMenu result clicks perform actual payment;
the study's ResearchNetwork path performs actual stage validation.
Additional tag-equivalent assertions and final test counts are to be
recorded from the actual final source/report rather than assumed here.

During the initial native run the new test vectors had Aqua/Terra swapped.
ArcaneModule.PRIMALS is Aer,Ignis,Aqua,Terra,Ordo,Perditio: the correct
Tip vector is[0,0,0,1,0,0], and Spool is[0,0,1,0,0,0]. The source recipe
JSON already specified the correct Terra/Aqua costs. The first run
reported both the aggregate paid-craft assertion and pinned-price
assertion failing. Correcting these test fixtures does not justify
changing either released recipe price or calling that failed attempt a
pass. A fresh complete native run is required after the correction.

The implemented-key set was independently derived as103/148, with
56 registered infusion recipes and30 reachable through implemented gates;
GRAPPLEGUN adds no infusion. This is an implementation census, not native
execution evidence. BASEELDRITCH, VISAMULET and remaining late keys are
not implicitly unlocked by the addition. Recharge, firing, collision,
rope/model rendering and their source quirks belong to the separate
Grapple common/client audits; a paid uncharged recipe alone proves none
of those mechanics. No visible client launch, input-desktop activation,
manual-save changes or developer gameplay labels are part of this scope.

## Фактически завершённый QA0.38

`grapple-038-server2` прошёл1559/1559; `grapple-038-client2`11/11, оба native exit0.
Два проверяющих просмотрели все11полных1600×1000PNG и оба листа.
Реальные64Theory/nugget1/10XP/3bench125vis4crystals/paidTipSpoolConsumed,
nativepedestal100charge/physicalpickup/3shots97/mainoffmovement/refire/2shift
связаны с final logs/SHA. Современная GLE-tessellation адаптация,
подготовленные условия, ранние отказы и границы — [VALIDATION.md](../../VALIDATION.md)
и [artifact-report0.38](../../validation/artifact-report-0.38.json).
