package thaumcraft.client.theory;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Original aid selection uses the aid's item/block, rather than translated text buttons. */
final class TheoryAidButton extends Button {
    private static final ResourceLocation BASE = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/gui/gui_base.png");
    private final ItemStack icon;
    boolean selected;

    TheoryAidButton(int x, int y, ItemStack icon, Component name, OnPress press) {
        super(x, y, 16, 16, name, press, DEFAULT_NARRATION);
        this.icon = icon.copy();
    }
    @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        if (selected || active && isHoveredOrFocused()) {
            graphics.setColor(1, 1, 1, selected ? 1 : .2F);
            graphics.blit(BASE, getX(), getY(), 16, 16, 0, 96, 16, 16, 256, 256);
            graphics.setColor(1, 1, 1, 1);
        }
        if(!icon.isEmpty())graphics.renderItem(icon, getX(), getY());
    }
}
