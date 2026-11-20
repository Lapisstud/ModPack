package com.example.grimoiremod.menu;

import com.example.grimoiremod.GrimoireMod;
import com.example.grimoiremod.item.GrimoireItem;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class GlyphWorkbenchMenu extends AbstractContainerMenu {
    private final Player player;
    private final int grimoireSlotIndex;

    public GlyphWorkbenchMenu(int id, Inventory playerInventory, int grimoireSlotIndex) {
        super(GrimoireMod.GLYPH_WORKBENCH_MENU.get(), id);
        this.player = playerInventory.player;
        this.grimoireSlotIndex = grimoireSlotIndex;

        this.addSlot(new Slot(playerInventory, grimoireSlotIndex >= 0 ? grimoireSlotIndex : 0, 20, 20) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
            @Override public boolean mayPickup(Player p) { return false; }
            @Override public boolean isActive() { return grimoireSlotIndex >= 0; }
        });
    }

    public ItemStack getGrimoire() {
        if (grimoireSlotIndex < 0 || grimoireSlotIndex >= player.getInventory().getContainerSize()) return ItemStack.EMPTY;
        return player.getInventory().getItem(grimoireSlotIndex);
    }

    public int getGrimoireSlot() { return grimoireSlotIndex; }

    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }

    @Override
    public boolean stillValid(Player player) {
        if (grimoireSlotIndex < 0 || grimoireSlotIndex >= player.getInventory().getContainerSize()) return false;
        return player.getInventory().getItem(grimoireSlotIndex).getItem() instanceof GrimoireItem;
    }
}
