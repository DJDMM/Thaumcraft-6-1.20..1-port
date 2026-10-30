package thaumcraft.world.eldritch.client;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="thaumcraft",value=net.minecraftforge.api.distmarker.Dist.CLIENT,bus=net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD)
public final class EldritchClient {
 private EldritchClient(){}
 @net.minecraftforge.eventbus.api.SubscribeEvent public static void renderers(net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers e){e.registerEntityRenderer(thaumcraft.world.eldritch.EldritchModule.INHABITED_ZOMBIE.get(),context->new net.minecraft.client.renderer.entity.ZombieRenderer(context){@Override public ResourceLocation getTextureLocation(net.minecraft.world.entity.monster.Zombie zombie){return ResourceLocation.fromNamespaceAndPath("thaumcraft","textures/entity/czombie.png");}});}
 public static void crabHelmet(Entity entity){if(!(entity.level() instanceof ClientLevel level))return;var item=ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("thaumcraft","crimson_plate_chest"));if(item==null)return;for(int i=0;i<5;i++)level.addParticle(new ItemParticleOption(ParticleTypes.ITEM,new ItemStack(item)),entity.getX(),entity.getY()+entity.getBbHeight()*.75,entity.getZ(),level.random.nextGaussian()*.05,.1,level.random.nextGaussian()*.05);entity.playSound(net.minecraft.sounds.SoundEvents.ITEM_BREAK,.8F,.8F+level.random.nextFloat()*.4F);}
 public static void inhabitedDeath(Entity entity){if(!(entity.level() instanceof ClientLevel level))return;for(int i=0;i<20;i++)level.addParticle(ParticleTypes.POOF,entity.getX()+level.random.nextFloat()*entity.getBbWidth()*2-entity.getBbWidth(),entity.getY()+level.random.nextFloat()*entity.getBbHeight(),entity.getZ()+level.random.nextFloat()*entity.getBbWidth()*2-entity.getBbWidth(),level.random.nextGaussian()*.02,level.random.nextGaussian()*.02,level.random.nextGaussian()*.02);}
}
