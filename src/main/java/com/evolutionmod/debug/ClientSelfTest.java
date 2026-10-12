package com.evolutionmod.debug;

import com.evolutionmod.EvolutionMod;
import com.evolutionmod.client.HudOverlay;
import com.evolutionmod.client.gui.GeneticLabScreen;
import com.evolutionmod.client.gui.JournalScreen;
import com.evolutionmod.client.gui.ScanScreen;
import com.evolutionmod.network.NotifyMsg;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Test visuel client (CI uniquement, -Devolutionmod.clienttest=true).
 * Le serveur de test ({@link SelfTest}) met en scène les créatures et ouvre
 * chaque interface ; le client se contente de les photographier.
 */
@Mod.EventBusSubscriber(modid = EvolutionMod.MOD_ID, value = Dist.CLIENT)
public final class ClientSelfTest {

    private ClientSelfTest() {}

    private static final boolean ENABLED = Boolean.getBoolean("evolutionmod.clienttest");
    private static int inGame;
    private static int onScreen;
    private static Object lastScreen;
    private static boolean journalDone, scanDone, hudDone;

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent.Post event) {
        if (!ENABLED) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        inGame++;
        if (mc.screen != lastScreen) {
            lastScreen = mc.screen;
            onScreen = 0;
        }
        onScreen++;

        if (inGame == 100) {
            HudOverlay.push(new NotifyMsg(NotifyMsg.Kind.SPECIATION, "evolutionmod.notify.speciation",
                List.of("#species:minecraft:zombie", "Neozombimorphus")));
            HudOverlay.push(new NotifyMsg(NotifyMsg.Kind.DAILY, "evolutionmod.notify.daily", List.of("42", "318", "64")));
        }
        if (!hudDone && inGame >= 160 && mc.screen == null) {
            hudDone = true;
            shot(mc, "01_hud_scanner");
        }

        if (mc.screen instanceof JournalScreen js && !journalDone) {
            switch (onScreen) {
                case 25 -> shot(mc, "02_journal_world");
                case 26, 46, 66 -> js.keyPressed(GLFW.GLFW_KEY_TAB, 0, 0);
                case 45 -> shot(mc, "03_journal_species");
                case 65 -> shot(mc, "04_journal_history");
                case 85 -> shot(mc, "05_journal_self");
                case 90 -> {
                    journalDone = true;
                    mc.setScreen(null);
                }
                default -> { }
            }
        } else if (mc.screen instanceof ScanScreen && !scanDone) {
            if (onScreen == 30) shot(mc, "06_scan");
            if (onScreen == 35) {
                scanDone = true;
                mc.setScreen(null);
            }
        } else if (mc.screen instanceof GeneticLabScreen) {
            if (onScreen == 50) shot(mc, "07_lab");
            if (onScreen == 60) finish(mc);
        }
        if (inGame > 4000) finish(mc);
    }

    private static void finish(Minecraft mc) {
        EvolutionMod.LOGGER.info("[CLIENTTEST] terminé");
        mc.stop();
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(),
            c -> EvolutionMod.LOGGER.info("[CLIENTTEST] capture {} : {}", name, c.getString()));
    }
}
