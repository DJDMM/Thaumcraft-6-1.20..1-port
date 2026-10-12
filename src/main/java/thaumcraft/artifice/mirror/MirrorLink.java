package thaumcraft.artifice.mirror;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** A complete, dimension-qualified physical endpoint; unrelated NBT is never a link. */
public record MirrorLink(ResourceKey<Level> dimension, BlockPos pos) {
    public MirrorLink { pos = pos.immutable(); }
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag(); tag.putString("Dimension", dimension.location().toString()); tag.putLong("Pos", pos.asLong()); return tag;
    }
    public void writeTo(ItemStack stack) { stack.getOrCreateTag().put("MirrorLink", save()); }
    @Nullable public static MirrorLink read(ItemStack stack) { return stack.hasTag() ? read(stack.getTag()) : null; }
    public static boolean isBound(ItemStack stack) { return read(stack) != null; }
    @Nullable public static MirrorLink read(CompoundTag outer) {
        if (outer.contains("MirrorLink", Tag.TAG_COMPOUND)) {
            CompoundTag tag = outer.getCompound("MirrorLink");
            if (!tag.contains("Dimension", Tag.TAG_STRING) || !tag.contains("Pos", Tag.TAG_LONG)) return null;
            ResourceLocation id = ResourceLocation.tryParse(tag.getString("Dimension"));
            return id == null ? null : new MirrorLink(ResourceKey.create(Registries.DIMENSION, id), BlockPos.of(tag.getLong("Pos")));
        }
        if (!outer.contains("linkX", Tag.TAG_INT) || !outer.contains("linkY", Tag.TAG_INT) || !outer.contains("linkZ", Tag.TAG_INT) || !outer.contains("linkDim", Tag.TAG_INT)) return null;
        ResourceKey<Level> dimension = switch (outer.getInt("linkDim")) { case 0 -> Level.OVERWORLD; case -1 -> Level.NETHER; case 1 -> Level.END; default -> null; };
        return dimension == null ? null : new MirrorLink(dimension, new BlockPos(outer.getInt("linkX"), outer.getInt("linkY"), outer.getInt("linkZ")));
    }
    public static void clear(ItemStack stack) {
        if (!stack.hasTag()) return;
        CompoundTag tag = stack.getTag(); tag.remove("MirrorLink");
        for (String key : new String[]{"linkX","linkY","linkZ","linkDim"}) tag.remove(key);
        if (tag.isEmpty()) stack.setTag(null);
    }
}
