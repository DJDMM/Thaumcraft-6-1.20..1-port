package thaumcraft.client.theory;

import net.minecraft.client.gui.GuiGraphics;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import thaumcraft.research.theory.TheoryAids;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.theory.ResearchTableMenu;
import thaumcraft.research.theory.TheoryCard;
import thaumcraft.research.theory.TheoryModule;
import thaumcraft.research.theory.TheoryNetwork;
import thaumcraft.research.theory.TheorySession;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** BETA26 desk, parchment offers and category percentages, driven only by server snapshots. */
public final class ResearchTableScreen extends AbstractContainerScreen<ResearchTableMenu> {
    private static final ResourceLocation DESK = texture("gui/gui_research_table.png");
    private static final ResourceLocation PAPER = texture("gui/paper.png");
    private static final ResourceLocation GILDED = texture("gui/papergilded.png");
    private static final ResourceLocation BASE = texture("gui/gui_base.png");
    private static final int LIGHT = 0xEADCB9, INK = 0x3F2D1E;
    private final Set<String> selectedAids = new HashSet<>();
    private static final List<String> AID_KEYS = List.copyOf(TheoryAids.keys());
    private final TheoryAidButton[] aidButtons = new TheoryAidButton[AID_KEYS.size()];
    private final TheoryCardAnimation animation = new TheoryCardAnimation();
    private final Map<String, Float> displayedTotals = new LinkedHashMap<>();
    private final TheoryGuiSparkles sparkles=new TheoryGuiSparkles();
    private int lastDisplayTick=-1;
    private int renderMouseX,renderMouseY;
    private TheoryCard displayedLastCard;
    private List<Long> displayedSavedCards=List.of();
    private int resolvingAnimations, drawnAnimations;
    private final TheoryButton[] choose = new TheoryButton[3];
    private TheoryButton start, draw, bonus, finish, scrap;
    private RequestSink previewSender;
    private boolean previewPending;

    @FunctionalInterface interface RequestSink {
        void send(int menuId, long revision, TheoryNetwork.Action action, int cardIndex, Set<String> aids);
    }

    public ResearchTableScreen(ResearchTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        // The desk remains 255x255; its original external category column also needs room.
        // Includes the original 3-card overhang at the left and category column at the right.
        imageWidth = 433;
        imageHeight = 255;
    }

    @Override protected void init() {
        super.init();
        leftPos+=48;
        start = button(103, 16, 51, 13, "start", b -> submit(TheoryNetwork.Action.START, -1));
        start.setMessage(Component.translatable("button.create.theory"));
        draw = button(10, 163, 79, 14, "draw", b -> submit(TheoryNetwork.Action.DRAW, -1));
        bonus = button(89, 163, 79, 14, "draw_bonus", b -> submit(TheoryNetwork.Action.DRAW_BONUS, -1));
        scrap = button(103, 162, 51, 13, "scrap", b -> submit(TheoryNetwork.Action.SCRAP, -1));
        scrap.setMessage(Component.translatable("button.scrap.theory"));scrap.tint=0xFF2222;
        finish = button(166, 90, 51, 13, "finish", b -> submit(TheoryNetwork.Action.FINISH, -1));
        finish.setMessage(Component.translatable("button.complete.theory"));
        for (int i = 0; i < aidButtons.length; i++) {
            final String key = AID_KEYS.get(i);
            int columns = Math.min(6, Math.max(1, AID_KEYS.size()));
            int x = 128 - columns * 10 + i % columns * 20;
            int y = 85 + i / columns * 35;
            aidButtons[i] = addRenderableWidget(new TheoryAidButton(leftPos + x, topPos + y, aidIcon(key),
                    tr("aid." + key.toLowerCase(Locale.ROOT)), b -> {
                if (pending() || !menu.canUse() || menu.session() != null || !menu.availableAids().contains(key)) return;
                if (!selectedAids.remove(key) && selectedAids.size() + 1 < menu.inspirationAvailable()) selectedAids.add(key);
            }));
        }
        for (int i = 0; i < choose.length; i++) {
            final int index = i;
            choose[i] = button(0, 145, 72, 13, "select", b -> submit(TheoryNetwork.Action.SELECT, index));
        }
        updateControls();
    }

    private TheoryButton button(int x, int y, int width, int height, String key, Button.OnPress press) {
        return addRenderableWidget(new TheoryButton(leftPos + x, topPos + y, width, height, tr(key), press));
    }

