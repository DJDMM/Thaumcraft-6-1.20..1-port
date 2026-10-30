package thaumcraft.artifice.late;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.*;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import thaumcraft.api.aspects.*;
public final class PotionSprayerBlockEntity extends LateInventoryBlockEntity implements IAspectContainer,IEssentiaTransport {
    private AspectList recipe=new AspectList(),progress=new AspectList();private int charges,color=0x333333,counter,venting;private boolean activated;private Aspect suction;
    public PotionSprayerBlockEntity(BlockPos p,BlockState s){super(LateDevicesModule.SPRAYER.get(),p,s);}
    public Direction facing(){return getBlockState().getValue(PotionSprayerBlock.FACING);}
    public int charges(){return charges;}
    public int color(){return color;}
    public AspectList recipe(){return recipe.copy();}
    public AspectList progress(){return progress.copy();}
    @Override public boolean canPlaceItem(int slot,ItemStack s){return slot==0&&PotionRecipeAspects.valid(s);}
    @Override public AbstractContainerMenu createMenu(int id,Inventory inv,Player p){return new LateDeviceMenu(id,inv,this);}
    @Override protected void inventoryChanged(){
        if(level!=null&&level.isClientSide)return;recipe=PotionRecipeAspects.recipe(getItem(0));progress=new AspectList();charges=0;
        color=getItem(0).isEmpty()?0x333333:PotionUtils.getColor(PotionUtils.getPotion(getItem(0)));changed();
    }
    public static void tick(Level l,BlockPos p,BlockState s,PotionSprayerBlockEntity t){
        ++t.counter;if(l.isClientSide){if(t.venting>0){--t.venting;net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,()->()->thaumcraft.artifice.late.client.LateDeviceEffects.spray(t,t.venting/2));}return;}
        if(l.restoringBlockSnapshots||l.captureBlockSnapshots)return;
        if(t.counter%5==0){t.suction=null;if(t.isEmpty()||t.charges>=8)return;boolean done=true;
            for(var a:t.recipe.getAspectsSortedByName())if(t.progress.getAmount(a)<t.recipe.getAmount(a)){t.suction=a;done=false;break;}
            if(done){t.progress=new AspectList();++t.charges;t.changed();}else if(t.suction!=null)t.fill();
        }
        if(!s.getValue(PotionSprayerBlock.ENABLED)){
            if(!t.activated&&t.charges>0){--t.charges;var center=p.relative(t.facing(),2);var area=new AABB(center).inflate(1);
                for(var target:l.getEntitiesOfClass(LivingEntity.class,area,e->e.isAlive()&&e.isAffectedByPotions()))for(var effect:PotionUtils.getMobEffects(t.getItem(0))){var type=effect.getEffect();if(type.isInstantenous())type.applyInstantenousEffect(null,null,target,effect.getAmplifier(),1);else target.addEffect(new MobEffectInstance(type,effect.getDuration(),effect.getAmplifier()));}
                l.playSound(null,p,SoundEvents.LAVA_EXTINGUISH,SoundSource.BLOCKS,.25F,2.6F+(l.random.nextFloat()-l.random.nextFloat())*.8F);l.blockEvent(p,s.getBlock(),0,0);t.changed();}
            t.activated=true;
        }else if(t.activated)t.activated=false;
    }
    private void fill(){
        if(level==null)return;for(int y=0;y<=1;y++)for(var d:Direction.values())if(d!=facing()){
            var q=worldPosition.above(y).relative(d);if(!level.hasChunkAt(q)||!(level.getBlockEntity(q) instanceof IEssentiaTransport source))continue;var face=d.getOpposite();
            if(!source.isConnectable(face)||!source.canOutputTo(face)||source.getEssentiaAmount(face)<=0||source.getSuctionAmount(face)>=getSuctionAmount(null)||source.getMinimumSuction()>getSuctionAmount(null))continue;
            int amount=source.takeEssentia(suction,1,face);if(amount>0){addToContainer(suction,amount);return;}
        }
    }
    private boolean mutable(){return level==null||!level.isClientSide;}
    @Override public boolean triggerEvent(int id,int data){if(id>=0){if(level!=null&&level.isClientSide)venting=15;return true;}return super.triggerEvent(id,data);}
    @Override public AspectList getAspects(){return progress.copy();}
    @Override public void setAspects(AspectList a){if(mutable()){progress=a==null?new AspectList():a.copy();changed();}}
    @Override public int addToContainer(Aspect a,int n){if(!mutable()||a==null||n<=0)return n;int add=Math.min(Math.max(0,recipe.getAmount(a)-progress.getAmount(a)),n);if(add>0){progress.add(a,add);changed();}return n-add;}
    @Override public boolean takeFromContainer(Aspect a,int n){return false;}
    @Override public boolean takeFromContainer(AspectList a){return false;}
    @Override public boolean doesContainerContain(AspectList a){return false;}
    @Override public boolean doesContainerContainAmount(Aspect a,int n){return progress.getAmount(a)>=n;}
    @Override public int containerContains(Aspect a){return progress.getAmount(a);}
    @Override public boolean doesContainerAccept(Aspect a){return true;}
    @Override public boolean isConnectable(Direction d){return d!=facing();}
    @Override public boolean canInputFrom(Direction d){return d!=facing();}
    @Override public boolean canOutputTo(Direction d){return false;}
    @Override public void setSuction(Aspect a,int amount){suction=a;}
    @Override public Aspect getSuctionType(Direction d){return suction;}
    @Override public int getSuctionAmount(Direction d){return suction==null?0:128;}
    @Override public int getMinimumSuction(){return 0;}
    @Override public Aspect getEssentiaType(Direction d){return null;}
    @Override public int getEssentiaAmount(Direction d){return 0;}
    @Override public int takeEssentia(Aspect a,int n,Direction d){return 0;}
    @Override public int addEssentia(Aspect a,int n,Direction d){return canInputFrom(d)?n-addToContainer(a,n):0;}
    @Override protected void saveAdditional(CompoundTag n){super.saveAdditional(n);recipe.writeToNBT(n,"recipe");progress.writeToNBT(n,"progress");n.putInt("charges",charges);n.putInt("color",color);}
    @Override public void load(CompoundTag n){super.load(n);recipe=new AspectList();recipe.readFromNBT(n,"recipe");progress=new AspectList();progress.readFromNBT(n,"progress");charges=Math.max(0,Math.min(8,n.getInt("charges")));color=n.getInt("color");}
}
