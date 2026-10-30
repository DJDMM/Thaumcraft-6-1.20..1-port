package thaumcraft.world.warp;

import java.util.ArrayList;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.registries.ForgeRegistries;
import thaumcraft.catalog.entities.VisualEntitiesModule;
import thaumcraft.equipment.GearSupport;
import thaumcraft.equipment.armor.FortressArmorSupport;
import thaumcraft.equipment.cleansing.CleansingSupport;
import thaumcraft.infusion.InfusionEffects;
import thaumcraft.research.*;
import thaumcraft.world.rift.RiftModule;

/** Released BETA26's 2000-tick activity counter and its deliberately uneven severity branches. */
public final class WarpEvents {
    private WarpEvents() {}
    public static boolean checkWarpEvent(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator() || !player.serverLevel().getServer().isSameThread()
                || RiftModule.WUSS_MODE.get() || CleansingSupport.isProtected(player)) return false;
        KnowledgeStore.addTemporaryWarp(player, -1);
        var knowledge = KnowledgeStore.get(player);
        int normal = knowledge.normalWarp(), actual = normal + knowledge.permanentWarp();
        int gear = GearSupport.getEquippedWarp(player);
        int total = knowledge.temporaryWarp() + actual + gear, counter = knowledge.warpCounter();
        var random = player.serverLevel().random;
        // Draw even with zero balances, preserving the released call order.
        int roll = random.nextInt(100);
        if (counter <= 0 || total <= 0 || roll > Math.sqrt(counter)) return false;
        int warp = Math.min(100, (total + total + counter) / 3);
        KnowledgeStore.setWarpCounter(player, counter - (int)Math.max(5, Math.sqrt(counter) * 2 - gear * 2));
        int severity = random.nextInt(warp) + gear;
        severity = FortressArmorSupport.reduceWarpEventSeverity(player, severity, random);
        WarpNetwork.send(player, 0);
        applyEvent(player, warp, severity, normal);
        if (actual > 10 && !knowledge.isResearchKnown("BATHSALTS") && !knowledge.isResearchKnown("!BATHSALTS")) {
            text(player, "warp.text.8"); KnowledgeStore.recordFact(player, "!BATHSALTS");
        }
        if (actual > 25) KnowledgeStore.recordFact(player, "ELDRITCHMINOR");
        if (actual > 50) KnowledgeStore.recordFact(player, "ELDRITCHMAJOR");
        return true;
    }

    /** Server-only execution entry; tests can exercise exact branches without resetting world RNG. */
    public static void applyEvent(ServerPlayer p, int warp, int severity, int previousNormal) {
        if (!p.isAlive() || p.isSpectator() || !p.serverLevel().getServer().isSameThread()) return;
        if (severity <= 0) return;
        var random = p.serverLevel().random;
        if (severity <= 4) {
            if (!RiftModule.NO_STRESS.get()) p.serverLevel().playSound(p, p.blockPosition(),
                    SoundEvents.CREEPER_PRIMED, SoundSource.AMBIENT, 1, .5F);
        } else if (severity <= 8) {
            if (!RiftModule.NO_STRESS.get()) p.serverLevel().playSound(p,
                    p.getX() + (random.nextFloat()-random.nextFloat())*10,
                    p.getY() + (random.nextFloat()-random.nextFloat())*10,
                    p.getZ() + (random.nextFloat()-random.nextFloat())*10,
                    SoundEvents.GENERIC_EXPLODE, SoundSource.AMBIENT, 4,
                    (1 + (random.nextFloat()-random.nextFloat())*.2F)*.7F);
        } else if (severity <= 12) text(p,"warp.text.11");
        else if (severity <= 16) { effect(p,InfusionEffects.VIS_EXHAUST.get(),5000,Math.min(3,warp/15),true,false);text(p,"warp.text.1"); }
        else if (severity <= 20) { effect(p,WarpModule.THAUMARHIA.get(),Math.min(32000,10*warp),0,true,false);text(p,"warp.text.15"); }
        else if (severity <= 24) { effect(p,WarpModule.UNNATURAL_HUNGER.get(),5000,Math.min(3,warp/15),true,true);text(p,"warp.text.2"); }
        else if (severity <= 28) text(p,"warp.text.12");
        else if (severity <= 32) spawnMist(p,1);
        else if (severity <= 36) effect(p,WarpModule.BLURRED_VISION.get(),Math.min(32000,10*warp),0,false,false);
        else if (severity <= 40) { effect(p,WarpModule.SUN_SCORNED.get(),5000,Math.min(3,warp/15),true,false);text(p,"warp.text.5"); }
        else if (severity <= 44) { effect(p,MobEffects.DIG_SLOWDOWN,1200,Math.min(3,warp/15),false,false);text(p,"warp.text.9"); }
        else if (severity <= 48) { effect(p,InfusionEffects.INFECTIOUS_VIS_EXHAUST.get(),6000,Math.min(3,warp/15),true,false,false);text(p,"warp.text.1"); }
        else if (severity <= 52) { effect(p,MobEffects.NIGHT_VISION,Math.min(40*warp,6000),0,false,false);text(p,"warp.text.10"); }
        else if (severity <= 56) { effect(p,WarpModule.DEATH_GAZE.get(),6000,Math.min(3,warp/15),true,false);text(p,"warp.text.4"); }
        else if (severity <= 60) suddenlySpiders(p,warp,false);
        else if (severity <= 64) text(p,"warp.text.13");
        else if (severity <= 68) spawnMist(p,warp/30);
        else if (severity <= 72) effect(p,MobEffects.BLINDNESS,Math.min(32000,5*warp),0,false,false);
        else if (severity == 76) {
            if (previousNormal > 0) KnowledgeStore.addNormalWarp(p,-1);
            text(p,"warp.text.14");
        } else if (severity <= 80) { effect(p,WarpModule.UNNATURAL_HUNGER.get(),6000,Math.min(3,warp/15),true,true);text(p,"warp.text.2"); }
        else if (severity <= 88) { if (spawn(p,"cultist_portal_lesser",false,false)) text(p,"warp.text.16"); }
        else if (severity <= 92) suddenlySpiders(p,warp,true);
        else spawnMist(p,warp/15);
    }
    private static void effect(ServerPlayer p,MobEffect type,int duration,int amplifier,boolean uncurable,boolean meat) {
        effect(p,type,duration,amplifier,uncurable,meat,true);
    }
    private static void effect(ServerPlayer p,MobEffect type,int duration,int amplifier,boolean uncurable,boolean meat,boolean ambient) {
        var effect = new MobEffectInstance(type,duration,amplifier,ambient,true);
        if (uncurable) effect.setCurativeItems(new ArrayList<>());
        if (meat) {
            effect.getCurativeItems().add(new ItemStack(Items.ROTTEN_FLESH));
            Item brain=ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("thaumcraft","brain"));
            if (brain!=null && brain!=Items.AIR) effect.getCurativeItems().add(new ItemStack(brain));
        }
        p.addEffect(effect);
    }
    private static void text(ServerPlayer p,String key) {
        p.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.DARK_PURPLE,ChatFormatting.ITALIC),true);
    }
    public static void spawnMist(ServerPlayer p,int guardians) {
        WarpNetwork.send(p,1);
        for(int i=0;i<Math.min(8,guardians);i++) spawn(p,"eldritch_guardian",true,false);
        text(p,"warp.text.6");
    }
    public static void suddenlySpiders(ServerPlayer p,int warp,boolean real) {
        for(int i=0;i<Math.min(50,warp);i++) spawn(p,"mind_spider",true,!real);
        text(p,"warp.text.7");
    }
    private static boolean spawn(ServerPlayer p,String id,boolean target,boolean harmless) {
        var registration=VisualEntitiesModule.LIVING.get(id);
        if(registration==null) return false;
        var mob=registration.get().create(p.serverLevel());
        // A catalogue shell is never a valid runtime substitute.
        if(mob==null || mob.getClass()==thaumcraft.catalog.entities.VisualMobEntity.class) return false;
        var level=p.serverLevel(); var random=level.random; BlockPos origin=p.blockPosition();
        for(int attempt=0;attempt<50;attempt++) {
            BlockPos at=origin.offset(Mth.nextInt(random,7,24)*Mth.nextInt(random,-1,1),
                    Mth.nextInt(random,7,24)*Mth.nextInt(random,-1,1),
                    Mth.nextInt(random,7,24)*Mth.nextInt(random,-1,1));
            if(!level.hasChunkAt(at) || !level.isInWorldBounds(at)
                    || !level.getBlockState(at.below()).isSolidRender(level,at.below())) continue;
            boolean portal=id.equals("cultist_portal_lesser");
            mob.setPos(at.getX()+(portal?.5:0),at.getY()+(portal?1:0),at.getZ()+(portal?.5:0));
            if(!level.noCollision(mob) || level.containsAnyLiquid(mob.getBoundingBox())) continue;
            if(portal) mob.finalizeSpawn(level,level.getCurrentDifficultyAt(at),MobSpawnType.EVENT,null,null);
            if(target) mob.setTarget(p);
            if(mob instanceof MindSpiderEntity spider && harmless) { spider.setViewer(p.getGameProfile().getName());spider.setHarmless(true); }
            return level.addFreshEntity(mob);
        }
        return false;
    }
    public static void checkDeathGaze(ServerPlayer p) {
        var effect=p.getEffect(WarpModule.DEATH_GAZE.get());
        if(effect==null || !p.isAlive() || p.isSpectator()) return;
        int range=Math.min(8+effect.getAmplifier()*3,24);
        Vec3 eye=p.getEyePosition(),look=p.getLookAngle();
        for(var e:p.serverLevel().getEntities(p,p.getBoundingBox().inflate(range))) {
            if(!(e instanceof LivingEntity living) || !living.isAlive() || !living.isPickable()
                    || living.hasEffect(MobEffects.WITHER) || e instanceof Player && !p.serverLevel().getServer().isPvpAllowed()
                    || !p.hasLineOfSight(e)) continue;
            Vec3 delta=e.getBoundingBox().getCenter().subtract(eye);
            double length=delta.length();
            double axial=look.dot(delta);
            if(length<=1e-9 || axial/length<=Math.cos(.75F/2F) || axial>=range) continue;
            living.setLastHurtByMob(p);p.setLastHurtMob(living);
            if(living instanceof PathfinderMob mob) mob.setTarget(p);
            living.addEffect(new MobEffectInstance(MobEffects.WITHER,80));
        }
    }
    public static void finishedEating(LivingEntityUseItemEvent.Finish event) {
        if(!(event.getEntity() instanceof ServerPlayer p)) return;
        var old=p.getEffect(WarpModule.UNNATURAL_HUNGER.get());
        if(old==null) return;
        ItemStack food=event.getItem();
        var id=ForgeRegistries.ITEMS.getKey(food.getItem());
        if(food.is(Items.ROTTEN_FLESH) || id!=null && id.toString().equals("thaumcraft:brain")) {
            int amplifier=old.getAmplifier()-1,duration=old.getDuration()-600;
            p.removeEffect(WarpModule.UNNATURAL_HUNGER.get());
            if(duration>0 && amplifier>=0) {
                var next=new MobEffectInstance(WarpModule.UNNATURAL_HUNGER.get(),duration,amplifier,true,false);
                next.setCurativeItems(new ArrayList<>(java.util.List.of(new ItemStack(Items.ROTTEN_FLESH))));
                p.addEffect(next);
            }
            p.sendSystemMessage(Component.translatable("warp.text.hunger.2").withStyle(ChatFormatting.DARK_GREEN,ChatFormatting.ITALIC));
        } else if(food.isEdible()) p.sendSystemMessage(Component.translatable("warp.text.hunger.1").withStyle(ChatFormatting.DARK_RED,ChatFormatting.ITALIC));
    }
}
