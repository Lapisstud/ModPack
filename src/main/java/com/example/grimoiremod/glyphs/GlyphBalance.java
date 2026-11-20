package com.example.grimoiremod.glyphs;

import java.util.List;

import static com.example.grimoiremod.glyphs.Stat.*;
import static com.example.grimoiremod.glyphs.StatMod.*;

/**
 * === ТАБЛИЦА БАЛАНСА ===
 * Меняй числа здесь — больше нигде.
 *
 * BASE — задаёт базовое значение (у формы).
 * ADD  — прибавляется (например, split режет урон).
 * MULT — умножается (например, focus сужает угол).
 *
 * Все статы валидируются через Stat. Если нужен новый стат — добавь в Stat.
 */
public final class GlyphBalance {
    private GlyphBalance() {}

    // ============ ФОРМЫ ============
    public static final List<StatMod> PROJECTILE = List.of(
        base(FALLBACK_DAMAGE, 2),
        base(CRIT_CHANCE,    0.12),
        base(CRIT_MULT,      0.42),
        base(COOLDOWN,       1.0),
        base(COUNT,          1)
    );

    public static final List<StatMod> SELF = List.of(
        base(COOLDOWN, 1.0)
    );

    public static final List<StatMod> TOUCH = List.of(
        base(COOLDOWN, 1.0)
    );

    // ============ СТИХИИ ============
    public static final List<StatMod> FIRE = List.of(
        base(DAMAGE,   4),
        base(DURATION, 5.0)   // поджог, сек
    );

    public static final List<StatMod> ICE = List.of(
        base(DAMAGE,   2),
        base(DURATION, 10.0)  // замедление, сек
    );

    public static final List<StatMod> WATER = List.of(
        base(DAMAGE,   1),
        base(DURATION, 4.0)   // wet, сек
    );

    // ============ ЭФФЕКТЫ ============
    public static final List<StatMod> HEAL = List.of(
        base(Stat.HEAL, 4)
    );

    public static final List<StatMod> SPEED = List.of(
        base(DURATION, 10.0)
    );

    public static final List<StatMod> SLOW = List.of(
        base(DURATION, 10.0)
    );

    // ============ МОДИФИКАТОРЫ ЭЛЕМЕНТА ============
    public static final List<StatMod> BIGGER = List.of(
        add(DAMAGE, 2)
    );

    // ============ МОДИФИКАТОРЫ ФОРМЫ ============
    public static final List<StatMod> SPLIT = List.of(
        add(COUNT,        1),
        add(DAMAGE_BONUS, -2)
    );

    public static final List<StatMod> FOCUS = List.of(
        mult(ANGLE,       0.3),
        add(DAMAGE_BONUS, 1)
    );

    public static final List<StatMod> SPREAD = List.of(
        mult(ANGLE,       2.0),
        mult(LIFE,        0.1),
        add(DAMAGE_BONUS, 2)
    );

    // ============ РАЗВИТЫЕ ГЛИФЫ ============
    public static final List<StatMod> FIREBALL = List.of(
        base(FALLBACK_DAMAGE, 2),
        base(CRIT_CHANCE,    0.15),
        base(CRIT_MULT,      0.5),
        base(COOLDOWN,       1.2),
        base(COUNT,          1),
        base(DAMAGE,         6),
        base(DURATION,       5.0)
    );
}
