package thaumcraft.alchemy;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import thaumcraft.catalog.CatalogModule;

/** BETA26's real, ungated recipe: sugar and three distinct chunks metadata. */
public final class TripleMeatTreatRecipe extends CustomRecipe {
    private static final Set<String> CHUNKS = Set.of("chunk_beef", "chunk_chicken", "chunk_pork", "chunk_fish", "chunk_rabbit", "chunk_mutton");
    public TripleMeatTreatRecipe(ResourceLocation id, CraftingBookCategory category) { super(id, category); }
    @Override public boolean matches(CraftingContainer input, Level level) {
        boolean sugar = false;
        Set<String> meats = new HashSet<>();
        for (int slot = 0; slot < input.getContainerSize(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) continue;
            if (stack.is(Items.SUGAR)) {
                if (sugar) return false;
                sugar = true;
            } else {
                ResourceLocation id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
                if (id == null || !id.getNamespace().equals("thaumcraft") || !CHUNKS.contains(id.getPath())
                        || !meats.add(id.getPath()) || meats.size() > 3) return false;
            }
        }
        return sugar && meats.size() == 3;
    }
    @Override public ItemStack assemble(CraftingContainer input, RegistryAccess access) { return CatalogModule.stack("triple_meat_treat"); }
    @Override public ItemStack getResultItem(RegistryAccess access) { return CatalogModule.stack("triple_meat_treat"); }
    @Override public boolean canCraftInDimensions(int width, int height) { return width * height >= 4; }
    @Override public RecipeSerializer<?> getSerializer() { return AlchemyModule.TRIPLE_MEAT_RECIPE.get(); }
}
