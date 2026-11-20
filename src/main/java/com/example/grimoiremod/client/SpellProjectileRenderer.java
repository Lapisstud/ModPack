package com.example.grimoiremod.client;

import com.example.grimoiremod.entity.SpellProjectile;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;

public class SpellProjectileRenderer extends EntityRenderer<SpellProjectile> {
    private static final Map<String, ItemStack> ICONS = new HashMap<>();
    static {
        ICONS.put("fire", new ItemStack(Items.FIRE_CHARGE));
        ICONS.put("fireball", new ItemStack(Items.FIRE_CHARGE));
        ICONS.put("ice", new ItemStack(Items.ICE));
        ICONS.put("water", new ItemStack(Items.PRISMARINE_SHARD));
        ICONS.put("heal", new ItemStack(Items.GLISTERING_MELON_SLICE));
        ICONS.put("speed", new ItemStack(Items.SUGAR));
        ICONS.put("slow", new ItemStack(Items.SLIME_BALL));
    }
    private static final ItemStack FALLBACK = new ItemStack(Items.SNOWBALL);

    public SpellProjectileRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(SpellProjectile entity, float entityYaw, float partialTick,
                       PoseStack pose, MultiBufferSource buffers, int packedLight) {
        ItemStack icon = ICONS.getOrDefault(entity.primaryElement(), FALLBACK);

        pose.pushPose();
        pose.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        pose.scale(0.6f, 0.6f, 0.6f);
        Minecraft.getInstance().getItemRenderer().renderStatic(
                icon, ItemDisplayContext.GROUND, packedLight, OverlayTexture.NO_OVERLAY,
                pose, buffers, entity.level(), entity.getId());
        pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(SpellProjectile entity) {
        return ResourceLocation.withDefaultNamespace("textures/misc/white.png");
    }
}
