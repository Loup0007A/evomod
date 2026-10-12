## Evolution Mod 2.0.0 — refonte complète

**Nouveautés**
- Journal d'évolution (touche **J**, objet ou `/evolution`) : statistiques du monde, fiche de chaque espèce (lignées, courbe de divergence, gènes comparés à l'ancêtre), histoire évolutive, votre propre génome.
- Écran d'analyse génétique (scanner) avec rendu 3D de la créature, traits et comparaison avec la moyenne de l'espèce. Lecture rapide en surimpression quand le scanner est en main.
- Laboratoire génétique : fusion, clonage et mutagenèse de cristaux génomiques.
- Notifications HUD animées (aube, spéciations, mutations du joueur).
- Vraie sélection naturelle : les mobs qui blessent ou tuent des joueurs, survivent ou se reproduisent façonnent leur espèce ; ceux que vous chassez en sont écartés. L'élevage devient de la sélection artificielle.
- Couleurs génétiques et bioluminescence visibles sur les créatures ; arbres lumineux et bois mutant ; cultures plus rapides et plus productives.
- Configuration (`config/evolutionmod-common.toml`), recettes pour tous les objets, onglet créatif dédié, traduction FR/EN.

**Corrections**
- Les mobs ne deviennent plus géants à 250 PV dès leur apparition (les gènes partent désormais de la valeur vanilla).
- Les données d'évolution sont réellement sauvegardées, par monde (plus de fuite entre mondes).
- Plus de plantage en serveur dédié (code client isolé).
- Le génome est synchronisé avec le client ; le joueur le conserve après la mort.
- Les modifications passent par des modificateurs d'attributs réversibles ; les entités abîmées par la v1.0 sont réparées automatiquement.
- Nombreux autres correctifs (dégâts de feu, logs, déclenchement des arbres, villageois/porte-armures exclus...).
