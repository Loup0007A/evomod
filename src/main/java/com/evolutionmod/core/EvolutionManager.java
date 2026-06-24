package com.evolutionmod.core;

import com.evolutionmod.EvolutionMod;
import com.evolutionmod.genetics.Gene;
import com.evolutionmod.genetics.GeneticEvent;
import com.evolutionmod.genetics.Genome;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.*;

/**
 * Le gestionnaire central de l'évolution.
 * Orchestre les cycles d'évolution quotidiens, gère les statistiques globales,
 * et coordonne les effets des mutations sur le monde.
 *
 * Chaque "jour Minecraft" = un cycle d'évolution.
 * Les entités évoluent, les arbres mutent, le monde change.
 */
public class EvolutionManager {

    private static EvolutionManager INSTANCE;

    // Statistiques globales d'évolution
    private long currentDay = 0;
    private long totalMutationsWorldwide = 0;
    private int speciationEvents = 0;
    private final Map<String, Integer> speciesPopulation = new HashMap<>();
    private final Map<String, List<GeneticEvent>> speciationHistory = new HashMap<>();

    // Pression environnementale actuelle (augmente avec le temps)
    private double globalEnvironmentalPressure = 0.1;

    // Cache des génomes globaux pour partager les mutations entre individus de même espèce
    // (utilisé pour les plantes : un seul génome de gabarit par espèce végétale)
    private final Map<String, Genome> speciesTemplates = new HashMap<>();

    // Pour les mobs (et le joueur), il n'existe pas de génome unique par espèce
    // — chaque individu a le sien. On garde ici, par espèce, l'individu le PLUS
    // MUTÉ observé jusqu'à présent : c'est l'information la plus pertinente à
    // afficher dans le GUI (la "pointe" de l'évolution de cette espèce), plutôt
    // qu'une moyenne qui lisserait les mutations intéressantes.
    private final Map<String, Genome> mostEvolvedPerSpecies = new HashMap<>();

    // =================== SINGLETON ===================

