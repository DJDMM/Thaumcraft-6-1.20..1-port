package thaumcraft.artifice.late;
import java.util.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import thaumcraft.api.aspects.*;
import thaumcraft.scanning.AspectRegistry;
/** Released ConfigAspects#getPotionAspects and AspectHelper#cullTags(...,10). */
public final class PotionRecipeAspects {
    private PotionRecipeAspects(){}
    public static boolean valid(ItemStack s){var potion=PotionUtils.getPotion(s);return !s.isEmpty()&&(s.is(Items.POTION)||s.is(Items.SPLASH_POTION)||s.is(Items.LINGERING_POTION))&&potion!=Potions.EMPTY&&potion!=Potions.WATER&&potion!=Potions.AWKWARD&&potion!=Potions.MUNDANE&&potion!=Potions.THICK;}
    public static AspectList recipe(ItemStack stack){
        if(!valid(stack))return new AspectList();var reagents=new HashSet<ItemStack>();collect(PotionUtils.getPotion(stack),reagents,new HashSet<>());
        var result=new AspectList();for(var reagent:reagents){var tags=AspectRegistry.getAspects(reagent);for(var a:tags.getAspects())result.add(a,tags.getAmount(a));result.add(Aspect.ALCHEMY,3);}
        if(reagents.isEmpty())result.add(Aspect.MAGIC,5).add(Aspect.ALCHEMY,5);
        else for(var a:result.getAspects())result.remove(a,(int)(result.getAmount(a)*.66));
        while(result.size()>10){Aspect lowest=null;float low=Short.MAX_VALUE;for(var a:result.getAspects()){float weight=result.getAmount(a);if(a.isPrimal())weight*=.9F;else for(var component:a.getComponents())if(!component.isPrimal()){weight*=1.1F;for(var sub:component.getComponents())if(!sub.isPrimal())weight*=1.05F;}if(weight<low){low=weight;lowest=a;}}if(lowest==null)break;result.remove(lowest);}
        return result;
    }
    private static void collect(Potion potion,Set<ItemStack> reagents,Set<Potion> seen){
        if(!seen.add(potion))return;
        List<PotionBrewing.Mix<Potion>> mixes=ObfuscationReflectionHelper.getPrivateValue(PotionBrewing.class,null,"f_43494_");
        for(var mix:mixes)if(mix.to.value()==potion){var candidates=mix.ingredient.getItems();if(candidates.length>0){reagents.add(candidates[0]);if(mix.from.value()!=Potions.WATER)collect(mix.from.value(),reagents,seen);}break;}
    }
}
