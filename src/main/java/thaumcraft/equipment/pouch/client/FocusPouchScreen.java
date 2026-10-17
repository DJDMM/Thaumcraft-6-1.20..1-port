package thaumcraft.equipment.pouch.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import thaumcraft.equipment.pouch.FocusPouchMenu;
import thaumcraft.equipment.pouch.FocusPouchModule;

public final class FocusPouchScreen extends AbstractContainerScreen<FocusPouchMenu> {
    private static final ResourceLocation TEXTURE=ResourceLocation.fromNamespaceAndPath("thaumcraft","textures/gui/gui_focuspouch.png");
    public FocusPouchScreen(FocusPouchMenu menu,Inventory inventory,Component name){super(menu,inventory,name);imageWidth=175;imageHeight=232;}
    @Override public void render(GuiGraphics graphics,int x,int y,float partial){renderBackground(graphics);super.render(graphics,x,y,partial);renderTooltip(graphics,x,y);}
    @Override protected void renderBg(GuiGraphics graphics,float partial,int x,int y){graphics.blit(TEXTURE,leftPos,topPos,0,0,imageWidth,imageHeight);}
    @Override protected void renderLabels(GuiGraphics graphics,int x,int y){int locked=menu.lockedHotbar();if(locked>=0){graphics.pose().pushPose();graphics.pose().translate(0,0,300);graphics.blit(TEXTURE,8+locked*18,209,240,0,16,16);graphics.pose().popPose();}}
    @Override protected boolean checkHotbarKeyPressed(int key,int scan){return false;}
    @Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {@SubscribeEvent public static void setup(FMLClientSetupEvent event){event.enqueueWork(()->MenuScreens.register(FocusPouchModule.MENU.get(),FocusPouchScreen::new));}}
}
