package com.example.grimoiremod;

import com.example.grimoiremod.data.GrimoireData;
import com.example.grimoiremod.data.SkillTreeData;
import com.example.grimoiremod.item.GrimoireItem;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = GrimoireMod.MOD_ID)
public class GrimoireCommand {
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("glyphtest")
            .then(Commands.argument("type", StringArgumentType.string())
            .then(Commands.argument("points", IntegerArgumentType.integer(0, 50))
            .executes(ctx -> {
                ServerPlayer player = ctx.getSource().getPlayerOrException();
                String type = StringArgumentType.getString(ctx, "type");
                int points = IntegerArgumentType.getInteger(ctx, "points");
                ItemStack stack = new ItemStack(GrimoireMod.GRIMOIRE.get());
                GrimoireData.debugGiveTestBook(stack, type, points);
                player.getInventory().add(stack);
                return 1;
            }))));

        event.getDispatcher().register(Commands.literal("grimoiredel")
            .requires(src -> src.hasPermission(2))
            .executes(ctx -> {
                ServerPlayer player = ctx.getSource().getPlayerOrException();
                boolean found = false;
                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    if (player.getInventory().getItem(i).getItem() instanceof GrimoireItem) {
                        player.getInventory().removeItem(i, 1);
                        found = true;
                        break;
                    }
                }
                if (found) {
                    player.sendSystemMessage(
                            Component.translatable("gui.grimoiremod.command.grimoire_removed")
                                    .withStyle(ChatFormatting.GREEN));
                } else {
                    player.sendSystemMessage(
                            Component.translatable("gui.grimoiremod.command.grimoire_not_found")
                                    .withStyle(ChatFormatting.RED));
                }
                return 1;
            }));

        event.getDispatcher().register(Commands.literal("grimoire")
            .then(Commands.literal("mastery")
                .requires(src -> src.hasPermission(2))
                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 100000))
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        int amount = IntegerArgumentType.getInteger(ctx, "amount");
                        ItemStack stack = player.getMainHandItem();
                        if (!(stack.getItem() instanceof GrimoireItem)) {
                            player.sendSystemMessage(
                                    Component.translatable("gui.grimoiremod.command.need_grimoire_in_hand")
                                            .withStyle(ChatFormatting.RED));
                            return 0;
                        }
                        int total = SkillTreeData.addPoints(stack, amount);
                        player.containerMenu.broadcastChanges();
                        player.sendSystemMessage(
                                Component.translatable("gui.grimoiremod.command.mastery_added", amount, total)
                                        .withStyle(ChatFormatting.GREEN));
                        return 1;
                    }))));
    }
}
