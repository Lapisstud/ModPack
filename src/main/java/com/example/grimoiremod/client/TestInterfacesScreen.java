package com.example.grimoiremod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class TestInterfacesScreen extends Screen {
    public TestInterfacesScreen() {
        super(Component.literal("Проверка интерфейсов"));
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int y = height / 2 - 60;
        addRenderableWidget(Button.builder(Component.literal("Гримуар"), b ->
                Minecraft.getInstance().setScreen(new GrimoirePreviewScreen()))
                .pos(cx - 100, y).size(200, 20).build());
        y += 26;
        addRenderableWidget(Button.builder(Component.literal("Верстак глифов"), b ->
                Minecraft.getInstance().setScreen(new WorkbenchPreviewScreen()))
                .pos(cx - 100, y).size(200, 20).build());
        y += 26;
        addRenderableWidget(Button.builder(Component.literal("Дерево навыков"), b ->
                Minecraft.getInstance().setScreen(
                        null))
                .pos(cx - 100, y).size(200, 20).build());
        y += 26;
        addRenderableWidget(Button.builder(Component.literal("Закрыть"), b -> onClose())
                .pos(cx - 100, y).size(200, 20).build());
    }

    @Override
    public void render(GuiGraphics gfx, int mx, int my, float pt) {
        renderBackground(gfx, mx, my, pt);
        gfx.drawString(font, "Проверка интерфейсов", width / 2 - 60, height / 2 - 90, 0xFFE0E0E0, false);
        super.render(gfx, mx, my, pt);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
