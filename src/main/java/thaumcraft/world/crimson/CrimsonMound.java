package thaumcraft.world.crimson;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraftforge.registries.ForgeRegistries;

/** Original 19x16x19 mound literal plan, in its original write order; no synthetic replacement dungeon. */
public final class CrimsonMound {
    private record Cell(int x, int y, int z, BlockState state) {}
    private static final List<Cell> CELLS = load();
    private CrimsonMound() {}
    public static int placementCount() { return CELLS.size(); }
    static List<Cell> layout() { return CELLS; }
    private static List<Cell> load() {
        try (var in = CrimsonMound.class.getResourceAsStream("/data/thaumcraft/crimson/mound_blocks.json")) {
            if (in == null) throw new IllegalStateException("Missing original mound layout");
            var json = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            var result = new ArrayList<Cell>();
            for (var value : json.getAsJsonArray("placements")) {
                var row = value.getAsJsonArray();
                result.add(new Cell(row.get(0).getAsInt(), row.get(1).getAsInt(), row.get(2).getAsInt(),
                        state(row.get(3).getAsString(), row.get(4).getAsInt())));
            }
            if (result.size() != 2441) throw new IllegalStateException("Original mound layout census changed");
            return List.copyOf(result);
        } catch (Exception e) { throw new IllegalStateException("Invalid BETA26 mound data", e); }
    }
    private static BlockState state(String block, int metadata) {
        return switch (block) {
            case "AIR" -> Blocks.AIR.defaultBlockState(); case "DIRT" -> Blocks.DIRT.defaultBlockState();
            case "GRASS" -> Blocks.GRASS_BLOCK.defaultBlockState(); case "COBBLESTONE" -> Blocks.COBBLESTONE.defaultBlockState();
            case "MOSSY_COBBLESTONE" -> Blocks.MOSSY_COBBLESTONE.defaultBlockState();
            case "IRON_BARS" -> Blocks.IRON_BARS.defaultBlockState(); case "TALLGRASS" -> Blocks.GRASS.defaultBlockState();
            case "STONEBRICK" -> Blocks.CHISELED_STONE_BRICKS.defaultBlockState();
            case "LADDER" -> Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.EAST);
            case "STONE_STAIRS" -> Blocks.COBBLESTONE_STAIRS.defaultBlockState()
                    .setValue(StairBlock.FACING, switch (metadata < 0 ? 3 : metadata & 3) { case 0 -> Direction.EAST; case 1 -> Direction.WEST; case 2 -> Direction.SOUTH; default -> Direction.NORTH; })
                    .setValue(StairBlock.HALF, metadata >= 0 && (metadata & 4) != 0 ? Half.TOP : Half.BOTTOM);
            default -> throw new IllegalArgumentException("Unsupported original mound block: " + block);
        };
    }
    public static boolean validSurface(WorldGenLevel level, BlockPos pos) {
        if (!level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) return false;
        int air = 0;
        while (air < 3 && !level.isEmptyBlock(pos.above(air))) air++;
        if (air > 2) return false;
        BlockPos ground = pos.above(air - 1);
        Block block = level.getBlockState(ground).getBlock();
        Block below = level.getBlockState(ground.below()).getBlock();
        if (!level.isEmptyBlock(ground.above())) return false;
        return validGround(block) || (block == Blocks.SNOW || block == Blocks.GRASS) && validGround(below);
    }
    private static boolean validGround(Block block) { return block == Blocks.STONE || block == Blocks.GRASS_BLOCK || block == Blocks.DIRT; }
    public static boolean generate(WorldGenLevel level, RandomSource random, BlockPos origin) {
        if (origin.getY() - 1 < level.getMinBuildHeight() || origin.getY() + 15 >= level.getMaxBuildHeight()) return false;
        // New world generation owns only already supplied chunks. Existing block entities veto the full plan before writes.
        for (int dx = 0; dx <= 18; dx++) for (int dz = 0; dz <= 18; dz++) {
            BlockPos pos = origin.offset(dx, 0, dz);
            if (!level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) return false;
            for (int dy = -1; dy <= 15; dy++) if (level.getBlockEntity(pos.above(dy)) != null) return false;
        }
        for (int[] corner : new int[][]{{9,9}, {0,0}, {18,0}, {18,18}, {0,18}})
            if (!validSurface(level, origin.offset(corner[0], 9, corner[1]))) return false;
        for (Cell cell : CELLS) level.setBlock(origin.offset(cell.x, cell.y, cell.z), cell.state, 2);
        treasure(level, random, origin.offset(9,1,7));
        treasure(level, random, origin.offset(9,1,11));
        BlockPos chest = origin.offset(10,1,9);
        boolean trapped = random.nextInt(3) == 0;
        level.setBlock(chest, (trapped ? Blocks.TRAPPED_CHEST : Blocks.CHEST).defaultBlockState().setValue(ChestBlock.FACING,Direction.WEST), 2);
        if (trapped) level.setBlock(origin.offset(10,-1,9), Blocks.TNT.defaultBlockState(), 2);
        if (level.getBlockEntity(chest) instanceof ChestBlockEntity tile) tile.setLootTable(BuiltInLootTables.SIMPLE_DUNGEON, random.nextLong());
        spawner(level, random, origin.offset(4,5,4), EntityType.SKELETON);
        spawner(level, random, origin.offset(4,5,14), EntityType.ZOMBIE);
        return true;
    }
    private static void treasure(WorldGenLevel level, RandomSource random, BlockPos pos) {
        float roll = random.nextFloat(); String rarity = roll < .1F ? "rare" : roll < .33F ? "uncommon" : "common";
        String kind = level.getRandom().nextFloat() < .3F ? "crate" : "urn";
        Block block = ForgeRegistries.BLOCKS.getValue(CrimsonModule.id("loot_" + kind + "_" + rarity));
        if (block == null || block == Blocks.AIR) throw new IllegalStateException("Original mound loot block missing");
        level.setBlock(pos, block.defaultBlockState(), 2);
    }
    private static void spawner(WorldGenLevel level, RandomSource random, BlockPos pos, EntityType<?> entity) {
        level.setBlock(pos, Blocks.SPAWNER.defaultBlockState(), 2);
        if (level.getBlockEntity(pos) instanceof SpawnerBlockEntity spawner) spawner.setEntityId(entity, random);
    }
}
