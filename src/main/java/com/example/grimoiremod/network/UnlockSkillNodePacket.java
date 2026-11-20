package com.example.grimoiremod.network;

import com.example.grimoiremod.data.SkillTreeData;
import com.example.grimoiremod.data.SkillTreeDef;
import com.example.grimoiremod.item.GrimoireItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record UnlockSkillNodePacket(int itemSlot, String nodeId) implements CustomPacketPayload {

    public static final Type<UnlockSkillNodePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("grimoiremod", "unlock_skill_node"));

    public static final StreamCodec<FriendlyByteBuf, UnlockSkillNodePacket> CODEC = StreamCodec.of(
        (buf, pkt) -> {
            buf.writeVarInt(pkt.itemSlot());
            buf.writeUtf(pkt.nodeId());
        },
        buf -> new UnlockSkillNodePacket(buf.readVarInt(), buf.readUtf(64))
    );

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static UnlockSkillNodePacket of(int itemSlot, String nodeId) {
        return new UnlockSkillNodePacket(itemSlot, nodeId);
    }

    public static void handle(UnlockSkillNodePacket pkt, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player)) return;
        ctx.enqueueWork(() -> {
            if (pkt.itemSlot() < 0 || pkt.itemSlot() >= player.getInventory().getContainerSize()) return;
            ItemStack stack = player.getInventory().getItem(pkt.itemSlot());
            if (!(stack.getItem() instanceof GrimoireItem)) return;
            SkillTreeDef.Node node = SkillTreeDef.byId(pkt.nodeId());
            if (node == null) return;

            if (SkillTreeData.isUnlocked(stack, node.id())) return;
            if (SkillTreeData.getPoints(stack) < node.cost()) {
                player.sendSystemMessage(
                        Component.translatable("gui.grimoiremod.skill_tree.no_points")
                                .withStyle(ChatFormatting.RED));
                return;
            }
            boolean ok = SkillTreeData.tryUnlock(stack, node.id(), node.cost(), node.prereqs());
            if (ok) {
                player.sendSystemMessage(
                        Component.translatable("gui.grimoiremod.skill_tree.unlocked")
                                .withStyle(ChatFormatting.GREEN));
            }
            player.containerMenu.broadcastChanges();
        });
    }
}
