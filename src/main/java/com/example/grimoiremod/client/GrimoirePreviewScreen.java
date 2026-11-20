package com.example.grimoiremod.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class GrimoirePreviewScreen extends Screen {
    private static final int PAGE_W = 180, PAGE_H = 290, SPINE = 8;
    private static final int BOOK_W = PAGE_W * 2 + SPINE, COVER_PAD = 10;
    private static final int COVER = 0xFF4A2E1E, PAGE_BG = 0xFFF2E8CF;
    private static final int INK = 0xFF3A2E20, INK_DIM = 0xFF886A4A;
    private static final int SLOT_BORDER = 0xFF5B4636, CAT_HEADER = 0xFF6A4A30;
    private static final int TAB_ACTIVE = 0xFFE8D8A0;

    private int bookLeft, bookTop, leftPageX, rightPageX;

    public GrimoirePreviewScreen() { super(Component.literal("Grimoire Preview")); }

    @Override
    protected void init() {
        bookLeft = (width - BOOK_W) / 2;
        bookTop = (height - PAGE_H) / 2;
        leftPageX = bookLeft;
        rightPageX = bookLeft + PAGE_W + SPINE;
    }

    @Override
    public void render(GuiGraphics gfx, int mx, int my, float pt) {
        gfx.fill(0, 0, width, height, 0xCC100808);
        gfx.fill(bookLeft - COVER_PAD, bookTop - COVER_PAD,
                 bookLeft + BOOK_W + COVER_PAD, bookTop + PAGE_H + COVER_PAD, COVER);
        gfx.fill(leftPageX + PAGE_W, bookTop, rightPageX, bookTop + PAGE_H, 0xFF2E1C10);
        gfx.fill(leftPageX, bookTop, leftPageX + PAGE_W, bookTop + PAGE_H, PAGE_BG);
        gfx.fill(rightPageX, bookTop, rightPageX + PAGE_W, bookTop + PAGE_H, PAGE_BG);

        int startX = leftPageX + 15, startY = bookTop + 70, SLOT = 24, SLOT_GAP = 4;
        int[] colors = {0xFFE05A5A, 0xFF5AB8E0, 0xFF5A8BE0, 0xFFC9A64A};
        for (int i = 0; i < 4; i++) {
            int x = startX, y = startY + i * (SLOT + SLOT_GAP);
            gfx.fill(x, y, x + SLOT, y + SLOT, SLOT_BORDER);
            gfx.fill(x + 1, y + 1, x + SLOT - 1, y + SLOT - 1, 0xFFE8DDC0);
            gfx.fill(x + 4, y + 4, x + SLOT - 4, y + SLOT - 4, colors[i]);
        }
        gfx.drawString(font, "Заклинание 1", leftPageX + 8, bookTop + 4, INK, false);
        gfx.drawString(font, "Итог:", leftPageX + 55, bookTop + 55, INK_DIM, false);
        gfx.drawString(font, "15 урона", leftPageX + 55, bookTop + 67, INK, false);
        gfx.drawString(font, "Банк глифов", rightPageX + 8, bookTop + 4, CAT_HEADER, false);

        String[] tabs = {"Вс", "Фм", "Ст", "Эф", "Мд", "Ф+"};
        int tabX = rightPageX + 8, tabY = bookTop + 16;
        for (String t : tabs) {
            int tw = font.width(t) + 8;
            gfx.fill(tabX, tabY, tabX + tw, tabY + 12, TAB_ACTIVE);
            gfx.drawString(font, t, tabX + 4, tabY + 2, INK, false);
            tabX += tw;
        }
        int bankX = rightPageX + 8, bankY = bookTop + 34;
        for (int i = 0; i < 12; i++) {
            int col = i % 4, row = i / 4;
            int x = bankX + col * 28, y = bankY + row * 28;
            gfx.fill(x, y, x + 26, y + 26, SLOT_BORDER);
            gfx.fill(x + 1, y + 1, x + 25, y + 25, 0xFFF8EFD5);
            gfx.fill(x + 5, y + 5, x + 21, y + 21,
                    (i % 3 == 0) ? 0xFFE05A5A : (i % 3 == 1) ? 0xFF5A8BE0 : 0xFF5AB8E0);
        }
        gfx.drawString(font, "Preview — ESC", width / 2 - 40, height - 20, 0xFFAAAAAA, false);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) { onClose(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override public boolean isPauseScreen() { return false; }
}
