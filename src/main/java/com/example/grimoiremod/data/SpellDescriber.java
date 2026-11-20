package com.example.grimoiremod.data;

import com.example.grimoiremod.glyphs.Glyph;
import com.example.grimoiremod.glyphs.Glyphs;
import com.example.grimoiremod.glyphs.SpellAggregator;
import com.example.grimoiremod.glyphs.Stat;
import com.example.grimoiremod.glyphs.StatBlock;
import com.example.grimoiremod.glyphs.StatMod;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class SpellDescriber {
    private SpellDescriber() {}

    public static List<Component> describeLines(GlyphPage page) {
        List<Component> out = new ArrayList<>();
        if (page == null) return out;
        if (page.isToc()) {
            out.add(Component.translatable("gui.grimoiremod.toc").withStyle(ChatFormatting.BOLD));
            return out;
        }

        SpellParser.Parsed parsed = SpellParser.parse(page);
        if (parsed.formId() == null) {
            out.add(Component.translatable("gui.grimoiremod.no_form").withStyle(ChatFormatting.RED));
            if (parsed.orphanModifier()) {
                out.add(Component.translatable("gui.grimoiremod.orphan_modifier").withStyle(ChatFormatting.RED));
            }
        } else {
            StatBlock sb = SpellAggregator.aggregate(page);

            for (SpellParser.Element e : parsed.elements()) {
                Glyph g = Glyphs.byId(e.instance().typeId());
                String name = g != null ? g.getDisplayName().getString() : e.instance().typeId();
                if (g != null && g.advanced()) {
                    name = Component.translatable("gui.grimoiremod.advanced", name).getString();
                }

                double baseDmg  = g != null ? g.baseStat(Stat.DAMAGE) : 0;
                double baseHeal = g != null ? g.baseStat(Stat.HEAL)   : 0;

                if (baseDmg > 0) {
                    double total = SpellAggregator.elementBaseDamage(e, sb);
                    ChatFormatting sign = total > 0 ? ChatFormatting.GREEN : (total < 0 ? ChatFormatting.RED : ChatFormatting.WHITE);
                    int col = g.color() & 0xFFFFFF;
                    out.add(Component.literal(name + ": ")
                            .withStyle(style -> style.withColor(col))
                            .append(Component.translatable("gui.grimoiremod.damage", Stat.trimNumber(total)).withStyle(sign)));
                } else if (baseHeal > 0) {
                    double total = SpellAggregator.elementBaseHeal(e, sb);
                    int col = g.color() & 0xFFFFFF;
                    out.add(Component.literal(name + ": ")
                            .withStyle(style -> style.withColor(col))
                            .append(Component.translatable("gui.grimoiremod.heal", Stat.trimNumber(total)).withStyle(ChatFormatting.GREEN)));
                } else {
                    int col = g != null ? (g.color() & 0xFFFFFF) : 0xFFFFFF;
                    out.add(Component.literal(name).withStyle(style -> style.withColor(col)));
                }
            }

            if (!parsed.elements().isEmpty()) out.add(Component.literal(" "));
            out.add(Component.literal("---").withStyle(ChatFormatting.DARK_GRAY));

            Glyph form = Glyphs.byId(parsed.formId());
            String formName = form != null ? form.getDisplayName().getString() : parsed.formId();
            int count = sb.getInt(Stat.COUNT);
            out.add(Component.translatable("gui.grimoiremod.form", formName + (count > 1 ? " x" + count : ""))
                    .withStyle(ChatFormatting.BOLD));

            if (parsed.formId().equals("projectile")) {
                double critChance = sb.get(Stat.CRIT_CHANCE);
                if (critChance > 0) {
                    out.add(Component.translatable("gui.grimoiremod.crit_chance", (int)Math.round(critChance * 100))
                            .withStyle(ChatFormatting.GOLD));
                }
                double critMult = sb.get(Stat.CRIT_MULT);
                if (critMult > 0) {
                    out.add(Component.translatable("gui.grimoiremod.crit_damage", (int)Math.round(critMult * 100))
                            .withStyle(ChatFormatting.GOLD));
                }
            }

            double cooldown = sb.get(Stat.COOLDOWN);
            if (cooldown > 0) {
                out.add(Component.translatable("gui.grimoiremod.cooldown", Stat.trimNumber(cooldown))
                        .withStyle(ChatFormatting.GRAY));
            }
        }

        if (!parsed.errors().isEmpty()) {
            out.add(Component.literal("---").withStyle(ChatFormatting.DARK_GRAY));
            out.add(Component.translatable("gui.grimoiremod.errors").withStyle(ChatFormatting.RED));
            for (var e : parsed.errors().entrySet()) {
                out.add(Component.literal(" . ").append(Component.translatable(e.getValue())).withStyle(ChatFormatting.RED));
            }
        }

        return out;
    }
}
