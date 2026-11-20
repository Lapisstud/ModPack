package com.example.grimoiremod.data;

import net.minecraft.nbt.*;

public final class GlyphPage {
    // bump при изменении формата GlyphPage; см. GrimoireData.readRoot
    public static final int VERSION = 12;

    public enum Kind { SPELL, TOC }

    public Kind kind;
    public int slotCount;
    public GlyphInstance[] slots;
    public String name;
    public int bookmarkColor;
    public boolean favorite;

    public GlyphPage(int slotCount, int bookmarkColor) {
        this(Kind.SPELL, slotCount, bookmarkColor);
    }

    public GlyphPage(Kind kind, int slotCount, int bookmarkColor) {
        this.kind = kind;
        this.slotCount = (kind == Kind.TOC) ? 0 : Math.max(1, slotCount);
        this.slots = new GlyphInstance[this.slotCount];
        this.name = "";
        this.bookmarkColor = bookmarkColor;
        this.favorite = (kind == Kind.TOC); // TOC всегда избранное
    }

    public static GlyphPage toc(int bookmarkColor) {
        return new GlyphPage(Kind.TOC, 0, bookmarkColor);
    }

    public boolean isSpell() { return kind == Kind.SPELL; }
    public boolean isToc()   { return kind == Kind.TOC; }

    public CompoundTag toNbt() {
        CompoundTag t = new CompoundTag();
        t.putString("Kind", kind.name());
        t.putInt("SlotCount", slotCount);
        ListTag list = new ListTag();
        for (GlyphInstance inst : slots) list.add(inst == null ? new CompoundTag() : inst.toNbt());
        t.put("Slots", list);
        t.putString("Name", name == null ? "" : name);
        t.putInt("Color", bookmarkColor);
        t.putBoolean("Favorite", favorite);
        return t;
    }

    public static GlyphPage fromNbt(CompoundTag t) {
        Kind kind = Kind.TOC.name().equals(t.getString("Kind")) ? Kind.TOC : Kind.SPELL;
        int n = t.getInt("SlotCount");
        if (kind == Kind.SPELL && n <= 0) n = 7;
        GlyphPage p = new GlyphPage(kind, n, t.getInt("Color"));
        p.name = t.getString("Name");
        p.favorite = (kind == Kind.TOC) || t.getBoolean("Favorite");

        if (t.contains("Slots")) {
            Tag slotsTag = t.get("Slots");
            if (slotsTag instanceof ListTag list) {
                for (int i = 0; i < p.slotCount && i < list.size(); i++) {
                    Tag elem = list.get(i);
                    if (elem instanceof StringTag sTag) {
                        if (!sTag.getAsString().isEmpty()) p.slots[i] = new GlyphInstance(sTag.getAsString());
                    } else if (elem instanceof CompoundTag cTag) {
                        if (!cTag.isEmpty() && cTag.contains("Type")) p.slots[i] = GlyphInstance.fromNbt(cTag);
                    }
                }
            }
        }
        return p;
    }
}
