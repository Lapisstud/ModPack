package com.example.grimoiremod.glyphs;

import com.example.grimoiremod.GrimoireMod;
import com.example.grimoiremod.data.SpellParser;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class GrimoireEffects {
    private GrimoireEffects() {}

    public static void apply(SpellParser.Element element, StatBlock formStats,
                             LivingEntity target, float critFactor, Entity caster, Entity directEntity) {
        if (element == null || formStats == null || target == null) return;
        String id = element.instance().typeId();
        Glyph g = Glyphs.byId(id);
        if (g == null) return;

        // СТАВИМ NBT МЕТКУ (чтобы отследить убийство даже после горения)
        if (caster != null) {
            target.getPersistentData().putString("GrimoireLastElement", id);
            target.getPersistentData().putUUID("GrimoireLastOwner", caster.getUUID());
        }

        int biggerCount = 0;
        for (String m : element.mods()) if ("bigger".equals(m)) biggerCount++;

        double baseDamage = SpellAggregator.elementBaseDamage(element, formStats);
        double baseHeal   = SpellAggregator.elementBaseHeal(element, formStats);
        int durationTicks = (int)Math.round(g.baseStat(Stat.DURATION) * 20.0);

        float finalDamage = (float)(baseDamage * critFactor);
        float finalHeal   = (float)(baseHeal   * critFactor);

        DamageSource ds = caster != null 
                ? target.damageSources().indirectMagic(caster, directEntity != null ? directEntity : caster)
                : target.damageSources().magic();

        switch (id) {
            case "fire", "fireball" -> {
                if (finalDamage > 0) target.hurt(ds, finalDamage);
                target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), durationTicks));
            }
            case "ice" -> {
                if (finalDamage > 0) target.hurt(ds, finalDamage);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, durationTicks, 0));
            }
            case "water" -> {
                if (finalDamage > 0) target.hurt(ds, finalDamage);
                target.setRemainingFireTicks(0);
                target.addEffect(new MobEffectInstance(GrimoireMod.WET, durationTicks, 0));
                if (target.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.SPLASH,
                            target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                            12, 0.3, 0.3, 0.3, 0.05);
                }
            }
            case "heal" -> {
                if (finalHeal > 0) target.heal(finalHeal);
                if (target.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.HEART,
                            target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                            4, 0.3, 0.3, 0.3, 0.05);
                }
            }
            case "speed" -> target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, durationTicks, Math.max(0, biggerCount)));
            case "slow" -> target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, durationTicks, Math.max(0, biggerCount)));
        }
    }
}
