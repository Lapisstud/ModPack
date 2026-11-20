package com.example.grimoiremod.glyphs;

import com.example.grimoiremod.data.GlyphInstance;
import com.example.grimoiremod.data.GlyphPage;
import com.example.grimoiremod.data.SpellParser;

import java.util.EnumMap;
import java.util.List;

/**
 * Единственный источник правды по урону/лечению.
 * Формовые статы (через StatBlock) и поэлементные (через elementBase*).
 */
public final class SpellAggregator {
    private static final int MAX_SPLIT = 2;
    private SpellAggregator() {}

    public static StatBlock aggregate(GlyphPage page) {
        if (page == null || page.isToc()) return StatBlock.defaults();

        SpellParser.Parsed parsed = SpellParser.parse(page);
        if (parsed.formId() == null) return StatBlock.defaults();
        Glyph form = Glyphs.byId(parsed.formId());
        if (form == null) return StatBlock.defaults();

        int splitCount = 0, focusCount = 0, spreadCount = 0;
        for (String modId : parsed.formMods()) {
            switch (modId) {
                case "split"  -> splitCount++;
                case "focus"  -> focusCount++;
                case "spread" -> spreadCount++;
            }
        }
        splitCount = Math.min(splitCount, MAX_SPLIT);
        if (focusCount > 0 && spreadCount > 0) { focusCount = 0; spreadCount = 0; }

        EnumMap<Stat, Double> stats = new EnumMap<>(Stat.class);
        for (Stat s : Stat.values()) stats.put(s, s.defaultValue);

        applyBase(stats, form.stats());
        applyModifier(stats, "split",  splitCount);
        applyModifier(stats, "focus",  focusCount);
        applyModifier(stats, "spread", spreadCount);

        // Применяем вложенные очки характеристик от глифа формы
        GlyphInstance formInst = null;
        for (GlyphInstance inst : page.slots) {
            if (inst != null && inst.typeId().equals(parsed.formId())) {
                formInst = inst;
                break;
            }
        }
        
        if (formInst != null) {
            // Перезарядка: каждое очко уменьшает время на 0.05 сек
            int cdPoints = formInst.getPoints(Stat.COOLDOWN);
            if (cdPoints > 0) {
                double baseCd = stats.get(Stat.COOLDOWN);
                stats.put(Stat.COOLDOWN, Math.max(0.1, baseCd - cdPoints * 0.05));
            }
            // Сюда в будущем можно добавить другие статы формы, если нужно
        }

        return new StatBlock(stats);
    }

    private static void applyBase(EnumMap<Stat, Double> stats, List<StatMod> mods) {
        for (StatMod m : mods) {
            if (m.phase() == Phase.BASE) stats.put(m.stat(), m.value());
        }
    }

    private static void applyModifier(EnumMap<Stat, Double> stats, String glyphId, int count) {
        if (count <= 0) return;
        Glyph g = Glyphs.byId(glyphId);
        if (g == null) return;
        for (int i = 0; i < count; i++) {
            for (StatMod m : g.stats()) {
                if (m.phase() == Phase.ADD) {
                    stats.merge(m.stat(), m.value(), Double::sum);
                } else if (m.phase() == Phase.MULT) {
                    stats.merge(m.stat(), m.value(), (a, b) -> a * b);
                }
            }
        }
    }

    /** Сумма ADD DAMAGE от модификаторов элемента (generic, не только bigger). */
    public static double elementModDamageBonus(SpellParser.Element e) {
        double bonus = 0;
        for (String m : e.mods()) {
            Glyph mg = Glyphs.byId(m);
            if (mg == null) continue;
            for (StatMod sm : mg.stats()) {
                if (sm.stat() == Stat.DAMAGE && sm.phase() == Phase.ADD) bonus += sm.value();
            }
        }
        return bonus;
    }

    /** Итоговый урон элемента ДО крита: base + моды + поинты + DAMAGE_BONUS формы. */
    public static double elementBaseDamage(SpellParser.Element e, StatBlock formStats) {
        Glyph g = Glyphs.byId(e.instance().typeId());
        if (g == null) return 0;
        return g.baseStat(Stat.DAMAGE)
             + elementModDamageBonus(e)
             + e.instance().getPoints(Stat.DAMAGE)
             + formStats.get(Stat.DAMAGE_BONUS);
    }

    /** Итоговое лечение элемента ДО крита. */
    public static double elementBaseHeal(SpellParser.Element e, StatBlock formStats) {
        Glyph g = Glyphs.byId(e.instance().typeId());
        if (g == null) return 0;
        return g.baseStat(Stat.HEAL)
             + e.instance().getPoints(Stat.HEAL)
             + formStats.get(Stat.DAMAGE_BONUS);
    }
}
