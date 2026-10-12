package thaumcraft.artifice.mirror;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectSource;
import thaumcraft.essentia.airborne.AirborneEssentiaManager;

/** BETA26 remote aspect source: no essentia tank, suction or tube ports. */
public final class EssentiaMirrorBlockEntity extends AbstractMirrorBlockEntity implements IAspectSource {
    public EssentiaMirrorBlockEntity(BlockPos pos, BlockState state) {
        super(MirrorModule.ESSENTIA_MIRROR.get(), pos, state);
    }
    @Override public AspectList getAspects() { return null; }
    @Override public void setAspects(AspectList aspects) {}
    @Override public boolean isBlocked() { return false; }
    @Override public boolean doesContainerAccept(Aspect aspect) {
        return AirborneEssentiaManager.mirrorCanAccept(this, aspect);
    }
    @Override public int addToContainer(Aspect aspect, int amount) {
        return amount == 1 && AirborneEssentiaManager.mirrorAdd(this, aspect) ? 0 : amount;
    }
    @Override public boolean takeFromContainer(Aspect aspect, int amount) {
        return amount == 1 && AirborneEssentiaManager.mirrorDrain(this, aspect);
    }
    @Override public boolean doesContainerContainAmount(Aspect aspect, int amount) {
        return amount == 1 && AirborneEssentiaManager.mirrorFind(this, aspect);
    }
    @Override public boolean takeFromContainer(AspectList aspects) { return false; }
    @Override public boolean doesContainerContain(AspectList aspects) { return false; }
    @Override public int containerContains(Aspect aspect) { return 0; }
    @Override public void setRemoved() {
        super.setRemoved();
        AirborneEssentiaManager.forgetConsumer(this);
    }
}
