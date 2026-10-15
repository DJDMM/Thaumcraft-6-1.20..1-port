package thaumcraft.artifice.pattern;

import java.util.*;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;

/** Released ten arrangements; one exemplar occupies every selected cell. */
public final class PatternRecipes {
    private static final int[][] CELLS={{0,1,2,3,4,5,6,7,8},{0},{0,1},{0,3},{0,1,3,4},{0,1,2},{0,3,6},{0,1,2,3,4,5},{0,1,3,4,6,7},{0,1,2,3,5,6,7,8}};
    private PatternRecipes(){}
    public static int amount(int type){return type>=0&&type<CELLS.length?CELLS[type].length:9;}
    public static CraftingContainer matrix(int type,ItemStack exemplar){var grid=new TransientCraftingContainer(new AbstractContainerMenu(null,-1){@Override public boolean stillValid(Player player){return false;}@Override public ItemStack quickMoveStack(Player player,int slot){return ItemStack.EMPTY;}},3,3);if(type>=0&&type<CELLS.length)for(int cell:CELLS[type])grid.setItem(cell,exemplar.copyWithCount(1));return grid;}
    public record Result(ItemStack output,List<ItemStack> remainders){public Result{output=output.copy();remainders=remainders.stream().map(ItemStack::copy).toList();}public List<ItemStack> allOutputs(){var all=new ArrayList<ItemStack>();all.add(output.copy());for(ItemStack remainder:remainders)if(!remainder.isEmpty())all.add(remainder.copy());return all;}}
    public static Optional<Result> craft(ServerLevel level,int type,ItemStack exemplar){CraftingContainer grid=matrix(type,exemplar);var match=level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,grid,level);if(match.isEmpty())return Optional.empty();ItemStack output=match.get().assemble(grid,level.registryAccess());NonNullList<ItemStack> remaining=level.getRecipeManager().getRemainingItemsFor(RecipeType.CRAFTING,grid,level);var kept=new ArrayList<ItemStack>();for(int i=0;i<9;i++)if(!grid.getItem(i).isEmpty()&&i<remaining.size()&&!remaining.get(i).isEmpty())kept.add(remaining.get(i).copy());return output.isEmpty()?Optional.empty():Optional.of(new Result(output,kept));}
}
