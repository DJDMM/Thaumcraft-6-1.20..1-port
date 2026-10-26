package thaumcraft.fx.emitters;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import thaumcraft.auromancy.focus.FocusNodeRegistry;
import thaumcraft.fx.FxDispatcher;

/** Client ticks reproduce EntityFocusCloud/Mine/SpellBat emitters, independent of render visibility. */
public final class FocusMediaFx {
    private FocusMediaFx(){}
    public static String choose(Entity entity,String keys){
        if(keys.isBlank())return "";
        String[] parts=keys.split("%");return parts[entity.level().random.nextInt(parts.length)];
    }
    public static int color(String key){var d=FocusNodeRegistry.get(key);return d==null?0xFFFFFF:d.color();}
    public static void cloud(Entity entity,String keys,float radius){
        if(!entity.level().isClientSide||keys.isBlank())return;
        var level=entity.level();var r=level.random;
        for(int i=0;i<radius;i++){
            String key=choose(entity,keys);
            Vec3 p=FocusFxPrograms.jitter(level,entity.position(),radius/2D*.85);
            Vec3 v=FocusFxPrograms.jitter(level,Vec3.ZERO,.01);
            FxDispatcher.emit(level,"focusCloud",p,v,color(key),1,0);
            FocusFxPrograms.render(level,FocusFxPrograms.jitter(level,entity.position(),radius/2D),
                    FocusFxPrograms.jitter(level,Vec3.ZERO,.009999999776482582),key);
        }
    }
    public static void mine(Entity entity,String keys,boolean armed,int counter){
        if(!entity.level().isClientSide||!armed||counter>0||entity.tickCount%5!=0||keys.isBlank())return;
        FocusFxPrograms.render(entity.level(),FocusFxPrograms.jitter(entity.level(),entity.position(),.1),
                FocusFxPrograms.jitter(entity.level(),Vec3.ZERO,.009999999776482582),choose(entity,keys));
    }
    public static void bat(Entity entity,String keys){
        if(!entity.level().isClientSide||keys.isBlank())return;
        FocusFxPrograms.render(entity.level(),FocusFxPrograms.jitter(entity.level(),entity.position().add(0,entity.getBbHeight()/2,0),.125),
                Vec3.ZERO,choose(entity,keys));
    }
}
