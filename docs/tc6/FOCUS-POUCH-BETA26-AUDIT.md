# Focus Pouch and focus radial — released BETA26

Scope0.39.0,2026-10-16. This document records the primary behavioral contract
and implementation boundaries. It does not claim executed QA in advance.
Root records actual native server/client results and reviewed screenshots.

## Primary authority

Released `Thaumcraft-1.12.2-6.1.BETA26.jar`, SHA256
`9425f8643581b27ff8845b087c8bc6fc10425a32942f1a3f0e265ce6b38f7b5f`,
is authoritative. The pinned readable reference is commit
`954022bb777b7546281fb36df8522f0ba6b43f81`. The audit inspected released
`ItemFocusPouch`, `ContainerFocusPouch`, `InventoryFocusPouch`, `GuiFocusPouch`,
`CasterManager`, `KeyHandler`, `PacketFocusChangeToServer` and
`WandRenderingHandler` bytecode alongside their readable counterparts.

The pouch is uncommon, stack1, undamageable and never foil. It implements
Baubles BELT; equip/unequip both return true and its worn callback is empty.
Opening has no research check, so gifted pouches remain usable. Original use
opens server GUI5 then returns the inherited PASS result. The port preserves
that result and opens a native server menu through the actual requested hand.

It stores eighteen slots in the standard root `Items` NBT list. Each slot has
capacity1 and accepts any original ItemFocus subclass, including a blank focus;
it rejects ordinary items and nested bags. Compiled spell selection separately
requires a valid package. Ordinary focus items retain their complete NBT.

## Original GUI and media

The native menu preserves all54 container slots. Pouch slots are6×3 at
`(37+column*18,51+row*18)`, player main rows start at `(8,151)` and hotbar
at `(8,209)`. The screen is175×232. It draws the released texture with native
background dimming and ordinary item tooltips, without a title, status panel,
instructions or invented controls. The main-hand holder receives the original
red-X16×16 lock overlay from texture `(240,0)`. Hotbar keys are disabled.
Offhand opening has no fake main-hotbar overlay.

Three256×256 PNG were copied byte-for-byte from the released JAR; the local
readable reference bytes matched before copying:

- `textures/gui/gui_focuspouch.png`: SHA256
  `d5002659e6972d7c982c3a3667fad6a6310a10ccfb935db9c82cdf695870137a`.
- `textures/misc/radial.png`: SHA256
  `07d379a3e754ed351b633bcd70649a465bbc9e1864c001d9886540d4a89c0d2d`.
- `textures/misc/radial2.png`: SHA256
  `931d88b76ff06276740628c956ac58e7f6d229e529b4f0862d474ecc98fadc61`.

The radial now draws those two original counter-rotating grey/alpha.5 rings,
with radius `16+2.5*choiceCount`, size multipliers2.75/2.55 and the released
quad UV orientation. Opening and closing use the original150ms scale slope;
hover increases toward1.3 at1/150ms and decreases toward1 at1/250ms.
The initial angle is `-90*scale`; all available focus items appear around the
circle without invented pagination. The installed focus remains in the center.
Original focus tooltips appear at the original upper-left anchor. There is no
handmade box, dark selector panel, title, instructions or remove-button label.

Hold F to view; release over a focus or left-click it to select. Shift+F removes
the installed focus. Both caster hands work, preferring main when both contain
casters. The configured binding can also be a mouse button. The selection
screen does not pause the world. It is a modern cursor/event bridge replacing
the original direct LWJGL cursor manipulation; it retains the source visual
and hold/release/click behavior without global computer input. Closing keeps
its brief150ms visual decay within that screen instead of an in-game HUD event.

## Physical ownership and explicit modern adaptations

Released right-click passes the requested hand, but its container ignores it:
the constructor always captures `InventoryPlayer.getCurrentItem()` and locks
`selected+45`. Consequently offhand-only original opening can load the wrong
or empty stack. Original `canInteractWith` always returns true; the client only
closes if that selected main slot becomes empty. On close it serializes its
cached input and may replace an active-hand stack based only on item equality.
These unsafe identity/hand quirks are not replicated as dupe or overwrite paths.

The native server menu captures the actual requested hand, selected slot and
live ItemStack reference. It refuses holder pickup/throw/quick-move and SWAP,
and invalidates if the owning hand/reference or main selection changes. It
persists every accepted native inventory change into that same physical bag;
the original wrote only on close. A current full-NBT snapshot also invalidates
externally edited storage, so close cannot overwrite another edit. A detached
bag is never reinserted, copied to another hand or flushed from a stale menu.
The client accepts ordinary native stack replacement during inventory sync;
the server alone authorizes physical identity and mutations. Native carried
cursor handling remains with AbstractContainerMenu.

