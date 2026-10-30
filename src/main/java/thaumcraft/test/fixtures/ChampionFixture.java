package thaumcraft.test.fixtures;

import com.electronwill.nightconfig.core.CommentedConfig;
import java.nio.file.*;
import java.util.Arrays;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.*;
import thaumcraft.world.taint.champion.Champions;

/** Explicit initial QA fixture: normal actors stay normal; explicit champion tests remain active.
 * Detached config preserves the process-owned file byte for byte. Never used by ordinary gameplay. */
public final class ChampionFixture implements AutoCloseable {
    private final ForgeConfigSpec spec;
    private final CommentedConfig previous;
    private final Path file;
    private final byte[] bytes;
    private final boolean enabled;
    private boolean closed;
    public ChampionFixture(Path ownedRun){
        try{
            var field=Champions.class.getDeclaredField("CONFIG");field.setAccessible(true);spec=(ForgeConfigSpec)field.get(null);
            var actual=ConfigTracker.INSTANCE.configSets().get(ModConfig.Type.COMMON).stream().filter(c->c.getSpec()==spec).findFirst().orElseThrow();
            file=actual.getFullPath().toRealPath();var root=ownedRun.toRealPath();
            require(file.equals(root.resolve("config/thaumcraft-champions.toml")),"Champion fixture is outside the owned QA run");
            bytes=Files.readAllBytes(file);previous=actual.getConfigData();enabled=Champions.ALLOW_CHAMPIONS.get();
            require(spec.isCorrect(previous),"Refuse correction of the original champion config");
            var memory=CommentedConfig.copy(previous);require(!(memory instanceof com.electronwill.nightconfig.core.file.FileConfig),"QA config is file-backed");
            spec.setConfig(memory);Champions.ALLOW_CHAMPIONS.set(false);
            require(!Champions.ALLOW_CHAMPIONS.get()&&Arrays.equals(bytes,Files.readAllBytes(file)),"QA config changed disk bytes");
            com.mojang.logging.LogUtils.getLogger().info("THAUMCRAFT_INITIAL_CHAMPION_FIXTURE: allowChampionMobs=false/detachedMemory/configBytesUnchanged/normalActorFixtures/explicitChampionTestsRetained/noNaturalChampionFrequencyClaim");
        }catch(Exception e){throw new IllegalStateException("Could not prepare owned champion QA fixture",e);}
    }
    @Override public void close(){
        if(closed)return;closed=true;
        try{
            require(spec.isCorrect(previous),"Original champion config became invalid");spec.setConfig(previous);
            require(Champions.ALLOW_CHAMPIONS.get()==enabled&&Arrays.equals(bytes,Files.readAllBytes(file)),"Champion config was not restored without disk writes");
        }catch(Exception e){throw new IllegalStateException("Could not restore champion QA fixture",e);}
    }
    private static void require(boolean ok,String detail){if(!ok)throw new IllegalStateException(detail);}
}
