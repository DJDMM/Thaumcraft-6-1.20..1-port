package thaumcraft.world.mobs;
import java.util.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraftforge.registries.ForgeRegistries;
import thaumcraft.catalog.CatalogModule;
import thaumcraft.catalog.blocks.CatalogBlocks;
import thaumcraft.alchemy.AspectCrystalItem;
import thaumcraft.api.aspects.Aspect;

/** Original ordered weighted lists, including duplicate entries and the three original forager-list insertion quirks. */
public final class PechTrades{
    private record Entry(int value,ItemStack stack){}
    private PechTrades(){}
    public static List<ItemStack> choices(int type,int value){return entries(type).stream().filter(e->e.value==value).map(e->e.stack.copy()).toList();}
    private static ItemStack tc(String id){return CatalogModule.stack(id);}
    private static ItemStack block(String id){return new ItemStack(CatalogBlocks.block(id));}
    private static void add(List<Entry> rows,int value,ItemStack stack){if(!stack.isEmpty())rows.add(new Entry(value,stack));}
    private static ItemStack book(net.minecraft.world.item.enchantment.Enchantment enchant){return EnchantedBookItem.createForEnchantment(new EnchantmentInstance(enchant,1));}
    private static List<Entry> entries(int type){
        var rows=new ArrayList<Entry>();
        if(type==0){
            for(String metal:List.of("iron","gold","cinnabar","quartz"))add(rows,1,tc("cluster_"+metal));
            for(String metal:List.of("copper","tin","silver","lead"))
                if(!ForgeRegistries.ITEMS.tags().getTag(TagKey.create(Registries.ITEM,ResourceLocation.fromNamespaceAndPath("forge","ingots/"+metal))).isEmpty())add(rows,1,tc("cluster_"+metal));
            add(rows,2,new ItemStack(Items.BLAZE_ROD));add(rows,2,new ItemStack(ForgeRegistries.ITEMS.getValue(MobsModule.id("sapling_greatwood"))));add(rows,2,new ItemStack(Items.DRAGON_BREATH));
            add(rows,3,new ItemStack(Items.EXPERIENCE_BOTTLE));add(rows,3,new ItemStack(Items.EXPERIENCE_BOTTLE));add(rows,3,new ItemStack(Items.GOLDEN_APPLE));
            for(String tool:List.of("pick","axe","hoe"))add(rows,4,tc("thaumium_"+tool));
            add(rows,5,new ItemStack(Items.ENCHANTED_GOLDEN_APPLE));add(rows,5,new ItemStack(ForgeRegistries.ITEMS.getValue(MobsModule.id("sapling_silverwood"))));add(rows,5,tc("curio_knowledge"));
            // Released ConfigEntities appends these to forInv, not forMag/forArc.
            add(rows,5,new ItemStack(Items.TOTEM_OF_UNDYING));add(rows,2,new ItemStack(Items.COMPASS));add(rows,4,new ItemStack(Items.SPECTRAL_ARROW));
        }else if(type==1){
            for(Aspect aspect:List.of(Aspect.AIR,Aspect.EARTH,Aspect.FIRE,Aspect.WATER,Aspect.ORDER,Aspect.ENTROPY))add(rows,1,AspectCrystalItem.create(aspect));
            // BETA26 constructs obsolete potion-damage values without Potion NBT. Preserve empty-potion behavior and distinct forms.
            ItemStack potion1=new ItemStack(Items.POTION);potion1.setDamageValue(8193);ItemStack potion2=new ItemStack(Items.POTION);potion2.setDamageValue(8261);
            add(rows,2,potion1);add(rows,2,potion2);add(rows,2,AspectCrystalItem.create(Aspect.FLUX));
            add(rows,3,new ItemStack(Items.EXPERIENCE_BOTTLE));add(rows,3,new ItemStack(Items.EXPERIENCE_BOTTLE));add(rows,3,AspectCrystalItem.create(Aspect.AURA));add(rows,3,new ItemStack(Items.GOLDEN_APPLE));
            for(String armor:List.of("boots","chest","legs"))add(rows,4,tc("cloth_"+armor));
            add(rows,5,new ItemStack(Items.ENCHANTED_GOLDEN_APPLE));add(rows,5,tc("pech_wand"));add(rows,5,tc("curio_knowledge"));add(rows,5,tc("amulet_vis_found"));
        }else{
            for(String color:List.of("black","red","green","brown","blue","purple","cyan","silver","gray","pink","lime","yellow","lightblue","magenta","orange"))add(rows,1,block("candle_"+color));
            add(rows,2,new ItemStack(Items.GHAST_TEAR));add(rows,2,book(Enchantments.POWER_ARROWS));
            add(rows,3,new ItemStack(Items.EXPERIENCE_BOTTLE));add(rows,3,new ItemStack(Items.EXPERIENCE_BOTTLE));add(rows,3,new ItemStack(Items.GOLDEN_APPLE));
            add(rows,4,tc("eldritch_eye"));add(rows,4,new ItemStack(Items.ENCHANTED_GOLDEN_APPLE));
            add(rows,5,tc("baubles_ring_apprentice"));add(rows,5,book(Enchantments.FLAMING_ARROWS));add(rows,5,book(Enchantments.INFINITY_ARROWS));add(rows,5,tc("curio_knowledge"));
        }return rows;
    }
}
