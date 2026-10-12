package net.minecraft.world.scores;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.slf4j.Logger;

public class Scoreboard {
   public static final String HIDDEN_SCORE_PREFIX = "#";
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Object2ObjectMap<String, Objective> objectivesByName = new Object2ObjectOpenHashMap(16, 0.5F);
   private final Reference2ObjectMap<ObjectiveCriteria, List<Objective>> objectivesByCriteria = new Reference2ObjectOpenHashMap();
   private final Map<String, PlayerScores> playerScores = new Object2ObjectOpenHashMap(16, 0.5F);
   private final Map<DisplaySlot, Objective> displayObjectives = new EnumMap<>(DisplaySlot.class);
   private final Object2ObjectMap<String, PlayerTeam> teamsByName = new Object2ObjectOpenHashMap();
   private final Object2ObjectMap<String, PlayerTeam> teamsByPlayer = new Object2ObjectOpenHashMap();

   @Nullable
   public Objective getObjective(@Nullable String pName) {
      return (Objective)this.objectivesByName.get(pName);
   }

   public Objective addObjective(
      String pName,
      ObjectiveCriteria pCriteria,
      Component pDisplayName,
      ObjectiveCriteria.RenderType pRenderType,
      boolean pDisplayAutoUpdate,
      @Nullable NumberFormat pNumberFormat
   ) {
      if (this.objectivesByName.containsKey(pName)) {
         throw new IllegalArgumentException("An objective with the name '" + pName + "' already exists!");
      } else {
         Objective $$6 = new Objective(this, pName, pCriteria, pDisplayName, pRenderType, pDisplayAutoUpdate, pNumberFormat);
         ((List)this.objectivesByCriteria.computeIfAbsent(pCriteria, p_310953_ -> Lists.newArrayList())).add($$6);
         this.objectivesByName.put(pName, $$6);
         this.onObjectiveAdded($$6);
         return $$6;
      }
   }

   public final void forAllObjectives(ObjectiveCriteria pCriteria, ScoreHolder pScoreHolder, Consumer<ScoreAccess> pAction) {
      ((List)this.objectivesByCriteria.getOrDefault(pCriteria, Collections.emptyList()))
         .forEach(p_309370_ -> pAction.accept(this.getOrCreatePlayerScore(pScoreHolder, p_309370_, true)));
   }

   private PlayerScores getOrCreatePlayerInfo(String pUsername) {
      return this.playerScores.computeIfAbsent(pUsername, p_309376_ -> new PlayerScores());
   }

   public ScoreAccess getOrCreatePlayerScore(ScoreHolder pScoreHolder, Objective pObjective) {
      return this.getOrCreatePlayerScore(pScoreHolder, pObjective, false);
   }

   public ScoreAccess getOrCreatePlayerScore(final ScoreHolder pScoreHolder, final Objective pObjective, boolean pReadOnly) {
      final boolean $$3 = pReadOnly || !pObjective.getCriteria().isReadOnly();
      PlayerScores $$4 = this.getOrCreatePlayerInfo(pScoreHolder.getScoreboardName());
      final MutableBoolean $$5 = new MutableBoolean();
      final Score $$6 = $$4.getOrCreate(pObjective, p_309375_ -> $$5.setTrue());
      return new ScoreAccess() {
         @Override
         public int get() {
            return $$6.value();
         }

         @Override
         public void set(int p_312858_) {
            if (!$$3) {
               throw new IllegalStateException("Cannot modify read-only score");
            } else {
               boolean $$1 = $$5.isTrue();
               if (pObjective.displayAutoUpdate()) {
                  Component $$2 = pScoreHolder.getDisplayName();
                  if ($$2 != null && !$$2.equals($$6.display())) {
                     $$6.display($$2);
                     $$1 = true;
                  }
               }

               if (p_312858_ != $$6.value()) {
                  $$6.value(p_312858_);
                  $$1 = true;
               }

               if ($$1) {
                  this.sendScoreToPlayers();
               }
            }
         }

         @Nullable
         @Override
         public Component display() {
            return $$6.display();
         }

         @Override
         public void display(@Nullable Component p_309551_) {
            if ($$5.isTrue() || !Objects.equals(p_309551_, $$6.display())) {
               $$6.display(p_309551_);
               this.sendScoreToPlayers();
            }
         }

         @Override
         public void numberFormatOverride(@Nullable NumberFormat p_312257_) {
            $$6.numberFormat(p_312257_);
            this.sendScoreToPlayers();
         }

         @Override
         public boolean locked() {
            return $$6.isLocked();
         }

         @Override
         public void unlock() {
            this.setLocked(false);
         }

         @Override
         public void lock() {
            this.setLocked(true);
         }

         private void setLocked(boolean p_311228_) {
            $$6.setLocked(p_311228_);
            if ($$5.isTrue()) {
               this.sendScoreToPlayers();
            }

            Scoreboard.this.onScoreLockChanged(pScoreHolder, pObjective);
         }

         private void sendScoreToPlayers() {
            Scoreboard.this.onScoreChanged(pScoreHolder, pObjective, $$6);
            $$5.setFalse();
         }
      };
   }

