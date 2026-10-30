package thaumcraft.world.mobs.client;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import thaumcraft.world.mobs.*;

public final class PechScreen extends AbstractContainerScreen<PechMenu>{
    private static final ResourceLocation TEXTURE=MobsModule.id("textures/gui/gui_pech.png");
    public PechScreen(PechMenu menu,Inventory inventory,Component title){super(menu,inventory,title);imageWidth=175;imageHeight=232;}
    @Override protected void renderBg(GuiGraphics graphics,float partial,int mouseX,int mouseY){
        RenderSystem.enableBlend();graphics.blit(TEXTURE,leftPos,topPos,0,0,imageWidth,imageHeight);
        if(menu.canTrade())graphics.blit(TEXTURE,leftPos+67,topPos+24,176,0,25,25);RenderSystem.disableBlend();
    }
    @Override protected void renderLabels(GuiGraphics graphics,int x,int y){}
    @Override public void render(GuiGraphics graphics,int x,int y,float partial){renderBackground(graphics);super.render(graphics,x,y,partial);renderTooltip(graphics,x,y);}
    @Override public boolean mouseClicked(double x,double y,int button){
        boolean nativeResult=super.mouseClicked(x,y,button);
        if(x>=leftPos+67&&x<leftPos+92&&y>=topPos+24&&y<topPos+49&&menu.canTrade()&&minecraft!=null&&minecraft.gameMode!=null){
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId,0);
            var sound=net.minecraftforge.registries.ForgeRegistries.SOUND_EVENTS.getValue(MobsModule.id("pech_dice"));
            if(sound!=null&&minecraft.player!=null)minecraft.player.playSound(sound,.5F,.95F+minecraft.player.getRandom().nextFloat()*.1F);
            return true;
        }return nativeResult;
    }
}
