package thaumcraft.artifice.construct;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;
import thaumcraft.catalog.CatalogItem;
import thaumcraft.catalog.CatalogModule;

/** BETA26's three construct variants and two-clear-block placement. */
public final class TurretPlacerItem extends CatalogItem {
    private static final Set<UUID> PLACING = new HashSet<>();
    public TurretPlacerItem(CatalogModule.Spec spec) { super(spec); }
    @Override public InteractionResult onItemUseFirst(ItemStack held, UseOnContext context) {
        var player = context.getPlayer(); var level = context.getLevel(); var clicked = context.getClickedPos();
        if (player == null || !player.isAlive() || player.isSpectator() || held.isEmpty() || held.getItem() != this
                || held != player.getItemInHand(context.getHand()) || context.getClickedFace() == Direction.DOWN || !level.hasChunkAt(clicked)) return InteractionResult.PASS;
        BlockPos pos = level.getBlockState(clicked).canBeReplaced() ? clicked : clicked.relative(context.getClickedFace());
        if (!level.hasChunkAt(pos) || !level.hasChunkAt(pos.above()) || level.isOutsideBuildHeight(pos) || level.isOutsideBuildHeight(pos.above())
                || !level.getWorldBorder().isWithinBounds(pos) || !level.getWorldBorder().isWithinBounds(pos.above())
                || player.distanceToSqr(pos.getX() + .5, pos.getY(), pos.getZ() + .5) > 36
                || !player.mayUseItemAt(pos, context.getClickedFace(), held) || !level.mayInteract(player, pos) || !level.mayInteract(player, pos.above())
                || !level.getBlockState(pos).canBeReplaced() || !level.getBlockState(pos.above()).canBeReplaced()
                || !level.getEntities(null, new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 2, pos.getZ() + 1)).isEmpty()) return InteractionResult.PASS;
        if (!(level instanceof ServerLevel server)) return InteractionResult.PASS;
        if (!server.getServer().isSameThread() || !PLACING.add(player.getUUID())) return InteractionResult.FAIL;
        try {
            String entityId = switch (spec().id()) {
                case "turret_basic" -> "turret_basic";
                case "turret_advanced" -> "turret_advanced";
                case "turret_bore" -> "arcane_bore";
                default -> null;
            };
            if (entityId == null) return InteractionResult.FAIL;
            var type = ForgeRegistries.ENTITY_TYPES.getValue(ResourceLocation.fromNamespaceAndPath("thaumcraft", entityId));
            if (type == null || !(type.create(level) instanceof OwnedConstructEntity turret)) return InteractionResult.FAIL;
            ItemStack before = held.copy();
            turret.moveTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5, 0, 0);
            turret.setOwnerId(player.getUUID()); turret.setValidSpawn();
            if (turret instanceof BoreEntity bore) bore.setFacing(player.getDirection());
            // Both replaceable blocks are intentionally overwritten, including
            // snow layers; the checked 1 x 2 volume fully contains all three variants.
            if (!level.addFreshEntity(turret)) return InteractionResult.FAIL;
            if (!turret.isAlive() || turret.isRemoved() || held != player.getItemInHand(context.getHand())
                    || !ItemStack.matches(before, held)
                    || !level.getBlockState(pos).canBeReplaced() || !level.getBlockState(pos.above()).canBeReplaced()) { turret.discard(); return InteractionResult.FAIL; }
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3); level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 3);
            // The original placer shrinks even a creative player's held stack.
            held.shrink(1);
            level.playSound(null, turret.getX(), turret.getY(), turret.getZ(), SoundEvents.ARMOR_STAND_PLACE, SoundSource.BLOCKS, .75F, .8F);
            return InteractionResult.CONSUME;
        } finally { PLACING.remove(player.getUUID()); }
    }
    @Override public InteractionResult useOn(UseOnContext context) { return onItemUseFirst(context.getItemInHand(), context); }
}
