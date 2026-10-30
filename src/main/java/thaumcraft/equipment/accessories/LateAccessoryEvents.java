package thaumcraft.equipment.accessories;
import java.util.*;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.*;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thaumcraft.research.*;

@Mod.EventBusSubscriber(modid="thaumcraft")
public final class LateAccessoryEvents {
    private static final Set<Player> RESCUING=Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Map<ExperienceOrb,PendingXp> PENDING_XP=new IdentityHashMap<>();
    private record PendingXp(ServerPlayer player,int original,int reduced,int count,KnowledgeType type,String category){}
    private LateAccessoryEvents(){}
    public static ItemStack equipped(Player player,LateAccessoryItem.Kind kind){
        return AccessoriesSupport.equipped(player).stream().filter(s->s.getItem() instanceof LateAccessoryItem item&&item.kind()==kind).findFirst().orElse(ItemStack.EMPTY);
    }
    @SubscribeEvent public static void xp(PlayerXpEvent.PickupXp event){
        // Modern orbs contain several equal-value units. Settle the previous touch before
        // lending this unit's reduced value to vanilla XP/Mending, then restore the rest.
        finishPendingXp(event.getOrb());
        if(!(event.getEntity() instanceof ServerPlayer player)||equipped(player,LateAccessoryItem.Kind.CURIOSITY).isEmpty()||event.getOrb().getValue()<=1)return;
        int original=event.getOrb().value,payment=original/2;event.getOrb().value-=payment;
        float roll=player.getRandom().nextFloat();
        KnowledgeType type=roll<.05*payment?KnowledgeType.THEORY:roll<.2*payment?KnowledgeType.OBSERVATION:null;
        var categories=ResearchCatalog.categories().stream().filter(k->!k.equals("PORT")).toList();
        String category=type==null||categories.isEmpty()?null:categories.get(player.getRandom().nextInt(categories.size()));
        PENDING_XP.put(event.getOrb(),new PendingXp(player,original,event.getOrb().value,orbCount(event.getOrb()),type,category));
    }
    private static int orbCount(ExperienceOrb orb){
        CompoundTag tag=new CompoundTag();orb.addAdditionalSaveData(tag);return tag.getInt("Count");
    }
    static void finishPendingXp(ExperienceOrb orb){
        var pending=PENDING_XP.remove(orb);if(pending==null)return;
        boolean pickedUp=orbCount(orb)<pending.count();
        if(orb.value==pending.reduced())orb.value=pending.original();
        if(pickedUp&&pending.type()!=null&&pending.category()!=null)
            KnowledgeStore.addKnowledge(pending.player(),pending.type(),pending.category(),1);
    }
    private static void finishPendingXp(MinecraftServer server){
        for(var orb:new ArrayList<>(PENDING_XP.keySet()))
            if(orb.level().getServer()==server)finishPendingXp(orb);
    }
    @SubscribeEvent public static void endTick(TickEvent.ServerTickEvent event){
        if(event.phase==TickEvent.Phase.END)finishPendingXp(event.getServer());
    }
    @SubscribeEvent public static void endPlayerTick(TickEvent.PlayerTickEvent event){
        if(event.phase==TickEvent.Phase.END&&event.player instanceof ServerPlayer player)
            for(var orb:new ArrayList<>(PENDING_XP.keySet()))
                if(PENDING_XP.get(orb).player()==player)finishPendingXp(orb);
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event){
        finishPendingXp(event.getServer());
    }
    @SubscribeEvent public static void death(LivingDeathEvent event){
        if(event.isCanceled()||!(event.getEntity() instanceof ServerPlayer player)||!RESCUING.add(player))return;
        try{
            ItemStack physical=equipped(player,LateAccessoryItem.Kind.UNDYING);
            if(physical.isEmpty())return;
            ItemStack specimen=physical.copy();physical.shrink(1);
            player.setHealth(1);player.removeAllEffects();
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION,900,1));
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION,100,1));
            event.setCanceled(true);player.awardStat(Stats.ITEM_USED.get(Items.TOTEM_OF_UNDYING));
            CriteriaTriggers.USED_TOTEM.trigger(player,specimen);
            player.level().broadcastEntityEvent(player,(byte)35);
        }finally{RESCUING.remove(player);}
    }
}
