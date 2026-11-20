package com.example.grimoiremod.glyphs;

/**
 * Фаза модификатора стата.
 * BASE — задаёт базовое значение (перезаписывает default).
 * ADD  — прибавляется к текущему (суммируются все ADD).
 * MULT — умножается на текущее (перемножаются все MULT).
 *
 * Порядок применения: BASE → сумма ADD → произведение MULT.
 */
public enum Phase {
    BASE, ADD, MULT
}
