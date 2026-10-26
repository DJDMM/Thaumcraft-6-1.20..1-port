package thaumcraft.catalog.armor.client;

import com.mojang.logging.LogUtils;
import java.io.File;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thaumcraft.alchemy.AspectCrystalItem;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.auromancy.focus.*;
import thaumcraft.auromancy.table.FocalManipulatorBlockEntity;
import thaumcraft.auromancy.table.client.FocalManipulatorScreen;
import thaumcraft.catalog.CatalogModule;
import thaumcraft.catalog.blocks.CatalogBlocks;
import thaumcraft.research.*;
import thaumcraft.world.aura.AuraManager;

/** Opt-in raw visual fixture. Native walking, actual armor hooks and real editor/start packets. */
@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public final class ArmorVisualClientSmokeTest {
    private static final String WORLD="thaumcraft-armor-visual-"+System.currentTimeMillis();
    private static final BlockPos TABLE=new BlockPos(8,250,0);
    private static final String[] SCENES={"fortress-one","fortress-two","fortress-three",
            "robe-front-wide","robe-front-crossed","robe-rear-wide","robe-rear-crossed",
            "knight-front-wide","knight-front-crossed","knight-rear-wide","knight-rear-crossed",
            "leader-front-wide","leader-front-crossed","leader-rear-wide","leader-rear-crossed",
            "focal-branches","focal-settings","focal-halo-early","focal-halo-late"};
    private static final String[] OUTFITS={"void_robe","crimson_plate","crimson_praetor"};
    private static final AtomicInteger SAVED=new AtomicInteger();
    private static CompletableFuture<Void> work;
    private static TutorialSteps previous;
    private static boolean started,prepared,stopped,capture,captured,ready;
    private static int scene,phase,stable;
    private static long began,haloTick,renderFrame,lastPlayerRenderFrame=-1;
    private static float paidRemaining;
    private ArmorVisualClientSmokeTest(){}
    private static boolean enabled(){return Boolean.getBoolean("thaumcraft.armorVisualSmokeTest");}
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static ServerPlayer player(Minecraft mc){var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());require(p!=null,"Owned visual player absent");return p;}
    private static void submit(Minecraft mc,Runnable action){require(work==null,"Overlapping owned server work");var future=new CompletableFuture<Void>();work=future;mc.getSingleplayerServer().execute(()->{try{action.run();future.complete(null);}catch(Throwable error){future.completeExceptionally(error);}});}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END||!enabled()||stopped)return;
        var mc=Minecraft.getInstance();if(began==0)began=System.nanoTime();
        try {
            require(System.nanoTime()-began<600_000_000_000L,"Armor visual timeout scene="+scene+" phase="+phase);
            if(!started){start(mc);return;}
            if(mc.level==null||mc.player==null||mc.getOverlay()!=null||mc.screen instanceof ReceivingLevelScreen)return;
            require(mc.getSingleplayerServer()!=null&&WORLD.equals(mc.getSingleplayerServer().getWorldData().getLevelName()),"Wrong owned visual world");
            mc.getToasts().clear();if(work!=null){if(!work.isDone())return;work.join();work=null;}
            if(!prepared){prepared=true;submit(mc,()->prepare(mc));return;}
            if(captured&&SAVED.get()>scene){scene++;phase=stable=0;captured=capture=ready=false;}
            if(scene==SCENES.length){finish(mc);return;}
            if(advance(mc)&&++stable>=3)capture=true;
        }catch(Throwable error){fail(mc,error);}
    }
    private static boolean advance(Minecraft mc) {
        if(scene<3) {
            if(phase==0){phase=1;mc.options.keyUp.setDown(false);submit(mc,()->equip(player(mc),"fortress",scene+1));return false;}
            if(!wearing(mc,"fortress",scene+1)||mc.player.distanceToSqr(.5,250,3.5)>.01)return false;
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);return mc.player.tickCount>20;
        }
        if(scene<15) {
            int index=(scene-3)/4,pose=(scene-3)%4;
            if(phase==0) {
                phase=1;
                if(pose==0){mc.options.keyUp.setDown(false);submit(mc,()->equip(player(mc),OUTFITS[index],3));return false;}
            }
            if(!wearing(mc,OUTFITS[index],3))return false;
            mc.options.setCameraType(pose<2?CameraType.THIRD_PERSON_FRONT:CameraType.THIRD_PERSON_BACK);
            mc.options.keyUp.setDown(true);
            float speed=mc.player.walkAnimation.speed(mc.getFrameTime());
            // RenderTick.END gates the actual captured walk phase. Checking it
            // here would accept a different partial frame from the screenshot.
            return speed>.35F;
        }
        if(scene==15) {
            if(phase==0){phase=1;mc.options.keyUp.setDown(false);mc.options.setCameraType(CameraType.FIRST_PERSON);submit(mc,()->{var p=player(mc);for(var slot:new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS})p.setItemSlot(slot,ItemStack.EMPTY);p.connection.teleport(8.5,250,3.5,180,10);p.inventoryMenu.broadcastChanges();});return false;}
            if(mc.player.distanceToSqr(8.5,250,3.5)>.01)return false;
            if(phase==1){phase=2;mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(TABLE.getCenter(),Direction.SOUTH,TABLE,false));return false;}
            if(!(mc.screen instanceof FocalManipulatorScreen screen)||!screen.getMenu().canUse())return false;
            if(phase==2){phase=3;mc.gameMode.handleInventoryMouseClick(screen.getMenu().containerId,1,0,ClickType.PICKUP,mc.player);mc.gameMode.handleInventoryMouseClick(screen.getMenu().containerId,0,0,ClickType.PICKUP,mc.player);return false;}
            if(screen.getMenu().pending()||screen.getMenu().focus().isEmpty()||screen.getMenu().graph().nodes().isEmpty())return false;
            if(phase==3){
                phase=4;
                LogUtils.getLogger().info("THAUMCRAFT_ARMOR_VISUAL_NATIVE_EDITOR_INPUT: nameBlur={} paletteDrag={}",screen.exerciseNameBlurForVisualSmoke(),screen.exercisePaletteDragForVisualSmoke());
                LogUtils.getLogger().info("THAUMCRAFT_ARMOR_VISUAL_NATIVE_EDITOR_INPUT: paletteWheelBounds={}",screen.exercisePaletteWheelBoundsForVisualSmoke());
                // An incomplete editable graph creates genuine nonzero/visible
                // graph scrollbars without paying or executing a focus plan.
                screen.selectForSmoke(1,FocusNodeRegistry.SPLITTRAJECTORY);
                screen.selectForSmoke(2,FocusNodeRegistry.SPLITTRAJECTORY);
                screen.selectForSmoke(4,FocusNodeRegistry.SPLITTRAJECTORY);
                return false;
            }
            if(phase==4){
                if(screen.getMenu().pending()||screen.getMenu().graph().nodes().size()!=8||!screen.graphSlidersReadyForVisualSmoke())return false;
                LogUtils.getLogger().info("THAUMCRAFT_ARMOR_VISUAL_NATIVE_EDITOR_INPUT: temporaryNativeGraphNodes={} graphDrags={}",screen.getMenu().graph().nodes().size(),screen.exerciseGraphDragsForVisualSmoke());
                phase=5;
                // Replace the temporary subtree through the same editor calls,
                // restoring the exact original six-node paid recipe and costs.
                screen.selectForSmoke(1,FocusNodeRegistry.SPLITTRAJECTORY);screen.selectForSmoke(2,FocusNodeRegistry.PROJECTILE);screen.selectForSmoke(4,FocusNodeRegistry.FIRE);screen.selectForSmoke(3,FocusNodeRegistry.BOLT);screen.selectForSmoke(5,FocusNodeRegistry.FROST);
                return false;
            }
            return !screen.getMenu().pending()&&screen.getMenu().graph().nodes().size()==6&&FocusCompiler.compile(screen.getMenu().graph(),screen.getMenu().focus(),screen.getMenu().knowledge()::isResearchCompleteStrict).success();
        }
        if(scene==16) {
            if(!(mc.screen instanceof FocalManipulatorScreen screen))return false;
            if(phase==0){phase=1;require(screen.clickNodeForVisualSmoke(2),"Native graph node click refused");require(screen.clickSettingForVisualSmoke("option",1),"Original list spinner click refused");LogUtils.getLogger().info("THAUMCRAFT_ARMOR_VISUAL_NATIVE_EDITOR_INPUT: sameTickDirtyConfirm={}",screen.exerciseSameTickConfirmGuardForVisualSmoke());require(screen.clickSettingForVisualSmoke("speed",1),"Original range spinner click refused");return false;}
            if(screen.getMenu().pending())return false;
            var node=screen.getMenu().graph().nodes().stream().filter(n->n.id()==2).findFirst().orElse(null);
            return node!=null&&node.settings().get("option")==1&&node.settings().get("speed")==2;
        }
        var table=mc.level.getBlockEntity(TABLE);
        if(!(table instanceof FocalManipulatorBlockEntity focal))return false;
        if(scene==17) {
            if(phase==0){if(!(mc.screen instanceof FocalManipulatorScreen screen))return false;if(!screen.clickConfirmForVisualSmoke())return false;phase=1;return false;}
            if(!focal.crafting())return false;
            if(phase==1){phase=2;mc.player.closeContainer();mc.setScreen(null);submit(mc,()->{var p=player(mc);var actual=(FocalManipulatorBlockEntity)p.serverLevel().getBlockEntity(TABLE);require(actual.crafting()&&!actual.orbitCrystals().isEmpty(),"No actual paid table crystals");paidRemaining=actual.remainingVis();require(p.experienceLevel<100,"Actual start did not pay XP");p.connection.teleport(8.5,250,4.5,180,12);});return false;}
            if(mc.player.distanceToSqr(8.5,250,4.5)>.01||mc.screen!=null||focal.orbitCrystals().size()<4)return false;
            if(haloTick==0)haloTick=mc.level.getGameTime();return mc.level.getGameTime()-haloTick>=8;
        }
        require(focal.crafting()&&!focal.orbitCrystals().isEmpty(),"Paid native halo expired before second scene");
        return mc.level.getGameTime()-haloTick>=53;
    }
    private static boolean wearing(Minecraft mc,String prefix,int count) {
        return mc.player.getItemBySlot(EquipmentSlot.CHEST).is(item(prefix+"_chest").getItem())
                &&(count<2?mc.player.getItemBySlot(EquipmentSlot.LEGS).isEmpty():mc.player.getItemBySlot(EquipmentSlot.LEGS).is(item(prefix+"_legs").getItem()))
                &&(count<3?mc.player.getItemBySlot(EquipmentSlot.HEAD).isEmpty():mc.player.getItemBySlot(EquipmentSlot.HEAD).is(item(prefix+"_helm").getItem()));
    }
    private static ItemStack item(String id){var stack=CatalogModule.stack(id);require(!stack.isEmpty(),"Raw visual fixture missing "+id);return stack;}
    private static void equip(ServerPlayer p,String prefix,int count) {
        p.setItemSlot(EquipmentSlot.CHEST,item(prefix+"_chest"));p.setItemSlot(EquipmentSlot.LEGS,count>=2?item(prefix+"_legs"):ItemStack.EMPTY);p.setItemSlot(EquipmentSlot.HEAD,count>=3?item(prefix+"_helm"):ItemStack.EMPTY);
        p.connection.teleport(.5,250,3.5,0,0);p.inventoryMenu.broadcastChanges();
    }
    private static void prepare(Minecraft mc) {
        var p=player(mc);var level=p.serverLevel();p.setInvulnerable(true);
        for(var pos:BlockPos.betweenClosed(-16,249,-16,16,259,28))level.setBlockAndUpdate(pos,pos.getY()==249?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState());
        // Native walking remains active while waiting for a phase. The owned
        // corridor prevents a slow rendering machine from walking off the set.
        for(var pos:BlockPos.betweenClosed(-3,249,29,3,254,128))level.setBlockAndUpdate(pos,pos.getY()==249?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState());
        level.setDayTime(4*24000L+12000L);level.setBlockAndUpdate(TABLE,CatalogBlocks.block("wand_workbench").defaultBlockState());
        p.getInventory().clearContent();p.getInventory().setItem(9,item("focus_3"));p.experienceLevel=100;
        Aspect[] aspects={Aspect.MOTION,Aspect.ENERGY,Aspect.FIRE,Aspect.COLD};
        for(int i=0;i<aspects.length;i++)p.getInventory().setItem(10+i,AspectCrystalItem.create(aspects[i],8));
        for(String key:new String[]{"BASEAUROMANCY","FOCUSBOLT","FOCUSPROJECTILE","FOCUSELEMENTAL","FOCUSSPLIT"})grant(p,key);
        AuraManager.drainVis(level,TABLE,Float.MAX_VALUE,false);AuraManager.addVis(level,TABLE,500);
        p.getInventory().selected=0;p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.connection.send(new ClientboundSetCarriedItemPacket(0));p.connection.teleport(.5,250,3.5,0,0);p.inventoryMenu.broadcastChanges();
        LogUtils.getLogger().info("THAUMCRAFT_ARMOR_VISUAL_FIXTURES: rawArmorAndGreaterFocus/rawCompletedParentResearch100XP8EachCrystal/ownedY250StoneArena/initial500Aura/notPaidResearchOrArmorRoute/noWalkPoseMotionInjection/noActiveAuraOrClockReset");
    }
    private static void grant(ServerPlayer p,String key) {try{var method=PlayerKnowledge.class.getDeclaredMethod("setResearchStage",String.class,int.class);method.setAccessible(true);method.invoke(KnowledgeStore.get(p),key,ResearchCatalog.get(key).stages().size()+1);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}}
    private static Object field(Object target,String name)throws ReflectiveOperationException{Field f=target.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(target);}
    private static ModelPart part(Object mesh,String name)throws ReflectiveOperationException{return (ModelPart)field(field(mesh,name),"part");}
    private static Map<String,Object> auditArmor(Minecraft mc)throws ReflectiveOperationException {
        var extension=IClientItemExtensions.of(mc.player.getItemBySlot(EquipmentSlot.CHEST));require(extension instanceof CatalogArmorExtension,"Custom original armor extension absent");
        Object mesh=((Map<?,?>)field(extension,"geometry")).get(false);require(mesh!=null,"No actual rendered outer armor mesh");
        var result=new LinkedHashMap<String,Object>();result.put("model",mesh.getClass().getSimpleName());
        if(scene<3) {
            String[] two={"ShoulderplateLtop","ShoulderplateL1","ShoulderplateRtop","ShoulderplateR1","SidepanelR2","SidepanelL2"};
            String[] three={"ShoulderplateL2","ShoulderplateL3","ShoulderplateR2","ShoulderplateR3","SidepanelR3","SidepanelL3"};
            for(var names:List.of(two,three))for(String name:names){boolean visible=part(mesh,name).visible;int count=names==two?2:3;require(visible==(scene+1>=count),"Original partial-set visibility differs "+name);result.put(name,visible);}
        } else {
            float partial=mc.getFrameTime(),swing=mc.player.walkAnimation.position(partial),amount=Math.min(1,mc.player.walkAnimation.speed(partial));
            float a=net.minecraft.util.Mth.cos(swing*.6662F)*1.4F*amount,b=net.minecraft.util.Mth.cos(swing*.6662F+3.1415927F)*1.4F*amount,c=Math.min(a,b);
            if(scene<7){check(mesh,"FrontclothR1",c-.1047198F,result);check(mesh,"FrontclothR2",c-.3316126F,result);check(mesh,"ClothBackR1",-c+.1047198F,result);check(mesh,"ClothBackR2",-c+.2268928F,result);}
            else if(scene<11){check(mesh,"Frontcloth1",c-.1047198F,result);check(mesh,"Frontcloth2",c-.3316126F,result);check(mesh,"Cloak1",-c/2+.1396263F,result);}
            else {check(mesh,"LegClothR",a-.1047198F,result);check(mesh,"LegClothL",b-.1047198F,result);check(mesh,"Cloak1",-c/2+.1396263F,result);}
            result.put("nativeLimbSwing",swing);result.put("nativeLimbAmount",amount);result.put("position",mc.player.position());
        }return result;
    }
    private static void check(Object mesh,String name,float expected,Map<String,Object> result)throws ReflectiveOperationException {float actual=part(mesh,name).xRot;require(Math.abs(actual-expected)<.0002F,"Native original cloth formula differs "+name+" actual="+actual+" expected="+expected);result.put(name,actual);}
    @SubscribeEvent public static void playerRendered(RenderPlayerEvent.Post event) {
        if(!enabled()||stopped)return;
        var mc=Minecraft.getInstance();
        if(event.getEntity()==mc.player&&mc.screen==null&&mc.getOverlay()==null
                &&!mc.options.getCameraType().isFirstPerson()&&mc.gameRenderer.getMainCamera().isDetached())
            lastPlayerRenderFrame=renderFrame;
    }
    @SubscribeEvent public static void rendered(TickEvent.RenderTickEvent event) {
        if(!enabled()||stopped)return;
        if(event.phase==TickEvent.Phase.START){renderFrame++;return;}
        if(!capture||captured)return;
        var mc=Minecraft.getInstance();try {
            // After the high-Y teleport, client destination sections may still
            // be compiling. A few elapsed ticks do not prove player rendering.
            // Require the real native player/layers to have completed this
            // exact world frame, then retain all actual mesh assertions.
            if(scene<15&&(mc.screen!=null||mc.getOverlay()!=null||lastPlayerRenderFrame!=renderFrame))return;
            if(scene>=3&&scene<15) {
                float partial=mc.getFrameTime(),amount=mc.player.walkAnimation.speed(partial);
                float wave=Math.abs(net.minecraft.util.Mth.cos(mc.player.walkAnimation.position(partial)*.6662F));
                if(amount<=.35F||((scene-3)%2==0?wave<=.9F:wave>=.18F))return;
            }
            require(mc.getWindow().getScreenWidth()==1600&&mc.getWindow().getScreenHeight()==1000,"Expected full1600x1000 native frame");
            Object facts=scene<15?auditArmor(mc):scene<17?((FocalManipulatorScreen)mc.screen).draftForSmoke():((FocalManipulatorBlockEntity)mc.level.getBlockEntity(TABLE)).orbitCrystals();
            captured=true;capture=false;String name="tc6-armor-visual-"+SCENES[scene]+".png";
            var file=new File(new File(mc.gameDirectory,"screenshots"),name);require(!file.exists(),"Refusing native image overwrite");
            LogUtils.getLogger().info("THAUMCRAFT_ARMOR_VISUAL_NATIVE_FRAME: name={} camera={} cameraType={} nativePlayerRenderFrame={}/{} facts={}",name,mc.gameRenderer.getMainCamera().getPosition(),mc.options.getCameraType(),lastPlayerRenderFrame,renderFrame,facts);
            Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),message->{if(file.isFile()&&file.length()>0){SAVED.incrementAndGet();LogUtils.getLogger().info("THAUMCRAFT_ARMOR_VISUAL_CLIENT_SMOKE_IMAGE: {}",file.getAbsolutePath());}else mc.execute(()->fail(mc,new AssertionError("Native screenshot missing")));});
        }catch(Throwable error){fail(mc,error);}
    }
    private static void start(Minecraft mc) {
        if(mc.screen instanceof AccessibilityOnboardingScreen){mc.options.onboardAccessibility=false;mc.options.save();mc.setScreen(new TitleScreen());return;}
        if(!(mc.screen instanceof TitleScreen)||mc.getOverlay()!=null)return;
        started=true;previous=mc.options.tutorialStep;mc.options.tutorialStep=TutorialSteps.NONE;mc.getTutorial().stop();
        mc.options.pauseOnLostFocus=false;mc.options.renderDistance().set(4);mc.options.simulationDistance().set(5);mc.options.guiScale().set(2);mc.options.fov().set(50);mc.options.bobView().set(false);mc.options.cloudStatus().set(CloudStatus.OFF);mc.options.graphicsMode().set(GraphicsStatus.FANCY);mc.options.hideGui=true;mc.resizeDisplay();
        var rules=new GameRules();rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false,null);rules.getRule(GameRules.RULE_DAYLIGHT).set(false,null);rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false,null);rules.getRule(GameRules.RULE_RANDOMTICKING).set(0,null);
        mc.createWorldOpenFlows().createFreshLevel(WORLD,new LevelSettings(WORLD,GameType.SURVIVAL,false,Difficulty.NORMAL,true,rules,WorldDataConfiguration.DEFAULT),new WorldOptions(0x544336041L,false,false),WorldPresets::createNormalWorldDimensions);
    }
    private static void finish(Minecraft mc) {
        mc.options.keyUp.setDown(false);require(SAVED.get()==19,"Native armor/GUI frames incomplete");
        stopped=true;mc.options.tutorialStep=previous;
        LogUtils.getLogger().info("THAUMCRAFT_ARMOR_VISUAL_RENDER_AUDIT_OK: 19 captures/3FortressPartialSets12Conditions/12NativeWalkFrontRearPhases3OriginalModels/sourceFocalWidgetsAnd2PaidOrbitPhases/realNameFocusBlurAnd3ScreenClickDragReleaseScrollbars/pixelAppearanceRequiresActualReview");
        LogUtils.getLogger().info("THAUMCRAFT_ARMOR_VISUAL_CLIENT_SMOKE_OK: 19 captures/rawVisualFixtures/nativeArmorHooks/noInjectedWalk/nativeEditorSpinnerAndPaidStartC2S/initialRemainingVis{},world={}",paidRemaining,WORLD);mc.stop();
    }
    private static void fail(Minecraft mc,Throwable error){if(stopped)return;stopped=true;mc.options.keyUp.setDown(false);if(previous!=null)mc.options.tutorialStep=previous;LogUtils.getLogger().error("THAUMCRAFT_ARMOR_VISUAL_CLIENT_SMOKE_FAILED",error);mc.stop();}
}
