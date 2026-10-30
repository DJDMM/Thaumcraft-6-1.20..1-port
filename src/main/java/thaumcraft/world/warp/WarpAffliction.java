package thaumcraft.world.warp;

import java.util.List;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.resources.ResourceLocation;

/** Released potion tick periods; event instances separately own their curative lists. */
public final class WarpAffliction extends MobEffect {
    public enum Kind { THAUMARHIA, HUNGER, SUN, GAZE, BLUR }
    private final Kind kind;
    public WarpAffliction(Kind kind) { super(MobEffectCategory.HARMFUL, 0x5A3473); this.kind = kind; }
    public Kind kind() { return kind; }
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientMobEffectExtensions> consumer) {
        consumer.accept(new thaumcraft.world.warp.client.WarpEffectIcons(this::kind));
    }
    @Override public String getDescriptionId() {
        return switch (kind) {
            case THAUMARHIA -> "potion.thaumarhia"; case HUNGER -> "potion.unhunger";
            case SUN -> "potion.sunscorned"; case GAZE -> "potion.deathgaze"; case BLUR -> "potion.blurred";
        };
    }
    @Override public boolean isDurationEffectTick(int duration, int amplifier) {
        return switch (kind) { case THAUMARHIA -> duration % 20 == 0; case SUN -> duration % 40 == 0; case HUNGER -> true; default -> false; };
    }
    @Override public void applyEffectTick(LivingEntity target, int amplifier) {
        if (target.level().isClientSide || !target.isAlive()) return;
        switch (kind) {
            case HUNGER -> { if (target instanceof Player player) player.causeFoodExhaustion(.025F * (amplifier + 1)); }
            case THAUMARHIA -> {
                if (target.getRandom().nextInt(15) == 0 && target.level().isEmptyBlock(target.blockPosition())) {
                    var goo = ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("thaumcraft", "flux_goo"));
                    if (goo != null && goo != Blocks.AIR) target.level().setBlockAndUpdate(target.blockPosition(), goo.defaultBlockState());
                }
            }
            case SUN -> {
                int light = target.level().getMaxLocalRawBrightness(target.blockPosition());
                float ambient = target.level().dimensionType().ambientLight();
                float brightness = (light / (60F - light * 3F)) * (1 - ambient) + ambient;
                if (brightness > .5F && target.getRandom().nextFloat() * 30 < (brightness - .4F) * 2
                        && target.level().canSeeSky(target.blockPosition())) target.setSecondsOnFire(4);
                else if (brightness < .25F && target.getRandom().nextFloat() > brightness * 2) target.heal(1);
            }
            default -> {}
        }
    }
}
