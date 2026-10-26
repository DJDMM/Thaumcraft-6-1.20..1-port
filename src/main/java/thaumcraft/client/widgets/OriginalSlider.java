package thaumcraft.client.widgets;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** GuiSliderTC track/8px knob and its pointer-to-value formula. */
public final class OriginalSlider extends AbstractWidget {
    private static final ResourceLocation BASE=ResourceLocation.fromNamespaceAndPath("thaumcraft","textures/gui/gui_base.png");
    private final boolean vertical;
    private final DoubleSupplier min,max,value;
    private final DoubleConsumer changed;
    public OriginalSlider(int x,int y,int width,int height,boolean vertical,DoubleSupplier min,DoubleSupplier max,DoubleSupplier value,DoubleConsumer changed) {
        super(x,y,width,height,Component.translatable("logistics.scrollbar"));
        this.vertical=vertical;this.min=min;this.max=max;this.value=value;this.changed=changed;
    }
    private float position() { double range=max.getAsDouble()-min.getAsDouble();return range<=0?0:Mth.clamp((float)((value.getAsDouble()-min.getAsDouble())/range),0,1); }
    private void pointer(double x,double y) {
        float p=Mth.clamp((float)((vertical?y-(getY()+4):x-(getX()+4))/(vertical?height-8:width-8)),0,1);
        changed.accept(min.getAsDouble()+(max.getAsDouble()-min.getAsDouble())*p);
    }
    @Override public void onClick(double x,double y) { pointer(x,y); }
    @Override protected void onDrag(double x,double y,double dx,double dy) { pointer(x,y); }
    @Override public boolean keyPressed(int key,int scan,int modifiers) {
        if(key==262||key==264||key==263||key==265) {
            changed.accept(Mth.clamp(value.getAsDouble()+((key==262||key==264)?1:-1),min.getAsDouble(),max.getAsDouble()));return true;
        }
        return super.keyPressed(key,scan,modifiers);
    }
    @Override protected void renderWidget(GuiGraphics graphics,int mouseX,int mouseY,float partial) {
        graphics.setColor(1,1,1,1);
        if(vertical) {
            graphics.blit(BASE,getX()+2,getY(),4,height,240,176,4,32,256,256);
            graphics.blit(BASE,getX(),getY()+(int)(position()*(height-8)),20,20,8,8,256,256);
        } else {
            graphics.blit(BASE,getX(),getY()+2,width,4,208,176,32,4,256,256);
            graphics.blit(BASE,getX()+(int)(position()*(width-8)),getY(),20,20,8,8,256,256);
        }
    }
    @Override protected void updateWidgetNarration(NarrationElementOutput output) { output.add(NarratedElementType.TITLE,getMessage()); }
}
