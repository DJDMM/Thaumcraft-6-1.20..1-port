# Arcane Bore: BETA26 study and manufacturing / 0.31

Authority is the released `work/Thaumcraft-1.12.2-6.1.BETA26.jar`,
SHA-256 `9425f8643581b27ff8845b087c8bc6fc10425a32942f1a3f0e265ce6b38f7b5f`,
and source commit `954022bb777b7546281fb36df8522f0ba6b43f81`.
The JAR hash was rechecked and its actual `assets/thaumcraft/research/artifice.json`
ARCANEBORE entry compared exactly with the pinned source. Independent local
`javap -p -c` extracts for ConfigRecipes, ConfigItems and InfusionRecipe plus
the released entry are preserved in `work/bore-031-progression-audit`.
This file defines contracts and test fixtures; integrated QA results belong
to VALIDATION.md, not an inferred success from these definitions.

## Original research

The ordered parents are `INFUSION`, `BASICTURRET`, `FOCUSBREAK`.
All three require strict completion to begin ARCANEBORE. Existing functional
research/recipe paths cover those parents; no Eldritch prerequisite, scan,
obtain item or craft proof is invented.

The two original stages charge only:

| Knowledge | Category | Points | Raw units |
| --- | --- | ---: | ---: |
| Theory | Golemancy |1|32|
| Observation | Artifice |1|16|
| Observation | Auromancy |1|16|

Start advances0→1, grants5XP and does not prepay stage1. The accepted
stage1 payment checks all three balances before any mutation, spends exactly
32/16/16, skips the empty final stage and saves completed stage3 with another
5XP. No stage warp or sibling is present. Missing book, incomplete parent,
stale stage and any one-unit knowledge shortage cannot partially pay.

The canonical census preserves the89 earlier keys and adds ARCANEBORE
for90/148. Mirrors and all unrelated unsupported branches remain unsupported.
The corresponding completed canonical chain opens exactly25 of the56 actual
infusion definitions; the exact set is tested rather than only its count.

## Released infusion

ConfigRecipes bytecode at offsets3905–4094 agrees with the pinned source:
the recipe is **infusion**, ID `thaumcraft:ArcaneBore`, bare research
`ARCANEBORE`, instability4 and turretPlacer output metadata2.
The existing modern recipe `thaumcraft:infusion/arcanebore` is retained
without a duplicate recipe. Its central basic turret and output `turret_bore`
preserve the prior split-ID normalization.

| Cost | Exact original amount |
| --- | ---: |
| Potentia |25|
| Terra |25|
| Machina |100|
| Vacuos |25|
| Motus |25|

Total200essentia. There is no arcane vis price or crystal price.
The central input is one basic turret. Eight peripheral positions contain
two greatwood planks, one complex mechanism, one brass plate (`plateBrass`),
one diamond pickaxe, one diamond shovel, one morphic resonator and one
rare-earth nugget. ConfigItems confirms `nugget` metadata10 is `rareearth`,
not quartz, quicksilver or a void metal. `Ingredient.fromItem` on the two
diamond tools accepts their actual durability/NBT, including worn enchanted
tools. Component matching is unordered and preserves both greatwood inputs;
iron tools, one missing plank or an extra unrelated ingredient cannot match.

Released InfusionRecipe.matches first invokes `isResearchKnown`, then the
central Ingredient and Forge RecipeMatcher. The original bare known gate
admits started stage1 without requiring completed stage3. Enabling the real
research makes the already reconstructed recipe accessible; it does not
complete research when crafting. This is ordinary replacement, not NBT
mutation: the manufactured bore is one fresh item and does not inherit the
basic turret's custom tags or peripheral tool enchantments/damage.
Diamond tools have no crafting remaining item; all eight components are
consumed by their real pedestal callbacks and the central input is replaced.

## Ordinary acquisition

All three parent research paths are functional before this tranche.
Greatwood trees already generate and provide logs/planks. Vanilla diamond
tools remain ordinary recipes. Brass and thaumium use METALLURGY, with
ordinary plates supporting complex mechanisms. Simple/complex mechanisms
retain the paid BASEARTIFICE bench routes. The morphic resonator retains
the BASEALCHEMY50vis/Aer1/Ignis1 recipe with brass plates, glass panes and
the original rare-earth nugget. Existing EarlyDeviceIngredientGameTests
exercise its real paid menu callback and the registered ore loot route.

Rare earth is the original stochastic nonsilk bonus on eligible coal,
lapis, redstone, diamond, emerald, Nether Quartz, Amber and quartz ore
families. The native global loot modifier already preserves original
thresholds and standard vanilla drops; modern deepslate counterparts map
their ore families. No supplied void material or unfinished late-world
source is required to manufacture a bore. This audit does not claim a new
manual end-to-end survival playthrough.

## Native progression and payment definitions

`ArcaneBoreProgressionGameTests` adds eight meaningful tests:

1. All three original parent completions, stage count,90-key census and
   unrelated mirror/late-branch closure.
2. Each separate one-unit shortage rejecting the complete mixed-category
   payment with inventory, balances and XP unchanged.
3. Exact32/16/16 payment, only three book knowledge rows, untouched inventory,
   zero warp, completed stage3,10XP, replay and persisted state.
4. Physical book and expected current stage on start/replay.
5. Original instability4/five aspects/eight ingredients, unknown rejection,
   started bare gate and actual recipe selector/output identity.
6. Worn enchanted tool matching, unordered components, every substitution,
   wrong central construct, missing/extra multiplicity and fresh output NBT.
7. Real recipe serializer round trip, wildcard durability and detached plan
   input/component/output data.
8. Actual native altar ticks/caster block callbacks, five physical jars,
   exactly200essentia/eight reagents/one basic turret paid for one bore,
   online-owner physical craft proof and100further native ticks without
   duplicate output or payment.

The last test installs its arena at absoluteY250 with explicitly loaded
surrounding chunks, sixteen real colored candle pairs and five physical
jars containing each exact recipe cost plus3. It does not manually tick
the matrix, supply a saved working plan, inject the manufactured bore,
lower instability or fake output callbacks. The native idle ticker charges
stability to25; its native craft then leaves precisely3 in every jar.
The diamond pickaxe is genuinely worn/enchanted and the shovel worn.
Parent completions, raw knowledge, materials, arena and jar contents are
explicit fixtures, not proof of a complete survival acquisition sequence.

The previous census assertions now expect90. The prior turret-chain test
expects ARCANEBORE to be implemented but locked for its unpaid parents;
it continues to require unsupported mirrors/late branches. The exact
functional infusion set is now25/56 including `infusion/arcanebore`.
