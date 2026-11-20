package com.example.grimoiremod;

import com.example.grimoiremod.block.GlyphWorkbenchBlock;
import com.example.grimoiremod.entity.SpellProjectile;
import com.example.grimoiremod.glyphs.WetEffect;
import com.example.grimoiremod.item.CreativeGrimoireItem;
import com.example.grimoiremod.item.GrimoireItem;
import com.example.grimoiremod.menu.GlyphWorkbenchMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(GrimoireMod.MOD_ID)
public class GrimoireMod {
    public static final String MOD_ID = "grimoiremod";

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, MOD_ID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, MOD_ID);
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MOD_ID);

    public static final DeferredItem<GrimoireItem> GRIMOIRE =
            ITEMS.register("grimoire", () -> new GrimoireItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<CreativeGrimoireItem> CREATIVE_GRIMOIRE =
            ITEMS.register("creative_grimoire", () -> new CreativeGrimoireItem(new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<Block, Block> GLYPH_WORKBENCH = BLOCKS.register("glyph_workbench",
            () -> new GlyphWorkbenchBlock(GlyphWorkbenchBlock.defaultProps()));

    public static final DeferredItem<BlockItem> GLYPH_WORKBENCH_ITEM = ITEMS.register("glyph_workbench",
            () -> new BlockItem(GLYPH_WORKBENCH.get(), new Item.Properties()));

    public static final DeferredHolder<MenuType<?>, MenuType<GlyphWorkbenchMenu>> GLYPH_WORKBENCH_MENU =
            MENUS.register("glyph_workbench", () -> IMenuTypeExtension.create(
                    (id, inv, buf) -> new GlyphWorkbenchMenu(id, inv, buf.readVarInt())));

    public static final DeferredHolder<EntityType<?>, EntityType<SpellProjectile>> SPELL_PROJECTILE =
            ENTITIES.register("spell_projectile", () -> EntityType.Builder
                    .<SpellProjectile>of(SpellProjectile::new, MobCategory.MISC)
                    .sized(0.4f, 0.4f)
                    .clientTrackingRange(4)
                    .updateInterval(1)
                    .build(MOD_ID + ":spell_projectile"));

    public static final DeferredHolder<MobEffect, MobEffect> WET =
            EFFECTS.register("wet", WetEffect::new);

    public GrimoireMod(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        ENTITIES.register(modEventBus);
        EFFECTS.register(modEventBus);
        MENUS.register(modEventBus);
    }
}
