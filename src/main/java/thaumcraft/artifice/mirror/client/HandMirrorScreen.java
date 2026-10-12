package thaumcraft.artifice.mirror.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import thaumcraft.artifice.mirror.HandMirrorMenu;

/** Original GuiHandMirror: one input, physical inventory and protected held slot. */
public final class HandMirrorScreen extends AbstractContainerScreen<HandMirrorMenu> {
    private static final ResourceLocation TEXTURE=ResourceLocation.fromNamespaceAndPath("thaumcraft","textures/gui/gui_handmirror.png");
    private final int selected;
    public HandMirrorScreen(HandMirrorMenu menu,Inventory inventory,Component title){super(menu,inventory,title);imageWidth=176;imageHeight=166;selected=menu.hand()==net.minecraft.world.InteractionHand.MAIN_HAND?inventory.selected:-1;}
    @Override protected void renderLabels(GuiGraphics graphics,int x,int y){}
    @Override protected boolean checkHotbarKeyPressed(int key,int scan){return false;}
    @Override protected void renderBg(GuiGraphics graphics,float partial,int x,int y){
        graphics.blit(TEXTURE,leftPos,topPos,0,0,imageWidth,imageHeight);
        if(selected>=0){graphics.pose().pushPose();graphics.pose().translate(0,0,300);
            graphics.blit(TEXTURE,leftPos+8+selected*18,topPos+142,240,0,16,16);graphics.pose().popPose();}
    }
    @Override public void render(GuiGraphics graphics,int x,int y,float partial){renderBackground(graphics);super.render(graphics,x,y,partial);renderTooltip(graphics,x,y);}
}
