package com.example.grimoiremod.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {
    public static final KeyMapping PREV_SPELL = new KeyMapping(
            "key.grimoiremod.prev", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_Z, "key.categories.grimoiremod");
    public static final KeyMapping NEXT_SPELL = new KeyMapping(
            "key.grimoiremod.next", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_X, "key.categories.grimoiremod");
    public static final KeyMapping SKILL_TREE = new KeyMapping(
            "key.grimoiremod.skill_tree", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K, "key.categories.grimoiremod");
}
