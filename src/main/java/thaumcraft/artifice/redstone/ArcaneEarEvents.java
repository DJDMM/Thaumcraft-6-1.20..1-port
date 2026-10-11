package thaumcraft.artifice.redstone;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.level.NoteBlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** One shared native note batch for every ear in the level, cleared after its block entities tick. */
@Mod.EventBusSubscriber(modid = "thaumcraft")
public final class ArcaneEarEvents {
    record PlayedNote(BlockPos pos, int instrument, int note) {}
    private static final WeakHashMap<ServerLevel, List<PlayedNote>> NOTES = new WeakHashMap<>();
    private ArcaneEarEvents() {}
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void notePlayed(NoteBlockEvent.Play event) {
        if (event.isCanceled() || !(event.getLevel() instanceof ServerLevel level) || !ArtificeRedstoneModule.loaded(level, event.getPos())) return;
        NOTES.computeIfAbsent(level, key -> new ArrayList<>()).add(new PlayedNote(event.getPos().immutable(), event.getInstrument().ordinal(), event.getVanillaNoteId()));
    }
    static List<PlayedNote> notes(ServerLevel level) { return NOTES.getOrDefault(level, List.of()); }
    @SubscribeEvent public static void levelTick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.level instanceof ServerLevel level) NOTES.remove(level);
    }
    @SubscribeEvent public static void unload(LevelEvent.Unload event) { if (event.getLevel() instanceof ServerLevel level) NOTES.remove(level); }
}
