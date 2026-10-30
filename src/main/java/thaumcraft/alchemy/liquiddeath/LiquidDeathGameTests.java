package thaumcraft.alchemy.liquiddeath;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.gametest.*;
import thaumcraft.alchemy.AspectCrystalItem;
@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public final class LiquidDeathGameTests {
 private LiquidDeathGameTests(){}
 @GameTest(template="empty") public static void sourceUsesOriginalDissolveDamageAndHalfHorizontalSpeed(GameTestHelper h){
  var cow=EntityType.COW.create(h.getLevel());cow.setHealth(10);cow.setDeltaMovement(1,.4,1);
  try{LiquidDeathModule.block().entityInside(LiquidDeathModule.block().defaultBlockState(),h.getLevel(),h.absolutePos(BlockPos.ZERO),cow);
   h.assertTrue(cow.getHealth()==5&&cow.getDeltaMovement().x==.5&&cow.getDeltaMovement().y==.4&&cow.getDeltaMovement().z==.5&&!cow.hasEffect(net.minecraft.world.effect.MobEffects.POISON),"Liquid Death substituted poison or changed source collision damage/motion");
   h.assertTrue(LiquidDeathModule.damage(cow).is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR)&&!LiquidDeathModule.damage(cow).is(net.minecraft.tags.DamageTypeTags.WITCH_RESISTANT_TO),"Dissolve source lost armor bypass or invented magic damage");
  }finally{cow.discard();}h.succeed();
 }
 @GameTest(template="empty") public static void fourQuantaPreserveOriginalHeightsAndThinFlowDamage(GameTestHelper h){
  var block=LiquidDeathModule.block();h.assertTrue(block.defaultBlockState().getFluidState().getOwnHeight()==.8F,"Classic four-quanta full height became vanilla eight ninths");
  var state=block.defaultBlockState().setValue(LiquidDeathBlock.LEVEL,3);var cow=EntityType.COW.create(h.getLevel());cow.setHealth(10);cow.setDeltaMovement(1,0,1);
  try{block.entityInside(state,h.getLevel(),h.absolutePos(BlockPos.ZERO),cow);h.assertTrue(cow.getHealth()==8&&cow.getDeltaMovement().x==.875&&state.getFluidState().getOwnHeight()==.2F,"One quantum damage/fraction/height changed");}finally{cow.discard();}h.succeed();
 }
 @GameTest(template="empty") public static void verticalClassicFlowKeepsMetaOneAndCannotMintSource(GameTestHelper h){
  var original=h.absolutePos(new BlockPos(2,0,2));var p=new BlockPos(original.getX(),250,original.getZ());var saved=new LinkedHashMap<BlockPos,BlockState>();
  try{for(int y=-2;y<=1;y++){var n=p.offset(0,y,0);saved.put(n,h.getLevel().getBlockState(n));h.getLevel().setBlockAndUpdate(n,y==-2?Blocks.BEDROCK.defaultBlockState():Blocks.AIR.defaultBlockState());}
   h.getLevel().setBlockAndUpdate(p,LiquidDeathModule.block().defaultBlockState());LiquidDeathModule.block().tick(h.getLevel().getBlockState(p),h.getLevel(),p,net.minecraft.util.RandomSource.create(4));
   var down=h.getLevel().getBlockState(p.below());h.assertTrue(down.is(LiquidDeathModule.block())&&down.getValue(LiquidDeathBlock.LEVEL)==1&&!down.getFluidState().isSource()&&down.getFluidState().getOwnHeight()==.6F,"Classic downward fluid became source/falling-eight-quanta");
  }finally{boolean restoring=h.getLevel().restoringBlockSnapshots;h.getLevel().restoringBlockSnapshots=true;try{var rows=new ArrayList<>(saved.entrySet());Collections.reverse(rows);for(var e:rows)h.getLevel().setBlock(e.getKey(),e.getValue(),3);}finally{h.getLevel().restoringBlockSnapshots=restoring;}}h.succeed();
 }
 @GameTest(template="empty") public static void dissolveCrystalsUseVictimAspectsWithoutPlayerCreditRequirement(GameTestHelper h){
  var victim=EntityType.COW.create(h.getLevel());var aspects=LiquidDeathModule.entityAspects(victim);var drops=new ArrayList<ItemEntity>();
  try{MinecraftForge.EVENT_BUS.post(new LivingDropsEvent(victim,LiquidDeathModule.damage(victim),drops,0,false));h.assertTrue(!drops.isEmpty()&&drops.size()<=1+aspects.visSize()/10,"Dissolve lost autonomous original crystal yield");
   for(var drop:drops)h.assertTrue(drop.getItem().getCount()==1&&aspects.getAmount(AspectCrystalItem.crystalAspect(drop.getItem()))>0&&drop.getY()==victim.getEyeY(),"Dissolve created untyped/nonvictim crystals or incorrect placement");
   h.assertTrue(aspects.visSize()==LiquidDeathModule.entityAspects(victim).visSize(),"Dissolve mutated global entity aspect definitions");
  }finally{victim.discard();for(var drop:drops)drop.discard();}h.succeed();
 }
}
