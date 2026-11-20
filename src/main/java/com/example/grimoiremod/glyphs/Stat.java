package com.example.grimoiremod.glyphs;

import net.minecraft.network.chat.Component;

public enum Stat {
    FALLBACK_DAMAGE(Kind.NUMBER,  2.0),
    CRIT_CHANCE    (Kind.PERCENT, 0.0),
    CRIT_MULT      (Kind.PERCENT, 0.0),
    COOLDOWN       (Kind.SECONDS, 1.0),
    COUNT          (Kind.INT,     1.0),
    ANGLE          (Kind.MULT,    1.0),
    LIFE           (Kind.MULT,    1.0),
    DAMAGE_BONUS   (Kind.NUMBER,  0.0),
    DAMAGE         (Kind.NUMBER,  0.0),
    HEAL           (Kind.NUMBER,  0.0),
    DURATION       (Kind.SECONDS, 0.0);

    public enum Kind { NUMBER, INT, PERCENT, SECONDS, MULT }

    public final Kind kind;
    public final double defaultValue;

    Stat(Kind kind, double defaultValue) {
        this.kind = kind;
        this.defaultValue = defaultValue;
    }

    public Component getDisplayName() {
        return Component.translatable("stat.grimoiremod." + name().toLowerCase());
    }

    public String format(double value, Phase phase) {
        String prefix = "";
        String body;
        switch (kind) {
            case NUMBER -> {
                if (phase == Phase.ADD && value >= 0) prefix = "+";
                body = trimNumber(value);
            }
            case INT -> {
                if (phase == Phase.ADD && value >= 0) prefix = "+";
                body = String.valueOf((int)Math.round(value));
            }
            case PERCENT -> {
                int pct = (int)Math.round(value * 100);
                if (phase == Phase.ADD && pct >= 0) prefix = "+";
                body = pct + "%";
            }
            case SECONDS -> {
                if (phase == Phase.ADD && value >= 0) prefix = "+";
                body = trimNumber(value) + "s";
            }
            case MULT -> {
                prefix = "x";
                body = trimNumber(value);
            }
            default -> body = String.valueOf(value);
        }
        return prefix + body;
    }

    // для целых — без хвоста, для дробных — до 2 знаков
    public static String trimNumber(double v) {
        if (v == Math.rint(v) && Math.abs(v) < 1e15) return String.valueOf((long)v);
        String s = String.format("%.2f", v);
        s = s.replaceAll("\\.?0+$", "");
        return s;
    }
}
