package thaumcraft.test.fixtures;

import java.nio.file.Path;
import net.minecraftforge.event.server.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="thaumcraft")
public final class NativeTestFixtures {
    private static ChampionFixture champions;
    private NativeTestFixtures(){}
    @SubscribeEvent public static void before(ServerAboutToStartEvent event){
        if(!Boolean.getBoolean("forge.gameTestServer"))return;
        var run=Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        if(!run.getFileName().toString().equals("gametest-server"))throw new IllegalStateException("Native tests must use their owned run directory");
        champions=new ChampionFixture(run);
    }
    @SubscribeEvent public static void after(ServerStoppedEvent event){if(champions!=null){champions.close();champions=null;}}
}
