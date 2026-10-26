package thaumcraft.auromancy.table.client;

import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Original GuiFocusSettingSpinnerButton sprites and two arrow-only click regions. */
final class FocusSettingSpinner extends Button {
    private static final ResourceLocation BASE=ResourceLocation.fromNamespaceAndPath("thaumcraft","textures/gui/gui_base.png");
    private final int span;
    private final Supplier<Component> text;
    private final java.util.function.IntConsumer changed;
    FocusSettingSpinner(int x,int y,int span,Component name,Supplier<Component> text,java.util.function.IntConsumer changed) {
        super(x,y,span+10,10,name,b->{},DEFAULT_NARRATION);
        this.span=span;this.text=text;this.changed=changed;
    }
    @Override public void onClick(double x,double y) { changed.accept(x<getX()+10?-1:1); }
    @Override public boolean mouseClicked(double x,double y,int button) {
        return active&&visible&&(x<getX()+10||x>=getX()+span)&&super.mouseClicked(x,y,button);
    }
    @Override protected void renderWidget(GuiGraphics graphics,int mouseX,int mouseY,float partial) {
        float c=active?(isHoveredOrFocused()?1:.9F):.5F;
        graphics.setColor(c,c,c,active?(isHoveredOrFocused()?1:.9F):.9F);
        graphics.blit(BASE,getX(),getY(),20,0,10,10,256,256);
        graphics.blit(BASE,getX()+span,getY(),30,0,10,10,256,256);
        graphics.setColor(1,1,1,1);
        graphics.drawCenteredString(Minecraft.getInstance().font,text.get(),getX()+(span+10)/2,getY()+1,0xFFFFFF);
    }
}
