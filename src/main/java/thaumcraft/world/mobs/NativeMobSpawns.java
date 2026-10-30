package thaumcraft.world.mobs;
import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.*;
import net.minecraftforge.common.world.*;

/** Modern biome tags replace1.12 BiomeManager/Dictionary while retaining source weights and dark-spawn rolls. */
public final class NativeMobSpawns implements BiomeModifier{
    public static final TagKey<Biome> MAGICAL=TagKey.create(Registries.BIOME,MobsModule.id("pech_magical_biomes"));
    public static <T extends Mob> boolean brainyRules(EntityType<T> type,ServerLevelAccessor level,MobSpawnType reason,net.minecraft.core.BlockPos pos,RandomSource random){
        if(level.getDifficulty()==Difficulty.PEACEFUL||level.getBrightness(LightLayer.SKY,pos)>random.nextInt(32))return false;
        int light=level.getLevel().isThundering()?level.getMaxLocalRawBrightness(pos,10):level.getMaxLocalRawBrightness(pos);
        return light<=random.nextInt(8)&&Mob.checkMobSpawnRules(type,level,reason,pos,random);
    }
    public static <T extends Mob> boolean firebatRules(EntityType<T> type,ServerLevelAccessor level,MobSpawnType reason,net.minecraft.core.BlockPos pos,RandomSource random){
        return level.getDifficulty()!=Difficulty.PEACEFUL&&level.getMaxLocalRawBrightness(pos)<=random.nextInt(7)&&Mob.checkMobSpawnRules(type,level,reason,pos,random);
    }
    public static <T extends Mob> boolean pechRules(EntityType<T> type,ServerLevelAccessor level,MobSpawnType reason,net.minecraft.core.BlockPos pos,RandomSource random){
        var biome=level.getBiome(pos);boolean magical=biome.is(MAGICAL);
        if(!level.getLevel().dimension().equals(Level.OVERWORLD)&&!biome.is(MobsModule.id("magical_forest"))&&!biome.is(MobsModule.id("eerie")))magical=false;
        return magical&&level.getLevel().getEntitiesOfClass(PechEntity.class,new net.minecraft.world.phys.AABB(pos).inflate(16)).size()<4&&brainyRules(type,level,reason,pos,random);
    }
    @Override public void modify(Holder<Biome> biome,Phase phase,ModifiableBiomeInfo.BiomeInfo.Builder builder){
        if(phase!=Phase.ADD)return;
        var spawns=builder.getMobSpawnSettings();
        if(MobsModule.SPAWN_BRAINY.get()&&biome.is(MobsModule.id("eerie"))){
            spawns.addSpawn(MobCategory.MONSTER,new MobSpawnSettings.SpawnerData(MobsModule.BRAINY_ZOMBIE.get(),32,1,1));
            spawns.addSpawn(MobCategory.MONSTER,new MobSpawnSettings.SpawnerData(MobsModule.GIANT_BRAINY_ZOMBIE.get(),8,1,1));
        }else if(MobsModule.SPAWN_BRAINY.get()&&biome.is(BiomeTags.IS_OVERWORLD)&&!spawns.getSpawner(MobCategory.MONSTER).isEmpty())
            spawns.addSpawn(MobCategory.MONSTER,new MobSpawnSettings.SpawnerData(MobsModule.BRAINY_ZOMBIE.get(),10,1,1));
        if(MobsModule.SPAWN_PECH.get()){
            var pech=thaumcraft.catalog.entities.VisualEntitiesModule.LIVING.get("pech").get();
            if(biome.is(MobsModule.id("magical_forest")))spawns.addSpawn(MobCategory.MONSTER,new MobSpawnSettings.SpawnerData(pech,20,1,2));
            if(biome.is(MAGICAL)&&!spawns.getSpawner(MobCategory.MONSTER).isEmpty())spawns.addSpawn(MobCategory.MONSTER,new MobSpawnSettings.SpawnerData(pech,10,1,1));
        }
        if(MobsModule.SPAWN_FIREBAT.get()){
            var bat=thaumcraft.catalog.entities.VisualEntitiesModule.LIVING.get("fire_bat").get();
            if(biome.is(BiomeTags.IS_NETHER))spawns.addSpawn(MobCategory.MONSTER,new MobSpawnSettings.SpawnerData(bat,10,1,2));
            var today=java.time.LocalDate.now();
            if(today.getMonthValue()==10&&today.getDayOfMonth()==31&&biome.is(BiomeTags.IS_OVERWORLD)&&!spawns.getSpawner(MobCategory.MONSTER).isEmpty())
                spawns.addSpawn(MobCategory.MONSTER,new MobSpawnSettings.SpawnerData(bat,5,1,2));
        }
    }
    @Override public Codec<? extends BiomeModifier> codec(){return MobsModule.SPAWN_CODEC.get();}
}
