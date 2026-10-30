package thaumcraft.artifice.late;
import com.google.gson.*;
import java.util.Set;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import thaumcraft.catalog.CatalogModule;
/** Original ModConfig chooses the first actually registered ingot; no fictitious addon item ID. */
public final class OptionalClusterSmeltingRecipe extends SmeltingRecipe {
    private static final Set<String> METALS=Set.of("copper","tin","silver","lead");
    private final String metal;private final TagKey<Item> ingots;
    public OptionalClusterSmeltingRecipe(ResourceLocation id,String metal){
        super(id,"",CookingBookCategory.MISC,Ingredient.of(CatalogModule.stack("cluster_"+checked(metal))),ItemStack.EMPTY,1,200);
        this.metal=metal;ingots=TagKey.create(Registries.ITEM,ResourceLocation.fromNamespaceAndPath("forge","ingots/"+metal));
    }
    private static String checked(String metal){if(!METALS.contains(metal))throw new JsonSyntaxException("Unknown optional TC6 cluster metal: "+metal);return metal;}
    private ItemStack output(){return ForgeRegistries.ITEMS.tags().getTag(ingots).stream().findFirst().map(item->new ItemStack(item,2)).orElse(ItemStack.EMPTY);}
    @Override public boolean matches(Container c,Level l){return !output().isEmpty()&&super.matches(c,l);}
    @Override public ItemStack assemble(Container c,RegistryAccess registries){return output();}
    @Override public ItemStack getResultItem(RegistryAccess registries){return output();}
    @Override public RecipeSerializer<?> getSerializer(){return LateDevicesModule.CLUSTER_SMELTING.get();}
    public static final class Serializer implements RecipeSerializer<OptionalClusterSmeltingRecipe>{
        @Override public OptionalClusterSmeltingRecipe fromJson(ResourceLocation id,JsonObject json){return new OptionalClusterSmeltingRecipe(id,GsonHelper.getAsString(json,"metal"));}
        @Override public OptionalClusterSmeltingRecipe fromNetwork(ResourceLocation id,FriendlyByteBuf b){return new OptionalClusterSmeltingRecipe(id,b.readUtf(16));}
        @Override public void toNetwork(FriendlyByteBuf b,OptionalClusterSmeltingRecipe recipe){b.writeUtf(recipe.metal,16);}
    }
}
