# Released BETA26 Vis Generator behavior

Authority is the released `Thaumcraft-1.12.2-6.1.BETA26.jar`, SHA-256 `9425f8643581b27ff8845b087c8bc6fc10425a32942f1a3f0e265ce6b38f7b5f`. Decompiled source at pinned reference commit `954022bb777b7546281fb36df8522f0ba6b43f81` is a reading aid. The implementation was checked against actual released bytecode, including inherited block behavior, rather than inferred from previous Thaumcraft versions.

## Primary evidence

Reproducible evidence is outside the distributable source, under the workspace `work/` directory. Invoke Java 17 `javap -c -p -classpath <released-jar> <class>`; mappings for vanilla names are in `work/vanilla-1.12.2-audit/joined.srg`.

| Evidence file | Classes / purpose | SHA-256 |
| --- | --- | --- |
| `generator-037-tile-javap.txt` | `TileVisGenerator`: real server tick, recharge, NBT and FE methods | `6c330aa2fe3d287789f314265515b60f3e800802fc424134cfe3675a953ee198` |
| `generator-037-block-javap.txt` | `BlockVisGenerator`: first receiver placement, spark, physical bounds | `a09f4120fced8ecdb40849d4b6ce75fb77fc81b9c76f345472d7cad2a8cbc072` |
| `generator-037-base-javap.txt` | `BlockTCDevice`, `BlockTCTile`, `IBlockFacing`, `Utils` | `cdf0ef748128775cd2b5936095de9e5ac625fefb70ddef604776a2944143ba47` |
| `generator-037-rotation-javap.txt` | Released `thaumcraft.codechicken.lib.vec.Rotation` | `e7b50eacc7d25f6dae57f7b6f4c0d0e0198ce60d49265dad89e83430a74d53e3` |
| `generator-037-legacy-facing-javap.txt` | Vanilla `fa` / `EnumFacing`, eye-position placement | `7c0522770bb3515b81a84d1bfb3c0423aba1a506b8519a2187b5b9dd9a29c164` |
| `generator-037-material-javap.txt` | Vanilla `bcz` / `Material`, actual wood lava ignition flag | `4252fb40195c9e41dcc10e715a7c81eae75ef8349cc6387a498cd16caf23c030` |

The official legacy vanilla client JAR used for the last two rows has SHA-256 `8ada07da5ee77dad3527bd7278fbd05ee1fc8a597813b216a871a2d7d64cc64f`. The preceding Pattern/Dioptra primary state-container audit establishes the legacy allowed-value ordering: the immutable boolean set starts TRUE, the six-direction EnumSet starts DOWN. `BlockTCDevice` discards the results of its constructor `withProperty` calls, so the actual base state is **DOWN, enabled TRUE**, even though its discarded six-facing expression mentions UP. Normal placement explicitly selects enabled TRUE; native `onPlace` and neighbor updates subsequently select inverse redstone power.

## Exact released operation

`TileVisGenerator.update` offsets 0–17 gate on server side and enabled state. It recharges first (20–21), then looks at the facing neighbor and its opposite capability side (24–115), checks `canReceive` (117–124), and makes one **non-simulated** `receiveEnergy(min(energy,20),false)` call (127–148). Only a positive reported acceptance is subtracted (150–163), marked dirty and, when the resulting energy is zero, synchronized (166–179). There is no cadence counter and no simulated FE preflight. A receiving neighbor is still offered zero FE if an empty source obtained no aura.

The private recharge routine runs only when `energy == 0`. It really drains up to **1.0 Vis**, multiplies the returned float by **1000.0f** and truncates with `f2i`. It marks dirty and synchronizes even when no Vis was available. It does this without requiring a receiver: an enabled idle generator keeps one paid buffer. A positive partial buffer never refills. Disabled generators neither charge nor push.

Capacity is **1000 FE**. The native exposed capability exists only on the facing side, including while disabled. Null and other sides fall through to inherited capabilities. Literal original API quirks are retained: **`canExtract() == true`, but `extractEnergy(...) == 0` for both simulated and real calls; `canReceive() == false`, `receiveEnergy(...) == 0`**. This is a push source, with no invented pull extraction, fuel slots, inventory, GUI, sound loop or player status text. The original saved/synchronized field is the integer `energy` only. Refill and complete exhaustion synchronize; ordinary positive partial drains do not receive an invented per-tick sync.

The six-face placement search visits DOWN, UP, NORTH, SOUTH, WEST, EAST and selects the first neighbor whose opposite-side energy capability can receive. With no receiver, the inherited legacy eye-position rule applies: when both X/Z distances from the placed block center are strictly below two, eyeY minus blockY strictly above two means UP, and blockY above eyeY means DOWN. Otherwise the player's opposite horizontal direction is used. Sneaking reverses this fallback. It does not override a discovered receiver.

The block is wood with wood sounds, hardness **2**, effective modern explosion resistance **12**, unrestricted harvesting, false full/opaque/support faces and no conductor/suffocation/view-blocking predicates. Old `Block.setResistance(20)` stores 60 and exposes 12 through the legacy explosion getter. The old wood `Material.g()` sets its combustible flag, so the modern block explicitly uses `ignitedByLava()`; no extra FireBlock flammability table or burn rate is invented. Dismantling returns one ordinary fresh block item, discarding buffered energy as in the original; it does not create a portable charged item.

