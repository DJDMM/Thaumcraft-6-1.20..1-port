package thaumcraft.artifice.late;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Regression for GL float clamping at the modern byte-packing boundary. */
@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public final class WaterStreamColorGameTests {
    private WaterStreamColorGameTests(){}
    @GameTest(template="empty") public static void urnBrightnessClampsBlueInsteadOfWrappingToGreen(GameTestHelper h){
        int bright=WaterStreamColor.packRgb(0x286ff6,1.1F);
        h.assertTrue(bright==0x2c7aff,"Original bright Urn colour must clamp blue255, not wrap270 to14: "+Integer.toHexString(bright));
        h.assertTrue(WaterStreamColor.packRgb(0xffffff,1.1F)==0xffffff,"Clamping must preserve white at its upper boundary");
        h.succeed();
    }
    @GameTest(template="empty") public static void urnInRangeBrightnessKeepsOriginalByteValues(GameTestHelper h){
        h.assertTrue(WaterStreamColor.packRgb(0x286ff6,1F)==0x286ff6,"Unit brightness changed the original Urn colour");
        h.assertTrue(WaterStreamColor.packRgb(0x286ff6,.9F)==0x2463dd,"Original in-range pulse colour changed");
        h.assertTrue(WaterStreamColor.packRgb(0x286ff6,.5F)==0x14377b,"Original in-range fractional byte conversion changed");
        h.succeed();
    }
}
