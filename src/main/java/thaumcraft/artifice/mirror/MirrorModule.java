package thaumcraft.artifice.mirror;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.registries.*;
import thaumcraft.catalog.blocks.CatalogBlocks;

/** Registries preserve existing item/block IDs; output retries never load a chunk. */
public final class MirrorModule {
    private static final DeferredRegister<BlockEntityType<?>> TILES=DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES,"thaumcraft");
    private static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(ForgeRegistries.MENU_TYPES,"thaumcraft");
    public static final RegistryObject<BlockEntityType<ItemMirrorBlockEntity>> ITEM_MIRROR=TILES.register("mirror",()->BlockEntityType.Builder.of(ItemMirrorBlockEntity::new,CatalogBlocks.block("mirror")).build(null));
    public static final RegistryObject<BlockEntityType<EssentiaMirrorBlockEntity>> ESSENTIA_MIRROR=TILES.register("mirror_essentia",()->BlockEntityType.Builder.of(EssentiaMirrorBlockEntity::new,CatalogBlocks.block("mirror_essentia")).build(null));
    public static final RegistryObject<MenuType<HandMirrorMenu>> HAND_MIRROR_MENU=MENUS.register("hand_mirror",()->IForgeMenuType.create(HandMirrorMenu::new));
    // The brain jar already owns the shared original sound registry ID.
    public static final RegistryObject<SoundEvent> JAR=thaumcraft.golemancy.jar.BrainJarModule.JAR;
    private MirrorModule(){}
    public static boolean handlesBlock(String id){return id.equals("mirror")||id.equals("mirror_essentia");}
    public static Block createBlock(String id){if(!handlesBlock(id))throw new IllegalArgumentException(id);return new MirrorBlock(id.equals("mirror_essentia"));}
    public static BlockItem createBlockItem(Block block){return new MirrorBlockItem(block);}
    public static boolean handlesItem(String id){return id.equals("hand_mirror");}
    public static Item createItem(String id){return handlesItem(id)?new HandMirrorItem():null;}
    public static void register(IEventBus bus){
        TILES.register(bus);MENUS.register(bus);
        MinecraftForge.EVENT_BUS.addListener(MirrorModule::tick);MinecraftForge.EVENT_BUS.addListener(MirrorModule::logout);
        MinecraftForge.EVENT_BUS.addListener(net.minecraftforge.eventbus.api.EventPriority.LOWEST,true,MirrorPlacementReceipt::event);
        MinecraftForge.EVENT_BUS.addListener(MirrorModule::unload);
        DistExecutor.safeRunWhenOn(Dist.CLIENT,()->thaumcraft.artifice.mirror.client.MirrorClient::register);
    }
    private static void tick(TickEvent.LevelTickEvent event){if(event.phase==TickEvent.Phase.END&&event.level instanceof net.minecraft.server.level.ServerLevel server){MirrorPlacementReceipt.tick(server);MirrorRefundSavedData.get(server).tick(server);}}
    private static void unload(net.minecraftforge.event.level.LevelEvent.Unload event){if(event.getLevel() instanceof net.minecraft.server.level.ServerLevel server)MirrorPlacementReceipt.forget(server);}
    private static void logout(PlayerEvent.PlayerLoggedOutEvent event){if(event.getEntity().containerMenu instanceof HandMirrorMenu menu)menu.removed(event.getEntity());}
    public static void playJar(Level level,BlockPos pos,float volume,float pitch){level.playSound(null,pos,JAR.get(),SoundSource.BLOCKS,volume,pitch);}
    static void playTeleport(Player player){player.level().playSound(null,player.blockPosition(),net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT,SoundSource.PLAYERS,.1F,1);}
    static void playZap(Player player){player.level().playSound(null,player.blockPosition(),thaumcraft.golemancy.entity.GolemEntitySounds.ZAP.get(),SoundSource.PLAYERS,1,.8F);}
    static void linkTooltip(ItemStack stack,List<Component> lines){
        MirrorLink link=MirrorLink.read(stack);if(link==null)return;
        var dimension=link.dimension();Component name;
        if(dimension.equals(Level.OVERWORLD))name=Component.translatable("tc.dimension.overworld");
        else if(dimension.equals(Level.NETHER))name=Component.translatable("tc.dimension.nether");
        else if(dimension.equals(Level.END))name=Component.translatable("tc.dimension.end");
        else name=Component.literal(dimension.location().getPath().replace('_',' '));
        lines.add(Component.translatable("tc.handmirrorlinkedto").append(" ").append(Component.translatable("tc.mirrorlinkdetail",link.pos().getX(),link.pos().getY(),link.pos().getZ(),name)));
    }
}
