package thaumcraft.world.mobs;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import thaumcraft.research.*;

/** Original consumable Pech curio; it was never a reusable player wand. */
public final class PechWandItem extends Item{
    public PechWandItem(){super(new Properties().rarity(Rarity.RARE));}
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> text,TooltipFlag flags){text.add(Component.translatable("item.curio.text"));}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        ItemStack held=player.getItemInHand(hand);
        if(player instanceof ServerPlayer server){
            if(!KnowledgeStore.get(server).isResearchKnown("BASEAUROMANCY")){player.sendSystemMessage(Component.translatable("not.pechwand").withStyle(ChatFormatting.RED));return InteractionResultHolder.pass(held);}
            if(!player.getAbilities().instabuild)held.shrink(1);
            var sound=ForgeRegistries.SOUND_EVENTS.getValue(MobsModule.id("learn"));if(sound!=null)level.playSound(null,player.blockPosition(),sound,net.minecraft.sounds.SoundSource.NEUTRAL,.5F,.4F/(level.random.nextFloat()*.4F+.8F));
            if(!KnowledgeStore.get(server).isResearchKnown("FOCUSPECH")){
                // Released BETA26 has no FOCUSPECH entry: the original progressResearch attempt is a no-op.
                if(ResearchCatalog.get("FOCUSPECH")!=null)ResearchProgression.advance(server,"FOCUSPECH",KnowledgeStore.get(server).researchStage("FOCUSPECH"));
                player.sendSystemMessage(Component.translatable("got.pechwand").withStyle(ChatFormatting.DARK_PURPLE));
            }
            String[] categories=ResearchCategories.keys().toArray(String[]::new);
            KnowledgeStore.addKnowledge(server,KnowledgeType.OBSERVATION,categories[player.getRandom().nextInt(categories.length)],5+player.getRandom().nextInt(4));
            KnowledgeStore.addKnowledge(server,KnowledgeType.THEORY,categories[player.getRandom().nextInt(categories.length)],6+player.getRandom().nextInt(3));
            player.awardStat(Stats.ITEM_USED.get(this));
        }return InteractionResultHolder.pass(held);
    }
}
