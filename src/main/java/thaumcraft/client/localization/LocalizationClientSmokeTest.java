package thaumcraft.client.localization;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import net.minecraft.client.CameraType;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import thaumcraft.alchemy.AspectCrystalItem;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IEssentiaContainerItem;
import thaumcraft.artifice.construct.BoreEntity;
import thaumcraft.artifice.construct.BoreMenu;
import thaumcraft.artifice.construct.client.BoreScreen;
import thaumcraft.catalog.CatalogModule;
import thaumcraft.catalog.entities.VisualEntitiesModule;
import thaumcraft.client.research.ResearchClient;
import thaumcraft.client.research.ThaumonomiconPageScreen;
import thaumcraft.client.research.ThaumonomiconScreen;
import thaumcraft.client.theory.ResearchTableScreen;
import thaumcraft.common.lib.enchantment.EnumInfusionEnchantment;
import thaumcraft.equipment.RechargeSupport;
import thaumcraft.research.KnowledgeStore;
import thaumcraft.research.PlayerKnowledge;
import thaumcraft.research.ResearchNetwork;
import thaumcraft.research.ResearchCatalog;
import thaumcraft.research.ResearchEvents;
import thaumcraft.research.theory.ResearchTableMenu;
import thaumcraft.research.theory.TheoryCard;

