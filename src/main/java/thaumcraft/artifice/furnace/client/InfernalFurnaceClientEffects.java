package thaumcraft.artifice.furnace.client;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

/** Original event3: five native lava particles and five local lava-pop sounds. */
public final class InfernalFurnaceClientEffects {
    private static int events,particles;
    private InfernalFurnaceClientEffects() {}
    public static boolean handle(Level level,BlockPos pos,net.minecraft.world.level.block.state.BlockState state,int event) {
        if(event!=3)return false;
        event(level,pos,state.getValue(thaumcraft.artifice.furnace.InfernalFurnaceBlock.FACING).getOpposite());return true;
    }
    public static void event(Level level,BlockPos pos,Direction output) {
        if(!level.isClientSide)return;
        int x=output.getStepX(),z=output.getStepZ();var random=level.random;
        for(int i=0;i<5;i++) {
            float qx=x==0?(random.nextFloat()-random.nextFloat())*.5F:x*random.nextFloat();
            float qz=z==0?(random.nextFloat()-random.nextFloat())*.5F:z*random.nextFloat();
            level.addParticle(ParticleTypes.LAVA,pos.getX()+.5F+(random.nextFloat()-random.nextFloat())*.3F+x,pos.getY()+.3F,pos.getZ()+.5F+(random.nextFloat()-random.nextFloat())*.3F+z,.15F*qx,.2F*random.nextFloat(),.15F*qz);
            level.playLocalSound(pos.getX()+.5F,pos.getY()+.5F,pos.getZ()+.5F,SoundEvents.LAVA_POP,SoundSource.BLOCKS,.1F+random.nextFloat()*.1F,.9F+random.nextFloat()*.15F,false);
        }
        if(Boolean.getBoolean("thaumcraft.infernalSmokeTest")) { events++;particles+=5; }
    }
    public static int events() { return events; }
    public static int particles() { return particles; }
}
