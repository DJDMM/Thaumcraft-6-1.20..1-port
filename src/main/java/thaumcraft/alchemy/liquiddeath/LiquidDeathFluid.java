package thaumcraft.alchemy.liquiddeath;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.fluids.ForgeFlowingFluid;
/** Four original quanta map to modern even amounts, preserving the original .8 full height. */
public final class LiquidDeathFluid {
 private LiquidDeathFluid(){}
 public static final class Source extends ForgeFlowingFluid.Source {
  public Source(Properties properties){super(properties);}
  @Override public float getOwnHeight(FluidState state){return .8F;}
  @Override public BlockState createLegacyBlock(FluidState state){return LiquidDeathModule.block().defaultBlockState();}
  @Override public void tick(Level level,BlockPos pos,FluidState state){}
 }
 public static final class Flowing extends ForgeFlowingFluid.Flowing {
  public Flowing(Properties properties){super(properties);}
  @Override public float getOwnHeight(FluidState state){return getAmount(state)/10F;}
  @Override public BlockState createLegacyBlock(FluidState state){return LiquidDeathModule.block().defaultBlockState().setValue(LiquidDeathBlock.LEVEL,Math.max(1,4-getAmount(state)/2));}
  @Override public void tick(Level level,BlockPos pos,FluidState state){}
 }
}
