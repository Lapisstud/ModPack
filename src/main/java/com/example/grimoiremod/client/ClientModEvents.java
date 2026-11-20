package com.example.grimoiremod.client;

import com.example.grimoiremod.GrimoireMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = GrimoireMod.MOD_ID, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(KeyBindings.PREV_SPELL);
        event.register(KeyBindings.NEXT_SPELL);
        event.register(KeyBindings.SKILL_TREE);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(GrimoireMod.SPELL_PROJECTILE.get(), SpellProjectileRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(GrimoireMod.GLYPH_WORKBENCH_MENU.get(), GlyphWorkbenchScreen::new);
    }
}
