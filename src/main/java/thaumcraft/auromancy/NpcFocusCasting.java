package thaumcraft.auromancy;

import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.*;
import net.minecraftforge.registries.ForgeRegistries;
import thaumcraft.auromancy.focus.*;
import thaumcraft.auromancy.projectile.FocusProjectileEntity;
import thaumcraft.catalog.CatalogModule;
import thaumcraft.catalog.entities.VisualMobEntity;

/** BETA26 Pech packages use the same projectile physics and effect programs without player payments. */
public final class NpcFocusCasting {
    private static final Set<String> EFFECTS=Set.of(FocusNodeRegistry.CURSE,FocusNodeRegistry.FLUX,FocusNodeRegistry.EARTH,FocusNodeRegistry.AIR,FocusNodeRegistry.FIRE);
    private NpcFocusCasting(){}
    public static boolean isCaster(Entity entity){
        return entity instanceof VisualMobEntity mob && mob.getClass()!=VisualMobEntity.class
                && mob.spec().id().equals("pech") && mob.isAlive() && !mob.isRemoved() && !mob.isSpectator()
                && mob.level() instanceof ServerLevel;
    }
    public static FocusPlan plan(String effect){
        if(!EFFECTS.contains(effect))return null;
        var graph=new FocusGraph(List.of(
            new FocusGraph.Node(0,-1,List.of(1),0,0,FocusNodeRegistry.ROOT,Map.of()),
            new FocusGraph.Node(1,0,List.of(2),0,1,FocusNodeRegistry.PROJECTILE,Map.of("speed",2,"option",0)),
            new FocusGraph.Node(2,1,List.of(),0,2,effect,FocusNodeRegistry.get(effect).defaultSettings())));
        var result=FocusCompiler.compile(graph,CatalogModule.stack("focus_1"),ignored->true);
        return result.success()?result.plan():null;
    }
    public static boolean valid(FocusPlan plan){
        if(plan==null||plan.graph().nodes().size()!=3||plan.maxComplexity()!=15)return false;
        var nodes=plan.graph().nodes();var root=nodes.get(0);var medium=nodes.get(1);var effect=nodes.get(2);
        return root.id()==0&&root.parent()==-1&&root.key().equals(FocusNodeRegistry.ROOT)&&root.settings().isEmpty()&&root.children().equals(List.of(1))
            &&medium.id()==1&&medium.parent()==0&&medium.key().equals(FocusNodeRegistry.PROJECTILE)&&medium.children().equals(List.of(2))
            &&medium.settings().equals(Map.of("speed",2,"option",0))
            &&effect.id()==2&&effect.parent()==1&&effect.children().isEmpty()&&EFFECTS.contains(effect.key())
            &&effect.settings().equals(FocusNodeRegistry.get(effect.key()).defaultSettings());
    }
    public static boolean cast(LivingEntity caster,LivingEntity target){
        if(!isCaster(caster)||target==null||!target.isAlive()||target.level()!=caster.level()
                || !caster.getServer().isSameThread())return false;
        var random=caster.getRandom();
        String effect=random.nextBoolean()?FocusNodeRegistry.CURSE:random.nextBoolean()?FocusNodeRegistry.FLUX:
            random.nextBoolean()?FocusNodeRegistry.EARTH:random.nextBoolean()?FocusNodeRegistry.AIR:FocusNodeRegistry.FIRE;
        var plan=plan(effect);if(plan==null)return false;
        Vec3 source=caster.getEyePosition().add(0,-.10000000149011612D,0);
        double offset=caster.distanceTo(target)/6F;
        Vec3 direction=target.position().add(0,target.getBbHeight()/2F+offset,0).subtract(source).normalize();
        if(!FocusProjectileEntity.spawnNpc(caster,plan,source,direction))return false;
        var level=(ServerLevel)caster.level();
        if(effect.equals(FocusNodeRegistry.FIRE))FocusCasting.playFireCastSound(level,caster);
        else{
            FocusEffects.playCastSound(level,caster,effect);AdvancedFocusEffects.playCastSound(level,caster,effect);
            thaumcraft.auromancy.remaining.RemainingFocusEffects.playCastSound(level,caster,effect);
        }
        return true;
    }
    public static void impact(ServerLevel level,LivingEntity caster,FocusPlan plan,HitResult hit,Vec3 direction){
        if(!isCaster(caster)||caster.level()!=level||!valid(plan)||hit==null||!level.getServer().isSameThread()
                ||hit.getType()==HitResult.Type.MISS||!FocusEffects.loaded(level,net.minecraft.core.BlockPos.containing(hit.getLocation())))return;
        if(hit instanceof EntityHitResult target&&(target.getEntity().level()!=level||target.getEntity().isRemoved()))return;
        var effect=plan.effect();
        switch(effect.key()){
            case FocusNodeRegistry.FIRE -> FocusCasting.applyFire(level,caster,hit,1,0,1);
            case FocusNodeRegistry.AIR,FocusNodeRegistry.EARTH -> FocusEffects.applyLiving(level,caster,effect,hit,direction,1);
            case FocusNodeRegistry.FLUX -> AdvancedFocusEffects.applyLiving(level,caster,effect,hit,direction,1);
            case FocusNodeRegistry.CURSE -> thaumcraft.auromancy.remaining.RemainingFocusEffects.curse(level,caster,effect,hit,1);
            default -> throw new IllegalStateException("Unvalidated NPC focus effect");
        }
    }
}
