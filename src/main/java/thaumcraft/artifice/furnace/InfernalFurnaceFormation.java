package thaumcraft.artifice.furnace;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.player.PlayerEvent;
import thaumcraft.catalog.blocks.CatalogBlocks;
import thaumcraft.research.KnowledgeStore;

/** BETA26 3x3x3 dust blueprint, with transactional permission/held-stack callbacks. */
public final class InfernalFurnaceFormation {
    public record Part(BlockPos pos,String id){}
    public record Layout(Direction facing,List<Part> parts){public BlockPos anchor(){return parts.stream().filter(p->p.id().equals("infernal_furnace")).findFirst().orElseThrow().pos();}}
    private static final Set<net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level>> FORMING=new HashSet<>();
    private static final Set<net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level>> DISMANTLING=new HashSet<>();
    private InfernalFurnaceFormation(){}
    public static List<Part> parts(BlockPos corner,Direction facing){
        List<Part> parts=new ArrayList<>();int turns=3-facing.get2DDataValue();
        for(int y=0;y<3;y++)for(int x=0;x<3;x++)for(int z=0;z<3;z++){
            if(y==0&&x==1&&z==1)continue;
            String id=x==1&&z==1&&y==1?"infernal_furnace":x==2&&z==1&&y==1?"opening":x%2==0&&z%2==0?"placeholder_brick":"placeholder_obsidian";
            int rx=x,rz=z;for(int t=0;t<turns;t++){int oldX=rx;rx=rz;rz=2-oldX;}parts.add(new Part(corner.offset(rx,2-y,rz),id));
        }
        return List.copyOf(parts);
    }
    private static boolean loaded(Level level,BlockPos pos){return !level.isOutsideBuildHeight(pos)&&level.hasChunkAt(pos)&&level.getWorldBorder().isWithinBounds(pos);}
    private static boolean matches(Level level,Part part){if(!loaded(level,part.pos()))return false;BlockState state=level.getBlockState(part.pos());return switch(part.id()){case "infernal_furnace"->state.is(Blocks.LAVA);case "opening"->state.is(Blocks.IRON_BARS);default->state.is(InfernalFurnacePlaceholderBlock.original(part.id()));};}
    public static Layout match(Level level,BlockPos clicked){for(int y=-3;y<=0;y++)for(int x=-3;x<=0;x++)for(int z=-3;z<=0;z++){BlockPos corner=clicked.offset(x,y,z);for(Direction facing:List.of(Direction.SOUTH,Direction.WEST,Direction.NORTH,Direction.EAST)){List<Part> parts=parts(corner,facing);if(parts.stream().allMatch(p->matches(level,p)))return new Layout(facing,parts);}}return null;}
    private static BlockState target(Part part,Direction facing){return part.id().equals("opening")?Blocks.AIR.defaultBlockState():part.id().equals("infernal_furnace")?CatalogBlocks.block(part.id()).defaultBlockState().setValue(InfernalFurnaceBlock.FACING,facing):CatalogBlocks.block(part.id()).defaultBlockState();}
    public static InteractionResult use(UseOnContext context){Level level=context.getLevel();Layout layout=match(level,context.getClickedPos());if(layout==null)return InteractionResult.PASS;if(level.isClientSide)return InteractionResult.SUCCESS;
        if(!(context.getPlayer() instanceof ServerPlayer player)||!player.mayBuild()||player.isSpectator()||!KnowledgeStore.get(player).isResearchKnown("INFERNALFURNACE"))return InteractionResult.FAIL;
        ItemStack physical=context.getItemInHand();int count=physical.getCount();CompoundTag tag=physical.getTag()==null?null:physical.getTag().copy();boolean creative=player.getAbilities().instabuild;
        if(!(physical.getItem() instanceof thaumcraft.alchemy.SalisMundusItem)||physical.isEmpty()||!FORMING.add(level.dimension()))return InteractionResult.FAIL;
        try{
            for(Part part:layout.parts())if(!level.mayInteract(player,part.pos())||!player.mayUseItemAt(part.pos(),context.getClickedFace(),physical))return InteractionResult.FAIL;
            List<BlockSnapshot> before=new ArrayList<>();for(Part part:layout.parts())before.add(BlockSnapshot.create(level.dimension(),level,part.pos()));
            // Part(Material.LAVA,target,true) sets isOpp, not applyPlayerFacing.
            Direction facing=layout.facing().getOpposite();
            Map<BlockPos,BlockState> installed=new LinkedHashMap<>();Map<BlockPos,net.minecraft.world.level.block.entity.BlockEntity> identities=new HashMap<>();
            boolean prior=level.captureBlockSnapshots,committed=false;int start=level.capturedBlockSnapshots.size();level.captureBlockSnapshots=true;
            try{
                for(Part part:layout.parts()){if(!matches(level,part))return InteractionResult.FAIL;BlockState state=target(part,facing);if(!level.setBlock(part.pos(),state,3))return InteractionResult.FAIL;installed.put(part.pos(),state);identities.put(part.pos(),level.getBlockEntity(part.pos()));}
                if(ForgeEventFactory.onMultiBlockPlace(player,before,context.getClickedFace()))return InteractionResult.FAIL;
                if(player.getItemInHand(context.getHand())!=physical||physical.getCount()!=count||!Objects.equals(tag,physical.getTag())||player.getAbilities().instabuild!=creative)return InteractionResult.FAIL;
                for(Part part:layout.parts())if(!level.getBlockState(part.pos()).equals(installed.get(part.pos()))||level.getBlockEntity(part.pos())!=identities.get(part.pos()))return InteractionResult.FAIL;
                if(!creative)physical.shrink(1);committed=true;level.captureBlockSnapshots=prior;
                if(!prior)for(BlockSnapshot snapshot:before){BlockState state=level.getBlockState(snapshot.getPos());state.onPlace(level,snapshot.getPos(),snapshot.getReplacedBlock(),false);level.markAndNotifyBlock(snapshot.getPos(),level.getChunkAt(snapshot.getPos()),snapshot.getReplacedBlock(),state,3,512);}
                level.playSound(null,layout.anchor(),net.minecraft.sounds.SoundEvents.ENCHANTMENT_TABLE_USE,net.minecraft.sounds.SoundSource.BLOCKS,.7f,1);
                MinecraftForge.EVENT_BUS.post(new PlayerEvent.ItemCraftedEvent(player,new ItemStack(CatalogBlocks.block("infernal_furnace")),new net.minecraft.world.SimpleContainer(1)));
                return InteractionResult.CONSUME;
            }finally{
                if(!committed){boolean restoring=level.restoringBlockSnapshots;level.restoringBlockSnapshots=true;try{for(int i=before.size()-1;i>=0;i--){BlockSnapshot snapshot=before.get(i);if(installed.containsKey(snapshot.getPos())&&level.getBlockState(snapshot.getPos()).equals(installed.get(snapshot.getPos()))&&level.getBlockEntity(snapshot.getPos())==identities.get(snapshot.getPos()))snapshot.restore(true,false);}}finally{level.restoringBlockSnapshots=restoring;}}
                if(level.capturedBlockSnapshots.size()>start)level.capturedBlockSnapshots.subList(start,level.capturedBlockSnapshots.size()).clear();level.captureBlockSnapshots=prior;
            }
        }finally{FORMING.remove(level.dimension());}
    }
    public static BlockPos findAnchor(Level level,BlockPos member){if(DISMANTLING.contains(level.dimension())||FORMING.contains(level.dimension()))return null;for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++){BlockPos pos=member.offset(x,y,z);if(loaded(level,pos)&&level.getBlockState(pos).is(CatalogBlocks.block("infernal_furnace")))return pos;}return null;}
    public static void destroy(Level level,BlockPos anchor,BlockState furnaceState,BlockPos broken){if(level.isClientSide||level.restoringBlockSnapshots||FORMING.contains(level.dimension())||!DISMANTLING.add(level.dimension()))return;try{
        for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++){BlockPos pos=anchor.offset(x,y,z);if(pos.equals(broken)||!loaded(level,pos))continue;var state=level.getBlockState(pos);if(state.is(CatalogBlocks.block("placeholder_brick")))level.setBlockAndUpdate(pos,Blocks.NETHER_BRICKS.defaultBlockState());else if(state.is(CatalogBlocks.block("placeholder_obsidian")))level.setBlockAndUpdate(pos,Blocks.OBSIDIAN.defaultBlockState());}
        BlockPos bars=anchor.relative(furnaceState.getValue(InfernalFurnaceBlock.FACING).getOpposite());if(loaded(level,bars)&&level.getBlockState(bars).isAir())level.setBlockAndUpdate(bars,Blocks.IRON_BARS.defaultBlockState());
        // Modern onRemove runs after replacement: restore original lava on an ordinary AIR/core break,
        // while retaining a deliberate external solid replacement instead of overwriting it.
        if(loaded(level,anchor)){var current=level.getBlockState(anchor);if(current.is(CatalogBlocks.block("infernal_furnace"))||anchor.equals(broken)&&current.isAir())level.setBlockAndUpdate(anchor,Blocks.LAVA.defaultBlockState());}
    }finally{DISMANTLING.remove(level.dimension());}}
}
