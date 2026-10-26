package thaumcraft.auromancy.table.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import thaumcraft.alchemy.AspectCrystalItem;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.auromancy.focus.*;
import thaumcraft.auromancy.table.*;
import thaumcraft.client.widgets.OriginalImageButton;
import thaumcraft.client.widgets.OriginalSlider;

import java.util.*;

/** TC6's metal frame, node sockets, original icons and one-focus inventory layout. */
public final class FocalManipulatorScreen extends AbstractContainerScreen<FocalManipulatorMenu> {
    private static final ResourceLocation FRAME = gui("gui_wandtable"), BACK = gui("gui_wandtable2"), INVENTORY = gui("gui_wandtable3"), BASE = gui("gui_base");
    private static final ResourceLocation MEDIUM = tex("textures/foci/_medium.png"), EFFECT = tex("textures/foci/_effect.png");
    private FocusGraph draft = new FocusGraph(List.of());
    private long seenRevision = -1;
    private int selected = 1, partsStart, scrollY,scrollX;
    private boolean dirty, changingName;
    private EditBox name;
    private Button confirm;
    private OriginalSlider partsSlider,verticalSlider,horizontalSlider;
    private final List<Button> settingsButtons = new ArrayList<>();

    public FocalManipulatorScreen(FocalManipulatorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title); imageWidth = 231; imageHeight = 231;
    }
    private static ResourceLocation gui(String name) { return tex("textures/gui/"+name+".png"); }
    private static ResourceLocation tex(String path) { return ResourceLocation.fromNamespaceAndPath("thaumcraft", path); }
    @Override protected void init() {
        super.init();
        name = new EditBox(font, leftPos+30, topPos+11, 170, 12, Component.translatable("wandtable.text6"));
        name.setMaxLength(50); name.setBordered(false); name.setTextColor(0xFFFFFF);
        name.setResponder(text -> { if (!changingName && !menu.busy()) markDirty(); });
        addRenderableWidget(name);
        confirm = addRenderableWidget(new OriginalImageButton(leftPos+232,topPos+7,24,16,BASE,232,240,
                Component.translatable("wandtable.text3"),button -> { if(!dirty&&!menu.pending())menu.start(); }));
        partsSlider=addRenderableWidget(new OriginalSlider(leftPos+51,topPos+30,8,149,true,()->0,()->Math.max(0,parts().size()-6),()->partsStart,value->partsStart=(int)Math.round(value)));
        verticalSlider=addRenderableWidget(new OriginalSlider(leftPos+203,topPos+32,8,156,true,()->0,this::maxScroll,()->scrollY,value->scrollY=(int)Math.round(value)));
        horizontalSlider=addRenderableWidget(new OriginalSlider(leftPos+64,topPos+195,132,8,false,this::minScrollX,this::maxScrollX,()->scrollX,value->scrollX=(int)Math.round(value)));
        refresh();
    }
    @Override protected void containerTick() {
        super.containerTick(); name.tick();
        if (!dirty && !menu.pending() && seenRevision != menu.revision()) refresh();
        boolean editing = menu.canUse() && !menu.busy() && !menu.pending() && !menu.focus().isEmpty();
        name.setEditable(editing);
        if (dirty && editing) { menu.edit(draft, name.getValue()); dirty = false; }
        var compiled = compile();
        confirm.active = editing && !dirty && !menu.pending() && compiled.success() && (menu.creative() || menu.experienceLevel() >= compiled.plan().xpLevels())
                && FocalManipulatorBlockEntity.crystalDebit(menu.playerInventory(), compiled.plan().crystals()) != null;
        settingsButtons.forEach(button -> button.active = editing);
    }
    private void refresh() {
        seenRevision = menu.revision(); draft = menu.graph();
        changingName = true; name.setValue(menu.focusName()); changingName = false;
        if (FocusStacks.isFocus(menu.focus()) && draft.nodes().isEmpty() && !menu.busy()) {
            draft = rootSocket(); selected = 1; markDirty();
        }
        scrollY = Math.max(0,Math.min(maxScroll(),scrollY));
        rebuildSettings();
    }
    private static FocusGraph rootSocket() {
        return new FocusGraph(List.of(new FocusGraph.Node(0,-1,List.of(1),0,0,FocusNodeRegistry.ROOT,Map.of()),
                new FocusGraph.Node(1,0,List.of(),0,1,"",Map.of())));
    }
    private void markDirty() {
        dirty = true;
        // A second input event can arrive before containerTick sends EDIT.
        // Never leave START active for the previous acknowledged server graph.
        if(confirm!=null)confirm.active=false;
    }
    private FocusCompiler.Result compile() { return FocusCompiler.compile(draft, menu.focus(), menu.knowledge()::isResearchCompleteStrict); }
    private FocusGraph.Node selectedNode() { return draft.nodes().stream().filter(node -> node.id() == selected).findFirst().orElse(null); }
    private List<FocusNodeRegistry.Definition> parts() {
        if (draft.nodes().isEmpty()) return List.of();
        var node = selectedNode(); if (node == null || node.id() == 0) return List.of();
        var parent = draft.nodes().stream().filter(current -> current.id() == node.parent()).findFirst().orElse(null);
        var supplies = parent == null ? null : FocusNodeRegistry.get(parent.key());
        if (supplies == null) return List.of();
        var existing=draft.nodes().stream().filter(n->n.id()!=node.id()).map(n->FocusNodeRegistry.get(n.key())).filter(Objects::nonNull).toList();
        boolean exclusiveMedium=existing.stream().anyMatch(d->d.type()==FocusNodeRegistry.Type.MEDIUM&&d.exclusive());
        boolean hasMedium=existing.stream().anyMatch(d->d.type()==FocusNodeRegistry.Type.MEDIUM&&!d.key().equals(FocusNodeRegistry.ROOT));
        return FocusNodeRegistry.all().stream().filter(part -> !part.key().equals(FocusNodeRegistry.ROOT)
                && menu.knowledge().isResearchCompleteStrict(part.research())
                && supplies.supplied().containsAll(part.requiredSupply())
                && (!part.exclusive()||existing.stream().noneMatch(d->d.key().equals(part.key())))
                && (part.type()!=FocusNodeRegistry.Type.MEDIUM||!exclusiveMedium&&(!part.exclusive()||!hasMedium))).toList();
    }
    private void install(FocusNodeRegistry.Definition part) {
        if(menu.busy()||menu.pending()||!part.runtimeSupported()||!menu.knowledge().isResearchCompleteStrict(part.research()))return;
        var node=selectedNode();if(node==null||node.id()==0||!parts().contains(part))return;
        List<FocusGraph.Node> nodes=withoutDescendants(node);
        int count=part.supplied().isEmpty()?0:FocusNodeRegistry.isSplit(part.key())?2:1;
        if(nodes.size()+count>FocusGraph.MAX_NODES)return;
        var children=new ArrayList<Integer>();var used=new HashSet<Integer>();nodes.forEach(n->used.add(n.id()));
        for(int i=0;i<count;i++){
            int id=0;while(used.contains(id))id++;used.add(id);children.add(id);
            nodes.add(new FocusGraph.Node(id,node.id(),List.of(),node.x()+ (count==2?i*2-1:0),node.y()+1,"",Map.of()));
        }
        for(int i=0;i<nodes.size();i++)if(nodes.get(i).id()==node.id())nodes.set(i,new FocusGraph.Node(node.id(),node.parent(),children,node.x(),node.y(),part.key(),part.defaultSettings()));
        draft=layout(new FocusGraph(nodes));selected=children.isEmpty()?node.id():children.get(0);
        scrollToSelection();partsStart=0;markDirty();rebuildSettings();
    }
    private void clearSelected(){
        if(menu.busy()||menu.pending())return;var node=selectedNode();if(node==null||node.id()==0)return;
        var nodes=withoutDescendants(node);
        for(int i=0;i<nodes.size();i++)if(nodes.get(i).id()==node.id())nodes.set(i,new FocusGraph.Node(node.id(),node.parent(),List.of(),node.x(),node.y(),"",Map.of()));
        draft=layout(new FocusGraph(nodes));scrollToSelection();markDirty();rebuildSettings();
    }
    /** Replace only the selected subtree; a completed sibling remains untouched. */
    private List<FocusGraph.Node> withoutDescendants(FocusGraph.Node node){
        Map<Integer,FocusGraph.Node> map=new HashMap<>();draft.nodes().forEach(n->map.put(n.id(),n));
        var removed=new HashSet<Integer>();var queue=new ArrayDeque<Integer>(node.children());
        while(!queue.isEmpty()){int id=queue.removeFirst();if(removed.add(id)&&map.containsKey(id))queue.addAll(map.get(id).children());}
        return new ArrayList<>(draft.nodes().stream().filter(n->!removed.contains(n.id())).toList());
    }
    private static FocusGraph layout(FocusGraph graph){
        Map<Integer,FocusGraph.Node> map=new LinkedHashMap<>();graph.nodes().forEach(n->map.put(n.id(),n));
        Map<Integer,int[]> positions=new HashMap<>();place(map,positions,0,0,new int[]{0});
        int rootX=positions.get(0)[0];
        return new FocusGraph(graph.nodes().stream().map(n->{int[] xy=positions.get(n.id());return new FocusGraph.Node(n.id(),n.parent(),n.children(),xy[0]-rootX,xy[1],n.key(),n.settings());}).toList());
    }
    private static int place(Map<Integer,FocusGraph.Node> map,Map<Integer,int[]> positions,int id,int depth,int[] cursor){
        var n=map.get(id);int x;
        if(n.children().isEmpty()){x=cursor[0];cursor[0]+=2;}
        else{int sum=0;for(int child:n.children())sum+=place(map,positions,child,depth+1,cursor);x=sum/n.children().size();}
        positions.put(id,new int[]{x,depth});return x;
    }
    private int maxScroll() { return Math.max(0,(draft.nodes().stream().mapToInt(FocusGraph.Node::y).max().orElse(0)-3)*32); }
    private int minScrollX() { return draft.nodes().stream().mapToInt(FocusGraph.Node::x).min().orElse(0)*24; }
    private int maxScrollX() { return draft.nodes().stream().mapToInt(FocusGraph.Node::x).max().orElse(0)*24; }
    private void scrollToSelection() {
        var node = selectedNode();if(node!=null)scrollX=node.x()*24; scrollY = Math.max(0,Math.min(maxScroll(),node == null ? 0 : node.y()*32-96));
    }
    private void rebuildSettings() {
        for (Button button : settingsButtons) removeWidget(button);
        settingsButtons.clear();
        var node = selectedNode(); if (node == null) return;
        var definition = FocusNodeRegistry.get(node.key()); if (definition == null) return;
        int row = 0;
        for (var setting : visibleSettings(definition)) {
            int y = topPos+imageHeight+3-definition.settings().size()*26+row++*26;
            boolean range=true;
            for(int i=0;i<setting.values().size();i++) if(!setting.descriptions().get(i).equals(Integer.toString(setting.values().get(i)))
                    ||i>0&&setting.values().get(i)!=setting.values().get(i-1)+1) range=false;
            settingsButtons.add(addRenderableWidget(new FocusSettingSpinner(leftPos+231,y,range?32:72,Component.translatable(setting.label()),
                    ()->settingDescription(setting),direction->changeSetting(setting,direction))));
        }
    }
    private List<FocusNodeRegistry.Setting> visibleSettings(FocusNodeRegistry.Definition definition) {
        return definition.settings().values().stream().filter(setting->setting.research()==null||menu.knowledge().isResearchCompleteStrict(setting.research())).toList();
    }
    private Component settingDescription(FocusNodeRegistry.Setting setting) {
        var node=selectedNode();int index=node==null?0:Math.max(0,setting.values().indexOf(node.settings().getOrDefault(setting.key(),setting.defaultValue())));
        return Component.translatable(setting.descriptions().get(index));
    }
    private void changeSetting(FocusNodeRegistry.Setting setting, int direction) {
        if (menu.busy() || menu.pending()) return;
        var node = selectedNode(); if (node == null) return;
        Map<String,Integer> changed = new LinkedHashMap<>(node.settings());
        int index = setting.values().indexOf(changed.getOrDefault(setting.key(), setting.defaultValue()));
        changed.put(setting.key(), setting.values().get(Math.max(0,Math.min(index+direction,setting.values().size()-1))));
        draft = new FocusGraph(draft.nodes().stream().map(current -> current.id() == node.id()
                ? new FocusGraph.Node(current.id(),current.parent(),current.children(),current.x(),current.y(),current.key(),changed) : current).toList());
        markDirty();
    }
    @Override protected void renderLabels(GuiGraphics graphics, int x, int y) {}
    @Override protected void renderBg(GuiGraphics graphics, float partial, int mouseX, int mouseY) {
        RenderSystem.enableBlend();
        graphics.blit(BACK,leftPos,topPos,0,0,231,231,256,256);
        graphics.blit(INVENTORY,leftPos-71,topPos-3,0,0,71,239,256,256);
        graphics.enableScissor(leftPos+63,topPos+31,leftPos+199,topPos+191);
        for (var node : draft.nodes()) {
            int x = leftPos+140+node.x()*24-scrollX, y = topPos+50+node.y()*32-scrollY;
            if (node.parent() >= 0) {
                var parent=draft.nodes().stream().filter(n->n.id()==node.parent()).findFirst().orElse(null);
                graphics.blit(FRAME,x-6,y-22,54,232,12,12,256,256);
                if(parent!=null&&FocusNodeRegistry.isSplit(parent.key())) for(int a=0;a<Math.abs(node.x()-parent.x());a++) {
                    if(node.x()<parent.x()) {
                        if(a==0) graphics.blit(FRAME,x-4,y-36,8,240,16,16,256,256);
                        else graphics.blit(FRAME,x-12+a*24,y-36,72,240,24,16,256,256);
                    } else {
                        if(a==0) graphics.blit(FRAME,x-12,y-36,24,240,16,16,256,256);
                        else graphics.blit(FRAME,x-12-a*24,y-36,72,240,24,16,256,256);
                    }
                }
            }
            var definition = FocusNodeRegistry.get(node.key());
            if (definition != null) drawPart(graphics,definition,x,y,32,hovered(mouseX,mouseY,x-10,y-10,20,20),true);
            else {
                graphics.blit(FRAME,x-12,y-12,120,232,24,24,256,256);
                var parent=draft.nodes().stream().filter(n->n.id()==node.parent()).findFirst().orElse(null);
                var supplier=parent==null?null:FocusNodeRegistry.get(parent.key());
                if(supplier!=null) {
                    int offset=supplier.supplied().size()>1?4:0;
                    if(supplier.supplied().contains(FocusNodeRegistry.Supply.TARGET))graphics.blit(FRAME,x-offset-4,y-4,8,8,152,240,16,16,256,256);
                    if(supplier.supplied().contains(FocusNodeRegistry.Supply.TRAJECTORY))graphics.blit(FRAME,x+offset-4,y-4,8,8,168,240,16,16,256,256);
                }
            }
            if (selected == node.id() && node.id() != 0) graphics.blit(FRAME,x-12,y-12,96,232,24,24,256,256);
        }
        graphics.disableScissor();
        graphics.blit(FRAME,leftPos,topPos,0,0,231,231,256,256);
        graphics.blit(BASE,leftPos+24,topPos+8,192,224,8,14,256,256);
        for (int i = 1; i < 22; i++) graphics.blit(BASE,leftPos+24+i*8,topPos+8,200,224,8,14,256,256);
        graphics.blit(BASE,leftPos+200,topPos+8,208,224,8,14,256,256);
        List<FocusNodeRegistry.Definition> parts = parts();
        partsStart = Math.max(0, Math.min(partsStart, Math.max(0,parts.size()-6)));
        for (int i = 0; i < 6 && partsStart+i < parts.size(); i++) {
            var part = parts.get(partsStart+i);
            boolean enabled = part.runtimeSupported() && menu.knowledge().isResearchCompleteStrict(part.research());
            drawPart(graphics,part,leftPos+38,topPos+43+i*25,part.type()==FocusNodeRegistry.Type.MOD?24:32,
                    hovered(mouseX,mouseY,leftPos+28,topPos+33+i*25,20,20),enabled);
        }
        partsSlider.visible=parts.size()>6;verticalSlider.visible=maxScroll()>0;
        horizontalSlider.visible=minScrollX()<-70||maxScrollX()>70;
        graphics.blit(gui("complex"),leftPos+227,topPos+34,12,12,0,0,16,16,16,16);
        graphics.blit(gui("costxp"),leftPos+227,topPos+48,12,12,0,0,16,16,16,16);
        graphics.blit(gui("costvis"),leftPos+227,topPos+62,12,12,0,0,16,16,16,16);
        var compiled = compile();
        int complexity = compiled.success() ? compiled.plan().complexity() : editorComplexity();
        int xp = (int)Math.max(1,Math.round(Math.sqrt(complexity))), capacity = FocusStacks.maxComplexity(menu.focus());
        graphics.drawString(font,complexity+"/"+capacity,leftPos+242,topPos+36,complexity > capacity ? 0xF66F68 : 0xFFDDBF);
        graphics.drawString(font,Integer.toString(xp),leftPos+242,topPos+50,menu.creative() || xp <= menu.experienceLevel() ? 0x99FF9D : 0xF66F68);
        graphics.drawString(font,Integer.toString((int)(menu.busy() ? menu.remainingVis() : complexity*10+capacity/5)),leftPos+242,topPos+64,0x55FFFF);
        graphics.drawString(font,Component.translatable("item.Focus.cost1").getString()+": "+String.format(Locale.ROOT,"%.1f",complexity/5F),leftPos+230,topPos+80,0x55FFFF);
        Map<String,Integer> crystals = editorCrystals();
        if (!crystals.isEmpty()) graphics.drawString(font,Component.translatable("wandtable.text4"),leftPos+230,topPos+94,0xFFCC55);
        int i = 0;
        for (var entry : crystals.entrySet()) {
            ItemStack crystal = AspectCrystalItem.create(Aspect.getAspect(entry.getKey()),entry.getValue());
            int x = leftPos+234+(i%5)*16, y = topPos+110+(i/5)*16;
            graphics.renderItem(crystal,x,y); graphics.renderItemDecorations(font,crystal,x,y); i++;
        }
        if (menu.busy()) graphics.drawString(font,Component.translatable("gui.thaumcraft.focal.busy"),leftPos+230,topPos+146,0xAACCFF);
        else if (!menu.status().isEmpty() && !menu.status().equals("ACCEPTED")) {
            var lines = font.split(Component.translatable("gui.thaumcraft.focal.result."+menu.status().toLowerCase(Locale.ROOT)),98);
            for (int line = 0; line < Math.min(3,lines.size()); line++) graphics.drawString(font,lines.get(line),leftPos+230,topPos+146+line*10,0xEEA0A0);
        }
        var selectedNode = selectedNode();
        var definition = selectedNode == null ? null : FocusNodeRegistry.get(selectedNode.key());
        if (definition != null) {
            int row = 0;
            for (var setting : visibleSettings(definition)) {
                int y=topPos+imageHeight-10-definition.settings().size()*26+row++*26;
                graphics.drawString(font,Component.translatable(setting.label()),leftPos+231,y,0xFFCC55);
            }
        }
    }
    private int editorComplexity() {
        int total = 0; Map<String,Integer> seen = new HashMap<>();
        for (var node : draft.nodes()) {
            var definition = FocusNodeRegistry.get(node.key()); if (definition == null) continue;
            total += (int)(definition.complexity(node.settings())*(.5F*(seen.merge(node.key(),1,Integer::sum)+1)));
        }
        return total;
    }
    private Map<String,Integer> editorCrystals() {
        Map<String,Integer> result = new LinkedHashMap<>();
        for (var node : draft.nodes()) {
            var definition = FocusNodeRegistry.get(node.key());
            if (definition != null && definition.aspect()!=null) result.merge(definition.aspect(),1,Integer::sum);
        }
        return result;
    }
    private void drawPart(GuiGraphics graphics, FocusNodeRegistry.Definition part, int x, int y, int size, boolean hover, boolean enabled) {
        float brightness = enabled ? 1F : .35F;
        int color = part.color(); RenderSystem.setShaderColor(((color>>16)&255)/255F*brightness,((color>>8)&255)/255F*brightness,(color&255)/255F*brightness,1);
        boolean root = part.key().equals(FocusNodeRegistry.ROOT)||part.type()==FocusNodeRegistry.Type.MOD;
        if(root)size*=2;
        if (!root && part.type() != FocusNodeRegistry.Type.MOD) graphics.blit(part.type() == FocusNodeRegistry.Type.EFFECT ? EFFECT : MEDIUM,
                x-(int)(size*.9F+(hover?2:0))/2,y-(int)(size*.9F+(hover?2:0))/2,(int)(size*.9F+(hover?2:0)),(int)(size*.9F+(hover?2:0)),0,0,32,32,32,32);
        RenderSystem.setShaderColor(brightness,brightness,brightness,1);
        int iconSize = size/2+(hover?2:0);
        graphics.blit(part.icon(),x-iconSize/2,y-iconSize/2,iconSize,iconSize,0,0,32,32,32,32);
        RenderSystem.setShaderColor(1,1,1,1);
        if (!enabled) graphics.drawString(font,"×",x+7,y+4,0xB86B69);
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        renderBackground(graphics); super.render(graphics,mouseX,mouseY,partial); renderTooltip(graphics,mouseX,mouseY);
        if(confirm.isHoveredOrFocused())graphics.renderTooltip(font,Component.translatable("wandtable.text3"),mouseX,mouseY);
        List<FocusNodeRegistry.Definition> parts = parts();
        for (int i = 0; i < 6 && partsStart+i < parts.size(); i++) if (hovered(mouseX,mouseY,leftPos+28,topPos+33+i*25,20,20)) {
            var definition = parts.get(partsStart+i);
            List<Component> tooltip = new ArrayList<>(); tooltip.add(Component.translatable(definition.key()+".name"));
            tooltip.add(Component.translatable(definition.key()+".text"));
            graphics.renderComponentTooltip(font,tooltip,mouseX,mouseY);
        }
    }
    private static boolean hovered(double x, double y, int left, int top, int width, int height) { return x >= left && x < left+width && y >= top && y < top+height; }
    @Override public boolean mouseClicked(double x, double y, int button) {
        if (!menu.busy() && !menu.pending()) {
            // The original name field receives every editable click, including
            // graph/palette hits which return before native widget dispatch.
            if (name.isFocused() && !name.isMouseOver(x,y)) setFocused(null);
            for (var node : draft.nodes()) if (node.id() != 0 && hovered(x,y,leftPos+63,topPos+31,136,160)
                    && hovered(x,y,leftPos+130+node.x()*24-scrollX,topPos+40+node.y()*32-scrollY,20,20)) {
                selected = node.id(); partsStart = 0; rebuildSettings(); if (button == 1) clearSelected(); return true;
            }
            var parts = parts();
            for (int i = 0; i < 6 && partsStart+i < parts.size(); i++) if (hovered(x,y,leftPos+28,topPos+33+i*25,20,20)) {
                if (button == 0) install(parts.get(partsStart+i)); return true;
            }
        }
        return super.mouseClicked(x,y,button);
    }
    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        // AbstractContainerScreen consumes inventory drags without dispatching
        // them to focused widgets; preserve that path except for our sliders.
        if (button == 0 && isDragging() && getFocused() instanceof OriginalSlider slider
                && slider.active && slider.visible)
            return slider.mouseDragged(x,y,button,dx,dy);
        return super.mouseDragged(x,y,button,dx,dy);
    }
    @Override public boolean mouseScrolled(double x, double y, double delta) {
        if(delta==0)return super.mouseScrolled(x,y,delta);
        if (hovered(x,y,leftPos+20,topPos+30,38,155)) { partsStart=Math.max(0,Math.min(Math.max(0,parts().size()-6),partsStart+(delta>0?-1:1)));return true; }
        if (hovered(x,y,leftPos+63,topPos+31,136,160)&&hasShiftDown()){scrollX=Math.max(minScrollX(),Math.min(maxScrollX(),scrollX+(delta>0?-24:24)));return true;}
        if (hovered(x,y,leftPos+63,topPos+31,136,160)) { scrollY = Math.max(0,Math.min(maxScroll(),scrollY+(delta > 0 ? -16 : 16))); return true; }
        return super.mouseScrolled(x,y,delta);
    }
    @Override public boolean keyPressed(int code, int scan, int modifiers) {
        if (name.isFocused() && code != 256) return name.keyPressed(code,scan,modifiers);
        return super.keyPressed(code,scan,modifiers);
    }
    /** Real QA drives the same editor method as a palette click, without global desktop input. */
    public void selectForSmoke(int node, String key) { selected = node; install(Objects.requireNonNull(FocusNodeRegistry.get(key))); }
    public void setNameForSmoke(String value) { name.setValue(value); }
    public FocusGraph draftForSmoke() { return draft; }
    public boolean clickNodeForVisualSmoke(int id) {
        requireVisualSmoke();var node=draft.nodes().stream().filter(n->n.id()==id).findFirst().orElse(null);
        return node!=null&&mouseClicked(leftPos+140+node.x()*24-scrollX,topPos+50+node.y()*32-scrollY,0);
    }
    public boolean clickSettingForVisualSmoke(String key,int direction) {
        requireVisualSmoke();var node=selectedNode();if(node==null)return false;var definition=FocusNodeRegistry.get(node.key());if(definition==null)return false;
        var rows=visibleSettings(definition);for(int i=0;i<rows.size();i++)if(rows.get(i).key().equals(key)) {
            var button=settingsButtons.get(i);return button.mouseClicked(direction<0?button.getX()+4:button.getX()+button.getWidth()-4,button.getY()+4,0);
        }return false;
    }
    public boolean clickConfirmForVisualSmoke() { requireVisualSmoke();return confirm.mouseClicked(confirm.getX()+12,confirm.getY()+8,0); }
    public Map<String,Object> exerciseNameBlurForVisualSmoke() {
        requireVisualSmoke();
        double x=name.getX()+5,y=name.getY()+5;
        if(!mouseClicked(x,y,0))throw new AssertionError("Native name field click refused");
        mouseReleased(x,y,0);
        if(!name.isFocused())throw new AssertionError("Native name field did not acquire focus");
        var node=draft.nodes().stream().filter(n->n.id()==1).findFirst().orElseThrow();
        x=leftPos+140+node.x()*24-scrollX;y=topPos+50+node.y()*32-scrollY;
        if(!mouseClicked(x,y,0))throw new AssertionError("Native graph click refused after name focus");
        mouseReleased(x,y,0);
        if(name.isFocused()||isDragging())throw new AssertionError("Graph click retained native name focus/dragging");
        return Map.of("nameFocusedByRealClick",true,"graphClickBlurredName",true,"draggingAfterRelease",isDragging());
    }
    public Map<String,Object> exercisePaletteDragForVisualSmoke() {
        requireVisualSmoke();return exerciseSliderDrag(partsSlider,"palette",0,Math.max(0,parts().size()-6));
    }
    public Map<String,Object> exercisePaletteWheelBoundsForVisualSmoke() {
        requireVisualSmoke();
        var options=parts();int max=Math.max(0,options.size()-6);
        if(options.isEmpty()||partsStart!=0)throw new AssertionError("Expected native palette at its first part");
        double x=leftPos+38,y=topPos+43;
        for(int i=0;i<options.size()+2;i++)mouseScrolled(x,y,-1);
        int lower=partsStart;
        if(lower!=max)throw new AssertionError("Native palette wheel exceeded last eligible part");
        for(int i=0;i<options.size()+2;i++)mouseScrolled(x,y,1);
        int upper=partsStart;
        if(upper!=0)throw new AssertionError("Native palette wheel exceeded first eligible part");
        int edited=selected;String expected=options.get(0).key();
        if(!mouseClicked(x,y,0))throw new AssertionError("Native palette click after boundary wheel refused");
        mouseReleased(x,y,0);
        var actual=draft.nodes().stream().filter(n->n.id()==edited).findFirst().orElseThrow();
        if(!actual.key().equals(expected)||isDragging())throw new AssertionError("Native boundary wheel/click selected wrong part");
        return Map.of("eligibleParts",options.size(),"maxStart",max,"afterBeyondEndWheel",lower,
                "afterBeyondStartWheel",upper,"realPaletteClickInstalled",actual.key());
    }
    public Map<String,Object> exerciseSameTickConfirmGuardForVisualSmoke() {
        requireVisualSmoke();
        if(!dirty||menu.pending()||menu.busy()||confirm.active)throw new AssertionError("Expected native same-tick dirty editor with inactive confirm");
        var before=draft.save();long revision=menu.revision();
        double x=confirm.getX()+confirm.getWidth()/2D,y=confirm.getY()+confirm.getHeight()/2D;
        mouseClicked(x,y,0);mouseReleased(x,y,0);
        if(menu.pending()||menu.busy()||menu.revision()!=revision||!dirty||!before.equals(draft.save()))
            throw new AssertionError("Same-tick native confirm started stale server graph");
        return Map.of("dirtyBeforeServerEdit",true,"confirmActive",confirm.active,
                "nativeScreenConfirmClickSentStart",false,"serverRevision",revision,"draftUnchanged",true);
    }
    public boolean graphSlidersReadyForVisualSmoke() {
        requireVisualSmoke();
        if(maxScroll()<=0||maxScrollX()<=minScrollX())throw new AssertionError("Temporary native graph has no scrollbar range");
        return verticalSlider.visible&&horizontalSlider.visible;
    }
    public List<Map<String,Object>> exerciseGraphDragsForVisualSmoke() {
        requireVisualSmoke();
        return List.of(exerciseSliderDrag(verticalSlider,"vertical",0,maxScroll()),
                exerciseSliderDrag(horizontalSlider,"horizontal",minScrollX(),maxScrollX()));
    }
    private int sliderValue(OriginalSlider slider) {
        if(slider==partsSlider)return partsStart;
        if(slider==verticalSlider)return scrollY;
        if(slider==horizontalSlider)return scrollX;
        throw new AssertionError("Unknown native editor scrollbar");
    }
    private Map<String,Object> exerciseSliderDrag(OriginalSlider slider,String label,int min,int max) {
        if(!slider.active||!slider.visible||max<=min)throw new AssertionError("Native scrollbar not eligible: "+label);
        boolean vertical=slider!=horizontalSlider;
        int before=sliderValue(slider);
        double span=(vertical?slider.getHeight():slider.getWidth())-8;
        double x=vertical?slider.getX()+4:slider.getX()+4+span*.25;
        double y=vertical?slider.getY()+4+span*.25:slider.getY()+4;
        if(!mouseClicked(x,y,0)||getFocused()!=slider||!isDragging())throw new AssertionError("Native scrollbar click not captured: "+label);
        int clicked=sliderValue(slider);
        double endX=vertical?x:slider.getX()+4+span*.9;
        double endY=vertical?slider.getY()+4+span*.9:y;
        if(!mouseDragged(endX,endY,0,endX-x,endY-y))throw new AssertionError("Native scrollbar drag refused: "+label);
        int dragged=sliderValue(slider);
        mouseReleased(endX,endY,0);
        if(dragged==clicked||isDragging())throw new AssertionError("Native scrollbar drag/release failed: "+label);
        double restore=(before-min)/(double)(max-min);
        double restoreX=vertical?x:slider.getX()+4+span*restore;
        double restoreY=vertical?slider.getY()+4+span*restore:y;
        if(!mouseClicked(restoreX,restoreY,0)||getFocused()!=slider)throw new AssertionError("Native scrollbar restore click refused: "+label);
        mouseReleased(restoreX,restoreY,0);
        if(sliderValue(slider)!=before||isDragging())throw new AssertionError("Native scrollbar did not restore through mouse input: "+label);
        var facts=new LinkedHashMap<String,Object>();
        facts.put("slider",label);facts.put("min",min);facts.put("max",max);facts.put("before",before);
        facts.put("afterRealClick",clicked);facts.put("afterRealDrag",dragged);facts.put("restoredByRealClick",sliderValue(slider));
        facts.put("click",List.of(x,y));facts.put("dragTo",List.of(endX,endY));facts.put("draggingAfterRelease",isDragging());
        return facts;
    }
    private static void requireVisualSmoke(){if(!Boolean.getBoolean("thaumcraft.armorVisualSmokeTest"))throw new IllegalStateException("Visual fixture disabled");}
}
