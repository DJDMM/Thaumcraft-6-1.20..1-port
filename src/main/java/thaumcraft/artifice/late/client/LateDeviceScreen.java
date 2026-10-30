package thaumcraft.artifice.late.client;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import thaumcraft.artifice.late.*;
/** Released textures, slot positions, mix icons, fluid strip, and ten aspect bars. */
public final class LateDeviceScreen extends AbstractContainerScreen<LateDeviceMenu> {
    private final boolean spa;private final ResourceLocation texture;
    public LateDeviceScreen(LateDeviceMenu menu,Inventory inv,Component title){super(menu,inv,title);spa=menu.tile() instanceof SpaBlockEntity;imageWidth=spa?176:192;imageHeight=spa?166:233;texture=ResourceLocation.fromNamespaceAndPath("thaumcraft","textures/gui/"+(spa?"gui_spa.png":"gui_potion_sprayer.png"));}
    @Override protected void renderLabels(GuiGraphics g,int x,int y){}
    @Override public void render(GuiGraphics g,int x,int y,float partial){renderBackground(g);super.render(g,x,y,partial);renderTooltip(g,x,y);
        if(spa&&menu.tile() instanceof SpaBlockEntity tile){if(isHovering(104,10,10,55,x,y)&&!tile.tank.isEmpty())g.renderComponentTooltip(font,List.of(tile.tank.getFluid().getDisplayName(),Component.literal(tile.tank.getFluidAmount()+" mb")),x,y);if(isHovering(88,34,10,10,x,y))g.renderTooltip(font,Component.translatable("text.spa.mix."+tile.mix()),x,y);}
    }
    @Override protected void renderBg(GuiGraphics g,float partial,int x,int y){
        g.blit(texture,leftPos,topPos,0,0,imageWidth,imageHeight);
        if(spa&&menu.tile() instanceof SpaBlockEntity tile){g.blit(texture,leftPos+89,topPos+35,208,tile.mix()?16:32,8,8);
            if(!tile.tank.isEmpty()){var fs=tile.tank.getFluid();var client=IClientFluidTypeExtensions.of(fs.getFluid());var icon=minecraft.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(client.getStillTexture(fs));int c=client.getTintColor(fs);g.setColor((c>>16&255)/255F,(c>>8&255)/255F,(c&255)/255F,1);for(int a=0;a<6;a++)g.blit(leftPos+107,topPos+15+a*8,0,8,8,icon);g.setColor(1,1,1,1);g.blit(texture,leftPos+107,topPos+15,107,15,10,(int)(48-48*tile.tank.getFluidAmount()/5000F));}
            g.blit(texture,leftPos+106,topPos+11,232,0,10,55);
        }else if(menu.tile() instanceof PotionSprayerBlockEntity tile){int c=tile.color();if(tile.charges()>0){g.setColor((c>>16&255)/255F,(c>>8&255)/255F,(c&255)/255F,1);g.blit(texture,leftPos+128,topPos+36+(8-tile.charges())*9,232,minecraft.player.tickCount%256,8,tile.charges()*9);g.setColor(1,1,1,1);}
            int index=0;var recipe=tile.recipe();var progress=tile.progress();for(var a:recipe.getAspectsSortedByName()){int column=index%2,row=index/2,px=leftPos+96+22*column,py=topPos+46+16*row;g.setColor(.2F,.2F,.2F,1);g.blit(texture,px,py-14,192,56,2,14);int filled=(int)(progress.getAmount(a)/(float)recipe.getAmount(a)*14);c=a.getColor();g.setColor((c>>16&255)/255F,(c>>8&255)/255F,(c&255)/255F,1);g.blit(texture,px,py-filled,192,56,2,filled);g.setColor(1,1,1,1);int ix=leftPos+79+22*column,iy=topPos+31+16*row;g.blit(a.getImage(),ix,iy,0,0,16,16,16,16);g.pose().pushPose();g.pose().translate(ix+14,iy+10,0);g.pose().scale(.5F,.5F,1);g.drawString(font,Integer.toString(recipe.getAmount(a)),0,0,0xFFFFFF,true);g.pose().popPose();index++;}
            g.blit(texture,leftPos+125,topPos+28,205,28,14,88);
        }
    }
    @Override public boolean mouseClicked(double x,double y,int button){boolean handled=super.mouseClicked(x,y,button);if(spa&&isHovering(89,35,8,8,x,y)){minecraft.gameMode.handleInventoryButtonClick(menu.containerId,1);var sound=net.minecraftforge.registries.ForgeRegistries.SOUND_EVENTS.getValue(ResourceLocation.fromNamespaceAndPath("thaumcraft","clack"));if(sound!=null)minecraft.player.playSound(sound,.4F,1);return true;}return handled;}
}
