package com.example.grimoiremod.client;

import com.example.grimoiremod.GrimoireMod;
import com.example.grimoiremod.glyphs.Glyph;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;

import java.util.HashMap;
import java.util.Map;

public final class ClientGlyphIcons {
    private static final Map<String, Boolean> CACHE = new HashMap<>();

    private ClientGlyphIcons() {}

    public static void clearCache() { CACHE.clear(); }

    public static boolean has(String glyphId) {
        if (glyphId == null || glyphId.isEmpty()) return false;
        return CACHE.computeIfAbsent(glyphId, k -> {
            try {
                var mc = Minecraft.getInstance();
                if (mc.getResourceManager() == null) return false;
                return mc.getResourceManager().getResource(path(k)).isPresent();
            } catch (Exception e) {
                return false;
            }
        });
    }

    public static ResourceLocation path(String glyphId) {
        return ResourceLocation.fromNamespaceAndPath(GrimoireMod.MOD_ID,
                "textures/gui/glyph/" + glyphId + ".png");
    }

    public static void draw(GuiGraphics gfx, Font font, Glyph g, int x, int y, int size) {
        int cx = x + (size - 16) / 2;
        int cy = y + (size - 16) / 2;
        if (has(g.id())) {
            gfx.blit(path(g.id()), cx, cy, 0, 0, 16, 16, 16, 16);
        } else {
            gfx.fill(cx, cy, cx + 16, cy + 16, g.color());
            String initial = g.getDisplayName().getString().substring(0, 1).toUpperCase();
            gfx.drawString(font, initial, cx + 8 - font.width(initial) / 2, cy + 4, 0xFFFFFFFF, false);
        }
    }

    @EventBusSubscriber(modid = GrimoireMod.MOD_ID, value = Dist.CLIENT)
    public static class ReloadListener {
        @SubscribeEvent
        public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
            event.registerReloadListener(
                new SimplePreparableReloadListener<Void>() {
                    @Override
                    protected Void prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
                        return null;
                    }
                    @Override
                    protected void apply(Void object, ResourceManager resourceManager, ProfilerFiller profiler) {
                        clearCache();
                    }
                }
            );
        }
    }
}
