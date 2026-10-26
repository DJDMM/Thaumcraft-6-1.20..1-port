package thaumcraft.client.widgets;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** TC6 GuiImageButton's centered image, hover brightness and inactive tint. */
public final class OriginalImageButton extends Button {
    private final ResourceLocation texture;
    private final int u,v;
    public OriginalImageButton(int centerX,int centerY,int width,int height,ResourceLocation texture,int u,int v,Component name,OnPress press) {
        super(centerX-width/2,centerY-height/2,width,height,name,press,DEFAULT_NARRATION);
        this.texture=texture;this.u=u;this.v=v;
    }
    @Override protected void renderWidget(GuiGraphics graphics,int mouseX,int mouseY,float partial) {
        float shade=active?(isHoveredOrFocused()?1:.9F):.5F;
        graphics.setColor(shade,shade,shade,active?1:.9F);
        graphics.blit(texture,getX(),getY(),u,v,width,height,256,256);
        graphics.setColor(1,1,1,1);
    }
}
