# Infusion upgrades: released BETA26 progression

Audit: 18 October 2026. This records the implementation contract for0.40,
not a claim that its pending native verification has passed.

The behavioral authority is `work/Thaumcraft-1.12.2-6.1.BETA26.jar`, SHA256
`9425f8643581b27ff8845b087c8bc6fc10425a32942f1a3f0e265ce6b38f7b5f`.
Readable source is pinned at `954022bb777b7546281fb36df8522f0ba6b43f81`.
The released `assets/thaumcraft/research/infusion.json` is byte-identical to
the retained modern legacy research resource; SHA256
`54d76698e0f842396e992fd9dec26a18b74f593cf765bcf6eb76211660e85913`.

`javap -p -c` evidence for ResearchManager and EntityEvents was captured in
`work/upgrades-040-reference/`. Original class SHA256:

| Class | SHA256 |
| --- | --- |
| ResearchManager | `0ee9b7926ac6fe02f6b82df69212927193cf889e3c8e844948e7ca6e8ca6b243` |
| EntityEvents | `29fd613b0d9826256b5d99dc653d42b8566a16f1501578ed7f4737c50ce34a00` |
| ItemFortressArmor | `be2a94920693b29d8757e45d2e6fd0a2a0228ca66d21d299ca3c705ae4b93d68` |

## Three studies

| Key | Strict completed parent | Raw payment | Obtain | Final Warp |
| --- | --- | --- | --- | --- |
| INFUSIONENCHANTMENT | ELEMENTALTOOLS | Theory INFUSION64 | none |0 |
| RUNICSHIELDING | INFUSIONENCHANTMENT | Theory INFUSION32 | amber1 |0 |
| FORTRESSMASK | ARMORFORTRESS | Theory INFUSION32 | none |2 |

Each has two stages, no siblings, no craft/aspect facts, +5XP on opening and
+5XP on paid completion. The final empty conclusion is completed with that
payment; no third player payment is invented. Final FORTRESSMASK Warp2 is
one permanent plus one normal Warp through the existing released transition.

Both first studies contain the malformed released descriptor
`minecraft:thaumcraft:enchanted_placeholder`. ResearchManager's item parser
returns no registered item for it and parseJsonOreList drops the empty result.
The existing modern invalid-ID rejection and the book's missing-item omission
retain this released behavior. Do not repair it into a sharpness/protection
item or an enchanted-book study cost. Enchanted books remain real *altar*
components for the eight enchanting recipes.

Amber obtains scan only original mainInventory: hotbar and inventory9..35,
including the selected main hand. Armor, Curios and offhand cannot pay study
obtains. All checks precede debits; stale expected stage, missing book,
spectator, missing parent, or short raw knowledge cannot partly pay.

## Existing working recipe boundary

Adding exactly these three keys changes the canonical count105→108/148.
The physical registry remains56, with operational research gates31→44:
eight `IE*` recipes, one dynamic RunicArmor, four fortress-helm mutations.
The eleven FAKE book previews remain display-only; no additional physical
recipes or costs were registered.

The exact original costs, matching, vanilla-enchantment distinction,
signed-byte item Warp and dynamic formula are audited in
[INFUSION-RECIPES-BETA26-AUDIT.md](INFUSION-RECIPES-BETA26-AUDIT.md).
First runic0→1 costs Protect40/Crystal20/Energy20 and Salis1+amber1;
1→2 costs60/30/30 and Salis1+two separate amber pedestals. Both instability5.
Modern negative/overflow charge rejection remains unchanged.

Original runic eligibility is ArmorItem **or the IBauble interface**, including
external implementations. The modern counterpart now accepts actual Curios
capabilities, in addition to armor and retained legacy catalogue accessory
classes. This is an explicit API adaptation, not arbitrary-item eligibility.
No parallel accessory inventory, death handling or shield storage is added.

## Applied effects and limits

The existing Fortress consumers already implement all three released masks:
Grinning reduces selected Warp-event severity by2..5, without subtracting
stored Warp; Angry applies Wither80 on the attacker with probability
incomingDamage/10; Sipping heals the attacker1HP with probability damage/12.
Masks are integer0/1/2; goggles is byte1 and grants both goggles/revealer
behavior by tag presence. Reapplying goggles or replacing masks is permitted.
The mutation changes one typed tag on the current physical central item and
preserves wear, vanilla enchantments, names and unrelated tags.

