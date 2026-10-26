package thaumcraft.fx;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.*;
import thaumcraft.alchemy.AlchemyModule;
import thaumcraft.catalog.blocks.*;
import thaumcraft.fx.nitor.*;

/** Native registry/block-entity/serialization checks; visible pixels require the separate client run. */
@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public final class FxCoreGameTests {
    private FxCoreGameTests(){}
    @GameTest(template="empty") public static void baseAndAllSixteenColoredNitorsCreateTheirFunctionalTileAndOriginalShape(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(1,2,1));var previous=level.getBlockState(pos);
        try{
            var blocks=new java.util.ArrayList<Block>();blocks.add(AlchemyModule.NITOR.get());CatalogBlocks.ENTRIES.forEach((id,b)->{if(id.startsWith("nitor_"))blocks.add(b.get());});
            h.assertTrue(blocks.size()==17,"Base alias plus original sixteen Nitor colors missing");
            for(var block:blocks){
                h.assertTrue(block instanceof NitorBlock,"Nitor remains a decorative catalogue block");var state=block.defaultBlockState();level.setBlockAndUpdate(pos,state);
                h.assertTrue(level.getBlockEntity(pos) instanceof NitorBlockEntity&&level.getBlockEntity(pos).getType()==AlchemyModule.NITOR_TILE.get(),"Actual placement failed to create native Nitor tile");
                h.assertTrue(state.getRenderShape()==RenderShape.INVISIBLE&&state.getCollisionShape(level,pos).isEmpty()&&state.getLightEmission(level,pos)==15,"Nitor is visible mesh/colliding/unlit");
                var bounds=state.getShape(level,pos).bounds();h.assertTrue(Math.abs(bounds.minX-.33000001311302185)<1.e-7&&Math.abs(bounds.maxY-.6600000262260437)<1.e-7,"Original Nitor selection bounds changed");
                var saved=level.getBlockEntity(pos).saveWithFullMetadata();var decoded=net.minecraft.world.level.block.entity.BlockEntity.loadStatic(pos,state,saved);
                h.assertTrue(decoded instanceof NitorBlockEntity&&decoded.getType()==AlchemyModule.NITOR_TILE.get(),"Native Nitor save/reload lost operational tile identity");
            }
        }finally{level.setBlockAndUpdate(pos,previous);}h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=30) public static void oldCatalogueNitorTileMigratesOnNativeWorldTick(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(1,2,1));var previous=level.getBlockState(pos);var state=CatalogBlocks.block("nitor_blue").defaultBlockState();
        level.setBlockAndUpdate(pos,state);var old=new CatalogBlockEntity(pos,state);level.setBlockEntity(old);
        h.startSequence().thenWaitUntil(()->h.assertTrue(level.getBlockEntity(pos) instanceof NitorBlockEntity,"Old visual tile has not become functional on its actual native tick"))
                .thenExecute(()->{h.assertTrue(level.getBlockState(pos)==state,"Migration replaced the physical colored block");level.setBlockAndUpdate(pos,previous);}).thenSucceed();
    }
    @GameTest(template="empty") public static void visualPacketRoundTripRetainsDimensionTargetsPhysicalItemAndFullOriginalParticleProgram(GameTestHelper h){
        var stack=new ItemStack(Items.DIAMOND);stack.getOrCreateTag().putString("physical","qa");var spec=new CompoundTag();spec.putInt("start",123);spec.putInt("life",33);spec.putIntArray("finalFrames",new int[]{65,66,66});var alpha=new ListTag();alpha.add(FloatTag.valueOf(1));alpha.add(FloatTag.valueOf(.1F));spec.put("alpha",alpha);
        var original=new FxDispatcher.Message(ResourceLocation.fromNamespaceAndPath("minecraft","the_nether"),"infusionItem",new Vec3(-3.25,112.5,7),new Vec3(1,115,-8.125),0xAABBCC,.75F,2,17,stack,spec,6);
        var buffer=new FriendlyByteBuf(Unpooled.buffer());try{FxDispatcher.encode(original,buffer);var copy=FxDispatcher.decode(buffer);
            h.assertTrue(copy.dimension().equals(original.dimension())&&copy.kind().equals(original.kind())&&copy.origin().equals(original.origin())&&copy.vector().equals(original.vector())&&copy.color()==original.color()&&copy.scale()==original.scale()&&copy.data()==original.data()&&copy.target()==original.target()&&copy.delay()==6&&copy.spec().equals(spec)&&ItemStack.matches(copy.stack(),stack)&&buffer.readableBytes()==0,"S2C visual routing lost original payload or physical item texture identity");
            h.assertTrue(ItemStack.matches(stack,original.stack())&&stack.getCount()==1,"Encoding consumed a physical ingredient");
        }finally{buffer.release();}h.succeed();
    }
}
