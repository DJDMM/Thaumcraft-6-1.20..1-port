package thaumcraft.artifice.redstone;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Independent input threshold and output strength; source byte keys remain readable. */
public final class RedstoneRelayBlockEntity extends BlockEntity {
    private int in = 1;
    private int out = 15;
    public RedstoneRelayBlockEntity(BlockPos pos, BlockState state) { super(ArtificeRedstoneModule.REDSTONE_RELAY.get(), pos, state); }
    public int getIn() { return in; }
    public int getOut() { return out; }
    public void setIn(int value) { in = value; }
    public void setOut(int value) { out = value; }
    public void increaseIn() {
        if (level != null && !level.isClientSide) { in++; if (in > 15) in = 1; changed(); }
    }
    public void increaseOut() {
        if (level != null && !level.isClientSide) { out++; if (out > 15) out = 1; changed(); }
    }
    private void changed() {
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag); tag.putByte("in", (byte) in); tag.putByte("out", (byte) out);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag); in = tag.getByte("in"); out = tag.getByte("out");
    }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
