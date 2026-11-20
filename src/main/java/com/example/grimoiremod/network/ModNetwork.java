package com.example.grimoiremod.network;

import com.example.grimoiremod.GrimoireMod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = GrimoireMod.MOD_ID)
public class ModNetwork {
    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(GrimoireActionPacket.TYPE, GrimoireActionPacket.CODEC, GrimoireActionPacket::handle);
        registrar.playToServer(AllocatePointPacket.TYPE, AllocatePointPacket.CODEC, AllocatePointPacket::handle);
        registrar.playToServer(UnlockSkillNodePacket.TYPE, UnlockSkillNodePacket.CODEC, UnlockSkillNodePacket::handle);
        registrar.playToServer(LevelUpSkillNodePacket.TYPE, LevelUpSkillNodePacket.CODEC, LevelUpSkillNodePacket::handle);
    }
}