   @Nullable
   public ReadOnlyScoreInfo getPlayerScoreInfo(ScoreHolder pScoreHolder, Objective pObjective) {
      PlayerScores $$2 = this.playerScores.get(pScoreHolder.getScoreboardName());
      return $$2 != null ? $$2.get(pObjective) : null;
   }

   public Collection<PlayerScoreEntry> listPlayerScores(Objective pObjective) {
      List<PlayerScoreEntry> $$1 = new ArrayList<>();
      this.playerScores.forEach((p_309362_, p_309363_) -> {
         Score $$4 = p_309363_.get(pObjective);
         if ($$4 != null) {
            $$1.add(new PlayerScoreEntry(p_309362_, $$4.value(), $$4.display(), $$4.numberFormat()));
         }
      });
      return $$1;
   }

   public Collection<Objective> getObjectives() {
      return this.objectivesByName.values();
   }

   public Collection<String> getObjectiveNames() {
      return this.objectivesByName.keySet();
   }

   public Collection<ScoreHolder> getTrackedPlayers() {
      return this.playerScores.keySet().stream().map(ScoreHolder::forNameOnly).toList();
   }

   public void resetAllPlayerScores(ScoreHolder pScoreHolder) {
      PlayerScores $$1 = this.playerScores.remove(pScoreHolder.getScoreboardName());
      if ($$1 != null) {
         this.onPlayerRemoved(pScoreHolder);
      }
   }

   public void resetSinglePlayerScore(ScoreHolder pScoreHolder, Objective pObjective) {
      PlayerScores $$2 = this.playerScores.get(pScoreHolder.getScoreboardName());
      if ($$2 != null) {
         boolean $$3 = $$2.remove(pObjective);
         if (!$$2.hasScores()) {
            PlayerScores $$4 = this.playerScores.remove(pScoreHolder.getScoreboardName());
            if ($$4 != null) {
               this.onPlayerRemoved(pScoreHolder);
            }
         } else if ($$3) {
            this.onPlayerScoreRemoved(pScoreHolder, pObjective);
         }
      }
   }

   public Object2IntMap<Objective> listPlayerScores(ScoreHolder pScoreHolder) {
      PlayerScores $$1 = this.playerScores.get(pScoreHolder.getScoreboardName());
      return $$1 != null ? $$1.listScores() : Object2IntMaps.emptyMap();
   }

   public void removeObjective(Objective pObjective) {
      this.objectivesByName.remove(pObjective.getName());

      for (DisplaySlot $$1 : DisplaySlot.values()) {
         if (this.getDisplayObjective($$1) == pObjective) {
            this.setDisplayObjective($$1, null);
         }
      }

      List<Objective> $$2 = (List<Objective>)this.objectivesByCriteria.get(pObjective.getCriteria());
      if ($$2 != null) {
         $$2.remove(pObjective);
      }

      for (PlayerScores $$3 : this.playerScores.values()) {
         $$3.remove(pObjective);
      }

      this.onObjectiveRemoved(pObjective);
   }

   public void setDisplayObjective(DisplaySlot pSlot, @Nullable Objective pObjective) {
      this.displayObjectives.put(pSlot, pObjective);
   }

   @Nullable
   public Objective getDisplayObjective(DisplaySlot pSlot) {
      return this.displayObjectives.get(pSlot);
   }

   @Nullable
   public PlayerTeam getPlayerTeam(String pTeamName) {
      return (PlayerTeam)this.teamsByName.get(pTeamName);
   }

