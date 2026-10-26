package thaumcraft.client.theory;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** The original BETA26 image button, with text fitted to the translated label. */
final class TheoryButton extends Button {
    private static final ResourceLocation BASE = new ResourceLocation("thaumcraft", "textures/gui/gui_base.png");
    int tint=0x88FFAA;

    TheoryButton(int x, int y, int width, int height, Component message, OnPress pressed) {
        super(x, y, width, height, message, pressed, DEFAULT_NARRATION);
    }

    @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Offered papers are layered at depth 10..12. Keep the controls above
        // that cosmetic layer; otherwise visible/clickable choices have no label.
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 30);
        float shade=active?(isHoveredOrFocused()?1:.9F):.5F;
        graphics.setColor((tint>>16&255)/255F*shade,(tint>>8&255)/255F*shade,(tint&255)/255F*shade,active?1:.9F);
        graphics.blit(BASE, getX(), getY(), getWidth(), getHeight(), 37F, 66F, 51, 13, 256, 256);
        graphics.setColor(1, 1, 1, 1);
        var font = Minecraft.getInstance().font;
        float scale = .5F;
        graphics.pose().pushPose();
        graphics.pose().translate(getX() + getWidth() / 2F, getY() + (getHeight() - font.lineHeight * scale) / 2F, 1);
        graphics.pose().scale(scale, scale, 1);
        graphics.drawString(font, getMessage(), -font.width(getMessage()) / 2, 0, active?(isHoveredOrFocused()?0xFFFFA0:0xFFFFFF):0xA0A0A0, true);
        graphics.pose().popPose();
        graphics.pose().popPose();
    }
}
