package thaumcraft.world.crimson;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.util.RandomSource;
import thaumcraft.catalog.entities.VisualEntitiesModule;

/** One original surface-generator decision per new overworld chunk, never a chunk-load retrofit. */
public final class CrimsonSurfaceFeature extends Feature<NoneFeatureConfiguration> {
    public static final TagKey<net.minecraft.world.level.block.Block> PORTAL_GROUND =
            TagKey.create(Registries.BLOCK, CrimsonModule.id("crimson_portal_ground"));
    public CrimsonSurfaceFeature() { super(NoneFeatureConfiguration.CODEC); }
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if (!CrimsonModule.GENERATE_STRUCTURES.get() || !context.level().getLevel().dimension().equals(Level.OVERWORLD)
                || context.chunkGenerator() instanceof FlatLevelSource) return false;
        int x = context.origin().getX() + 8 + context.random().nextInt(9) - 4;
        int z = context.origin().getZ() + 8 + context.random().nextInt(9) - 4;
        int y = context.level().getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 9;
        return generate(context.level(), context.random(), new BlockPos(x, y, z));
    }
    public static boolean generate(WorldGenLevel level, RandomSource random, BlockPos origin) {
        if (origin.getY() >= level.getMaxBuildHeight()) return false;
        if (random.nextInt(100) == 0) return CrimsonMound.generate(level, random, origin);
        if (random.nextInt(500) != 0) return false;
        BlockPos ground = origin.above(8);
        if (!level.hasChunk(ground.getX() >> 4, ground.getZ() >> 4)
                || !level.getBlockState(ground).is(PORTAL_GROUND)) return false;
        var holder = VisualEntitiesModule.LIVING.get("cultist_portal_lesser");
        if (holder == null || !(holder.get().create(level.getLevel()) instanceof LesserCultistPortalEntity portal)) return false;
        portal.setPos(ground.getX() + .5, ground.getY() + 1, ground.getZ() + .5);
        portal.finalizeSpawn(level, level.getCurrentDifficultyAt(portal.blockPosition()), MobSpawnType.STRUCTURE, null, null);
        // WorldGenRegion queues the real entity in its protochunk; do not mutate ServerLevel from its generation worker.
        return level.addFreshEntity(portal);
    }
}
