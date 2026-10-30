# Liquid Death — TC6 BETA26 contract

Authority: released Thaumcraft6.1.BETA26 JAR and pinned source
`common/blocks/misc/BlockFluidDeath.java`,
`ConfigBlocks.FluidDeath`, `ConfigRecipes`,
`EntityEvents.livingDrops` and `FXSlimyBubble`.
Source implementation is not a native execution/client-visual pass.

The source fluid uses four quanta. Metadata0..3 means4..1quanta. Its full
render height is4/5=.8, not modern vanilla's8/9. Downward classic flow is
metadata1 (three quanta), not a newly minted source or a full falling column.
Viscosity1500 gives a seven-tick update delay in original Forge. The block owns
the original classic automaton, including two-block slope search and decay;
modern FlowingFluid stores/render amounts but does not run a second flow engine.
The automaton adapts
[Forge1.12 BlockFluidClassic](https://github.com/MinecraftForge/MinecraftForge/blob/1.12.x/src/main/java/net/minecraftforge/fluids/BlockFluidClassic.java)
and [BlockFluidBase](https://github.com/MinecraftForge/MinecraftForge/blob/1.12.x/src/main/java/net/minecraftforge/fluids/BlockFluidBase.java).
Chunk presence and block-entity protection are explicit modern guards.
Modern fluid corner interpolation is not claimed bit-identical to the old GPU.

Collision scales horizontal movement by1−quanta/4/2, preserving vertical motion.
Every server collision attempts original dissolve damage5−metadata. This damage
bypasses armor and retains ordinary native hurt invulnerability. It is not poison,
a magic potion, fire or a delayed health setter. Its modern damage type is
`thaumcraft:dissolve` with only the original armor-bypass semantic.

A dissolve death adds uniform random victim-typed crystals independently of
player credit: count is1..(1+totalAspectUnits/10), each one physical crystal at
eye height. The original entity/player aspect pool is copied; drops do not debit
or mutate registered aspect definitions. A player's pool retains the original
name-seeded three aspects plus Human4.

The original crucible recipe consumes one empty bucket and Mortuus100,
Alkimia20, Perditio50, gated by LIQUIDDEATH. The compatibility bucket replaces
Forge1.12's universal fluid bucket and uses original bucket_death media.
The fluid's animatedglow texture and exact ARGB−263978855 tint are preserved.
Client effects are actual FXSlimyBubble atlas frames/age phases, alpha.8,
source colors/scales and the original lava-pop sound rolls.

Four dedicated native GameTests cover collision/source semantics, four-quanta
heights/thin damage, downward metadata and victim-aspect drops. Root must execute
these with the complete suite and inspect actual native frames before any
gameplay/visual pass is claimed. No full survival, multiplayer, every camera
angle or arbitrary shader-pack result is implied by this implementation.
