package thaumcraft.world.warp.client;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.client.extensions.common.IClientMobEffectExtensions;
import thaumcraft.world.warp.WarpAffliction;
public final class WarpEffectIcons implements IClientMobEffectExtensions {
    private static final ResourceLocation ATLAS=ResourceLocation.fromNamespaceAndPath("thaumcraft","textures/misc/potions.png");
    // Forge calls initializeClient from the MobEffect superclass constructor.
    // Resolve subclass state only when rendering, after construction is complete.
    private final java.util.function.Supplier<WarpAffliction.Kind> kind;
    public WarpEffectIcons(java.util.function.Supplier<WarpAffliction.Kind> kind){this.kind=kind;}
    private int x(){return switch(kind.get()){case HUNGER,THAUMARHIA->7;case SUN->6;case GAZE->4;case BLUR->5;};}
    private int y(){return kind.get()==WarpAffliction.Kind.HUNGER?1:2;}
    @Override public boolean renderInventoryIcon(MobEffectInstance effect,EffectRenderingInventoryScreen<?> screen,GuiGraphics graphics,int left,int top,int z){
        graphics.blit(ATLAS,left+6,top+7,x()*18,198+y()*18,18,18);return true;
    }
    @Override public boolean renderGuiIcon(MobEffectInstance effect,Gui gui,GuiGraphics graphics,int left,int top,float z,float alpha){
        graphics.setColor(1,1,1,alpha);graphics.blit(ATLAS,left+3,top+3,x()*18,198+y()*18,18,18);graphics.setColor(1,1,1,1);return true;
    }
}
