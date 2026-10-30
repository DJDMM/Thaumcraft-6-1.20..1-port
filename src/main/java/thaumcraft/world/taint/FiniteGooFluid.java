package thaumcraft.world.taint;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.fluids.ForgeFlowingFluid;
/** Fluid metadata for the finite block automaton; FlowingFluid never duplicates its contents. */
public final class FiniteGooFluid {
 private FiniteGooFluid(){}
 public static final class Source extends ForgeFlowingFluid.Source {
  public Source(Properties properties){super(properties);}
  @Override public BlockState createLegacyBlock(FluidState state){return TaintModule.block("flux_goo").defaultBlockState().setValue(FluxGooBlock.LEVEL,7);}
  @Override public void tick(Level level,BlockPos pos,FluidState state){}
 }
 public static final class Flowing extends ForgeFlowingFluid.Flowing {
  public Flowing(Properties properties){super(properties);}
  @Override public BlockState createLegacyBlock(FluidState state){return TaintModule.block("flux_goo").defaultBlockState().setValue(FluxGooBlock.LEVEL,Math.max(0,getAmount(state)-1));}
  @Override public void tick(Level level,BlockPos pos,FluidState state){}
 }
}
