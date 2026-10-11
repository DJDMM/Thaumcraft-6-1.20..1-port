package thaumcraft.world.taint;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/** Terrain decay shared with cleansing and the original taint ecology. */
public interface ITaintBlock {
    void die(ServerLevel level, BlockPos pos, BlockState state);
}
