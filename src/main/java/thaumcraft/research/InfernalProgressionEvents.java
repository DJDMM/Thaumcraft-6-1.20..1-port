package thaumcraft.research;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Released ConfigResearch's biome visit fact, checked by LivingUpdate every 200 ticks. */
@Mod.EventBusSubscriber(modid = "thaumcraft")
public final class InfernalProgressionEvents {
    private InfernalProgressionEvents() {}
    @SubscribeEvent public static void livingTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 200 != 0
                || !player.isAlive() || player.isSpectator() || !player.serverLevel().getServer().isSameThread()
                || KnowledgeStore.get(player).isResearchKnown("m_hellandback")) return;
        // BiomeDictionary.Type.NETHER maps to the modern biome tag. Dimension
        // identity, a portal touch, a Nether item and a scanner are not the predicate.
        if (player.serverLevel().getBiome(player.blockPosition()).is(BiomeTags.IS_NETHER)
                && KnowledgeStore.recordFact(player, "m_hellandback") && player.connection != null)
            player.displayClientMessage(Component.translatable("got.hellandback").withStyle(ChatFormatting.DARK_PURPLE), true);
    }
}
