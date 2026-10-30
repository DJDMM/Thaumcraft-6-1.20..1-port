package thaumcraft.world.voidsiphon.client;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import thaumcraft.world.voidsiphon.VoidSiphonMenu;
/** Released176x166 panel; the original did not draw a numeric progress label. */
public final class VoidSiphonScreen extends AbstractContainerScreen<VoidSiphonMenu> {
    private static final ResourceLocation TEXTURE=ResourceLocation.fromNamespaceAndPath("thaumcraft","textures/gui/gui_void_siphon.png");
    public VoidSiphonScreen(VoidSiphonMenu menu,Inventory inv,Component title){super(menu,inv,title);imageWidth=176;imageHeight=166;}
    @Override public void render(GuiGraphics g,int x,int y,float partial){renderBackground(g);super.render(g,x,y,partial);renderTooltip(g,x,y);}
    @Override protected void renderBg(GuiGraphics g,float partial,int x,int y){g.blit(TEXTURE,leftPos,topPos,0,0,imageWidth,imageHeight);}
    @Override protected void renderLabels(GuiGraphics g,int x,int y){}
}
