package com.example.grimoiremod.network;

import com.example.grimoiremod.data.GlyphPage;
import com.example.grimoiremod.data.GrimoireData;
import com.example.grimoiremod.glyphs.Stat;
import com.example.grimoiremod.item.GrimoireItem;
import com.example.grimoiremod.menu.GlyphWorkbenchMenu;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.Map;

public record GrimoireActionPacket(int itemSlot, String action, int[] ints, String[] strings)
        implements CustomPacketPayload {

    private static final int MAX_INTS = 32;
    private static final int MAX_STRINGS = 16;
    private static final int MAX_STR_LEN = 256;

    public static final Type<GrimoireActionPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("grimoiremod", "grimoire_action"));

    public static final StreamCodec<FriendlyByteBuf, GrimoireActionPacket> CODEC = StreamCodec.of(
        (buf, pkt) -> {
            buf.writeVarInt(pkt.itemSlot());
            buf.writeUtf(pkt.action());
            buf.writeVarInt(pkt.ints().length);
            for (int i : pkt.ints()) buf.writeVarInt(i);
            buf.writeVarInt(pkt.strings().length);
            for (String s : pkt.strings()) buf.writeUtf(s);
        },
        buf -> {
            int itemSlot = buf.readVarInt();
            String action = buf.readUtf(32);
            int intCount = buf.readVarInt();
            if (intCount < 0 || intCount > MAX_INTS) throw new DecoderException("Invalid int count");
            int[] ints = new int[intCount];
            for (int i = 0; i < intCount; i++) ints[i] = buf.readVarInt();
            int strCount = buf.readVarInt();
            if (strCount < 0 || strCount > MAX_STRINGS) throw new DecoderException("Invalid string count");
            String[] strings = new String[strCount];
            for (int i = 0; i < strCount; i++) strings[i] = buf.readUtf(MAX_STR_LEN);
            return new GrimoireActionPacket(itemSlot, action, ints, strings);
        }
    );

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static GrimoireActionPacket setPage(int slot, int page) {
        return new GrimoireActionPacket(slot, "setPage", new int[]{page}, new String[0]);
    }
    public static GrimoireActionPacket switchPage(int slot, boolean next) {
        return new GrimoireActionPacket(slot, "switchPage", new int[]{next ? 1 : 0}, new String[0]);
    }
    public static GrimoireActionPacket placeGlyph(int slot, int page, int row, String typeId, Map<Stat, Integer> allocs) {
        StringBuilder sb = new StringBuilder();
        for (var e : allocs.entrySet()) {
            if (sb.length() > 0) sb.append(",");
            sb.append(e.getKey().name()).append(":").append(e.getValue());
        }
        return new GrimoireActionPacket(slot, "placeGlyph", new int[]{page, row}, new String[]{typeId, sb.toString()});
    }
    public static GrimoireActionPacket moveGlyph(int slot, int page, int fromRow, int toRow) {
        return new GrimoireActionPacket(slot, "moveGlyph", new int[]{page, fromRow, toRow}, new String[0]);
    }
    public static GrimoireActionPacket removeGlyph(int slot, int page, int row) {
        return new GrimoireActionPacket(slot, "removeGlyph", new int[]{page, row}, new String[0]);
    }
    public static GrimoireActionPacket setName(int slot, int page, String name) {
        return new GrimoireActionPacket(slot, "setName", new int[]{page},
                new String[]{name == null ? "" : name});
    }
    public static GrimoireActionPacket setFavorite(int slot, int page, boolean fav) {
        return new GrimoireActionPacket(slot, "setFavorite", new int[]{page, fav ? 1 : 0}, new String[0]);
    }
    public static GrimoireActionPacket movePage(int slot, int from, int to) {
        return new GrimoireActionPacket(slot, "movePage", new int[]{from, to}, new String[0]);
    }
    public static GrimoireActionPacket resetGlyph(int slot, int page, int slotIdx) {
        return new GrimoireActionPacket(slot, "resetGlyph", new int[]{page, slotIdx}, new String[0]);
    }
    public static GrimoireActionPacket resetGlyphInBank(int slot, int bankIdx) {
        return new GrimoireActionPacket(slot, "resetGlyphInBank", new int[]{bankIdx}, new String[0]);
    }

    public static void handle(GrimoireActionPacket pkt, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player)) return;

        ctx.enqueueWork(() -> {
            ItemStack stack;
            if (player.containerMenu instanceof GlyphWorkbenchMenu menu) {
                stack = menu.getGrimoire();
            } else {
                int slot = pkt.itemSlot();
                if (slot < 0 || slot >= player.getInventory().getContainerSize()) return;
                stack = player.getInventory().getItem(slot);
            }

            if (!(stack.getItem() instanceof GrimoireItem)) return;

            int[] a = pkt.ints();
            String[] s = pkt.strings();

            switch (pkt.action()) {
                case "setPage"           -> { if (a.length < 1) return; }
                case "switchPage"        -> { if (a.length < 1) return; }
                case "placeGlyph"        -> { if (a.length < 2 || s.length < 1) return; }
                case "moveGlyph"         -> { if (a.length < 3) return; }
                case "removeGlyph"       -> { if (a.length < 2) return; }
                case "setName"           -> { if (a.length < 1 || s.length < 1) return; }
                case "setFavorite"       -> { if (a.length < 2) return; }
                case "movePage"          -> { if (a.length < 2) return; }
                case "resetGlyph"        -> { if (a.length < 2) return; }
                case "resetGlyphInBank"  -> { if (a.length < 1) return; }
                default -> { }
            }

            switch (pkt.action()) {
                case "setPage"     -> GrimoireData.setActivePage(stack, a[0]);
                case "switchPage"  -> { GrimoireData.switchPage(stack, a[0] != 0); broadcastTitle(player, stack); }
                case "placeGlyph"  -> {
                    Map<Stat, Integer> allocs = new HashMap<>();
                    if (s.length > 1 && !s[1].isEmpty()) {
                        for (String pair : s[1].split(",")) {
                            String[] kv = pair.split(":");
                            if (kv.length != 2) continue;
                            try {
                                int v = Integer.parseInt(kv[1]);
                                allocs.put(Stat.valueOf(kv[0]), v);
                            } catch (Exception ignored) {}
                        }
                    }
                    GrimoireData.placeGlyphWithAllocs(stack, a[0], a[1], s[0], allocs);
                }
                case "moveGlyph"   -> GrimoireData.moveGlyph(stack, a[0], a[1], a[2]);
                case "removeGlyph" -> GrimoireData.removeGlyph(stack, a[0], a[1]);
                case "setName"     -> GrimoireData.setPageName(stack, a[0], s[0]);
                case "setFavorite" -> GrimoireData.setPageFavorite(stack, a[0], a[1] != 0);
                case "movePage"    -> { GrimoireData.movePage(stack, a[0], a[1]); broadcastTitle(player, stack); }
                case "resetGlyph"  -> GrimoireData.resetGlyph(stack, a[0], a[1]);
                case "resetGlyphInBank" -> GrimoireData.resetGlyphInBank(stack, a[0]);
                default -> { }
            }
            player.containerMenu.broadcastChanges();
        });
    }

    private static void broadcastTitle(ServerPlayer player, ItemStack stack) {
        int idx = GrimoireData.getActivePage(stack);
        GlyphPage page = GrimoireData.getPage(stack, idx);
        Component msg;
        if (page == null) msg = Component.translatable("gui.grimoiremod.action.page", idx + 1);
        else if (page.isToc()) msg = Component.translatable("gui.grimoiremod.action.toc");
        else {
            int n = GrimoireData.spellNumber(stack, idx);
            String name = page.name == null ? "" : page.name.trim();
            msg = name.isEmpty()
                ? Component.translatable("gui.grimoiremod.action.spell", n)
                : Component.translatable("gui.grimoiremod.action.spell_named", n, name);
        }
        player.displayClientMessage(msg, true);
    }
}
