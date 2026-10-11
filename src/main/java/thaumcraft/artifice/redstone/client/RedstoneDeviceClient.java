package thaumcraft.artifice.redstone.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thaumcraft.artifice.redstone.ArcaneEarBlockEntity;
import thaumcraft.artifice.redstone.RedstoneRelayBlock;
import thaumcraft.artifice.redstone.RedstoneRelayBlockEntity;
import thaumcraft.equipment.armor.GogglesArmorSupport;

/** Original physical dial labels; the revealing goggles also show an ear/note block's pitch. */
@Mod.EventBusSubscriber(modid = "thaumcraft", value = Dist.CLIENT)
public final class RedstoneDeviceClient {
    private static final MultiBufferSource.BufferSource BUFFERS = MultiBufferSource.immediate(new BufferBuilder(512));
    private RedstoneDeviceClient() {}
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || mc.player == null || mc.level == null
                || mc.options.hideGui || mc.screen != null || mc.isPaused() || !mc.player.isAlive() || mc.player.isSpectator()
                || !(mc.hitResult instanceof BlockHitResult hit)) return;
        var pos = hit.getBlockPos(); var state = mc.level.getBlockState(pos); var tile = mc.level.getBlockEntity(pos);
        Component text = null; double height = .8;
        if (tile instanceof RedstoneRelayBlockEntity relay) {
            int knob = RedstoneRelayBlock.selectedKnob(state, hit.getLocation().subtract(Vec3.atLowerCornerOf(pos)));
            if (knob == 0) text = Component.translatableWithFallback("thaumcraft.device.output", "Out: %s", relay.getOut());
            if (knob == 1) text = Component.translatableWithFallback("thaumcraft.device.input", "In: %s", relay.getIn());
        } else if (GogglesArmorSupport.hasGoggles(mc.player)) {
            int note = tile instanceof ArcaneEarBlockEntity ear ? ear.note : state.is(Blocks.NOTE_BLOCK) ? state.getValue(NoteBlock.NOTE) : -1;
            if (note >= 0) { text = Component.translatableWithFallback("thaumcraft.device.note", "Note: %s", note); height = 1.5; }
        }
        if (text == null) return;
        Vec3 origin = Vec3.atLowerCornerOf(pos).add(.5, height, .5); Vec3 camera = event.getCamera().getPosition();
        float yaw = (float) Math.toDegrees(Math.atan2(camera.x - origin.x, camera.z - origin.z));
        var pose = event.getPoseStack(); pose.pushPose();
        pose.translate(origin.x - camera.x, origin.y - camera.y, origin.z - camera.z);
        pose.mulPose(Axis.YP.rotationDegrees(yaw)); pose.scale(-.0125F, -.0125F, .0125F);
        mc.font.drawInBatch(text, 1 - mc.font.width(text) / 2F, 1, 0xFFFFFFFF, true, pose.last().pose(), BUFFERS,
                Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
        pose.popPose(); BUFFERS.endBatch();
    }
}
