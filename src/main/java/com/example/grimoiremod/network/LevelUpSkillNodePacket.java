package com.example.grimoiremod.network;

import com.example.grimoiremod.data.SkillTreeData;
import com.example.grimoiremod.data.SkillTreeDef;
import com.example.grimoiremod.item.GrimoireItem;
import com.example.grimoiremod.util.GrimoireLocator;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record LevelUpSkillNodePacket(int itemSlot, String nodeId) implements CustomPacketPayload {

    public static final Type<LevelUpSkillNodePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("grimoiremod", "level_up_skill_node"));

    public static final StreamCodec<FriendlyByteBuf, LevelUpSkillNodePacket> CODEC = StreamCodec.of(
        (buf, pkt) -> {
            buf.writeVarInt(pkt.itemSlot());
            buf.writeUtf(pkt.nodeId());
        },
        buf -> new LevelUpSkillNodePacket(buf.readVarInt(), buf.readUtf(64))
    );

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(LevelUpSkillNodePacket pkt, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player)) return;
        ctx.enqueueWork(() -> {
            int actualSlot = pkt.itemSlot();
            if (actualSlot < 0) actualSlot = GrimoireLocator.findFirst(player);
            if (actualSlot < 0 || actualSlot >= player.getInventory().getContainerSize()) return;

            ItemStack stack = player.getInventory().getItem(actualSlot);
            if (!(stack.getItem() instanceof GrimoireItem)) return;

            SkillTreeDef.Node node = SkillTreeDef.byId(pkt.nodeId());
            if (node == null || !SkillTreeData.isUnlocked(stack, node.id())) return;

            int level = SkillTreeData.getNodeLevel(stack, node.id());
            if (level >= node.maxLevel()) return;
            if (SkillTreeData.getPointsFrac(stack) < 1.0f) return;

            SkillTreeData.addPoints(stack, -1.0f);
            SkillTreeData.addNodeLevel(stack, node.id(), 1);
            player.containerMenu.broadcastChanges();
        });
    }
}
