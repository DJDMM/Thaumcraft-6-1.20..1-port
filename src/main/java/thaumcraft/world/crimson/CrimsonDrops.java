package thaumcraft.world.crimson;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.registries.ForgeRegistries;
import thaumcraft.research.KnowledgeStore;

/** The released extra player-kill book roll is separate from the cultist loot table. */
public final class CrimsonDrops {
    private CrimsonDrops() {}
    public static ItemStack rites() {
        Item item = ForgeRegistries.ITEMS.getValue(CrimsonModule.id("curio_rites"));
        return item == null || item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }
    public static int denominator(ServerPlayer player) {
        int chance = KnowledgeStore.get(player).isResearchKnown("!CrimsonCultist@2") ? 20 : 4;
        ItemStack book = rites();
        // Released InventoryUtils.getPlayerSlotFor searches mainInventory and exact NBT only.
        for (int slot = 0; slot < player.getInventory().items.size(); slot++)
            if (ItemStack.isSameItemSameTags(player.getInventory().getItem(slot), book)) return 50;
        return chance;
    }
    static void addRites(LivingDropsEvent event, RandomSource random) {
        if (event.isCanceled() || event.getEntity().level().isClientSide || !(event.getEntity() instanceof CultistEntity)
                || !(event.getSource().getEntity() instanceof ServerPlayer player) || player instanceof FakePlayer) return;
        if (random.nextInt(denominator(player)) != 0) return;
        ItemStack book = rites();
        if (!book.isEmpty()) event.getDrops().add(new ItemEntity(event.getEntity().level(), event.getEntity().getX(),
                event.getEntity().getY() + event.getEntity().getEyeHeight(), event.getEntity().getZ(), book));
    }
    public static void onDrops(LivingDropsEvent event) { addRites(event, event.getEntity().getRandom()); }
}
