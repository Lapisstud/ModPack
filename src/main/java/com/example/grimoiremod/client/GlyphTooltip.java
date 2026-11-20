package com.example.grimoiremod.client;

import com.example.grimoiremod.data.GlyphInstance;
import com.example.grimoiremod.glyphs.Glyph;
import com.example.grimoiremod.glyphs.Glyphs;
import com.example.grimoiremod.glyphs.Phase;
import com.example.grimoiremod.glyphs.Stat;
import com.example.grimoiremod.glyphs.StatMod;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class GlyphTooltip {
    private GlyphTooltip() {}

    public static List<Component> build(GlyphInstance inst, String errKey) {
        List<Component> tooltip = new ArrayList<>();
        if (inst == null) return tooltip;
        Glyph g = Glyphs.byId(inst.typeId());
        if (g == null) return tooltip;

        tooltip.add(Component.literal(". ").withStyle(s -> s.withColor(g.color() & 0xFFFFFF))
                .append(g.getDisplayName().copy().withStyle(ChatFormatting.WHITE)));
        if (inst.totalPoints() > 0) {
            tooltip.add(Component.translatable("gui.grimoiremod.points_spent", inst.totalPoints())
                    .withStyle(ChatFormatting.GOLD));
        }
        
        for (StatMod sm : g.stats()) {
            if (sm.phase() == Phase.BASE) {
                double val = sm.value();
                int pts = inst.getPoints(sm.stat());
                
                // Применяем вложенные очки
                if (sm.stat() == Stat.COOLDOWN) {
                    val = Math.max(0.1, val - pts * 0.05);
                } else {
                    val += pts;
                }
                
                tooltip.add(Component.literal("  ").append(
                        Component.empty()
                                .append(sm.stat().getDisplayName())
                                .append(": " + sm.stat().format(val, sm.phase()))
                ).withStyle(ChatFormatting.GRAY));
            } else {
                tooltip.add(Component.literal("  ").append(sm.tooltip()).withStyle(ChatFormatting.GRAY));
            }
        }
        
        if (g.stats().isEmpty()) {
            tooltip.add(Component.translatable("gui.grimoiremod.no_stats").withStyle(ChatFormatting.DARK_GRAY));
        }
        if (errKey != null) {
            tooltip.add(Component.literal("  ").append(Component.translatable(errKey))
                    .withStyle(ChatFormatting.RED));
        }
        return tooltip;
    }
}
