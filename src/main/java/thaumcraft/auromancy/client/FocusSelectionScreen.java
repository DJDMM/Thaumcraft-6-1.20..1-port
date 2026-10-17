package thaumcraft.auromancy.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import thaumcraft.auromancy.FocusSelection;
import thaumcraft.auromancy.FocusSelectionNetwork;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Original hold-F radial HUD, with a non-pausing Screen as the modern cursor bridge. */
public final class FocusSelectionScreen extends Screen {
    private static final ResourceLocation R1=ResourceLocation.fromNamespaceAndPath("thaumcraft","textures/misc/radial.png");
    private static final ResourceLocation R2=ResourceLocation.fromNamespaceAndPath("thaumcraft","textures/misc/radial2.png");
    private final InteractionHand hand;
    private ItemStack caster=ItemStack.EMPTY;
    private final List<FocusSelection.Choice> choices=new ArrayList<>();
    private float[] hoverScale=new float[0];
    private int hovered=-1;
    private float scale;
    private long lastFrame;
    private boolean closing,sent;
    public FocusSelectionScreen(InteractionHand hand){super(Component.empty());this.hand=hand;}
    @Override protected void init(){
        choices.clear();caster=minecraft.player.getItemInHand(hand).copy();choices.addAll(FocusSelection.choices(minecraft.player));
        choices.sort(Comparator.comparing(c->c.focus().getHoverName().getString()+sortingHash(c.focus())));
        hoverScale=new float[choices.size()];java.util.Arrays.fill(hoverScale,1);lastFrame=System.nanoTime();
    }
    @Override public boolean isPauseScreen(){return false;}
    @Override public void tick(){if(minecraft.player==null||!ItemStack.matches(caster,minecraft.player.getItemInHand(hand)))closing=true;}
    public int choiceCount(){return choices.size();}
    private static int sortingHash(ItemStack focus){int cached=focus.hasTag()?focus.getTag().getInt("srt"):0;return cached!=0?cached:thaumcraft.auromancy.focus.FocusStacks.readPlan(focus).orElseThrow().sortingHash();}
    private double radius(){return 16+choices.size()*2.5;}
    private double angle(int i){return Math.toRadians(-90*scale+360.0*i/Math.max(1,choices.size()));}
    public int choiceX(int index){return width/2+(int)(Math.cos(angle(index))*radius()*scale);}
    public int choiceY(int index){return height/2+(int)(Math.sin(angle(index))*radius()*scale);}
    private void hover(double x,double y){hovered=-1;if(closing)return;for(int i=0;i<choices.size();i++)if(Math.abs(x-choiceX(i))<=10&&Math.abs(y-choiceY(i))<=10){hovered=i;break;}}
    private void ring(GuiGraphics graphics,ResourceLocation texture,float degrees,double size){
        graphics.pose().pushPose();graphics.pose().translate(width/2.,height/2.,0);graphics.pose().mulPose(Axis.ZP.rotationDegrees(degrees+90));
        graphics.pose().scale((float)(size/256),(float)(size/256),1);graphics.blit(texture,-128,-128,0,0,256,256);graphics.pose().popPose();
    }
    @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partial){
        long now=System.nanoTime();float elapsed=Math.min(100,(now-lastFrame)/1_000_000F);lastFrame=now;
        scale=Math.max(0,Math.min(1,scale+(closing?-1:1)*elapsed/150));
        if(closing&&scale<=0){minecraft.setScreen(null);return;}
        hover(mouseX,mouseY);if(choices.isEmpty())return;
        float rotation=partial+minecraft.player.tickCount%720/2F;
        RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();graphics.setColor(.5F,.5F,.5F,.5F);
        ring(graphics,R1,rotation,radius()*2.75*scale);ring(graphics,R2,-rotation,radius()*2.55*scale);graphics.setColor(1,1,1,1);
        ItemStack installed=FocusSelection.installed(minecraft.player.getItemInHand(hand)),tooltip=ItemStack.EMPTY;
        if(!installed.isEmpty()){graphics.renderItem(installed,width/2-8,height/2-8);if(Math.abs(mouseX-width/2)<=10&&Math.abs(mouseY-height/2)<=10)tooltip=installed;}
        for(int i=0;i<choices.size();i++){
            hoverScale[i]=i==hovered?Math.min(1.3F,hoverScale[i]+elapsed/150):Math.max(1,hoverScale[i]-elapsed/250);
            graphics.pose().pushPose();graphics.pose().translate(choiceX(i),choiceY(i),100);graphics.pose().scale(hoverScale[i]*scale,hoverScale[i]*scale,1);graphics.renderItem(choices.get(i).focus(),-8,-8);graphics.pose().popPose();
        }
        if(hovered>=0)tooltip=choices.get(hovered).focus();if(!tooltip.isEmpty())graphics.renderTooltip(font,tooltip,-4,20);
        RenderSystem.disableBlend();
    }
    private void choose(){if(sent||closing)return;sent=true;if(hovered>=0){var choice=choices.get(hovered);FocusSelectionNetwork.request(hand,choice.source(),caster,choice.pouch(),choice.focus());}closing=true;}
    @Override public boolean mouseClicked(double x,double y,int button){if(button!=0)return false;hover(x,y);if(hovered>=0)choose();return true;}
    @Override public boolean mouseReleased(double x,double y,int button){var key=FocusSelectionClient.CHANGE.getKey();if(key.getType()==com.mojang.blaze3d.platform.InputConstants.Type.MOUSE&&key.getValue()==button){hover(x,y);choose();return true;}return super.mouseReleased(x,y,button);}
    @Override public boolean keyReleased(int key,int scan,int modifiers){if(FocusSelectionClient.CHANGE.matches(key,scan)){choose();return true;}return super.keyReleased(key,scan,modifiers);}
    @Override public void onClose(){closing=true;}
}
