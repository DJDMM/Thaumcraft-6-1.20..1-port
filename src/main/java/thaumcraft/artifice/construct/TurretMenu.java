package thaumcraft.artifice.construct;

import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Original live mainhand slot plus player inventory; ordinary menu packets. */
public final class TurretMenu extends AbstractContainerMenu {
    private final Inventory inventory;
    private final TurretEntity turret;
    private final boolean advanced;
    public TurretMenu(int id, Inventory inventory, FriendlyByteBuf data) { this(id, inventory, resolve(inventory, data.readVarInt()), data.readBoolean()); }
    public TurretMenu(int id, Inventory inventory, TurretEntity turret) { this(id, inventory, turret, turret.isAdvanced()); }
    private static TurretEntity resolve(Inventory inventory, int entityId) { return inventory.player.level().getEntity(entityId) instanceof TurretEntity turret ? turret : null; }
    private TurretMenu(int id, Inventory inventory, @Nullable TurretEntity turret, boolean advanced) {
        super(TurretModule.MENU.get(), id); this.inventory = inventory; this.turret = turret; this.advanced = advanced;
        Container ammo = turret == null ? new SimpleContainer(1) : new Container() {
            @Override public int getContainerSize() { return 1; }
            @Override public boolean isEmpty() { return turret.ammo().isEmpty(); }
            @Override public ItemStack getItem(int slot) { return slot == 0 ? turret.ammo() : ItemStack.EMPTY; }
            @Override public ItemStack removeItem(int slot, int count) { if (slot != 0 || count <= 0) return ItemStack.EMPTY; ItemStack out = turret.ammo().split(count); if (turret.ammo().isEmpty()) turret.setAmmo(ItemStack.EMPTY); return out; }
            @Override public ItemStack removeItemNoUpdate(int slot) { if (slot != 0) return ItemStack.EMPTY; ItemStack out = turret.ammo(); turret.setAmmo(ItemStack.EMPTY); return out; }
            @Override public void setItem(int slot, ItemStack stack) { if (slot == 0) turret.setAmmo(stack); }
            @Override public void setChanged() {}
            @Override public boolean stillValid(Player player) { return turret.canManage(player); }
            @Override public void clearContent() { turret.setAmmo(ItemStack.EMPTY); }
        };
        addSlot(new Slot(ammo, 0, advanced ? 42 : 80, 29) { @Override public boolean mayPlace(ItemStack stack) { return TurretEntity.isAmmo(stack); } });
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, 9 + row * 9 + column, 8 + column * 18, 84 + row * 18));
        for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, 8 + column * 18, 142));
    }
    @Nullable public TurretEntity turret() { return turret; }
    public boolean advanced() { return advanced; }
    public boolean isAdvanced() { return advanced; }
    public byte targetFlags() { return turret instanceof AdvancedTurretEntity target ? target.getTargetFlags() : 0; }
    private boolean context(Player player) { return player == inventory.player && stillValid(player) && (!(player instanceof ServerPlayer server) || server.containerMenu == this); }
    @Override public boolean stillValid(Player player) { return turret != null && turret.isAdvanced() == advanced && turret.canManage(player); }
    @Override public boolean clickMenuButton(Player player, int action) { return player instanceof ServerPlayer && context(player) && turret instanceof AdvancedTurretEntity target && target.toggleTarget(action); }
    @Override public void clicked(int slot, int button, ClickType type, Player player) { if (context(player)) super.clicked(slot, button, type, player); }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (!context(player) || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index); if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack source = slot.getItem(), original = source.copy();
        if (index == 0) { if (!moveItemStackTo(source, 1, slots.size(), true)) return ItemStack.EMPTY; }
        else if (!moveItemStackTo(source, 0, 1, false)) return ItemStack.EMPTY;
        if (source.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        if (source.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, source); return original;
    }
}
