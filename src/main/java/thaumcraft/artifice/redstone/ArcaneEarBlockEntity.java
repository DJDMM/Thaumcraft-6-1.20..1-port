package thaumcraft.artifice.redstone;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class ArcaneEarBlockEntity extends BlockEntity {
    public byte note;
    public byte tone;
    public int redstoneSignal;
    private boolean fresh = true;
    public ArcaneEarBlockEntity(BlockPos pos, BlockState state) { super(ArtificeRedstoneModule.ARCANE_EAR.get(), pos, state); }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag); tag.putByte("note", note); tag.putByte("tone", tone);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag); note = (byte) Math.max(0, Math.min(24, tag.getByte("note"))); tone = tag.getByte("tone");
        // Original redstoneSignal is transient; the enabled state is persisted in the chunk's block state.
        redstoneSignal = 0;
        fresh = false;
    }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }
    public void changePitch() { note = (byte) ((note + 1) % 25); changed(); }
    public void updateTone() {
        if (level == null || !(getBlockState().getBlock() instanceof ArcaneEarBlock)) return;
        BlockPos support = worldPosition.relative(getBlockState().getValue(ArcaneEarBlock.FACING).getOpposite());
        if (ArtificeRedstoneModule.loaded(level, support)) { tone = instrument(level.getBlockState(support)); fresh = false; changed(); }
    }
    /** Old material classes no longer exist. Preserve their ten instruments, including pre-1.14 fallback materials. */
    public static byte instrument(BlockState support) {
        return (byte) switch (support.instrument()) {
            case BASEDRUM, BIT -> 1;
            case SNARE, COW_BELL -> 2;
            case HAT, PLING -> 3;
            case BASS -> 4;
            case FLUTE -> 5;
            case BELL -> 6;
            case GUITAR -> 7;
            case CHIME -> 8;
            case XYLOPHONE -> 9;
            default -> 0;
        };
    }
    public void triggerNote(boolean sound) {
        if (level == null || !(getBlockState().getBlock() instanceof ArcaneEarBlock)) return;
        BlockPos support = worldPosition.relative(getBlockState().getValue(ArcaneEarBlock.FACING).getOpposite());
        if (!ArtificeRedstoneModule.loaded(level, support)) return;
        int instrument = sound ? instrument(level.getBlockState(support)) : -1;
        level.blockEvent(worldPosition, getBlockState().getBlock(), instrument, note);
    }
    public static void serverTick(Level world, BlockPos pos, BlockState state, ArcaneEarBlockEntity ear) {
        if (!(world instanceof ServerLevel level) || ear.isRemoved() || !ArtificeRedstoneModule.loaded(level, pos)
                || !(level.getBlockState(pos).getBlock() instanceof ArcaneEarBlock block)) return;
        if (ear.fresh) ear.updateTone();
        if (ear.redstoneSignal > 0 && --ear.redstoneSignal == 0) block.setSignal(level, pos, false);
        for (var played : ArcaneEarEvents.notes(level)) {
            if (played.instrument() == ear.tone && played.note() == ear.note && pos.distSqr(played.pos()) <= 4096) {
                ear.triggerNote(true);
                if (block.isToggle()) block.setSignal(level, pos, !level.getBlockState(pos).getValue(ArcaneEarBlock.ENABLED));
                else { ear.redstoneSignal = 10; block.setSignal(level, pos, true); }
                break;
            }
        }
    }
}
