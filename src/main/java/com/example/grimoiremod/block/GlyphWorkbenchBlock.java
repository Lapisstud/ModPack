package com.example.grimoiremod.block;

import com.example.grimoiremod.data.GlyphPage;
import com.example.grimoiremod.data.GrimoireData;
import com.example.grimoiremod.item.GrimoireItem;
import com.example.grimoiremod.menu.GlyphWorkbenchMenu;
import com.example.grimoiremod.util.GrimoireLocator;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class GlyphWorkbenchBlock extends Block {
    public GlyphWorkbenchBlock(Properties props) {
        super(props);
    }

    public static BlockBehaviour.Properties defaultProps() {
        return BlockBehaviour.Properties.of()
                .strength(2.5F)
                .sound(SoundType.WOOD);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            int slot = GrimoireLocator.findFirst(player);
            if (slot < 0) {
                player.displayClientMessage(Component.translatable("gui.grimoiremod.need_grimoire"), true);
                return InteractionResult.CONSUME;
            }

            // Фикс: если открыто оглавление, переключаем на первое заклинание
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.getItem() instanceof GrimoireItem) {
                int active = GrimoireData.getActivePage(stack);
                GlyphPage page = GrimoireData.getPage(stack, active);
                if (page == null || page.isToc()) {
                    GrimoireData.switchPage(stack, true);
                }
            }

            MenuProvider provider = new SimpleMenuProvider(
                    (id, inv, p) -> new GlyphWorkbenchMenu(id, inv, slot),
                    Component.translatable("gui.grimoiremod.workbench")
            );
            ((ServerPlayer) player).openMenu(provider, buf -> buf.writeVarInt(slot));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
