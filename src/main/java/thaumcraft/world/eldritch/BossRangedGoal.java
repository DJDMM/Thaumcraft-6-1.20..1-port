package thaumcraft.world.eldritch;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
/** Original AILongRangeAttack applies its minimum only when acquiring the ranged goal. */
final class BossRangedGoal extends RangedAttackGoal {
    private final Mob mob;private final double minimumSquared;
    <T extends Mob & RangedAttackMob> BossRangedGoal(T mob,double minimum,int minTicks,int maxTicks,float range){super(mob,1,minTicks,maxTicks,range);this.mob=mob;this.minimumSquared=minimum*minimum;}
    @Override public boolean canUse(){return mob.getTarget()!=null&&mob.distanceToSqr(mob.getTarget())>=minimumSquared&&super.canUse();}
}
