package com.example.grimoiremod.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class SkillTreeData {
    public static final String KEY      = "SkillTree";
    public static final String POINTS   = "Points";
    public static final String UNLOCKED = "Unlocked";
    public static final String LEVELS   = "Levels";
    public static final String EXP      = "Exp";

    private static final String ROOT = "Grimoire";
    public static final int STARTING_POINTS = 5;

    private SkillTreeData() {}

    public static CompoundTag fresh() {
        CompoundTag t = new CompoundTag();
        t.putFloat(POINTS, STARTING_POINTS);
        t.put(UNLOCKED, new ListTag());
        t.put(LEVELS, new CompoundTag());
        t.put(EXP, new CompoundTag());
        return t;
    }

    private static CompoundTag treeOrNull(ItemStack stack) {
        CompoundTag root = GrimoireData.readRoot(stack);
        CompoundTag g = root.getCompound(ROOT);
        return g.contains(KEY) ? g.getCompound(KEY) : null;
    }

    public static float getPointsFrac(ItemStack stack) {
        CompoundTag tree = treeOrNull(stack);
        if (tree == null) return 0f;
        return tree.contains(POINTS, Tag.TAG_FLOAT) ? tree.getFloat(POINTS) : tree.getInt(POINTS);
    }

    public static int getPoints(ItemStack stack) {
        return (int)getPointsFrac(stack);
    }

    public static boolean isUnlocked(ItemStack stack, String nodeId) {
        CompoundTag tree = treeOrNull(stack);
        return tree != null && isUnlockedIn(tree, nodeId);
    }

    private static boolean isUnlockedIn(CompoundTag tree, String nodeId) {
        ListTag list = tree.getList(UNLOCKED, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            if (list.getString(i).equals(nodeId)) return true;
        }
        return false;
    }

    public static int getNodeLevel(ItemStack stack, String nodeId) {
        CompoundTag tree = treeOrNull(stack);
        if (tree == null) return 0;
        return tree.getCompound(LEVELS).getInt(nodeId);
    }

    public static float getNodeExp(ItemStack stack, String nodeId) {
        CompoundTag tree = treeOrNull(stack);
        if (tree == null) return 0;
        return tree.getCompound(EXP).getFloat(nodeId);
    }

    public static void addNodeLevel(ItemStack stack, String nodeId, int amount) {
        CompoundTag root = GrimoireData.readRoot(stack);
        CompoundTag g = root.getCompound(ROOT);
        CompoundTag tree = g.contains(KEY) ? g.getCompound(KEY) : fresh();
        CompoundTag levels = tree.getCompound(LEVELS);
        levels.putInt(nodeId, levels.getInt(nodeId) + amount);
        tree.put(LEVELS, levels);
        g.put(KEY, tree);
        root.put(ROOT, g);
        GrimoireData.saveRoot(stack, root);
    }

    public static void addNodeExp(ItemStack stack, String nodeId, float amount, int maxLevel) {
        CompoundTag root = GrimoireData.readRoot(stack);
        CompoundTag g = root.getCompound(ROOT);
        CompoundTag tree = g.contains(KEY) ? g.getCompound(KEY) : fresh();

        CompoundTag levels = tree.getCompound(LEVELS);
        CompoundTag exp = tree.getCompound(EXP);

        int currentLevel = levels.getInt(nodeId);
        if (currentLevel >= maxLevel) return;

        float currentExp = exp.getFloat(nodeId) + amount;
        while (currentExp >= 1.0f && currentLevel < maxLevel) {
            currentExp -= 1.0f;
            currentLevel++;
        }
        levels.putInt(nodeId, currentLevel);
        exp.putFloat(nodeId, currentExp);

        tree.put(LEVELS, levels);
        tree.put(EXP, exp);
        g.put(KEY, tree);
        root.put(ROOT, g);
        GrimoireData.saveRoot(stack, root);
    }

    public static int addPoints(ItemStack stack, float amount) {
        CompoundTag root = GrimoireData.readRoot(stack);
        CompoundTag g = root.getCompound(ROOT);
        CompoundTag tree = g.contains(KEY) ? g.getCompound(KEY) : fresh();

        float cur = tree.contains(POINTS, Tag.TAG_FLOAT) ? tree.getFloat(POINTS) : tree.getInt(POINTS);
        float next = Math.max(0, cur + amount);
        tree.putFloat(POINTS, next);

        g.put(KEY, tree);
        root.put(ROOT, g);
        GrimoireData.saveRoot(stack, root);
        return (int)next;
    }

    public static boolean tryUnlock(ItemStack stack, String nodeId, int cost, List<String> prereqs) {
        CompoundTag root = GrimoireData.readRoot(stack);
        CompoundTag g = root.getCompound(ROOT);
        CompoundTag tree = g.contains(KEY) ? g.getCompound(KEY) : fresh();

        if (isUnlockedIn(tree, nodeId)) return false;
        for (String p : prereqs) if (!isUnlockedIn(tree, p)) return false;
        
        float pts = tree.contains(POINTS, Tag.TAG_FLOAT) ? tree.getFloat(POINTS) : tree.getInt(POINTS);
        if (pts < cost) return false;

        tree.putFloat(POINTS, pts - cost);
        ListTag list = tree.getList(UNLOCKED, Tag.TAG_STRING);
        ListTag newList = new ListTag();
        for (int i = 0; i < list.size(); i++) newList.add(list.get(i));
        newList.add(StringTag.valueOf(nodeId));
        tree.put(UNLOCKED, newList);

        // При разблокировке сразу ставим 1 уровень
        CompoundTag levels = tree.getCompound(LEVELS);
        if (levels.getInt(nodeId) == 0) {
            levels.putInt(nodeId, 1);
            tree.put(LEVELS, levels);
        }

        g.put(KEY, tree);
        root.put(ROOT, g);
        GrimoireData.saveRoot(stack, root);
        return true;
    }
}
