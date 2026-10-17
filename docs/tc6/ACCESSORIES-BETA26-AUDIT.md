# BETA26 accessories: storage, consumers and Vis Amulet

This audit covers the 0.39 common accessory foundation and both Vis Amulets.
Focus Pouch ownership/menu/selector and research/recipes have separate audits.
The tests described below are prepared checks until a native run is recorded in
`VALIDATION.md` and the final artifact report; this document alone is not a pass.

## Primary evidence

Authority is the released `Thaumcraft-1.12.2-6.1.BETA26.jar`, SHA-256
`9425f8643581b27ff8845b087c8bc6fc10425a32942f1a3f0e265ce6b38f7b5f`.
Java 17 `javap -c -p` was read for `ItemBaubles`, `ItemAmuletVis`,
`ItemFocusPouch`, `CasterManager`, `EntityUtils`, `PlayerEvents`, `ItemGoggles`,
`RechargeHelper` and `WarpEvents`. Outside-project evidence files are:

| Evidence | SHA-256 |
| --- | --- |
| `work/accessories-039-tc6-primary-javap.txt` | `b2058e63381b73bb2b00c239db8944dd6271ad2231f0b9559dd3c0519dd7047e` |
| `work/accessories-039-recharge-warp-primary-javap.txt` | `c4c86a4867ca85a9069ba37d0ee35cb47e23b122ecb0955985e0c48d13006339` |

The source reference commit
`954022bb777b7546281fb36df8522f0ba6b43f81` is a navigation aid, not a
replacement for released bytecode. No TC4/5 behavior is inferred.

