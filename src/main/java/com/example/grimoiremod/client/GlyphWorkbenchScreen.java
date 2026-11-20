package com.example.grimoiremod.client;

import com.example.grimoiremod.data.GlyphInstance;
import com.example.grimoiremod.data.GlyphPage;
import com.example.grimoiremod.data.GrimoireData;
import com.example.grimoiremod.data.SpellParser;
import com.example.grimoiremod.glyphs.Glyph;
import com.example.grimoiremod.glyphs.Glyphs;
import com.example.grimoiremod.glyphs.Stat;
import com.example.grimoiremod.item.GrimoireItem;
import com.example.grimoiremod.menu.GlyphWorkbenchMenu;
import com.example.grimoiremod.network.AllocatePointPacket;
import com.example.grimoiremod.network.GrimoireActionPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GlyphWorkbenchScreen extends AbstractContainerScreen<GlyphWorkbenchMenu> {
    private final int grimoireSlot;
    private int selectedSlot = -1;
    private GlyphInstance selectedBankInst = null;
    private int bankScroll = 0;

    private static final int BUTTON_W = 14;
    private static final int BUTTON_H = 12;
    private static final int BANK_COLS = 7;
    private static final int BANK_ROWS_VISIBLE = 3;
    private static final int CELL_SIZE = 18;
    private static final int GAP = 2;
    private static final int SLOT_SIZE = 24;
    private static final int SLOT_GAP = 6;
    
    private static final int MAX_SLOTS_PER_PAGE = 7;
    private static final int WORKBENCH_SLOTS = 10;

    private GlyphInstance hoverInst = null;
    private String hoverErr = null;

    public GlyphWorkbenchScreen(GlyphWorkbenchMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.grimoireSlot = menu.getGrimoireSlot();
        this.imageWidth = 350;
        this.imageHeight = 240;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
    }

    @Override
    protected void init() {
        super.init();
        ClientGlyphIcons.clearCache();
    }

    @Override
    protected void renderLabels(GuiGraphics gfx, int mouseX, int mouseY) {
        gfx.drawString(font, this.title, this.titleLabelX, this.titleLabelY, 0xFFE0E0E0, false);
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float partialTick, int mouseX, int mouseY) {
        gfx.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF2E1B15);
        gfx.fill(leftPos + 2, topPos + 2, leftPos + imageWidth - 2, topPos + imageHeight - 2, 0xFF5D4037);

        int pX = leftPos + 180;
        int pY = topPos + 10;
        int pW = 160;
        int pH = 220;
        gfx.fill(pX, pY, pX + pW, pY + pH, 0xFFF2E8CF);
        gfx.fill(pX, pY, pX + pW, pY + 1, 0xFF8D6E63);
        gfx.fill(pX, pY, pX + 1, pY + pH, 0xFF8D6E63);

        gfx.fill(leftPos + 19, topPos + 19, leftPos + 37, topPos + 37, 0xFF373737);
        gfx.fill(leftPos + 20, topPos + 20, leftPos + 36, topPos + 36, 0xFF8B8B8B);

        int startX = leftPos + 18;
        int startY = topPos + 60;
        for (int i = 0; i < WORKBENCH_SLOTS; i++) {
            int x = startX + (i % 5) * (SLOT_SIZE + SLOT_GAP);
            int y = startY + (i / 5) * (SLOT_SIZE + SLOT_GAP);
            gfx.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0xFF373737);
            gfx.fill(x + 1, y + 1, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, 0xFF8B8B8B);
        }

        gfx.fill(leftPos + 10, topPos + 130, leftPos + 170, topPos + 131, 0xFF8D6E63);
        gfx.fill(leftPos + 10, topPos + 132, leftPos + 170, topPos + 232, 0xFF3A251A);
    }

    private record BankView(List<GlyphInstance> groups, Map<GlyphInstance, Integer> counts,
                            Map<GlyphInstance, Integer> firstIndices) {}

    private static BankView buildBankView(ItemStack book) {
        List<GlyphInstance> raw = GrimoireData.getBankList(book);
        Map<GlyphInstance, Integer> counts = new LinkedHashMap<>();
        Map<GlyphInstance, Integer> firstIndices = new HashMap<>();
        for (int i = 0; i < raw.size(); i++) {
            GlyphInstance inst = raw.get(i);
            if (Glyphs.byId(inst.typeId()) == null) continue;
            if (!firstIndices.containsKey(inst)) firstIndices.put(inst, i);
            counts.merge(inst, 1, Integer::sum);
        }
        return new BankView(new ArrayList<>(counts.keySet()), counts, firstIndices);
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gfx, mouseX, mouseY, partialTick);
        hoverInst = null;
        hoverErr = null;
        super.render(gfx, mouseX, mouseY, partialTick);

        ItemStack book = menu.getGrimoire();
        boolean hoverBook = inRect(mouseX, mouseY, leftPos + 20, topPos + 20, 16, 16);
        if (hoverBook) gfx.fill(leftPos + 19, topPos + 19, leftPos + 37, topPos + 37, 0x80FFFFFF);

        if (!(book.getItem() instanceof GrimoireItem)) {
            gfx.drawString(font, Component.translatable("gui.grimoiremod.grimoire_not_found").getString(),
                    leftPos + 50, topPos + 25, 0xFFFF5555, false);
            this.renderTooltip(gfx, mouseX, mouseY);
            return;
        }

        int pageCount = GrimoireData.getPageCount(book);
        int active = GrimoireData.getActivePage(book);
        GlyphPage page = GrimoireData.getPage(book, active);

        int navX = leftPos + 60, navY = topPos + 22;
        boolean hoverPrev = inRect(mouseX, mouseY, navX, navY, 12, 12);
        boolean hoverNext = inRect(mouseX, mouseY, navX + 40, navY, 12, 12);
        gfx.drawString(font, "<", navX + 3, navY + 2, hoverPrev ? 0xFFFFFFFF : 0xFFAAAAAA, false);
        gfx.drawString(font, ">", navX + 43, navY + 2, hoverNext ? 0xFFFFFFFF : 0xFFAAAAAA, false);

        int displayTotal = pageCount;
        boolean hasToc = false;
        for (int i = 0; i < pageCount; i++) {
            GlyphPage p = GrimoireData.getPage(book, i);
            if (p != null && p.isToc()) hasToc = true;
        }
        if (hasToc) displayTotal--;

        int displayActive = GrimoireData.spellNumber(book, active);
        String navText = (page != null && page.isToc()) 
                ? Component.translatable("gui.grimoiremod.toc_short").getString() 
                : (displayActive + "/" + displayTotal);
        gfx.drawString(font, navText, navX + 18, navY + 2, 0xFFAAAAAA, false);

        if (page != null && !page.isToc()) {
            String spellName = page.name == null || page.name.isBlank()
                    ? Component.translatable("gui.grimoiremod.spell", GrimoireData.spellNumber(book, active)).getString()
                    : page.name;
            gfx.drawString(font, spellName, leftPos + 20, topPos + 45, 0xFFE0E0E0, false);

            int startX = leftPos + 18;
            int startY = topPos + 60;
            SpellParser.Parsed parsed = SpellParser.parse(page);
            Map<Integer, String> errors = parsed.errors();

            for (int i = 0; i < WORKBENCH_SLOTS; i++) {
                int x = startX + (i % 5) * (SLOT_SIZE + SLOT_GAP);
                int y = startY + (i / 5) * (SLOT_SIZE + SLOT_GAP);
                boolean isLocked = i >= page.slotCount;
                boolean isSel = (i == selectedSlot);
                boolean hover = inRect(mouseX, mouseY, x, y, SLOT_SIZE, SLOT_SIZE);

                int border = isLocked ? 0xFF222222 : (isSel ? 0xFFFFAA00 : 0xFF373737);
                int fill = isLocked ? 0xFF333333 : 0xFF8B8B8B;
                gfx.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, border);
                gfx.fill(x + 1, y + 1, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, fill);

                if (isLocked) {
                    String lockStr = "\uD83D\uDD12";
                    int lw = font.width(lockStr);
                    gfx.drawString(font, lockStr, x + SLOT_SIZE / 2 - lw / 2, y + SLOT_SIZE / 2 - 4, 0xFF888888, false);
                    continue;
                }

                GlyphInstance inst = page.slots[i];
                if (inst != null) {
                    Glyph g = Glyphs.byId(inst.typeId());
                    if (g != null) {
                        ClientGlyphIcons.draw(gfx, font, g, x + 2, y + 2, 20);
                        if (errors.containsKey(i)) {
                            gfx.fill(x - 1, y - 1, x + 9, y + 9, 0xFFFF0000);
                            gfx.drawString(font, "!", x + 4 - font.width("!") / 2, y, 0xFFFFFFFF, false);
                            if (hover) hoverErr = errors.get(i);
                        }
                        if (hover) hoverInst = inst;
                    }
                }
            }

            if (errors.containsKey(-1) && inRect(mouseX, mouseY, leftPos + 18, topPos + 60,
                    5 * (SLOT_SIZE + SLOT_GAP), 2 * (SLOT_SIZE + SLOT_GAP))) {
                if (hoverInst == null && hoverErr == null) hoverErr = errors.get(-1);
            }

            gfx.drawString(font, Component.translatable("gui.grimoiremod.bank").getString(),
                    leftPos + 12, topPos + 136, 0xFFAAAAAA, false);

            BankView bankView = buildBankView(book);
            List<GlyphInstance> groupList = bankView.groups();
            Map<GlyphInstance, Integer> counts = bankView.counts();

            int bankX = leftPos + 20, bankY = topPos + 150;
            int maxScroll = Math.max(0, groupList.size() - (BANK_COLS * BANK_ROWS_VISIBLE));
            if (bankScroll > maxScroll) bankScroll = maxScroll;

            for (int gi = bankScroll; gi < groupList.size() && gi < bankScroll + BANK_COLS * BANK_ROWS_VISIBLE; gi++) {
                GlyphInstance inst = groupList.get(gi);
                Glyph g = Glyphs.byId(inst.typeId());
                if (g == null) continue;
                int localIdx = gi - bankScroll;
                int col = localIdx % BANK_COLS, row = localIdx / BANK_COLS;
                int x = bankX + col * (CELL_SIZE + GAP);
                int y = bankY + row * (CELL_SIZE + GAP);
                boolean isSel = (selectedBankInst != null && selectedBankInst.equals(inst));
                int border = isSel ? 0xFFFFAA00 : 0xFF222222;
                gfx.fill(x, y, x + CELL_SIZE, y + CELL_SIZE, border);
                gfx.fill(x + 1, y + 1, x + CELL_SIZE - 1, y + CELL_SIZE - 1, 0xFF555555);
                ClientGlyphIcons.draw(gfx, font, g, x + 1, y + 1, 16);
                if (inRect(mouseX, mouseY, x, y, CELL_SIZE, CELL_SIZE)) hoverInst = inst;
                int count = counts.get(inst);
                if (count > 1) {
                    String cnt = "x" + count;
                    gfx.pose().pushPose();
                    gfx.pose().translate(x + CELL_SIZE - 1, y + CELL_SIZE - 6, 0);
                    float scale = count >= 100 ? 0.5f : (count >= 10 ? 0.6f : 0.75f);
                    gfx.pose().scale(scale, scale, 1.0f);
                    gfx.drawString(font, cnt, -font.width(cnt), 0, 0xFF333333, false);
                    gfx.pose().popPose();
                }
                if (inst.totalPoints() > 0) gfx.drawString(font, "+", x + CELL_SIZE - 6, y, 0xFFFFAA00, false);
            }

            if (groupList.size() > BANK_COLS * BANK_ROWS_VISIBLE) {
                int scrollX = leftPos + 162;
                boolean hoverUp = inRect(mouseX, mouseY, scrollX, bankY, 10, 10);
                boolean hoverDown = inRect(mouseX, mouseY, scrollX, bankY + 40, 10, 10);
                gfx.drawString(font, "^", scrollX, bankY, hoverUp ? 0xFFFFFFFF : 0xFFAAAAAA, false);
                gfx.drawString(font, "v", scrollX, bankY + 40, hoverDown ? 0xFFFFFFFF : 0xFFAAAAAA, false);
            }

            GlyphInstance targetInst = null;
            boolean isBankTarget = false;

            if (selectedSlot >= 0 && selectedSlot < page.slotCount && page.slots[selectedSlot] != null) {
                targetInst = page.slots[selectedSlot];
            } else if (selectedBankInst != null) {
                targetInst = selectedBankInst;
                isBankTarget = true;
            }

            if (targetInst != null) {
                Glyph g = Glyphs.byId(targetInst.typeId());
                if (g != null) {
                    int textX = leftPos + 190, textY = topPos + 20;
                    String prefix = isBankTarget ? Component.translatable("gui.grimoiremod.bank_prefix").getString() : "";
                    gfx.drawString(font, prefix + g.getDisplayName().getString(), textX, textY, 0xFF3A2E20, false);
                    gfx.drawString(font, Component.translatable("gui.grimoiremod.points", targetInst.totalPoints()).getString(),
                            textX, textY + 12, 0xFF886A4A, false);
                    gfx.fill(textX, textY + 24, leftPos + 330, textY + 25, 0xFF8D6E63);

                    List<Stat> stats = new ArrayList<>();
                    for (Stat cand : Stat.values()) {
                        if (g.canUpgrade(cand)) stats.add(cand);
                    }
                    
                    for (int s = 0; s < stats.size(); s++) {
                        Stat stat = stats.get(s);
                        int pts = targetInst.getPoints(stat);
                        int y = textY + 35 + s * 24;
                        gfx.drawString(font, stat.getDisplayName().getString() + ": " + pts, textX, y + 2, 0xFF333333, false);

                        int plusX = leftPos + 280, minusX = leftPos + 305;
                        boolean hoverPlus = inRect(mouseX, mouseY, plusX, y, BUTTON_W, BUTTON_H);
                        boolean hoverMinus = inRect(mouseX, mouseY, minusX, y, BUTTON_W, BUTTON_H);
                        boolean isAdvanced = g.advanced();
                        boolean canAdd = !isAdvanced && (GrimoireData.getSpentPoints(book, stat) < GrimoireData.getStatBudget(book, stat));
                        boolean canSub = !isAdvanced && (pts > 0);

                        int plusFill = canAdd ? (hoverPlus ? 0xFF77AA77 : 0xFF555555) : 0xFF333333;
                        gfx.fill(plusX, y, plusX + BUTTON_W, y + BUTTON_H, plusFill);
                        gfx.drawString(font, "+", plusX + 4, y + 2, canAdd ? 0xFFFFFFFF : 0xFF888888, false);

                        int minusFill = canSub ? (hoverMinus ? 0xFFAA7777 : 0xFF555555) : 0xFF333333;
                        gfx.fill(minusX, y, minusX + BUTTON_W, y + BUTTON_H, minusFill);
                        gfx.drawString(font, "-", minusX + 5, y + 2, canSub ? 0xFFFFFFFF : 0xFF888888, false);
                    }

                    int resetY = textY + 165;
                    boolean hoverReset = inRect(mouseX, mouseY, textX, resetY, 60, 16);
                    gfx.fill(textX, resetY, textX + 60, resetY + 16, hoverReset ? 0xFFAA4444 : 0xFF555555);
                    gfx.drawString(font, Component.translatable("gui.grimoiremod.reset").getString(), textX + 8, resetY + 4, 0xFFFFFFFF, false);
                }
            } else {
                String msg = Component.translatable("gui.grimoiremod.select_glyph").getString();
                int msgW = font.width(msg);
                gfx.drawString(font, msg, leftPos + 180 + (160 - msgW) / 2, topPos + 100, 0xFF888888, false);
            }
        }

        this.renderTooltip(gfx, mouseX, mouseY);

        if (hoverInst != null) {
            List<Component> tooltip = GlyphTooltip.build(hoverInst, hoverErr);
            if (!tooltip.isEmpty()) gfx.renderTooltip(font, tooltip, java.util.Optional.empty(), mouseX, mouseY);
        } else if (hoverErr != null) {
            gfx.renderTooltip(font, Component.translatable(hoverErr), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        ItemStack book = menu.getGrimoire();
        if (!(book.getItem() instanceof GrimoireItem)) return super.mouseClicked(mx, my, button);

        int active = GrimoireData.getActivePage(book);
        GlyphPage page = GrimoireData.getPage(book, active);
        if (page == null || page.isToc()) return super.mouseClicked(mx, my, button);

        int navX = leftPos + 60, navY = topPos + 22;
        if (inRect(mx, my, navX, navY, 12, 12)) {
            PacketDistributor.sendToServer(GrimoireActionPacket.switchPage(grimoireSlot, false));
            selectedSlot = -1; selectedBankInst = null; return true;
        }
        if (inRect(mx, my, navX + 40, navY, 12, 12)) {
            PacketDistributor.sendToServer(GrimoireActionPacket.switchPage(grimoireSlot, true));
            selectedSlot = -1; selectedBankInst = null; return true;
        }

        int startX = leftPos + 18, startY = topPos + 60;
        for (int i = 0; i < WORKBENCH_SLOTS; i++) {
            if (i >= page.slotCount) continue;
            int x = startX + (i % 5) * (SLOT_SIZE + SLOT_GAP);
            int y = startY + (i / 5) * (SLOT_SIZE + SLOT_GAP);
            if (inRect(mx, my, x, y, SLOT_SIZE, SLOT_SIZE)) {
                if (button == 1) {
                    if (page.slots[i] != null) {
                        PacketDistributor.sendToServer(GrimoireActionPacket.removeGlyph(grimoireSlot, active, i));
                        selectedSlot = -1;
                    }
                } else {
                    if (page.slots[i] != null) {
                        selectedSlot = (selectedSlot == i) ? -1 : i;
                        selectedBankInst = null;
                    } else if (selectedBankInst != null) {
                        PacketDistributor.sendToServer(GrimoireActionPacket.placeGlyph(
                                grimoireSlot, active, i, selectedBankInst.typeId(), selectedBankInst.allocations()));
                        selectedBankInst = null;
                    }
                }
                return true;
            }
        }

        BankView bankView = buildBankView(book);
        List<GlyphInstance> groupList = bankView.groups();
        int bankX = leftPos + 20, bankY = topPos + 150;
        int maxScroll = Math.max(0, groupList.size() - (BANK_COLS * BANK_ROWS_VISIBLE));

        if (groupList.size() > BANK_COLS * BANK_ROWS_VISIBLE) {
            int scrollX = leftPos + 162;
            if (inRect(mx, my, scrollX, bankY, 10, 10)) { bankScroll = Math.max(0, bankScroll - BANK_COLS); return true; }
            if (inRect(mx, my, scrollX, bankY + 40, 10, 10)) { bankScroll = Math.min(maxScroll, bankScroll + BANK_COLS); return true; }
        }

        for (int gi = bankScroll; gi < groupList.size() && gi < bankScroll + BANK_COLS * BANK_ROWS_VISIBLE; gi++) {
            int localIdx = gi - bankScroll;
            int col = localIdx % BANK_COLS, row = localIdx / BANK_COLS;
            int x = bankX + col * (CELL_SIZE + GAP);
            int y = bankY + row * (CELL_SIZE + GAP);
            if (inRect(mx, my, x, y, CELL_SIZE, CELL_SIZE)) {
                GlyphInstance inst = groupList.get(gi);
                selectedBankInst = (selectedBankInst != null && selectedBankInst.equals(inst)) ? null : inst;
                selectedSlot = -1;
                return true;
            }
        }

        List<GlyphInstance> rawBank = GrimoireData.getBankList(book);
        GlyphInstance targetInst = null;
        boolean isBankTarget = false;
        if (selectedSlot >= 0 && selectedSlot < page.slotCount && page.slots[selectedSlot] != null) {
            targetInst = page.slots[selectedSlot];
        } else if (selectedBankInst != null) {
            targetInst = selectedBankInst;
            isBankTarget = true;
        }

        if (targetInst != null) {
            List<Stat> stats = new ArrayList<>();
            Glyph g = Glyphs.byId(targetInst.typeId());
            for (Stat cand : Stat.values()) {
                if (g.canUpgrade(cand)) stats.add(cand);
            }
            
            int textY = topPos + 20;
            boolean isAdvanced = g != null && g.advanced();
            for (int s = 0; s < stats.size(); s++) {
                int y = textY + 35 + s * 24;
                int plusX = leftPos + 280, minusX = leftPos + 305;
                
                int targetIdx = -1;
                if (isBankTarget) {
                    targetIdx = rawBank.indexOf(selectedBankInst);
                } else {
                    targetIdx = selectedSlot;
                }

                if (inRect(mx, my, plusX, y, BUTTON_W, BUTTON_H)) {
                    if (!isAdvanced && GrimoireData.canAllocate(book, stats.get(s), 1) && targetIdx != -1) {
                        PacketDistributor.sendToServer(new AllocatePointPacket(
                                grimoireSlot, active, targetIdx, stats.get(s), 1, isBankTarget));
                    }
                    return true;
                }
                if (inRect(mx, my, minusX, y, BUTTON_W, BUTTON_H)) {
                    if (!isAdvanced && targetIdx != -1) {
                        PacketDistributor.sendToServer(new AllocatePointPacket(
                                grimoireSlot, active, targetIdx, stats.get(s), -1, isBankTarget));
                    }
                    return true;
                }
            }
            int resetY = textY + 165;
            if (inRect(mx, my, leftPos + 190, resetY, 60, 16)) {
                if (isBankTarget) {
                    int idx = rawBank.indexOf(selectedBankInst);
                    if (idx != -1) PacketDistributor.sendToServer(GrimoireActionPacket.resetGlyphInBank(grimoireSlot, idx));
                } else {
                    PacketDistributor.sendToServer(GrimoireActionPacket.resetGlyph(grimoireSlot, active, selectedSlot));
                }
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    private boolean inRect(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
