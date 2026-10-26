package thaumcraft.alchemy;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraftforge.network.NetworkHooks;

public final class AlumentumProjectile extends ThrowableItemProjectile {
    public AlumentumProjectile(EntityType<? extends AlumentumProjectile> type, Level level) { super(type, level); }
    @Override protected Item getDefaultItem() { return AlchemyModule.ALUMENTUM.get(); }
    @Override protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (!level().isClientSide && !isRemoved()) {
            level().explode(this, getX(), getY(), getZ(), 1.1F, Level.ExplosionInteraction.TNT);
            discard();
        }
    }
    @Override public void tick() {
        super.tick();
        if (level().isClientSide) {
            thaumcraft.fx.emitters.MachineFxPrograms.throwable(this,false);
        }
        if (!level().isClientSide && tickCount > 1200) discard();
    }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
}
