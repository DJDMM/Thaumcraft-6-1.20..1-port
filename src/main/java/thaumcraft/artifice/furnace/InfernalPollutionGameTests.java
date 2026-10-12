package thaumcraft.artifice.furnace;

import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;
import thaumcraft.world.aura.AuraManager;

/** Original byte/count transport and real committed Flux/dimension/radius rules. */
@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public final class InfernalPollutionGameTests {
    private InfernalPollutionGameTests(){}
    @GameTest(template="empty") public static void originalSignedByteSmokeCountAndTypedDimensionCodecPreservePhysicalCoordinates(GameTestHelper h){
        var mutable=new BlockPos.MutableBlockPos(-182,92,430);var packet=new InfernalPollutionPacket(Level.NETHER.location(),mutable,InfernalPollutionPacket.originalAmount(.1f));mutable.set(0,0,0);
        var buffer=new FriendlyByteBuf(Unpooled.buffer());try{InfernalPollutionPacket.encode(packet,buffer);var decoded=InfernalPollutionPacket.decode(buffer);h.assertTrue(decoded.equals(packet)&&decoded.pos().equals(new BlockPos(-182,92,430))&&decoded.particleCount()==1&&!buffer.isReadable(),"Typed packet lost original x/y/z/byte or copied mutable source");}finally{buffer.release();}
        h.assertTrue(new InfernalPollutionPacket(Level.OVERWORLD.location(),BlockPos.ZERO,(byte)41).particleCount()==40&&new InfernalPollutionPacket(Level.OVERWORLD.location(),BlockPos.ZERO,InfernalPollutionPacket.originalAmount(128)).particleCount()==0&&InfernalPollutionPacket.originalAmount(0)==0,"Original signed byte/cap40/sub1positive conversion changed");h.succeed();
    }
    @GameTest(template="empty") public static void actualPaidFluxOneUsesOnlySameDimensionPlayersInsideStrictIntegerRadiusThirtyTwo(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(2,1,2));var remote=level.getServer().getLevel(Level.NETHER);h.assertTrue(remote!=null&&level.hasChunkAt(pos),"Native loaded source/Nether dimension fixture unavailable");
        float previous=AuraManager.getFlux(level,pos);var same=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"infernal_fx_near"));var other=new FakePlayer(remote,new GameProfile(UUID.randomUUID(),"infernal_fx_other"));
        try{same.setPos(pos.getX()+31.999,pos.getY(),pos.getZ());other.setPos(same.position());h.assertTrue(InfernalPollutionPacket.recipient(level,pos,same)&&!InfernalPollutionPacket.recipient(level,pos,other),"Original same-dimension radius routing changed");same.setPos(pos.getX()+32,pos.getY(),pos.getZ());h.assertTrue(!InfernalPollutionPacket.recipient(level,pos,same),"Exact32radius erroneously received original ALLAROUNDPOINT");same.setPos(pos.getX()+32.001,pos.getY(),pos.getZ());h.assertTrue(!InfernalPollutionPacket.recipient(level,pos,same),"Outside32radius received pollution smoke");
            InfernalPollutionPacket.pollute(level,pos,1);h.assertTrue(AuraManager.getFlux(level,pos)==previous+1,"Smoke publication didn't commit exact1Flux");InfernalPollutionPacket.pollute(level,pos,0);InfernalPollutionPacket.pollute(level,pos,-1);InfernalPollutionPacket.pollute(level,pos,Float.NaN);h.assertTrue(AuraManager.getFlux(level,pos)==previous+1,"Invalid publication changed actual paid Flux");
        }finally{AuraManager.drainFlux(level,pos,Float.MAX_VALUE,false);AuraManager.addFlux(level,pos,previous);}h.succeed();
    }
}