    private boolean inkAvailable() {
        ItemStack ink = menu.getSlot(0).getItem();
        return ink.is(TheoryModule.SCRIBING_TOOLS.get()) && ink.getDamageValue() < ink.getMaxDamage();
    }

    private boolean paperAvailable() { return menu.getSlot(1).getItem().is(Items.PAPER); }
    private boolean pending() { return menu.requestPending() || previewPending; }

    private boolean canRequest(TheoryNetwork.Action action, int index) {
        if (pending() || !menu.canUse() || previewSender == null && animation.busy()) return false;
        TheorySession session = menu.session();
        return switch (action) {
            case START -> session == null && inkAvailable() && paperAvailable() && menu.inspirationAvailable() > selectedAids.size();
            case DRAW -> session != null && !session.complete() && session.choices().isEmpty() && paperAvailable();
            case DRAW_BONUS -> session != null && !session.complete() && session.choices().isEmpty() && paperAvailable() && session.bonusDraws() > 0;
            case SELECT -> {
                if (session == null || session.complete() || !inkAvailable() || index < 0 || index >= session.choices().size()) yield false;
                yield session.canSelect(menu.playerKnowledge(), menu.playerInventory(), menu.playerExperienceLevel(), index);
            }
            case FINISH -> session != null && session.complete();
            case SCRAP -> session != null && !session.complete();
        };
    }

    private void submit(TheoryNetwork.Action action, int index) {
        if (!canRequest(action, index)) return;
        if (action == TheoryNetwork.Action.START || action == TheoryNetwork.Action.SCRAP || action == TheoryNetwork.Action.FINISH) playPaper("clack", .4F);
        Set<String> aids = action == TheoryNetwork.Action.START ? Set.copyOf(selectedAids) : Set.of();
        if (previewSender != null) {
            previewPending = true;
            previewSender.send(menu.containerId, menu.revision(), action, index, aids);
        } else menu.request(action, index, aids);
        updateControls();
    }

    private void updateControls() {
        if (start == null) return;
        TheorySession session = menu.session();
        selectedAids.retainAll(menu.availableAids());
        boolean idle = session == null, complete = session != null && session.complete();
        start.visible = idle;
        draw.visible = bonus.visible = false;
        scrap.visible = !idle && !complete;
        finish.visible = complete && !animation.busy();
        start.active = canRequest(TheoryNetwork.Action.START, -1);
        draw.active = canRequest(TheoryNetwork.Action.DRAW, -1);
        bonus.active = canRequest(TheoryNetwork.Action.DRAW_BONUS, -1);
        scrap.active = canRequest(TheoryNetwork.Action.SCRAP, -1);
        finish.active = canRequest(TheoryNetwork.Action.FINISH, -1);
        int aidColumns = Math.min(6, Math.max(1, menu.availableAids().size()));
        int visibleAid = 0;
        for (int i = 0; i < aidButtons.length; i++) {
            String key = AID_KEYS.get(i);
            boolean selected = selectedAids.contains(key);
            aidButtons[i].visible = idle && menu.availableAids().contains(key);
            if (aidButtons[i].visible) {
                aidButtons[i].setX(leftPos + 128 - aidColumns * 10 + visibleAid % aidColumns * 20);
                aidButtons[i].setY(topPos + 85 + visibleAid / aidColumns * 35);
                visibleAid++;
            }
            aidButtons[i].active = !pending() && menu.canUse() && (selected || selectedAids.size() + 1 < menu.inspirationAvailable());
            aidButtons[i].selected = selected;
        }
        for (int i = 0; i < choose.length; i++) {
            choose[i].visible = false; // The original selects the parchment itself.
            choose[i].active = canRequest(TheoryNetwork.Action.SELECT, i);
            int count = session == null ? 0 : session.choices().size();
            choose[i].setX(leftPos + cardX(i, count) + 7);
            choose[i].setWidth(cardWidth(count) - 14);
        }
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.blit(DESK,leftPos,topPos,0,0,255,255,256,256);
        TheorySession session=menu.session();
        int maximum=session==null?menu.inspirationAvailable():session.inspirationStart();
        int inspiration=session==null?maximum-selectedAids.size():session.inspiration();
        for(int i=0;i<maximum;i++)graphics.blit(BASE,leftPos+128-maximum*5+i*10,topPos+(session==null?55:16),8,8,
                inspiration>i?32:48,96,16,16,256,256);
        if(session!=null) {
            renderDraw(graphics,session);
            if(!animation.cards().isEmpty()&&previewSender==null)for(int i=0;i<animation.cards().size();i++)renderAnimatedCard(graphics,animation.cards().get(i),i,animation.cards().size());
            else if(!session.choices().isEmpty())for(int i=0;i<session.choices().size();i++)renderCard(graphics,session.choices().get(i),i,session.choices().size());
            renderProgress(graphics,session);
        }
        Component message=statusMessage();
        if(message!=null)text(graphics,message,8,178,238,.65F,0xFFBC9C,true);
    }

