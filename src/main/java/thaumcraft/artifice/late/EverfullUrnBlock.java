package thaumcraft.artifice.late;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
public final class EverfullUrnBlock extends BaseEntityBlock {
    public EverfullUrnBlock(){super(Properties.copy(Blocks.STONE).strength(3).noOcclusion());}
    @Override public RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
    @Override public VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return Block.box(3,0,3,13,16,13);}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new EverfullUrnBlockEntity(p,s);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> type){return createTickerHelper(type,LateDevicesModule.URN.get(),EverfullUrnBlockEntity::tick);}
    @Override public InteractionResult use(BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){
        if(!l.isClientSide&&l.getBlockEntity(p) instanceof EverfullUrnBlockEntity tile){
            boolean success=FluidUtil.interactWithFluidHandler(player,hand,tile.tank);var held=player.getItemInHand(hand);
            if(!success&&held.is(Items.GLASS_BOTTLE)&&tile.tank.getFluidAmount()>=333){var output=PotionUtils.setPotion(new ItemStack(Items.POTION),Potions.WATER);if(!player.getAbilities().instabuild)held.shrink(1);if(held.isEmpty())player.setItemInHand(hand,output);else if(!player.getInventory().add(output))player.drop(output,false);tile.tank.drain(333,IFluidHandler.FluidAction.EXECUTE);success=true;}
            if(success){tile.changed();player.inventoryMenu.broadcastChanges();l.playSound(null,p,SoundEvents.BOTTLE_FILL,SoundSource.BLOCKS,.33F,1+(l.random.nextFloat()-l.random.nextFloat())*.3F);}
        }return InteractionResult.sidedSuccess(l.isClientSide);
    }
    @Override public void animateTick(BlockState s,Level l,BlockPos p,RandomSource r){if(l.getBlockEntity(p) instanceof EverfullUrnBlockEntity t&&t.tank.getFluidAmount()>=1000)net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,()->()->thaumcraft.artifice.late.client.LateDeviceEffects.urnSplash(l,p));}
}
