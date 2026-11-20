package com.example.grimoiremod.client;

import com.example.grimoiremod.data.GlyphInstance;
import com.example.grimoiremod.data.GlyphPage;
import com.example.grimoiremod.data.GrimoireData;
import com.example.grimoiremod.data.SpellDescriber;
import com.example.grimoiremod.data.SpellParser;
import com.example.grimoiremod.glyphs.Glyph;
import com.example.grimoiremod.glyphs.GlyphCategory;
import com.example.grimoiremod.glyphs.Glyphs;
import com.example.grimoiremod.item.GrimoireItem;
import com.example.grimoiremod.network.GrimoireActionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GrimoireScreen extends Screen {

    private static final int PAGE_W = 180;
    private static final int PAGE_H = 290;
    private static final int SPINE  = 8;
    private static final int BOOK_W = PAGE_W * 2 + SPINE;
    private static final int BOOK_H = PAGE_H;
    private static final int COVER_PAD = 10;
    private static final int MAX_SLOTS_PER_PAGE = 7;

    private static final int ICON = 16;
    private static final int SLOT = 24;
    private static final int SLOT_GAP = 4;
    private static final int BANK_CELL = 24;
    private static final int BANK_GAP = 4;
    private static final int BANK_COLS = 4;

    private static final int TOC_ROW_H = 22;
    private static final int TOC_TOP   = 30;

    private static final int COVER         = 0xFF4A2E1E;
    private static final int PAGE_BG       = 0xFFF2E8CF;
    private static final int PAGE_BG_ALT   = 0xFFEADDBA;
    private static final int PAGE_BG_HOVER = 0xFFF7EDCB;
    private static final int PAGE_BG_DRAG  = 0xFFD8C89A;
    private static final int INK           = 0xFF3A2E20;
    private static final int INK_DIM       = 0xFF886A4A;
    private static final int SLOT_BORDER   = 0xFF5B4636;
    private static final int SLOT_SEL      = 0xFFFFE9A0;
    private static final int SLOT_DROP     = 0xFF88CC66;
    private static final int CAT_HEADER    = 0xFF6A4A30;
    private static final int TOC_GOLD      = 0xFFC9A64A;
    private static final int TAB_ACTIVE    = 0xFFE8D8A0;
    private static final int TAB_HOVER     = 0xFFF0E8C0;
    private static final int TAB_IDLE      = 0xFFDCCFA0;

    private static final GlyphCategory[] TAB_CATEGORIES = {
            null, GlyphCategory.FORM, GlyphCategory.ELEMENT,
            GlyphCategory.EFFECT, GlyphCategory.MODIFIER, GlyphCategory.FORM_MODIFIER
    };
    private static final String[] TAB_LABELS_SHORT = { "gui.grimoiremod.tab.all_short", "gui.grimoiremod.tab.forms_short", "gui.grimoiremod.tab.elements_short", "gui.grimoiremod.tab.effects_short", "gui.grimoiremod.tab.modifiers_short", "gui.grimoiremod.tab.form_mods_short" };
    private static final String[] TAB_LABELS_FULL  = { "gui.grimoiremod.tab.all", "gui.grimoiremod.tab.forms", "gui.grimoiremod.tab.elements", "gui.grimoiremod.tab.effects", "gui.grimoiremod.tab.modifiers", "gui.grimoiremod.tab.form_mods" };

    private final int itemSlotId;
    private int bookLeft, bookTop;
    private int leftPageX, leftPageY, rightPageX, rightPageY;

    private int selectedSlot = -1;
    private int bankScroll = 0;
    private GlyphInstance hoverGlyph = null;
    private String hoverError = null;
    private int hoverBankTab = -1;
    private int hoverNav = 0;
    private int hoverBookmark = -1;
    private GlyphInstance draggingInst = null;
    private boolean draggingFromPage = false;

    private GlyphCategory selectedBankTab = null;
    private final int[] tabWidths = new int[TAB_CATEGORIES.length];

    private int tocDragFrom = -1;
    private int tocDropTarget = -1;
    private boolean tocDropAfter = false;
    private boolean tocDragMoved = false;

    private EditBox nameField;
    private int lastPageForName = -1;
    private final int[][] tabPositions = new int[TAB_CATEGORIES.length][4];

    private CompoundTag cachedTag;
    private Snapshot  cachedSnap;

    public GrimoireScreen(int itemSlotId) {
        super(Component.translatable("gui.grimoiremod.grimoire"));
        this.itemSlotId = itemSlotId;
    }

    public int getSlotId() { return itemSlotId; }

    public boolean isNameFieldFocused() {
        return nameField != null && nameField.isFocused();
    }

    private ItemStack stack() {
        var p = Minecraft.getInstance().player;
        if (p == null) return ItemStack.EMPTY;
        ItemStack s = p.getInventory().getItem(itemSlotId);
        return s.getItem() instanceof GrimoireItem ? s : ItemStack.EMPTY;
    }

    private Snapshot snapshot() {
        ItemStack s = stack();
        if (s.isEmpty()) return null;
        CustomData cd = s.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = cd != null ? cd.copyTag() : new CompoundTag();
        if (tag.equals(cachedTag) && cachedSnap != null) return cachedSnap;

        int pageCount = GrimoireData.getPageCount(s);
        int active    = GrimoireData.getActivePage(s);
        List<GlyphPage> pages = new ArrayList<>(pageCount);
        int[] spellNums = new int[pageCount];
        int n = 0;
        boolean hasToc = false;
        for (int i = 0; i < pageCount; i++) {
            GlyphPage p = GrimoireData.getPage(s, i);
            pages.add(p);
            if (p != null && p.isSpell()) n++;
            if (p != null && p.isToc()) hasToc = true;
            spellNums[i] = n;
        }
        Map<GlyphInstance, Integer> bank = null;
        GlyphPage activePage = (active >= 0 && active < pages.size()) ? pages.get(active) : null;
        if (activePage != null && activePage.isSpell()) {
            bank = GrimoireData.getBankGrouped(s);
        }

        SpellParser.Parsed parsed = null;
        List<Component> described = null;
        if (activePage != null && activePage.isSpell()) {
            parsed = SpellParser.parse(activePage);
            described = SpellDescriber.describeLines(activePage);
        }

        cachedTag = tag;
        cachedSnap = new Snapshot(pageCount, active, pages, spellNums, bank, hasToc, parsed, described);
        return cachedSnap;
    }

    private record Snapshot(int pageCount, int active, List<GlyphPage> pages,
                            int[] spellNumbers, Map<GlyphInstance, Integer> bank,
                            boolean hasToc, SpellParser.Parsed parsed, List<Component> describedLines) {
        GlyphPage page(int i) { return (i >= 0 && i < pages.size()) ? pages.get(i) : null; }
        GlyphPage activePage() { return page(active); }
        int spellNumber(int i) { return (i >= 0 && i < spellNumbers.length) ? spellNumbers[i] : 0; }
    }

    @Override
    protected void init() {
        bookLeft = (width - BOOK_W) / 2;
        bookTop  = (height - BOOK_H) / 2;
        leftPageX  = bookLeft;
        leftPageY  = bookTop;
        rightPageX = bookLeft + PAGE_W + SPINE;
        rightPageY = bookTop;

        ClientGlyphIcons.clearCache();
        selectedBankTab = null;

        int availableW = PAGE_W - 16;
        for (int t = 0; t < TAB_CATEGORIES.length; t++) tabWidths[t] = availableW / TAB_CATEGORIES.length;

        nameField = new EditBox(font, leftPageX + 8, leftPageY + 20, PAGE_W - 16, 12,
                Component.translatable("gui.grimoiremod.name"));
        nameField.setMaxLength(32);
        nameField.setBordered(true);
        lastPageForName = -1;
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override
    public void render(GuiGraphics gfx, int mx, int my, float pt) {
        super.render(gfx, mx, my, pt);

        Snapshot snap = snapshot();
        if (snap == null) { Minecraft.getInstance().tell(this::onClose); return; }

        int pageCount = snap.pageCount();
        int active    = snap.active();
        GlyphPage page = snap.activePage();

        gfx.fill(0, 0, width, height, 0x99000000);
        gfx.fill(bookLeft - COVER_PAD, bookTop - COVER_PAD,
                 bookLeft + BOOK_W + COVER_PAD, bookTop + BOOK_H + COVER_PAD, COVER);
        gfx.fill(leftPageX + PAGE_W, leftPageY, rightPageX, rightPageY, 0xFF2E1C10);
        gfx.fill(leftPageX, leftPageY, leftPageX + PAGE_W, leftPageY + PAGE_H, PAGE_BG);
        gfx.fill(rightPageX, rightPageY, rightPageX + PAGE_W, rightPageY + PAGE_H, PAGE_BG);

        hoverGlyph = null;
        hoverError = null;
        hoverNav = 0;
        hoverBookmark = -1;

        if (page == null) return;

        if (page.isToc()) hoverBankTab = -1;

        if (active != lastPageForName) {
            lastPageForName = active;
            nameField.setValue(page.name == null ? "" : page.name);
            nameField.setFocused(false);
            selectedSlot = -1;
        }

        renderBookmarks(gfx, mx, my, snap, active);

        if (page.isToc()) {
            if (tocDragFrom >= 0) updateTocDrop(mx, my, snap);
            renderTocPage(gfx, mx, my, snap);
        } else {
            renderTabs(gfx, mx, my);
            renderLeftPage(gfx, mx, my, snap, page, active);
            renderBank(gfx, mx, my, snap);
            nameField.render(gfx, mx, my, pt);
        }

        renderNav(gfx, mx, my, active, pageCount, snap);

        if (draggingInst != null && !page.isToc()) {
            int dropSlot = slotUnderCursor(mx, my, page);
            if (dropSlot >= 0 && page.slots[dropSlot] == null) {
                highlightSlot(gfx, dropSlot, SLOT_DROP);
            }
            Glyph g = Glyphs.byId(draggingInst.typeId());
            if (g != null) {
                ClientGlyphIcons.draw(gfx, font, g, (int)(mx - ICON / 2.0), (int)(my - ICON / 2.0), ICON);
            }
        }

        if (hoverGlyph != null) {
            List<Component> tooltip = GlyphTooltip.build(hoverGlyph, hoverError);
            if (!tooltip.isEmpty()) gfx.renderTooltip(font, tooltip, java.util.Optional.empty(), mx, my);
        }
    }

    private void renderBookmarks(GuiGraphics gfx, int mx, int my, Snapshot snap, int active) {
        int bx = bookLeft + BOOK_W + COVER_PAD + 2;
        int by0 = bookTop + 8;

        int maxBookmarks = 10;
        int bh = (BOOK_H - 16) / maxBookmarks;
        int favCount = 0;
        hoverBookmark = -1;

        for (int i = 0; i < snap.pageCount() && favCount < maxBookmarks; i++) {
            GlyphPage p = snap.page(i);
            if (p == null || !p.favorite) continue;

            int by = by0 + favCount * bh;
            boolean hover = inRect(mx, my, bx, by, 22, bh - 1);
            if (hover) hoverBookmark = i;

            int color = (i == active) ? brighten(p.bookmarkColor) : p.bookmarkColor;
            int border = hover ? 0xFFFFFFFF : 0xFF2E1C10;
            gfx.fill(bx - 1, by - 1, bx + 23, by + bh, border);
            gfx.fill(bx, by, bx + 22, by + bh - 1, color);

            String label = p.isToc() ? "T" : String.valueOf(snap.spellNumber(i));
            int textColor = getContrastColor(color);
            gfx.drawString(font, label, bx + 11 - font.width(label) / 2, by + bh / 2 - 4, textColor, false);

            if (hover) {
                String name = p.isToc()
                        ? Component.translatable("gui.grimoiremod.toc").getString()
                        : (p.name == null || p.name.isBlank()
                            ? Component.translatable("gui.grimoiremod.spell", snap.spellNumber(i)).getString()
                            : p.name);
                int tw = font.width(name);
                gfx.fill(bx + 22, by, bx + 22 + tw + 8, by + bh - 1, color);
                gfx.fill(bx + 22 + tw + 8, by, bx + 22 + tw + 9, by + bh - 1, border);
                gfx.drawString(font, name, bx + 26, by + bh / 2 - 4, textColor, false);
            }

            favCount++;
        }
    }

    private int getContrastColor(int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int yiq = ((r * 299) + (g * 587) + (b * 114)) / 1000;
        return (yiq >= 128) ? 0xFF000000 : 0xFFFFFFFF;
    }

    private void renderTocPage(GuiGraphics gfx, int mx, int my, Snapshot snap) {
        gfx.drawString(font, Component.translatable("gui.grimoiremod.toc").getString(),
                leftPageX + 8, leftPageY + 4, INK, false);
        gfx.fill(leftPageX + 8, leftPageY + 18, leftPageX + PAGE_W - 8, leftPageY + 19, INK_DIM);

        int count = snap.pageCount();
        int rowY = leftPageY + TOC_TOP;
        boolean hasIndicator = tocDragFrom >= 0 && tocDropTarget >= 0 && tocDragMoved;
        int indicatorY = -1;

        if (hasIndicator) {
            int to = computeToPage(tocDragFrom, tocDropTarget, tocDropAfter);
            if (to != tocDragFrom) {
                int scanY = leftPageY + TOC_TOP;
                for (int i = 0; i < count; i++) {
                    GlyphPage p = snap.page(i);
                    if (p == null || p.isToc()) continue;
                    if (i == tocDropTarget) {
                        indicatorY = tocDropAfter ? scanY + TOC_ROW_H - 2 : scanY;
                        break;
                    }
                    scanY += TOC_ROW_H;
                }
            }
        }

        for (int i = 0; i < count; i++) {
            GlyphPage p = snap.page(i);
            if (p == null) continue;
            if (p.isToc()) continue;

            boolean isDragging = (i == tocDragFrom);
            boolean hover = inRect(mx, my, leftPageX + 8, rowY, PAGE_W - 16, TOC_ROW_H - 2);

            int bg;
            if (isDragging && tocDragMoved) bg = PAGE_BG_DRAG;
            else if (hover) bg = PAGE_BG_HOVER;
            else bg = PAGE_BG_ALT;

            gfx.fill(leftPageX + 8, rowY, leftPageX + PAGE_W - 8, rowY + TOC_ROW_H - 2, bg);
            gfx.fill(leftPageX + 12, rowY + 4, leftPageX + 16, rowY + TOC_ROW_H - 6, p.bookmarkColor);

            int num = snap.spellNumber(i);
            String label = Component.translatable("gui.grimoiremod.spell", num).getString();
            if (p.name != null && !p.name.isBlank()) label += ": " + p.name;
            int textColor = (isDragging && tocDragMoved) ? INK_DIM : INK;
            gfx.drawString(font, label, leftPageX + 22, rowY + 5, textColor, false);

            String favStr = p.favorite ? "*" : "-";
            gfx.drawString(font, favStr, leftPageX + PAGE_W - 20, rowY + 5,
                    p.favorite ? 0xFFFFAA00 : INK_DIM, false);

            rowY += TOC_ROW_H;
        }

        if (indicatorY >= 0) {
            gfx.fill(leftPageX + 8, indicatorY - 1, leftPageX + PAGE_W - 8, indicatorY + 1, TOC_GOLD);
        }

        String hint = tocDragFrom >= 0 && tocDragMoved
                ? Component.translatable("gui.grimoiremod.drop_hint").getString()
                : Component.translatable("gui.grimoiremod.drag_hint").getString();
        gfx.drawString(font, hint, leftPageX + 8, leftPageY + PAGE_H - 20, INK_DIM, false);
    }

    private void renderLeftPage(GuiGraphics gfx, int mx, int my, Snapshot snap, GlyphPage page, int pageIdx) {
        int num = snap.spellNumber(pageIdx);
        gfx.drawString(font, Component.translatable("gui.grimoiremod.spell", num).getString(),
                leftPageX + 8, leftPageY + 4, INK, false);

        String favStr = page.favorite ? "*" : "-";
        gfx.drawString(font, favStr, leftPageX + PAGE_W - 20, leftPageY + 4,
                page.favorite ? 0xFFFFAA00 : INK_DIM, false);

        int startY = leftPageY + 70;
        int startX = leftPageX + 15;

        SpellParser.Parsed parsed = snap.parsed();
        Map<Integer, String> errors = parsed != null ? parsed.errors() : Map.of();

        int slotsToRender = Math.min(page.slotCount, MAX_SLOTS_PER_PAGE);
        for (int i = 0; i < slotsToRender; i++) {
            int x = startX;
            int y = startY + i * (SLOT + SLOT_GAP);
            boolean selected = i == selectedSlot;
            boolean hover = inRect(mx, my, x, y, SLOT, SLOT);

            int border = selected ? SLOT_SEL : SLOT_BORDER;
            gfx.fill(x, y, x + SLOT, y + SLOT, border);
            int bg = hover ? 0xFFFFF4C0 : 0xFFE8DDC0;
            gfx.fill(x + 1, y + 1, x + SLOT - 1, y + SLOT - 1, bg);

            GlyphInstance inst = page.slots[i];
            if (inst != null) {
                Glyph g = Glyphs.byId(inst.typeId());
                if (g != null) {
                    ClientGlyphIcons.draw(gfx, font, g, x, y, SLOT);
                    if (errors.containsKey(i)) {
                        gfx.fill(x - 1, y - 1, x + 9, y + 9, 0xFFFF0000);
                        gfx.drawString(font, "!", x + 4 - font.width("!") / 2, y, 0xFFFFFFFF, false);
                        if (hover) hoverError = errors.get(i);
                    }
                    if (hover) hoverGlyph = inst;
                }
            }
        }

        if (errors.containsKey(-1) && inRect(mx, my, startX, startY, SLOT, slotsToRender * (SLOT + SLOT_GAP))) {
            if (hoverGlyph == null && hoverError == null) hoverError = errors.get(-1);
        }

        int resX = leftPageX + 55;
        int resY = leftPageY + 55;
        gfx.drawString(font, Component.translatable("gui.grimoiremod.result").getString(), resX, resY, INK_DIM, false);

        List<Component> resultLines = snap.describedLines();
        if (resultLines == null) resultLines = List.of();
        int lineY = resY + 12;
        for (Component line : resultLines) {
            List<FormattedCharSequence> wrapped = font.split(line, PAGE_W - 65);
            for (FormattedCharSequence seq : wrapped) {
                if (lineY + 10 > leftPageY + PAGE_H - 20) break;
                gfx.drawString(font, seq, resX, lineY, INK, false);
                lineY += 10;
            }
        }
    }

    private void renderBank(GuiGraphics gfx, int mx, int my, Snapshot snap) {
        gfx.drawString(font, Component.translatable("gui.grimoiremod.bank").getString(),
                rightPageX + 8, rightPageY + 4, CAT_HEADER, false);

        int startX = rightPageX + 8;
        int startY = rightPageY + 38;

        Map<GlyphInstance, Integer> bank = snap.bank();
        if (bank == null) return;

        List<Map.Entry<GlyphInstance, Integer>> filtered = new ArrayList<>();
        for (var e : bank.entrySet()) {
            Glyph g = Glyphs.byId(e.getKey().typeId());
            if (g == null) continue;
            if (selectedBankTab != null && g.category() != selectedBankTab) continue;
            filtered.add(e);
        }

        int bankAreaH = PAGE_H - 38 - 6;
        int maxVisible = BANK_COLS * (bankAreaH / (BANK_CELL + BANK_GAP));
        int maxScroll = Math.max(0, filtered.size() - maxVisible);
        if (bankScroll > maxScroll) bankScroll = maxScroll;
        if (bankScroll < 0) bankScroll = 0;

        for (int gi = bankScroll; gi < filtered.size() && gi < bankScroll + maxVisible; gi++) {
            var e = filtered.get(gi);
            GlyphInstance inst = e.getKey();
            Glyph g = Glyphs.byId(inst.typeId());
            if (g == null) continue;

            int localIdx = gi - bankScroll;
            int col = localIdx % BANK_COLS;
            int row = localIdx / BANK_COLS;
            int x = startX + col * (BANK_CELL + BANK_GAP);
            int y = startY + row * (BANK_CELL + BANK_GAP);

            boolean hover = inRect(mx, my, x, y, ICON + 4, ICON + 4);
            if (hover && draggingInst == null) {
                hoverGlyph = inst;
                hoverError = null;
            }

            int border = hover ? 0xFFFFFFFF : SLOT_BORDER;
            gfx.fill(x - 1, y - 1, x + ICON + 3, y + ICON + 3, border);
            gfx.fill(x, y, x + ICON + 2, y + ICON + 2, 0xFFF8EFD5);
            ClientGlyphIcons.draw(gfx, font, g, x + 1, y + 1, ICON);

            String cnt = "x" + e.getValue();
            int tw = font.width(cnt);
            int cx = x + ICON + 2 - tw + 1;
            int cy = y + ICON + 2 - 8;
            gfx.drawString(font, cnt, cx, cy, 0xFF333333, false);

            if (inst.totalPoints() > 0) {
                gfx.drawString(font, "+" + inst.totalPoints(), x + 1, y - 6, 0xFFFFAA00, false);
            }
        }

        if (filtered.size() > maxVisible) {
            int barX = rightPageX + PAGE_W - 5;
            int barY = startY;
            gfx.fill(barX, barY, barX + 2, barY + bankAreaH, 0x33FFFFFF);
            int handleH = Math.max(10, bankAreaH * maxVisible / filtered.size());
            int handleY = barY + (bankAreaH - handleH) * bankScroll / Math.max(1, maxScroll);
            gfx.fill(barX, handleY, barX + 2, handleY + handleH, 0xFFCCCCCC);
        }

        if (filtered.isEmpty()) {
            String msg = Component.translatable("gui.grimoiremod.no_glyphs").getString();
            int tw = font.width(msg);
            gfx.drawString(font, msg, rightPageX + PAGE_W / 2 - tw / 2, startY + 40, INK_DIM, false);
        }
    }

    private void renderNav(GuiGraphics gfx, int mx, int my, int active, int total, Snapshot snap) {
        int y = bookTop + BOOK_H - 14;
        int lx = leftPageX + 6;
        int rx = rightPageX + PAGE_W - 20;
        boolean lHover = inRect(mx, my, lx, y, 14, 12);
        boolean rHover = inRect(mx, my, rx, y, 14, 12);
        gfx.drawString(font, "<", lx + 3, y, lHover ? INK : INK_DIM, false);
        gfx.drawString(font, ">", rx + 3, y, rHover ? INK : INK_DIM, false);
        if (lHover) hoverNav = -1;
        if (rHover) hoverNav = +1;

        int displayTotal = total;
        if (snap.hasToc()) displayTotal--;

        int num = snap.spellNumber(active);
        String pg = (num == 0)
                ? Component.translatable("gui.grimoiremod.toc_short").getString()
                : (num + " / " + displayTotal);
        gfx.drawString(font, pg, leftPageX + PAGE_W - 10 - font.width(pg), y, INK_DIM, false);
    }

    private void renderTabs(GuiGraphics gfx, int mx, int my) {
        int startX = rightPageX + 4;
        int startY = rightPageY + 20;
        int tabH = 14;
        int activeIdx = getTabIndex(selectedBankTab);
        int expandedIdx = (hoverBankTab != -1) ? hoverBankTab : activeIdx;
        int currentX = startX;

        for (int t = 0; t < TAB_CATEGORIES.length; t++) {
            String label = (t == expandedIdx)
                    ? Component.translatable(TAB_LABELS_FULL[t]).getString()
                    : Component.translatable(TAB_LABELS_SHORT[t]).getString();
            int w = font.width(label) + 6;

            boolean isActive = (t == activeIdx);
            boolean isHover = (t == hoverBankTab);
            int color = isActive ? TAB_ACTIVE : (isHover ? TAB_HOVER : TAB_IDLE);

            gfx.fill(currentX, startY, currentX + w, startY + tabH, color);
            gfx.fill(currentX, startY + tabH, currentX + w, startY + tabH + 1, SLOT_BORDER);
            if (isActive) gfx.fill(currentX, startY + tabH - 2, currentX + w, startY + tabH, TOC_GOLD);

            gfx.drawString(font, label, currentX + 3, startY + 3, INK, false);

            tabPositions[t] = new int[]{currentX, startY, w, tabH};
            currentX += w + 1;
        }
    }

    private void highlightSlot(GuiGraphics gfx, int slot, int color) {
        int startY = leftPageY + 70;
        int startX = leftPageX + 15;
        int y = startY + slot * (SLOT + SLOT_GAP);
        gfx.fill(startX - 2, y - 2, startX + SLOT + 2, y + SLOT + 2, color);
        gfx.fill(startX, y, startX + SLOT, y + SLOT, SLOT_BORDER);
        gfx.fill(startX + 1, y + 1, startX + SLOT - 1, y + SLOT - 1, 0xFFF0F5D8);
    }

    private int slotUnderCursor(double mx, double my, GlyphPage page) {
        int startY = leftPageY + 70;
        int startX = leftPageX + 15;
        for (int i = 0; i < Math.min(page.slotCount, MAX_SLOTS_PER_PAGE); i++) {
            int y = startY + i * (SLOT + SLOT_GAP);
            if (inRect(mx, my, startX, y, SLOT, SLOT)) return i;
        }
        return -1;
    }

    private int getTabIndex(GlyphCategory cat) {
        for (int i = 0; i < TAB_CATEGORIES.length; i++) {
            if (TAB_CATEGORIES[i] == cat) return i;
        }
        return 0;
    }

    private void refreshHoverState(double mx, double my) {
        hoverBookmark = -1;
        hoverNav = 0;
        Snapshot snap = snapshot();
        if (snap != null) {
            int bx = bookLeft + BOOK_W + COVER_PAD + 2;
            int by0 = bookTop + 8;
            int bh = (BOOK_H - 16) / 10;
            int favCount = 0;
            for (int i = 0; i < snap.pageCount() && favCount < 10; i++) {
                if (snap.page(i) == null || !snap.page(i).favorite) continue;
                if (inRect(mx, my, bx, by0 + favCount * bh, 22, bh - 1)) { hoverBookmark = i; break; }
                favCount++;
            }
            int y = bookTop + BOOK_H - 14;
            if (inRect(mx, my, leftPageX + 6, y, 14, 12)) hoverNav = -1;
            if (inRect(mx, my, rightPageX + PAGE_W - 20, y, 14, 12)) hoverNav = +1;
        }
        updateTabHover(mx, my);
    }

    private void updateTabHover(double mx, double my) {
        int newHover = -1;
        for (int t = 0; t < TAB_CATEGORIES.length; t++) {
            if (tabPositions[t][2] > 0 && inRect(mx, my, tabPositions[t][0], tabPositions[t][1], tabPositions[t][2], tabPositions[t][3])) {
                newHover = t;
                break;
            }
        }
        hoverBankTab = newHover;
    }

    @Override
    public void mouseMoved(double mx, double my) {
        updateTabHover(mx, my);
        super.mouseMoved(mx, my);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        Snapshot snap = snapshot();
        boolean onToc = snap == null || snap.activePage() == null || snap.activePage().isToc();
        if (!onToc && nameField != null && nameField.isMouseOver(mx, my)) {
            nameField.setFocused(true);
            nameField.mouseClicked(mx, my, button);
            return true;
        }
        if (snap == null) return super.mouseClicked(mx, my, button);

        boolean wasFocused = nameField != null && nameField.isFocused();
        if (nameField != null) nameField.setFocused(false);
        if (wasFocused && snap.activePage() != null && snap.activePage().isSpell()) {
            sendName(snap.active(), snap.activePage());
        }

        refreshHoverState(mx, my);

        int active = snap.active();
        GlyphPage page = snap.activePage();
        if (page == null) return super.mouseClicked(mx, my, button);

        if (hoverBookmark >= 0) {
            if (hoverBookmark != active) {
                sendName(active, page);
                PacketDistributor.sendToServer(GrimoireActionPacket.setPage(itemSlotId, hoverBookmark));
                selectedSlot = -1;
                draggingInst = null;
            }
            return true;
        }

        if (hoverNav != 0) {
            sendName(active, page);
            PacketDistributor.sendToServer(GrimoireActionPacket.switchPage(itemSlotId, hoverNav > 0));
            selectedSlot = -1;
            draggingInst = null;
            return true;
        }

        if (!page.isToc()) {
            if (inRect(mx, my, leftPageX + PAGE_W - 20, leftPageY + 4, 12, 12)) {
                PacketDistributor.sendToServer(GrimoireActionPacket.setFavorite(itemSlotId, active, !page.favorite));
                return true;
            }
            for (int t = 0; t < TAB_CATEGORIES.length; t++) {
                if (tabPositions[t][2] > 0 && inRect(mx, my, tabPositions[t][0], tabPositions[t][1], tabPositions[t][2], tabPositions[t][3])) {
                    selectedBankTab = TAB_CATEGORIES[t];
                    return true;
                }
            }
        }

        if (page.isToc()) {
            int count = snap.pageCount();
            int rowY = leftPageY + TOC_TOP;
            for (int i = 0; i < count; i++) {
                GlyphPage p = snap.page(i);
                if (p == null || p.isToc()) continue;
                if (inRect(mx, my, leftPageX + PAGE_W - 20, rowY, 12, TOC_ROW_H - 2)) {
                    PacketDistributor.sendToServer(GrimoireActionPacket.setFavorite(itemSlotId, i, !p.favorite));
                    return true;
                }
                if (inRect(mx, my, leftPageX + 8, rowY, PAGE_W - 16, TOC_ROW_H - 2)) {
                    resetTocDrag();
                    tocDragFrom = i;
                    selectedSlot = -1;
                    draggingInst = null;
                    return true;
                }
                rowY += TOC_ROW_H;
            }
            return super.mouseClicked(mx, my, button);
        }

        Map<GlyphInstance, Integer> bank = snap.bank();
        if (bank != null) {
            int startX = rightPageX + 8;
            int startY = rightPageY + 38;
            List<Map.Entry<GlyphInstance, Integer>> filtered = new ArrayList<>();
            for (var e : bank.entrySet()) {
                Glyph g = Glyphs.byId(e.getKey().typeId());
                if (g == null) continue;
                if (selectedBankTab != null && g.category() != selectedBankTab) continue;
                filtered.add(e);
            }
            int bankAreaH = PAGE_H - 38 - 6;
            int maxVisible = BANK_COLS * (bankAreaH / (BANK_CELL + BANK_GAP));
            for (int gi = bankScroll; gi < filtered.size() && gi < bankScroll + maxVisible; gi++) {
                int localIdx = gi - bankScroll;
                int col = localIdx % BANK_COLS;
                int row = localIdx / BANK_COLS;
                int x = startX + col * (BANK_CELL + BANK_GAP);
                int y = startY + row * (BANK_CELL + BANK_GAP);
                if (inRect(mx, my, x, y, ICON + 4, ICON + 4)) {
                    draggingInst = filtered.get(gi).getKey();
                    draggingFromPage = false;
                    return true;
                }
            }
        }

        int startY = leftPageY + 70;
        int startX = leftPageX + 15;
        for (int i = 0; i < Math.min(page.slotCount, MAX_SLOTS_PER_PAGE); i++) {
            int y = startY + i * (SLOT + SLOT_GAP);
            if (inRect(mx, my, startX, y, SLOT, SLOT)) {
                if (button == 1) {
                    if (page.slots[i] != null) {
                        PacketDistributor.sendToServer(GrimoireActionPacket.removeGlyph(itemSlotId, active, i));
                    }
                } else {
                    if (page.slots[i] != null) {
                        selectedSlot = (selectedSlot == i) ? -1 : i;
                        draggingInst = page.slots[i];
                        draggingFromPage = true;
                    } else {
                        selectedSlot = -1;
                    }
                }
                return true;
            }
        }

        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        Snapshot snap = snapshot();
        if (snap == null) return super.mouseScrolled(mx, my, dx, dy);
        GlyphPage activePage = snap.activePage();
        if (activePage == null || activePage.isToc() || snap.bank() == null) {
            return super.mouseScrolled(mx, my, dx, dy);
        }
        if (!inRect(mx, my, rightPageX, rightPageY + 38, PAGE_W, PAGE_H - 38)) {
            return super.mouseScrolled(mx, my, dx, dy);
        }
        int bankAreaH = PAGE_H - 38 - 6;
        int maxVisible = BANK_COLS * (bankAreaH / (BANK_CELL + BANK_GAP));
        int visible = 0;
        for (var e : snap.bank().entrySet()) {
            Glyph g = Glyphs.byId(e.getKey().typeId());
            if (g == null) continue;
            if (selectedBankTab != null && g.category() != selectedBankTab) continue;
            visible++;
        }
        int maxScroll = Math.max(0, visible - maxVisible);
        bankScroll = (int)Math.max(0, Math.min(maxScroll, bankScroll - Math.signum(dy)));
        return true;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (tocDragFrom >= 0) {
            Snapshot snap = snapshot();
            if (snap != null) updateTocDrop(mx, my, snap);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (tocDragFrom >= 0) {
            int from = tocDragFrom;
            int target = tocDropTarget;
            boolean after = tocDropAfter;
            boolean moved = tocDragMoved;
            resetTocDrag();
            if (!moved) {
                PacketDistributor.sendToServer(GrimoireActionPacket.setPage(itemSlotId, from));
                selectedSlot = -1;
            } else if (target >= 0) {
                int to = computeToPage(from, target, after);
                if (to != from) {
                    PacketDistributor.sendToServer(GrimoireActionPacket.movePage(itemSlotId, from, to));
                }
            }
            return true;
        }

        if (draggingInst != null) {
            GlyphInstance drag = draggingInst;
            draggingInst = null;
            Snapshot snap = snapshot();
            if (snap != null) {
                int active = snap.active();
                GlyphPage page = snap.activePage();
                if (page != null && !page.isToc()) {
                    int dropSlot = slotUnderCursor(mx, my, page);
                    if (draggingFromPage) {
                        if (dropSlot < 0) {
                            PacketDistributor.sendToServer(GrimoireActionPacket.removeGlyph(itemSlotId, active, selectedSlot));
                            selectedSlot = -1;
                        } else if (dropSlot != selectedSlot && page.slots[dropSlot] == null) {
                            PacketDistributor.sendToServer(GrimoireActionPacket.moveGlyph(itemSlotId, active, selectedSlot, dropSlot));
                            selectedSlot = -1;
                        }
                    } else if (dropSlot >= 0 && page.slots[dropSlot] == null) {
                        PacketDistributor.sendToServer(
                                GrimoireActionPacket.placeGlyph(itemSlotId, active, dropSlot, drag.typeId(), drag.allocations()));
                    } else if (selectedSlot >= 0 && selectedSlot < page.slotCount && page.slots[selectedSlot] == null) {
                        PacketDistributor.sendToServer(
                                GrimoireActionPacket.placeGlyph(itemSlotId, active, selectedSlot, drag.typeId(), drag.allocations()));
                        selectedSlot = -1;
                    }
                }
            }
            draggingFromPage = false;
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (nameField != null && nameField.isFocused()) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                Snapshot snap = snapshot();
                if (snap != null) sendName(snap.active(), snap.activePage());
                nameField.setFocused(false);
                return true;
            }
            if (nameField.keyPressed(keyCode, scanCode, modifiers)) return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (tocDragFrom >= 0) { resetTocDrag(); return true; }
            if (draggingInst != null) { draggingInst = null; return true; }
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char ch, int modifiers) {
        if (nameField != null && nameField.isFocused() && nameField.charTyped(ch, modifiers)) return true;
        return super.charTyped(ch, modifiers);
    }

    @Override
    public void onClose() {
        resetTocDrag();
        draggingInst = null;
        Snapshot snap = snapshot();
        if (snap != null) sendName(snap.active(), snap.activePage());
        super.onClose();
    }

    private void sendName(int active, GlyphPage page) {
        if (nameField == null || page == null || !page.isSpell()) return;
        String name = nameField.getValue();
        String old = page.name == null ? "" : page.name;
        if (!name.equals(old)) {
            PacketDistributor.sendToServer(GrimoireActionPacket.setName(itemSlotId, active, name));
        }
    }

    private static boolean inRect(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static int brighten(int color) {
        int a = (color >> 24) & 0xFF;
        int r = Math.min(255, ((color >> 16) & 0xFF) + 30);
        int g = Math.min(255, ((color >>  8) & 0xFF) + 30);
        int b = Math.min(255, ( color        & 0xFF) + 30);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private void updateTocDrop(double mx, double my, Snapshot snap) {
        int count = snap.pageCount();
        int rowY = leftPageY + TOC_TOP;
        int found = -1;
        boolean after = false;

        for (int i = 0; i < count; i++) {
            GlyphPage p = snap.page(i);
            if (p == null || p.isToc()) continue;
            int top = rowY;
            int bottom = rowY + TOC_ROW_H - 2;
            if (my >= top && my < bottom) {
                found = i;
                after = my >= (top + bottom) / 2.0;
                break;
            }
            rowY += TOC_ROW_H;
        }

        if (found < 0) {
            if (my < leftPageY + TOC_TOP) {
                for (int i = 0; i < count; i++) {
                    GlyphPage p = snap.page(i);
                    if (p != null && !p.isToc()) { found = i; after = false; break; }
                }
            } else {
                for (int i = count - 1; i >= 0; i--) {
                    GlyphPage p = snap.page(i);
                    if (p != null && !p.isToc()) { found = i; after = true; break; }
                }
            }
        }

        tocDropTarget = found;
        tocDropAfter = after;
        if (found != -1 && found != tocDragFrom) tocDragMoved = true;
    }

    private int computeToPage(int from, int tgt, boolean after) {
        if (tgt == from) return from;
        if (after) return (tgt > from) ? tgt : tgt + 1;
        else       return (tgt > from) ? tgt - 1 : tgt;
    }

    private void resetTocDrag() {
        tocDragFrom = -1;
        tocDropTarget = -1;
        tocDropAfter = false;
        tocDragMoved = false;
    }
}
