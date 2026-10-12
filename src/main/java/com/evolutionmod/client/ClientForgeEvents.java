package com.evolutionmod.client;

import com.evolutionmod.EvolutionMod;
import com.evolutionmod.network.EvoNetwork;
import com.evolutionmod.network.RequestJournalMsg;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Événements client (bus Forge) : touche du journal et HUD. */
@Mod.EventBusSubscriber(modid = EvolutionMod.MOD_ID, value = Dist.CLIENT)
public final class ClientForgeEvents {

    private ClientForgeEvents() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        while (ClientModEvents.OPEN_JOURNAL.consumeClick()) {
            if (mc.player != null && mc.screen == null && mc.getConnection() != null) {
                EvoNetwork.sendToServer(new RequestJournalMsg());
            }
        }
        HudOverlay.tick();
    }

    /**
     * Forge 1.21.1 n'expose pas d'événement de HUD générique ; l'événement de
     * personnalisation du chat est émis à chaque image pendant le rendu du HUD,
     * on s'en sert donc pour dessiner nos propres éléments.
     */
    @SubscribeEvent
    public static void onHud(CustomizeGuiOverlayEvent.Chat event) {
        HudOverlay.render(event.getGuiGraphics(), event.getPartialTick());
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        HudOverlay.clear();
    }
}
