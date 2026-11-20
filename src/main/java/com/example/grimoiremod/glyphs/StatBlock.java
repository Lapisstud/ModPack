package com.example.grimoiremod.glyphs;

import java.util.EnumMap;

/** Результат агрегации. Читается одним способом везде: в бою, в тултипах, в Итоге. */
public final class StatBlock {
    private final EnumMap<Stat, Double> values;

    public StatBlock(EnumMap<Stat, Double> values) { this.values = values; }

    public static StatBlock defaults() {
        EnumMap<Stat, Double> m = new EnumMap<>(Stat.class);
        for (Stat s : Stat.values()) m.put(s, s.defaultValue);
        return new StatBlock(m);
    }

    public double get(Stat s) { return values.getOrDefault(s, s.defaultValue); }
    public int    getInt(Stat s)   { return (int)Math.round(get(s)); }
    public float  getFloat(Stat s) { return (float)get(s); }
}
