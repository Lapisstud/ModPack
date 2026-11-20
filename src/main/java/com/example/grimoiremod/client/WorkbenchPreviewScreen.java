package com.example.grimoiremod.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class WorkbenchPreviewScreen extends Screen {
    private static final int BG = 0xFF2E1B15, BG_IN = 0xFF5D4037;
    private static final int PAPER = 0xFFF2E8CF, LINE = 0xFF8D6E63;

    public WorkbenchPreviewScreen() { super(Component.literal("Workbench Preview")); }

    @Override
    public void render(GuiGraphics gfx, int mx, int my, float pt) {
        gfx.fill(0, 0, width, height, 0xCC100808);
        int guiW = 350, guiH = 240;
        int leftPos = (width - guiW) / 2, topPos = (height - guiH) / 2;

        gfx.fill(leftPos, topPos, leftPos + guiW, topPos + guiH, BG);
        gfx.fill(leftPos + 2, topPos + 2, leftPos + guiW - 2, topPos + guiH - 2, BG_IN);

        int pX = leftPos + 180, pY = topPos + 10, pW = 160, pH = 220;
        gfx.fill(pX, pY, pX + pW, pY + pH, PAPER);
        gfx.fill(pX, pY, pX + pW, pY + 1, LINE);
        gfx.fill(pX, pY, pX + 1, pY + pH, LINE);

        gfx.fill(leftPos + 19, topPos + 19, leftPos + 37, topPos + 37, 0xFF373737);
        gfx.fill(leftPos + 20, topPos + 20, leftPos + 36, topPos + 36, 0xFF8B8B8B);

        gfx.fill(leftPos + 10, topPos + 140, leftPos + 170, topPos + 141, LINE);
        gfx.fill(leftPos + 10, topPos + 142, leftPos + 170, topPos + 232, 0xFF3A251A);

        int startX = leftPos + 20, startY = topPos + 60, slotSize = 24, gap = 6;
        int[] colors = {0xFFE05A5A, 0xFF5AB8E0, 0xFF5A8BE0};
        for (int i = 0; i < 3; i++) {
            int x = startX + (i % 2) * (slotSize + gap);
            int y = startY + (i / 2) * (slotSize + gap);
            gfx.fill(x, y, x + slotSize, y + slotSize, 0xFF373737);
            gfx.fill(x + 1, y + 1, x + slotSize - 1, y + slotSize - 1, 0xFF8B8B8B);
            gfx.fill(x + 4, y + 4, x + slotSize - 4, y + slotSize - 4, colors[i]);
        }

        gfx.drawString(font, "Банк глифов", leftPos + 12, topPos + 146, 0xFFAAAAAA, false);
        int bankX = leftPos + 14, bankY = topPos + 160;
        for (int i = 0; i < 21; i++) {
            int col = i % 7, row = i / 7;
            int x = bankX + col * 20, y = bankY + row * 20;
            gfx.fill(x, y, x + 18, y + 18, 0xFF222222);
            gfx.fill(x + 1, y + 1, x + 17, y + 17, 0xFF555555);
            gfx.fill(x + 4, y + 4, x + 14, y + 14,
                    (i % 3 == 0) ? 0xFFE05A5A : (i % 3 == 1) ? 0xFF5A8BE0 : 0xFF5AB8E0);
        }

        // Right panel stats
        gfx.drawString(font, "Банк: fire", leftPos + 190, topPos + 20, 0xFF3A2E20, false);
        gfx.drawString(font, "Очки: 3", leftPos + 190, topPos + 32, 0xFF886A4A, false);
        gfx.fill(leftPos + 190, topPos + 44, leftPos + 330, topPos + 45, LINE);
        gfx.drawString(font, "Damage: 3", leftPos + 190, topPos + 57, 0xFF333333, false);
        gfx.fill(leftPos + 280, topPos + 55, leftPos + 294, topPos + 67, 0xFF555555);
        gfx.drawString(font, "+", leftPos + 284, topPos + 57, 0xFFFFFFFF, false);

        gfx.drawString(font, "Preview — ESC", width / 2 - 40, height - 20, 0xFFAAAAAA, false);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) { onClose(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override public boolean isPauseScreen() { return false; }
}