Baubles itself is verified against official
[Azanor/Baubles commit 5df3d468afed5ebba1d5df983221e90ff2a9a9b3](https://github.com/Azanor/Baubles/tree/5df3d468afed5ebba1d5df983221e90ff2a9a9b3),
whose `build.gradle` declares `1.12-1.5.2`. In particular:

| Pinned source | SHA-256 of fetched source |
| --- | --- |
| `baubles/api/BaubleType.java` | `dca82def0dcc402ac7c91d7c05f083923a86416063d387f5b3e742059d1dc4b4` |
| `baubles/common/event/EventHandlerEntity.java` | `0f9a5cddc018316e1daab97d71e9a7416c76bd951a673db81ae12ddf2ff91db4` |

Modern dependency is official Curios Forge **5.14.1+1.20.1**. Its binary SHA-256
is `6d77ae8ad532fdf303390f404b0081b3b4ac4f61e7f1f4b4d4a9077e132dae4f`;
sources JAR SHA-256 is
`610da786714970b3f3952dc0710b39224bfeba888a94be7619735db16b71e431`.
Official source commit
`a91da6ba2396f6ed9042c8a47a471ca5efc97da8` declares that same version.
Its `CuriosEventHandler.java`, SHA-256
`df2be1dcb92b4d2bdbe7ebfb92263705091488e7221325f0fa5bf9404319583e`,
was read alongside `DynamicStackHandler`, `CurioInventoryCapability` and the
public API. Dependency lifecycle semantics are from this 1.20.1 source, not
newer Curios documentation.

## Seven original indices and modern storage

Official `BaubleType` assigns AMULET(0), RING(1,2), BELT(3), HEAD(4), BODY(5),
CHARM(6); TRINKET accepts all seven. The port binds real Curios groups as follows:

| Released index | Curios identifier/index |
| --- | --- |
| 0 | `necklace[0]` |
| 1 | `ring[0]` |
| 2 | `ring[1]` |
| 3 | `belt[0]` |
| 4 | `head[0]` |
| 5 | `body[0]` |
| 6 | `charm[0]` |

`AccessoriesSupport.equipped` reads actual `getStacks()` references in this
fixed order. Empty/unavailable original slots retain their index; cosmetics are
excluded and hidden normal equipment remains functional. Modern extra slots
append after seven in sorted identifier/index order. This extension allows
other Curios mods without depending on the modern GUI/map ordering. The default
datapack exposes seven normal slots, not a parallel TC inventory or catalog GUI.

Curios owns capability attachment, native item/slot validation, equip/unequip
events, normal stack lifecycle, synchronization, saving and death/clone. TC does
not duplicate that storage or add another drop handler. `insertItem` validates;
raw `setStackInSlot` is an administrative fixture API and bypasses validation.
Tests distinguish those routes. Ordinary items do not invent equip effects.

Released Baubles invokes worn callbacks in index order on both sides at player
END tick. Its menu `SlotBauble` invokes equip/unequip callbacks; raw handler
mutation alone does not. It synchronizes changes and opted-in auto-sync items,
and clones serialized storage. Curios invokes `curioTick` through its native
living tick and detects ordinary item/NBT changes against copied previous
stacks, sending native packets and equip/unequip/change callbacks. This is an
explicit modern lifecycle adaptation; packet bytes and callback timing are not
claimed to match Baubles.

## Ordinary forms and consumers

The seven released `ItemBaubles` variants are mundane amulet/ring/belt,
apprentice ring, fancy amulet/ring/belt. All stack to one, have no durability,
and use their original slot types. Variants 0..2 are common, 3..6 uncommon.
Only apprentice ring returns five percent Vis discount; two distinct rings add
five plus five. There is no original unique-ring restriction. Mundane/fancy
forms do not invent passive stats.

Released consumer scopes are retained using actual equipped normal stacks:

- `CasterManager.getTotalVisDiscount`: accessories then four armor slots,
  subtract ten percent times one plus the **maximum** ordinary/infectious Vis
  Exhaust amplifier, then divide by 100. The caster consumes this shared value
  once; it does not apply the disease a second time.
- `WarpEvents.getWarpFromGear`: main hand, four armor slots, seven accessories;
  offhand is excluded. The original signed `TC.WARP` byte remains signed.
- Runic maximum: armor and accessories. Existing port behavior normalizes a
  malformed negative signed `TC.RUNIC` byte to zero. This normalization predates
  0.39 and is deliberately retained; it is **not** released signed-byte parity.
- `EntityUtils.hasGoggles`: main hand, armor, accessories, excluding offhand.
  Revealer checks both hands, armor and accessories. Native head-slot goggles
  keep their original five-percent discount. Distinct helmet and accessory
  goggles can each contribute; hidden ordinary gear remains functional.

## Vis Stone and Amulet of Vis

Released `ItemAmuletVis` implements IBauble, **not IRechargable**. Both are
necklace accessories, stack one, no damage, no internal charge/buffer/fuel/menu.
Found stone is uncommon and checks `ticksExisted % 40 == 0`; crafted amulet is
rare and checks `% 5 == 0`. The aqua `item.amulet_vis.text` tooltip is retained.

On eligible server player ticks, it tries hotbar indices 0..8, then Baubles
indices 0..6, then the armor list FEET/LEGS/CHEST/HEAD. Every attempt asks for one
charge unit from ambient aura at the wearer. First strictly positive credit
returns immediately. Offhand and main inventory 9..35 are not targets.
`RechargeHelper.rechargeItem` bytecode offsets 68..95 drains float Vis, performs
`f2i`, adds a positive integer credit and returns that integer as float. Thus
0.75 Vis can be drained for zero credit; subsequent targets still get tried but
no fractional charge is accumulated. This released quirk is preserved.
`AURAPRESERVE` skips below ten percent base aura; the exact boundary is allowed.
Creative wearers still pay the same ambient Vis.

Modern guards reject cosmetic/detached/dead/spectator sources, changed worlds or
positions, unloaded wearer chunks, block snapshot capture/restore, and nested
attempts for the same wearer. A target's external max-charge callback is queried
before payment, then actual source and target identities/NBT are rechecked.
After native aura payment, the charge NBT is committed directly without another
external max-charge callback. That differs from the original helper's second
max query during `addCharge`, preventing a stale callback from consuming paid
Vis for a moved/replaced stack. These are explicit ownership protections, not
an invented recharge buffer. The loaded-only claim applies to this active
recharge path; it is not a claim that all arbitrary Curios addon callbacks are
bounded or side-effect-free. A foreign callback's own unrelated side effects
cannot be rolled back universally.

## Native persistence and death adaptations

Released Baubles copies equipped stacks into native drops when keepInventory
is false, delay40, then clears storage. With keepInventory true it retains
them for serialized PlayerEvent.Clone. It has no Vanishing Curse branch there.

Curios performs the equivalent ownership transition through its native
LivingDropsEvent and clone handler, while honoring its server `keepCurios`
configuration, item/slot/event drop rules and **Vanishing Curse**. Those modern
rules remain active. TC does not silently override other mods' rules to copy
Baubles. Curios clears dropped handler stacks before posting CurioDropsEvent;
a third party canceling that event owns its behavior. No universal cancellation
refund, addon death matrix, actual player respawn or cross-version Baubles-save
migration is claimed by the common tests.

## Prepared native checks and limits

`AccessoriesGameTests` prepares **29** native cases using actual registered TC
items, Curios capability/slot handlers, Forge lifecycle events and real aura:
seven-slot order/capacity/validation, all seven bauble forms, two rings and
independent goggles, equip/unequip veto, cosmetic/hidden/expanded slots, both
amulet cadences, original target priority/exclusions, actual Curios worn dispatch,
fractional aura, preservation, source guards, full/empty targets, storage copy,
clone, both gamerule death routes, native Vanishing, Warp/Goggles/Runic consumers,
one shared exhaustion penalty and copied NBT-change synchronization state.

Every case owns an isolated loaded aura chunk and restores its aura; test
players are untracked and have unconnected transports. Tick counters, knowledge,
initial aura and raw addon-slot/armor-list markers are declared fixtures. Guns
in raw accessory/armor slots test ordering only, not a new legal equip route.
Tests assert native sync **decision/snapshot/change event**, not delivery to an
untracked client. The joint hidden-client trial must establish actual packet/UI
observations separately. There are no gameplay dummy receivers/items or new
developer labels. Arbitrary hostile addon callbacks, multiplayer, persisted
world restart and full survival acquisition remain outside these case claims.

## Фактически завершённый QA0.39

`accessories-039-server2` прошёл1628/1628; `accessories-039-client2`14/14, оба native exit0.
Два проверяющих просмотрели все14полных1600×1000PNG и оба листа.
Настоящие study/recipe payments, Curios equip, pouch/source ownership,
radial C2S, worn recharge/render и точные native observations/SHA
содержатся в [artifact-report0.39](../../validation/artifact-report-0.39.json).
Server altar209→9 и client203→3 — отдельные подготовленные маршруты.
Ранние отказы, фикстуры, современные адаптации и границы —
[VALIDATION.md](../../VALIDATION.md); полного survival не заявляется.
