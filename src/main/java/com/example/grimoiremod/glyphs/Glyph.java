package com.example.grimoiremod.glyphs;

import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Set;

public record Glyph(
        String id,
        GlyphCategory category,
        int color,
        List<StatMod> stats,
        boolean advanced,
        Set<Stat> upgradeableStats
) {
    public Glyph(String id, GlyphCategory category, int color) {
        this(id, category, color, List.of(), false, Set.of());
    }
    public Glyph(String id, GlyphCategory category, int color, List<StatMod> stats) {
        this(id, category, color, stats, false, Set.of());
    }
    public Glyph(String id, GlyphCategory category, int color, List<StatMod> stats, boolean advanced) {
        this(id, category, color, stats, advanced, Set.of());
    }

    public Component getDisplayName() {
        return Component.translatable("glyph.grimoiremod." + id);
    }

    public double baseStat(Stat stat) {
        for (StatMod m : stats) {
            if (m.stat() == stat && m.phase() == Phase.BASE) return m.value();
        }
        return stat.defaultValue;
    }

    public boolean canUpgrade(Stat stat) {
        return upgradeableStats.contains(stat);
    }
}
