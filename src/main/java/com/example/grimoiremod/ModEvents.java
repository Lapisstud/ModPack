package com.example.grimoiremod;

import com.example.grimoiremod.data.SkillTreeData;
import com.example.grimoiremod.entity.SpellProjectile;
import com.example.grimoiremod.item.GrimoireItem;
import com.example.grimoiremod.util.GrimoireLocator;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.UUID;

@EventBusSubscriber(modid = GrimoireMod.MOD_ID)
public class ModEvents {

    private static final String SAVED_GRIMOIRE = "SavedGrimoire";

    @SubscribeEvent
    public static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(GrimoireMod.GRIMOIRE);
            event.accept(GrimoireMod.CREATIVE_GRIMOIRE);
        }
    }

    @SubscribeEvent
    public static void onItemToss(ItemTossEvent event) {
        if (!(event.getEntity().getItem().getItem() instanceof GrimoireItem)) return;
        if (event.getEntity().getTarget() == null) {
            if (event.getPlayer().getInventory().add(event.getEntity().getItem())) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity deadEntity = event.getEntity();
        if (deadEntity.level().isClientSide()) return;

        ServerPlayer player = null;
        String element = null;

        // 1. Проверяем прямой удар снарядом
        Entity direct = event.getSource().getDirectEntity();
        if (direct instanceof SpellProjectile proj) {
            if (proj.getOwner() instanceof ServerPlayer p) {
                player = p;
                element = proj.primaryElement();
            }
        } 
        // 2. Если умер от горения/яда — читаем NBT метку
        else {
            CompoundTag data = deadEntity.getPersistentData();
            if (data.contains("GrimoireLastOwner")) {
                UUID ownerUUID = data.getUUID("GrimoireLastOwner");
                player = deadEntity.getServer().getPlayerList().getPlayer(ownerUUID);
                element = data.getString("GrimoireLastElement");
            }
        }

        if (player == null || element == null || element.isEmpty()) return;

        String elementId = element.equals("fireball") ? "fire" : element;
        String nodeId = "grimoiremod:element_" + elementId + "_t1";

        int slot = GrimoireLocator.findFirst(player);
        if (slot < 0) {
            player.sendSystemMessage(Component.literal("§c[DEBUG] Гримуар не найден в инвентаре!"));
            return;
        }
        
        ItemStack stack = player.getInventory().getItem(slot);
        if (!(stack.getItem() instanceof GrimoireItem)) return;

        if (!SkillTreeData.isUnlocked(stack, nodeId)) {
            player.sendSystemMessage(Component.literal("§c[DEBUG] Узел " + elementId + " не изучен!"));
            return;
        }

        float freeMastery = 0.1f;
        float nodeExp = 0.9f;

        SkillTreeData.addNodeExp(stack, nodeId, nodeExp, 100);
        SkillTreeData.addPoints(stack, freeMastery);
        
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();

        player.sendSystemMessage(Component.literal("§a+0.1 mastery (" + elementId + ")"), true);
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        event.getDrops().removeIf(drop -> {
            if (drop.getItem().getItem() instanceof GrimoireItem) {
                Tag tag = drop.getItem().save(player.level().registryAccess());
                CompoundTag wrapper = new CompoundTag();
                wrapper.put("Item", tag);
                player.getPersistentData().put(SAVED_GRIMOIRE, wrapper);
                return true;
            }
            return false;
        });
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;
        CompoundTag data = event.getOriginal().getPersistentData();
        if (data.contains(SAVED_GRIMOIRE)) {
            CompoundTag wrapper = data.getCompound(SAVED_GRIMOIRE);
            Tag itemTag = wrapper.get("Item");
            if (itemTag != null) {
                ItemStack saved = ItemStack.CODEC.parse(
                        event.getEntity().level().registryAccess().createSerializationContext(NbtOps.INSTANCE),
                        itemTag
                ).result().orElse(ItemStack.EMPTY);
                if (!saved.isEmpty()) {
                    event.getEntity().getInventory().add(saved);
                }
            }
            data.remove(SAVED_GRIMOIRE);
        }
    }
}
