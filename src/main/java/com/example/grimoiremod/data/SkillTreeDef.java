package com.example.grimoiremod.data;

import java.util.List;

public final class SkillTreeDef {
    public enum NodeType {
        ROOT, ELEMENT_SELECTOR, ELEMENT_TIER, STAT_BOOST
    }

    public record Node(String id, String nameKey, int x, int y, int cost, 
                       List<String> prereqs, int maxLevel, NodeType type) {}

    public static final List<Node> NODES = List.of(
            // --- ЦЕНТР ---
            new Node("grimoiremod:root", "gui.grimoiremod.skill.root", 0, 0, 0, List.of(), 1, NodeType.ROOT),

            // --- ВЕТКА СТИХИЙ (Вверх) ---
            new Node("grimoiremod:mastery_elements", "gui.grimoiremod.skill.mastery_elements", 0, -2, 1, List.of("grimoiremod:root"), 1, NodeType.ELEMENT_SELECTOR),
            new Node("grimoiremod:element_fire_t1", "gui.grimoiremod.skill.elem.fire", -4, -4, 1, List.of("grimoiremod:mastery_elements"), 100, NodeType.ELEMENT_TIER),
            new Node("grimoiremod:element_ice_t1", "gui.grimoiremod.skill.elem.ice", -2, -4, 1, List.of("grimoiremod:mastery_elements"), 100, NodeType.ELEMENT_TIER),
            new Node("grimoiremod:element_water_t1", "gui.grimoiremod.skill.elem.water", 0, -4, 1, List.of("grimoiremod:mastery_elements"), 100, NodeType.ELEMENT_TIER),
            new Node("grimoiremod:element_lightning_t1", "gui.grimoiremod.skill.elem.lightning", 2, -4, 1, List.of("grimoiremod:mastery_elements"), 100, NodeType.ELEMENT_TIER),
            new Node("grimoiremod:element_nature_t1", "gui.grimoiremod.skill.elem.nature", 4, -4, 1, List.of("grimoiremod:mastery_elements"), 100, NodeType.ELEMENT_TIER),

            // --- ВЕТКА БОЯ (Вправо) ---
            new Node("grimoiremod:mastery_combat", "gui.grimoiremod.skill.mastery_combat", 2, 0, 1, List.of("grimoiremod:root"), 1, NodeType.ROOT),
            new Node("grimoiremod:combat_damage", "gui.grimoiremod.skill.combat.damage", 4, -1, 1, List.of("grimoiremod:mastery_combat"), 100, NodeType.STAT_BOOST),
            new Node("grimoiremod:combat_radius", "gui.grimoiremod.skill.combat.radius", 4, 1, 1, List.of("grimoiremod:mastery_combat"), 100, NodeType.STAT_BOOST),

            // --- ВЕТКА ПОДДЕРЖКИ (Влево) ---
            new Node("grimoiremod:mastery_support", "gui.grimoiremod.skill.mastery_support", -2, 0, 1, List.of("grimoiremod:root"), 1, NodeType.ROOT),
            new Node("grimoiremod:support_heal", "gui.grimoiremod.skill.support.heal", -4, 1, 1, List.of("grimoiremod:mastery_support"), 100, NodeType.STAT_BOOST),

            // --- ВЕТКА УТИЛИТ (Вниз) ---
            new Node("grimoiremod:mastery_utility", "gui.grimoiremod.skill.mastery_utility", 0, 2, 1, List.of("grimoiremod:root"), 1, NodeType.ROOT),
            new Node("grimoiremod:utility_cooldown", "gui.grimoiremod.skill.utility.cooldown", 1, 4, 1, List.of("grimoiremod:mastery_utility"), 100, NodeType.STAT_BOOST)
    );

    private SkillTreeDef() {}

    public static Node byId(String id) {
        if (id == null) return null;
        for (Node n : NODES) if (n.id().equals(id)) return n;
        return null;
    }
}
