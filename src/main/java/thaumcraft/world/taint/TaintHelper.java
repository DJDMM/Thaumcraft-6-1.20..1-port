package thaumcraft.world.taint;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.registries.ForgeRegistries;
import thaumcraft.catalog.entities.VisualEntitiesModule;
import thaumcraft.world.aura.AuraManager;
import thaumcraft.world.rift.RiftModule;

/** BETA26's seed-centred spread. Loaded-chunk checks replace the old force-loading reads. */
public final class TaintHelper {
    private static final Map<ServerLevel,LinkedHashSet<BlockPos>> SEEDS=new WeakHashMap<>();
    private TaintHelper() {}
    public static void addTaintSeed(ServerLevel level,BlockPos pos) {
        if (level.getServer().isSameThread()) SEEDS.computeIfAbsent(level,l->new LinkedHashSet<>()).add(pos.immutable());
    }
    public static void removeTaintSeed(ServerLevel level,BlockPos pos) {
        var seeds=SEEDS.get(level); if(seeds!=null) seeds.remove(pos);
    }
    private static boolean seed(Entity entity) {
        var id=ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        return entity.isAlive() && id!=null && id.getNamespace().equals("thaumcraft")
                && (id.getPath().equals("taint_seed")||id.getPath().equals("taint_seed_prime"));
    }
    public static boolean isNearTaintSeed(ServerLevel level,BlockPos pos) {
        var seeds=SEEDS.get(level); if(seeds==null || !level.getServer().isSameThread()) return false;
        double area=(double)TaintModule.spreadArea()*TaintModule.spreadArea();
        for(var p:java.util.List.copyOf(seeds)) if(p.distSqr(pos)<=area) {
            if(!TaintModule.loaded(level,p)) continue;
            if(level.getEntities((Entity)null,new AABB(p.getX()-.5,p.getY()-.5,p.getZ()-.5,p.getX()+1.5,p.getY()+1.5,p.getZ()+1.5),TaintHelper::seed).isEmpty()) {
                removeTaintSeed(level,p); return false;
            }
            return true;
        }
        return false;
    }
    public static boolean isAtTaintSeedEdge(ServerLevel level,BlockPos pos) {
        var seeds=SEEDS.get(level); if(seeds==null) return false;
        double area=(double)TaintModule.spreadArea()*TaintModule.spreadArea(),fringe=area*.8*.8;
        for(var p:seeds) if(TaintModule.loaded(level,p)) { double d=p.distSqr(pos); if(d<area&&d>fringe) return true; }
        return false;
    }
    public static boolean isTainted(BlockState state) {
        var id=ForgeRegistries.BLOCKS.getKey(state.getBlock());
        return id!=null&&id.getNamespace().equals("thaumcraft")
                &&(TaintModule.handlesBlock(id.getPath())||id.getPath().equals("flux_goo"));
    }
    public static void spreadFibres(ServerLevel level,BlockPos pos) { spreadFibres(level,pos,false); }
    public static void spreadFibres(ServerLevel level,BlockPos pos,boolean ignore) {
        if(!level.getServer().isSameThread()||!TaintModule.loaded(level,pos)||(!ignore&&RiftModule.WUSS_MODE.get())) return;
        int base=AuraManager.getAuraBase(level,pos);
        float saturation=AuraManager.getFlux(level,pos)/base;
        if(!ignore&&level.random.nextFloat()>TaintModule.spreadRate()/100F*(.001F+saturation*2F)) return;
        if(!isNearTaintSeed(level,pos)) return;
        BlockPos target=pos.offset(level.random.nextInt(3)-1,level.random.nextInt(3)-1,level.random.nextInt(3)-1);
        if(target.equals(pos)||!TaintModule.loaded(level,target)||!allNeighborsLoaded(level,target)) return;
        var state=level.getBlockState(target);
        float hardness=state.getDestroySpeed(level,target);
        if(hardness<0||hardness>10) return;
        boolean leaves=state.is(BlockTags.LEAVES),liquid=!state.getFluidState().isEmpty();
        if(!leaves&&!liquid&&(state.isAir()||state.canBeReplaced()||state.getBlock() instanceof FlowerBlock||state.getBlock() instanceof IPlantable)
                &&adjacentToSolid(level,target)&&!TaintFibreBlock.isOnlyAdjacentToTaint(level,target)) {
            placePaid(level,target,TaintModule.block("taint_fibre").defaultBlockState()); return;
        }
        if(leaves) {
            Direction touching=null;
            if(level.random.nextFloat()<.6F) for(Direction dir:Direction.values()) if(level.getBlockState(target.relative(dir)).is(TaintModule.block("taint_log"))) { touching=dir;break; }
            if(touching!=null) level.setBlockAndUpdate(target,TaintModule.block("taint_feature").defaultBlockState().setValue(TaintFeatureBlock.FACING,touching.getOpposite()));
            else placePaid(level,target,TaintModule.block("taint_fibre").defaultBlockState());
            return;
        }
        if(TaintFibreBlock.isHemmedByTaint(level,target)&&hardness<5) {
            if(state.is(BlockTags.LOGS)&&!isTainted(state)) {
                var axis=state.hasProperty(BlockStateProperties.AXIS)?state.getValue(BlockStateProperties.AXIS):Direction.Axis.Y;
                level.setBlockAndUpdate(target,TaintModule.block("taint_log").defaultBlockState().setValue(TaintLogBlock.AXIS,axis)); return;
            }
            String converted=conversion(state);
            if(converted!=null) { placePaid(level,target,TaintModule.block(converted).defaultBlockState()); return; }
        }
        if((state.is(TaintModule.block("taint_soil"))||state.is(TaintModule.block("taint_rock")))&&TaintModule.loaded(level,target.above())
                &&level.isEmptyBlock(target.above())&&AuraManager.getFlux(level,target)>=5
                &&level.random.nextFloat()<TaintModule.spreadRate()/100F*.33&&isAtTaintSeedEdge(level,target)) {
            Entity candidate=VisualEntitiesModule.LIVING.get("taint_seed").get().create(level);
            if(candidate==null) return;
            candidate.moveTo(target.getX()+.5,target.getY()+1,target.getZ()+.5,level.random.nextInt(360),0);
            if(candidate instanceof Mob mob && mob.checkSpawnRules(level,net.minecraft.world.entity.MobSpawnType.NATURAL)&&mob.checkSpawnObstruction(level)) {
                AuraManager.drainFlux(level,target,5,false);
                if(!level.addFreshEntity(candidate)) AuraManager.addFlux(level,target,5);
            }
        }
    }
    private static String conversion(BlockState state) {
        if(isTainted(state)) return null;
        Block block=state.getBlock();
        // 1.20 removed Material: native tags, legacy classes and the rock-support tag retain its tested categories.
        if(state.is(BlockTags.PLANKS)||state.is(BlockTags.WOODEN_FENCES)||state.is(BlockTags.WOODEN_STAIRS)||state.is(BlockTags.WOODEN_SLABS)
                ||state.is(BlockTags.WOODEN_DOORS)||state.is(BlockTags.WOODEN_TRAPDOORS)||state.is(BlockTags.WOODEN_BUTTONS)||state.is(BlockTags.WOODEN_PRESSURE_PLATES)
                ||block instanceof HugeMushroomBlock||block instanceof CactusBlock||block instanceof CoralBlock||block instanceof CoralWallFanBlock||block instanceof CoralFanBlock
                ||block instanceof SpongeBlock||block instanceof WetSpongeBlock||block==Blocks.PUMPKIN||block==Blocks.CARVED_PUMPKIN||block==Blocks.JACK_O_LANTERN||block==Blocks.MELON
                ||block instanceof ChestBlock||block instanceof BarrelBlock||block==Blocks.CRAFTING_TABLE||block instanceof SignBlock||block instanceof BedBlock||block==Blocks.BOOKSHELF||block==Blocks.CHISELED_BOOKSHELF) return "taint_crust";
        if(state.is(BlockTags.DIRT)||state.is(BlockTags.SAND)||block==Blocks.CLAY||block instanceof FarmBlock||block==Blocks.GRAVEL||block==Blocks.DIRT_PATH||block==Blocks.SOUL_SAND||block==Blocks.SOUL_SOIL) return "taint_soil";
        if(state.is(thaumcraft.world.crystal.PrimalCrystalClusterBlock.ROCK_SUPPORT)) return "taint_rock";
        return null;
    }
    private static boolean allNeighborsLoaded(ServerLevel level,BlockPos pos) {
        for(Direction dir:Direction.values()) if(!TaintModule.loaded(level,pos.relative(dir))) return false; return true;
    }
    private static boolean adjacentToSolid(ServerLevel level,BlockPos pos) {
        for(Direction dir:Direction.values()) if(level.getBlockState(pos.relative(dir)).isFaceSturdy(level,pos.relative(dir),dir.getOpposite())) return true; return false;
    }
    private static void placePaid(ServerLevel level,BlockPos pos,BlockState state) {
        if(level.setBlockAndUpdate(pos,state)) { level.blockEvent(pos,state.getBlock(),1,0);AuraManager.drainFlux(level,pos,.01F,false); }
    }
}
