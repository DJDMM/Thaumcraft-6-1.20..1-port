package thaumcraft.artifice.late;

/** Clamp released GL float colours before modern unsigned-byte vertex packing. */
public final class WaterStreamColor {
    private WaterStreamColor(){}
    public static int packRgb(int rgb,float brightness){
        return channel(rgb>>16&255,brightness)<<16|channel(rgb>>8&255,brightness)<<8|channel(rgb&255,brightness);
    }
    private static int channel(int source,float brightness){
        float value=source/255F*brightness;
        return (int)(Math.max(0F,Math.min(1F,value))*255F);
    }
}
