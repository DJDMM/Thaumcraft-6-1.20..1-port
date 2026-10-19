package thaumcraft.equipment.enchantment;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thaumcraft.alchemy.AspectCrystalItem;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.catalog.blocks.CatalogBlocks;
import thaumcraft.common.lib.enchantment.EnumInfusionEnchantment;
import thaumcraft.scanning.AspectRegistry;

import java.util.ArrayList;
import java.util.List;

/** Global BETA26 enchantment effects; an infused vanilla item needs no Thaumcraft item subclass. */
@Mod.EventBusSubscriber(modid="thaumcraft")
public final class InfusionEnchantmentEffects {
    private static final List<LightRequest> LIGHTS = new ArrayList<>();
    private InfusionEnchantmentEffects() {}

    /** The released bytecode differs from the recovered source's misleading for-loop increment. */
    public static List<ItemEntity> essenceDrops(LivingDropsEvent event, ItemStack weapon) {
        int rank = Math.max(0, Math.min(5, EnumInfusionEnchantment.getInfusionEnchantmentLevel(weapon, EnumInfusionEnchantment.ESSENCE)));
        List<ItemEntity> result = new ArrayList<>();
        if (rank == 0 || !(event.getEntity().level() instanceof ServerLevel level)) return result;
        AspectList remaining = entityAspects(event.getEntity());
        Aspect[] choices = remaining.getAspects();
        if (choices.length == 0) return result;
        int progress = level.random.nextInt(5) < rank ? 0 : 99;
        while (progress < rank && choices.length > 0) {
            Aspect aspect = choices[level.random.nextInt(choices.length)];
            if (remaining.getAmount(aspect) > 0) {
                remaining.remove(aspect, 1);
                ItemStack crystal = AspectCrystalItem.create(aspect);
                if (!crystal.isEmpty()) result.add(new ItemEntity(level, event.getEntity().getX(),
                        event.getEntity().getY() + event.getEntity().getEyeHeight(), event.getEntity().getZ(), crystal));
                progress++;
            }
            choices = remaining.getAspects();
            // In the released JAR this extra increment is conditional, not the unconditional source for-step.
            if (level.random.nextInt(rank) == 0) progress += 1 + level.random.nextInt(2);
        }
        return result;
    }

    private static AspectList entityAspects(net.minecraft.world.entity.Entity entity) {
        if (!(entity instanceof Player)) return AspectRegistry.getAspects(entity);
        AspectList result = new AspectList().add(Aspect.MAN, 4);
        java.util.Random random = new java.util.Random(entity.getName().getString().hashCode());
        Aspect[] choices = Aspect.aspects.values().toArray(new Aspect[0]);
        if (choices.length > 0) for (int i=0;i<3;i++) result.add(choices[random.nextInt(choices.length)], 15);
        return result;
    }

    /** Only a successfully harvested physical block may schedule a light. No item, vis or durability price. */
    public static void harvested(ServerPlayer player, BlockPos pos, ItemStack tool, boolean sneaking) {
        if (!sneaking && EnumInfusionEnchantment.getInfusionEnchantmentLevel(tool, EnumInfusionEnchantment.LAMPLIGHT) > 0)
            LIGHTS.add(new LightRequest(player.serverLevel(), player, pos.immutable(), player.serverLevel().getGameTime()));
    }

    @SubscribeEvent public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || LIGHTS.isEmpty()) return;
        List<LightRequest> requests = List.copyOf(LIGHTS);
        LIGHTS.clear();
        for (LightRequest request : requests) {
            if (!request.eligible()) continue;
            // Modern threaded lighting still contains the removed opaque block's light in this tick.
            // Give the native world/light engine a complete tick before testing the original threshold.
            if (request.level.getGameTime() <= request.harvestTick) LIGHTS.add(request);
            else request.place();
        }
    }

    private record LightRequest(ServerLevel level, ServerPlayer owner, BlockPos pos, long harvestTick) {
        boolean eligible() {
            return owner.isAlive() && owner.level() == level
                    && level.getServer().getLevel(level.dimension()) == level && level.hasChunkAt(pos)
                    && level.getWorldBorder().isWithinBounds(pos) && level.mayInteract(owner, pos)
                    && !owner.blockActionRestricted(level, pos, owner.gameMode.getGameModeForPlayer())
                    && level.isEmptyBlock(pos);
        }
        void place() {
            if (!eligible()
                    || level.getRawBrightness(pos, 0) >= 10) return;
            BlockSnapshot before = BlockSnapshot.create(level.dimension(), level, pos);
            var glimmer = CatalogBlocks.block("effect_glimmer").defaultBlockState();
            if (!level.setBlock(pos, glimmer, Block.UPDATE_ALL)) return;
            // A later native placement veto only rolls back our still-owned glimmer, never a callback replacement.
            boolean veto = ForgeEventFactory.onBlockPlace(owner, before, Direction.UP);
            if (veto && level.getBlockState(pos) == glimmer) before.restore(true, false);
        }
    }
}
