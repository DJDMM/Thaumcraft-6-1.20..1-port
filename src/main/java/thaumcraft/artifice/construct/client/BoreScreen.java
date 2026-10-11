package thaumcraft.artifice.construct.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.Blocks;
import thaumcraft.artifice.construct.BoreMenu;

/** Released GuiArcaneBore: live pickaxe, health, broken-tool overlay and original properties. */
public final class BoreScreen extends AbstractContainerScreen<BoreMenu> {
    private static final ResourceLocation TEXTURE=ResourceLocation.fromNamespaceAndPath("thaumcraft","textures/gui/gui_arcanebore.png");
    public BoreScreen(BoreMenu menu,Inventory inventory,Component title){super(menu,inventory,title);imageWidth=175;imageHeight=232;}
    @Override protected void renderLabels(GuiGraphics graphics,int x,int y){}
    @Override protected void renderBg(GuiGraphics graphics,float partial,int x,int y) {
        graphics.blit(TEXTURE,leftPos,topPos,0,0,imageWidth,imageHeight);
        var bore=menu.bore();if(bore==null)return;
        int health=Mth.clamp((int)(39*bore.getHealth()/bore.getMaxHealth()),0,39);
        graphics.blit(TEXTURE,leftPos+68,topPos+59,192,48,health,6);
        var tool=bore.tool();
        if(!tool.isEmpty()&&tool.getDamageValue()+1>=tool.getMaxDamage())graphics.blit(TEXTURE,leftPos+80,topPos+29,240,0,16,16);
        graphics.pose().pushPose();graphics.pose().translate(leftPos+124,topPos+18,505);graphics.pose().scale(.5F,.5F,1);
        graphics.drawString(font,"Width: "+(1+bore.getDigRadius()*2),0,0,0xffffff,true);
        graphics.drawString(font,"Depth: "+bore.getDigDepth(),64,0,0xffffff,true);
        graphics.drawString(font,"Speed: +"+bore.getDigSpeed(Blocks.STONE.defaultBlockState()),0,10,0xffffff,true);
        int row=34,refining=bore.getRefining(),fortune=bore.getFortune();
        if(bore.hasSilkTouch()||refining>0||fortune>0)graphics.drawString(font,"Other properties:",0,24,0xffffff,true);
        if(refining>0){graphics.drawString(font,"Refining "+refining,4,row,12632256,true);row+=9;}
        if(fortune>0){graphics.drawString(font,"Fortune "+fortune,4,row,15648330,true);row+=9;}
        if(bore.hasSilkTouch())graphics.drawString(font,"Silk Touch",4,row,8421631,true);
        graphics.pose().popPose();
    }
    @Override public void render(GuiGraphics graphics,int x,int y,float partial){renderBackground(graphics);super.render(graphics,x,y,partial);renderTooltip(graphics,x,y);}
}