Malformed encoded entries, duplicate or out-of-range slots, unknown/non-focus
items, nested pouches, wrong count tags and counts other than1 reject editing
without normalizing or silently deleting the original payload. This protects
saved data rather than pretending arbitrary malformed items fit eighteen slots.

The selector scans actual non-cosmetic equipped stacks in the seven original
logical slot order, followed by extra Curios slots, then main inventory0..35.
Both loose compiled foci and pouch contents participate. An offhand pouch is
openable but is not invented as another selector source: the original scans
Baubles plus the36 main slots. Returned foci prefer an available pouch slot,
then ordinary main inventory. Taking a focus first reserves its vacated slot,
so a full bag/main inventory can still swap without duplication or loss.

Released selection uses TreeMap keys `displayName+sortingHash`: equal keys
overwrite earlier physical sources. The modern radial keeps the same ordering
but retains distinct physical sources instead of erasing duplicates. Old
client-provided sorting names no longer identify an inventory location. Source
protocol2 carries kind/outer/inner and expected caster, pouch and focus snapshots.
The server checks bounds, compiled focus, whole snapshots, actual live ownership
and ordinary inventory-menu context before committing. This is a value-checked
physical slot transaction, not a claim of globally unique item UUIDs across
identical copies. Old public loose-slot APIs remain for earlier regression QA.

Source and return mutations are planned together before writing. No unavailable
destination may consume a focus. Only touched bags receive NBT writes. Equipped
bags use the actual Curios stack references; native Curios full-NBT comparison
synchronizes them on its ordinary tick. No fake accessory inventory, internal
Curios packet, direct drop fallback or extra worn-pouch GUI/key is introduced.

The shared focus vis-consumption modifier now uses centralized GearSupport
discount calculation once. It therefore includes actual equipped vis-discount
gear and applies the strongest normal/infectious exhaustion penalty once,
preserving the previous five-percent goggles/twenty-percent exhaustion1.15 case.

## Progression and verification boundaries

FOCUSPOUCH requires completed BASEAUROMANCY, one original AUROMANCY observation
and ARTIFICE observation (16raw each) and earns5+5XP across its two stages.
Its original arcane recipe costs25Vis, no crystals, seven leather, one exact
vanilla gold ingot and a mundane belt. Research/recipes and shared accessory
foundation have separate owners and audits; this document does not claim those
paths merely because the item or an archived research page exists.

`FocusPouchGameTests` defines25 meaningful native trials: storage/NBT/filter/
geometry, real quick-move and ordinary clicks, holder/number/offhand swap locks,
requested-hand identity, detached/external edits, malformed payload preservation,
full inventories and reserved return slots, stale/bounds requests, distinct
equal-sort-key sources, open-container concurrency, offhand caster and four
actual Curios belt source/return/ordering/removal trials. Supplied valid spell
packages and FakePlayers are explicit test inputs, not claims of spell crafting.
These trials do not launch a client, change terrain or spawn test actors.
An isolated native click regression also models synchronized replacement of the
client inventory stack while its menu retains the old reference: the immutable
main-holder index remains protected before and after that replacement. This is
a server-hosted prediction model, not a claim that GameTests instantiate a client.

The joint hidden client driver is root-owned. It must use paid pouch/research,
actual native menu clicks, real equipped Curios storage and C2S selection, with
fresh1600×1000 captures reviewed in full. No pass, multiplayer, third-party
Curios matrix, audio or uninterrupted ordinary Survival run is claimed here.

The native quick-move conservation fixture snapshots its expected focus before
transfer: Minecraft shrinks the original source stack to EMPTY. The assertion
therefore checks that empty source, the unchanged full-NBT snapshot in the bag,
and exactly one matching focus after withdrawal. It never compares storage to
the already-consumed mutable source reference. The client driver accepts both
native Curios menu versions, including the default CuriosContainerV2, without
changing the dependency's menu configuration.

## Фактически завершённый QA0.39

`accessories-039-server2` прошёл1628/1628; `accessories-039-client2`14/14, оба native exit0.
Два проверяющих просмотрели все14полных1600×1000PNG и оба листа.
Настоящие study/recipe payments, Curios equip, pouch/source ownership,
radial C2S, worn recharge/render и точные native observations/SHA
содержатся в [artifact-report0.39](../../validation/artifact-report-0.39.json).
Server altar209→9 и client203→3 — отдельные подготовленные маршруты.
Ранние отказы, фикстуры, современные адаптации и границы —
[VALIDATION.md](../../VALIDATION.md); полного survival не заявляется.