    private void renderDraw(GuiGraphics graphics,TheorySession session) {
        int sheets=menu.getSlot(1).hasItem()?1+menu.getSlot(1).getItem().getCount()/4:0;
        java.util.Random paperRandom=new java.util.Random(55L);
        if(!session.complete())for(int i=0;i<sheets;i++)drawSheet(graphics,null,65,100,6,paperRandom,1,1);
        if(sheets>0&&!session.complete()) {
            boolean hover=isHovering(25,55,75,90,renderMouseX,renderMouseY)&&session.choices().isEmpty();
            int size=hover?28:24;graphics.setColor(1,1,1,hover?1:.5F);
            graphics.blit(texture("aspects/_unknown.png"),leftPos+65-size/2,topPos+100-size/2,size,size,0,0,256,256,256,256);
            graphics.setColor(1,1,1,1);
        }
        // Original right stack retains the resolved card, without adding a completion sheet.
        for(long seed:displayedSavedCards)drawSheet(graphics,null,191,100,6,new java.util.Random(seed),1,1);
        if(displayedLastCard!=null)drawSheet(graphics,displayedLastCard,191,100,6,new java.util.Random(displayedLastCard.seed()),1,1);
        renderBonus(graphics,session);
    }

    private void renderCard(GuiGraphics graphics,TheoryCard card,int index,int count) {
        drawSheet(graphics,card,183-55*count+110*index,100,8,new java.util.Random(card.seed()),1,0);
    }

