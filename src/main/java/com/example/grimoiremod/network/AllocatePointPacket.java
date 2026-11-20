package com.example.grimoiremod.network;

import com.example.grimoiremod.data.GrimoireData;
import com.example.grimoiremod.glyphs.Stat;
import com.example.grimoiremod.item.GrimoireItem;
import com.example.grimoiremod.menu.GlyphWorkbenchMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record AllocatePointPacket(int itemSlot, int pageIdx, int slotIdx, Stat stat, int delta, boolean isBank)
        implements CustomPacketPayload {

    public static final Type<AllocatePointPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("grimoiremod", "allocate_point"));

    public static final StreamCodec<FriendlyByteBuf, AllocatePointPacket> CODEC = StreamCodec.of(
        (buf, pkt) -> {
            buf.writeVarInt(pkt.itemSlot());
            buf.writeVarInt(pkt.pageIdx());
            buf.writeVarInt(pkt.slotIdx());
            buf.writeUtf(pkt.stat().name());
            buf.writeVarInt(pkt.delta());
            buf.writeBoolean(pkt.isBank());
        },
        buf -> {
            int itemSlot = buf.readVarInt();
            int pageIdx = buf.readVarInt();
            int slotIdx = buf.readVarInt();
            String statName = buf.readUtf(64);
            int delta = buf.readVarInt();
            boolean isBank = buf.readBoolean();

            Stat stat;
            int safeDelta = Math.max(-20, Math.min(20, delta));
            try { stat = Stat.valueOf(statName); }
            catch (IllegalArgumentException e) { stat = Stat.DAMAGE; safeDelta = 0; }
            return new AllocatePointPacket(itemSlot, pageIdx, slotIdx, stat, safeDelta, isBank);
        }
    );

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(AllocatePointPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ItemStack stack;
            if (ctx.player().containerMenu instanceof GlyphWorkbenchMenu menu) {
                stack = menu.getGrimoire();
            } else {
                int slot = pkt.itemSlot();
                if (slot < 0 || slot >= ctx.player().getInventory().getContainerSize()) return;
                stack = ctx.player().getInventory().getItem(slot);
            }

            if (stack.getItem() instanceof GrimoireItem) {
                if (pkt.isBank()) {
                    GrimoireData.allocatePointToBank(stack, pkt.slotIdx(), pkt.stat(), pkt.delta());
                } else {
                    GrimoireData.allocatePoint(stack, pkt.pageIdx(), pkt.slotIdx(), pkt.stat(), pkt.delta());
                }
                ctx.player().containerMenu.broadcastChanges();
            }
        });
    }
}
