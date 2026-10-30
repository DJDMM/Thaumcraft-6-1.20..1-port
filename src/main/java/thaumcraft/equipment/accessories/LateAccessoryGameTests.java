package thaumcraft.equipment.accessories;

import com.mojang.authlib.GameProfile;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.gametest.*;
import thaumcraft.catalog.CatalogModule;
import thaumcraft.equipment.RechargeSupport;
import thaumcraft.infusion.InfusionEffects;
import thaumcraft.research.*;
import top.theillusivec4.curios.api.*;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

/** Native Curios storage, real XP touches/Mending, death dispatch and physical charge consumers.
 * Detached survival players, initial ailments/charge/orb counts and flight state are fixtures. */
@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public final class LateAccessoryGameTests {
    private LateAccessoryGameTests(){}
    private static ServerPlayer player(GameTestHelper h){
        var p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),new GameProfile(UUID.randomUUID(),"TC6LateGear"));
        p.connection=new ServerGamePacketListenerImpl(h.getLevel().getServer(),new Connection(PacketFlow.SERVERBOUND),p);
        p.setPos(h.absolutePos(new net.minecraft.core.BlockPos(1,3,1)).getCenter());p.setGameMode(GameType.SURVIVAL);return p;
    }
    private static IDynamicStackHandler slot(ServerPlayer p,String id){return CuriosApi.getCuriosInventory(p).resolve().orElseThrow().getStacksHandler(id).orElseThrow().getStacks();}
    private static ItemStack equip(GameTestHelper h,ServerPlayer p,String slot,String item){
        var handler=slot(p,slot);var stack=CatalogModule.stack(item);
        h.assertTrue(!stack.isEmpty()&&handler.insertItem(0,stack,false).isEmpty(),"Native Curios rejected "+item);
        return handler.getStackInSlot(0);
    }
    private static void worn(ServerPlayer p,ItemStack charm,int clock){p.tickCount=clock;((VerdantCharmItem)charm.getItem()).curioTick(new SlotContext("charm",p,0,false,true),charm);}
    private static int theory(ServerPlayer p){return ResearchCatalog.categories().stream().mapToInt(c->KnowledgeStore.get(p).rawKnowledge(KnowledgeType.THEORY,c)).sum();}
    private static ExperienceOrb orb(ServerPlayer p,int value,int count){
        var orb=new ExperienceOrb(p.level(),p.getX(),p.getY(),p.getZ(),value);
        var tag=new CompoundTag();orb.addAdditionalSaveData(tag);tag.putInt("Count",count);orb.readAdditionalSaveData(tag);return orb;
    }
    private static int count(ExperienceOrb orb){var tag=new CompoundTag();orb.addAdditionalSaveData(tag);return tag.getInt("Count");}
    private static void touch(ServerPlayer p,ExperienceOrb orb){p.takeXpDelay=0;orb.playerTouch(p);LateAccessoryEvents.finishPendingXp(orb);}

    @GameTest(template="empty") public static void nativeSlotsAcceptOnlyTheOriginalLateAccessoryTypes(GameTestHelper h){
        var p=player(h);
        for(var entry:Map.of("cloud_ring","ring","curiosity_band","head","charm_undying","charm","verdant_charm","charm").entrySet()){
            for(String id:List.of("necklace","ring","belt","head","body","charm")){
                var handler=slot(p,id);var supplied=CatalogModule.stack(entry.getKey());var left=handler.insertItem(0,supplied,false);
                h.assertTrue(id.equals(entry.getValue())?left.isEmpty():!left.isEmpty()&&handler.getStackInSlot(0).isEmpty(),"Incorrect native slot eligibility "+entry.getKey()+"/"+id);
                handler.setStackInSlot(0,ItemStack.EMPTY);
            }
        }h.succeed();
    }
    @GameTest(template="empty") public static void verdantUsesExactCurePriorityAndOneOperationPerWornInterval(GameTestHelper h){
        var p=player(h);var charm=equip(h,p,"charm","verdant_charm");RechargeSupport.addCharge(charm,p,200);
        p.addEffect(new MobEffectInstance(MobEffects.WITHER,600));p.addEffect(new MobEffectInstance(MobEffects.POISON,600));p.addEffect(new MobEffectInstance(InfusionEffects.FLUX_TAINT.get(),600));
        worn(p,charm,19);h.assertTrue(RechargeSupport.getCharge(charm)==200,"Non20tick callback consumed charge");
        worn(p,charm,20);h.assertTrue(!p.hasEffect(MobEffects.WITHER)&&p.hasEffect(MobEffects.POISON)&&p.hasEffect(InfusionEffects.FLUX_TAINT.get())&&RechargeSupport.getCharge(charm)==180,"Original first cure/20charge differs");
        worn(p,charm,40);h.assertTrue(!p.hasEffect(MobEffects.POISON)&&p.hasEffect(InfusionEffects.FLUX_TAINT.get())&&RechargeSupport.getCharge(charm)==170,"Original poison cure/10charge differs");
        worn(p,charm,60);h.assertTrue(!p.hasEffect(InfusionEffects.FLUX_TAINT.get())&&RechargeSupport.getCharge(charm)==165,"Original FluxTaint cure/5charge differs");h.succeed();
    }
    @GameTest(template="empty") public static void unaffordableCureFallsThroughToAffordableCureAndDetachedCopiesCannotSpend(GameTestHelper h){
        var p=player(h);var charm=equip(h,p,"charm","verdant_charm");RechargeSupport.addCharge(charm,p,10);
        p.addEffect(new MobEffectInstance(MobEffects.WITHER,600));p.addEffect(new MobEffectInstance(MobEffects.POISON,600));
        worn(p,charm.copy(),20);h.assertTrue(p.hasEffect(MobEffects.POISON)&&RechargeSupport.getCharge(charm)==10,"Detached stack consumed physical owner charge");
        worn(p,charm,40);h.assertTrue(p.hasEffect(MobEffects.WITHER)&&!p.hasEffect(MobEffects.POISON)&&RechargeSupport.getCharge(charm)==0,"Unaffordable Wither prevented original affordable poison branch");
        worn(p,charm,60);h.assertTrue(p.hasEffect(MobEffects.WITHER)&&RechargeSupport.getCharge(charm)==0,"Empty charm manufactured a cure");h.succeed();
    }
    @GameTest(template="empty") public static void lifeAndSustainPreserveNbtChargeAndAirBeforeFood(GameTestHelper h){
        var p=player(h);var charm=equip(h,p,"charm","verdant_charm");RechargeSupport.addCharge(charm,p,200);p.setHealth(10);
        worn(p,charm,20);h.assertTrue(p.getHealth()==10&&RechargeSupport.getCharge(charm)==200,"Untyped charm gained a life effect");
        charm.getOrCreateTag().putByte("type",(byte)1);worn(p,charm,40);h.assertTrue(p.getHealth()==11&&RechargeSupport.getCharge(charm)==195,"Life charm did not heal1/cost5");
        charm.getOrCreateTag().putByte("type",(byte)2);p.setAirSupply(99);p.getFoodData().setFoodLevel(10);p.getFoodData().setSaturation(0);
        worn(p,charm,60);h.assertTrue(p.getAirSupply()==300&&p.getFoodData().getFoodLevel()==10&&RechargeSupport.getCharge(charm)==194,"Sustain did not prioritize air300/cost1");
        worn(p,charm,80);h.assertTrue(p.getFoodData().getFoodLevel()==11&&Math.abs(p.getFoodData().getSaturationLevel()-.6F)<.0001&&RechargeSupport.getCharge(charm)==193,"Sustain food/saturation/cost differs");
        var restored=ItemStack.of(charm.save(new CompoundTag()));h.assertTrue(VerdantCharmItem.type(restored)==2&&RechargeSupport.getCharge(restored)==193&&RechargeSupport.maxCharge(restored,p)==200,"Typed charm save lost original data");h.succeed();
    }
    @GameTest(template="empty") public static void mergedExperienceUnitsPayIndividuallyAndOnlyActualPickupAwardsKnowledge(GameTestHelper h){
        var p=player(h);equip(h,p,"head","curiosity_band");var orb=orb(p,100,3);int before=theory(p);
        for(int i=1;i<=3;i++){touch(p,orb);h.assertTrue(p.totalExperience==i*50&&theory(p)==before+i&&count(orb)==3-i&&orb.getValue()==100,"Merged unit was repeatedly halved or wrongly rewarded at touch"+i);}
        h.assertTrue(orb.isRemoved(),"Last merged unit was not consumed by vanilla");h.succeed();
    }
    @GameTest(template="empty") public static void lateCancelledExperiencePickupRestoresOrbWithoutKnowledgeOrPayment(GameTestHelper h){
        var p=player(h);equip(h,p,"head","curiosity_band");var orb=orb(p,100,3);int before=theory(p);
        Consumer<PlayerXpEvent.PickupXp> cancel=e->{if(e.getEntity()==p)e.setCanceled(true);};MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST,cancel);
        try{touch(p,orb);}finally{MinecraftForge.EVENT_BUS.unregister(cancel);}
        h.assertTrue(p.totalExperience==0&&theory(p)==before&&count(orb)==3&&orb.getValue()==100&&!orb.isRemoved(),"Cancelled pickup retained a payment/reward or changed remaining units");h.succeed();
    }
    @GameTest(template="empty") public static void curiosityPaysBeforeNativeMendingAndInventoryCopiesHaveNoEffect(GameTestHelper h){
        var p=player(h);var tool=new ItemStack(Items.DIAMOND_PICKAXE);tool.enchant(Enchantments.MENDING,1);tool.setDamageValue(150);p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,tool);
        p.getInventory().setItem(1,CatalogModule.stack("curiosity_band"));touch(p,orb(p,20,1));h.assertTrue(tool.getDamageValue()==110&&p.totalExperience==0&&theory(p)==0,"Inventory-only band affected native Mending");
        equip(h,p,"head","curiosity_band");touch(p,orb(p,100,1));h.assertTrue(tool.getDamageValue()==10&&p.totalExperience==0&&theory(p)==1,"Native Mending did not receive exactly the paid50XP");h.succeed();
    }
    @GameTest(template="empty") public static void nativeDeathEventConsumesOnePhysicalCharmAndUsesOriginalTwoBuffs(GameTestHelper h){
        var p=player(h);var charm=equip(h,p,"charm","charm_undying");p.addEffect(new MobEffectInstance(MobEffects.POISON,600));p.setHealth(0);
        var event=new LivingDeathEvent(p,p.damageSources().generic());MinecraftForge.EVENT_BUS.post(event);
        h.assertTrue(event.isCanceled()&&charm.isEmpty()&&slot(p,"charm").getStackInSlot(0).isEmpty()&&p.getHealth()==1&&!p.hasEffect(MobEffects.POISON),"Death rescue lost physical ownership/health/cure");
        var regen=p.getEffect(MobEffects.REGENERATION);var absorption=p.getEffect(MobEffects.ABSORPTION);
        h.assertTrue(regen!=null&&regen.getDuration()==900&&regen.getAmplifier()==1&&absorption!=null&&absorption.getDuration()==100&&absorption.getAmplifier()==1&&!p.hasEffect(MobEffects.FIRE_RESISTANCE),"Undying buffs differ from BETA26");
        var again=new LivingDeathEvent(p,p.damageSources().generic());MinecraftForge.EVENT_BUS.post(again);h.assertTrue(!again.isCanceled(),"Consumed charm rescued a second death");h.succeed();
    }
    @GameTest(template="empty") public static void cancelledDeathAndInventoryOnlyCharmCannotConsumeOrRescue(GameTestHelper h){
        var p=player(h);p.getInventory().setItem(0,CatalogModule.stack("charm_undying"));var event=new LivingDeathEvent(p,p.damageSources().generic());MinecraftForge.EVENT_BUS.post(event);
        h.assertTrue(!event.isCanceled()&&p.getInventory().getItem(0).getCount()==1,"Inventory charm gained worn rescue");
        var charm=equip(h,p,"charm","charm_undying");var cancelled=new LivingDeathEvent(p,p.damageSources().generic());cancelled.setCanceled(true);MinecraftForge.EVENT_BUS.post(cancelled);
        h.assertTrue(charm.getCount()==1,"Already cancelled death consumed a charm");h.succeed();
    }
    @GameTest(template="empty") public static void cloudRingValidatesWornOwnershipAndOneJumpPerFlight(GameTestHelper h){
        var p=player(h);p.setOnGround(false);p.getInventory().setItem(0,CatalogModule.stack("cloud_ring"));h.assertTrue(!CloudRingSupport.tryJump(p),"Inventory-only cloud ring granted flight");
        equip(h,p,"ring","cloud_ring");p.fallDistance=9;p.setDeltaMovement(.1,-.5,.2);h.assertTrue(CloudRingSupport.tryJump(p)&&p.getDeltaMovement().y==.75&&p.fallDistance==0,"Original .75 airborne jump missing");
        h.assertTrue(!CloudRingSupport.tryJump(p),"Second airborne request granted another jump");
        p.setOnGround(true);CloudRingSupport.grounded(p);h.assertTrue(!CloudRingSupport.tryJump(p),"Grounded request granted cloud jump");p.setOnGround(false);
        p.addEffect(new MobEffectInstance(MobEffects.JUMP,200,1));h.assertTrue(CloudRingSupport.tryJump(p)&&Math.abs(p.getDeltaMovement().y-.95)<.0001,"Grounding did not rearm jump boost");h.succeed();
    }
}
