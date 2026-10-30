package thaumcraft.world.warp;

import java.util.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import thaumcraft.catalog.entities.VisualEntitiesModule;
import thaumcraft.equipment.cleansing.CleansingModule;
import thaumcraft.infusion.InfusionEffects;
import thaumcraft.research.*;
import thaumcraft.world.rift.RiftModule;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public final class WarpGameTests {
    private static FakePlayer player(GameTestHelper h){
        var p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"warp_qa"));
        p.setPos(h.absolutePos(new BlockPos(3,2,3)).getCenter());return p;
    }
    private static void close(FakePlayer p){p.discard();p.serverLevel().players().remove(p);}
    @GameTest(template="empty") public static void wardAndWussPreventBothActivityAndTemporaryDecay(GameTestHelper h){
        var p=player(h);boolean old=RiftModule.WUSS_MODE.get();
        try{
            KnowledgeStore.addTemporaryWarp(p,20);KnowledgeStore.addNormalWarp(p,30);
            p.addEffect(new MobEffectInstance(CleansingModule.WARP_WARD.get(),200));
            var before=KnowledgeStore.get(p).save();
            h.assertTrue(!WarpEvents.checkWarpEvent(p)&&before.equals(KnowledgeStore.get(p).save()),"Ward allowed Warp activity or decay");
            p.removeEffect(CleansingModule.WARP_WARD.get());RiftModule.WUSS_MODE.set(true);
            h.assertTrue(!WarpEvents.checkWarpEvent(p)&&before.equals(KnowledgeStore.get(p).save()),"Original wuss guard failed to suppress Warp event/decay");
        }finally{RiftModule.WUSS_MODE.set(old);close(p);}h.succeed();
    }
    @GameTest(template="empty") public static void activityCounterPersistsWithoutChangingThreeBalances(GameTestHelper h){
        var p=player(h);
        try{
            KnowledgeStore.addPermanentWarp(p,20);KnowledgeStore.addNormalWarp(p,10);KnowledgeStore.addTemporaryWarp(p,5);
            KnowledgeStore.setWarpCounter(p,19);var k=KnowledgeStore.get(p);
            var loaded=PlayerKnowledge.load(k.save());
            h.assertTrue(loaded.permanentWarp()==20&&loaded.normalWarp()==10&&loaded.temporaryWarp()==5&&loaded.warpCounter()==19,"Counter debit corrupted balances/save");
            KnowledgeStore.setWarpCounter(p,-100);h.assertTrue(k.warpCounter()==0,"Counter was allowed below0");
            KnowledgeStore.setWarpCounter(p,100000);h.assertTrue(k.warpCounter()==1500,"Counter overflowed valid saved limit");
        }finally{close(p);}h.succeed();
    }
    @GameTest(template="empty") public static void exactOriginalIllnessDurationsAmplifiersAndCuratives(GameTestHelper h){
        var p=player(h);
        try{
            int[] severity={16,20,24,36,40,44,48,52,56,72};
            MobEffect[] effects={InfusionEffects.VIS_EXHAUST.get(),WarpModule.THAUMARHIA.get(),WarpModule.UNNATURAL_HUNGER.get(),
                WarpModule.BLURRED_VISION.get(),WarpModule.SUN_SCORNED.get(),MobEffects.DIG_SLOWDOWN,InfusionEffects.INFECTIOUS_VIS_EXHAUST.get(),
                MobEffects.NIGHT_VISION,WarpModule.DEATH_GAZE.get(),MobEffects.BLINDNESS};
            int[] duration={5000,1000,5000,1000,5000,1200,6000,4000,6000,500};
            int[] amplifiers={3,0,3,0,3,3,3,0,3,0};
            for(int i=0;i<severity.length;i++){
                p.removeAllEffects();WarpEvents.applyEvent(p,100,severity[i],0);
                var effect=p.getEffect(effects[i]);
                h.assertTrue(effect!=null&&effect.getDuration()==duration[i]&&effect.getAmplifier()==amplifiers[i],"Released effect branch differed at severity"+severity[i]);
                if(Set.of(16,20,40,48,56).contains(severity[i]))h.assertTrue(effect.getCurativeItems().isEmpty(),"Uncurable original branch gained milk cure");
                if(severity[i]==24)h.assertTrue(effect.getCurativeItems().stream().anyMatch(s->s.is(Items.ROTTEN_FLESH)),"Original unnatural hunger lost rotten flesh cure");
            }
        }finally{close(p);}h.succeed();
    }
    @GameTest(template="empty") public static void severity76IsSingleNormalRemovalWhile73Through75AreHunger(GameTestHelper h){
        var p=player(h);
        try{
            KnowledgeStore.addNormalWarp(p,5);KnowledgeStore.addPermanentWarp(p,9);KnowledgeStore.addTemporaryWarp(p,7);
            for(int severity=73;severity<=75;severity++){p.removeAllEffects();WarpEvents.applyEvent(p,100,severity,5);
                h.assertTrue(p.hasEffect(WarpModule.UNNATURAL_HUNGER.get())&&KnowledgeStore.get(p).normalWarp()==5,"Released unequal73..76 severity branches changed");}
            p.removeAllEffects();WarpEvents.applyEvent(p,100,76,5);var k=KnowledgeStore.get(p);
            h.assertTrue(!p.hasEffect(WarpModule.UNNATURAL_HUNGER.get())&&k.normalWarp()==4&&k.permanentWarp()==9&&k.temporaryWarp()==7,"Severity76 removed wrong warp type");
        }finally{close(p);}h.succeed();
    }
    @GameTest(template="empty") public static void rottenFoodUsesSpecificHungerReductionAndRetainsOriginalCuratives(GameTestHelper h){
        var p=player(h);
        try{
            WarpEvents.applyEvent(p,100,24,0);
            WarpEvents.finishedEating(new LivingEntityUseItemEvent.Finish(p,new ItemStack(Items.ROTTEN_FLESH),32,ItemStack.EMPTY));
            var effect=p.getEffect(WarpModule.UNNATURAL_HUNGER.get());
            h.assertTrue(effect!=null&&effect.getAmplifier()==2&&effect.getDuration()==4400&&!effect.isVisible(),"Original food recovery parameters changed");
            h.assertTrue(effect.getCurativeItems().size()==1&&effect.getCurativeItems().get(0).is(Items.ROTTEN_FLESH),"Recovered Hunger retained the initial brain curative quirk");
            var before=effect.save(new CompoundTag());
            WarpEvents.finishedEating(new LivingEntityUseItemEvent.Finish(p,new ItemStack(Items.BREAD),32,ItemStack.EMPTY));
            h.assertTrue(before.equals(p.getEffect(WarpModule.UNNATURAL_HUNGER.get()).save(new CompoundTag())),"Ordinary food cured unnatural hunger");
        }finally{close(p);}h.succeed();
    }
    @GameTest(template="empty") public static void privateAndRealMindSpidersHaveNativeDamageAndOriginalSaveQuirk(GameTestHelper h){
        var mob=VisualEntitiesModule.LIVING.get("mind_spider").get().create(h.getLevel());
        h.assertTrue(mob instanceof MindSpiderEntity&&!mob.isNoAi()&&mob.getMaxHealth()==1&&mob.getAttributeValue(Attributes.ATTACK_DAMAGE)==1,"Mind spider still a catalogue shell");
        var spider=(MindSpiderEntity)mob;Cow cow=EntityType.COW.create(h.getLevel());
        spider.setPos(h.absolutePos(new BlockPos(2,2,2)).getCenter());cow.setPos(spider.position().add(1,0,0));
        try{
            spider.setHarmless(true);spider.setViewer("owner");float health=cow.getHealth();
            h.assertTrue(!spider.doHurtTarget(cow)&&cow.getHealth()==health,"Private hallucination damaged its target");
            var nbt=new CompoundTag();spider.addAdditionalSaveData(nbt);
            var loaded=(MindSpiderEntity)VisualEntitiesModule.LIVING.get("mind_spider").get().create(h.getLevel());loaded.readAdditionalSaveData(nbt);
            h.assertTrue(loaded.harmless()&&loaded.viewer().equals("owner")&&!loaded.isNoAi()&&!nbt.contains("VisualOnly"),"Original hallucination identity failed migration");
            loaded.discard();spider.setHarmless(false);h.assertTrue(spider.doHurtTarget(cow)&&cow.getHealth()<health,"Real mind spider has no native attack");
        }finally{spider.discard();cow.discard();}h.succeed();
    }
    @GameTest(template="empty") public static void guardianHasOriginalCombatAttributesAndPersistsHome(GameTestHelper h){
        var raw=VisualEntitiesModule.LIVING.get("eldritch_guardian").get().create(h.getLevel());
        h.assertTrue(raw instanceof EldritchGuardianEntity&&!raw.isNoAi()&&raw.getMaxHealth()==50
            &&raw.getAttributeValue(Attributes.ATTACK_DAMAGE)==7&&raw.getArmorValue()==4&&raw.getAttributeValue(Attributes.FOLLOW_RANGE)==40,"Guardian native contract absent");
        var guardian=(EldritchGuardianEntity)raw;var home=h.absolutePos(new BlockPos(2,2,2));guardian.restrictTo(home,24);
        var nbt=new CompoundTag();guardian.addAdditionalSaveData(nbt);
        var loaded=(EldritchGuardianEntity)VisualEntitiesModule.LIVING.get("eldritch_guardian").get().create(h.getLevel());
        try{loaded.readAdditionalSaveData(nbt);h.assertTrue(!nbt.contains("VisualOnly")&&loaded.getRestrictCenter().equals(home)
            &&loaded.getRestrictRadius()==24&&!loaded.removeWhenFarAway(100000),"Guardian original home persistence/despawn changed");}
        finally{guardian.discard();loaded.discard();}h.succeed();
    }
    @GameTest(template="empty") public static void deathGazeUsesOriginalNarrowConeAndRealLineOfSight(GameTestHelper h){
        var p=player(h);p.setPos(h.absolutePos(new BlockPos(3,2,1)).getCenter());p.setYRot(0);p.setXRot(0);
        var centre=EntityType.COW.create(h.getLevel());var side=EntityType.COW.create(h.getLevel());
        centre.setPos(p.position().add(0,0,4));side.setPos(p.position().add(4,0,0));
        h.getLevel().addFreshEntity(centre);h.getLevel().addFreshEntity(side);
        try{
            p.addEffect(new MobEffectInstance(WarpModule.DEATH_GAZE.get(),200));WarpEvents.checkDeathGaze(p);
            h.assertTrue(centre.hasEffect(MobEffects.WITHER)&&!side.hasEffect(MobEffects.WITHER),"Gaze ignored the original .75radian aperture");
            h.assertTrue(centre.getLastHurtByMob()==p,"Gaze did not set native revenge target");
        }finally{centre.discard();side.discard();close(p);}h.succeed();
    }
}
