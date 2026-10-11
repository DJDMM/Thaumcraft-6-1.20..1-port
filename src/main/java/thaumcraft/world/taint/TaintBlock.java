package thaumcraft.world.taint;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.*;
import thaumcraft.alchemy.AspectCrystalItem;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.infusion.InfusionEffects;
import thaumcraft.world.aura.AuraManager;

/** BETA26 tainted soil, stone, crust and geyser, retaining their distinct tick branches. */
public final class TaintBlock extends Block implements ITaintBlock {
    private final String kind;
    public TaintBlock(String kind) {
        super(Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(10,100).sound(TaintModule.SOUND).randomTicks());
        this.kind=kind;
    }
    public String kind() { return kind; }
    @Override public VoxelShape getBlockSupportShape(BlockState state,BlockGetter level,BlockPos pos) { return Shapes.empty(); }
    @Override public void die(ServerLevel level,BlockPos pos,BlockState state) {
        Block replacement=switch(kind) {
            case "taint_rock" -> TaintModule.block("stone_porous");
            case "taint_soil" -> Blocks.DIRT;
            case "taint_crust","taint_geyser" -> TaintModule.block("flux_goo");
            default -> Blocks.AIR;
        };
        level.setBlockAndUpdate(pos,replacement.defaultBlockState());
    }
    @Override public void randomTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random) {
        if(!level.getServer().isSameThread()||!TaintModule.loaded(level,pos)||!level.getBlockState(pos).equals(state)) return;
        if(!TaintHelper.isNearTaintSeed(level,pos)&&random.nextInt(10)==0) { die(level,pos,state);return; }
        if(kind.equals("taint_rock")) TaintHelper.spreadFibres(level,pos);
        if(kind.equals("taint_crust")) {
            if(tryToFall(level,pos,pos)) return;
            if(TaintModule.loaded(level,pos.above())&&level.isEmptyBlock(pos.above())) {
                Direction dir=new Direction[]{Direction.SOUTH,Direction.WEST,Direction.NORTH,Direction.EAST}[random.nextInt(4)];
                boolean slide=true;
                for(int depth=1;depth<4;depth++) {
                    BlockPos side=pos.relative(dir).below(depth),under=pos.below(depth);
                    if(!TaintModule.loaded(level,side)||!TaintModule.loaded(level,under)||!level.isEmptyBlock(side)||!level.getBlockState(under).is(this)) { slide=false;break; }
                }
                if(slide) tryToFall(level,pos,pos.relative(dir));
            }
        } else if(kind.equals("taint_geyser")) {
            boolean spawn=level.random.nextFloat()<.2&&level.hasNearbyAlivePlayer(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,32)
                    &&level.getEntities((Entity)null,new net.minecraft.world.phys.AABB(pos.getX()+.5-32,pos.getY()+.5-32,pos.getZ()+.5-32,pos.getX()+.5+32,pos.getY()+.5+32,pos.getZ()+.5+32),
                        e->net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(e.getType()).equals(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("thaumcraft","taint_swarm"))).isEmpty();
            if(spawn) {
                var swarm=TaintModule.activeMob("taint_swarm",level);
                if(swarm!=null) { swarm.moveTo(pos.getX()+.5,pos.getY()+1.25,pos.getZ()+.5,level.random.nextInt(360),0);level.addFreshEntity(swarm); }
            } else if(AuraManager.getFlux(level,pos)<2) AuraManager.addFlux(level,pos,.25F);
        }
    }
    public static boolean canFallBelow(LevelReader level,BlockPos pos) {
        if(!TaintModule.loaded(level,pos)) return false;
        for(BlockPos other:BlockPos.betweenClosed(pos.offset(-1,-1,-1),pos.offset(1,1,1))) {
            if(!TaintModule.loaded(level,other)) return false;
            var state=level.getBlockState(other);
            if(state.is(BlockTags.LOGS)||state.is(TaintModule.block("taint_log"))) return false;
        }
        var state=level.getBlockState(pos);
        boolean goo=state.is(TaintModule.block("flux_goo"));
        if(goo) {
            var property=state.getBlock().getStateDefinition().getProperty("level");
            if(property!=null&&Integer.parseInt(String.valueOf(state.getValue(property)))>=4) return false;
        }
        return state.isAir()||state.is(Blocks.FIRE)||state.is(TaintModule.block("taint_fibre"))||state.canBeReplaced()||!state.getFluidState().isEmpty()||goo;
    }
    public boolean tryToFall(ServerLevel level,BlockPos source,BlockPos target) {
        if(!TaintFibreBlock.isOnlyAdjacentToTaint(level,source)||target.getY()<level.getMinBuildHeight()||!canFallBelow(level,target.below())) return false;
        // No catalogue effect is substituted for the original moving block entity.
        return TaintModule.spawnFallingTaint(level,source,target,level.getBlockState(source));
    }
    static void contact(Level level,Entity entity,int chance) {
        if(!level.isClientSide&&entity instanceof LivingEntity living&&living.getMobType()!=MobType.UNDEAD&&level.random.nextInt(chance)==0)
            living.addEffect(new MobEffectInstance(InfusionEffects.FLUX_TAINT.get(),200,0,false,true));
    }
    @Override public void stepOn(Level level,BlockPos pos,BlockState state,Entity entity) { contact(level,entity,250);super.stepOn(level,pos,state,entity); }
    @Override public boolean triggerEvent(BlockState state,Level level,BlockPos pos,int id,int parameter) {
        if(id==1) {
            if(level.isClientSide) level.playLocalSound(pos,SoundEvents.CHORUS_FLOWER_DEATH,SoundSource.BLOCKS,.1F,.9F+level.random.nextFloat()*.2F,false);
            return true;
        }
        return super.triggerEvent(state,level,pos,id,parameter);
    }
    @Override public List<ItemStack> getDrops(BlockState state,LootParams.Builder builder) {
        ItemStack tool=builder.getOptionalParameter(LootContextParams.TOOL);
        if(tool!=null&&EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH,tool)>0) return List.of(new ItemStack(this));
        if(kind.equals("taint_rock")) {
            int fortune=tool==null?0:EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLOCK_FORTUNE,tool);
            if(builder.getLevel().random.nextInt(15)+fortune>13) return List.of(AspectCrystalItem.create(Aspect.FLUX));
        }
        return List.of();
    }
}