No rewrite of FortressArmorSupport or RunicShielding was necessary. Existing
shield consumers sum actual noncosmetic Curios+armor, pay1vis per absorption,
and retain the original wall-clock recharge/depletion intervals. The prior
normalization of malformed negative TC.RUNIC remains an explicit adaptation.
The full Warp-event system is not implemented by this tranche; the correct
Grinning severity operation is tested without claiming world Warp events.

`InfusionUpgradesProgressionGameTests` defines13 native checks: exact study
graph/payments/replays, all13 plans/network, reagent refusal, real Curios
eligibility, eight physical enchantment altars, four physical helm mutations
with real hurt/revealer consumers, and both a ring and armor physically
augmented0→1→2 and then used by native Curios/armor shield consumers.
The tests use actual matrix block-entity callbacks rather than asserting
elapsed world ticks. Parents/raw knowledge/materials/essentia/stable arenas
and deterministic shield clock are explicit fixtures. All container leftovers,
literal aspect totals and idle nonreplay are checked. This does not establish
full survival acquisition, real-time shield waits, multiplayer, all render
angles, audio, or the unfinished late Eldritch chain. Final pass counts,
client evidence and source bindings belong in the root validation report.

## Corrections exposed by the first native0.40 run

The first full native run is retained as a failure, not QA success. Its new
progression fixtures used CatalogModule.stack for amber, but that helper only
covers its own later catalogue entries. Amber belongs to the original primary
Thaumcraft item register, so the test accidentally supplied EMPTY. The test now
resolves the actual Forge item register and refuses missing/AIR fixtures; the
original amber study payment and production matcher were not changed.

The controlled Angry-mask wearer was a newly constructed ServerPlayer. Its
separate spawnInvulnerableTime60 rejected the attack before LivingHurtEvent;
clearing the unrelated living damage cooldown did not bypass that native guard.
The fixture now invokes61 ordinary ServerPlayer.tick callbacks before the attack.
This ages the real guard rather than changing a private invulnerability field;
these local callbacks still do not claim elapsed world ticks.

One actual recipe defect was also exposed: IELAMPLIGHT referenced the absent
thaumcraft:nitor item tag. The existing forge:nitor tag contains the original
color forms and retained primary yellow alias. That one ingredient now points
to forge:nitor, preserving original any-Nitor matching. No new recipe, tag file,
cost, resource count or research gate was introduced. Both all13 plan checks
and the eight paid native enchanting altars exercise this correction.

Native altar-start assertions now distinguish prepared-input matching, actual
start, and exact price/component count, retaining concrete observed state on
failure. These pure checks do not call the RNG-consuming plan selector twice.

## Corrections exposed by the second native0.40 run

The second full run is also retained as a failure. Its paid-ring shielding
fixture prepared and measured aura at the matrix, while the actual wearer
stood3blocks south. Those positions can belong to different aura chunks.
Production RunicShielding correctly charges the wearer's chunk. The fixture
now prepares, measures and restores the exact immutable player block position,
checks actual worn capacity and reports both chunks, absorption, owner Vis and
physical Curios NBT on failure. Neither Curios storage nor shield production
mechanics were changed for this correction.

The now-accepted Angry Ghost attack causes real native helmet durability wear.
The following Sipping mask mutation originally asserted the starting wear19,
incorrectly rejecting preservation of the current worn helmet. Each mutation
now copies the actual current physical helmet, changes only the expected
integer mask tag, and compares the full result against that copy. Real attack
wear remains paid; damage, names, vanilla enchantments and unrelated NBT are
not reset to the original fixture values.

These two corrections preserve all13 progression test definitions. The separate
bright-outdoor Lamplight failure and its modern lighting timing adaptation are
covered by the root-owned runtime audit; this progression audit does not treat
those failed runs as a completed QA pass.

## Фактически завершённый QA0.40

`infusion-upgrades-040-server3` прошёл1673/1673; `infusion-upgrades-040-client4`24/24, оба native exit0.
Два проверяющих просмотрели все24 полных1600×1000PNG и оба листа.
Фактические платежи, физические предметы, потребители и точные native
observations/SHA — в [artifact-report0.40](../../validation/artifact-report-0.40.json).
Фикстуры, ранние отказы, современные адаптации и пределы —
[VALIDATION.md](../../VALIDATION.md); полного survival/WarpEvents не заявляется.