The base AABB is `(.25,0,.25)–(.75,.875,.75)`. `Utils.rotateBlockAABB` translates by negative half a block, applies `Rotation.sideRotations[face.index]`, then translates back. Released CCL DOWN is identity, UP reverses Y/Z, NORTH maps Y=-Z/Z=Y, SOUTH maps Y=Z/Z=-Y, WEST maps X=Y/Y=-X, EAST maps X=-Y/Y=X. Exact native outline/collision boxes are:

| Facing | Minimum | Maximum |
| --- | --- | --- |
| DOWN | .25,0,.25 | .75,.875,.75 |
| UP | .25,.125,.25 | .75,1,.75 |
| NORTH | .25,.25,0 | .75,.75,.875 |
| SOUTH | .25,.25,.125 | .75,.75,1 |
| WEST | 0,.25,.25 | .875,.75,.75 |
| EAST | .125,.25,.25 | 1,.75,.75 |

The original client random-display spark checks the facing capability's positive stored energy, **without an enabled check**. A charged powered generator therefore continues to spark. The separate client audit describes actual original texture, random offsets, particle lifetime/rendering and the executed hidden-client evidence.

## Explicit modern callback and loading contract

Active FE work and capability neighbor selection are bounded to already loaded native chunks and legal build height. Server ticks require the original attached, nonremoved block entity and reject Forge captured/restoring snapshots and provisional unpaid native item placement. Active FE work and palette migration do not force-load chunks. This does not promise that ordinary native block placement/redstone notifications or an arbitrary addon's own capability callback never request another chunk: inverse redstone retains the native `Level.hasNeighborSignal` behavior. The missing-neighbor test therefore installs a pre-existing loaded source by an owned raw palette fixture, then checks the actual native energy tick without ordinary placement notifications. Only missing or exact empty old catalogue anchors are repaired; existing native energy, foreign block entities and nonempty catalogue save data remain their owners' property. Repair is idempotent and sends a native chunk/block update.

The real native BlockItem route retains normal placement validation, BlockStateTag/BlockEntityTag, sound and criteria. A detached shadow stack prevents prepayment mutation of the physical hand during Forge callbacks. The hand is debited once after an uncanceled, identity-stable permitted placement; cancellation restores only this transaction's unchanged target. Separate callback-owned blocks, hands and captured receipts are not rolled back. Provisional NBT energy cannot execute a server tick before the real physical hand payment.

Ordinary addon `IEnergyStorage` capabilities are accepted, including real `AttachCapabilitiesEvent` providers. There is no blanket receiver whitelist or fake acknowledgement layer. A busy guard prevents callback tick reentry. Capability resolution and `canReceive` callbacks must leave the original source state/storage generation, loaded neighbor identity and valid LazyOptional unchanged. Stale or throwing preflight endpoints are skipped before payment.

Immediately before the single real `receiveEnergy` callback, the source **reserves the offered FE** and marks its loaded chunk unsaved without invoking comparator callbacks. A callback saving the source observes the reserved balance. A truthful returned acceptance is bounded to the original offer; its unaccepted portion is restored only to the same attached original storage generation. Facing/redstone changes during the actual receive call retain that same energy owner and settle normally. Removal, replacement or an explicit NBT reload owns a different balance; an old reservation never refunds into it. Rotation/removal invalidate old native LazyOptionals; toggling enabled keeps the original facing capability available.

**Opaque receiver limit:** Forge FE has no universal two-party commit/rollback API. A receiver which accepts energy and lies about its return value cannot be detected from its source. A receiver which throws before/after accepting leaves acceptance unknowable; the port conservatively retains the offered debit, so a broken endpoint can lose up to the offered 20 FE, but does not receive a source-generated replay/refund. A receiver or external callback which deliberately replaces saved balances remains responsible for its own new storage. No atomic rollback, hostile-addon correctness, cross-mod transactional recovery or full integration with every FE mod is claimed. Negative/oversized saved energy is normalized to 0–1000 and malformed acknowledgements to 0–offered as explicit modern hardening.

## Verification scope

Prepared tests: **28 common + 4 physical ownership = 32**. They check original defaults/materials/all six exact shapes; placement precedence and legacy fallback; idle/partial/fractional/zero conversion; native real pushes with no simulation; all output sides and original extraction quirk; actual redstone; persistence/cap invalidation; reentrant save/payment; preflight replacement/rotation/invalidation; receive-time reload/replacement/refund; malformed/throwing endpoints; captured/uncommitted/detached behavior; loaded-only boundaries; idempotent migration with foreign data; actual world tick cadence; survival/creative physical placement/cancellation and native teardown. These are prepared checks, **not an unexecuted pass claim**. Root records executed native server, hidden client, final build and exact source bindings in `VALIDATION.md` and the 0.37 artifact report.

Raw initial aura/buffer/NBT, ordinary test-only chest FE implementations and isolated terrain are explicit fixtures. No test-only receiver block/type is registered in gameplay. Arenas are separated by 128 blocks at an isolated coordinate band; cleanup owns its terrain, source position and original aura amounts. The actual-world test uses a native region ticket, waits for actual entity/block ticking, observes 40 world ticks, and owns idempotent ticket/terrain cleanup including its bounded timeout. Synchronous manual tick trials establish behavior rather than a gameplay cadence claim. Ordinary teardown intercepts the native ItemEntity emission to retire only the owned QA drop; the separate client driver photographs actual paid survival placement and physical dismantling. This is not full survival, an independent multiplayer/reload session, audio QA or an exhaustive arbitrary-addon test.

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
