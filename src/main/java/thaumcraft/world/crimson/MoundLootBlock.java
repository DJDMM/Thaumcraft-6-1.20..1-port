package thaumcraft.world.crimson;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.*;
import thaumcraft.equipment.items.LootBagItem;

/** Original fragile loot crates/urns: Silk Touch keeps the block, otherwise 1+tier+[0,2] rolls. */
public final class MoundLootBlock extends Block {
    private final boolean urn;
    private final int tier;
    public MoundLootBlock(String id) {
        super(BlockBehaviour.Properties.of().mapColor(id.contains("_urn_") ? net.minecraft.world.level.material.MapColor.STONE : net.minecraft.world.level.material.MapColor.WOOD).strength(.15F,0).noOcclusion()
                .sound(id.contains("_urn_") ? new net.minecraftforge.common.util.ForgeSoundType(.5F,1.5F,
                        MoundLootBlock::urnSound,MoundLootBlock::urnSound,MoundLootBlock::urnSound,MoundLootBlock::urnSound,MoundLootBlock::urnSound) : SoundType.WOOD));
        urn = id.contains("_urn_"); tier = id.endsWith("_rare") ? 2 : id.endsWith("_uncommon") ? 1 : 0;
    }
    public static boolean handles(String id) { return id.startsWith("loot_crate_") || id.startsWith("loot_urn_"); }
    private static net.minecraft.sounds.SoundEvent urnSound(){return net.minecraftforge.registries.ForgeRegistries.SOUND_EVENTS.getValue(CrimsonModule.id("urnbreak"));}
    public int lootTier() { return tier; }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return urn ? box(2,1,2,14,13,14) : box(1,0,1,15,14,15);
    }
    @Override public List<ItemStack> getDrops(BlockState state, LootParams.Builder context) {
        ItemStack tool = context.getOptionalParameter(LootContextParams.TOOL);
        if (tool != null && EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH,tool) > 0) return List.of(new ItemStack(this));
        RandomSource random = context.getLevel().random;
        var result = new ArrayList<ItemStack>(); int count = 1 + tier + random.nextInt(3);
        Float explosion = context.getOptionalParameter(LootContextParams.EXPLOSION_RADIUS);
        for (int i = 0; i < count; i++) {
            ItemStack stack = LootBagItem.generateLoot(tier, random);
            if (!stack.isEmpty() && (explosion == null || random.nextFloat() <= 1F / explosion)) result.add(stack.copy());
        }
        return result;
    }
}
