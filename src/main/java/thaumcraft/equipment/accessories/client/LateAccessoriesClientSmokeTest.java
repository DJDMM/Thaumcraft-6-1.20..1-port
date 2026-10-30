package thaumcraft.equipment.accessories.client;

import com.mojang.logging.LogUtils;
import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import thaumcraft.equipment.RechargeSupport;
import thaumcraft.equipment.accessories.*;
import thaumcraft.infusion.InfusionEffects;
import thaumcraft.research.*;

/** Opt-in native Curios, wearer ticks, XP contact, damage and real local key binding in an owned world. */
@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public final class LateAccessoriesClientSmokeTest {
    private static final String WORLD="thaumcraft-late-accessories-"+System.currentTimeMillis();
    private static final String[] IMAGES={"original-three-verdant-forms","native-verdant-cure-inputs","native-wither-priority",
        "native-three-cures-paid","native-life-heart-healing","native-sustenance-air-food","original-worn-curiosity-band",
        "native-curiosity-xp-contact","native-equipped-undying-charm","native-damage-undying-rescue","native-equipped-cloud-ring",
        "native-cloud-double-jump","native-third-jump-rejected","native-cloud-landing"};
    private static final String MARK="LateAccessorySmokeFixture";
    private static final AtomicInteger SAVED=new AtomicInteger();
    private static CompletableFuture<Void> work;
    private static TutorialSteps previous;
    private static boolean started,prepared,stopped,captured,captureRequested;
    private static volatile int scene,phase;
    private static int wearPhase,ready,stable,heartbeat,initialMeshTicks;
    private static volatile boolean curesStarted,lifeStarted,sustainStarted,airPaid,foodPaid,xpStarted,xpPaid,rescued,cloudAccepted;
    private static volatile int cureStep,previousCureCharge=40,sustainCharge=10;
    private static volatile long worldTime;
    private static volatile int theoryBefore,experienceBefore;
    private static volatile UUID owner;
    private static long began,actionTick,bandDraws;
    private static boolean cloudPredicted;
    private static double thirdJumpBefore;
    private static volatile int observedClientCloudImpulses,observedServerCloudImpulses;
    private static final Set<thaumcraft.fx.client.FxParticle> cloudBefore=Collections.newSetFromMap(new IdentityHashMap<>());
    private static boolean cloudFrameReady;
    private static long cloudFrameWorldTime=-1,cloudEmittedBefore;
    private static List<String> cloudFrameFacts=List.of();
    private LateAccessoriesClientSmokeTest(){}
    private static boolean enabled(){return Boolean.getBoolean("thaumcraft.lateAccessoriesSmokeTest");}
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static ItemStack item(String key){
        var value=ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("thaumcraft",key));
        require(value!=null&&value!=Items.AIR,"Missing original item "+key);return new ItemStack(value);
    }
    private static ItemStack raw(String key,String fixture,int type,int charge){
        ItemStack stack=item(key);stack.getOrCreateTag().putString(MARK,fixture);
        if(type>0)stack.getOrCreateTag().putByte("type",(byte)type);
        if(charge>=0)stack.getOrCreateTag().putInt(RechargeSupport.CHARGE_TAG,charge);return stack;
    }
    private static boolean marked(ItemStack stack,String key){return !stack.isEmpty()&&stack.hasTag()&&key.equals(stack.getTag().getString(MARK));}
    private static ItemStack physical(net.minecraft.world.entity.player.Player p,String key){
        for(ItemStack stack:AccessoriesSupport.equipped(p))if(marked(stack,key))return stack;
        return p.getInventory().items.stream().filter(s->marked(s,key)).findFirst().orElse(ItemStack.EMPTY);
    }
    private static int theory(ServerPlayer p){return ResearchCategories.keys().stream().mapToInt(c->KnowledgeStore.get(p).rawKnowledge(KnowledgeType.THEORY,c)).sum();}
    private static ServerPlayer player(Minecraft mc){var p=mc.getSingleplayerServer().getPlayerList().getPlayer(owner);require(p!=null,"Owned player absent");return p;}
    private static void submit(Minecraft mc,Runnable action){
        require(work==null,"Overlapping server callback");var next=new CompletableFuture<Void>();work=next;
        mc.getSingleplayerServer().execute(()->{try{action.run();next.complete(null);}catch(Throwable e){next.completeExceptionally(e);}});
    }
    private static void close(Minecraft mc){mc.player.closeContainer();mc.setScreen(null);}
    private static boolean elapsed(Minecraft mc,int ticks){return mc.level.getGameTime()-actionTick>=ticks;}
    private static boolean curios(Minecraft mc){String name=mc.player.containerMenu.getClass().getName();return name.equals("top.theillusivec4.curios.common.inventory.container.CuriosContainer")||name.equals("top.theillusivec4.curios.common.inventory.container.CuriosContainerV2");}
    private static boolean openCurios(Minecraft mc){
        if(curios(mc))return true;
        if(!(mc.screen instanceof InventoryScreen)){mc.setScreen(new InventoryScreen(mc.player));return false;}
        if(++ready<4)return false;
        for(var child:mc.screen.children())if(child instanceof AbstractButton button&&child.getClass().getName().endsWith(".CuriosButton")){
            require(button.mouseClicked(button.getX()+button.getWidth()/2.,button.getY()+button.getHeight()/2.,0),"Native Curios button rejected click");ready=0;return false;
        }throw new AssertionError("Native Curios inventory button absent");
    }
    private static int curioSlot(Minecraft mc,String group){
        for(int i=0;i<mc.player.containerMenu.slots.size();i++){var slot=mc.player.containerMenu.getSlot(i);
            try{if(slot.getClass().getName().endsWith(".CurioSlot")&&group.equals(slot.getClass().getMethod("getIdentifier").invoke(slot))
                    &&!(boolean)slot.getClass().getMethod("isCosmetic").invoke(slot)&&slot.getContainerSlot()==0)return i;
            }catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
        }throw new AssertionError("Missing native Curios "+group+"0 slot");
    }
    private static int inventorySlot(AbstractContainerMenu menu,net.minecraft.world.entity.player.Player p,int index){
        for(int i=0;i<menu.slots.size();i++)if(menu.getSlot(i).container==p.getInventory()&&menu.getSlot(i).getContainerSlot()==index)return i;
        throw new AssertionError("Native player inventory slot absent "+index);
    }
    /** Native C2S clicks conserve the old physical item, then equip the one raw input. Null removes only. */
    private static boolean changeCurio(Minecraft mc,String group,String fixture){
        if(!openCurios(mc))return false;var menu=mc.player.containerMenu;int target=curioSlot(mc,group);
        if(wearPhase==0){
            if(menu.getSlot(target).getItem().isEmpty())wearPhase=2;
            else{mc.gameMode.handleInventoryMouseClick(menu.containerId,target,0,ClickType.PICKUP,mc.player);wearPhase=1;}return false;
        }
        if(wearPhase==1){
            if(menu.getCarried().isEmpty())return false;int empty=-1;
            for(int i=9;i<36;i++)if(mc.player.getInventory().getItem(i).isEmpty()){empty=i;break;}
            require(empty>=0,"No space to conserve unequipped physical charm");
            mc.gameMode.handleInventoryMouseClick(menu.containerId,inventorySlot(menu,mc.player,empty),0,ClickType.PICKUP,mc.player);wearPhase=2;return false;
        }
        if(wearPhase==2){
            if(!menu.getCarried().isEmpty())return false;
            if(fixture==null)return menu.getSlot(target).getItem().isEmpty();
            int source=-1;for(int i=0;i<36;i++)if(marked(mc.player.getInventory().getItem(i),fixture)){source=i;break;}
            require(source>=0,"Physical fixture item absent "+fixture);
            mc.gameMode.handleInventoryMouseClick(menu.containerId,inventorySlot(menu,mc.player,source),0,ClickType.PICKUP,mc.player);wearPhase=3;return false;
        }
        if(wearPhase==3){
            if(!marked(menu.getCarried(),fixture))return false;
            mc.gameMode.handleInventoryMouseClick(menu.containerId,target,0,ClickType.PICKUP,mc.player);wearPhase=4;return false;
        }
        return menu.getCarried().isEmpty()&&marked(menu.getSlot(target).getItem(),fixture);
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!enabled()||stopped)return;var mc=Minecraft.getInstance();if(began==0)began=System.nanoTime();
        try{
            require(System.nanoTime()-began<900_000_000_000L,"Late accessory timeout scene="+scene+" phase="+phase+" wear="+wearPhase);
            if(!started){start(mc);return;}
            if(mc.level==null||mc.player==null||mc.getOverlay()!=null||mc.screen instanceof ReceivingLevelScreen)return;
            require(mc.getSingleplayerServer()!=null&&WORLD.equals(mc.getSingleplayerServer().getWorldData().getLevelName()),"Wrong owned world");
            if(owner==null)owner=mc.player.getUUID();mc.getToasts().clear();
            if(++heartbeat%200==0)LogUtils.getLogger().info("THAUMCRAFT_LATE_ACCESSORIES_HEARTBEAT: scene={} phase={} wear={} cures={} air={} food={} xp={} rescue={} cloud={}",scene,phase,wearPhase,cureStep,airPaid,foodPaid,xpPaid,rescued,cloudAccepted);
            if(work!=null){if(!work.isDone())return;work.join();work=null;}
            if(!prepared){prepared=true;submit(mc,()->prepare(player(mc)));return;}
            if(captured){if(SAVED.get()==scene+1&&++stable>=3){scene++;phase=wearPhase=ready=stable=0;captured=captureRequested=false;}return;}
            if(scene==IMAGES.length){finish(mc);return;}
            if(advance(mc))captureRequested=true;
        }catch(Throwable error){fail(mc,error);}
    }
    private static boolean advance(Minecraft mc){
        return switch(scene){
            case 0->{
                if(++initialMeshTicks<60||mc.player.position().distanceToSqr(.5,112,.5)>.01
                        ||!mc.level.getBlockState(new BlockPos(0,111,0)).is(Blocks.BEDROCK)
                        ||!mc.levelRenderer.isChunkCompiled(new BlockPos(-8,111,-8))
                        ||!mc.levelRenderer.isChunkCompiled(new BlockPos(8,111,8))
                        ||!mc.levelRenderer.isChunkCompiled(new BlockPos(0,111,0)))yield false;
                if(!(mc.screen instanceof InventoryScreen))mc.setScreen(new InventoryScreen(mc.player));
                yield physical(mc.player,"base").getCount()==1&&physical(mc.player,"life").getCount()==1&&physical(mc.player,"sustain").getCount()==1;
            }
            case 1,2,3->cures(mc);
            case 4->life(mc);
            case 5->sustain(mc);
            case 6->band(mc);
            case 7->xp(mc);
            case 8->undyingEquip(mc);
            case 9->rescue(mc);
            case 10->cloudEquip(mc);
            case 11->cloudJump(mc);
            case 12->thirdJump(mc);
            case 13->mc.player.onGround()&&CloudRingSupport.jumpDelay(mc.player)==0;
            default->false;
        };
    }
    private static boolean cures(Minecraft mc){
        if(scene==1){
            if(phase==0){if(!changeCurio(mc,"charm","base"))return false;phase=1;close(mc);return false;}
            if(!curesStarted){submit(mc,()->{var p=player(mc);if(p.tickCount%20>3)return;
                require(RechargeSupport.getCharge(physical(p,"base"))==40,"Raw base charm charge changed before cure inputs");
                p.addEffect(new MobEffectInstance(MobEffects.WITHER,2400));p.addEffect(new MobEffectInstance(MobEffects.POISON,2400));p.addEffect(new MobEffectInstance(InfusionEffects.FLUX_TAINT.get(),2400));curesStarted=true;
                LogUtils.getLogger().info("THAUMCRAFT_LATE_ACCESSORIES_CURE_FIXTURE: oneInitial40Charge/oneInitialWitherPoisonFluxTaint/actualEquippedCurios/nativeClockReadOnly/noActiveResets");
            });return false;}
            require(cureStep==0,"Missed original cure-input capture before first native interval");
            return mc.player.hasEffect(MobEffects.WITHER)&&mc.player.hasEffect(MobEffects.POISON)&&mc.player.hasEffect(InfusionEffects.FLUX_TAINT.get());
        }
        if(scene==2){require(cureStep<=1,"Missed actual first priority interval");return cureStep==1&&!mc.player.hasEffect(MobEffects.WITHER)&&mc.player.hasEffect(MobEffects.POISON)&&RechargeSupport.getCharge(physical(mc.player,"base"))==20;}
        return cureStep==3&&RechargeSupport.getCharge(physical(mc.player,"base"))==5&&!mc.player.hasEffect(MobEffects.WITHER)&&!mc.player.hasEffect(MobEffects.POISON)&&!mc.player.hasEffect(InfusionEffects.FLUX_TAINT.get());
    }
    private static boolean life(Minecraft mc){
        if(phase==0){if(!changeCurio(mc,"charm","life"))return false;phase=1;close(mc);submit(mc,()->{var p=player(mc);require(RechargeSupport.getCharge(physical(p,"life"))==20,"Raw life charge changed before its health input");p.setHealth(19);lifeStarted=true;});return false;}
        if(!lifeStarted||RechargeSupport.getCharge(physical(mc.player,"life"))!=15||mc.player.getHealth()<19.99F)return false;
        if(phase==1){phase=2;submit(mc,()->{var p=player(mc);require(p.getHealth()==20&&RechargeSupport.getCharge(physical(p,"life"))==15,"Native one-health/5charge life operation changed");LogUtils.getLogger().info("THAUMCRAFT_LATE_ACCESSORIES_LIFE_OK: nativeCuriosTick/type1/rawHealth19to20/rawCharge20to15/noDirectHealCall");});return false;}return true;
    }
    private static boolean sustain(Minecraft mc){
        if(phase==0){if(!changeCurio(mc,"charm","sustain"))return false;phase=1;close(mc);submit(mc,()->{var p=player(mc);require(RechargeSupport.getCharge(physical(p,"sustain"))==10,"Raw sustenance charge changed before its inputs");
            for(var q:BlockPos.betweenClosed(3,112,-1,5,113,1))if(q.getX()!=4||q.getZ()!=0)p.serverLevel().setBlockAndUpdate(q,Blocks.GLASS.defaultBlockState());
            p.serverLevel().setBlockAndUpdate(new BlockPos(4,112,0),Blocks.WATER.defaultBlockState());p.serverLevel().setBlockAndUpdate(new BlockPos(4,113,0),Blocks.WATER.defaultBlockState());
            p.connection.teleport(4.5,112,.5,180,0);p.setAirSupply(25);p.getFoodData().setFoodLevel(19);sustainStarted=true;
            LogUtils.getLogger().info("THAUMCRAFT_LATE_ACCESSORIES_SUSTAIN_FIXTURE: onceInitialAir25Food19Charge10/actualWaterEyeContact/noActiveAirFoodChargeResets");
        });return false;}
        return airPaid&&foodPaid&&RechargeSupport.getCharge(physical(mc.player,"sustain"))==8&&mc.player.getFoodData().getFoodLevel()==20;
    }
    private static boolean band(Minecraft mc){
        if(phase==0){if(!changeCurio(mc,"charm",null))return false;phase=1;wearPhase=0;close(mc);submit(mc,()->{var p=player(mc);p.serverLevel().setBlockAndUpdate(new BlockPos(4,112,0),Blocks.AIR.defaultBlockState());p.serverLevel().setBlockAndUpdate(new BlockPos(4,113,0),Blocks.AIR.defaultBlockState());p.connection.teleport(.5,112,.5,180,0);});return false;}
        if(phase==1){if(!changeCurio(mc,"head","band"))return false;phase=2;close(mc);bandDraws=GogglesCurioRenderer.draws();mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);return false;}
        return mc.screen==null&&marked(AccessoriesSupport.equipped(mc.player).get(4),"band")&&GogglesCurioRenderer.draws()>bandDraws;
    }
    private static boolean xp(Minecraft mc){
        if(phase==0){phase=1;submit(mc,()->{var p=player(mc);theoryBefore=theory(p);experienceBefore=p.totalExperience;
            var orb=new ExperienceOrb(p.serverLevel(),p.getX()+.2,p.getY()+.1,p.getZ(),100);require(p.serverLevel().addFreshEntity(orb),"Raw native XP orb insertion rejected");xpStarted=true;
            LogUtils.getLogger().info("THAUMCRAFT_LATE_ACCESSORIES_XP_FIXTURE: oneInitialNativeValue100Count1Orb/naturalContact/noDirectPlayerTouchOrPickupEvent");
        });return false;}
        return xpPaid&&mc.player.totalExperience==experienceBefore+50;
    }
    private static boolean undyingEquip(Minecraft mc){
        if(phase==0){if(!changeCurio(mc,"charm","undying"))return false;phase=1;return false;}
        return curios(mc)&&marked(AccessoriesSupport.equipped(mc.player).get(6),"undying");
    }
    private static boolean rescue(Minecraft mc){
        if(phase==0){phase=1;close(mc);submit(mc,()->{var p=player(mc);p.setInvulnerable(false);require(!physical(p,"undying").isEmpty(),"Physical undying input absent");
            p.hurt(p.damageSources().generic(),1000);
            require(p.isAlive()&&p.getHealth()==1&&physical(p,"undying").isEmpty()&&p.hasEffect(MobEffects.REGENERATION)&&p.hasEffect(MobEffects.ABSORPTION),"Actual fatal damage did not consume/rescue via native charm");rescued=true;
            LogUtils.getLogger().info("THAUMCRAFT_LATE_ACCESSORIES_UNDYING_OK: actualHurt1000/nativeLivingDeath/onePhysicalCharmConsumed/health1/regen900amp1/absorption100amp1/entityState35/noManualDeathEvent");
        });return false;}
        return rescued&&physical(mc.player,"undying").isEmpty()&&mc.player.getHealth()>0&&mc.player.hasEffect(MobEffects.REGENERATION)&&mc.player.hasEffect(MobEffects.ABSORPTION);
    }
    private static boolean cloudEquip(Minecraft mc){
        // Observe the real activation animation's natural expiry; never clear its timer.
        if(phase==0&&net.minecraftforge.fml.util.ObfuscationReflectionHelper.<Integer,net.minecraft.client.renderer.GameRenderer>getPrivateValue(net.minecraft.client.renderer.GameRenderer.class,mc.gameRenderer,"f_109047_")>0)return false;
        if(phase==0){if(!changeCurio(mc,"ring","cloud"))return false;phase=1;return false;}
        return curios(mc)&&marked(AccessoriesSupport.equipped(mc.player).get(1),"cloud");
    }
    private static boolean cloudJump(Minecraft mc){
        if(phase==0){phase=1;close(mc);mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);mc.player.setXRot(-45);actionTick=mc.level.getGameTime();return false;}
        if(phase==1){if(!mc.player.onGround()||!elapsed(mc,3))return false;mc.options.keyJump.setDown(true);phase=2;return false;}
        if(phase==2){if(mc.player.onGround())return false;mc.options.keyJump.setDown(false);require(mc.player.getDeltaMovement().y>0,"Native first jump produced no upward movement");phase=3;actionTick=mc.level.getGameTime();LogUtils.getLogger().info("THAUMCRAFT_LATE_ACCESSORIES_FIRST_JUMP: nativeLocalPlayerInput/noMotionSetter");return false;}
        if(phase==3){if(!elapsed(mc,5)||CloudRingSupport.jumpDelay(mc.player)!=0)return false;require(!mc.player.onGround(),"First native jump landed before second click");cloudBefore.clear();cloudBefore.addAll(thaumcraft.fx.client.FxEngine.snapshot());cloudEmittedBefore=thaumcraft.fx.client.FxEngine.emitted();KeyMapping.click(mc.options.keyJump.getKey());phase=4;actionTick=mc.level.getGameTime();return false;}
        if(mc.player.getDeltaMovement().y>.6){cloudPredicted=true;}
        require(!elapsed(mc,60),"Native cloud prediction/server validation absent");
        return cloudPredicted&&cloudAccepted&&!mc.player.onGround();
    }
    private static boolean thirdJump(Minecraft mc){
        if(phase==0){if(mc.player.onGround()||mc.player.getDeltaMovement().y>.45)return false;
            require(observedClientCloudImpulses==1&&observedServerCloudImpulses==1,"Exactly one native cloud impulse per logical side must precede the third click");
            thirdJumpBefore=mc.player.getDeltaMovement().y;
            auditFlight(mc.player,"before-third-native-click");
            KeyMapping.click(mc.options.keyJump.getKey());phase=1;actionTick=mc.level.getGameTime();return false;}
        auditFlight(mc.player,"after-third-native-click");
        require(mc.player.getDeltaMovement().y<=thirdJumpBefore+.05,"Third native click added a second cloud impulse within the same flight: before="+thirdJumpBefore+", now="+mc.player.getDeltaMovement().y+", ground="+mc.player.onGround()+", position="+mc.player.position()+", clientImpulses="+observedClientCloudImpulses+", serverImpulses="+observedServerCloudImpulses);
        if(!elapsed(mc,3))return false;
        require(!mc.player.onGround()&&observedClientCloudImpulses==1&&observedServerCloudImpulses==1,"Third click must remain in the same flight without a second client or server cloud impulse");
        if(phase==1){phase=2;LogUtils.getLogger().info("THAUMCRAFT_LATE_ACCESSORIES_THIRD_JUMP_REJECTED: actualKeyBindingClick/sameFlight/noNewUpwardImpulse/clientBefore{}clientNow{}",thirdJumpBefore,mc.player.getDeltaMovement().y);}return true;
    }
    /** Read-only native hook observations; never calls jump or changes per-flight state. */
    @SubscribeEvent public static void observedJump(net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent event){
        if(!enabled()||stopped||owner==null||scene<11||scene>12||!(event.getEntity() instanceof net.minecraft.world.entity.player.Player p)||!owner.equals(p.getUUID()))return;
        if(Math.abs(p.getDeltaMovement().y-.75)<1e-8){if(p.level().isClientSide)observedClientCloudImpulses++;else observedServerCloudImpulses++;}
        auditFlight(p,"native-living-jump-hook");
    }
    private static void auditFlight(net.minecraft.world.entity.player.Player p,String observation){
        try{
            var field=CloudRingSupport.class.getDeclaredField(p.level().isClientSide?"CLIENT_STATES":"SERVER_STATES");field.setAccessible(true);
            var states=(Map<?,?>)field.get(null);var entries=new ArrayList<String>();
            synchronized(states){for(var entry:states.entrySet()){
                var key=(net.minecraft.world.entity.player.Player)entry.getKey();var state=entry.getValue();
                var level=state.getClass().getDeclaredField("level");level.setAccessible(true);var used=state.getClass().getDeclaredField("used");used.setAccessible(true);
                entries.add("keySide="+(key.level().isClientSide?"client":"server")+"/keyIdentity="+System.identityHashCode(key)+"/sameObject="+(key==p)+"/equalPlayer="+key.equals(p)+"/storedLevelSame="+(level.get(state)==p.level())+"/used="+used.getBoolean(state));
            }}
            LogUtils.getLogger().info("THAUMCRAFT_LATE_ACCESSORIES_NATIVE_FLIGHT: observation={} scene={} phase={} side={} entityId={} identity={} tick={} gameTime={} ground={} jumpDelay={} position={} velocity={} clientCloudImpulses={} serverCloudImpulses={} states={}",observation,scene,phase,p.level().isClientSide?"client":"server",p.getId(),System.identityHashCode(p),p.tickCount,p.level().getGameTime(),p.onGround(),CloudRingSupport.jumpDelay(p),p.position(),p.getDeltaMovement(),observedClientCloudImpulses,observedServerCloudImpulses,entries);
        }catch(ReflectiveOperationException failure){throw new IllegalStateException("Read-only Cloud ring flight audit failed",failure);}
    }
    /** Resolve the actual client override and quad texture IDs without changing animation state. */
    private static void auditVerdantModels(Minecraft mc){
        String[] names={"base","life","sustain"};
        for(int expected=0;expected<names.length;expected++){
            var stack=physical(mc.player,names[expected]);int type=VerdantCharmItem.type(stack);
            var getter=net.minecraft.client.renderer.item.ItemProperties.getProperty(stack.getItem(),ResourceLocation.fromNamespaceAndPath("thaumcraft","type"));
            float predicate=getter==null?Float.NaN:getter.call(stack,mc.level,mc.player,0);
            var model=mc.getItemRenderer().getModel(stack,mc.level,mc.player,0);
            var sprites=model.getQuads(null,null,net.minecraft.util.RandomSource.create(0)).stream().map(q->q.getSprite().contents().name().toString()).collect(java.util.stream.Collectors.toCollection(TreeSet::new));
            require(stack.getCount()==1&&type==expected&&predicate==expected,"Client lost original Verdant type/predicate: "+names[expected]+"/"+stack.getTag()+"/"+predicate);
            require(sprites.contains("thaumcraft:items/verdant_charm_over_"+expected),"Actual Verdant override selected wrong texture: "+names[expected]+"/"+sprites);
            LogUtils.getLogger().info("THAUMCRAFT_LATE_ACCESSORIES_NATIVE_VERDANT_MODEL: scene={} side=client fixture={} count={} tag={} type={} actualPredicate={} actualQuadTextures={} animationClockUnmodified/original11frameOverlaysMayConvergeVisually",scene,names[expected],stack.getCount(),stack.getTag(),type,predicate,sprites);
        }
    }
    /** Observe the production particle draw with this actual world's camera and matrices. */
    @SubscribeEvent public static void cloudFrame(net.minecraftforge.client.event.RenderLevelStageEvent event){
        if(!enabled()||stopped||event.getStage()!=net.minecraftforge.client.event.RenderLevelStageEvent.Stage.AFTER_PARTICLES||scene!=11||phase<4)return;
        var mc=Minecraft.getInstance();cloudFrameReady=false;cloudFrameWorldTime=mc.level.getGameTime();var facts=new ArrayList<String>();
        for(var particle:thaumcraft.fx.client.FxEngine.snapshot()){
            if(cloudBefore.contains(particle)||!particle.isAlive()||particle.grid!=16||particle.start!=123||particle.age()<6||particle.age()>12)continue;
            var vertices=new CloudVertices(event.getProjectionMatrix());
            // CPU-only sink: no buffer submission, effect emission or particle/timer mutation.
            particle.draw(vertices,event.getCamera(),event.getPartialTick(),event.getPoseStack().last().pose());
            if(vertices.visible>=2){cloudFrameReady=true;facts.add("age="+particle.age()+"/"+particle.lifespan()+",position="+particle.position()+",texture="+particle.texture+",quad="+vertices.facts);}
        }
        cloudFrameFacts=List.copyOf(facts);
    }
    private static final class CloudVertices implements com.mojang.blaze3d.vertex.VertexConsumer {
        private final org.joml.Matrix4f projection;
        private final List<String> facts=new ArrayList<>(4);
        private double x,y,z;private int alpha,visible;
        private CloudVertices(org.joml.Matrix4f projection){this.projection=new org.joml.Matrix4f(projection);}
        @Override public com.mojang.blaze3d.vertex.VertexConsumer vertex(double x,double y,double z){this.x=x;this.y=y;this.z=z;return this;}
        @Override public com.mojang.blaze3d.vertex.VertexConsumer color(int r,int g,int b,int a){alpha=a;return this;}
        @Override public com.mojang.blaze3d.vertex.VertexConsumer uv(float u,float v){return this;}
        @Override public com.mojang.blaze3d.vertex.VertexConsumer overlayCoords(int u,int v){return this;}
        @Override public com.mojang.blaze3d.vertex.VertexConsumer uv2(int u,int v){return this;}
        @Override public com.mojang.blaze3d.vertex.VertexConsumer normal(float x,float y,float z){return this;}
        @Override public void endVertex(){
            var clip=projection.transform(new org.joml.Vector4f((float)x,(float)y,(float)z,1));
            float nx=clip.x/clip.w,ny=clip.y/clip.w;
            if(clip.w>0&&alpha>=48&&nx>-.95F&&nx<.95F&&ny>-.75F&&ny<.9F)visible++;
            facts.add("view="+x+","+y+","+z+",clipW="+clip.w+",ndc="+nx+","+ny+",alpha="+alpha);
        }
        @Override public void defaultColor(int r,int g,int b,int a){alpha=a;}
        @Override public void unsetDefaultColor(){}
    }
    @SubscribeEvent public static void serverTick(TickEvent.ServerTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!enabled()||stopped||!prepared||owner==null||!WORLD.equals(event.getServer().getWorldData().getLevelName()))return;
        try{
            var p=event.getServer().getPlayerList().getPlayer(owner);if(p==null)return;worldTime=p.serverLevel().getGameTime();
            if(curesStarted&&cureStep<3){int charge=RechargeSupport.getCharge(physical(p,"base"));if(charge!=previousCureCharge){
                int[] expected={20,10,5};require(charge==expected[cureStep],"Native cure order/charge changed at step "+cureStep);
                if(cureStep==0)require(!p.hasEffect(MobEffects.WITHER)&&p.hasEffect(MobEffects.POISON)&&p.hasEffect(InfusionEffects.FLUX_TAINT.get()),"Wither did not have first priority");
                if(cureStep==1)require(!p.hasEffect(MobEffects.POISON)&&p.hasEffect(InfusionEffects.FLUX_TAINT.get()),"Poison did not have second priority");
                if(cureStep==2)require(!p.hasEffect(InfusionEffects.FLUX_TAINT.get()),"Flux Taint was not removed");
                require(p.tickCount%20==0,"Native Verdant did not use20wearer tick cadence");previousCureCharge=charge;cureStep++;
                LogUtils.getLogger().info("THAUMCRAFT_LATE_ACCESSORIES_CURE_STEP_OK: step={} nativeWearerTick={} actualCharge={} noCurioTickOrCureCalls",cureStep,p.tickCount,charge);
            }}
            if(sustainStarted&&!foodPaid){int charge=RechargeSupport.getCharge(physical(p,"sustain"));if(charge!=sustainCharge){
                if(!airPaid){require(charge==9&&p.getAirSupply()>=270&&p.getFoodData().getFoodLevel()==19,"Native air-first operation changed");airPaid=true;}
                else{require(charge==8&&p.getFoodData().getFoodLevel()==20,"Native sustenance food operation changed");foodPaid=true;}
                sustainCharge=charge;LogUtils.getLogger().info("THAUMCRAFT_LATE_ACCESSORIES_SUSTAIN_OK: nativeTick={} charge={} air={} food={} airPaid={} foodPaid={}",p.tickCount,charge,p.getAirSupply(),p.getFoodData().getFoodLevel(),airPaid,foodPaid);
            }}
            if(xpStarted&&!xpPaid&&p.totalExperience==experienceBefore+50&&theory(p)==theoryBefore+1){xpPaid=true;LogUtils.getLogger().info("THAUMCRAFT_LATE_ACCESSORIES_XP_OK: actualNativeOrbContact/value100to50Payment/theoryRawPoint1/realEquippedHeadBand/noDirectKnowledgeAward");}
            if(scene==11&&phase>=4&&!cloudAccepted&&p.getDeltaMovement().y>.6&&!p.onGround()){
                require(marked(AccessoriesSupport.equipped(p).get(1),"cloud"),"Cloud server impulse lacked real ring ownership");cloudAccepted=true;
                LogUtils.getLogger().info("THAUMCRAFT_LATE_ACCESSORIES_CLOUD_SERVER_OK: actualKeyClick/clientPrediction/nativeC2S/physicalRing/authoritativeVertical{}fallDistance{}",p.getDeltaMovement().y,p.fallDistance);
            }
        }catch(Throwable error){Minecraft.getInstance().execute(()->fail(Minecraft.getInstance(),error));}
    }
    private static void prepare(ServerPlayer p){
        var level=p.serverLevel();for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)level.getChunk(x,z);
        for(var pos:BlockPos.betweenClosed(-8,111,-8,8,120,8))level.setBlock(pos,pos.getY()==111?Blocks.BEDROCK.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
        level.setDayTime(6000);p.setInvulnerable(true);p.getInventory().clearContent();
        p.getInventory().setItem(9,raw("verdant_charm","base",0,40));p.getInventory().setItem(10,raw("verdant_charm","life",1,20));p.getInventory().setItem(11,raw("verdant_charm","sustain",2,10));
        p.getInventory().setItem(12,raw("curiosity_band","band",0,-1));p.getInventory().setItem(13,raw("charm_undying","undying",0,-1));p.getInventory().setItem(14,raw("cloud_ring","cloud",0,-1));
        p.connection.teleport(.5,112,.5,180,0);p.inventoryMenu.broadcastChanges();
        LogUtils.getLogger().info("THAUMCRAFT_LATE_ACCESSORIES_FIXTURES: ownedNewSurvivalWorld/rawSixPhysicalAccessories/initialBase40Life20Sustain10Charge/oneLaterInitialCureHealthAirFoodScenarioInput/nativeCuriosInventoryButtonAndC2SClicks/oneNative100XPOrbContact/oneActualFatalHurt/nativeJumpKeyBinding/noPaidResearchOrCraftClaim/noInjectedEffectsOrActiveChargeClockRngResets/notFullSurvivalMultiplayerAudio");
    }
    @SubscribeEvent public static void rendered(TickEvent.RenderTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!enabled()||stopped||!captureRequested||captured)return;var mc=Minecraft.getInstance();
        try{
            if(scene==11&&(!cloudFrameReady||cloudFrameWorldTime!=mc.level.getGameTime()))return;
            require(mc.getWindow().getScreenWidth()==1600&&mc.getWindow().getScreenHeight()==1000,"Expected1600x1000 native capture");captured=true;captureRequested=false;stable=0;
            if(scene==11)LogUtils.getLogger().info("THAUMCRAFT_LATE_ACCESSORIES_CLOUD_NATIVE_FRAME: sameWorldFrame={} camera={} rotation={} nativeEmittedDelta={} baselineParticles={} actualNewBamfQuads={} readOnlyActualDrawPoseAndProjection/noEmissionOrActiveAgeRngMutation",cloudFrameWorldTime,mc.gameRenderer.getMainCamera().getPosition(),mc.gameRenderer.getMainCamera().rotation(),thaumcraft.fx.client.FxEngine.emitted()-cloudEmittedBefore,cloudBefore.size(),cloudFrameFacts);
            auditVerdantModels(mc);
            String name="tc6-late-accessories-"+IMAGES[scene]+".png";var file=new File(new File(mc.gameDirectory,"screenshots"),name);require(!file.exists(),"Preserve existing capture "+name);
            LogUtils.getLogger().info("THAUMCRAFT_LATE_ACCESSORIES_NATIVE_FRAME: name={} camera={} mode={} health={} baseCharge={} lifeCharge={} sustainCharge={} screen={}",name,mc.gameRenderer.getMainCamera().getPosition(),mc.options.getCameraType(),mc.player.getHealth(),RechargeSupport.getCharge(physical(mc.player,"base")),RechargeSupport.getCharge(physical(mc.player,"life")),RechargeSupport.getCharge(physical(mc.player,"sustain")),mc.screen==null?"world":mc.screen.getClass().getSimpleName());
            Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),message->{if(file.isFile()&&file.length()>0){SAVED.incrementAndGet();LogUtils.getLogger().info("THAUMCRAFT_LATE_ACCESSORIES_CLIENT_SMOKE_IMAGE: {}",file.getAbsolutePath());}else mc.execute(()->fail(mc,new AssertionError("Native screenshot absent")));});
        }catch(Throwable error){fail(mc,error);}
    }
    private static void start(Minecraft mc){
        if(mc.screen instanceof AccessibilityOnboardingScreen){mc.options.onboardAccessibility=false;mc.options.save();mc.setScreen(new TitleScreen());return;}
        if(!(mc.screen instanceof TitleScreen)||mc.getOverlay()!=null)return;started=true;previous=mc.options.tutorialStep;mc.options.tutorialStep=TutorialSteps.NONE;mc.getTutorial().stop();
        mc.options.pauseOnLostFocus=false;mc.options.renderDistance().set(3);mc.options.simulationDistance().set(5);mc.options.guiScale().set(2);mc.options.fov().set(55);mc.options.bobView().set(false);mc.options.cloudStatus().set(CloudStatus.OFF);mc.options.hideGui=false;mc.options.setCameraType(CameraType.FIRST_PERSON);mc.resizeDisplay();
        var rules=new GameRules();rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false,null);rules.getRule(GameRules.RULE_DAYLIGHT).set(false,null);rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false,null);rules.getRule(GameRules.RULE_RANDOMTICKING).set(0,null);rules.getRule(GameRules.RULE_NATURAL_REGENERATION).set(false,null);
        mc.createWorldOpenFlows().createFreshLevel(WORLD,new LevelSettings(WORLD,GameType.SURVIVAL,false,Difficulty.NORMAL,true,rules,WorldDataConfiguration.DEFAULT),new WorldOptions(0x544336042AL,false,false),WorldPresets::createNormalWorldDimensions);
    }
    private static void finish(Minecraft mc){
        if(phase==0){phase=1;submit(mc,()->{var p=player(mc);
            require(cureStep==3&&airPaid&&foodPaid&&xpPaid&&rescued&&cloudAccepted&&cloudPredicted,"Native late accessory route incomplete");
            require(RechargeSupport.getCharge(physical(p,"base"))==5&&RechargeSupport.getCharge(physical(p,"life"))==15&&RechargeSupport.getCharge(physical(p,"sustain"))==8,"Final physical charm charges changed");
            String[] forms={"base","life","sustain"};for(int i=0;i<forms.length;i++){var charm=physical(p,forms[i]);require(charm.getCount()==1&&VerdantCharmItem.type(charm)==i,"Curios did not conserve original server Verdant form "+forms[i]+"/"+charm.getTag());LogUtils.getLogger().info("THAUMCRAFT_LATE_ACCESSORIES_NATIVE_VERDANT_FINAL: side=server fixture={} count={} type={} tag={}",forms[i],charm.getCount(),VerdantCharmItem.type(charm),charm.getTag());}
            require(physical(p,"undying").isEmpty()&&marked(AccessoriesSupport.equipped(p).get(4),"band")&&marked(AccessoriesSupport.equipped(p).get(1),"cloud"),"Consumed/worn physical ownership changed");
            require(theory(p)==theoryBefore+1&&p.totalExperience==experienceBefore+50,"Actual curiosity payment drifted");
            LogUtils.getLogger().info("THAUMCRAFT_LATE_ACCESSORIES_CLIENT_FACTS: captures14/rawSixItems/oneConsumedCharmUndying/fiveConservedPhysicalItems/threeVerdantTypeSprites/nativeCuriosClicks/nativeCureCharge40to5/nativeLifeCharge20to15/nativeSustainCharge10to8/nativeXP100to50+Theory1/nativeDamageRescue/nativeCloudKeyPrediction+C2S/thirdClickRejected/actualLanding; rawGearChargeEffectsHealthAirFoodXPOrbAreFixtures/notPaidCrafting/notFullSurvivalMultiplayerAudio");
        });return;}
        require(SAVED.get()==IMAGES.length,"Incomplete native captures");stopped=true;mc.options.keyJump.setDown(false);mc.options.tutorialStep=previous;
        LogUtils.getLogger().info("THAUMCRAFT_LATE_ACCESSORIES_RENDER_AUDIT_OK: 14captures/originalThreeVerdantForms/realCuriosGUI/nativeAfflictionHUD/originalWornCuriosity/XPContact/actualFatalDamageTotemState/nativeCloudBamf/actualPixelsRequireReview");
        LogUtils.getLogger().info("THAUMCRAFT_LATE_ACCESSORIES_CLIENT_SMOKE_OK: 14 captures,world={}",WORLD);mc.stop();
    }
    private static void fail(Minecraft mc,Throwable error){if(stopped)return;stopped=true;mc.options.keyJump.setDown(false);if(previous!=null)mc.options.tutorialStep=previous;LogUtils.getLogger().error("THAUMCRAFT_LATE_ACCESSORIES_CLIENT_SMOKE_FAILED",error);mc.stop();}
}
