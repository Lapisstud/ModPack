package com.example.grimoiremod.glyphs;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class Glyphs {
    private static final Map<String, Glyph> BY_ID = new LinkedHashMap<>();

    public static final Glyph PROJECTILE = reg(new Glyph("projectile", GlyphCategory.FORM, 0xFFCC4444, GlyphBalance.PROJECTILE, false, Set.of(Stat.COOLDOWN)));
    public static final Glyph SELF       = reg(new Glyph("self",       GlyphCategory.FORM, 0xFF4444CC, GlyphBalance.SELF, false, Set.of(Stat.COOLDOWN)));
    public static final Glyph TOUCH      = reg(new Glyph("touch",      GlyphCategory.FORM, 0xFF44CC44, GlyphBalance.TOUCH, false, Set.of(Stat.COOLDOWN)));
    
    public static final Glyph FIRE       = reg(new Glyph("fire",       GlyphCategory.ELEMENT, 0xFFFFA500, GlyphBalance.FIRE, false, Set.of(Stat.DAMAGE)));
    public static final Glyph ICE        = reg(new Glyph("ice",        GlyphCategory.ELEMENT, 0xFF6AD4FF, GlyphBalance.ICE, false, Set.of(Stat.DAMAGE)));
    public static final Glyph WATER      = reg(new Glyph("water",      GlyphCategory.ELEMENT, 0xFF3A9AE8, GlyphBalance.WATER, false, Set.of(Stat.DAMAGE)));
    
    public static final Glyph HEAL       = reg(new Glyph("heal",       GlyphCategory.EFFECT, 0xFF66DD66, GlyphBalance.HEAL, false, Set.of(Stat.HEAL)));
    public static final Glyph SPEED      = reg(new Glyph("speed",      GlyphCategory.EFFECT, 0xFF55AA88, GlyphBalance.SPEED, false, Set.of()));
    public static final Glyph SLOW       = reg(new Glyph("slow",       GlyphCategory.EFFECT, 0xFF5588AA, GlyphBalance.SLOW, false, Set.of()));
    
    public static final Glyph BIGGER     = reg(new Glyph("bigger",     GlyphCategory.MODIFIER, 0xFFAA44AA, GlyphBalance.BIGGER));
    public static final Glyph SPLIT      = reg(new Glyph("split",      GlyphCategory.FORM_MODIFIER, 0xFFAA8844, GlyphBalance.SPLIT));
    public static final Glyph FOCUS      = reg(new Glyph("focus",      GlyphCategory.FORM_MODIFIER, 0xFF4477CC, GlyphBalance.FOCUS));
    public static final Glyph SPREAD     = reg(new Glyph("spread",     GlyphCategory.FORM_MODIFIER, 0xFFCC7733, GlyphBalance.SPREAD));
    
    public static final Glyph FIREBALL   = reg(new Glyph("fireball",   GlyphCategory.FORM, 0xFFFF4500, GlyphBalance.FIREBALL, true, Set.of(Stat.DAMAGE)));

    private static Glyph reg(Glyph g) { BY_ID.put(g.id(), g); return g; }
    private Glyphs() {}

    public static Glyph byId(String id) { return id == null ? null : BY_ID.get(id); }
    public static Collection<Glyph> all() { return Collections.unmodifiableCollection(BY_ID.values()); }
}
