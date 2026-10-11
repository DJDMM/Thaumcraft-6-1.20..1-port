package thaumcraft.world.taint;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import thaumcraft.catalog.blocks.CatalogBlocks;
import thaumcraft.world.rift.RiftModule;

/** Original BETA26 tainted terrain, under its existing catalogue registry IDs. */
public final class TaintModule {
    private static final DeferredRegister<SoundEvent> SOUNDS=DeferredRegister.create(ForgeRegistries.SOUND_EVENTS,"thaumcraft");
    public static final RegistryObject<SoundEvent> GORE=SOUNDS.register("gore",()->SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft","gore")));
    public static final SoundType SOUND=new SoundType(.5F,1F,
            SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft","gore")),
            SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft","gore")),
            SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft","gore")),
            SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft","gore")),
            SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft","gore")));
    private TaintModule() {}
    public static void register(IEventBus bus) { SOUNDS.register(bus); }
    public static int spreadArea() { return RiftModule.TAINT_SPREAD_AREA.get(); }
    public static float spreadRate() { return RiftModule.TAINT_SPREAD_RATE.get().floatValue(); }
    public static boolean handlesBlock(String id) { return java.util.Set.of("taint_fibre","taint_soil","taint_rock","taint_crust","taint_log","taint_feature","taint_geyser").contains(id); }
    public static Block createBlock(String id) {
        return switch(id) {
            case "taint_fibre" -> new TaintFibreBlock();
            case "taint_log" -> new TaintLogBlock();
            case "taint_feature" -> new TaintFeatureBlock();
            case "taint_soil","taint_rock","taint_crust","taint_geyser" -> new TaintBlock(id);
            default -> throw new IllegalArgumentException(id);
        };
    }
    public static Block block(String id) { return CatalogBlocks.block(id); }
    static boolean spawnFallingTaint(net.minecraft.server.level.ServerLevel level,BlockPos original,BlockPos target,net.minecraft.world.level.block.state.BlockState state) {
        var type=thaumcraft.catalog.entities.VisualEntitiesModule.EFFECTS.get("falling_taint");
        if(type==null) return false;
        var entity=type.get().create(level);
        if(!(entity instanceof FallingTaint falling)) return false;
        entity.moveTo(target.getX()+.5,target.getY()+.5,target.getZ()+.5,0,0);
        falling.initializeFallingTaint(state,original);
        return level.addFreshEntity(entity);
    }
    static net.minecraft.world.entity.Mob activeMob(String id,net.minecraft.server.level.ServerLevel level) {
        var type=thaumcraft.catalog.entities.VisualEntitiesModule.LIVING.get(id);
        if(type==null) return null;
        var mob=type.get().create(level);
        return mob!=null&&mob.getClass()!=thaumcraft.catalog.entities.VisualMobEntity.class?mob:null;
    }
    public static boolean loaded(net.minecraft.world.level.BlockGetter level,BlockPos pos) {
        return !(level instanceof net.minecraft.world.level.LevelReader reader) || (!reader.isOutsideBuildHeight(pos)&&reader.hasChunkAt(pos));
    }
}
