package thaumcraft.world.taint;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import thaumcraft.alchemy.AspectCrystalItem;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.catalog.entities.VisualMobEntity;

/** Released larger seed: area2,150health,7attack and two independent bonus crystals. */
public final class TaintSeedPrimeEntity extends TaintSeedEntity {
    public TaintSeedPrimeEntity(EntityType<? extends VisualMobEntity> type, Level level) {
        super(type, level);
        xpReward = 12;
    }
    public static AttributeSupplier.Builder attributes() {
        return TaintSeedEntity.attributes().add(Attributes.MAX_HEALTH, 150).add(Attributes.ATTACK_DAMAGE, 7);
    }
    @Override protected int getArea() { return 2; }
    @Override protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        if (random.nextBoolean()) spawnAtLocation(AspectCrystalItem.create(Aspect.FLUX), getBbHeight() / 2);
        if (random.nextBoolean()) spawnAtLocation(AspectCrystalItem.create(Aspect.FLUX), getBbHeight() / 2);
    }
}
