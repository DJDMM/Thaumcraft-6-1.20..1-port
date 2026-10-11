package thaumcraft.artifice.construct.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import thaumcraft.artifice.construct.TurretMenu;

/** Original texture, live ammunition slot, health strip and advanced target toggles. */
public final class TurretScreen extends AbstractContainerScreen<TurretMenu> {
    private static final ResourceLocation BASIC=ResourceLocation.fromNamespaceAndPath("thaumcraft","textures/gui/gui_turret_basic.png");
    private static final ResourceLocation ADVANCED=ResourceLocation.fromNamespaceAndPath("thaumcraft","textures/gui/gui_turret_advanced.png");
    public TurretScreen(TurretMenu menu,Inventory inventory,Component title) {
        super(menu,inventory,title);imageWidth=175;imageHeight=232;
    }
    @Override protected void renderLabels(GuiGraphics graphics,int x,int y) {}
    @Override protected void renderBg(GuiGraphics graphics,float partial,int mouseX,int mouseY) {
        var texture=menu.advanced()?ADVANCED:BASIC;
        graphics.blit(texture,leftPos,topPos,0,0,imageWidth,imageHeight);
        var turret=menu.turret();
        if(turret!=null) {
            int health=Mth.clamp((int)(39*turret.getHealth()/turret.getMaxHealth()),0,39);
            graphics.blit(texture,leftPos+(menu.advanced()?30:68),topPos+59,192,48,health,6);
        }
        if(menu.advanced())for(int action=1;action<=4;action++) {
            int y=topPos+13+(action-1)*14;
            graphics.blit(texture,leftPos+90,y,192,16,8,8);
            if((menu.targetFlags()&(1<<(action-1)))!=0)graphics.blit(texture,leftPos+90,y,192,24,8,8);
            graphics.drawString(font,Component.translatable("button.turretfocus."+action),leftPos+102,y,0xffffff,true);
        }
    }
    @Override public void render(GuiGraphics graphics,int x,int y,float partial) {
        renderBackground(graphics);super.render(graphics,x,y,partial);renderTooltip(graphics,x,y);
    }
    /** Same window-bound C2S menu button used by an ordinary mouse click. */
    public void toggleTarget(int action) {
        if(!menu.advanced()||action<1||action>4||minecraft==null||minecraft.gameMode==null)return;
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId,action);
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK,1));
    }
    @Override public boolean mouseClicked(double x,double y,int button) {
        if(button==0&&menu.advanced())for(int action=1;action<=4;action++) {
            int yy=13+(action-1)*14;
            if(isHovering(90,yy,8,8,x,y)) {
                toggleTarget(action);return true;
            }
        }
        return super.mouseClicked(x,y,button);
    }
}
