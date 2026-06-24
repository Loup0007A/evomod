package com.evolutionmod.genetics;

import net.minecraftforge.eventbus.api.IEventBus;

/**
 * Point d'extension réservé pour un futur registre Forge de gènes
 * (par ex. si les GeneType devaient devenir des objets enregistrés via
 * datapack plutôt qu'un enum fixe en dur). Non appelé actuellement —
 * {@link Gene.GeneType} suffit tant que la liste de gènes reste statique.
 * Conservé pour documenter où brancher cette extension si elle devient
 * nécessaire, plutôt que de le faire disparaître silencieusement.
 */
public class GeneRegistry {

    public static void register(IEventBus modEventBus) {
        // Intentionnellement vide — voir Javadoc de la classe.
    }
}

