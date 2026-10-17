package thaumcraft.auromancy;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import java.util.Optional;
import java.util.function.Supplier;

public final class FocusSelectionNetwork {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "focus_selection"), () -> "2", "2"::equals, "2"::equals);
    private FocusSelectionNetwork() {}
    public static void request(InteractionHand hand, int slot, ItemStack caster, ItemStack focus) {
        if(slot < -1||slot>=36)return;
        request(hand,slot==-1?null:FocusSource.inventory(slot),caster,ItemStack.EMPTY,focus);
    }
    public static void request(InteractionHand hand,FocusSource source,ItemStack caster,ItemStack pouch,ItemStack focus){CHANNEL.sendToServer(new Change(hand,source,caster,pouch,focus));}
    @Mod.EventBusSubscriber(modid="thaumcraft",bus=Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void setup(FMLCommonSetupEvent event) {
            CHANNEL.registerMessage(0, Change.class, Change::encode, Change::decode, Change::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        }
    }
    public record Change(InteractionHand hand,FocusSource source,ItemStack caster,ItemStack pouch,ItemStack focus) {
        public Change { caster=caster.copy();pouch=pouch.copy();focus=focus.copy(); }
        static void encode(Change p,FriendlyByteBuf buf){buf.writeEnum(p.hand);buf.writeBoolean(p.source!=null);if(p.source!=null){buf.writeEnum(p.source.kind());buf.writeVarInt(p.source.outer());buf.writeVarInt(p.source.inner());}buf.writeItem(p.caster);buf.writeItem(p.pouch);buf.writeItem(p.focus);}
        static Change decode(FriendlyByteBuf buf){var hand=buf.readEnum(InteractionHand.class);FocusSource source=buf.readBoolean()?new FocusSource(buf.readEnum(FocusSource.Kind.class),buf.readVarInt(),buf.readVarInt()):null;return new Change(hand,source,buf.readItem(),buf.readItem(),buf.readItem());}
        static void handle(Change p, Supplier<NetworkEvent.Context> context) {
            context.get().enqueueWork(() -> FocusSelection.change(context.get().getSender(),p.hand,p.source,p.caster,p.pouch,p.focus));
            context.get().setPacketHandled(true);
        }
    }
}
