package thaumcraft.research;

import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import thaumcraft.world.rift.WispEntity;
import thaumcraft.world.taint.TaintSeedEntity;

/** Released ConfigResearch ScanBlock/ScanEntity families backed by native runtime mechanics. */
public final class RemainingProgressionScans {
    private static final Set<String> CANONICAL = Set.of("PLANTS", "!Wisp", "!TaintSeed", "!ThaumSlime",
            "!CrimsonCultist", "!EldritchGuardian", "!TaintCrawler", "!Taintacle", "!TaintSwarm", "!Pech", "!BrainyZombie", "!EldritchCrab");
    private static final Set<String> PLANTS = Set.of("log_greatwood", "log_silverwood", "sapling_greatwood",
            "sapling_silverwood", "cinderpearl", "shimmerleaf", "vishroom");
    private static final List<TagKey<net.minecraft.world.item.Item>> VOID = List.of("ingots/void", "storage_blocks/void", "plates/void").stream()
            .map(path -> TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("forge", path))).toList();
    private RemainingProgressionScans() {}

    public static boolean isCanonicalScan(String key) { return CANONICAL.contains(key); }

    /** Read-only matching is also used by the HUD; only the actual scan commit can acquire a fact. */
    public static List<String> scanFacts(@Nullable Object specimen) {
        if (specimen instanceof WispEntity) return List.of("!Wisp");
        if (specimen instanceof thaumcraft.world.eldritch.EldritchCrabEntity) return List.of("!EldritchCrab");
        if (specimen instanceof thaumcraft.world.mobs.PechEntity) return List.of("!Pech");
        if (specimen instanceof thaumcraft.world.mobs.BrainyZombieEntity) return List.of("!BrainyZombie");
        // Original ScanEntity(...,true) includes the native greater/prime seed subclass.
        if (specimen instanceof TaintSeedEntity) return List.of("!TaintSeed");
        if (specimen instanceof thaumcraft.world.taint.ThaumicSlimeEntity) return List.of("!ThaumSlime");
        if (specimen instanceof thaumcraft.world.crimson.CultistEntity) return List.of("!CrimsonCultist");
        if (specimen instanceof thaumcraft.world.warp.EldritchGuardianEntity) return List.of("!EldritchGuardian");
        if (specimen instanceof thaumcraft.world.taint.TaintCrawlerEntity) return List.of("!TaintCrawler");
        if (specimen instanceof thaumcraft.world.taint.TaintacleEntity || specimen instanceof thaumcraft.world.taint.TaintacleSmallEntity) return List.of("!Taintacle");
        if (specimen instanceof thaumcraft.world.taint.TaintSwarmEntity) return List.of("!TaintSwarm");
        if (specimen instanceof ItemEntity item) specimen = item.getItem();
        if (specimen instanceof ItemStack stack && stack.is(net.minecraft.world.item.Items.TOTEM_OF_UNDYING)) return List.of("!TOTEMUNDYING");
        ItemStack material = specimen instanceof BlockState state ? new ItemStack(state.getBlock())
                : specimen instanceof ItemStack stack ? stack : ItemStack.EMPTY;
        if (!material.isEmpty() && VOID.stream().anyMatch(material::is)) return List.of("f_MATVOID");
        ResourceLocation id;
        if (specimen instanceof BlockState state) id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        else if (specimen instanceof ItemStack stack && !stack.isEmpty()) id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        else return List.of();
        if(id != null && id.getNamespace().equals("thaumcraft")) {
            switch(id.getPath()) {
                case "stone_ancient", "stone_ancient_tile": return List.of("!ORBLOCK1");
                case "stone_eldritch_tile": return List.of("!ORBLOCK2");
                case "stone_ancient_glyphed": return List.of("!ORBLOCK3");
            }
        }
        // ScanBlock registers matching ScanItems in its constructor. Leaves, planks,
        // vanilla plants and unrelated mod blocks were never members of this family.
        if (id == null || !id.getNamespace().equals("thaumcraft") || !PLANTS.contains(id.getPath())) return List.of();
        return List.of("PLANTS", switch (id.getPath()) {
            case "cinderpearl" -> "!PLANTCINDERPEARL";
            case "shimmerleaf" -> "!PLANTSHIMMERLEAF";
            case "vishroom" -> "!PLANTVISHROOM";
            default -> "!PLANTWOOD";
        });
    }
}
