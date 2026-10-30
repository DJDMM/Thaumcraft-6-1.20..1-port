package thaumcraft.artifice.late.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import java.io.File;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.*;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import thaumcraft.alchemy.AspectCrystalItem;
import thaumcraft.api.aspects.*;
import thaumcraft.arcane.*;
import thaumcraft.artifice.late.*;
import thaumcraft.catalog.blocks.CatalogBlocks;
import thaumcraft.equipment.cleansing.CleansingModule;
import thaumcraft.essentia.EssentiaJarBlockEntity;
import thaumcraft.essentia.transport.TubeBlockEntity;
import thaumcraft.fx.client.*;
import thaumcraft.research.*;
import thaumcraft.world.aura.AuraManager;

/** Opt-in owned native world and menus. Raw initial resources are fixtures, never active clocks or FX. */
@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public final class LateDevicesClientSmokeTest {
    private static final String WORLD="thaumcraft-late-devices-"+System.currentTimeMillis();
    private static final String[] IMAGES={"charger-native-nine-chunks","charger-delegated-bench-preview","charger-paid-spa-result",
        "spa-original-water-salt-gui","spa-native-mix-button","spa-native-paid-bath","urn-native-aura-refill",
        "urn-native-capability-water-stream","sprayer-original-potion-gui","sprayer-native-essentia-progress",
        "sprayer-native-one-charge-gui","sprayer-native-rising-edge-vent2","sprayer-native-held-power",
        "travel-native-speed-jump","barrier-native-mob-runes","barrier-native-player-passage","barrier-native-powered-runes"};
    private static final BlockPos BENCH=new BlockPos(-8,112,3),SPA=new BlockPos(-2,112,3),SPA_LEVER=SPA.east(),
        URN=new BlockPos(-10,113,-7),RECEIVER=URN.offset(-2,-1,-1),SPRAYER=new BlockPos(7,113,-2),
        SPRAY_LEVER=SPRAYER.south(),TRAVEL=new BlockPos(0,112,-11),BARRIER=new BlockPos(8,112,-12),BARRIER_LEVER=BARRIER.south();
    private static final AtomicInteger SAVED=new AtomicInteger();
    private static final List<BlockPos> JARS=new ArrayList<>(),TUBES=new ArrayList<>();
    private static final Map<BlockPos,Integer> INITIAL_ESSENTIA=new LinkedHashMap<>();
    private static CompletableFuture<Void> work;
    private static TutorialSteps previous;
    private static boolean started,prepared,stopped,captureRequested,captured,validated,walk;
    private static volatile int scene;
    private static int phase,stable,ready,heartbeat,renderFrame,lastFxFrame=-1,waterVertices,waterClear,ventQuads,runeQuads,initialClientTicks;
    private static volatile boolean benchPaid,bathPaid,urnRefilled,waterPaid,sprayPaid,heldPaid,travelPaid,barrierPaid,playerPassed,powerPassed;
    private static volatile int urnAmount,receiverAmount,sprayCharges,progressAmount,jarAmount,health,travelActor=-1,sprayActor=-1,barrierActor=-1,refills,transfers;
    private static volatile long worldTime;
    private static int previousUrn,previousReceiver;private static float previousUrnVis,poolBefore;private static boolean monitorUrn;
    private static int initialRecipeUnits;private static long began,actionTick,initialWorldTick=-1;private static String visualFacts="";
    private static int positionScene=-1,positionPhase=-1;private static Vec3 expectedPosition;
    private static long positionSince,lastPositionAudit=-20;private static volatile String serverPositionFacts="not yet observed";
    private static boolean waterPathReady;private static String waterPathFacts="not yet observed";private static long lastWaterAudit=-20;
    private static long lastTravelClientAudit=-20,lastTravelServerAudit=-20,lastBarrierClientAudit=-20,lastBarrierServerAudit=-20;
    private static volatile String serverTravelFacts="not yet observed",serverBarrierFacts="not yet observed";
    private static volatile Vec3 travelInitialPosition=Vec3.ZERO;private static boolean travelClientObserved;
    private LateDevicesClientSmokeTest(){}
    private static boolean enabled(){return Boolean.getBoolean("thaumcraft.lateDevicesSmokeTest");}
    private static void require(boolean ok,String text){if(!ok)throw new AssertionError(text);}
    private static ResourceLocation id(String key){return ResourceLocation.fromNamespaceAndPath("thaumcraft",key);}
    private static ItemStack item(String key){var value=ForgeRegistries.ITEMS.getValue(id(key));require(value!=null&&value!=Items.AIR,"Missing late-device item "+key);return new ItemStack(value);}
    private static ServerPlayer player(Minecraft mc){var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());require(p!=null,"Owned late-device player missing");return p;}
    private static void submit(Minecraft mc,Runnable action){require(work==null,"Overlapping late-device server tasks");var f=new CompletableFuture<Void>();work=f;mc.getSingleplayerServer().execute(()->{try{action.run();f.complete(null);}catch(Throwable error){f.completeExceptionally(error);}});}
    private static void rawStudy(ServerPlayer p,String key){try{var m=PlayerKnowledge.class.getDeclaredMethod("setResearchStage",String.class,int.class);m.setAccessible(true);m.invoke(KnowledgeStore.get(p),key,ResearchCatalog.get(key).stages().size()+1);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}}
    private static void stow(ServerPlayer p){var stack=p.getMainHandItem();if(!stack.isEmpty()){p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);for(int i=1;i<36&&!stack.isEmpty();i++){var slot=p.getInventory().getItem(i);if(!slot.isEmpty()&&ItemStack.isSameItemSameTags(slot,stack)){int n=Math.min(stack.getCount(),slot.getMaxStackSize()-slot.getCount());slot.grow(n);stack.shrink(n);}}for(int i=1;i<36&&!stack.isEmpty();i++)if(p.getInventory().getItem(i).isEmpty()){p.getInventory().setItem(i,stack);stack=ItemStack.EMPTY;}require(stack.isEmpty(),"Cannot conserve physical held item");}p.inventoryMenu.broadcastChanges();}
    private static void held(ServerPlayer p,ItemStack stack){require(p.getInventory().selected==0,"Owned selected slot drifted");stow(p);p.setItemInHand(InteractionHand.MAIN_HAND,stack);p.connection.send(new ClientboundSetCarriedItemPacket(0));p.inventoryMenu.broadcastChanges();}
    private static int count(ServerPlayer p,String key){int count=0;for(var stack:p.getInventory().items)if(stack.is(item(key).getItem()))count+=stack.getCount();return count;}
    private static ItemStack take(ServerPlayer p,String key){for(int i=0;i<36;i++){var stack=p.getInventory().getItem(i);if(stack.is(item(key).getItem())){var result=stack.split(1);if(stack.isEmpty())p.getInventory().setItem(i,ItemStack.EMPTY);return result;}}throw new AssertionError("Missing physically paid "+key);}
    private static void close(Minecraft mc){mc.player.closeContainer();mc.setScreen(null);}
    private static void click(Minecraft mc,BlockPos at,Direction side){mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(at.getCenter().add(Vec3.atLowerCornerOf(side.getNormal()).scale(.5)),side,at,false));}
    private static boolean position(Minecraft mc,double x,double y,double z){
        var expected=new Vec3(x,y,z);long now=mc.level.getGameTime();
        if(positionScene!=scene||positionPhase!=phase||!expected.equals(expectedPosition)){
            positionScene=scene;positionPhase=phase;expectedPosition=expected;positionSince=now;ready=0;serverPositionFacts="not yet observed for this position";
        }
        if(mc.player.distanceToSqr(x,y,z)>.15){
            ready=0;var expectedSupport=BlockPos.containing(x,y-1,z);var actualSupport=mc.player.blockPosition().below();
            require(now-positionSince<160,"Native fixture position did not settle within160ticks: expected="+expected+", client="+mc.player.position()+", clientGround="+mc.player.onGround()+", expectedSupport="+mc.level.getBlockState(expectedSupport)+", actualSupport="+mc.level.getBlockState(actualSupport)+", server="+serverPositionFacts);
            if(now-lastPositionAudit>=20&&work==null){
                lastPositionAudit=now;LogUtils.getLogger().info("THAUMCRAFT_LATE_DEVICES_POSITION_CLIENT: scene={} phase={} expected={} actual={} ground={} expectedSupport={} actualSupport={}",scene,phase,expected,mc.player.position(),mc.player.onGround(),mc.level.getBlockState(expectedSupport),mc.level.getBlockState(actualSupport));
                submit(mc,()->{var p=player(mc);var l=p.serverLevel();serverPositionFacts="position="+p.position()+"/ground="+p.onGround()+"/expectedSupport="+l.getBlockState(expectedSupport)+"/actualSupport="+l.getBlockState(p.blockPosition().below());LogUtils.getLogger().info("THAUMCRAFT_LATE_DEVICES_POSITION_SERVER: scene={} phase={} expected={} {}",scene,phase,expected,serverPositionFacts);});
            }
            return false;
        }
        return ++ready>=4;
    }
    private static void cameraSupport(ServerLevel level,double x,double y,double z){
        var support=BlockPos.containing(x,y-1,z);var state=level.getBlockState(support);
        require(state.is(Blocks.STONE_BRICKS)&&state.isFaceSturdy(level,support,Direction.UP),"Declared ground camera lacks exact native stone-brick support: feet="+new Vec3(x,y,z)+", support="+support+", state="+state);
    }
    private static void view(ServerPlayer p,double x,double y,double z,Vec3 target){stow(p);Vec3 d=target.subtract(x,y+p.getEyeHeight(),z);p.connection.teleport(x,y,z,(float)Math.toDegrees(Math.atan2(d.z,d.x))-90,(float)-Math.toDegrees(Math.atan2(d.y,Math.sqrt(d.x*d.x+d.z*d.z))));}
    private static Cow cow(ServerPlayer p,BlockPos at){var c=EntityType.COW.create(p.serverLevel());require(c!=null,"Missing raw Cow");c.setNoAi(true);c.setPersistenceRequired();c.setPos(at.getX()+.5,at.getY(),at.getZ()+.5);require(p.serverLevel().addFreshEntity(c),"Raw native Cow insertion rejected");return c;}
    private static ArcaneWorkbenchBlockEntity bench(ServerLevel l){return (ArcaneWorkbenchBlockEntity)l.getBlockEntity(BENCH);}
    private static SpaBlockEntity spa(ServerLevel l){return (SpaBlockEntity)l.getBlockEntity(SPA);}
    private static PotionSprayerBlockEntity sprayer(ServerLevel l){return (PotionSprayerBlockEntity)l.getBlockEntity(SPRAYER);}
    private static float pool(ServerLevel l){float n=0;for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)n+=AuraManager.getVis(l,BENCH.offset(16*x,0,16*z));return n;}
    private static int integerPool(ServerLevel l){int n=0;for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)n+=(int)AuraManager.getVis(l,BENCH.offset(16*x,0,16*z));return n;}
    private static void lever(ServerLevel l,BlockPos at){l.setBlockAndUpdate(at,Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE,AttachFace.FLOOR).setValue(LeverBlock.FACING,Direction.NORTH));}
    private static boolean elapsed(int ticks){return worldTime-actionTick>=ticks;}

    @SubscribeEvent public static void input(MovementInputUpdateEvent e){if(!enabled()||stopped||!walk)return;var mc=Minecraft.getInstance();if(e.getEntity()!=mc.player||mc.getSingleplayerServer()==null||!WORLD.equals(mc.getSingleplayerServer().getWorldData().getLevelName()))return;var in=e.getInput();in.forwardImpulse=1;in.leftImpulse=0;in.up=true;in.down=in.left=in.right=in.jumping=in.shiftKeyDown=false;}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(e.phase!=TickEvent.Phase.END||!enabled()||stopped)return;var mc=Minecraft.getInstance();if(began==0)began=System.nanoTime();try{
            require(System.nanoTime()-began<1_200_000_000_000L,"Late-device timeout scene="+scene+" phase="+phase);
            if(!started){start(mc);return;}if(mc.level==null||mc.player==null||mc.getOverlay()!=null||mc.screen instanceof ReceivingLevelScreen)return;
            require(mc.getSingleplayerServer()!=null&&WORLD.equals(mc.getSingleplayerServer().getWorldData().getLevelName()),"Wrong owned late-device world");mc.getToasts().clear();
            if(++heartbeat%100==0)LogUtils.getLogger().info("THAUMCRAFT_LATE_DEVICES_HEARTBEAT: scene={} phase={} urn={} receiver={} charges={} progress={} source={} health={} waterVertices={} clear={} ventQuads={} runeQuads={} facts={}",scene,phase,urnAmount,receiverAmount,sprayCharges,progressAmount,jarAmount,health,waterVertices,waterClear,ventQuads,runeQuads,visualFacts);
            if(work!=null){if(!work.isDone())return;work.join();work=null;}
            if(!prepared){prepared=true;submit(mc,()->prepare(player(mc)));return;}
            if(captured){if(SAVED.get()==scene+1&&++stable>=3){scene++;phase=stable=ready=0;captureRequested=captured=false;lastFxFrame=-1;}return;}
            if(scene==IMAGES.length){finish(mc);return;}if(!advance(mc)){stable=0;return;}
            if(scene==7||scene==9||scene==11||scene==14||scene==16||++stable>=10)captureRequested=true;
        }catch(Throwable error){fail(mc,error);}
    }
    private static boolean initialReady(Minecraft mc){
        if(initialWorldTick<0)initialWorldTick=mc.level.getGameTime();initialClientTicks++;
        require(mc.level.getGameTime()-initialWorldTick<600,"Initial late-device arena meshes did not become ready");
        return initialClientTicks>=60&&mc.level.getGameTime()-initialWorldTick>=60
            &&mc.level.hasChunkAt(BENCH)&&mc.level.getBlockState(new BlockPos(0,111,0)).is(Blocks.STONE_BRICKS)
            &&mc.levelRenderer.isChunkCompiled(BENCH)&&mc.levelRenderer.isChunkCompiled(new BlockPos(-16,111,-16))
            &&mc.levelRenderer.isChunkCompiled(new BlockPos(-16,111,0))
            &&mc.level.getBrightness(LightLayer.SKY,new BlockPos(-5,114,6))>=12;
    }
    private static boolean advance(Minecraft mc){if(scene==0&&!initialReady(mc))return false;return switch(scene){case 0,1,2->charger(mc);case 3,4,5->spa(mc);case 6,7->urn(mc);case 8,9,10,11,12->sprayer(mc);case 13->travel(mc);default->barrier(mc);};}
    private static boolean charger(Minecraft mc){
        if(scene==0){if(phase==0){phase=1;submit(mc,()->{var p=player(mc);var l=p.serverLevel();require(bench(l).availableVis()==integerPool(l)&&AuraManager.getVis(l,BENCH)<50&&bench(l).availableVis()>50,"Native charger did not expose surrounding integer chunk pool");view(p,-5.0,112,6.8,BENCH.above().getCenter());LogUtils.getLogger().info("THAUMCRAFT_LATE_DEVICES_CHARGER_AVAILABLE: actualNineSeparatelyTruncatedChunks={} bareCentre={} rawChargerFixture",bench(l).availableVis(),AuraManager.getVis(l,BENCH));});return false;}return mc.screen==null&&position(mc,-5,112,6.8);}
        if(scene==1){if(phase==0){phase=1;submit(mc,()->{var p=player(mc);var l=p.serverLevel();var r=(ArcaneRecipe)l.getRecipeManager().byKey(id("arcane/arcanespa")).orElseThrow();require(r.vis()==50&&r.crystalCost(2)==1,"Original Spa50Vis/Aqua1 changed");for(int i=0;i<r.getIngredients().size();i++)if(!r.getIngredients().get(i).isEmpty())bench(l).setItem(i%r.gridWidth()+3*(i/r.gridWidth()),r.getIngredients().get(i).getItems()[0].copyWithCount(1));for(int i=0;i<6;i++)if(r.crystalCost(i)>0)bench(l).setItem(9+i,AspectCrystalItem.create(Aspect.getAspect(ArcaneModule.PRIMALS[i]),r.crystalCost(i)));view(p,-7.5,112,5.5,BENCH.above().getCenter());});return false;}
            if(phase==1){if(!position(mc,-7.5,112,5.5))return false;phase=2;mc.options.hideGui=false;click(mc,BENCH.above(),Direction.SOUTH);return false;}
            return mc.player.containerMenu instanceof ArcaneWorkbenchMenu menu&&menu.craftable()&&menu.requiredVis()==50&&menu.availableVis()>50&&menu.getSlot(0).getItem().is(item("spa").getItem());}
        if(!(mc.player.containerMenu instanceof ArcaneWorkbenchMenu menu))return false;
        if(phase==0){phase=1;submit(mc,()->{var l=player(mc).serverLevel();require(AuraManager.getVis(l,BENCH)<50,"Paid charger fixture no longer distinguishes neighbouring aura");poolBefore=pool(l);});return false;}
        if(phase==1){phase=2;mc.gameMode.handleInventoryMouseClick(menu.containerId,0,0,ClickType.QUICK_MOVE,mc.player);return false;}
        if(phase==2){if(menu.hasRecipe())return false;phase=3;submit(mc,()->{var p=player(mc);var l=p.serverLevel();require(bench(l).isEmpty()&&count(p,"spa")==1&&Math.abs(poolBefore-pool(l)-50)<.001F&&KnowledgeStore.get(p).hasCraft("thaumcraft:spa"),"Native charger craft lost exact50Vis/oneAqua/physicalingredients/output evidence");benchPaid=true;LogUtils.getLogger().info("THAUMCRAFT_LATE_DEVICES_CHARGER_PAID: actualC2SDelegatedMenu/50VisAcrossNineChunks/Aqua1/physicalOriginalIngredients/oneSpa/bareCentreInsufficient");});return false;}return benchPaid;
    }
    private static boolean spa(Minecraft mc){
        if(scene==3){if(phase==0){phase=1;close(mc);submit(mc,()->{var p=player(mc);var l=p.serverLevel();l.setBlockAndUpdate(SPA_LEVER,Blocks.REDSTONE_BLOCK.defaultBlockState());held(p,take(p,"spa"));viewWithHeld(p,-1.5,112,5.5,SPA.getCenter());});return false;}
            if(phase==1){if(!position(mc,-1.5,112,5.5)||!mc.player.getMainHandItem().is(item("spa").getItem()))return false;phase=2;click(mc,SPA.below(),Direction.UP);return false;}
            if(phase==2){if(!(mc.level.getBlockEntity(SPA) instanceof SpaBlockEntity))return false;phase=3;submit(mc,()->{var p=player(mc);var l=p.serverLevel();require(count(p,"spa")==0,"Native Survival Spa placement did not consume paid output");var cap=spa(l).getCapability(ForgeCapabilities.FLUID_HANDLER,Direction.NORTH).orElseThrow(AssertionError::new);require(cap.fill(new FluidStack(Fluids.WATER,3000),IFluidHandler.FluidAction.EXECUTE)==3000,"Initial physical Spa water fixture rejected");spa(l).setItem(0,item("bath_salts").copyWithCount(2));stow(p);});return false;}
            if(phase==3){if(!mc.player.getMainHandItem().isEmpty())return false;phase=4;click(mc,SPA,Direction.SOUTH);return false;}
            return mc.screen instanceof LateDeviceScreen&&mc.player.containerMenu instanceof LateDeviceMenu menu&&menu.tile() instanceof SpaBlockEntity s&&s.mix()&&s.tank.getFluidAmount()==3000&&s.getItem(0).getCount()==2;}
        if(scene==4){if(!(mc.screen instanceof LateDeviceScreen screen)||!(mc.player.containerMenu instanceof LateDeviceMenu menu)||!(menu.tile() instanceof SpaBlockEntity s))return false;if(phase==0){phase=1;require(screen.mouseClicked((screen.width-176)/2.+93,(screen.height-166)/2.+39,0),"Native Spa mix button rejected");return false;}return !s.mix()&&s.tank.getFluidAmount()==3000&&s.getItem(0).getCount()==2;}
        if(phase==0){require(mc.screen instanceof LateDeviceScreen,"Native Spa screen absent before restoring mix");var screen=(LateDeviceScreen)mc.screen;require(screen.mouseClicked((screen.width-176)/2.+93,(screen.height-166)/2.+39,0),"Native Spa restore-mix click rejected");phase=1;return false;}
        if(phase==1){if(!(mc.player.containerMenu instanceof LateDeviceMenu m)||!(m.tile() instanceof SpaBlockEntity s)||!s.mix())return false;phase=2;close(mc);mc.options.hideGui=true;submit(mc,()->{var p=player(mc);held(p,new ItemStack(Items.DIAMOND_PICKAXE));viewWithHeld(p,-.5,112,5.5,SPA.getCenter());});return false;}
        if(phase==2){if(!position(mc,-.5,112,5.5)||!mc.player.getMainHandItem().is(Items.DIAMOND_PICKAXE))return false;phase=3;mc.gameMode.startDestroyBlock(SPA_LEVER,Direction.SOUTH);return false;}
        if(phase==3){if(mc.level.getBlockState(SPA_LEVER).is(Blocks.REDSTONE_BLOCK)){mc.gameMode.continueDestroyBlock(SPA_LEVER,Direction.SOUTH);return false;}phase=4;submit(mc,()->view(player(mc),.7,112,6.5,SPA.above().getCenter()));return false;}
        if(!bathPaid){if(!mc.level.getBlockState(SPA.above()).is(CleansingModule.block()))return false;submit(mc,()->{var l=player(mc).serverLevel();require(spa(l).tank.getFluidAmount()==2000&&spa(l).getItem(0).getCount()==1&&l.getFluidState(SPA.above()).isSource(),"Native Spa first successful placement did not pay1000mB/oneSalt");bathPaid=true;LogUtils.getLogger().info("THAUMCRAFT_LATE_DEVICES_SPA_PAID: paidSpaNativeSurvivalPlacement/nativeOriginalMenuButton1bothDirections/nativeRedstoneHarvest/actualWorldTicker/3000to2000mB/salt2to1/realPurifyingBathSource");});return false;}
        return mc.screen==null&&position(mc,.7,112,6.5);
    }
    private static void viewWithHeld(ServerPlayer p,double x,double y,double z,Vec3 target){Vec3 d=target.subtract(x,y+p.getEyeHeight(),z);p.connection.teleport(x,y,z,(float)Math.toDegrees(Math.atan2(d.z,d.x))-90,(float)-Math.toDegrees(Math.atan2(d.y,Math.sqrt(d.x*d.x+d.z*d.z))));}
    private static boolean urn(Minecraft mc){
        if(scene==6){if(phase==0){phase=1;close(mc);mc.options.hideGui=true;submit(mc,()->{var p=player(mc);var l=p.serverLevel();l.setBlockAndUpdate(URN.below(),Blocks.STONE_BRICKS.defaultBlockState());require(l.isEmptyBlock(URN),"Urn fixture replaced active tile");l.setBlockAndUpdate(URN,CatalogBlocks.block("everfull_urn").defaultBlockState());var u=(EverfullUrnBlockEntity)l.getBlockEntity(URN);require(u.tank.isEmpty(),"New Urn fixture had water");previousUrn=previousReceiver=0;previousUrnVis=AuraManager.getVis(l,URN);monitorUrn=true;view(p,-7.5,112,-3.5,URN.getCenter().add(0,.5,0));});return false;}
            return urnRefilled&&mc.level.getBlockEntity(URN) instanceof EverfullUrnBlockEntity u&&u.tank.getFluidAmount()==1000&&position(mc,-7.5,112,-3.5);}
        if(phase==0){phase=1;submit(mc,()->{var p=player(mc);var l=p.serverLevel();actionTick=l.getGameTime();l.setBlockAndUpdate(RECEIVER.east(),Blocks.REDSTONE_BLOCK.defaultBlockState());l.setBlockAndUpdate(RECEIVER,CatalogBlocks.block("spa").defaultBlockState());var s=(SpaBlockEntity)l.getBlockEntity(RECEIVER);require(s.tank.isEmpty(),"Initial UP fluid recipient was not empty");require(s.getCapability(ForgeCapabilities.FLUID_HANDLER,Direction.UP).isPresent(),"Recipient lacks native UP handler");view(p,-8.1,112,-11.5,URN.getCenter().add(-.7,.8,-.3));});return false;}
        require(!elapsed(600),"Native Urn transfer lacked a visible unfolded path within600ticks: "+waterPathFacts);
        return waterPaid&&waterPathReady&&mc.screen==null;
    }
    private static boolean sprayer(Minecraft mc){
        if(scene==8){if(phase==0){phase=1;close(mc);submit(mc,()->{var p=player(mc);var l=p.serverLevel();for(var q:BlockPos.betweenClosed(SPRAYER.offset(-1,-1,-4),SPRAYER.offset(1,-1,1)))l.setBlockAndUpdate(q,Blocks.STONE_BRICKS.defaultBlockState());l.setBlockAndUpdate(SPRAYER,CatalogBlocks.block("potion_sprayer").defaultBlockState().setValue(PotionSprayerBlock.FACING,Direction.NORTH));lever(l,SPRAY_LEVER);var target=cow(p,SPRAYER.north(2));target.setHealth(4);sprayActor=target.getId();held(p,PotionUtils.setPotion(new ItemStack(Items.POTION),Potions.HEALING));cameraSupport(l,7.5,112,1.5);viewWithHeld(p,7.5,112,1.5,SPRAYER.getCenter());});return false;}
            if(phase==1){if(!position(mc,7.5,112,1.5)||!mc.player.getMainHandItem().is(Items.POTION))return false;phase=2;mc.options.hideGui=false;click(mc,SPRAYER,Direction.SOUTH);return false;}
            if(!(mc.screen instanceof LateDeviceScreen)||!(mc.player.containerMenu instanceof LateDeviceMenu menu))return false;
            if(phase==2){phase=3;mc.gameMode.handleInventoryMouseClick(menu.containerId,28,0,ClickType.QUICK_MOVE,mc.player);return false;}
            return menu.tile() instanceof PotionSprayerBlockEntity s&&s.recipe().visSize()>0&&s.progress().visSize()==0&&s.charges()==0&&PotionUtils.getPotion(s.getItem(0))==Potions.HEALING;}
        if(scene==9){if(phase==0){phase=1;submit(mc,()->{var p=player(mc);var l=p.serverLevel();actionTick=l.getGameTime();var recipe=sprayer(l).recipe();initialRecipeUnits=recipe.visSize();require(initialRecipeUnits>0&&recipe.getAmount(Aspect.ALCHEMY)>0,"Original Healing recipe missing");int i=0;for(var a:recipe.getAspectsSortedByName()){var q=SPRAYER.offset(2+i,-1,0);l.setBlockAndUpdate(q,CatalogBlocks.block("jar_normal").defaultBlockState());var jar=(EssentiaJarBlockEntity)l.getBlockEntity(q);require(jar.addExact(a,recipe.getAmount(a)),"Initial typed physical jar fixture rejected "+a);JARS.add(q);INITIAL_ESSENTIA.put(q,recipe.getAmount(a));i++;}for(int x=1;x<=i+1;x++){var q=SPRAYER.east(x);l.setBlockAndUpdate(q,CatalogBlocks.block("tube").defaultBlockState());TUBES.add(q);}LogUtils.getLogger().info("THAUMCRAFT_LATE_DEVICES_SPRAYER_SOURCES: initialPhysicalTypedJars={} units={} ordinaryNativeTubeNetwork/noSourceAmountOrSuctionSetterAfterPlacement",INITIAL_ESSENTIA,initialRecipeUnits);});return false;}
            return mc.player.containerMenu instanceof LateDeviceMenu menu&&menu.tile() instanceof PotionSprayerBlockEntity s&&s.progress().visSize()>0&&s.charges()==0&&jarAmount<initialRecipeUnits;}
        if(scene==10){require(!elapsed(5000)||sprayCharges>0,"Sprayer native tube draw stalled");return mc.player.containerMenu instanceof LateDeviceMenu menu&&menu.tile() instanceof PotionSprayerBlockEntity s&&s.charges()==1&&s.progress().visSize()==0&&jarAmount==0;}
        if(scene==11){if(phase==0){phase=1;close(mc);mc.options.hideGui=true;submit(mc,()->{var p=player(mc);stow(p);var l=p.serverLevel();require(sprayer(l).charges()==1&&sprayer(l).progress().visSize()==0&&ownedEssentia(l)==0,"One original recipe did not pay exactly one charge");require(((Cow)l.getEntity(sprayActor)).getHealth()==4,"Sprayer target changed before real edge");l.setBlockAndUpdate(new BlockPos(9,112,-4),Blocks.STONE_BRICKS.defaultBlockState());l.setBlockAndUpdate(new BlockPos(10,112,-4),Blocks.STONE_BRICKS.defaultBlockState());cameraSupport(l,9.8,113,-3.9);view(p,9.8,113,-3.9,SPRAYER.getCenter().add(0,0,-1));});return false;}
            if(phase==1){if(!position(mc,9.8,113,-3.9))return false;phase=2;click(mc,SPRAY_LEVER,Direction.UP);return false;}
            return sprayPaid&&ventQuads>0&&mc.screen==null;}
        if(phase==0){phase=1;actionTick=worldTime;return false;}if(!elapsed(60))return false;
        if(!heldPaid){submit(mc,()->{var p=player(mc);var l=p.serverLevel();require(sprayer(l).charges()==0&&((Cow)l.getEntity(sprayActor)).getHealth()==8&&PotionUtils.getPotion(sprayer(l).getItem(0))==Potions.HEALING&&l.hasNeighborSignal(SPRAYER),"Held native power repeated dose or lost retained potion");heldPaid=true;LogUtils.getLogger().info("THAUMCRAFT_LATE_DEVICES_SPRAYER_PAID: exactPhysicalOriginalRecipe/nativeWorldTubeSuction/oneCharge/nativeC2SLeverEdge/health4to8/fullInstantHealing/nativeVent2/held60worldTicks/noPotionConsumption");});return false;}return mc.screen==null;
    }
    private static int ownedEssentia(ServerLevel l){int n=0;for(var q:JARS)n+=((EssentiaJarBlockEntity)l.getBlockEntity(q)).amount();for(var q:TUBES)n+=((TubeBlockEntity)l.getBlockEntity(q)).getEssentiaAmount(null);return n;}
    private static String actorFacts(Entity entity){
        if(entity==null)return "absent";var support=BlockPos.containing(entity.getX(),entity.getBoundingBox().minY-.01,entity.getZ());
        String facts="id="+entity.getId()+"/age="+entity.tickCount+"/alive="+entity.isAlive()+"/position="+entity.position()+"/ground="+entity.onGround()+"/motion="+entity.getDeltaMovement()
            +"/underFeet="+entity.level().getBlockState(support)+"/blockBelow="+entity.level().getBlockState(entity.blockPosition().below());
        if(entity instanceof Mob mob)facts+="/noAi="+mob.isNoAi();
        if(entity instanceof LivingEntity living){
            facts+="/health="+living.getHealth()+"/effects="+living.getActiveEffects();
            var speed=living.getAttribute(Attributes.MOVEMENT_SPEED);if(speed!=null)facts+="/speedBase="+speed.getBaseValue()+"/speedValue="+speed.getValue()+"/speedModifiers="+speed.getModifiers();
        }return facts;
    }
    private static String barrierFacts(Level level,Player p){return "player="+actorFacts(p)+"/cow="+actorFacts(level.getEntity(barrierActor))+"/powered="+level.hasNeighborSignal(BARRIER)
        +"/base="+level.getBlockState(BARRIER)+"/lower="+level.getBlockState(BARRIER.above())+"/upper="+level.getBlockState(BARRIER.above(2))+"/lever="+level.getBlockState(BARRIER_LEVER);}
    private static boolean travel(Minecraft mc){
        if(phase==0){phase=1;submit(mc,()->{var p=player(mc);var l=p.serverLevel();actionTick=l.getGameTime();for(int x=0;x<4;x++)l.setBlockAndUpdate(TRAVEL.east(x),CatalogBlocks.block("paving_stone_travel").defaultBlockState());
            var c=EntityType.COW.create(l);require(c!=null&&!c.isNoAi(),"Initial Travel Cow must retain its native default AI/physics");c.setPersistenceRequired();c.setPos(TRAVEL.getX()+.5,TRAVEL.getY()+1,TRAVEL.getZ()+.5);require(l.addFreshEntity(c),"Initial native Travel Cow insertion rejected");
            require(!c.hasEffect(MobEffects.MOVEMENT_SPEED)&&!c.hasEffect(MobEffects.JUMP),"Raw travel actor had effects");travelInitialPosition=c.position();c.setDeltaMovement(.08,0,0);travelActor=c.getId();view(p,-1.5,112,-6.0,TRAVEL.getCenter().add(1,1,0));LogUtils.getLogger().info("THAUMCRAFT_LATE_DEVICES_TRAVEL_FIXTURE: nativeDefaultAICow/oneInitialHorizontal008Impulse/nativeGravityAndStepOn/noActiveMotionGroundOrEffectSetters");});return false;}
        require(!elapsed(160),"Native Travel contact did not occur within160ticks: client="+actorFacts(mc.level.getEntity(travelActor))+", server="+serverTravelFacts);
        if(worldTime-lastTravelClientAudit>=20){lastTravelClientAudit=worldTime;LogUtils.getLogger().info("THAUMCRAFT_LATE_DEVICES_TRAVEL_CLIENT: paid={} actual={}",travelPaid,actorFacts(mc.level.getEntity(travelActor)));}
        if(!travelPaid||!(mc.level.getEntity(travelActor) instanceof Cow c)||mc.screen!=null)return false;
        var vanilla=MobEffects.MOVEMENT_SPEED.getAttributeModifiers().get(Attributes.MOVEMENT_SPEED);
        var speed=c.getAttribute(Attributes.MOVEMENT_SPEED);var received=speed==null?null:speed.getModifier(vanilla.getId());
        var delta=c.position().subtract(travelInitialPosition);var support=BlockPos.containing(c.getX(),c.getBoundingBox().minY-.01,c.getZ());
        boolean observed=received!=null&&received.getOperation()==vanilla.getOperation()
            &&Math.abs(received.getAmount()-MobEffects.MOVEMENT_SPEED.getAttributeModifierValue(1,vanilla))<1e-12
            &&delta.horizontalDistanceSqr()>.0001&&mc.level.getBlockState(support).is(CatalogBlocks.block("paving_stone_travel"));
        if(observed&&!travelClientObserved){travelClientObserved=true;LogUtils.getLogger().info("THAUMCRAFT_LATE_DEVICES_TRAVEL_CLIENT_OK: actualNativeCowMovement={} support={} receivedVanillaSpeedModifierUuid={} amount={} operation={}/exactServer40tickSpeedIIJumpIRetained/noUnbroadcastClientEffectMapAssumption",delta,support,received.getId(),received.getAmount(),received.getOperation());}
        return observed;
    }
    private static boolean barrier(Minecraft mc){
        if(phase==0)actionTick=worldTime;
        require(!elapsed(scene==15?160:600),"Native Barrier capture did not settle: scene="+scene+", phase="+phase+", client="+barrierFacts(mc.level,mc.player)+", server="+serverBarrierFacts);
        if(worldTime-lastBarrierClientAudit>=20){lastBarrierClientAudit=worldTime;LogUtils.getLogger().info("THAUMCRAFT_LATE_DEVICES_BARRIER_CLIENT: scene={} phase={} walk={} native={}",scene,phase,walk,barrierFacts(mc.level,mc.player));}
        if(scene==14){if(phase==0){phase=1;submit(mc,()->{var p=player(mc);var l=p.serverLevel();for(int x=-3;x<=3;x++)l.setBlockAndUpdate(BARRIER.east(x),Blocks.STONE_BRICKS.defaultBlockState());l.setBlockAndUpdate(BARRIER,CatalogBlocks.block("paving_stone_barrier").defaultBlockState());lever(l,BARRIER_LEVER);var c=cow(p,BARRIER.west().above());barrierActor=c.getId();view(p,10.7,112,-8.0,BARRIER.getCenter().add(0,1,0));});return false;}
            return barrierPaid&&runeQuads>0&&mc.screen==null;}
        if(scene==15){if(phase==0){phase=1;mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);submit(mc,()->{var p=player(mc);stow(p);cameraSupport(p.serverLevel(),5.7,113,-11.5);p.connection.teleport(5.7,113,-11.5,-90,5);});return false;}
            if(phase==1){if(!position(mc,5.7,113,-11.5))return false;walk=true;phase=2;return false;}
            if(!playerPassed)return false;walk=false;if(phase==2){phase=3;submit(mc,()->{var p=player(mc);var l=p.serverLevel();require(l.noCollision(p,new AABB(BARRIER.above()))&&!l.noCollision(l.getEntity(barrierActor),new AABB(BARRIER.above())),"Native player passage changed mob exclusion");cameraSupport(l,10.8,112,-10.2);view(p,10.8,112,-10.2,BARRIER.above().getCenter());LogUtils.getLogger().info("THAUMCRAFT_LATE_DEVICES_BARRIER_PLAYER: realClientMovementPacketsCrossedUnpoweredNativeFence/playerCollisionEmpty/mobCollisionSolid/noPassengerOrPowerBypass");});return false;}return mc.screen==null;}
        if(phase==0){phase=1;mc.options.setCameraType(CameraType.FIRST_PERSON);submit(mc,()->view(player(mc),10.7,112,-8.0,BARRIER.above().getCenter()));return false;}
        if(phase==1){if(!position(mc,10.7,112,-8))return false;phase=2;click(mc,BARRIER_LEVER,Direction.UP);return false;}
        return powerPassed&&runeQuads>0&&mc.screen==null;
    }

    @SubscribeEvent public static void serverTick(TickEvent.ServerTickEvent e){
        if(e.phase!=TickEvent.Phase.END||!enabled()||stopped||!prepared||!WORLD.equals(e.getServer().getWorldData().getLevelName()))return;var l=e.getServer().overworld();worldTime=l.getGameTime();try{
            if(monitorUrn&&l.getBlockEntity(URN) instanceof EverfullUrnBlockEntity u){urnAmount=u.tank.getFluidAmount();receiverAmount=l.getBlockEntity(RECEIVER) instanceof SpaBlockEntity s?s.tank.getFluidAmount():0;int incoming=receiverAmount-previousReceiver,generated=urnAmount-previousUrn+incoming;float now=AuraManager.getVis(l,URN);
                if(incoming>0){require(incoming==25,"Native Urn UP capability accepted other than25mB");transfers++;waterPaid=true;}
                if(generated>0){require(generated<=100,"Native Urn created more than100mB per5ticks");if(worldTime%20!=0)require(Math.abs(previousUrnVis-now-generated/1000F)<.002F,"Native Urn water did not pay actual fractional aura");refills++;}
                if(urnAmount==1000&&receiverAmount==0&&refills>=10)urnRefilled=true;previousUrn=urnAmount;previousReceiver=receiverAmount;previousUrnVis=now;
            }
            if(l.getBlockEntity(SPRAYER) instanceof PotionSprayerBlockEntity s){sprayCharges=s.charges();progressAmount=s.progress().visSize();jarAmount=JARS.stream().mapToInt(q->((EssentiaJarBlockEntity)l.getBlockEntity(q)).amount()).sum();if(l.getEntity(sprayActor) instanceof Cow c){health=(int)c.getHealth();if(scene>=11&&l.hasNeighborSignal(SPRAYER)&&sprayCharges==0&&health==8)sprayPaid=true;}}
            if(scene==13&&worldTime-lastTravelServerAudit>=20){lastTravelServerAudit=worldTime;serverTravelFacts=actorFacts(l.getEntity(travelActor));LogUtils.getLogger().info("THAUMCRAFT_LATE_DEVICES_TRAVEL_SERVER: paid={} actual={}",travelPaid,serverTravelFacts);}
            if(scene>=14&&scene<=16&&worldTime-lastBarrierServerAudit>=20){lastBarrierServerAudit=worldTime;var p=e.getServer().getPlayerList().getPlayers().stream().findFirst().orElse(null);serverBarrierFacts=barrierFacts(l,p);LogUtils.getLogger().info("THAUMCRAFT_LATE_DEVICES_BARRIER_SERVER: scene={} phase={} walk={} native={}",scene,phase,walk,serverBarrierFacts);}
            if(l.getEntity(travelActor) instanceof Cow c&&!travelPaid&&c.hasEffect(MobEffects.MOVEMENT_SPEED)&&c.hasEffect(MobEffects.JUMP)){var speed=c.getEffect(MobEffects.MOVEMENT_SPEED);var jump=c.getEffect(MobEffects.JUMP);require(speed.getAmplifier()==1&&jump.getAmplifier()==0&&speed.getDuration()<=40&&jump.getDuration()<=40&&speed.getDuration()>0&&jump.getDuration()>0,"Native travel effect differs from40tickSpeedII/JumpI");travelPaid=true;LogUtils.getLogger().info("THAUMCRAFT_LATE_DEVICES_TRAVEL: realNativeCowPhysics/stepOn/worldTicks/SpeedIIremaining{}/JumpIremaining{}/noDirectEffectOrStepOnCall",speed.getDuration(),jump.getDuration());}
            if(l.getBlockState(BARRIER.above()).is(CatalogBlocks.block("barrier"))&&l.getBlockState(BARRIER.above(2)).is(CatalogBlocks.block("barrier"))&&l.getEntity(barrierActor) instanceof Cow c){var p=e.getServer().getPlayerList().getPlayers().stream().findFirst().orElseThrow();var s=l.getBlockState(BARRIER.above());if(!l.hasNeighborSignal(BARRIER)){require(!s.getCollisionShape(l,BARRIER.above(),CollisionContext.of(c)).isEmpty()&&s.getCollisionShape(l,BARRIER.above(),CollisionContext.of(p)).isEmpty(),"Native barrier lost living/player distinction");barrierPaid=true;if(scene==15&&p.getX()>BARRIER.getX()+1.2)playerPassed=true;}else{require(s.getCollisionShape(l,BARRIER.above(),CollisionContext.of(c)).isEmpty(),"Native powered barrier still blocks mob");powerPassed=true;}}
        }catch(Throwable error){Minecraft.getInstance().execute(()->fail(Minecraft.getInstance(),error));}
    }
    private static void prepare(ServerPlayer p){
        var l=p.serverLevel();for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++)l.getChunk(x,z);for(var q:BlockPos.betweenClosed(-25,111,-19,26,120,12))l.setBlock(q,q.getY()==111?Blocks.STONE_BRICKS.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
        // Initial moon4 removes natural vis regeneration; normal diffusion remains enabled throughout.
        l.setDayTime(4*24000L+6000);for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++){var q=new ChunkPos(x,z).getMiddleBlockPosition(112);AuraManager.drainVis(l,q,Float.MAX_VALUE,false);AuraManager.drainFlux(l,q,Float.MAX_VALUE,false);AuraManager.addVis(l,q,400);}AuraManager.drainVis(l,BENCH,Float.MAX_VALUE,false);
        l.setBlockAndUpdate(BENCH,ArcaneModule.WORKBENCH.get().defaultBlockState());l.setBlockAndUpdate(BENCH.above(),CatalogBlocks.block("arcane_workbench_charger").defaultBlockState());p.setInvulnerable(true);p.getInventory().clearContent();p.getInventory().selected=0;rawStudy(p,"ARCANESPA");ResearchNetwork.sync(p);view(p,-5,112,6.8,BENCH.above().getCenter());
        LogUtils.getLogger().info("THAUMCRAFT_LATE_DEVICES_FIXTURES: ownedNewSurvivalWorld/rawChargerBench/completedARCANESPA/prerequisiteRecipeIngredientsCrystals/initialMoon4/initial400VisLoadedChunksExceptEmptyBenchCentre/arena/initialWaterSaltPotionEssentiaActorsDeferred; actualNativePaidSpa/menuButtons/worldTickConsumers/particlesNativeOnly/noActiveAuraChargeSourceClockAgeOrRngReset/notFullSurvivalOrNaturalAcquisition");
    }

    private static Object field(Object object,String name){try{Field f=(object instanceof Class<?> c?c:object.getClass()).getDeclaredField(name);f.setAccessible(true);return f.get(object instanceof Class<?>?null:object);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}}
    private static double[] screenPoint(Minecraft mc,Vec3 point){
        var c=mc.gameRenderer.getMainCamera();var d=point.subtract(c.getPosition());var f=c.getLookVector();var u=c.getUpVector();var left=c.getLeftVector();
        double depth=d.x*f.x()+d.y*f.y()+d.z*f.z(),tangent=Math.tan(Math.toRadians(mc.options.fov().get()/2D)),aspect=mc.getWindow().getScreenWidth()/(double)mc.getWindow().getScreenHeight();
        if(depth<=.05)return null;double x=(d.x*left.x()+d.y*left.y()+d.z*left.z())/depth/tangent/aspect,y=(d.x*u.x()+d.y*u.y()+d.z*u.z())/depth/tangent;
        return Math.abs(x)>.93||Math.abs(y)>.93?null:new double[]{(.5-x*.5)*mc.getWindow().getScreenWidth(),(.5-y*.5)*mc.getWindow().getScreenHeight()};
    }
    private static boolean pointClear(Minecraft mc,Vec3 point){return screenPoint(mc,point)!=null&&mc.level.clip(new ClipContext(mc.gameRenderer.getMainCamera().getPosition(),point,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,mc.player)).getType()==HitResult.Type.MISS;}
    private static final class WaterFacts {
        final Vec3 start,target,direction;final int age,life,points;final double squaredDistance;final int[] bins=new int[4];final java.util.Set<Integer> indices=new java.util.HashSet<>();
        double minX=Double.POSITIVE_INFINITY,maxX=Double.NEGATIVE_INFINITY,minY=minX,maxY=maxX,minProgress=minX,maxProgress=maxX;int vertices,visible;
        WaterFacts(Vec3 start,Vec3 target,int age,int life,int points){this.start=start;this.target=target;this.age=age;this.life=life;this.points=points;direction=target.subtract(start);squaredDistance=direction.lengthSqr();}
        void visibleVertex(Minecraft mc,Vec3 point,int index,double radius){
            if(Math.abs(radius)<.01||squaredDistance<.000001||!pointClear(mc,point))return;
            double progress=point.subtract(start).dot(direction)/squaredDistance;if(progress<.1||progress>.9)return;
            var screen=screenPoint(mc,point);if(screen==null)return;
            bins[Math.min(3,(int)((progress-.1)/.2))]++;indices.add(index);visible++;
            minX=Math.min(minX,screen[0]);maxX=Math.max(maxX,screen[0]);minY=Math.min(minY,screen[1]);maxY=Math.max(maxY,screen[1]);minProgress=Math.min(minProgress,progress);maxProgress=Math.max(maxProgress,progress);
        }
        boolean ready(){return age>=20&&points>=20&&java.util.Arrays.stream(bins).allMatch(n->n>=4)&&indices.size()>=6&&Math.hypot(maxX-minX,maxY-minY)>=80;}
        double score(){return java.util.Arrays.stream(bins).filter(n->n>=4).count()*1000D+visible;}
        String summary(){return "age="+age+"/life="+life+"/points="+points+"/emittedVertices="+vertices+"/visibleVertices="+visible+"/visibleIndices="+indices+"/progressBins="+java.util.Arrays.toString(bins)+"/progress="+minProgress+".."+maxProgress+"/screenRect="+minX+","+minY+".."+maxX+","+maxY+"/start="+start+"/target="+target+"/ready="+ready();}
    }
    private static QuadFacts quad(Minecraft mc,FxParticle p){var sink=new QuadFacts(mc.gameRenderer.getMainCamera().getPosition());p.draw(sink,mc.gameRenderer.getMainCamera(),mc.getFrameTime(),new org.joml.Matrix4f());return sink;}
    private static boolean visible(Minecraft mc,FxParticle p){var q=quad(mc,p);return q.alpha>=30&&q.points.stream().filter(v->pointClear(mc,v)).count()>=2;}
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void worldRendered(RenderLevelStageEvent e){
        if(!enabled()||stopped||e.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES||Minecraft.getInstance().level==null||(scene!=7&&scene!=11&&scene!=14&&scene!=16))return;var mc=Minecraft.getInstance();try{
            ventQuads=runeQuads=waterVertices=waterClear=0;waterPathReady=false;
            for(var p:FxEngine.snapshot()){if(p.getClass().getSimpleName().equals("Vent2")&&p.position().distanceToSqr(SPRAYER.getCenter())<25&&visible(mc,p))ventQuads++;if(p.getClass().getSimpleName().equals("Rune")&&p.position().distanceToSqr(BARRIER.getCenter())<12&&p.age()>=8&&visible(mc,p)){var q=quad(mc,p);if(scene!=16||q.r<150&&q.b>200)runeQuads++;}}
            if(scene==7){
                WaterFacts best=null;
                for(var stream:(List<?>)field(WaterStreams.class,"ACTIVE")){
                    var points=(double[][])field(stream,"points");var radii=(double[])field(stream,"radii");Vec3 start=(Vec3)field(stream,"start"),target=(Vec3)field(stream,"target");
                    if(points==null||points.length<3||start.distanceToSqr(URN.getCenter())>4||target.distanceToSqr(RECEIVER.getCenter())>.000001)continue;
                    var facts=new WaterFacts(start,target,(Integer)field(stream,"age"),(Integer)field(stream,"life"),points.length);
                    WaterStreamGeometry.emit(points,radii,(x,y,z,u,v,index)->{waterVertices++;facts.vertices++;var point=start.add(x,y,z);if(pointClear(mc,point))waterClear++;facts.visibleVertex(mc,point,index,radii[index]);});
                    if(facts.ready())waterPathReady=true;
                    if(best==null||facts.ready()&&!best.ready()||facts.ready()==best.ready()&&facts.score()>best.score())best=facts;
                }
                waterPathFacts=best==null?"no owned native stream points":best.summary();
                if(mc.level.getGameTime()-lastWaterAudit>=20){lastWaterAudit=mc.level.getGameTime();LogUtils.getLogger().info("THAUMCRAFT_LATE_DEVICES_URN_PATH_WAIT: paid={} actualNativePath={}",waterPaid,waterPathFacts);}
            }
            visualFacts="waterDrawVertices="+waterVertices+",clear="+waterClear+",waterPath="+waterPathFacts+",Vent2VisibleQuads="+ventQuads+",RuneVisibleQuads="+runeQuads;
            if(scene==7&&waterPathReady||scene==11&&ventQuads>0||scene==14&&runeQuads>0||scene==16&&runeQuads>0)lastFxFrame=renderFrame;
        }catch(Throwable error){fail(mc,error);}
    }
    private static final class QuadFacts implements VertexConsumer {
        final Vec3 camera;final List<Vec3> points=new ArrayList<>(4);double x,y,z;int alpha,r,g,b;
        QuadFacts(Vec3 camera){this.camera=camera;}
        @Override public VertexConsumer vertex(double x,double y,double z){this.x=x;this.y=y;this.z=z;return this;}
        @Override public VertexConsumer color(int r,int g,int b,int a){this.r=r;this.g=g;this.b=b;alpha=a;return this;}
        @Override public VertexConsumer uv(float u,float v){return this;}
        @Override public VertexConsumer overlayCoords(int u,int v){return this;}
        @Override public VertexConsumer uv2(int u,int v){return this;}
        @Override public VertexConsumer normal(float x,float y,float z){return this;}
        @Override public void endVertex(){points.add(camera.add(x,y,z));}
        @Override public void defaultColor(int r,int g,int b,int a){color(r,g,b,a);}
        @Override public void unsetDefaultColor(){}
    }
    @SubscribeEvent public static void rendered(TickEvent.RenderTickEvent e){
        if(!enabled()||stopped)return;if(e.phase==TickEvent.Phase.START){renderFrame++;return;}if(!captureRequested||captured)return;var mc=Minecraft.getInstance();try{
            if((scene==7||scene==11||scene==14||scene==16)&&lastFxFrame!=renderFrame)return;
            require(mc.getWindow().getScreenWidth()==1600&&mc.getWindow().getScreenHeight()==1000,"Expected1600x1000 native capture");captured=true;captureRequested=false;stable=0;
            String name="tc6-late-devices-"+IMAGES[scene]+".png";File file=new File(new File(mc.gameDirectory,"screenshots"),name);require(!file.exists(),"Preserve existing late-device capture "+name);
            LogUtils.getLogger().info("THAUMCRAFT_LATE_DEVICES_NATIVE_FRAME: name={} camera={} yaw={} pitch={} screen={} nativeFxFrame={}/{} urn={} receiver={} charges={} progress={} jars={} targetHealth={} visual={} actualPixelsRequireReview",name,mc.gameRenderer.getMainCamera().getPosition(),mc.player.getYRot(),mc.player.getXRot(),mc.screen==null?"world":mc.screen.getClass().getSimpleName(),lastFxFrame,renderFrame,urnAmount,receiverAmount,sprayCharges,progressAmount,jarAmount,health,visualFacts);
            if(scene==11||scene==14||scene==16)LogUtils.getLogger().info("THAUMCRAFT_LATE_DEVICES_ACTUAL_DRAW_QUADS: {}",FxEngine.snapshot().stream().filter(p->p.getClass().getSimpleName().equals(scene==11?"Vent2":"Rune")&&visible(mc,p)).limit(8).map(p->{var q=quad(mc,p);return p.getClass().getSimpleName()+":age"+p.age()+":rgba"+q.r+","+q.g+","+q.b+","+q.alpha+":actualWorldCorners"+q.points;}).toList());
            Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),message->{if(file.isFile()&&file.length()>0){SAVED.incrementAndGet();LogUtils.getLogger().info("THAUMCRAFT_LATE_DEVICES_CLIENT_SMOKE_IMAGE: {}",file.getAbsolutePath());}else mc.execute(()->fail(mc,new AssertionError("Fresh late-device screenshot absent")));});
        }catch(Throwable error){fail(mc,error);}
    }
    private static void start(Minecraft mc){
        if(mc.screen instanceof AccessibilityOnboardingScreen){mc.options.onboardAccessibility=false;mc.options.save();mc.setScreen(new TitleScreen());return;}if(!(mc.screen instanceof TitleScreen)||mc.getOverlay()!=null)return;started=true;previous=mc.options.tutorialStep;mc.options.tutorialStep=TutorialSteps.NONE;mc.getTutorial().stop();mc.options.pauseOnLostFocus=false;mc.options.renderDistance().set(4);mc.options.simulationDistance().set(5);mc.options.guiScale().set(2);mc.options.fov().set(65);mc.options.bobView().set(false);mc.options.cloudStatus().set(CloudStatus.OFF);mc.options.graphicsMode().set(GraphicsStatus.FANCY);mc.options.particles().set(ParticleStatus.ALL);mc.options.setCameraType(CameraType.FIRST_PERSON);mc.options.hideGui=true;mc.resizeDisplay();
        var rules=new GameRules();rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false,null);rules.getRule(GameRules.RULE_DAYLIGHT).set(false,null);rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false,null);rules.getRule(GameRules.RULE_RANDOMTICKING).set(0,null);mc.createWorldOpenFlows().createFreshLevel(WORLD,new LevelSettings(WORLD,GameType.SURVIVAL,false,Difficulty.PEACEFUL,true,rules,WorldDataConfiguration.DEFAULT),new WorldOptions(0x544336042L,false,false),WorldPresets::createNormalWorldDimensions);
    }
    private static void finish(Minecraft mc){
        walk=false;if(phase==0){phase=1;close(mc);submit(mc,()->{var p=player(mc);var l=p.serverLevel();require(benchPaid&&bathPaid&&urnRefilled&&waterPaid&&sprayPaid&&heldPaid&&travelPaid&&barrierPaid&&playerPassed&&powerPassed,"Incomplete native late-device facts");require(ownedEssentia(l)==0&&sprayer(l).charges()==0&&PotionUtils.getPotion(sprayer(l).getItem(0))==Potions.HEALING&&((Cow)l.getEntity(sprayActor)).getHealth()==8,"Final physical Sprayer conservation changed");LogUtils.getLogger().info("THAUMCRAFT_LATE_DEVICES_CLIENT_FACTS: captures={} chargerActualNineChunk50VisAqua1PaidSpa/nativeSpaPlacementMixButtonsBath1000mB1Salt/UrnActualAuraRefills{}UPcapability25mBTransfers{}/SprayerOriginalPhysicalEssentia{}NativeTubesOneChargeFull4HealingHeld60ticks/nativeTravelSpeedIIJumpI/nativeBarrierTwoCellsMobPlayerWalkPower/initialResourcesExplicit/notFullSurvivalMultiplayerReloadAudioAllAngles",SAVED.get(),refills,transfers,initialRecipeUnits);validated=true;});return;}if(!validated)return;require(SAVED.get()==IMAGES.length,"Incomplete native late-device images");stopped=true;mc.options.tutorialStep=previous;LogUtils.getLogger().info("THAUMCRAFT_LATE_DEVICES_RENDER_AUDIT_OK: 17captures/originalSpaSprayerScreens/nativeWaterEssentiaTextureEightSidedGeometry/nativeVent2/nativeBarrierRunes/sameNativeWorldFrameAndActualDrawVertexReadiness/noInjectedParticlesOrActiveClockRng/actualPixelsRequireIndependentReview");LogUtils.getLogger().info("THAUMCRAFT_LATE_DEVICES_CLIENT_SMOKE_OK: {} captures",SAVED.get());mc.stop();
    }
    private static void fail(Minecraft mc,Throwable error){if(stopped)return;stopped=true;walk=false;if(previous!=null)mc.options.tutorialStep=previous;LogUtils.getLogger().error("THAUMCRAFT_LATE_DEVICES_CLIENT_SMOKE_FAILED",error);mc.stop();}
}
