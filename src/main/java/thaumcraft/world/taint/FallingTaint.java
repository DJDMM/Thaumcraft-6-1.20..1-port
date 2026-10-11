package thaumcraft.world.taint;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Original falling-taint entity initialization; visual catalogue entities cannot satisfy it. */
public interface FallingTaint {
    void initializeFallingTaint(BlockState state, BlockPos originalPos);
}
