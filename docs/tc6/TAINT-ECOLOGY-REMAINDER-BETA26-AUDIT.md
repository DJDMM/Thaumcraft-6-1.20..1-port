# Remaining taint ecology — released TC6 BETA26 contract

This implementation continues the existing seed/Prime/tiny-tentacle ecology.
Authority: released Thaumcraft6.1.BETA26 JAR SHA256
`9425f8643581b27ff8845b087c8bc6fc10425a32942f1a3f0e265ce6b38f7b5f`
and the pinned source checkout954022bb777b7546281fb36df8522f0ba6b43f81.
This document records source implementation and required validation, not a native
test/client pass. Root records actual execution and reviewed images separately.

## Fluid, bottle and slime

Flux Goo is finite: LEVEL0..7 means one through eight quanta, with initial LEVEL7.
Original conversion precedes evaporation/flow: an exposed meta2..5 has1/50 chance
to become a size1 slime, meta6+ a size2 slime. One in four ordinary updates removes
one quantum and adds1Flux. A last quantum has an equal choice between Flux+air or
taint fibre. Evaporation is not an infinite-source operation.

Vertical merging and four-neighbor integer balancing adapt the original Forge
[BlockFluidFinite automaton](https://github.com/MinecraftForge/MinecraftForge/blob/1.12.x/src/main/java/net/minecraftforge/fluids/BlockFluidFinite.java).
The algorithm conserves quanta, retains original remainder allocation, density
ordering and lower-neighbor selection. Production requires loaded chunks and
protects block entities; those are modern integration guards. Block-owned ticks
prevent modern FlowingFluid from independently creating sources. Modern bucket
pickup requires eight quanta: the old universal bucket's arbitrary fractional
fluid quantities are not represented by an ordinary vanilla bucket. The bucket is
a compatibility item, not an original TC6 catalogue entry.

Goo retains original placement/neighbor event updates after30ticks and ordinary
random ticks; an evaporation metadata decrement does not enqueue a periodic
stable update. Native0.42 server1 exposed a modern lifecycle mismatch:1.20.1
`LevelChunk.setBlockState` invokes `onPlace` for a same-block LEVEL change, while
the original1.12 block-added callback only ran after block identity changed.
`FluxGooBlock.onPlace` therefore checks the old block before scheduling. The
original Forge callback also becomes a block-owned `neighborChanged` override;
shape-only flag2 writes must not wake adjacent stable Goo. An explicit block
`isRandomlyTicking` override preserves the old random dispatch because modern
`LiquidBlock` otherwise reads the inert fluid's false flag. Modern
`Level.removeBlock` restores the fluid's legacy block, so Goo exhaustion,
conversion and complete downward transfers explicitly write air instead.
The native regressions separately require one-quantum debit, no local/adjacent
evaporation-created tick, exact30tick placement/direct-neighbor/block-change
schedules, all eight random-tick states, full-transfer conservation, last-quantum
air and accepted size1/2conversion removal. These repairs still require root's
fresh native execution; they are not a recorded pass.

Non-slime collision multiplies horizontal movement by1−quanta/8 and adds
uncurable600tick Vis Exhaust at meta/3. A slime with size<meta can gain one size
and debit original Goo metadata, including the original two-quantum meta1 removal.

Bottle of Taint retains stack8, speed.66/pitch−5/inaccuracy1, a five-block cube
for100tick Flux Taint (ITaintedMob/undead excluded), and ten triangular horizontal
offset attempts with the original coin flip/one-block lower fallback. An accepted
server impact commits once. Throw denial does not consume an item; that ownership
guard is a modern adaptation.

Thaumic Slime uses synchronized integer sizes, size² health, speed.2+.1size,
size+1 damage and size+2XP. Spitting occurs after100 nearby-player ticks at
distance>4 and size>2; a successful spawn consumes one size and resets health as
the original setter does. A dying large slime splits into exactly size unit
slimes, not vanilla's2..4 half-size children. Natural spawn remains disabled.
Original launched counter is decremented client-side only; server contact range
retains that released quirk. Size saves as size−1.

## Monsters

| Entity | Original runtime |
| --- | --- |
| Crawler | HP8, speed.275, damage2, forty-tick terrain carrying including stationary attempts, original difficulty infection roll,1/8Flux crystal. |
| Swarm | HP30, damage2, flying pursuit, fifteen..twentyfour attack cooldown,100tick Weakness, restores target movement,1/2Flux crystal. Summoned swarms lose5HP without a target. |
| Taintacle | HP50, damage7, stationary horizontal movement, one starvation damage every20ticks off taint, tiny summons every40ticks for visible targets with16<distance²<256. |
| Giant | HP175/damage9 base, forced original Champion,35incoming damage cap, first200tick enrage effects, heal1/30ticks, boss bar, final giant within48 drops a Pearl. Natural spawning remains false. |

Counterintuitive released inheritance is preserved: Giant Taintacle extends
ordinary Taintacle, not EntityThaumcraftBoss. Its forced Champion therefore adds
100maxHP and multiplies damage by3, before modifier-specific effects. It receives
ordinary Champion naming/rewards as well as its boss bar and pearl. No TC4 Outer
Lands generation/access chain has been invented.

## Champion attributes and callbacks

The real synchronized, NBT-persisted Champion attribute starts at−2. Original
normal modifier adds1 (−1); one of the fourteen original UUID modifiers adds
id+2. Arbitrary persistent NBT cannot grant a Champion or taint immunity.
Existing Champion balances are not overwritten or repeatedly buffed on reload.

All fourteen source effects are present: Bold speed, Spined retaliation, Armored
19/25damage, Mighty damage, Grim Wither, Warded absorption/regeneration, Warp,
Undying healing, Fiery ignition, Sickly Hunger, Venomous Poison, Vampiric healing,
Infested Crawler and Tainted passive-AI conversion. Player-hit reward rules,
nonfake-player requirement and tainted/boss exclusions follow the original.
The original whitelist/default chance and difficulty/biome adjustments apply.
The released dangerous-location helper always returns false; no bonus was added.

Tainted passive mobs acquire actual melee/target goals and the original permanent
AI marker. Selectors are obtained through stable Forge SRG reflection rather than
expanding the existing access transformer. On reload the marker is reset so a
newly constructed animal's default goals are replaced again.

## Presentation and evidence boundaries

The original Goo atlas animation, thirty following Swarm particles, slime/bottle
quarter-texture fragments, tentacle emergence, enrage particles and Champion
programs use the shared native FX engine. Tainted overlay uses the original
taint_fibres texture and entity-ID-based UV transform. The vanilla1.12.2 slimeball
compatibility texture is copied byte-for-byte from the same pinned original
Minecraft client used by the Crucible water; it is separate from TC6-media counts.

Twelve dedicated native GameTests cover conservation, collision/uncurability,
bucket debit, size persistence, actual Champion attributes, Giant caps and
bottle cube/exclusions, original evaporation scheduling and summoned Swarm
gravity/cadence/Weakness/kill-credit/no-knockback. They are definitions until root executes the complete
native suite. No screenshot, multiplayer, audio or natural acquisition pass is
claimed here; actual pixel/visual review remains required.
