package com.example.grimoiremod.data;

import com.example.grimoiremod.glyphs.Glyph;
import com.example.grimoiremod.glyphs.Glyphs;
import com.example.grimoiremod.glyphs.Stat;
import com.example.grimoiremod.item.CreativeGrimoireItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class GrimoireData {
    private static final String ROOT = "Grimoire";
    private static final String VERSION_KEY = "Version";
    private static final String ACTIVE_PAGE = "ActivePage";
    private static final String PAGES = "Pages";
    private static final String BANK = "Bank";

    private static final int MAX_POINTS_PER_GLYPH = 1000;
    private static final int MAX_NAME_LEN = 32;

    private static final int[] BOOKMARK_PALETTE = {
            0xFFC9A64A, 0xFFCC4444, 0xFF44CC44, 0xFF4444CC, 0xFFAA44AA,
            0xFF44AAAA, 0xFFAAAA44, 0xFFCC7733, 0xFF7733CC, 0xFF33CC77
    };

    private GrimoireData() {}

    private static int paletteColor(int index) {
        return BOOKMARK_PALETTE[Math.floorMod(index, BOOKMARK_PALETTE.length)];
    }

    static CompoundTag copyRoot(ItemStack stack) {
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        return cd != null ? cd.copyTag() : new CompoundTag();
    }
    static void saveRoot(ItemStack stack, CompoundTag root) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }

    static CompoundTag readRoot(ItemStack stack) {
        CompoundTag root = copyRoot(stack);
        boolean needFresh;
        if (!root.contains(ROOT)) needFresh = true;
        else needFresh = root.getCompound(ROOT).getInt(VERSION_KEY) != GlyphPage.VERSION;
        if (needFresh) {
            CompoundTag fresh = (stack.getItem() instanceof CreativeGrimoireItem)
                    ? freshCreativeGrimoire() : freshGrimoire();
            if (root.contains(ROOT)) {
                tryMigrate(root.getCompound(ROOT), fresh);
            }
            root.put(ROOT, fresh);
            saveRoot(stack, root);
        }
        return root;
    }

    private static void tryMigrate(CompoundTag old, CompoundTag fresh) {
        try {
            if (old.contains(PAGES)) {
                ListTag oldPages = old.getList(PAGES, Tag.TAG_COMPOUND);
                ListTag newPages = new ListTag();
                newPages.add(GlyphPage.toc(paletteColor(0)).toNbt());
                int limit = Math.min(oldPages.size(), 30);
                for (int i = 0; i < limit; i++) {
                    try {
                        GlyphPage gp = GlyphPage.fromNbt(oldPages.getCompound(i));
                        if (gp.isToc()) continue;
                        newPages.add(gp.toNbt());
                    } catch (Exception ignored) {}
                }
                if (newPages.size() > 1) fresh.put(PAGES, newPages);
            }
            if (old.contains(BANK)) {
                ListTag oldBank = old.getList(BANK, Tag.TAG_COMPOUND);
                ListTag newBank = new ListTag();
                int limit = Math.min(oldBank.size(), 500);
                for (int i = 0; i < limit; i++) {
                    try {
                        GlyphInstance inst = GlyphInstance.fromNbt(oldBank.getCompound(i));
                        if (Glyphs.byId(inst.typeId()) != null) newBank.add(inst.toNbt());
                    } catch (Exception ignored) {}
                }
                if (!newBank.isEmpty()) fresh.put(BANK, newBank);
            }
            if (old.contains(SkillTreeData.KEY)) {
                fresh.put(SkillTreeData.KEY, old.getCompound(SkillTreeData.KEY).copy());
            }
            if (old.contains(ACTIVE_PAGE)) {
                fresh.putInt(ACTIVE_PAGE, old.getInt(ACTIVE_PAGE));
            }
        } catch (Exception ignored) {}
    }

    private static CompoundTag freshCreativeGrimoire() {
        CompoundTag g = new CompoundTag();
        g.putInt(VERSION_KEY, GlyphPage.VERSION);
        g.putInt(ACTIVE_PAGE, 1);
        ListTag pages = new ListTag();
        pages.add(GlyphPage.toc(paletteColor(0)).toNbt());
        for (int i = 0; i < 9; i++) {
            GlyphPage p = new GlyphPage(7, paletteColor(i + 1));
            if (i == 0) p.favorite = true;
            pages.add(p.toNbt());
        }
        g.put(PAGES, pages);
        ListTag bank = new ListTag();
        String[] all = {"projectile","self","touch","fire","ice","water","heal","speed","slow",
                        "bigger","split","focus","spread","fireball"};
        for (String id : all) for (int i = 0; i < 20; i++) bank.add(new GlyphInstance(id).toNbt());
        g.put(BANK, bank);
        g.put(SkillTreeData.KEY, SkillTreeData.fresh());
        return g;
    }

    private static CompoundTag freshGrimoire() {
        CompoundTag g = new CompoundTag();
        g.putInt(VERSION_KEY, GlyphPage.VERSION);
        g.putInt(ACTIVE_PAGE, 0);
        ListTag pages = new ListTag();
        pages.add(GlyphPage.toc(paletteColor(0)).toNbt());
        GlyphPage p1 = new GlyphPage(7, paletteColor(1));
        p1.favorite = true;
        pages.add(p1.toNbt());
        pages.add(new GlyphPage(7, paletteColor(2)).toNbt());
        g.put(PAGES, pages);
        ListTag bank = new ListTag();
        String[] defs = {"projectile","projectile","projectile","self","self","touch","touch",
                         "fire","fire","fire","ice","ice","ice","water","water","water",
                         "heal","heal","heal","speed","speed","speed","slow","slow","slow",
                         "bigger","bigger","bigger","split","split","focus","focus","spread","spread"};
        for (String d : defs) bank.add(new GlyphInstance(d).toNbt());
        g.put(BANK, bank);
        g.put(SkillTreeData.KEY, SkillTreeData.fresh());
        return g;
    }

    public static void debugGiveTestBook(ItemStack stack, String typeId, int points) {
        CompoundTag root = new CompoundTag();
        CompoundTag g = freshGrimoire();
        GlyphInstance testGlyph = new GlyphInstance(typeId);
        if (points > 0) testGlyph.allocations().put(Stat.DAMAGE, points);
        ListTag pages = g.getList(PAGES, Tag.TAG_COMPOUND);
        GlyphPage page1 = GlyphPage.fromNbt(pages.getCompound(1));
        page1.slots[0] = testGlyph;
        pages.set(1, page1.toNbt());
        g.put(PAGES, pages);
        root.put(ROOT, g);
        saveRoot(stack, root);
    }

    public static int getPageCount(ItemStack stack) {
        return readRoot(stack).getCompound(ROOT).getList(PAGES, Tag.TAG_COMPOUND).size();
    }
    public static int getActivePage(ItemStack stack) {
        CompoundTag g = readRoot(stack).getCompound(ROOT);
        int active = g.getInt(ACTIVE_PAGE);
        int count = g.getList(PAGES, Tag.TAG_COMPOUND).size();
        return Math.max(0, Math.min(count - 1, active));
    }
    public static GlyphPage getPage(ItemStack stack, int index) {
        ListTag pages = readRoot(stack).getCompound(ROOT).getList(PAGES, Tag.TAG_COMPOUND);
        if (index < 0 || index >= pages.size()) return null;
        return GlyphPage.fromNbt(pages.getCompound(index));
    }
    public static int spellNumber(ItemStack stack, int pageIdx) {
        ListTag pages = readRoot(stack).getCompound(ROOT).getList(PAGES, Tag.TAG_COMPOUND);
        int n = 0;
        for (int i = 0; i <= pageIdx && i < pages.size(); i++)
            if (!GlyphPage.fromNbt(pages.getCompound(i)).isToc()) n++;
        return n;
    }

    public static List<GlyphInstance> getBankList(ItemStack stack) {
        ListTag bank = readRoot(stack).getCompound(ROOT).getList(BANK, Tag.TAG_COMPOUND);
        List<GlyphInstance> out = new ArrayList<>();
        for (int i = 0; i < bank.size(); i++) out.add(GlyphInstance.fromNbt(bank.getCompound(i)));
        return out;
    }

    public static Map<GlyphInstance, Integer> getBankGrouped(ItemStack stack) {
        Map<GlyphInstance, Integer> grouped = new LinkedHashMap<>();
        for (GlyphInstance inst : getBankList(stack)) grouped.merge(inst, 1, Integer::sum);
        return grouped;
    }

    public static void addToBank(ItemStack stack, GlyphInstance inst) {
        CompoundTag root = readRoot(stack);
        CompoundTag g = root.getCompound(ROOT);
        ListTag bank = g.getList(BANK, Tag.TAG_COMPOUND);
        bank.add(inst.toNbt());
        g.put(BANK, bank);
        saveRoot(stack, root);
    }

    private static void withPage(ItemStack stack, int index, Consumer<GlyphPage> fn) {
        CompoundTag root = readRoot(stack);
        CompoundTag g = root.getCompound(ROOT);
        ListTag pages = g.getList(PAGES, Tag.TAG_COMPOUND);
        if (index < 0 || index >= pages.size()) return;
        GlyphPage page = GlyphPage.fromNbt(pages.getCompound(index));
        fn.accept(page);
        ListTag newList = new ListTag();
        for (int i = 0; i < pages.size(); i++)
            newList.add(i == index ? page.toNbt() : pages.getCompound(i).copy());
        g.put(PAGES, newList);
        saveRoot(stack, root);
    }

    public static void setActivePage(ItemStack stack, int page) {
        CompoundTag root = readRoot(stack);
        CompoundTag g = root.getCompound(ROOT);
        int count = g.getList(PAGES, Tag.TAG_COMPOUND).size();
        if (count == 0) return;
        g.putInt(ACTIVE_PAGE, ((page % count) + count) % count);
        saveRoot(stack, root);
    }

    public static void switchPage(ItemStack stack, boolean next) {
        int count = getPageCount(stack);
        if (count <= 1) return;
        int active = getActivePage(stack);
        int dir = next ? 1 : -1;
        for (int i = 0; i < count; i++) {
            active = ((active + dir) % count + count) % count;
            GlyphPage p = getPage(stack, active);
            if (p != null && !p.isToc()) { setActivePage(stack, active); return; }
        }
    }

    public static void setPageName(ItemStack stack, int idx, String name) {
        String safe = (name == null) ? "" : (name.length() > MAX_NAME_LEN ? name.substring(0, MAX_NAME_LEN) : name);
        withPage(stack, idx, page -> page.name = safe);
    }

    public static void setPageFavorite(ItemStack stack, int idx, boolean fav) {
        withPage(stack, idx, page -> {
            if (page.isToc()) return;
            page.favorite = fav;
        });
    }

    public static boolean placeGlyphWithAllocs(ItemStack stack, int pageIdx, int slot, String typeId, Map<Stat, Integer> allocs) {
        Glyph g = Glyphs.byId(typeId);
        if (g == null) return false;

        for (var entry : allocs.entrySet()) {
            int v = entry.getValue();
            if (v < 0 || v > MAX_POINTS_PER_GLYPH) return false;
        }

        CompoundTag root = readRoot(stack);
        CompoundTag gr = root.getCompound(ROOT);
        ListTag pages = gr.getList(PAGES, Tag.TAG_COMPOUND);
        if (pageIdx < 0 || pageIdx >= pages.size()) return false;
        GlyphPage page = GlyphPage.fromNbt(pages.getCompound(pageIdx));
        if (!page.isSpell() || slot < 0 || slot >= page.slotCount || page.slots[slot] != null) return false;

        ListTag bank = gr.getList(BANK, Tag.TAG_COMPOUND);
        GlyphInstance toPlace = null;
        for (int i = 0; i < bank.size(); i++) {
            GlyphInstance inst = GlyphInstance.fromNbt(bank.getCompound(i));
            if (inst.typeId().equals(typeId) && inst.allocations().equals(allocs)) {
                toPlace = inst;
                bank.remove(i);
                break;
            }
        }
        if (toPlace == null) return false;

        page.slots[slot] = toPlace;
        ListTag newList = new ListTag();
        for (int i = 0; i < pages.size(); i++)
            newList.add(i == pageIdx ? page.toNbt() : pages.getCompound(i).copy());
        gr.put(PAGES, newList);
        gr.put(BANK, bank);
        saveRoot(stack, root);
        return true;
    }

    public static boolean moveGlyph(ItemStack stack, int pageIdx, int fromSlot, int toSlot) {
        if (fromSlot == toSlot) return false;
        CompoundTag root = readRoot(stack);
        CompoundTag gr = root.getCompound(ROOT);
        ListTag pages = gr.getList(PAGES, Tag.TAG_COMPOUND);
        if (pageIdx < 0 || pageIdx >= pages.size()) return false;
        GlyphPage page = GlyphPage.fromNbt(pages.getCompound(pageIdx));
        if (!page.isSpell()) return false;
        if (fromSlot < 0 || fromSlot >= page.slotCount || toSlot < 0 || toSlot >= page.slotCount) return false;
        GlyphInstance from = page.slots[fromSlot];
        if (from == null) return false;
        if (page.slots[toSlot] != null) return false;

        page.slots[toSlot] = from;
        page.slots[fromSlot] = null;
        ListTag newList = new ListTag();
        for (int i = 0; i < pages.size(); i++)
            newList.add(i == pageIdx ? page.toNbt() : pages.getCompound(i).copy());
        gr.put(PAGES, newList);
        saveRoot(stack, root);
        return true;
    }

    public static boolean removeGlyph(ItemStack stack, int pageIdx, int slot) {
        CompoundTag root = readRoot(stack);
        CompoundTag gr = root.getCompound(ROOT);
        ListTag pages = gr.getList(PAGES, Tag.TAG_COMPOUND);
        if (pageIdx < 0 || pageIdx >= pages.size()) return false;
        GlyphPage page = GlyphPage.fromNbt(pages.getCompound(pageIdx));
        if (!page.isSpell() || slot < 0 || slot >= page.slotCount) return false;
        GlyphInstance removed = page.slots[slot];
        if (removed == null) return false;

        ListTag bank = gr.getList(BANK, Tag.TAG_COMPOUND);
        bank.add(removed.toNbt());

        page.slots[slot] = null;
        ListTag newList = new ListTag();
        for (int i = 0; i < pages.size(); i++)
            newList.add(i == pageIdx ? page.toNbt() : pages.getCompound(i).copy());
        gr.put(PAGES, newList);
        gr.put(BANK, bank);
        saveRoot(stack, root);
        return true;
    }

    public static void movePage(ItemStack stack, int from, int to) {
        CompoundTag root = readRoot(stack);
        CompoundTag g = root.getCompound(ROOT);
        ListTag pages = g.getList(PAGES, Tag.TAG_COMPOUND);
        if (from < 0 || from >= pages.size() || to < 0 || to >= pages.size() || from == to) return;
        if (GlyphPage.fromNbt(pages.getCompound(from)).isToc()) return;
        if (GlyphPage.fromNbt(pages.getCompound(to)).isToc()) return;

        List<CompoundTag> list = new ArrayList<>();
        for (int i = 0; i < pages.size(); i++) list.add(pages.getCompound(i).copy());
        CompoundTag moved = list.remove(from);
        list.add(to, moved);
        ListTag newList = new ListTag();
        for (CompoundTag c : list) newList.add(c);
        g.put(PAGES, newList);

        int active = g.getInt(ACTIVE_PAGE);
        if (active == from) active = to;
        else if (from < active && to >= active) active--;
        else if (from > active && to <= active) active++;
        g.putInt(ACTIVE_PAGE, active);
        saveRoot(stack, root);
    }

    // === СВЯЗЬ С ДЕРЕВОМ НАВЫКОВ ===
    public static int getStatBudget(ItemStack stack, Stat stat) {
        String nodeId = switch (stat) {
            case DAMAGE -> "grimoiremod:combat_damage";
            case HEAL -> "grimoiremod:support_heal";
            case COOLDOWN -> "grimoiremod:utility_cooldown";
            case ANGLE -> "grimoiremod:combat_radius";
            default -> null;
        };
        if (nodeId == null) return 0; // Если стат не прокачивается, бюджет 0
        return SkillTreeData.getNodeLevel(stack, nodeId);
    }

    public static int getSpentPoints(ItemStack stack, Stat stat) {
        CompoundTag gr = readRoot(stack).getCompound(ROOT);
        int spent = 0;
        ListTag pages = gr.getList(PAGES, Tag.TAG_COMPOUND);
        for (int i = 0; i < pages.size(); i++) {
            GlyphPage page = GlyphPage.fromNbt(pages.getCompound(i));
            if (page.isToc()) continue;
            for (GlyphInstance inst : page.slots) if (inst != null) spent += inst.getPoints(stat);
        }
        ListTag bank = gr.getList(BANK, Tag.TAG_COMPOUND);
        for (int i = 0; i < bank.size(); i++) spent += GlyphInstance.fromNbt(bank.getCompound(i)).getPoints(stat);
        return spent;
    }

    public static boolean canAllocate(ItemStack stack, Stat stat, int delta) {
        if (delta <= 0) return true;
        return getSpentPoints(stack, stat) + delta <= getStatBudget(stack, stat);
    }

    public static void allocatePoint(ItemStack stack, int pageIdx, int slotIdx, Stat stat, int delta) {
        if (delta > 0 && !canAllocate(stack, stat, delta)) return;
        withPage(stack, pageIdx, page -> {
            if (slotIdx < 0 || slotIdx >= page.slotCount) return;
            GlyphInstance inst = page.slots[slotIdx];
            if (inst == null) return;
            Glyph g = Glyphs.byId(inst.typeId());
            if (g != null && g.advanced()) return; // Развитые нельзя
            Map<Stat, Integer> newAllocs = new HashMap<>(inst.allocations());
            int current = newAllocs.getOrDefault(stat, 0);
            int next = Math.max(0, current + delta);
            if (next == current) return;
            if (next > MAX_POINTS_PER_GLYPH) return;
            if (next == 0) newAllocs.remove(stat); else newAllocs.put(stat, next);
            page.slots[slotIdx] = new GlyphInstance(inst.typeId(), newAllocs);
        });
    }

    public static void allocatePointToBank(ItemStack stack, int bankIndex, Stat stat, int delta) {
        if (delta > 0 && !canAllocate(stack, stat, delta)) return;
        CompoundTag root = readRoot(stack);
        CompoundTag gr = root.getCompound(ROOT);
        ListTag bank = gr.getList(BANK, Tag.TAG_COMPOUND);
        if (bankIndex < 0 || bankIndex >= bank.size()) return;

        GlyphInstance inst = GlyphInstance.fromNbt(bank.getCompound(bankIndex));
        Glyph g = Glyphs.byId(inst.typeId());
        if (g != null && g.advanced()) return;
        Map<Stat, Integer> newAllocs = new HashMap<>(inst.allocations());
        int current = newAllocs.getOrDefault(stat, 0);
        int next = Math.max(0, current + delta);
        if (next == current) return;
        if (next > MAX_POINTS_PER_GLYPH) return;
        if (next == 0) newAllocs.remove(stat); else newAllocs.put(stat, next);

        bank.set(bankIndex, new GlyphInstance(inst.typeId(), newAllocs).toNbt());
        gr.put(BANK, bank);
        saveRoot(stack, root);
    }

    public static void resetGlyph(ItemStack stack, int pageIdx, int slotIdx) {
        withPage(stack, pageIdx, page -> {
            if (slotIdx < 0 || slotIdx >= page.slotCount) return;
            GlyphInstance inst = page.slots[slotIdx];
            if (inst != null) page.slots[slotIdx] = new GlyphInstance(inst.typeId());
        });
    }

    public static void resetGlyphInBank(ItemStack stack, int bankIndex) {
        CompoundTag root = readRoot(stack);
        CompoundTag gr = root.getCompound(ROOT);
        ListTag bank = gr.getList(BANK, Tag.TAG_COMPOUND);
        if (bankIndex < 0 || bankIndex >= bank.size()) return;
        GlyphInstance inst = GlyphInstance.fromNbt(bank.getCompound(bankIndex));
        bank.set(bankIndex, new GlyphInstance(inst.typeId()).toNbt());
        gr.put(BANK, bank);
        saveRoot(stack, root);
    }
}
