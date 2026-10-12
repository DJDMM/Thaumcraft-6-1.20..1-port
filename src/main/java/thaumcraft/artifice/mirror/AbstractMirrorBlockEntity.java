package thaumcraft.artifice.mirror;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import thaumcraft.world.aura.AuraManager;

/** The released link machine, with loaded-only resolution and reciprocal invalidation. */
public abstract class AbstractMirrorBlockEntity extends BlockEntity {
    public enum Status { UNAVAILABLE, INVALID, VALID }
    public enum Kind { ITEM, ESSENTIA }
    public record Resolution(Status status, @Nullable AbstractMirrorBlockEntity peer, @Nullable ServerLevel level) {}
    @Nullable private MirrorLink link;
    private boolean linked;
    private int instability, count, inc = 40;
    private long revision;
    private boolean linking, removing, ticking;
    private boolean creativeRemoval;
    @Nullable private final MirrorPlacementReceipt placement;

    protected AbstractMirrorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); placement=MirrorPlacementReceipt.capture(this); }
    @Nullable public final MirrorLink link() { return link; }
    public final boolean linked() { return linked; }
    public final int instability() { return instability; }
    public final int count() { return count; }
    public final int retryModulus() { return inc; }
    public final long revision() { return revision; }
    public final Direction facing() { return getBlockState().hasProperty(MirrorBlock.FACING) ? getBlockState().getValue(MirrorBlock.FACING) : Direction.DOWN; }
    public Kind kind() { return this instanceof ItemMirrorBlockEntity ? Kind.ITEM : Kind.ESSENTIA; }
    protected int instabilityThreshold() { return kind() == Kind.ITEM ? 128 : 64; }
    public final MirrorLink endpoint() { return new MirrorLink(level.dimension(), worldPosition); }
    public final boolean physicallyPresent() {
        if (!(level instanceof ServerLevel server) || isRemoved()) return false;
        LevelChunk chunk = server.getChunkSource().getChunkNow(worldPosition.getX() >> 4, worldPosition.getZ() >> 4);
        return chunk != null && chunk.getBlockEntity(worldPosition, LevelChunk.EntityCreationType.CHECK) == this;
    }
    public final void setLink(@Nullable MirrorLink value) { requireThread(); link = value; linked = false; changedLink(); }
    private void changedLink() { revision++; setChanged(); sync(); }
    protected final void requireThread() { if (level instanceof ServerLevel server && !server.getServer().isSameThread()) throw new IllegalStateException("Mirror API must run on server thread"); }
    public final Resolution resolvePeer(boolean requirePair) {
        requireThread();
        if (!(level instanceof ServerLevel server) || link == null || !physicallyPresent() || (requirePair && !linked)) return new Resolution(Status.INVALID, null, null);
        ServerLevel remote = server.getServer().getLevel(link.dimension());
        if (remote == null) return new Resolution(Status.UNAVAILABLE, null, null);
        LevelChunk chunk = remote.getChunkSource().getChunkNow(link.pos().getX() >> 4, link.pos().getZ() >> 4);
        if (chunk == null) return new Resolution(Status.UNAVAILABLE, null, remote);
        BlockEntity tile = chunk.getBlockEntity(link.pos(), LevelChunk.EntityCreationType.CHECK);
        if (!(tile instanceof AbstractMirrorBlockEntity peer) || peer == this || peer.kind() != kind() || peer.isRemoved()) return new Resolution(Status.INVALID, null, remote);
        if (requirePair && (!peer.linked || !endpoint().equals(peer.link))) return new Resolution(Status.INVALID, peer, remote);
        return new Resolution(Status.VALID, peer, remote);
    }
    public final boolean isLinkValid() {
        Resolution resolution = resolvePeer(true);
        if (resolution.status == Status.INVALID && linked) { linked = false; changedLink(); }
        return resolution.status == Status.VALID;
    }
    public final boolean isLinkValidSimple() { return resolvePeer(true).status == Status.VALID; }
    public final boolean restoreLink() {
        if (linking || removing || !physicallyPresent()) return false;
        requireThread(); linking = true;
        try {
            Resolution destination = resolvePeer(false);
            if (destination.status != Status.VALID) { if (destination.status == Status.INVALID && linked) { linked = false; changedLink(); } return false; }
            AbstractMirrorBlockEntity peer = destination.peer;
            Resolution pair = peer.resolvePeer(true);
            if (pair.status == Status.VALID || pair.status == Status.UNAVAILABLE || peer.linking || peer.removing) return false;
            peer.linking = true;
            try { peer.link = endpoint(); peer.linked = true; linked = true; peer.changedLink(); changedLink(); return true; }
            finally { peer.linking = false; }
        } finally { linking = false; }
    }
    public final void invalidateLink() {
        requireThread();
        Resolution resolution = resolvePeer(false);
        if (resolution.status == Status.VALID && endpoint().equals(resolution.peer.link)) { resolution.peer.linked = false; resolution.peer.changedLink(); }
        if (linked) { linked = false; changedLink(); }
    }
    public final void addInstability(int amount) { requireThread(); if (amount > 0) { instability = (int)Math.min(Integer.MAX_VALUE, (long)instability + amount); setChanged(); } }
    protected void tickOutput() {}
    public final void serverTick() {
        if (!(level instanceof ServerLevel server) || !physicallyPresent() || removing || ticking) return;
        requireThread();ticking=true;
        try {
            tickOutput();
            if (removing || !physicallyPresent()) return;
            if (instability > instabilityThreshold()) { AuraManager.addFlux(server, worldPosition, 1); instability -= instabilityThreshold(); setChanged(); }
            if (instability > 0 && count % 100 == 0) instability--;
            if (count++ % inc == 0) { if (!isLinkValidSimple()) { if (inc < 600) inc += 20; restoreLink(); } else inc = 40; }
        }finally{ticking=false;}
    }
    public final void setCreativeRemoval(boolean value) { creativeRemoval = value; }
    protected final boolean removing() { return removing; }
    public final void removeMirror() {
        if (removing || !(level instanceof ServerLevel server)) return;
        removing = true;
        net.minecraft.world.item.ItemStack drop = new net.minecraft.world.item.ItemStack(getBlockState().getBlock());
        if (linked && link != null) link.writeTo(drop);
        if (!creativeRemoval) {
            if(placement!=null)MirrorRefundSavedData.get(server).addPlacement(worldPosition,facing(),drop,placement);
            else MirrorRefundSavedData.get(server).add(worldPosition,facing(),drop);
        }
        onPhysicalRemoval();
        // Paid ownership exists before a synchronous sync/removal listener can reenter.
        // Resolve while this physical endpoint is still present; only its own backlink may be cleared.
        invalidateLink();
    }
    protected void onPhysicalRemoval() {}
    final void cancelPlacement(){if(placement!=null)placement.canceled();}
    protected final void sync() { if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3); }
    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); writeSync(tag); }
    private void writeSync(CompoundTag tag) { if (link != null) tag.put("MirrorLink", link.save()); tag.putBoolean("linked", linked); tag.putInt("instability", instability); }
    @Override public void load(CompoundTag tag) { super.load(tag); link = MirrorLink.read(tag); linked = link != null && tag.getBoolean("linked"); instability = Math.max(0, tag.getInt("instability")); count = 0; inc = 40; revision++; }
    @Override public CompoundTag getUpdateTag() { CompoundTag tag = new CompoundTag(); writeSync(tag); return tag; }
    @Override public void handleUpdateTag(CompoundTag tag) { link = MirrorLink.read(tag); linked = link != null && tag.getBoolean("linked"); instability = Math.max(0, tag.getInt("instability")); revision++; }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    public final void transferFx() { if (level != null) level.blockEvent(worldPosition, getBlockState().getBlock(), 1, 0); }
    @Override public boolean triggerEvent(int id, int value) {
        if(id==1){if(level!=null&&level.isClientSide)net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,()->()->thaumcraft.artifice.mirror.client.MirrorEffects.transfer(worldPosition,facing()));return true;}
        return super.triggerEvent(id,value);
    }
}
