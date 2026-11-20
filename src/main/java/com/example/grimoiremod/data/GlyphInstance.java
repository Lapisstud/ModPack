package com.example.grimoiremod.data;

import com.example.grimoiremod.glyphs.Stat;
import net.minecraft.nbt.CompoundTag;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class GlyphInstance {
    private final String typeId;
    private final Map<Stat, Integer> allocations;

    public GlyphInstance(String typeId) {
        this(typeId, new HashMap<>());
    }

    public GlyphInstance(String typeId, Map<Stat, Integer> allocations) {
        this.typeId = typeId;
        this.allocations = new HashMap<>(allocations);
    }

    public String typeId() { return typeId; }
    public Map<Stat, Integer> allocations() { return allocations; }

    public int getPoints(Stat stat) {
        return allocations.getOrDefault(stat, 0);
    }

    public int totalPoints() {
        return allocations.values().stream().mapToInt(Integer::intValue).sum();
    }

    public CompoundTag toNbt() {
        CompoundTag t = new CompoundTag();
        t.putString("Type", typeId);
        CompoundTag allocs = new CompoundTag();
        allocations.forEach((stat, val) -> allocs.putInt(stat.name(), val));
        t.put("Allocs", allocs);
        return t;
    }

    public static GlyphInstance fromNbt(CompoundTag t) {
        String type = t.getString("Type");
        Map<Stat, Integer> allocs = new HashMap<>();
        CompoundTag a = t.getCompound("Allocs");
        for (String key : a.getAllKeys()) {
            try { allocs.put(Stat.valueOf(key), a.getInt(key)); }
            catch (IllegalArgumentException ignored) {}
        }
        return new GlyphInstance(type, allocs);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GlyphInstance that = (GlyphInstance) o;
        return typeId.equals(that.typeId) && allocations.equals(that.allocations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(typeId, allocations);
    }
}
