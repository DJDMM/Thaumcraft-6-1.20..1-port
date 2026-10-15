# Dioptra: released TC6 BETA26 contract

Implementation and primary-source audit for0.36.0, not a passed execution claim.
Actual native logs, hashes, prepared fixtures and limitations belong in the
finished VALIDATION/artifact report only after execution and visual review.

Primary release JAR SHA256:
`9425f8643581b27ff8845b087c8bc6fc10425a32942f1a3f0e265ce6b38f7b5f`.
Readable companion source is pinned to `954022bb777b7546281fb36df8522f0ba6b43f81`.
Both released device classes were disassembled with Java17 javap; their parent
classes, aura-cache lifecycle and the original renderer were read independently.

| Released class | SHA256 |
| --- | --- |
| `common/tiles/devices/TileDioptra` | `1d367b3262655191c6635db14baaceef7e6088f850054c38f924548c9b950654` |
| `common/blocks/devices/BlockDioptra` | `ff2954a4eda1a32ca474a587695fe7e8ee44cb5d7b18a1d99762d10a161a9f3a` |
| `common/blocks/BlockTCDevice` | `810b34318db98129a228ef58d2b002d2a81345834f3f4c0c2d8af053b453341b` |
| `common/blocks/BlockTCTile` | `afbce7401569205fa5130432790e785f7e7e99b5fd8e013e111146f201d0caeb` |
| `common/tiles/TileThaumcraft` | `a82d3fd869f1711f499cf6e9f27fea70732060d80fff52e22ad2a2af518ca514` |
| `common/lib/events/ChunkEvents` | `3db54f08f17d87b6c6297232b290a1b9dd9a52f7e316d1ce28ac9bc4bd194a18` |
| `client/renderers/tile/TileDioptraRenderer` | `df0536b3166f83fca0d2919c6a632782561d92aef08ea472245eb8a67a288ea4` |

## Default state and geometry

BlockTCDevice discards the return of withProperty(ENABLED,true). This does NOT
make the original default false: legacy PropertyBool enumerates TRUE before
FALSE, and BlockStateContainer preserves that collection order and chooses the
first generated state. The official Mojang1.12.2 client JAR SHA256 is
`8ada07da5ee77dad3527bd7278fbd05ee1fc8a597813b216a871a2d7d64cc64f`,
SHA1 `0f275bc1547d01fa5f56ba34bdc87d981ee12daf`. Pinned MCP mappings resolve
PropertyBool=axf and BlockStateContainer=awu. axf constructor offsets8/12/16
build ImmutableSet.of(TRUE,FALSE); awu's private e() takes getAllowedValues
unchanged and its b() returns the first state. Modern BooleanProperty has a
different default ordering; explicit ENABLED=true preserves the release.
The inherited placement method also explicitly sets true.

The block retains original rock material/stone sound, hardness2, effective
modern explosion resistance12 (old setResistance20 multiplication/division),
full-cell collision/selection, non-opaque rendering and unrestricted ordinary
harvest. Native pickaxe speed is supplied by the modern mineable tag; no new
correct-tool payment gate is imposed. Plain drops do not copy the display grid.
The rendered bounds are exactly block(-.3,-.3,-.3)..(1.3,2.3,1.3).

## Tick and aura sampling

The server increments its transient counter every actual update, sampling on
counter%20==0. The first new sample is therefore tick20. On the client the
original update increments then resets counter to0; its displayed mesh pulse
retains that original behavior. Sampling clears all169 bytes first, iterates
xx=0..12 outside zz=0..12, and stores at xx+zz*13. Chunk coordinates are
(blockX>>4)+xx-6 and (blockZ>>4)+zz-6, including negative-coordinate floor shift.
No radius-in-blocks substitute, sphere, interpolation or additional sensor fuel
is introduced.

ENABLED=true reads Vis; false reads Flux. Each byte is the Java truncation of
min(64F, amount/500F*64F). The center is index84. Sampling neither drains nor
adds aura, creates rifts or advances aura simulation itself. Original chunk-save
logic removes unloaded chunks from AuraHandler's active cache. The modern aura
store persists unloaded records, so this display explicitly requires
getChunkNow before reading AuraManager: old persisted records outside loaded
chunks remain zero. Existing loaded chunks may initialize their normal aura
record through the shared lazy API; missing chunks never load or gain records
because the display looked at them.

Each sample calls native setChanged and sends the block-entity update with
flag2, retaining normal comparator updates. No output inventory, fluid, energy
or essence capability is added.

## Interaction and comparator

Every ordinary accepted block right-click flips the existing boolean mode;
no GUI, wrench, crouch requirement, vis fee or manufactured discovery is added.
Modern server authority respects native permission/capture boundaries and does
not let a spectator change the device. A click keeps the current169 samples and
counter; the next scheduled sample measures the new quantity. Original
BlockDioptra.updateState is an empty override, so nearby redstone never changes
mode and does not pause sampling.

The comparator reads center/64F, then floor(ratio*14F)+(ratio>0?1:0). Zero is0,
the smallest positive byte is1, and64 produces15. It does not inspect another
chunk, the other aura quantity or the world's raw amount directly.

## Persistence, compatibility and client

Only the original grid_a byte array is tile data; the mode lives in blockstate.
The unsaved clock starts0 on a genuine load and is not reset by update packets.
The modern reader bounds malformed arrays to169 values in0..64 so crafted old
NBT cannot crash the native mesh or emit a comparator signal beyond15. Exports
copy the array. This is a display-data validation adaptation, not extra gameplay.

LegacyDioptraMigration uses the existing bounded64/200 loaded-chunk queue and
palette scan, because the preceding catalogue Dioptra had no block entity. It
repairs absent or exact storage-free Catalogue owners, preserves an existing
native clock/grid or a foreign owner, avoids captured/restoring transactions
and refreshes tracking clients with the native chunk packet. No chunk ticket is
created by production code.

The client audit records the original144 surface quads,48 edge panels and four
side panels, original additive layer/depth/light/color behavior and two exact
PNG imports. No generated graphics, invented GUI or developer overlay is used.
Research and paid arcane crafting are specified in
[the progression audit](PATTERN-DIOPTRA-PROGRESSION-BETA26-AUDIT.md).
Prepared native tests are definitions until the root executes the whole suite;
screenshots require actual hidden-client execution and two real visual reviews.

## Фактически завершённый QA0.36

`pattern-036-server4` прошёл1488/1488; `pattern-036-client1`23/23, оба nativeexit0.
Два проверяющих просмотрели все23полных1600×1000PNG и оба листа.
Настоящие3scan/2study/2bench/100vis5crystals/96Theory16Observation20XP,
nativeDioptraVisFlux,10dies/10crafts/24outputs, 80тиков паузы,
продолжение и физическая разборка связаны с финальными logs/SHA.
Фикстуры, source quirks, современное loaded-only/владение, ранние попытки
и пределы доказательства сохранены в [VALIDATION.md](../../VALIDATION.md)
и [artifact-report0.36](../../validation/artifact-report-0.36.json).