    private void drawSheet(GuiGraphics graphics,TheoryCard card,float x,float y,float scale,java.util.Random random,float alpha,float tilt) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.pose().pushPose();
        graphics.pose().translate(leftPos+x+random.nextGaussian(),topPos+y+random.nextGaussian(),10);
        graphics.pose().scale(scale,scale,1);
        graphics.pose().mulPose(Axis.ZP.rotationDegrees((float)(random.nextGaussian()*tilt)));
        graphics.pose().pushPose();
        if(random.nextBoolean())graphics.pose().mulPose(Axis.ZP.rotationDegrees(180));
        if(random.nextBoolean())graphics.pose().mulPose(Axis.YP.rotationDegrees(180));
        graphics.setColor(1,1,1,alpha);
        // The original randomly mirrors whole sheets around Y and explicitly
        // disables culling, otherwise half the seeded papers face away.
        RenderSystem.disableCull();
        graphics.blit(card!=null&&card.fromAid()?GILDED:PAPER,-8,-8,16,16,0,0,256,256,256,256);
        RenderSystem.enableCull();
        graphics.pose().popPose();
        graphics.setColor(1,1,1,1);
        if(card!=null&&alpha==1) {
            if(card.category()!=null&&!card.category().isEmpty()) {
                graphics.setColor(1,1,1,1/6F);
                ResourceLocation icon=card.category().equals("BASICS")?texture("items/thaumonomicon_cheat.png"):texture("research/cat_"+card.category().toLowerCase(Locale.ROOT)+".png");
                graphics.blit(icon,-4,-4,8,8,0,0,256,256,256,256);graphics.setColor(1,1,1,1);
            }
            graphics.pose().pushPose();graphics.pose().scale(1/16F,1/16F,1);
            Component name=card.title().copy().withStyle(ChatFormatting.BOLD);
            graphics.drawString(font,name,-font.width(name)/2,-65,0,false);
            var lines=font.split(card.description(),140);
            for(int i=0;i<lines.size();i++)graphics.drawString(font,lines.get(i),-70,-48+i*font.lineHeight,0,false);
            int cost=card.cost();boolean refund=cost<0;if(refund)cost=Math.abs(cost)+1;
            for(int i=0;i<cost;i++)graphics.blit(BASE,-10*cost+20*i,-95,refund&&i==0?48:32,refund&&i==0?0:96,16,16,256,256);
            graphics.pose().popPose();
            var required=card.requiredItems();
            graphics.pose().pushPose();graphics.pose().scale(.125F,.125F,1);
            for(int i=0;i<required.size();i++) {
                int ix=-9*required.size()+18*i;
                graphics.renderItem(required.get(i).stack(),ix,35);
                if(required.get(i).consumed()) {
                    float bounce=(float)Math.sin((minecraft.player.tickCount+i*2+minecraft.getFrameTime())/2F)*.03F;
                    graphics.pose().pushPose();graphics.pose().translate(ix-2,45+bounce*10,1);graphics.pose().scale(.5F,.5F+bounce,1);
                    graphics.blit(BASE,0,0,64,120,16,16,256,256);graphics.pose().popPose();
                }
            }
            graphics.pose().popPose();
        }
        graphics.setColor(1,1,1,1);graphics.pose().popPose();
    }

    private Component cardInkCost(TheoryCard card) {
        if (card.tablePaperExtra() == 0 && card.tableInkExtra() == 0) return tr("ink_cost");
        ItemStack tools = menu.getSlot(0).getItem();
        return tr("scripting_cost", Math.min(1 + card.tableInkExtra(), tools.getMaxDamage() - tools.getDamageValue()),
                Math.min(menu.getSlot(1).getItem().getCount(), card.tablePaperExtra()));
    }

    private void renderRequiredItems(GuiGraphics graphics, TheoryCard card, int x, int width) {
        List<TheoryCard.RequiredItem> requirements = card.requiredItems();
        for (int i = 0; i < requirements.size(); i++) {
            var required = requirements.get(i);
            ItemStack wanted = required.stack();
            int itemX = requiredItemX(x, width, requirements.size(), i);
            long owned = requiredCount(wanted);
            graphics.pose().pushPose();
            graphics.pose().translate(leftPos + itemX, topPos + 108, 4);
            graphics.pose().scale(0.75F, 0.75F, 1);
            graphics.renderItem(wanted, 0, 0);
            graphics.pose().popPose();
            if (required.consumed()) text(graphics, Component.literal("!").withStyle(ChatFormatting.BOLD), itemX + 10, 106 + Math.round((float)Math.sin((System.nanoTime() / 100_000_000D + i) / 2)), 5, 0.78F, 0xD6A723, true);
            text(graphics, Component.literal(Math.min(owned, wanted.getCount()) + "/" + wanted.getCount()), itemX - 2, 121, 19, 0.58F,
                    owned < wanted.getCount() ? 0xA5372B : 0x437632, true);
        }
    }

    private long requiredCount(ItemStack wanted) {
        return menu.playerInventory().items.stream().filter(found -> TheoryCard.matchesRequirement(found, wanted)).mapToLong(ItemStack::getCount).sum();
    }
    private static int requiredItemX(int cardX, int width, int count, int index) { return cardX+width/2-9*count+18*index; }

    private void renderProgress(GuiGraphics graphics,TheorySession session) {
        if(session==null)return;
        var sorted=displayedTotals.entrySet().stream().filter(entry->entry.getValue()!=0)
                .sorted(java.util.Comparator.<Map.Entry<String,Float>>comparingDouble(Map.Entry::getValue).reversed()).toList();
        for(int i=0;i<sorted.size();i++) {
            var row=sorted.get(i);int percentage=Math.round(row.getValue());
            categoryIcon(graphics,row.getKey(),253,16+i*18+(i>0?4:0),16);
            String value=percentage+"%"+(i>session.penaltyStart()?" (-"+percentage/3+")":"");
            int color=session.blocked().contains(row.getKey())?6316128:i<=session.penaltyStart()?57536:0xFFFFFF;
            graphics.drawString(font,value,leftPos+276,topPos+20+i*18+(i>session.penaltyStart()?4:0),color,true);
        }
    }

    private Component statusMessage() {
        if (pending()) return null;
        if (!menu.canUse()) return tr("unavailable");
        TheorySession session = menu.session();
        if (session == null || !session.complete()) {
            if (!inkAvailable()) return tr("no_ink");
            if (!paperAvailable() && (session == null || session.choices().isEmpty())) return tr("no_paper");
        }
        String result = menu.status();
        if (result != null && !result.isBlank() && !result.equals("ACCEPTED")) return tr("result." + result.toLowerCase(Locale.ROOT));
        return null;
    }

    private void parchment(GuiGraphics graphics, ResourceLocation resource, int x, int y, int width, int height) {
        // Both original papers have a 26px transparent margin. Map the measured paper
        // bounds to the layout rectangle so content padding refers to the visible page.
        graphics.blit(resource, leftPos + x, topPos + y, width, height, 26F, 7F, 204, 240, 256, 256);
    }

    private void text(GuiGraphics graphics, Component message, int x, int y, int width, float wantedScale, int color, boolean centered) {
        float scale = Math.min(wantedScale, width / (float) Math.max(1, font.width(message)));
        graphics.pose().pushPose();
        graphics.pose().translate(leftPos + x + (centered ? width / 2F : 0), topPos + y, 1);
        graphics.pose().scale(scale, scale, 1);
        graphics.drawString(font, message, centered ? -font.width(message) / 2 : 0, 0, color, false);
        graphics.pose().popPose();
    }

    private void paragraph(GuiGraphics graphics, Component message, int x, int y, int width, int height, float scale, int color) {
        List<FormattedCharSequence> lines = font.split(message, (int) (width / scale));
        int maximum = Math.max(1, (int) (height / (font.lineHeight * scale)));
        graphics.pose().pushPose();
        graphics.pose().translate(leftPos + x, topPos + y, 1);
        graphics.pose().scale(scale, scale, 1);
        for (int i = 0; i < Math.min(lines.size(), maximum); i++) {
            if (i == maximum - 1 && lines.size() > maximum) graphics.drawString(font, "…", 0, i * font.lineHeight, color, false);
            else graphics.drawString(font, lines.get(i), 0, i * font.lineHeight, color, false);
        }
        graphics.pose().popPose();
    }

    private void categoryIcon(GuiGraphics graphics, String category, int x, int y, int size) {
        ResourceLocation resource = category.equals("BASICS") ? texture("items/thaumonomicon_cheat.png")
                : texture("research/cat_" + category.toLowerCase(Locale.ROOT) + ".png");
        graphics.blit(resource, leftPos + x, topPos + y, size, size, 0F, 0F, 256, 256, 256, 256);
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {}

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (smokePointer != null) { mouseX = smokePointer[0]; mouseY = smokePointer[1]; }
        renderBackground(graphics);
        updateAnimation(mouseX, mouseY,partialTick);
        updateControls();
        float scale = layoutScale();
        int adjustedX = (int) localX(mouseX), adjustedY = (int) localY(mouseY);
        renderMouseX=adjustedX;renderMouseY=adjustedY;
        graphics.pose().pushPose();
        graphics.pose().translate(width * (1 - scale) / 2F, height * (1 - scale) / 2F, 0);
        graphics.pose().scale(scale, scale, 1);
        super.render(graphics, adjustedX, adjustedY, partialTick);
        renderTooltip(graphics, adjustedX, adjustedY);
        renderHints(graphics, adjustedX, adjustedY);
        sparkles.render(graphics,partialTick);
        graphics.pose().popPose();
    }

    private void renderHints(GuiGraphics graphics, int mouseX, int mouseY) {
        if (isHovering(16, 15, 16, 16, mouseX, mouseY) && !menu.getSlot(0).hasItem()) tooltip(graphics, List.of(tr("ink_slot")), mouseX, mouseY);
        else if (isHovering(224, 16, 16, 16, mouseX, mouseY) && !menu.getSlot(1).hasItem()) tooltip(graphics, List.of(tr("paper_slot")), mouseX, mouseY);
        for (int i = 0; i < aidButtons.length; i++) if (aidButtons[i].visible && aidButtons[i].isHovered()) {
            List<Component> lines = new ArrayList<>(List.of(tr("aid." + AID_KEYS.get(i).toLowerCase(Locale.ROOT)), tr("aid_inspiration")));
            for (String card : TheoryAids.cards(AID_KEYS.get(i)).stream().distinct().toList()) {
                String translation = switch (card) { case "dark_whispers" -> "darkwhisper"; case "glyphs" -> "glyph"; case "mind_over_matter" -> "mindmatter"; default -> card; };
                lines.add(Component.translatable("card." + translation + ".name", tr(card.equals("channel") ? "aid_any_aspect" : "aid_any_category")));
            }
            tooltip(graphics, lines, mouseX, mouseY);
        }
        TheorySession session = menu.session();
        if (session != null && (previewSender != null || !animation.busy())) for (int i = 0; i < session.choices().size(); i++) {
            TheoryCard card = session.choices().get(i);
            List<TheoryCard.RequiredItem> items = card.requiredItems();
            for (int item = 0; item < items.size(); item++) {
                int x = requiredItemX(cardX(i, session.choices().size()), cardWidth(session.choices().size()), items.size(), item);
                if (!isHovering(x, 135, 16, 16, mouseX, mouseY)) continue;
                var required = items.get(item);
                ItemStack wanted = required.stack();
                List<Component> lines = new ArrayList<>(wanted.getTooltipLines(minecraft.player, TooltipFlag.NORMAL));
                lines.add(tr(required.consumed() ? "consumed" : "required", wanted.getCount()).copy().withStyle(required.consumed() ? ChatFormatting.GOLD : ChatFormatting.GRAY));
                lines.add(tr("owned", requiredCount(wanted), wanted.getCount()));
                if (!card.hasRequiredItems(menu.playerInventory())) lines.add(tr("card_requirements_missing").copy().withStyle(ChatFormatting.RED));
                tooltip(graphics, lines, mouseX, mouseY);
                return;
            }
            if (!isHovering(cardX(i, session.choices().size()), 40, cardWidth(session.choices().size()), 120, mouseX, mouseY)) continue;
            List<Component> lines = new ArrayList<>(List.of(card.title(), card.description(), tr(card.cost() < 0 ? "refund" : "cost", Math.abs(card.cost())), cardInkCost(card)));
            if (card.requiredLevels() > 0) lines.add(tr("levels", card.requiredLevels(), menu.playerExperienceLevel()));
            if (card.id().equals("dark_whispers")) lines.add(tr("levels_all", menu.playerExperienceLevel()));
            if (card.id().equals("spellbinding")) lines.add(tr("levels_upto", 5, menu.playerExperienceLevel()));
            if (!canRequest(TheoryNetwork.Action.SELECT, i) && !pending()) lines.add(tr("card_requirements_missing").copy().withStyle(ChatFormatting.RED));
            for (var required : items) {
                lines.addAll(required.stack().getTooltipLines(minecraft.player, TooltipFlag.NORMAL));
                lines.add(tr(required.consumed() ? "consumed" : "required", required.stack().getCount()).copy().withStyle(required.consumed() ? ChatFormatting.GOLD : ChatFormatting.GRAY));
            }
            if (card.id().equals("analyze")) lines.add(tr("analyze_cost", categoryName(card.category())));
            if (card.fromAid()) lines.add(tr("aid_card"));
            tooltip(graphics, lines, mouseX, mouseY);
        }
    }

    private void tooltip(GuiGraphics graphics, List<Component> lines, int mouseX, int mouseY) {
        int wrap = Math.min(220, (int) (width / layoutScale()) - 24);
        List<FormattedCharSequence> wrapped = lines.stream().flatMap(line -> font.split(line, wrap).stream()).toList();
        graphics.renderTooltip(font, wrapped, mouseX, mouseY);
    }

    private float layoutScale() { return Math.min(1F, Math.min((width - 12F) / imageWidth, (height - 12F) / imageHeight)); }
    private double localX(double x) { float scale = layoutScale(); return (x - width * (1 - scale) / 2F) / scale; }
    private double localY(double y) { float scale = layoutScale(); return (y - height * (1 - scale) / 2F) / scale; }
    @Override public boolean mouseClicked(double x, double y, int button) {
        updateControls();
        double lx = localX(x), ly = localY(y);
        // Modern container screens consume even an empty desk click. BETA26's
        // void container handler still continued into the parchment hitboxes.
        boolean handled = super.mouseClicked(lx, ly, button);
        TheorySession session = menu.session();
        if (button == 0 && session != null && !session.complete() && canRequest(TheoryNetwork.Action.DRAW, -1)
                && isHovering(25, 55, 75, 90, lx, ly)) {
            submit(session.bonusDraws() > 0 ? TheoryNetwork.Action.DRAW_BONUS : TheoryNetwork.Action.DRAW, -1); return true;
        }
        if (button == 0 && session != null && !session.choices().isEmpty()) {
            int count = session.choices().size();
            for (int i = 0; i < count; i++) if (isHovering(cardX(i, count), 40, cardWidth(count), 120, lx, ly)
                    && canRequest(TheoryNetwork.Action.SELECT, i)) { submit(TheoryNetwork.Action.SELECT, i); return true; }
        }
        return handled;
    }
    @Override public boolean mouseReleased(double x, double y, int button) { return super.mouseReleased(localX(x), localY(y), button); }
    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) { return super.mouseDragged(localX(x), localY(y), button, dx / layoutScale(), dy / layoutScale()); }

    private static int cardWidth(int count) { return 100; }
    private static int cardX(int index, int count) { return 133-55*count+110*index; }
    private static ResourceLocation texture(String path) { return new ResourceLocation("thaumcraft", "textures/" + path); }
    private static Component categoryName(String category) { return Component.translatable("tc.research_category." + category); }
    private static Component tr(String key, Object... args) { return Component.translatable("gui.thaumcraft.theory." + key, args); }

    void pointerForSmokeTest(int[] point) { requireSmokeTest(); smokePointer = point; }
    private int[] smokePointer;

    private void updateAnimation(int mouseX,int mouseY,float partialTick) {
        if(previewSender==null) {
            var before=animation.phase();animation.sync(menu.session());
            if(animation.phase()==TheoryCardAnimation.Phase.DEAL&&before!=animation.phase())drawnAnimations++;
            if(animation.phase()==TheoryCardAnimation.Phase.RESOLVE&&before!=animation.phase()){resolvingAnimations++;playPaper("pageturn",1);}
            var advancing=animation.phase();
            animation.advance(partialTick,(float)(localX(mouseX)-leftPos),(float)(localY(mouseY)-topPos));
            for(int i=animation.takePageFlips();i>0;i--)playPaper("page",1);
            if(advancing==TheoryCardAnimation.Phase.RESOLVE&&animation.phase()==TheoryCardAnimation.Phase.NONE)playPaper("write",.3F);
        }
        if(animation.phase()!=TheoryCardAnimation.Phase.RESOLVE) {
            displayedLastCard=menu.session()==null?null:menu.session().lastCard();
            displayedSavedCards=menu.session()==null?List.of():List.copyOf(menu.session().savedCardSeeds());
        }
        if(minecraft.player==null||lastDisplayTick==minecraft.player.tickCount)return;
        boolean initial=lastDisplayTick<0;lastDisplayTick=minecraft.player.tickCount;
        sparkles.tick(minecraft.player.getRandom());
        var totals=menu.session()==null?Map.<String,Integer>of():menu.session().totals();
        Set<String> increased=new HashSet<>();
        for(String category:ResearchCategories.keys()) {
            int target=totals.getOrDefault(category,0),current=Math.round(displayedTotals.getOrDefault(category,initial?(float)target:0F));
            if(current==0&&target==0){displayedTotals.remove(category);continue;}
            if(current<target){current++;increased.add(category);}else if(current>target)current--;
            displayedTotals.put(category,(float)current);
        }
        if(menu.session()!=null) {
            var sorted=displayedTotals.entrySet().stream().filter(entry->entry.getValue()!=0).sorted(java.util.Comparator.<Map.Entry<String,Float>>comparingDouble(Map.Entry::getValue).reversed()).toList();
            for(int i=0;i<sorted.size();i++) {
                var row=sorted.get(i);int current=Math.round(row.getValue()),target=totals.getOrDefault(row.getKey(),0);
                if(!increased.contains(row.getKey()))continue;
                String text=current+"%"+(i>menu.session().penaltyStart()?" (-"+current/3+")":"");
                var random=minecraft.player.getRandom();
                for(int n=0;n<2;n++)sparkles.add(random,leftPos+276+random.nextFloat()*font.width(text),topPos+20+random.nextFloat()*8+i*18+(i>menu.session().penaltyStart()?4:0),(float)random.nextGaussian()*.5F,(float)random.nextGaussian()*.5F);
            }
        }
    }

    private void renderAnimatedCard(GuiGraphics graphics,TheoryCard card,int index,int count) {
        float out=animation.deal(index),in=animation.zoomIn(index),hover=animation.hover(index);
        float x=65+(183-55*count+110*index-65)*out;
        if(animation.selected(index))x+=(191-x)*in;
        float alpha=animation.phase()!=TheoryCardAnimation.Phase.RESOLVE||animation.selected(index)?1:1-in;
        drawSheet(graphics,card,x,100,6+out*2-in*2+hover,new java.util.Random(card.seed()),alpha,Math.max(1-out,in));
    }

    private void renderBonus(GuiGraphics graphics, TheorySession session) {
        for(int i=0;i<session.bonusDraws();i++)graphics.blit(BASE,leftPos+15+i*2,topPos+150+i,64,96,16,16,256,256);
    }

    private void playPaper(String key, float volume) {
        if (minecraft != null) minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                net.minecraft.sounds.SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft", key)), 1, volume));
    }

    private static ItemStack aidIcon(String key) {
        var block=TheoryAids.block(key);
        // BETA26 renders only ItemStack/Block aid objects. Portals have no BlockItem,
        // and the Crimson portal is an Entity class: all three retain empty icons.
        return block==null||block.asItem()==Items.AIR?ItemStack.EMPTY:new ItemStack(block);
    }

    int resolvingAnimationsForSmokeTest() { requireSmokeTest(); return resolvingAnimations; }
    int drawnAnimationsForSmokeTest() { requireSmokeTest(); return drawnAnimations; }
    boolean animationBusyForSmokeTest() { requireSmokeTest(); return animation.busy(); }
    int aidIconsForSmokeTest() { requireSmokeTest(); return (int)java.util.Arrays.stream(aidButtons).filter(b -> b.visible).count(); }
    boolean hoverRaisedForSmokeTest(int index) { requireSmokeTest(); return animation.hover(index) > .2F; }
    int[] cardPointForSmokeTest(int index) { requireSmokeTest(); return realPointForSmokeTest(leftPos + cardX(index, menu.session().choices().size()) + 20, topPos + 86); }

    void senderForSmokeTest(RequestSink sink) {
        requireSmokeTest();
        previewSender = sink;
    }
    void applyForSmokeTest(CompoundTag state) {
        requireSmokeTest();
        menu.setState(state);
        if (state.contains("Result")) previewPending = false;
        updateControls();
    }
    void actionForSmokeTest(TheoryNetwork.Action action, int index) { requireSmokeTest(); submit(action, index); }
    boolean clickActionForSmokeTest(TheoryNetwork.Action action, int index) {
        requireSmokeTest();
        updateControls();
        if(action==TheoryNetwork.Action.DRAW||action==TheoryNetwork.Action.DRAW_BONUS||action==TheoryNetwork.Action.SELECT) {
            if(!canRequest(action,index))return false;
            int[] point=action==TheoryNetwork.Action.SELECT?cardPointForSmokeTest(index):realPointForSmokeTest(leftPos+65,topPos+100);
            return mouseClicked(point[0],point[1],0);
        }
        TheoryButton control = switch (action) {
            case START -> start;
            case DRAW -> draw;
            case DRAW_BONUS -> bonus;
            case FINISH -> finish;
            case SCRAP -> scrap;
            case SELECT -> choose[index];
        };
        if (!control.visible || !control.active) return false;
        float scale = layoutScale();
        double x = width * (1 - scale) / 2F + (control.getX() + control.getWidth() / 2D) * scale;
        double y = height * (1 - scale) / 2F + (control.getY() + control.getHeight() / 2D) * scale;
        return mouseClicked(x, y, 0);
    }
    boolean clickAidForSmokeTest(String key) {
        requireSmokeTest(); updateControls();
        int index = AID_KEYS.indexOf(key);
        if (index < 0 || !aidButtons[index].visible || !aidButtons[index].active) return false;
        TheoryAidButton control = aidButtons[index];
        int[] point = realPointForSmokeTest(control.getX() + control.getWidth() / 2, control.getY() + control.getHeight() / 2);
        return mouseClicked(point[0], point[1], 0);
    }
    int[] hoverForSmokeTest(boolean requiredItem) {
        requireSmokeTest();
        int count = menu.session().choices().size();
        int x = requiredItem ? requiredItemX(cardX(0, count), cardWidth(count), menu.session().choices().get(0).requiredItems().size(), 0) + 6 : cardX(0, count) + 20;
        return realPointForSmokeTest(leftPos + x, topPos + (requiredItem ? 142 : 86));
    }
    private int[] realPointForSmokeTest(int x, int y) {
        float scale = layoutScale();
        return new int[]{Math.round(width * (1 - scale) / 2F + x * scale), Math.round(height * (1 - scale) / 2F + y * scale)};
    }
    Set<String> selectedAidsForSmokeTest() { return Set.copyOf(selectedAids); }
    boolean availableForSmokeTest(TheoryNetwork.Action action, int index) { return canRequest(action, index); }
    boolean pendingForSmokeTest() { return pending(); }
    boolean layoutFitsForSmokeTest() { return imageWidth * layoutScale() <= width - 11 && imageHeight * layoutScale() <= height - 11; }
    boolean slotHitboxesForSmokeTest() {
        float scale = layoutScale();
        for (var slot : menu.slots) {
            double x = width * (1 - scale) / 2F + (leftPos + slot.x + 8) * scale;
            double y = height * (1 - scale) / 2F + (topPos + slot.y + 8) * scale;
            if (!isHovering(slot.x, slot.y, 16, 16, localX(x), localY(y))) return false;
        }
        return true;
    }
    private static void requireSmokeTest() { if (!Boolean.getBoolean("thaumcraft.theorySmokeTest") && !Boolean.getBoolean("thaumcraft.theoryCompleteSmokeTest") && !Boolean.getBoolean("thaumcraft.visualGuiSmokeTest")) throw new IllegalStateException("Theory smoke preview is disabled"); }
}
