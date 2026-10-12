package thaumcraft.artifice.furnace;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootParams;

/** Invisible solid shell; breaking it drops its paid original material and dismantles the furnace. */
public final class InfernalFurnacePlaceholderBlock extends Block {
    private final String id;
    public InfernalFurnacePlaceholderBlock(String id){super(Properties.of().strength(2.5f,1.2f).sound(SoundType.STONE).noOcclusion().pushReaction(PushReaction.BLOCK));this.id=id;}
    public static Block original(String id){return id.equals("placeholder_brick")?Blocks.NETHER_BRICKS:Blocks.OBSIDIAN;}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.INVISIBLE;}
    @Override public List<ItemStack> getDrops(BlockState state,LootParams.Builder context){return List.of(new ItemStack(original(id)));}
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState replacement,boolean moving){if(!state.is(replacement.getBlock())&&!level.isClientSide&&!level.restoringBlockSnapshots){BlockPos anchor=InfernalFurnaceFormation.findAnchor(level,pos);if(anchor!=null)InfernalFurnaceFormation.destroy(level,anchor,level.getBlockState(anchor),pos);}super.onRemove(state,level,pos,replacement,moving);}
}
