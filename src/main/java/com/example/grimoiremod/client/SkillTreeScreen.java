package com.example.grimoiremod.client;

import com.example.grimoiremod.data.SkillTreeData;
import com.example.grimoiremod.data.SkillTreeDef;
import com.example.grimoiremod.item.GrimoireItem;
import com.example.grimoiremod.network.LevelUpSkillNodePacket;
import com.example.grimoiremod.network.UnlockSkillNodePacket;
import com.example.grimoiremod.util.GrimoireLocator;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class SkillTreeScreen extends Screen {
    private static final int NODE_SIZE = 28;
    private static final int SPACING = 70;
    private static final int TAB_GENERAL = 0;
    private static final int TAB_MAGIC = 1;

    private static final int LINE_LOCKED = 0xFF332211;
    private static final int LINE_UNLOCKED = 0xFF886A4A;

    private final int itemSlotId;
    private int activeTab = TAB_MAGIC;
    private final int[][] tabRects = new int[2][4];
    
    private float zoom = 1.0f;
    private float panX = 0;
    private float panY = 0;
    
    private boolean isDragging = false;
    private double lastMouseX, lastMouseY;

    private String hoverNodeId = null;
    private String selectedNodeId = null;

    public SkillTreeScreen(int itemSlotId) {
        super(Component.translatable("gui.grimoiremod.skill_tree.title"));
        this.itemSlotId = itemSlotId;
    }

    private ItemStack book() {
        var p = Minecraft.getInstance().player;
        if (p == null) return ItemStack.EMPTY;
        int actualSlot = itemSlotId >= 0 ? itemSlotId : GrimoireLocator.findFirst(p);
        if (actualSlot < 0 || actualSlot >= p.getInventory().getContainerSize()) return ItemStack.EMPTY;
        ItemStack s = p.getInventory().getItem(actualSlot);
        return s.getItem() instanceof GrimoireItem ? s : ItemStack.EMPTY;
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override
    public void render(GuiGraphics gfx, int mx, int my, float pt) {
        super.render(gfx, mx, my, pt);
        gfx.fill(0, 0, width, height, 0x80000000); // Полупрозрачный фон

        renderTabs(gfx, mx, my);

        if (activeTab == TAB_GENERAL) {
            String soon = Component.translatable("gui.grimoiremod.skill_tree.soon").getString();
            gfx.drawString(font, soon, width / 2 - font.width(soon) / 2, height / 2, 0xFFAAAAAA, false);
            return;
        }

        ItemStack book = book();
        boolean hasBook = !book.isEmpty();
        float ptsFrac = hasBook ? SkillTreeData.getPointsFrac(book) : 0f;
        
        String ptsStr = String.valueOf((int)ptsFrac);
        if (ptsFrac - (int)ptsFrac > 0.01f) ptsStr = String.format("%.1f", ptsFrac);

        gfx.pose().pushPose();
        gfx.pose().translate(width / 2.0f + panX, height / 2.0f + panY, 0);
        gfx.pose().scale(zoom, zoom, 1.0f);

        hoverNodeId = null;

        // 1. Линии
        for (SkillTreeDef.Node n : SkillTreeDef.NODES) {
            for (String preId : n.prereqs()) {
                SkillTreeDef.Node pre = SkillTreeDef.byId(preId);
                if (pre == null) continue;
                
                boolean unlocked = hasBook && SkillTreeData.isUnlocked(book, n.id());
                int color = unlocked ? LINE_UNLOCKED : LINE_LOCKED;
                
                drawLine(gfx, pre.x() * SPACING, pre.y() * SPACING, n.x() * SPACING, n.y() * SPACING, color);
            }
        }

        // 2. Узлы
        for (SkillTreeDef.Node n : SkillTreeDef.NODES) {
            int nx = n.x() * SPACING - NODE_SIZE / 2;
            int ny = n.y() * SPACING - NODE_SIZE / 2;
            
            boolean unlocked = hasBook && SkillTreeData.isUnlocked(book, n.id());
            boolean canUnlock = false;
            if (hasBook && !unlocked) {
                boolean hasPrereqs = true;
                for (String p : n.prereqs()) {
                    if (!SkillTreeData.isUnlocked(book, p)) { hasPrereqs = false; break; }
                }
                if (hasPrereqs && ptsFrac >= n.cost()) canUnlock = true;
            }

            double worldMx = (mx - (width / 2.0f + panX)) / zoom;
            double worldMy = (my - (height / 2.0f + panY)) / zoom;
            boolean hover = worldMx >= nx && worldMx < nx + NODE_SIZE && worldMy >= ny && worldMy < ny + NODE_SIZE;
            if (hover) hoverNodeId = n.id();

            int border = unlocked ? 0xFF55AA55 : (canUnlock ? 0xFFFFFF55 : 0xFF555555);
            int fill = unlocked ? 0xFF2A5A2A : (canUnlock ? 0xFF5A5A2A : 0xFF2A2A2A);
            
            if (hover) border = 0xFFFFFFFF;

            gfx.fill(nx - 2, ny - 2, nx + NODE_SIZE + 2, ny + NODE_SIZE + 2, 0xFF000000);
            gfx.fill(nx, ny, nx + NODE_SIZE, ny + NODE_SIZE, border);
            gfx.fill(nx + 2, ny + 2, nx + NODE_SIZE - 2, ny + NODE_SIZE - 2, fill);

            // Локализованная первая буква
            String name = Component.translatable(n.nameKey()).getString();
            String label = name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase();
            
            int tw = font.width(label);
            gfx.drawString(font, label, nx + NODE_SIZE / 2 - tw / 2, ny + NODE_SIZE / 2 - 4, 0xFFFFFFFF, false);
        }

        gfx.pose().popPose();

        String ptsText = Component.translatable("gui.grimoiremod.skill_tree.points", ptsStr).getString();
        gfx.drawString(font, ptsText, 10, 30, 0xFFFFFFFF, true);

        if (hoverNodeId != null) {
            SkillTreeDef.Node n = SkillTreeDef.byId(hoverNodeId);
            if (n != null) {
                List<Component> tooltip = new ArrayList<>();
                tooltip.add(Component.translatable(n.nameKey()).withStyle(ChatFormatting.YELLOW));

                if (!hasBook) {
                    tooltip.add(Component.translatable("gui.grimoiremod.skill_tree.requires_grimoire").withStyle(ChatFormatting.RED));
                } else if (SkillTreeData.isUnlocked(book, n.id())) {
                    int level = SkillTreeData.getNodeLevel(book, n.id());
                    float exp = SkillTreeData.getNodeExp(book, n.id());
                    tooltip.add(Component.translatable("gui.grimoiremod.skill_tree.level", level, n.maxLevel()));
                    if (level < n.maxLevel()) {
                        tooltip.add(Component.translatable("gui.grimoiremod.skill_tree.exp", String.format("%.1f", exp * 100)));
                        if (ptsFrac >= 1.0f) {
                            tooltip.add(Component.translatable("gui.grimoiremod.skill_tree.level_up").withStyle(ChatFormatting.GREEN));
                        }
                    } else {
                        tooltip.add(Component.translatable("gui.grimoiremod.skill_tree.max_level").withStyle(ChatFormatting.GOLD));
                    }
                } else {
                    List<String> missingPrereqs = new ArrayList<>();
                    for (String p : n.prereqs()) {
                        if (!SkillTreeData.isUnlocked(book, p)) {
                            SkillTreeDef.Node pNode = SkillTreeDef.byId(p);
                            missingPrereqs.add(pNode != null ? Component.translatable(pNode.nameKey()).getString() : p);
                        }
                    }
                    
                    if (!missingPrereqs.isEmpty()) {
                        tooltip.add(Component.translatable("gui.grimoiremod.skill_tree.requires_node", String.join(", ", missingPrereqs)).withStyle(ChatFormatting.RED));
                    } else if (ptsFrac < n.cost()) {
                        tooltip.add(Component.translatable("gui.grimoiremod.skill_tree.no_points").withStyle(ChatFormatting.RED));
                    } else {
                        tooltip.add(Component.translatable("gui.grimoiremod.skill_tree.unlock", n.cost()).withStyle(ChatFormatting.GREEN));
                    }
                }
                gfx.renderTooltip(font, tooltip, java.util.Optional.empty(), mx, my);
            }
        }

        if (selectedNodeId != null) {
            SkillTreeDef.Node n = SkillTreeDef.byId(selectedNodeId);
            if (n != null) {
                renderComingSoon(gfx, n, hasBook ? book : ItemStack.EMPTY);
            }
        }
    }

    private void renderTabs(GuiGraphics gfx, int mx, int my) {
        String[] labels = {
                Component.translatable("gui.grimoiremod.skill_tree.general").getString(),
                Component.translatable("gui.grimoiremod.skill_tree.main").getString()
        };
        int totalW = 0;
        for (String l : labels) totalW += font.width(l) + 16 + 2;
        int x = width / 2 - totalW / 2;
        int y = 6;
        for (int t = 0; t < 2; t++) {
            int w = font.width(labels[t]) + 16;
            boolean active = t == activeTab;
            boolean hover = inRect(mx, my, x, y, w, 20);
            gfx.fill(x, y, x + w, y + 20, active ? 0xEEFFFFFF : (hover ? 0x44FFFFFF : 0x88000000));
            gfx.drawString(font, labels[t], x + 8, y + 6, active ? 0xFF000000 : 0xFFAAAAAA, false);
            if (active) gfx.fill(x, y + 20, x + w, y + 22, 0xFFFFAA00);
            tabRects[t] = new int[]{x, y, w, 20};
            x += w + 2;
        }
    }

    private void renderComingSoon(GuiGraphics gfx, SkillTreeDef.Node n, ItemStack book) {
        int w = 200, h = 120;
        int px = (width - w) / 2;
        int py = (height - h) / 2;
        
        gfx.fill(0, 0, width, height, 0x88000000);
        gfx.fill(px, py, px + w, py + h, 0xFF2E1C10);
        gfx.fill(px + 2, py + 2, px + w - 2, py + h - 2, 0xFFF2E8CF);

        gfx.drawString(font, Component.translatable(n.nameKey()).getString(), px + 10, py + 10, 0xFF3A2E20, false);
        gfx.fill(px + 10, py + 22, px + w - 10, py + 23, 0xFF886A4A);

        int level = book.isEmpty() ? 0 : SkillTreeData.getNodeLevel(book, n.id());
        float exp = book.isEmpty() ? 0 : SkillTreeData.getNodeExp(book, n.id());
        
        gfx.drawString(font, "Level: " + level + " / " + n.maxLevel(), px + 10, py + 35, 0xFF3A2E20, false);
        gfx.drawString(font, String.format("Progress: %.1f%%", exp * 100), px + 10, py + 50, 0xFF3A2E20, false);
        
        gfx.drawString(font, "Coming soon...", px + 10, py + 80, 0xFFAA4444, false);
        gfx.drawString(font, "Click outside to close", px + 10, py + h - 15, 0xFF888888, false);
    }

    private void drawLine(GuiGraphics gfx, int x1, int y1, int x2, int y2, int color) {
        int dx = Math.abs(x2 - x1), dy = Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1, sy = y1 < y2 ? 1 : -1;
        int err = dx - dy;
        while (true) {
            gfx.fill(x1 - 2, y1 - 2, x1 + 2, y1 + 2, color);
            if (x1 == x2 && y1 == y2) break;
            int e2 = 2 * err;
            if (e2 > -dy) { err -= dy; x1 += sx; }
            if (e2 < dx) { err += dx; y1 += sy; }
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        for (int t = 0; t < 2; t++) {
            int[] r = tabRects[t];
            if (r[2] > 0 && inRect(mx, my, r[0], r[1], r[2], r[3])) {
                activeTab = t;
                selectedNodeId = null;
                return true;
            }
        }

        if (activeTab == TAB_GENERAL) return super.mouseClicked(mx, my, button);

        if (selectedNodeId != null) {
            selectedNodeId = null;
            return true;
        }

        double worldMx = (mx - (width / 2.0f + panX)) / zoom;
        double worldMy = (my - (height / 2.0f + panY)) / zoom;

        for (SkillTreeDef.Node n : SkillTreeDef.NODES) {
            int nx = n.x() * SPACING - NODE_SIZE / 2;
            int ny = n.y() * SPACING - NODE_SIZE / 2;
            if (worldMx >= nx && worldMx < nx + NODE_SIZE && worldMy >= ny && worldMy < ny + NODE_SIZE) {
                ItemStack book = book();
                if (book.isEmpty()) return true;

                if (button == 0) {
                    if (!SkillTreeData.isUnlocked(book, n.id())) {
                        boolean hasPrereqs = true;
                        for (String p : n.prereqs()) {
                            if (!SkillTreeData.isUnlocked(book, p)) { hasPrereqs = false; break; }
                        }
                        if (hasPrereqs && SkillTreeData.getPointsFrac(book) >= n.cost()) {
                            PacketDistributor.sendToServer(UnlockSkillNodePacket.of(itemSlotId, n.id()));
                        }
                    } else {
                        selectedNodeId = n.id();
                    }
                } else if (button == 1) {
                    if (SkillTreeData.isUnlocked(book, n.id())) {
                        int level = SkillTreeData.getNodeLevel(book, n.id());
                        if (level < n.maxLevel() && SkillTreeData.getPointsFrac(book) >= 1.0f) {
                            PacketDistributor.sendToServer(new LevelUpSkillNodePacket(itemSlotId, n.id()));
                        }
                    }
                }
                return true;
            }
        }

        if (button == 0) {
            isDragging = true;
            lastMouseX = mx;
            lastMouseY = my;
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        isDragging = false;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (isDragging) {
            panX += (float) (mx - lastMouseX);
            panY += (float) (my - lastMouseY);
            lastMouseX = mx;
            lastMouseY = my;
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        if (activeTab == TAB_GENERAL) return super.mouseScrolled(mx, my, dx, dy);
        float oldZoom = zoom;
        zoom = (float) Math.max(0.4, Math.min(2.0, zoom + Math.signum(dy) * 0.1));
        float scaleDiff = zoom - oldZoom;
        panX -= (float) (mx - width / 2.0f) * scaleDiff / oldZoom;
        panY -= (float) (my - height / 2.0f) * scaleDiff / oldZoom;
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_K) { onClose(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private static boolean inRect(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
