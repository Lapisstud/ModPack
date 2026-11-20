package com.example.grimoiremod.util;

import com.example.grimoiremod.item.GrimoireItem;
import net.minecraft.world.entity.player.Player;

public final class GrimoireLocator {
    private GrimoireLocator() {}

    /** Возвращает слот активного гримуара: selected если гримуар, иначе первый в инвентаре. -1 = нет. */
    public static int findFirst(Player player) {
        var inv = player.getInventory();
        if (inv.getItem(inv.selected).getItem() instanceof GrimoireItem) return inv.selected;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).getItem() instanceof GrimoireItem) return i;
        }
        return -1;
    }
}
