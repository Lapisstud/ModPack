package com.example.grimoiremod.glyphs;

import net.minecraft.network.chat.Component;

public record StatMod(Stat stat, Phase phase, double value) {

    public static StatMod base(Stat s, double v) { return new StatMod(s, Phase.BASE, v); }
    public static StatMod add(Stat s, double v)  { return new StatMod(s, Phase.ADD,  v); }
    public static StatMod mult(Stat s, double v) { return new StatMod(s, Phase.MULT, v); }

    /** Строка для тултипа: "Урон: 4", "Крит шанс: +12%", "Угол: ×0.3". */
    public Component tooltip() {
        return Component.empty()
                .append(stat.getDisplayName())
                .append(": " + stat.format(value, phase));
    }
}