/** Opt-in localization QA. Raw knowledge, detached card offers and enchanted Bore are explicit fixtures. */
@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public final class LocalizationClientSmokeTest {
    private static final String WORLD="thaumcraft-localization-smoke-"+System.currentTimeMillis();
    private static final String[] LOCALES={"en_us","ru_ru"};
    private static final List<String> STUDIES=List.of("FIRSTSTEPS","KNOWLEDGETYPES","THEORYRESEARCH","BASICTURRET");
    private static final String[] IMAGES={"research-map","firststeps","theory-cards","bore-gui","dream-journal"};
    private static final Pattern PLACEHOLDER=Pattern.compile("%(?:[0-9]+\\$)?[sdf]");
    private static final BlockPos BORE=new BlockPos(0,112,0);
    private static final AtomicInteger SAVED=new AtomicInteger();
    private static final List<String> VERIFIED=new ArrayList<>();
    private static boolean started,prepared,stopped,captured,captureRequested;
    private static int scene,phase,stable,wait,boreId=-1;
    private static long began,worldBegan;
    private static TutorialSteps previousTutorial;
    private static String previousLanguage;
    private static CompletableFuture<Void> work,reload;
    private static PlayerKnowledge knowledge;
    private static volatile ItemStack journal;
    private LocalizationClientSmokeTest(){}
    private static boolean enabled(){return Boolean.getBoolean("thaumcraft.localizationSmokeTest");}
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static ServerPlayer player(Minecraft mc){var result=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());require(result!=null,"Missing owned localization player");return result;}
    private static void submit(Minecraft mc,Runnable action){require(work==null,"Overlapping localization server work");work=new CompletableFuture<>();var pending=work;mc.getSingleplayerServer().execute(()->{try{action.run();pending.complete(null);}catch(Throwable e){pending.completeExceptionally(e);}});}
    private static void clean(String value,String context){require(!value.isBlank()&&!value.contains("Format error")&&!value.contains("FORMAT ERROR")&&!PLACEHOLDER.matcher(value).find(),"Unformatted localization "+context+": "+value);require(!value.matches("^(?:item|block|entity|effect|card|research)\\.[a-zA-Z0-9_.]+$"),"Raw translation key "+context+": "+value);}
    private static void close(Minecraft mc){mc.player.closeContainer();mc.setScreen(null);}
    private static void stage(PlayerKnowledge target,String key){try{var method=PlayerKnowledge.class.getDeclaredMethod("setResearchStage",String.class,int.class);method.setAccessible(true);method.invoke(target,key,ResearchCatalog.get(key).stages().size()+1);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}}

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!enabled()||stopped)return;var mc=Minecraft.getInstance();
        try{
            if(began==0)began=System.nanoTime();require(System.nanoTime()-began<600_000_000_000L,"Localization startup timeout");
            if(!started){start(mc);return;}if(mc.level==null||mc.player==null||mc.getOverlay()!=null||mc.screen instanceof ReceivingLevelScreen)return;
            if(worldBegan==0)worldBegan=System.nanoTime();require(System.nanoTime()-worldBegan<180_000_000_000L,"Localization world timeout scene="+scene+" phase="+phase);
            require(mc.getSingleplayerServer()!=null&&WORLD.equals(mc.getSingleplayerServer().getWorldData().getLevelName()),"Wrong localization world");mc.getToasts().clear();
            if(++wait%100==0)LogUtils.getLogger().info("THAUMCRAFT_LOCALIZATION_CLIENT_HEARTBEAT: scene={} phase={} locale={} screen={}",scene,phase,mc.options.languageCode,mc.screen==null?"none":mc.screen.getClass().getSimpleName());
            if(work!=null){if(!work.isDone())return;work.join();work=null;}
            if(!prepared){prepared=true;submit(mc,()->prepare(mc));return;}
            if(captured){if(SAVED.get()==scene+1&&++stable>=4){scene++;phase=stable=wait=0;captured=captureRequested=false;}return;}
            if(scene==LOCALES.length*IMAGES.length){finish(mc);return;}
            if(!advance(mc)){stable=0;return;}if(++stable>=(scene%IMAGES.length==2?45:12))captureRequested=true;
        }catch(Throwable e){fail(mc,e);}
    }
    private static boolean advance(Minecraft mc)throws Exception{
        int view=scene%IMAGES.length;String locale=LOCALES[scene/IMAGES.length];
        if(view==0&&phase<3){
            if(phase==0){phase=1;close(mc);mc.getLanguageManager().setSelected(locale);mc.options.languageCode=locale;reload=mc.reloadResourcePacks();return false;}
            if(phase==1){if(!reload.isDone())return false;reload.join();reload=null;require(mc.getLanguageManager().getSelected().equals(locale),"Native language selection lost");verifyLocale(mc,locale);phase=2;return false;}
            var received=ResearchClient.golemPressKnowledge();if(!STUDIES.stream().allMatch(received::isResearchCompleteStrict))return false;knowledge=received;phase=3;mc.setScreen(new ThaumonomiconScreen(knowledge,knowledge.scanCount()));return false;
        }
        if(view==0)return mc.screen instanceof ThaumonomiconScreen;
        if(view==1){if(phase==0){var received=ResearchClient.golemPressKnowledge();if(!received.isResearchCompleteStrict("FIRSTSTEPS"))return false;knowledge=received;phase=1;var browser=new ThaumonomiconScreen(knowledge,knowledge.scanCount());mc.setScreen(new ThaumonomiconPageScreen(browser,ResearchCatalog.get("FIRSTSTEPS"),knowledge,knowledge.scanCount()));}return mc.screen instanceof ThaumonomiconPageScreen page&&pageProseReady(page);}
        if(view==2){if(phase==0){phase=1;var menu=ResearchTableMenu.preview(0,mc.player.getInventory(),theoryState(mc));mc.setScreen(new ResearchTableScreen(menu,mc.player.getInventory(),Component.translatable("block.thaumcraft.research_table")));}return mc.screen instanceof ResearchTableScreen table&&table.getMenu().session()!=null&&table.getMenu().session().choices().size()==3;}
        if(view==4){if(phase==0){phase=1;mc.setScreen(new BookViewScreen(BookViewScreen.BookAccess.fromItem(journal.copy())));}return mc.screen instanceof BookViewScreen;}
        if(phase==0){phase=1;close(mc);return false;}
        var bore=mc.level.getEntity(boreId);if(!(bore instanceof BoreEntity entity)||entity.tool().isEmpty())return false;
        if(phase==1){phase=2;mc.gameMode.interact(mc.player,entity,InteractionHand.MAIN_HAND);return false;}
        if(!(mc.screen instanceof BoreScreen)||!(mc.player.containerMenu instanceof BoreMenu menu)||menu.bore()!=entity)return false;
        require(entity.getRefining()==4&&entity.getFortune()==3&&entity.hasSilkTouch(),"Native enchanted Bore labels not active");
        for(String key:List.of("tc.bore.width","tc.bore.depth","tc.bore.speed"))clean(Component.translatable(key,7).getString(),locale+" "+key);
        clean(Component.translatable("tc.bore.properties").getString(),locale+" properties");return true;
    }
    /** Read-only visible native layout observation, not an injected page/clock/knowledge override. */
    private static boolean pageProseReady(ThaumonomiconPageScreen page)throws ReflectiveOperationException{
        var type=ThaumonomiconPageScreen.class;var visible=type.getDeclaredField("visibleStage");visible.setAccessible(true);if(visible.getInt(page)<=0)return false;
        var chaptersField=type.getDeclaredField("chapters");chaptersField.setAccessible(true);var chapters=(List<?>)chaptersField.get(page);var chapterField=type.getDeclaredField("chapter");chapterField.setAccessible(true);int chapter=chapterField.getInt(page);if(chapter<0||chapter>=chapters.size())return false;
        var entry=ResearchCatalog.get("FIRSTSTEPS");var expectedStage=entry.stages().get(entry.stages().size()-1);if(!(chapters.get(chapter) instanceof thaumcraft.research.ResearchEntry.Stage stage)||!stage.text().equals(expectedStage.text()))return false;
        var pagesField=type.getDeclaredField("pages");pagesField.setAccessible(true);var pages=(List<?>)pagesField.get(page);var spreadField=type.getDeclaredField("spread");spreadField.setAccessible(true);int first=spreadField.getInt(page)*2;var shown=new StringBuilder();
        for(int i=first;i<Math.min(first+2,pages.size());i++)for(var piece:(List<?>)pages.get(i))if(piece.getClass().getSimpleName().equals("TextPiece")){var textMethod=piece.getClass().getDeclaredMethod("text");textMethod.setAccessible(true);var text=(net.minecraft.util.FormattedCharSequence)textMethod.invoke(piece);text.accept((index,style,point)->{shown.appendCodePoint(point);return true;});}
        String expected=Component.translatable(expectedStage.text()).getString().replaceAll("<[^>]+>"," ").replaceAll("§.","").replaceAll("\\s+","");String rendered=shown.toString().replaceAll("\\s+","");
        return expected.length()>=80&&rendered.contains(expected.substring(0,80));
    }
    private static void verifyKey(JsonObject local,String key,String context,List<String> missing){if(!local.has(key)||local.get(key).getAsString().isBlank()||!I18n.exists(key))missing.add(context+" -> "+key+" [local="+local.has(key)+",loaded="+I18n.exists(key)+"]");}
    private static void verifyLocale(Minecraft mc,String locale)throws Exception{
        JsonObject local;try(var stream=mc.getResourceManager().open(ResourceLocation.fromNamespaceAndPath("thaumcraft","lang/"+locale+".json"));var reader=new java.io.InputStreamReader(stream,StandardCharsets.UTF_8)){local=JsonParser.parseReader(reader).getAsJsonObject();}
        int items=0,blocks=0,entities=0,effects=0,attributes=0,cards=0;var missing=new ArrayList<String>();
        for(var entry:ForgeRegistries.ITEMS.getEntries())if(entry.getKey().location().getNamespace().equals("thaumcraft")){verifyKey(local,entry.getValue().getDescriptionId(),locale+" item "+entry.getKey().location(),missing);items++;}
        for(var entry:ForgeRegistries.BLOCKS.getEntries())if(entry.getKey().location().getNamespace().equals("thaumcraft")){verifyKey(local,entry.getValue().getDescriptionId(),locale+" block "+entry.getKey().location(),missing);blocks++;}
        for(var entry:ForgeRegistries.ENTITY_TYPES.getEntries())if(entry.getKey().location().getNamespace().equals("thaumcraft")){verifyKey(local,entry.getValue().getDescriptionId(),locale+" entity "+entry.getKey().location(),missing);entities++;}
        for(var entry:ForgeRegistries.MOB_EFFECTS.getEntries())if(entry.getKey().location().getNamespace().equals("thaumcraft")){verifyKey(local,entry.getValue().getDescriptionId(),locale+" effect "+entry.getKey().location(),missing);effects++;}
        for(var entry:ForgeRegistries.ATTRIBUTES.getEntries())if(entry.getKey().location().getNamespace().equals("thaumcraft")){verifyKey(local,entry.getValue().getDescriptionId(),locale+" attribute "+entry.getKey().location(),missing);attributes++;}
        for(var entry:ResearchCatalog.entries()){verifyKey(local,entry.title(),locale+" study "+entry.key(),missing);for(var stage:entry.stages())if(!stage.text().isBlank())verifyKey(local,stage.text(),locale+" stage "+entry.key(),missing);}
        if(!missing.isEmpty())LogUtils.getLogger().error("THAUMCRAFT_LOCALIZATION_MISSING_KEYS: locale={},count={},keys={}",locale,missing.size(),missing);
        require(missing.isEmpty(),"Missing native localization keys for "+locale+" ("+missing.size()+"): "+missing);
        for(String id:TheoryCard.ids()){var card=card(id);clean(card.title().getString(),locale+" "+id+" title");clean(card.description().getString(),locale+" "+id+" description");cards++;}
        Object[] sentinels=new Object[16];java.util.Arrays.fill(sentinels,"17");
        for(var entry:local.entrySet())if(entry.getKey().startsWith("card.")&&(entry.getKey().endsWith(".name")||entry.getKey().endsWith(".text")))clean(Component.translatable(entry.getKey(),sentinels).getString(),locale+" original "+entry.getKey());
        var specimens=new ArrayList<ItemStack>();
        for(int damage:new int[]{0,3,6}){var stack=CatalogModule.stack("primordial_pearl");stack.setDamageValue(damage);specimens.add(stack);}
        for(int type=0;type<3;type++){var stack=CatalogModule.stack("verdant_charm");stack.getOrCreateTag().putByte("type",(byte)type);stack.getOrCreateTag().putInt(RechargeSupport.CHARGE_TAG,100);specimens.add(stack);}
        for(Aspect aspect:Aspect.aspects.values()){specimens.add(AspectCrystalItem.create(aspect));for(String id:List.of("phial_filled","label_filled")){var stack=CatalogModule.stack(id);((IEssentiaContainerItem)stack.getItem()).setAspects(stack,new AspectList().add(aspect,id.equals("phial_filled")?10:1));specimens.add(stack);}}
        for(String id:List.of("caster_basic","grapple_gun","traveller_boots","voidseer_charm")){var stack=CatalogModule.stack(id);if(!stack.isEmpty()){stack.getOrCreateTag().putInt(RechargeSupport.CHARGE_TAG,10);specimens.add(stack);}}
        for(var specimen:specimens){require(!specimen.isEmpty(),"Empty dynamic specimen");clean(specimen.getHoverName().getString(),locale+" dynamic "+specimen.getItem());for(var line:specimen.getTooltipLines(mc.player,TooltipFlag.Default.NORMAL))if(!line.getString().isBlank())clean(line.getString(),locale+" tooltip "+specimen.getItem());}
        for(int page=1;page<=3;page++){String value=Component.translatable("book.start."+page).getString();clean(value,locale+" dream page"+page);require(!value.startsWith("\"")&&!value.endsWith("\"")&&!value.contains("\\n"),"Dream page retains JSON quoting/escaped newline: "+locale+" page "+page);}
        var access=BookViewScreen.BookAccess.fromItem(journal.copy());require(access.getPageCount()==3,"Original dream journal lost pages");for(int page=0;page<3;page++)require(access.getPage(page).getString().equals(Component.translatable("book.start."+(page+1)).getString()),"Native written-book text differs from decoded original");
        VERIFIED.add(locale);LogUtils.getLogger().info("THAUMCRAFT_LOCALIZATION_LOCALE_OK: locale={},items={},blocks={},entities={},effects={},attributes={},actualTheoryCards={},dynamicSpecimens={},loadedResourceKeys={}",locale,items,blocks,entities,effects,attributes,cards,specimens.size(),local.size());
    }
    private static TheoryCard card(String id){
        var tag=new CompoundTag();tag.putString("Id",id);tag.putLong("Seed",100+TheoryCard.ids().indexOf(id));tag.putBoolean("FromAid",true);
        String category=switch(id){case "concentrate","reactions","synthesis"->"ALCHEMY";case "calibrate","mind_over_matter","tinker"->"ARTIFICE";case "measure","channel","infuse"->"INFUSION";case "focus","awareness","spellbinding"->"AUROMANCY";case "sculpting","scripting","synergy"->"GOLEMANCY";case "dark_whispers","glyphs","portal","revelation","realization"->"ELDRITCH";default->"ARTIFICE";};
        tag.putString("Category",category);tag.putString("SourceCategory","ALCHEMY");tag.putString("TargetCategory","ARTIFICE");tag.putInt("Amount",20);tag.putInt("md1",0);tag.putInt("md2",1);
        var parameters=new CompoundTag();var compound=Aspect.getCompoundAspects().get(0);parameters.putString("aspect1",id.equals("synthesis")?compound.getComponents()[0].getTag():compound.getTag());parameters.putString("aspect2",id.equals("synthesis")?compound.getComponents()[1].getTag():Aspect.getCompoundAspects().get(1).getTag());parameters.putString("aspect3",compound.getTag());parameters.putInt("value",20);
        var stack=id.equals("curio")?CatalogModule.stack("curio_arcane"):new ItemStack(id.equals("infuse")?Items.APPLE:Items.ANVIL);parameters.put("stack",stack.save(new CompoundTag()));tag.put("Parameters",parameters);
        var result=TheoryCard.load(tag);require(result!=null,"Invalid explicit card fixture "+id);return result;
    }
    private static CompoundTag theoryState(Minecraft mc){
        var session=new CompoundTag();session.putInt("Version",1);session.putUUID("Owner",mc.player.getUUID());session.putInt("Inspiration",5);session.putInt("InspirationStart",5);var choices=new ListTag();for(String id:List.of("analyze","notation","tinker"))choices.add(card(id).save());session.put("Choices",choices);
        var state=new CompoundTag();state.putBoolean("CanUse",true);state.putInt("InspirationAvailable",5);state.putInt("ExperienceLevel",30);state.put("Knowledge",knowledge.save());state.put("Session",session);return state;
    }
    private static void prepare(Minecraft mc){
        var p=player(mc);var level=p.serverLevel();level.setDayTime(6000);p.setInvulnerable(true);for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)level.getChunk(x,z);
        for(var pos:BlockPos.betweenClosed(-6,111,-6,6,116,6))level.setBlockAndUpdate(pos,pos.getY()==111?Blocks.BEDROCK.defaultBlockState():Blocks.AIR.defaultBlockState());
        var actualKnowledge=KnowledgeStore.get(p);for(String key:STUDIES)stage(actualKnowledge,key);ResearchNetwork.sync(p);
        journal=ResearchEvents.createDreamJournal(p).copy();p.getInventory().clearContent();p.connection.teleport(2.5,112,2.5,135,15);var bore=(BoreEntity)VisualEntitiesModule.LIVING.get("arcane_bore").get().create(level);require(bore!=null,"Missing native Bore");bore.setOwnerId(p.getUUID());bore.setValidSpawn();bore.moveTo(.5,112,.5,180,0);
        var pick=new ItemStack(Items.DIAMOND_PICKAXE);pick.enchant(Enchantments.BLOCK_FORTUNE,3);pick.enchant(Enchantments.SILK_TOUCH,1);EnumInfusionEnchantment.addInfusionEnchantment(pick,EnumInfusionEnchantment.REFINING,4);bore.setTool(pick);require(level.addFreshEntity(bore),"Bore fixture rejected");boreId=bore.getId();
        LogUtils.getLogger().info("THAUMCRAFT_LOCALIZATION_FIXTURES: fresh owned world; four raw completed server studies with authoritative research sync; detached original production theory GUI with three raw initialized offers; raw owned enchanted Bore through real native interaction/menu; no paid acquisition/survival claim");
    }
    @SubscribeEvent public static void rendered(TickEvent.RenderTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!enabled()||stopped||!captureRequested||captured)return;var mc=Minecraft.getInstance();
        try{if(scene%IMAGES.length==1){require(mc.screen instanceof ThaumonomiconPageScreen page&&pageProseReady(page),"First Steps native visible layout lacks original prose at capture");LogUtils.getLogger().info("THAUMCRAFT_LOCALIZATION_BOOK_PROSE_VISIBLE: locale={},actual synced completed FIRSTSTEPS,visible chapter matches original final stage and first80 nonspace prose characters are present on current native spread",LOCALES[scene/IMAGES.length]);}captured=true;captureRequested=false;String name="tc6-localization-"+LOCALES[scene/IMAGES.length]+"-"+IMAGES[scene%IMAGES.length]+".png";var file=new File(new File(mc.gameDirectory,"screenshots"),name);require(!file.exists(),"Refusing to overwrite native localization evidence "+name);Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),message->{if(file.isFile()&&file.length()>0){SAVED.incrementAndGet();LogUtils.getLogger().info("THAUMCRAFT_LOCALIZATION_CLIENT_SMOKE_IMAGE: {}",file.getAbsolutePath());}else mc.execute(()->fail(mc,new AssertionError("Missing fresh localization capture")));});}catch(Throwable e){fail(mc,e);}
    }
    private static void start(Minecraft mc){
        if(mc.screen instanceof AccessibilityOnboardingScreen){mc.options.onboardAccessibility=false;mc.options.save();mc.setScreen(new TitleScreen());return;}if(!(mc.screen instanceof TitleScreen)||mc.getOverlay()!=null)return;
        started=true;previousTutorial=mc.options.tutorialStep;previousLanguage=mc.options.languageCode;mc.options.tutorialStep=TutorialSteps.NONE;mc.getTutorial().stop();mc.options.pauseOnLostFocus=false;mc.options.renderDistance().set(3);mc.options.simulationDistance().set(5);mc.options.guiScale().set(2);mc.options.cloudStatus().set(CloudStatus.OFF);mc.options.setCameraType(CameraType.FIRST_PERSON);mc.options.hideGui=false;mc.resizeDisplay();
        knowledge=new PlayerKnowledge();
        var rules=new GameRules();rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false,null);rules.getRule(GameRules.RULE_DAYLIGHT).set(false,null);rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false,null);
        mc.createWorldOpenFlows().createFreshLevel(WORLD,new LevelSettings(WORLD,GameType.SURVIVAL,false,Difficulty.PEACEFUL,true,rules,WorldDataConfiguration.DEFAULT),new WorldOptions(0x54434c34L,false,false),WorldPresets::createNormalWorldDimensions);
    }
    private static void finish(Minecraft mc){require(SAVED.get()==10&&VERIFIED.equals(List.of(LOCALES)),"Incomplete localization locale/render audit");stopped=true;mc.options.tutorialStep=previousTutorial;mc.options.languageCode=previousLanguage;mc.getLanguageManager().setSelected(previousLanguage);LogUtils.getLogger().info("THAUMCRAFT_LOCALIZATION_RENDER_AUDIT_OK: 10 actual native PNG,English+Russian book graph/prose/original theory GUI/enchanted Bore GUI/three-page dream journal; registry/dynamic card+NBT names checked after actual pack reload; explicit server-knowledge/detached-card/raw-entity fixtures,no survival claim");LogUtils.getLogger().info("THAUMCRAFT_LOCALIZATION_CLIENT_SMOKE_OK: 10 captures; locales=en_us,ru_ru; world={}",WORLD);mc.stop();}
    private static void fail(Minecraft mc,Throwable e){if(stopped)return;stopped=true;if(previousTutorial!=null)mc.options.tutorialStep=previousTutorial;if(previousLanguage!=null){mc.options.languageCode=previousLanguage;mc.getLanguageManager().setSelected(previousLanguage);}LogUtils.getLogger().error("THAUMCRAFT_LOCALIZATION_CLIENT_SMOKE_FAILED",e);mc.stop();}
}
