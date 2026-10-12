package thaumcraft.artifice.furnace;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.SimpleContainer;
import thaumcraft.catalog.CatalogModule;
import thaumcraft.essentia.production.SmelterBellowsBlock;

/** Original timing, experience rounding and ordered ConfigRecipes bonus table. */
public final class InfernalFurnaceRules {
    private InfernalFurnaceRules(){}
    public static Optional<SmeltingRecipe> recipe(Level level,ItemStack in){return in.isEmpty()?Optional.empty():level.getRecipeManager().getRecipeFor(RecipeType.SMELTING,new SimpleContainer(in),level);}
    public static int cookTime(int speedyTime,int bellows){int b=Math.min(4,Math.max(0,bellows));return Math.max(10,(speedyTime>0?80:140)-b*(21-b));}
    public static int bellows(Level level,BlockPos pos){int count=0;for(Direction dir:Direction.values()){if(dir==Direction.UP)continue;BlockPos target=pos.relative(dir,2);if(!level.hasChunkAt(target))continue;var state=level.getBlockState(target);if(state.getBlock() instanceof SmelterBellowsBlock&&level.getBlockEntity(target)!=null&&state.getValue(SmelterBellowsBlock.FACING)==dir.getOpposite()&&!level.hasNeighborSignal(target))count++;}return Math.min(4,count);}
    public static int experience(int count,float experience,double roll){if(experience==0)return 0;if(experience<1){double total=count*experience;int floor=(int)Math.floor(total);return floor+(floor<Math.ceil(total)&&roll<total-floor?1:0);}return count;}
    private record Bonus(java.util.function.Predicate<ItemStack> input,java.util.function.Supplier<ItemStack> output,float chance){}
    private static java.util.function.Predicate<ItemStack> ore(String material){return stack->stack.is(ItemTags.create(new ResourceLocation("forge","ores/"+material)))
            ||material.equals("iron")&&stack.is(Items.RAW_IRON)||material.equals("gold")&&stack.is(Items.RAW_GOLD)||material.equals("copper")&&stack.is(Items.RAW_COPPER);}
    private static java.util.function.Predicate<ItemStack> item(Item item){return stack->stack.is(item);}
    private static java.util.function.Predicate<ItemStack> catalog(String id){return stack->stack.is(CatalogModule.stack(id).getItem());}
    private static java.util.function.Supplier<ItemStack> catalogOut(String id){return ()->CatalogModule.stack(id);}
    private static List<Bonus> table(){List<Bonus> rows=new ArrayList<>();
        String[] ores={"gold","iron","cinnabar","copper","tin","silver","lead","quartz"};
        String[] nuggets={"gold","iron","quicksilver","copper","tin","silver","lead","quartz"};
        for(int i=0;i<ores.length;i++){String n=nuggets[i];rows.add(new Bonus(ore(ores[i]),()->n.equals("gold")?new ItemStack(Items.GOLD_NUGGET):n.equals("iron")?new ItemStack(Items.IRON_NUGGET):CatalogModule.stack("nugget_"+n),.33f));}
        String[] clusters={"iron","gold","cinnabar","copper","tin","silver","lead","quartz"};
        for(String c:clusters){String n=c.equals("cinnabar")?"quicksilver":c;rows.add(new Bonus(catalog("cluster_"+c),()->n.equals("iron")?new ItemStack(Items.IRON_NUGGET):n.equals("gold")?new ItemStack(Items.GOLD_NUGGET):CatalogModule.stack("nugget_"+n),.33f));}
        Item[] meats={Items.BEEF,Items.CHICKEN,Items.PORKCHOP,Items.COD,Items.RABBIT,Items.MUTTON};String[] chunks={"beef","chicken","pork","fish","rabbit","mutton"};
        for(int i=0;i<meats.length;i++)rows.add(new Bonus(i==3?stack->stack.is(Items.COD)||stack.is(Items.SALMON)||stack.is(Items.TROPICAL_FISH)||stack.is(Items.PUFFERFISH):item(meats[i]),catalogOut("chunk_"+chunks[i]),.33f));
        String[] rare={"diamond","redstone","lapis","emerald","gold","iron","cinnabar","copper","tin","silver","lead","quartz"};
        float[] chance={.025f,.01f,.01f,.025f,.02f,.01f,.025f,.01f,.01f,.02f,.01f,.01f};
        for(int i=0;i<rare.length;i++)rows.add(new Bonus(ore(rare[i]),catalogOut("nugget_rareearth"),chance[i]));
        rows.add(new Bonus(stack->Arrays.stream(clusters).anyMatch(c->catalog("cluster_"+c).test(stack)),catalogOut("nugget_rareearth"),.02f));return rows;
    }
    public static List<ItemStack> bonuses(ItemStack input,int bellows,RandomSource random){List<ItemStack> result=new ArrayList<>();List<Bonus> table=table();for(int call=0;call<Math.min(4,Math.max(0,bellows))+1;call++)for(Bonus row:table)if(row.input.test(input)&&random.nextFloat()<=row.chance){ItemStack stack=row.output.get().copy();if(stack.getCount()<1)stack.setCount(1);result.add(stack);}return result;}
}
