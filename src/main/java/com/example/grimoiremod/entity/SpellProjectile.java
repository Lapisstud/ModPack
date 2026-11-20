package com.example.grimoiremod.entity;

import com.example.grimoiremod.GrimoireMod;
import com.example.grimoiremod.data.GlyphInstance;
import com.example.grimoiremod.data.SpellParser;
import com.example.grimoiremod.glyphs.GrimoireEffects;
import com.example.grimoiremod.glyphs.Stat;
import com.example.grimoiremod.glyphs.StatBlock;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

public class SpellProjectile extends Projectile {
    private static final EntityDataAccessor<String> PRIMARY =
            SynchedEntityData.defineId(SpellProjectile.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> LIFE =
            SynchedEntityData.defineId(SpellProjectile.class, EntityDataSerializers.INT);

    private static final int BASE_MAX_LIFE = 100;
    private static final int MIN_MAX_LIFE = 5;
    private static final double SPEED = 1.0;
    private static final int TRAIL_DELAY_TICKS = 3;

    private List<SpellParser.Element> elements = new ArrayList<>();
    private StatBlock formStats = StatBlock.defaults();
    private float maxLifeMultiplier = 1.0f;
    private float critFactor = 1.0f;

    public SpellProjectile(EntityType<? extends SpellProjectile> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public SpellProjectile(Level level, LivingEntity owner,
                           List<SpellParser.Element> elements,
                           StatBlock formStats, float critFactor, Vec3 direction) {
        super(GrimoireMod.SPELL_PROJECTILE.get(), level);
        setOwner(owner);
        setNoGravity(true);
        this.formStats = formStats;
        this.critFactor = critFactor;
        this.maxLifeMultiplier = formStats.getFloat(Stat.LIFE);
        setElements(elements);
        setPos(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
        setDeltaMovement(direction.normalize().scale(SPEED));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(PRIMARY, "");
        builder.define(LIFE, 0);
    }

    private int life() { return entityData.get(LIFE); }
    private void setLife(int v) { entityData.set(LIFE, v); }

    public void setElements(List<SpellParser.Element> els) {
        this.elements = new ArrayList<>(els);
        String primary = els.isEmpty() ? "" : els.get(0).instance().typeId();
        entityData.set(PRIMARY, primary);
    }

    public String primaryElement() {
        return entityData.get(PRIMARY);
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide()) {
            if (life() < TRAIL_DELAY_TICKS) return;
            spawnTrail();
            return;
        }

        setLife(life() + 1);
        int maxLife = Math.max(MIN_MAX_LIFE, (int)(BASE_MAX_LIFE * maxLifeMultiplier));
        if (life() > maxLife) { discard(); return; }

        Vec3 from = position();
        Vec3 to = from.add(getDeltaMovement());

        EntityHitResult entHit = findEntityHit(from, to);
        if (entHit != null) {
            if (entHit.getEntity() instanceof LivingEntity target) {
                for (SpellParser.Element el : elements) {
                    GrimoireEffects.apply(el, formStats, target, critFactor, getOwner(), this);
                }
            }
            burstParticles();
            discard();
            return;
        }

        HitResult blockHit = level().clip(new ClipContext(from, to,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (blockHit.getType() != HitResult.Type.MISS) {
            setPos(blockHit.getLocation());
            burstParticles();
            discard();
            return;
        }

        setPos(to);
    }

    private EntityHitResult findEntityHit(Vec3 from, Vec3 to) {
        EntityHitResult best = null;
        double bestDist = Double.MAX_VALUE;
        AABB sweep = getBoundingBox().expandTowards(getDeltaMovement()).inflate(0.5);
        for (var e : level().getEntities(this, sweep,
                e -> e instanceof LivingEntity && e != getOwner() && e.isAlive())) {
            AABB box = e.getBoundingBox().inflate(0.3);
            var opt = box.clip(from, to);
            if (opt.isPresent()) {
                double d = from.distanceToSqr(opt.get());
                if (d < bestDist) { bestDist = d; best = new EntityHitResult(e, opt.get()); }
            }
        }
        return best;
    }

    private void spawnTrail() {
        Vec3 p = position();
        switch (primaryElement()) {
            case "fire", "fireball" -> {
                level().addParticle(ParticleTypes.FLAME, p.x, p.y, p.z, jitter(0.02), jitter(0.02), jitter(0.02));
                if (random.nextInt(3) == 0)
                    level().addParticle(ParticleTypes.SMOKE, p.x, p.y, p.z, jitter(0.03), 0.02, jitter(0.03));
            }
            case "ice" -> level().addParticle(ParticleTypes.SNOWFLAKE, p.x, p.y, p.z, jitter(0.04), jitter(0.04), jitter(0.04));
            case "water" -> level().addParticle(ParticleTypes.SPLASH, p.x, p.y, p.z, jitter(0.04), jitter(0.04), jitter(0.04));
            case "heal" -> level().addParticle(ParticleTypes.HEART, p.x, p.y, p.z, jitter(0.04), jitter(0.04), jitter(0.04));
            case "speed", "slow" -> level().addParticle(ParticleTypes.ENCHANTED_HIT, p.x, p.y, p.z, jitter(0.04), jitter(0.04), jitter(0.04));
            default -> level().addParticle(ParticleTypes.CRIT, p.x, p.y, p.z, jitter(0.06), jitter(0.06), jitter(0.06));
        }
    }

    private double jitter(double amp) { return (random.nextDouble() - 0.5) * 2 * amp; }

    private void burstParticles() {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 p = position();
        switch (primaryElement()) {
            case "fire", "fireball" -> {
                sl.sendParticles(ParticleTypes.FLAME, p.x, p.y, p.z, 25, 0.4, 0.4, 0.4, 0.10);
                sl.sendParticles(ParticleTypes.LAVA,  p.x, p.y, p.z,  6, 0.3, 0.3, 0.3, 0.00);
                sl.sendParticles(ParticleTypes.SMOKE, p.x, p.y, p.z, 12, 0.4, 0.4, 0.4, 0.04);
            }
            case "ice" -> {
                sl.sendParticles(ParticleTypes.SNOWFLAKE, p.x, p.y, p.z, 25, 0.4, 0.4, 0.4, 0.10);
                sl.sendParticles(ParticleTypes.CLOUD,     p.x, p.y, p.z, 12, 0.4, 0.4, 0.4, 0.04);
            }
            case "water" -> {
                sl.sendParticles(ParticleTypes.SPLASH, p.x, p.y, p.z, 25, 0.4, 0.4, 0.4, 0.10);
                sl.sendParticles(ParticleTypes.CLOUD,  p.x, p.y, p.z,  8, 0.4, 0.4, 0.4, 0.04);
            }
            case "heal" -> {
                sl.sendParticles(ParticleTypes.HEART, p.x, p.y, p.z, 12, 0.4, 0.4, 0.4, 0.10);
                sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.x, p.y, p.z, 8, 0.4, 0.4, 0.4, 0.05);
            }
            case "speed", "slow" -> sl.sendParticles(ParticleTypes.ENCHANTED_HIT, p.x, p.y, p.z, 20, 0.4, 0.4, 0.4, 0.10);
            default -> {
                sl.sendParticles(ParticleTypes.CRIT,          p.x, p.y, p.z, 20, 0.4, 0.4, 0.4, 0.12);
                sl.sendParticles(ParticleTypes.ENCHANTED_HIT, p.x, p.y, p.z, 10, 0.4, 0.4, 0.4, 0.06);
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(PRIMARY, tag.getString("Primary"));
        setLife(tag.getInt("Life"));
        this.maxLifeMultiplier = tag.contains("LifeMult") ? tag.getFloat("LifeMult") : 1.0f;
        this.critFactor = tag.contains("CritFactor") ? tag.getFloat("CritFactor") : 1.0f;

        this.formStats = StatBlock.defaults();
        if (tag.contains("FormStats")) {
            CompoundTag fs = tag.getCompound("FormStats");
            EnumMap<Stat, Double> m = new EnumMap<>(Stat.class);
            for (Stat s : Stat.values()) {
                m.put(s, fs.contains(s.name()) ? fs.getDouble(s.name()) : s.defaultValue);
            }
            this.formStats = new StatBlock(m);
        }

        this.elements = new ArrayList<>();
        ListTag list = tag.getList("Elements", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag et = list.getCompound(i);
            GlyphInstance inst = GlyphInstance.fromNbt(et);
            List<String> mods = new ArrayList<>();
            ListTag modList = et.getList("Mods", Tag.TAG_STRING);
            for (int j = 0; j < modList.size(); j++) mods.add(modList.getString(j));
            this.elements.add(new SpellParser.Element(inst, mods));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putString("Primary", entityData.get(PRIMARY));
        tag.putInt("Life", life());
        tag.putFloat("LifeMult", maxLifeMultiplier);
        tag.putFloat("CritFactor", critFactor);

        CompoundTag fs = new CompoundTag();
        for (Stat s : Stat.values()) fs.putDouble(s.name(), formStats.get(s));
        tag.put("FormStats", fs);

        ListTag list = new ListTag();
        for (SpellParser.Element e : elements) {
            CompoundTag et = e.instance().toNbt();
            ListTag modList = new ListTag();
            for (String m : e.mods()) modList.add(StringTag.valueOf(m));
            et.put("Mods", modList);
            list.add(et);
        }
        tag.put("Elements", list);
    }
}
