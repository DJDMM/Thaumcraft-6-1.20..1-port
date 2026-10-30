package thaumcraft.world.taint;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import thaumcraft.catalog.entities.VisualMobEntity;
import thaumcraft.world.rift.FluxTaintEffect;

/** Native mobile tainted monster; old catalogue saves acquire real AI on load. */
abstract class TaintedMobileMob extends VisualMobEntity implements Enemy {
    private boolean nativePersistence;
    TaintedMobileMob(EntityType<? extends VisualMobEntity> type,Level level) {
        super(type,level);
        setNoAi(false);setNoGravity(false);nativePersistence=false;
    }
    @Override public boolean canAttackType(EntityType<?> type) {
        ResourceLocation id=ForgeRegistries.ENTITY_TYPES.getKey(type);
        return !(id!=null&&id.getNamespace().equals("thaumcraft")&&java.util.Set.of("thaumic_slime","taint_crawler","taint_swarm","taintacle","taintacle_tiny","taintacle_giant","taint_seed","taint_seed_prime").contains(id.getPath()))&&super.canAttackType(type);
    }
    @Override public boolean isAlliedTo(Entity entity) {
        return entity instanceof LivingEntity living&&FluxTaintEffect.isNativeTainted(living)||super.isAlliedTo(entity);
    }
    @Override public boolean removeWhenFarAway(double distance) { return true; }
    @Override protected boolean shouldDespawnInPeaceful() { return true; }
    @Override public boolean isPersistenceRequired() { return nativePersistence; }
    @Override public void setPersistenceRequired() { super.setPersistenceRequired();nativePersistence=true; }
    public boolean isPreventingPlayerRest(Player player) { return true; }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);tag.remove("VisualOnly");tag.putBoolean("PersistenceRequired",nativePersistence);
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);setNoAi(!tag.getBoolean("VisualOnly")&&tag.getBoolean("NoAI"));setNoGravity(false);
        nativePersistence=!tag.getBoolean("VisualOnly")&&tag.getBoolean("PersistenceRequired");
    }
}