   public PlayerTeam addPlayerTeam(String pName) {
      PlayerTeam $$1 = this.getPlayerTeam(pName);
      if ($$1 != null) {
         LOGGER.warn("Requested creation of existing team '{}'", pName);
         return $$1;
      } else {
         $$1 = new PlayerTeam(this, pName);
         this.teamsByName.put(pName, $$1);
         this.onTeamAdded($$1);
         return $$1;
      }
   }

   public void removePlayerTeam(PlayerTeam pPlayerTeam) {
      this.teamsByName.remove(pPlayerTeam.getName());

      for (String $$1 : pPlayerTeam.getPlayers()) {
         this.teamsByPlayer.remove($$1);
      }

      this.onTeamRemoved(pPlayerTeam);
   }

   public boolean addPlayerToTeam(String pPlayerName, PlayerTeam pTeam) {
      if (this.getPlayersTeam(pPlayerName) != null) {
         this.removePlayerFromTeam(pPlayerName);
      }

      this.teamsByPlayer.put(pPlayerName, pTeam);
      return pTeam.getPlayers().add(pPlayerName);
   }

   public boolean removePlayerFromTeam(String pPlayerName) {
      PlayerTeam $$1 = this.getPlayersTeam(pPlayerName);
      if ($$1 != null) {
         this.removePlayerFromTeam(pPlayerName, $$1);
         return true;
      } else {
         return false;
      }
   }

   public void removePlayerFromTeam(String pUsername, PlayerTeam pPlayerTeam) {
      if (this.getPlayersTeam(pUsername) != pPlayerTeam) {
         throw new IllegalStateException("Player is either on another team or not on any team. Cannot remove from team '" + pPlayerTeam.getName() + "'.");
      } else {
         this.teamsByPlayer.remove(pUsername);
         pPlayerTeam.getPlayers().remove(pUsername);
      }
   }

   public Collection<String> getTeamNames() {
      return this.teamsByName.keySet();
   }

   public Collection<PlayerTeam> getPlayerTeams() {
      return this.teamsByName.values();
   }

   @Nullable
   public PlayerTeam getPlayersTeam(String pUsername) {
      return (PlayerTeam)this.teamsByPlayer.get(pUsername);
   }

   public void onObjectiveAdded(Objective pObjective) {
   }

   public void onObjectiveChanged(Objective pObjective) {
   }

   public void onObjectiveRemoved(Objective pObjective) {
   }

   protected void onScoreChanged(ScoreHolder pScoreHolder, Objective pObjective, Score pScore) {
   }

   protected void onScoreLockChanged(ScoreHolder pScoreHolder, Objective pObjective) {
   }

   public void onPlayerRemoved(ScoreHolder pScoreHolder) {
   }

   public void onPlayerScoreRemoved(ScoreHolder pScoreHolder, Objective pObjective) {
   }

   public void onTeamAdded(PlayerTeam pPlayerTeam) {
   }

   public void onTeamChanged(PlayerTeam pPlayerTeam) {
   }

   public void onTeamRemoved(PlayerTeam pPlayerTeam) {
   }

   public void entityRemoved(Entity pEntity) {
      if (!(pEntity instanceof Player) && !pEntity.isAlive()) {
         this.resetAllPlayerScores(pEntity);
         this.removePlayerFromTeam(pEntity.getScoreboardName());
      }
   }

   protected ListTag savePlayerScores(HolderLookup.Provider pLevelRegistry) {
      ListTag $$1 = new ListTag();
      this.playerScores.forEach((p_327666_, p_327667_) -> p_327667_.listRawScores().forEach((p_327662_, p_327663_) -> {
            CompoundTag $$5 = p_327663_.write(pLevelRegistry);
            $$5.putString("Name", p_327666_);
            $$5.putString("Objective", p_327662_.getName());
            $$1.add($$5);
         }));
      return $$1;
   }

   protected void loadPlayerScores(ListTag pTag, HolderLookup.Provider pLevelRegistry) {
      for (int $$2 = 0; $$2 < pTag.size(); $$2++) {
         CompoundTag $$3 = pTag.getCompound($$2);
         Score $$4 = Score.read($$3, pLevelRegistry);
         String $$5 = $$3.getString("Name");
         String $$6 = $$3.getString("Objective");
         Objective $$7 = this.getObjective($$6);
         if ($$7 == null) {
            LOGGER.error("Unknown objective {} for name {}, ignoring", $$6, $$5);
         } else {
            this.getOrCreatePlayerInfo($$5).setScore($$7, $$4);
         }
      }
   }
}