    public static EvolutionManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new EvolutionManager();
        }
        return INSTANCE;
    }

    // =================== CYCLE JOURNALIER ===================

    /**
     * Appelé une fois par jour Minecraft (au changement de jour).
     * C'est le point central de tout le système évolutif.
     */
    public void onDayChange(long newDay, ServerLevel level) {
        this.currentDay = newDay;

        // La pression environnementale augmente progressivement
        // (le monde devient de plus en plus "hostile" aux formes de vie statiques)
        globalEnvironmentalPressure = Math.min(1.0, 0.05 + (newDay / 1000.0) * 0.95);

        EvolutionMod.LOGGER.info("=== CYCLE D'ÉVOLUTION - Jour {} === (pression: {:.2f})",
            newDay, globalEnvironmentalPressure);

        // Évoluer toutes les entités vivantes dans tous les mondes chargés
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            for (ServerLevel serverLevel : server.getAllLevels()) {
                evolveLevel(serverLevel, newDay);
            }
        }

        // Évoluer les arbres et plantes
        evolvePlants(level, newDay);

        logEvolutionStatus();
    }

    /**
     * Évolue toutes les entités d'un niveau — mobs, plantes-support, ET joueurs.
     *
     * NOTE 1.21.1 : WorldBorder n'a pas de méthode createBoundingBox(int).
     * ServerLevel#getAllEntities() retourne directement toutes les entités
     * actuellement chargées dans ce niveau, sans avoir besoin de définir
     * une zone de recherche — c'est l'API correcte ici.
     *
     * Les joueurs évoluent désormais comme toute autre créature, mais avec
     * une pression environnementale très réduite (voir PLAYER_MUTATION_DAMPING)
     * pour que les changements restent graduels sur plusieurs dizaines de
     * jours plutôt que de transformer le joueur en quelques minutes — ce
     * serait injouable et illisible en survie.
     */
    private static final double PLAYER_MUTATION_DAMPING = 0.12;

    private void evolveLevel(ServerLevel level, long day) {
        List<LivingEntity> entities = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof LivingEntity living) {
                entities.add(living);
            }
        }

        for (LivingEntity entity : entities) {
            boolean isPlayer = entity instanceof Player;

            EvolutionCapability.getEvolutionData(entity).ifPresent(evoData -> {
                if (!evoData.hasEvolvedToday(day)) {
                    Genome genome = evoData.getGenome();
                    if (genome != null) {
                        double effectivePressure = isPlayer
                            ? globalEnvironmentalPressure * PLAYER_MUTATION_DAMPING
                            : globalEnvironmentalPressure;

                        List<GeneticEvent> events = genome.applyDailyMutation(effectivePressure, day);

                        applyGenomeMutations(entity, genome);

                        evoData.setLastEvolutionDay(day);
                        totalMutationsWorldwide += events.size();

                        for (GeneticEvent event : events) {
                            if (event.type == GeneticEvent.EventType.SPECIATION && !isPlayer) {
                                speciationEvents++;
                                announceSpeciation(entity, genome.getSpeciesLineage(), level);
                            }
                        }

                        // Notifier le joueur de sa propre évolution (sinon c'est invisible
                        // et incompréhensible — contrairement aux mobs qu'on peut scanner,
                        // le joueur ne va pas se scanner lui-même au Cristal Génomique).
                        if (isPlayer && !events.isEmpty() && entity instanceof net.minecraft.server.level.ServerPlayer sp) {
                            GeneticEvent notable = events.get(0);
                            sp.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                                "§d§l[ÉVOLUTION]§r §7Votre corps change légèrement... §8(" +
                                notable.affectedGene.name() + ")"
                            ));
                        }

                        // Maintenir le "record" d'évolution par espèce pour le GUI
                        // (/evolution status) — voir mostEvolvedPerSpecies plus haut.
                        updateMostEvolvedRecord(entity, genome);
                    }
                }
            });
        }

        EvolutionMod.LOGGER.debug("Évolution de {} entités dans {}", entities.size(), level.dimension().location());
    }

    /**
     * Applique les valeurs génomiques aux attributs Minecraft réels de l'entité.
     * C'est ici que la génétique devient réalité dans le jeu.
     */
    public void applyGenomeMutations(LivingEntity entity, Genome genome) {
        try {
            // TAILLE DU CORPS
            double bodySize = genome.getGeneValue(Gene.GeneType.BODY_SIZE);
            if (bodySize > 0.1) {
                // Modifier l'échelle via NBT ou attributs
                entity.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH)
                    .setBaseValue(Math.max(1.0, genome.getGeneValue(Gene.GeneType.HEALTH_MAX)));
            }

            // VITESSE
            double speed = genome.getGeneValue(Gene.GeneType.SPEED);
            var speedAttr = entity.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
            if (speedAttr != null) {
                speedAttr.setBaseValue(Math.max(0.01, Math.min(1.0, speed * 0.1)));
            }

            // FORCE D'ATTAQUE
            double strength = genome.getGeneValue(Gene.GeneType.STRENGTH);
            var attackAttr = entity.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
            if (attackAttr != null) {
                attackAttr.setBaseValue(Math.max(0.5, strength));
            }

            // KNOCKBACK
            double defense = genome.getGeneValue(Gene.GeneType.DEFENSE);
            var knockback = entity.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE);
            if (knockback != null) {
                knockback.setBaseValue(Math.min(1.0, defense));
            }

            // BIOLUMINESCENCE -> effet lumineux
            double biolum = genome.getGeneValue(Gene.GeneType.BIOLUMINESCENCE);
            if (biolum > 5.0) {
                entity.setGlowingTag(true);
            }

            // VENIN -> application de l'effet potion lors des attaques
            // (géré dans EntityEvolutionHandler)

        } catch (Exception e) {
            EvolutionMod.LOGGER.warn("Erreur lors de l'application des mutations à {}: {}",
                entity.getType().getDescriptionId(), e.getMessage());
        }
    }

    /**
     * Fait évoluer les arbres ET les cultures (blé, carottes, etc.) du monde.
     * Toutes les espèces végétales accumulent des mutations dans leur génome
     * de gabarit d'espèce, qui dérive lentement au fil des jours.
     */
    private void evolvePlants(ServerLevel level, long day) {
        // L'évolution végétale est gérée différemment des mobs : on maintient
        // des "génomes d'espèce" globaux (un par espèce d'arbre/culture, pas
        // un par plant individuel) qui dérivent lentement et affectent tous
        // les nouveaux plants générés (voir TreeEvolutionHandler/CropEvolutionHandler).

        String[] treeSpecies = {"oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry"};
        String[] cropSpecies = {"wheat", "carrot", "potato", "beetroot", "sugar_cane", "melon", "pumpkin", "cactus"};

        evolveSpeciesGroup(treeSpecies, day, 0.5);   // Les arbres évoluent plus lentement
        evolveSpeciesGroup(cropSpecies, day, 0.8);   // Les cultures, cycle court, évoluent plus vite
    }

    private void evolveSpeciesGroup(String[] species, long day, double pressureMultiplier) {
        for (String s : species) {
            if (!speciesTemplates.containsKey(s)) {
                speciesTemplates.put(s, Genome.createForSpecies(s));
            }

            Genome genome = speciesTemplates.get(s);
            List<GeneticEvent> events = genome.applyDailyMutation(
                globalEnvironmentalPressure * pressureMultiplier, day
            );

            if (!events.isEmpty()) {
                EvolutionMod.LOGGER.debug("Espèce végétale {} a mutué : {} changement(s)", s, events.size());
            }
        }
    }

    /**
     * Construit les données environnementales pour une entité selon sa position.
     *
     * NOTE 1.21.1 : Biome#getDownfall() n'a pas pu être vérifié de façon fiable
     * dans la documentation disponible pour cette version exacte (les mappings
     * Mojang/Parchment diffèrent de Yarn/Fabric sur ce point). L'humidité n'étant
     * qu'un facteur secondaire non-critique dans EnvironmentData (elle n'intervient
     * dans aucun calcul de fitness actuel), on utilise une valeur neutre fixe
     * plutôt que de risquer un nom de méthode incorrect non confirmé.
     */
    private Genome.EnvironmentData buildEnvironmentData(LivingEntity entity) {
        Level level = entity.level();
        float temperature = level.getBiome(entity.blockPosition()).value()
            .getBaseTemperature();
        float humidity = 0.5f; // valeur neutre — voir note ci-dessus

        double predatorPressure = calculateLocalPredatorPressure(entity);
        double foodAvailability = 0.5; // Simplification

        Genome.EnvironmentData env = new Genome.EnvironmentData(
            temperature, humidity, predatorPressure, foodAvailability
        );
        env.isNether = level.dimension() == Level.NETHER;
        env.isEnd = level.dimension() == Level.END;

        return env;
    }

    private double calculateLocalPredatorPressure(LivingEntity entity) {
        // Compter les mobs hostiles proches
        long hostileCount = entity.level().getEntitiesOfClass(
            net.minecraft.world.entity.monster.Monster.class,
            entity.getBoundingBox().inflate(32)
        ).size();
        return Math.min(1.0, hostileCount / 20.0);
    }

    /**
     * Annonce une spéciation dans le chat des joueurs.
     */
    private void announceSpeciation(LivingEntity entity, String newLineage, ServerLevel level) {
        String entityName = entity.getType().getDescriptionId();
        String message = String.format(
            "§6§l[ÉVOLUTION]§r §eMutation radicale! Un %s a divergé vers '%s'! §7(Jour %d)",
            entityName, newLineage.split(">")[newLineage.split(">").length - 1], currentDay
        );

        level.getServer().getPlayerList().getPlayers().forEach(player ->
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(message))
        );

        speciationHistory.computeIfAbsent(entityName, k -> new ArrayList<>())
            .add(new GeneticEvent(GeneticEvent.EventType.SPECIATION, null, 0, currentDay));
    }

    private void logEvolutionStatus() {
        EvolutionMod.LOGGER.info(
            "Bilan évolutif - Jour: {}, Mutations totales: {}, Speciations: {}, Pression: {:.3f}",
            currentDay, totalMutationsWorldwide, speciationEvents, globalEnvironmentalPressure
        );
    }

    // =================== GETTERS ===================

    public long getCurrentDay() { return currentDay; }
    public long getTotalMutationsWorldwide() { return totalMutationsWorldwide; }
    public int getSpeciationEvents() { return speciationEvents; }
    public double getGlobalEnvironmentalPressure() { return globalEnvironmentalPressure; }
    public Map<String, Genome> getSpeciesTemplates() { return Collections.unmodifiableMap(speciesTemplates); }

    public Genome getSpeciesTemplate(String species) {
        return speciesTemplates.getOrDefault(species, null);
    }

    /**
     * Met à jour le génome "record" (le plus muté) connu pour l'espèce de
     * cette entité, si le génome actuel dépasse le précédent record en
     * nombre de mutations cumulées. Inclut le joueur (clé "player").
     */
    private void updateMostEvolvedRecord(LivingEntity entity, Genome genome) {
        String key = (entity instanceof Player)
            ? "player"
            : entity.getType().getDescriptionId().replace("entity.minecraft.", "");

        Genome current = mostEvolvedPerSpecies.get(key);
        if (current == null || genome.getTotalMutations() > current.getTotalMutations()) {
            mostEvolvedPerSpecies.put(key, genome);
        }
    }

    /**
     * Vue combinée pour le GUI : toutes les espèces végétales (gabarit
     * partagé) ET toutes les espèces de mobs/joueur (génome record observé).
     * C'est la liste complète de "tout ce qui peut évoluer" pour l'écran
     * /evolution status.
     */
    public Map<String, Genome> getAllEvolvableSpecies() {
        Map<String, Genome> combined = new LinkedHashMap<>();
        combined.putAll(speciesTemplates);
        combined.putAll(mostEvolvedPerSpecies);
        return combined;
    }

    /**
     * Restaure un génome d'espèce depuis la sauvegarde persistante.
     * Appelé uniquement par {@link com.evolutionmod.world.EvolutionDataSavedData#load}.
     */
    public void injectSpeciesTemplate(String speciesName, Genome genome) {
        speciesTemplates.put(speciesName, genome);
        EvolutionMod.LOGGER.debug("Template restauré pour '{}'  ({} mutations)",
                speciesName, genome.getTotalMutations());
    }
}
