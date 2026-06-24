package com.evolutionmod.events;

import com.evolutionmod.EvolutionMod;
import com.evolutionmod.core.EvolutionManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.TickEvent;

/**
 * Détecte le changement de jour Minecraft et déclenche le cycle d'évolution.
 *
 * Un "jour" Minecraft = 24000 ticks = environ 20 minutes réelles.
 * C'est à chaque aube que l'évolution se manifeste.
 */
@Mod.EventBusSubscriber(modid = EvolutionMod.MOD_ID)
public class DayChangeEventHandler {

    private static long lastTrackedDay = -1;

    @SubscribeEvent
    public static void onWorldTick(TickEvent.LevelTickEvent event) {
        // Seulement côté serveur, seulement en overworld, seulement en fin de tick
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.level instanceof ServerLevel serverLevel)) return;
        if (serverLevel.dimension() != Level.OVERWORLD) return;

        long currentDay = serverLevel.getDayTime() / 24000L;

        if (currentDay != lastTrackedDay) {
            lastTrackedDay = currentDay;
            triggerDailyEvolution(serverLevel, currentDay);
        }
    }

    private static void triggerDailyEvolution(ServerLevel level, long day) {
        EvolutionMod.LOGGER.info("Aube du Jour {} - Déclenchement du cycle d'évolution...", day);

        try {
            EvolutionManager.getInstance().onDayChange(day, level);
        } catch (Exception e) {
            EvolutionMod.LOGGER.error("Erreur lors du cycle d'évolution du jour {}: {}", day, e.getMessage());
        }
    }

    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel serverLevel &&
            serverLevel.dimension() == Level.OVERWORLD) {
            // Réinitialiser le tracker au chargement
            lastTrackedDay = serverLevel.getDayTime() / 24000L;
            EvolutionMod.LOGGER.info("Monde chargé - Evolution tracker initialisé au jour {}", lastTrackedDay);
        }
    }
}
