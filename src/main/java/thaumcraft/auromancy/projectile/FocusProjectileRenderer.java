package thaumcraft.auromancy.projectile;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import thaumcraft.fx.FxDispatcher;
import thaumcraft.fx.emitters.*;
import java.util.Map;
import java.util.WeakHashMap;

/** BETA26 RenderNoProjectile: no static mesh, fire motes sampled after each .2 render fraction. */
public final class FocusProjectileRenderer extends EntityRenderer<FocusProjectileEntity> {
    private static final ResourceLocation TEXTURE=ResourceLocation.fromNamespaceAndPath("thaumcraft","textures/misc/particles.png");
    private static final class Sample {float partial;int effectTick=-1;}
    private final Map<FocusProjectileEntity,Sample> samples=new WeakHashMap<>();
    public FocusProjectileRenderer(EntityRendererProvider.Context context){super(context);shadowRadius=.1F;}
    @Override public ResourceLocation getTextureLocation(FocusProjectileEntity entity){return TEXTURE;}
    @Override public void render(FocusProjectileEntity entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
        if(entity.effectKey().isEmpty())return;
        Sample state=samples.computeIfAbsent(entity,k->new Sample());float delta=partial-state.partial;if(delta<0)delta++;
        if(delta>.2F){
            state.partial=partial;var level=entity.level();var r=level.random;
            String keys=entity.visualEffects().isEmpty()?entity.effectKey():entity.visualEffects();
            String key=FocusMediaFx.choose(entity,keys);
            Vec3 point=new Vec3(entity.xo+(entity.getX()-entity.xo)*partial,entity.yo+(entity.getY()-entity.yo)*partial+entity.getBbHeight()/2,entity.zo+(entity.getZ()-entity.zo)*partial);
            Vec3 mote=new Vec3(.0125F*(r.nextFloat()-.5F),.0125F*(r.nextFloat()-.5F),.0125F*(r.nextFloat()-.5F));
            FxDispatcher.emit(level,"fireMote",point,mote,FocusMediaFx.color(key),7,0);
            if(state.effectTick!=entity.tickCount){
                state.effectTick=entity.tickCount;
                FocusFxPrograms.render(level,FocusFxPrograms.jitter(level,point,.10000000149011612),FocusFxPrograms.jitter(level,Vec3.ZERO,.009999999776482582),key);
            }
        }
        super.render(entity,yaw,partial,pose,buffers,light);
    }
}
