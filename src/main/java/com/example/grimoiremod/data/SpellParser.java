package com.example.grimoiremod.data;

import com.example.grimoiremod.glyphs.Glyph;
import com.example.grimoiremod.glyphs.GlyphCategory;
import com.example.grimoiremod.glyphs.Glyphs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SpellParser {
    private SpellParser() {}

    public record Element(GlyphInstance instance, List<String> mods) {}

    public record Parsed(String formId, List<String> formMods, List<Element> elements,
                         boolean orphanModifier, Map<Integer, String> errors) {}

    public static Parsed parse(GlyphPage page) {
        if (page == null || page.isToc()) {
            return new Parsed(null, List.of(), List.of(), false, new HashMap<>());
        }

        String formId = null;
        List<String> formMods = new ArrayList<>();
        List<Element> elements = new ArrayList<>();
        boolean orphanModifier = false;
        List<String> activeAnchorMods = null;
        Map<Integer, String> errors = new HashMap<>();

        for (int i = 0; i < page.slots.length; i++) {
            GlyphInstance inst = page.slots[i];
            if (inst == null) continue;
            Glyph g = Glyphs.byId(inst.typeId());
            if (g == null) continue;

            if (g.category() == GlyphCategory.FORM) {
                if (formId == null) {
                    formId = g.id();
                    activeAnchorMods = formMods;
                    if (g.id().equals("fireball")) {
                        Element e = new Element(inst, new ArrayList<>());
                        elements.add(e);
                        activeAnchorMods = e.mods();
                    }
                } else {
                    errors.put(i, "gui.grimoiremod.error.multiple_forms");
                }
            } else if (g.category() == GlyphCategory.ELEMENT) {
                boolean hasElement = false;
                for (Element e : elements) {
                    Glyph eg = Glyphs.byId(e.instance().typeId());
                    if (eg != null && eg.category() == GlyphCategory.ELEMENT) { hasElement = true; break; }
                }
                if (!hasElement) {
                    Element e = new Element(inst, new ArrayList<>());
                    elements.add(e);
                    activeAnchorMods = e.mods();
                } else {
                    errors.put(i, "gui.grimoiremod.error.multiple_elements");
                }
            } else if (g.category() == GlyphCategory.EFFECT) {
                boolean duplicate = false;
                for (Element e : elements) {
                    if (e.instance().typeId().equals(g.id())) { duplicate = true; break; }
                }
                if (!duplicate) {
                    Element e = new Element(inst, new ArrayList<>());
                    elements.add(e);
                    activeAnchorMods = e.mods();
                } else {
                    errors.put(i, "gui.grimoiremod.error.duplicate_effect");
                }
            } else if (g.category() == GlyphCategory.FORM_MODIFIER) {
                formMods.add(g.id());
            } else if (g.category() == GlyphCategory.MODIFIER) {
                if (activeAnchorMods != null) activeAnchorMods.add(g.id());
                else {
                    orphanModifier = true;
                    errors.put(i, "gui.grimoiremod.error.orphan_modifier");
                }
            }
        }

        if (formId == null && page.slots.length > 0) {
            boolean hasAnyGlyph = false;
            for (GlyphInstance inst : page.slots) if (inst != null) hasAnyGlyph = true;
            if (hasAnyGlyph) errors.put(-1, "gui.grimoiremod.error.no_form");
        }

        return new Parsed(formId, formMods, elements, orphanModifier, errors);
    }
}
