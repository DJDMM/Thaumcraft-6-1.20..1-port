package thaumcraft.artifice.lamp;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import thaumcraft.api.aspects.Aspect;

/** Desiderium refill and original exact-class population-limited pairing. */
public final class FertilityLampBlockEntity extends EssentiaLampBlockEntity {
    private int count;
    public FertilityLampBlockEntity(BlockPos pos,BlockState state){super(LampModule.LAMP_FERTILITY.get(),pos,state);}
    @Override protected Aspect fuel(){return Aspect.DESIRE;}
    public int activeTicks(){return count;} public int count(){return count;}
    @Override public int getSuctionAmount(Direction face){return face==face()?128-charges*10:0;}
    public static void tick(Level level,BlockPos pos,BlockState state,FertilityLampBlockEntity lamp){lamp.serverTick();}
    public void serverTick(){
        if(!ready())return;operation=true;
        try{
            if(charges<10){
                if(drawEssentia()){++charges;changed();}
                if(!live())return;
                if(charges<=1)enabled(false);
                else if(Boolean.FALSE.equals(powered()))enabled(true);
            }
            if(live()&&Boolean.FALSE.equals(powered())&&charges>1&&count++%300==0)updateAnimals();
        }finally{operation=false;}
    }
    private void updateAnimals(){
        // Mapped Forge EntitySectionStorage expands its X/Z section search by literal2.0.
        // Include AABB max=block+8 and that margin, not just centre coordinates at+7.
        int margin=2;
        for(int x=(worldPosition.getX()-7-margin)>>4;x<=(worldPosition.getX()+8+margin)>>4;x++)for(int z=(worldPosition.getZ()-7-margin)>>4;z<=(worldPosition.getZ()+8+margin)>>4;z++)
            if(!loaded(level,new BlockPos(x<<4,worldPosition.getY(),z<<4)))return;
        List<Animal> animals=level.getEntitiesOfClass(Animal.class,new AABB(worldPosition).inflate(7));
        for(Animal animal:animals){
            if(animal.getAge()!=0||animal.isInLove())continue;
            List<Animal> same=new ArrayList<>();for(Animal candidate:animals)if(candidate.getClass()==animal.getClass())same.add(candidate);
            if(same.size()>9)continue;
            Animal partner=null;
            for(Animal candidate:same){
                if(candidate.getAge()!=0||candidate.isInLove())continue;
                if(partner!=null){
                    if(!live()||!candidate.isAlive()||!partner.isAlive()||candidate.level()!=level||partner.level()!=level)return;
                    // This can turn2 charges into-3: the released >1 gate and literal5 debit are retained.
                    charges-=5;setChanged();candidate.setInLove(null);partner.setInLove(null);return;
                }partner=candidate;
            }
        }
    }
    @Override protected void readReservoir(CompoundTag tag){charges=tag.getInt("charges");}
    @Override protected void writeReservoir(CompoundTag tag){tag.putInt("charges",charges);}
    @Override protected void resetTransient(){count=0;}
}
