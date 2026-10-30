package thaumcraft.catalog.entities;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import thaumcraft.auromancy.projectile.FocusProjectileEntity;
import thaumcraft.golemancy.entity.ThaumcraftGolemEntity;

/** Original registrations: catalogue mobs and effects, with operational projectiles retaining their IDs. */
public final class VisualEntitiesModule {
    public static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, "thaumcraft");
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "thaumcraft");
    public static final Map<String, RegistryObject<EntityType<VisualMobEntity>>> LIVING = new LinkedHashMap<>();
    public static final Map<String, RegistryObject<EntityType<VisualEffectEntity>>> EFFECTS = new LinkedHashMap<>();
    public static final RegistryObject<EntityType<ThaumcraftGolemEntity>> GOLEM = TYPES.register("golem", () ->
            EntityType.Builder.<ThaumcraftGolemEntity>of(ThaumcraftGolemEntity::new,MobCategory.MISC).sized(.4F,.9F)
                    .clientTrackingRange(4).updateInterval(3).setShouldReceiveVelocityUpdates(true).build("thaumcraft:golem"));
    public static final RegistryObject<EntityType<thaumcraft.golemancy.entity.GolemDartEntity>> GOLEM_DART=TYPES.register("golem_dart",()->
            EntityType.Builder.<thaumcraft.golemancy.entity.GolemDartEntity>of(thaumcraft.golemancy.entity.GolemDartEntity::new,MobCategory.MISC).sized(.2F,.2F)
                    .clientTrackingRange(4).updateInterval(20).setShouldReceiveVelocityUpdates(false).build("thaumcraft:golem_dart"));
    public static final RegistryObject<EntityType<thaumcraft.golemancy.entity.GolemOrbEntity>> GOLEM_ORB=TYPES.register("golem_orb",()->
            EntityType.Builder.<thaumcraft.golemancy.entity.GolemOrbEntity>of(thaumcraft.golemancy.entity.GolemOrbEntity::new,MobCategory.MISC).sized(.25F,.25F)
                    .clientTrackingRange(4).updateInterval(3).setShouldReceiveVelocityUpdates(true).build("thaumcraft:golem_orb"));
    public static final RegistryObject<EntityType<FocusProjectileEntity>> FOCUS_PROJECTILE = TYPES.register("focus_projectile", () ->
            EntityType.Builder.<FocusProjectileEntity>of(FocusProjectileEntity::new,MobCategory.MISC).sized(.15F,.15F)
                    .clientTrackingRange(4).updateInterval(20).setShouldReceiveVelocityUpdates(true).build("thaumcraft:focus_projectile"));
    public static final RegistryObject<EntityType<thaumcraft.world.rift.collapser.CausalityCollapserEntity>> CAUSALITY_COLLAPSER = TYPES.register("causality_collapser", () ->
            EntityType.Builder.<thaumcraft.world.rift.collapser.CausalityCollapserEntity>of(thaumcraft.world.rift.collapser.CausalityCollapserEntity::new,MobCategory.MISC)
                    .sized(.25F,.25F).clientTrackingRange(4).updateInterval(20).setShouldReceiveVelocityUpdates(true).build("thaumcraft:causality_collapser"));

    public static final RegistryObject<EntityType<thaumcraft.artifice.grapple.GrappleEntity>> GRAPPLE=TYPES.register("grapple",()->
            EntityType.Builder.<thaumcraft.artifice.grapple.GrappleEntity>of(thaumcraft.artifice.grapple.GrappleEntity::new,MobCategory.MISC).sized(.1F,.1F)
                    .clientTrackingRange(4).updateInterval(20).setShouldReceiveVelocityUpdates(true).build("thaumcraft:grapple"));

    static {
        ITEMS.register("golem_spawn_egg", () -> new ForgeSpawnEggItem(GOLEM,6842578,8421504,new Item.Properties()));
        ITEMS.register("spell_bat_spawn_egg",()->{var spec=VisualEntitySpec.byId("spell_bat");return new ForgeSpawnEggItem(thaumcraft.auromancy.media.FocusMediaModule.ORIGINAL_SPELL_BAT,spec.eggBase(),spec.eggSpots(),new Item.Properties());});
        for (VisualEntitySpec spec : VisualEntitySpec.ALL) {
            if (java.util.Set.of("alumentum","focus_projectile","golem","golem_dart","golem_orb","causality_collapser","grapple","eldritch_orb","bottle_taint","brainy_zombie","giant_brainy_zombie","inhabited_zombie","special_item","spell_bat").contains(spec.id())) continue;
            if (spec.living()) {
                var type = TYPES.register(spec.id(), () -> originalFlags(EntityType.Builder.<VisualMobEntity>of(
                        switch (spec.id()) {
                            case "turret_basic" -> thaumcraft.artifice.construct.TurretEntity::new;
                            case "turret_advanced" -> thaumcraft.artifice.construct.AdvancedTurretEntity::new;
                            case "arcane_bore" -> thaumcraft.artifice.construct.BoreEntity::new;
                            case "wisp" -> thaumcraft.world.rift.WispEntity::new;
                            case "fire_bat" -> thaumcraft.world.mobs.FirebatEntity::new;
                            case "pech" -> thaumcraft.world.mobs.PechEntity::new;
                            case "mind_spider" -> thaumcraft.world.warp.MindSpiderEntity::new;
                            case "eldritch_guardian" -> thaumcraft.world.warp.EldritchGuardianEntity::new;
                            case "eldritch_crab" -> thaumcraft.world.eldritch.EldritchCrabEntity::new;
                            case "eldritch_warden" -> thaumcraft.world.eldritch.EldritchWardenEntity::new;
                            case "eldritch_golem" -> thaumcraft.world.eldritch.EldritchGolemEntity::new;
                            case "cultist_leader" -> thaumcraft.world.eldritch.CultistLeaderEntity::new;
                            case "cultist_portal_greater" -> thaumcraft.world.eldritch.GreaterCultistPortalEntity::new;
                            case "cultist_knight" -> thaumcraft.world.crimson.CultistKnightEntity::new;
                            case "cultist_cleric" -> thaumcraft.world.crimson.CultistClericEntity::new;
                            case "cultist_portal_lesser" -> thaumcraft.world.crimson.LesserCultistPortalEntity::new;
                            case "taint_crawler" -> thaumcraft.world.taint.TaintCrawlerEntity::new;
                            case "taint_swarm" -> thaumcraft.world.taint.TaintSwarmEntity::new;
                            case "taintacle" -> thaumcraft.world.taint.TaintacleEntity::new;
                            case "taintacle_giant" -> thaumcraft.world.taint.TaintacleGiantEntity::new;
                            case "thaumic_slime" -> thaumcraft.world.taint.ThaumicSlimeEntity::new;
                            case "taint_seed" -> thaumcraft.world.taint.TaintSeedEntity::new;
                            case "taint_seed_prime" -> thaumcraft.world.taint.TaintSeedPrimeEntity::new;
                            case "taintacle_tiny" -> thaumcraft.world.taint.TaintacleSmallEntity::new;
                            default -> VisualMobEntity::new;
                        },
                        java.util.Set.of("wisp", "taint_seed", "taint_seed_prime", "taintacle_tiny", "mind_spider", "eldritch_guardian",
                            "cultist_knight","cultist_cleric","cultist_portal_lesser","taint_crawler","taint_swarm","taintacle","taintacle_giant","thaumic_slime","fire_bat","pech","eldritch_crab","eldritch_warden","eldritch_golem","cultist_leader","cultist_portal_greater").contains(spec.id())
                                ? MobCategory.MONSTER : MobCategory.MISC).sized(spec.width(), spec.height())
                        .clientTrackingRange(Math.max(1, spec.trackingBlocks() / 16))
                        .updateInterval(spec.updateInterval()).setShouldReceiveVelocityUpdates(spec.trackVelocity()),spec.id())
                        .build("thaumcraft:" + spec.id()));
                LIVING.put(spec.id(), type);
                ITEMS.register(spec.id() + "_spawn_egg", () -> new ForgeSpawnEggItem(type,
                        spec.eggBase(), spec.eggSpots(), new Item.Properties()));
            } else {
                var type = TYPES.register(spec.id(), () -> EntityType.Builder.<VisualEffectEntity>of(
                        (entityType, level) -> spec.id().equals("flux_rift")
                                ? new thaumcraft.world.rift.FluxRiftEntity(entityType, level)
                                : spec.id().equals("falling_taint") ? new thaumcraft.world.taint.FallingTaintEntity(entityType, level)
                                : new VisualEffectEntity(entityType, level), MobCategory.MISC).sized(spec.width(), spec.height())
                        .clientTrackingRange(Math.max(1, spec.trackingBlocks() / 16))
                        .updateInterval(spec.updateInterval()).setShouldReceiveVelocityUpdates(spec.trackVelocity())
                        .build("thaumcraft:" + spec.id()));
                EFFECTS.put(spec.id(), type);
            }
        }
    }

    private VisualEntitiesModule() {}
    private static EntityType.Builder<VisualMobEntity> originalFlags(EntityType.Builder<VisualMobEntity> builder,String id){
        if(java.util.Set.of("fire_bat","eldritch_golem","cultist_portal_lesser","cultist_portal_greater").contains(id))builder.fireImmune();
        return builder;
    }

    public static void register(IEventBus bus) {
        TYPES.register(bus);
        ITEMS.register(bus);
        thaumcraft.world.rift.WispSounds.register(bus);
        thaumcraft.world.taint.TaintEntitySounds.register(bus);
        bus.addListener(VisualEntitiesModule::attributes);
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        var attributes = VisualMobEntity.createMobAttributes().add(Attributes.MAX_HEALTH, 20)
                .add(Attributes.MOVEMENT_SPEED, 0).add(Attributes.FOLLOW_RANGE, 0).build();
        LIVING.forEach((id,type) -> event.put(type.get(), switch (id) {
            case "turret_basic" -> thaumcraft.artifice.construct.TurretEntity.attributes().build();
            case "turret_advanced" -> thaumcraft.artifice.construct.AdvancedTurretEntity.attributes().build();
            case "arcane_bore" -> thaumcraft.artifice.construct.BoreEntity.attributes().build();
            case "wisp" -> thaumcraft.world.rift.WispEntity.attributes().build();
            case "fire_bat" -> thaumcraft.world.mobs.FirebatEntity.attributes().build();
            case "pech" -> thaumcraft.world.mobs.PechEntity.attributes().build();
            case "mind_spider" -> thaumcraft.world.warp.MindSpiderEntity.attributes().build();
            case "eldritch_guardian" -> thaumcraft.world.warp.EldritchGuardianEntity.attributes().build();
            case "eldritch_crab" -> thaumcraft.world.eldritch.EldritchCrabEntity.attributes().build();
            case "eldritch_warden" -> thaumcraft.world.eldritch.EldritchWardenEntity.attributes().build();
            case "eldritch_golem" -> thaumcraft.world.eldritch.EldritchGolemEntity.attributes().build();
            case "cultist_leader" -> thaumcraft.world.eldritch.CultistLeaderEntity.attributes().build();
            case "cultist_portal_greater" -> thaumcraft.world.eldritch.GreaterCultistPortalEntity.attributes().build();
            case "cultist_knight" -> thaumcraft.world.crimson.CultistKnightEntity.attributes().build();
            case "cultist_cleric" -> thaumcraft.world.crimson.CultistClericEntity.attributes().build();
            case "cultist_portal_lesser" -> thaumcraft.world.crimson.LesserCultistPortalEntity.attributes().build();
            case "taint_crawler" -> thaumcraft.world.taint.TaintCrawlerEntity.attributes().build();
            case "taint_swarm" -> thaumcraft.world.taint.TaintSwarmEntity.attributes().build();
            case "taintacle" -> thaumcraft.world.taint.TaintacleEntity.attributes().build();
            case "taintacle_giant" -> thaumcraft.world.taint.TaintacleGiantEntity.attributes().build();
            case "thaumic_slime" -> thaumcraft.world.taint.ThaumicSlimeEntity.attributes().build();
            case "taint_seed" -> thaumcraft.world.taint.TaintSeedEntity.attributes().build();
            case "taint_seed_prime" -> thaumcraft.world.taint.TaintSeedPrimeEntity.attributes().build();
            case "taintacle_tiny" -> thaumcraft.world.taint.TaintacleSmallEntity.attributes().build();
            default -> attributes;
        }));
        event.put(GOLEM.get(),ThaumcraftGolemEntity.attributes().build());
    }
}
