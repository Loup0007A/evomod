package com.evolutionmod.genetics;

/**
 * Enregistrement d'un événement génétique.
 * Permet de tracer l'histoire évolutive des entités et espèces.
 */
public class GeneticEvent {

    public enum EventType {
        MUTATION,           // Mutation simple d'un gène
        RADICAL_MUTATION,   // Mutation radicale
        GENETIC_DRIFT,      // Dérive génétique neutre
        SPECIATION,         // Divergence en nouvelle espèce
        HORIZONTAL_TRANSFER,// Transfert génétique (infection, magie)
        ADAPTATION,         // Adaptation sélective à l'environnement
        EXTINCTION_RISK,    // L'individu s'éloigne dangereusement de l'optimum
    }

    public final EventType type;
    public final Gene.GeneType affectedGene;
    public final double newValue;
    public final long dayOccurred;
    public final String description;

    public GeneticEvent(EventType type, Gene.GeneType gene, double value, long day) {
        this.type = type;
        this.affectedGene = gene;
        this.newValue = value;
        this.dayOccurred = day;
        this.description = generateDescription(type, gene, value, day);
    }

    private static String generateDescription(EventType type, Gene.GeneType gene, double value, long day) {
        return switch (type) {
            case MUTATION -> String.format("Jour %d: Mutation de %s → %.2f", day, gene.name(), value);
            case RADICAL_MUTATION -> String.format("Jour %d: MUTATION RADICALE de %s → %.2f !", day, gene.name(), value);
            case GENETIC_DRIFT -> String.format("Jour %d: Dérive génétique sur %s", day, gene.name());
            case SPECIATION -> String.format("Jour %d: SPÉCIATION ! Nouvelle espèce diverge !", day);
            case HORIZONTAL_TRANSFER -> String.format("Jour %d: Transfert génétique horizontal sur %s", day, gene.name());
            case ADAPTATION -> String.format("Jour %d: Adaptation sélective de %s", day, gene.name());
            case EXTINCTION_RISK -> String.format("Jour %d: RISQUE D'EXTINCTION - mauvaise adaptation", day);
        };
    }

    @Override
    public String toString() {
        return description;
    }
}
