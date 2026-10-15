package thaumcraft.artifice.pattern;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import thaumcraft.world.aura.AuraManager;

/** Released twenty-tick same-material crafter, with detached recipe plans and physical payment. */
public final class PatternCrafterBlockEntity extends BlockEntity {
    public byte type;public float rot,rp;public int rotTicks;
    private int count=new Random(System.currentTimeMillis()).nextInt(20);
    private float power;private UUID ownerId=UUID.randomUUID();private boolean ticking,released;
    public PatternCrafterBlockEntity(BlockPos pos,BlockState state){super(PatternCrafterModule.BLOCK_ENTITY.get(),pos,state);}
    public int type(){return type;}public int count(){return count;}public float power(){return power;}public UUID ownerId(){return ownerId;}
    public int pendingOutputCount(){return level instanceof ServerLevel server?PatternOutputData.get(server).pendingCount(ownerId):0;}
    private boolean attached(){return level instanceof ServerLevel server&&!isRemoved()&&!released&&NativePatternInventory.loaded(server,worldPosition)&&server.getBlockEntity(worldPosition)==this&&!server.captureBlockSnapshots&&!server.restoringBlockSnapshots&&!PatternCrafterBlockItem.uncommitted(server,worldPosition);}
    public static void tick(Level level,BlockPos pos,BlockState state,PatternCrafterBlockEntity tile){if(level.isClientSide)tile.clientTick();else tile.serverTick();}
    private void clientTick(){if(rotTicks>0){--rotTicks;if(rotTicks%Math.floor(Math.max(1,rp))==0){var sound=ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("thaumcraft","clack"));if(sound!=null)level.playLocalSound(worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5,sound,SoundSource.BLOCKS,.2f,1.7f,false);}++rp;}else rp*=.8f;rot+=rp;}
    public void serverTick(){if(ticking||!attached())return;ticking=true;try{
        // Release increments even while powered; its local clock is not persisted.
        if(count++%20!=0||!getBlockState().getValue(PatternCrafterBlock.ENABLED))return;
        ServerLevel server=(ServerLevel)level;
        if(power<=0){power+=AuraManager.drainVis(server,worldPosition,5,false);setChanged();}
        if(!attached()||pendingOutputCount()>0)return;
        NativePatternInventory input=NativePatternInventory.capture(server,worldPosition.above(),net.minecraft.core.Direction.DOWN),output=NativePatternInventory.capture(server,worldPosition.below(),net.minecraft.core.Direction.UP);
        if(input==null||output==null||!attached())return;
        int selected=type,amount=PatternRecipes.amount(selected);
        for(int slot=0;slot<input.size();slot++){
            ItemStack exemplar=input.exemplar(slot);if(exemplar.isEmpty())continue;
            List<NativePatternInventory.Debit> debits=input.debitPlan(exemplar,amount);
            if(!validPlan(input,output,selected))return;if(debits==null)continue;
            var craft=PatternRecipes.craft(server,selected,exemplar);
            if(!validPlan(input,output,selected))return;
            if(craft.isEmpty()||power<1)continue;
            List<ItemStack> outputs=craft.get().allOutputs();var capacity=output.reserve(outputs);
            if(!validPlan(input,output,selected))return;if(capacity==null)continue;
            // These operations cannot invoke external code: the already-observed physical stacks
            // pay first, their result receives a persistent owner, then callbacks may run.
            input.debit(debits);power-=1;PatternOutputData.get(server).add(worldPosition,ownerId,outputs);
            try{input.changed();setChanged();}catch(RuntimeException callback){/* The already-paid result retains its independent owner. */}
            if(attached())server.blockEvent(worldPosition,getBlockState().getBlock(),1,0);
            PatternOutputData.get(server).tick(server);break;
        }
    }finally{ticking=false;}}
    private boolean validPlan(NativePatternInventory input,NativePatternInventory output,int selected){return attached()&&type==selected&&input.stable()&&output.stable()&&getBlockState().getValue(PatternCrafterBlock.ENABLED);}
    public void cycle(){if(!(level instanceof ServerLevel)||!attached())return;type++;if(type>9)type=0;setChanged();level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    public void releasePaidOutputs(){if(released||!(level instanceof ServerLevel server)||server.restoringBlockSnapshots)return;released=true;PatternOutputData.get(server).detach(ownerId);PatternOutputData.get(server).tick(server);}
    @Override public boolean triggerEvent(int event,int value){if(event==1){if(level!=null&&level.isClientSide)rotTicks=10;return true;}return super.triggerEvent(event,value);}
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);tag.putByte("type",type);tag.putFloat("power",power);tag.putUUID("PhysicalOwner",ownerId);}
    @Override public void load(CompoundTag tag){super.load(tag);type=tag.getByte("type");if(type<0||type>9)type=0;float stored=tag.getFloat("power");power=Float.isFinite(stored)?Math.max(0,stored):0;if(tag.hasUUID("PhysicalOwner"))ownerId=tag.getUUID("PhysicalOwner");released=false;}
    @Override public CompoundTag getUpdateTag(){var tag=new CompoundTag();tag.putByte("type",type);return tag;}
    @Override public void handleUpdateTag(CompoundTag tag){type=tag.getByte("type");if(type<0||type>9)type=0;}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    @Override public void onDataPacket(net.minecraft.network.Connection connection,ClientboundBlockEntityDataPacket packet){if(packet.getTag()!=null)handleUpdateTag(packet.getTag());}
}
