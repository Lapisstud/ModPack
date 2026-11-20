package com.example.grimoiremod.client;

import com.example.grimoiremod.GrimoireMod;
import com.example.grimoiremod.item.GrimoireItem;
import com.example.grimoiremod.network.GrimoireActionPacket;
import com.example.grimoiremod.util.GrimoireLocator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = GrimoireMod.MOD_ID, value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        if (mc.screen instanceof GrimoireScreen gs && gs.isNameFieldFocused()) {
            while (KeyBindings.PREV_SPELL.consumeClick()) { }
            while (KeyBindings.NEXT_SPELL.consumeClick()) { }
            while (KeyBindings.SKILL_TREE.consumeClick()) { }
            return;
        }

        while (KeyBindings.PREV_SPELL.consumeClick()) switchPage(player, false);
        while (KeyBindings.NEXT_SPELL.consumeClick()) switchPage(player, true);
        while (KeyBindings.SKILL_TREE.consumeClick()) openSkillTree(player);
    }

    private static void switchPage(Player player, boolean next) {
        Minecraft mc = Minecraft.getInstance();
        int slot;
        if (mc.screen instanceof GrimoireScreen gs) {
            slot = gs.getSlotId();
        } else {
            slot = GrimoireLocator.findFirst(player);
            if (slot < 0) return;
        }
        ItemStack stack = player.getInventory().getItem(slot);
        if (!(stack.getItem() instanceof GrimoireItem)) return;
        PacketDistributor.sendToServer(GrimoireActionPacket.switchPage(slot, next));
    }

    private static void openSkillTree(Player player) {
        Minecraft mc = Minecraft.getInstance();
        int slot;
        if (mc.screen instanceof GrimoireScreen gs) slot = gs.getSlotId();
        else slot = GrimoireLocator.findFirst(player);
        
        // Теперь открывается всегда, даже если гримуара нет (slot = -1)
        mc.setScreen(new SkillTreeScreen(slot));
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        int slot = GrimoireLocator.findFirst(mc.player);
        if (slot < 0) return;

        if (event.getScreen() instanceof InventoryScreen inv) {
            int guiLeft = (inv.width - 176) / 2;
            int guiTop = (inv.height - 166) / 2;
            int x = Math.max(4, guiLeft - 64);
            int y = guiTop + 4;
            event.addListener(Button.builder(
                    Component.translatable("gui.grimoiremod.grimoire"),
                    b -> mc.setScreen(new GrimoireScreen(slot))
            ).pos(x, y).size(60, 20).build());
            return;
        }

        if (event.getScreen() instanceof CreativeModeInventoryScreen) {
            event.addListener(Button.builder(
                    Component.translatable("gui.grimoiremod.grimoire"),
                    b -> mc.setScreen(new GrimoireScreen(slot))
            ).pos(4, 4).size(60, 20).build());
        }
    }

    public static int findGrimoireSlot(Player player) {
        return GrimoireLocator.findFirst(player);
    }
}
