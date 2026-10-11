package thaumcraft.world.taint;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Monster rest semantics for the operational entities whose shared renderer base is PathfinderMob. */
@Mod.EventBusSubscriber(modid="thaumcraft")
public final class TaintRestEvents {
    private TaintRestEvents() {}
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void sleep(PlayerSleepInBedEvent event) {
        if (event.getResultStatus()!=null || !(event.getEntity() instanceof ServerPlayer player)
                || player.isCreative() || player.isSleeping() || !player.isAlive()
                || !player.level().dimensionType().natural() || event.getOptionalPos().isEmpty()) return;
        var server=player.serverLevel();BlockPos pos=event.getOptionalPos().get();
        if (!TaintModule.loaded(server,pos)) return;
        var state=server.getBlockState(pos);
        if (!(state.getBlock() instanceof BedBlock) || !state.hasProperty(HorizontalDirectionalBlock.FACING)) return;
        Direction facing=state.getValue(HorizontalDirectionalBlock.FACING);BlockPos foot=pos.relative(facing.getOpposite());
        if (!reachable(player,pos) && !reachable(player,foot)) return;
        if (!TaintModule.loaded(server,pos.above()) || !TaintModule.loaded(server,foot.above())
                || server.getBlockState(pos.above()).isSuffocating(server,pos.above())
                || server.getBlockState(foot.above()).isSuffocating(server,foot.above())) return;
        Vec3 centre=Vec3.atBottomCenterOf(pos);
        AABB range=new AABB(centre,centre).inflate(8,5,8);
        if (server.getEntitiesOfClass(TaintedStationaryMob.class,range,
                mob->!mob.isRemoved() && mob.isPreventingPlayerRest(player)).isEmpty()) return;
        // ServerPlayer performs these same actions after reach/obstruction and before
        // Monster.class safety. Emit the ordinary Forge time hook exactly once even
        // when another mod allows daytime sleep; no new message or alternate result.
        player.setRespawnPosition(server.dimension(),pos,player.getYRot(),false,true);
        event.setResult(ForgeEventFactory.fireSleepingTimeCheck(player,event.getOptionalPos())
                ? Player.BedSleepingProblem.NOT_SAFE : Player.BedSleepingProblem.NOT_POSSIBLE_NOW);
    }
    private static boolean reachable(Player player,BlockPos pos) {
        Vec3 centre=Vec3.atBottomCenterOf(pos);
        return Math.abs(player.getX()-centre.x)<=3 && Math.abs(player.getY()-centre.y)<=2
                && Math.abs(player.getZ()-centre.z)<=3;
    }
}
