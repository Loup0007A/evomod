package net.minecraft.world.scores;

import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.network.chat.numbers.NumberFormatTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.slf4j.Logger;

public class ScoreboardSaveData extends SavedData {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final String FILE_ID = "scoreboard";
   private final Scoreboard scoreboard;

   public ScoreboardSaveData(Scoreboard pScoreboard) {
      this.scoreboard = pScoreboard;
   }

   public ScoreboardSaveData load(CompoundTag pTag, HolderLookup.Provider pLevelRegistry) {
      this.loadObjectives(pTag.getList("Objectives", 10), pLevelRegistry);
      this.scoreboard.loadPlayerScores(pTag.getList("PlayerScores", 10), pLevelRegistry);
      if (pTag.contains("DisplaySlots", 10)) {
         this.loadDisplaySlots(pTag.getCompound("DisplaySlots"));
      }

      if (pTag.contains("Teams", 9)) {
         this.loadTeams(pTag.getList("Teams", 10), pLevelRegistry);
      }

      return this;
   }

   private void loadTeams(ListTag pTag, HolderLookup.Provider pLevelRegistry) {
      for (int $$2 = 0; $$2 < pTag.size(); $$2++) {
         CompoundTag $$3 = pTag.getCompound($$2);
         String $$4 = $$3.getString("Name");
         PlayerTeam $$5 = this.scoreboard.addPlayerTeam($$4);
         Component $$6 = Component.Serializer.fromJson($$3.getString("DisplayName"), pLevelRegistry);
         if ($$6 != null) {
            $$5.setDisplayName($$6);
         }

         if ($$3.contains("TeamColor", 8)) {
            $$5.setColor(ChatFormatting.getByName($$3.getString("TeamColor")));
         }

         if ($$3.contains("AllowFriendlyFire", 99)) {
            $$5.setAllowFriendlyFire($$3.getBoolean("AllowFriendlyFire"));
         }

         if ($$3.contains("SeeFriendlyInvisibles", 99)) {
            $$5.setSeeFriendlyInvisibles($$3.getBoolean("SeeFriendlyInvisibles"));
         }

         if ($$3.contains("MemberNamePrefix", 8)) {
            Component $$7 = Component.Serializer.fromJson($$3.getString("MemberNamePrefix"), pLevelRegistry);
            if ($$7 != null) {
               $$5.setPlayerPrefix($$7);
            }
         }

         if ($$3.contains("MemberNameSuffix", 8)) {
            Component $$8 = Component.Serializer.fromJson($$3.getString("MemberNameSuffix"), pLevelRegistry);
            if ($$8 != null) {
               $$5.setPlayerSuffix($$8);
            }
         }

         if ($$3.contains("NameTagVisibility", 8)) {
            Team.Visibility $$9 = Team.Visibility.byName($$3.getString("NameTagVisibility"));
            if ($$9 != null) {
               $$5.setNameTagVisibility($$9);
            }
         }

         if ($$3.contains("DeathMessageVisibility", 8)) {
            Team.Visibility $$10 = Team.Visibility.byName($$3.getString("DeathMessageVisibility"));
            if ($$10 != null) {
               $$5.setDeathMessageVisibility($$10);
            }
         }

         if ($$3.contains("CollisionRule", 8)) {
            Team.CollisionRule $$11 = Team.CollisionRule.byName($$3.getString("CollisionRule"));
            if ($$11 != null) {
               $$5.setCollisionRule($$11);
            }
         }

         this.loadTeamPlayers($$5, $$3.getList("Players", 8));
      }
   }

   private void loadTeamPlayers(PlayerTeam pPlayerTeam, ListTag pTagList) {
      for (int $$2 = 0; $$2 < pTagList.size(); $$2++) {
         this.scoreboard.addPlayerToTeam(pTagList.getString($$2), pPlayerTeam);
      }
   }

   private void loadDisplaySlots(CompoundTag pCompound) {
      for (String $$1 : pCompound.getAllKeys()) {
         DisplaySlot $$2 = DisplaySlot.CODEC.byName($$1);
         if ($$2 != null) {
            String $$3 = pCompound.getString($$1);
            Objective $$4 = this.scoreboard.getObjective($$3);
            this.scoreboard.setDisplayObjective($$2, $$4);
         }
      }
   }

   private void loadObjectives(ListTag pTag, HolderLookup.Provider pLevelRegistry) {
      for (int $$2 = 0; $$2 < pTag.size(); $$2++) {
         CompoundTag $$3 = pTag.getCompound($$2);
         String $$4 = $$3.getString("CriteriaName");
         ObjectiveCriteria $$5 = ObjectiveCriteria.byName($$4).orElseGet(() -> {
            LOGGER.warn("Unknown scoreboard criteria {}, replacing with {}", $$4, ObjectiveCriteria.DUMMY.getName());
            return ObjectiveCriteria.DUMMY;
         });
         String $$6 = $$3.getString("Name");
         Component $$7 = Component.Serializer.fromJson($$3.getString("DisplayName"), pLevelRegistry);
         ObjectiveCriteria.RenderType $$8 = ObjectiveCriteria.RenderType.byId($$3.getString("RenderType"));
         boolean $$9 = $$3.getBoolean("display_auto_update");
         NumberFormat $$10 = (NumberFormat)NumberFormatTypes.CODEC
            .parse(pLevelRegistry.createSerializationContext(NbtOps.INSTANCE), $$3.get("format"))
            .result()
            .orElse(null);
         this.scoreboard.addObjective($$6, $$5, $$7, $$8, $$9, $$10);
      }
   }

   @Override
   public CompoundTag save(CompoundTag pTag, HolderLookup.Provider pRegistries) {
      pTag.put("Objectives", this.saveObjectives(pRegistries));
      pTag.put("PlayerScores", this.scoreboard.savePlayerScores(pRegistries));
      pTag.put("Teams", this.saveTeams(pRegistries));
      this.saveDisplaySlots(pTag);
      return pTag;
   }

   private ListTag saveTeams(HolderLookup.Provider pLevelRegistry) {
      ListTag $$1 = new ListTag();

      for (PlayerTeam $$3 : this.scoreboard.getPlayerTeams()) {
         CompoundTag $$4 = new CompoundTag();
         $$4.putString("Name", $$3.getName());
         $$4.putString("DisplayName", Component.Serializer.toJson($$3.getDisplayName(), pLevelRegistry));
         if ($$3.getColor().getId() >= 0) {
            $$4.putString("TeamColor", $$3.getColor().getName());
         }

         $$4.putBoolean("AllowFriendlyFire", $$3.isAllowFriendlyFire());
         $$4.putBoolean("SeeFriendlyInvisibles", $$3.canSeeFriendlyInvisibles());
         $$4.putString("MemberNamePrefix", Component.Serializer.toJson($$3.getPlayerPrefix(), pLevelRegistry));
         $$4.putString("MemberNameSuffix", Component.Serializer.toJson($$3.getPlayerSuffix(), pLevelRegistry));
         $$4.putString("NameTagVisibility", $$3.getNameTagVisibility().name);
         $$4.putString("DeathMessageVisibility", $$3.getDeathMessageVisibility().name);
         $$4.putString("CollisionRule", $$3.getCollisionRule().name);
         ListTag $$5 = new ListTag();

         for (String $$6 : $$3.getPlayers()) {
            $$5.add(StringTag.valueOf($$6));
         }

         $$4.put("Players", $$5);
         $$1.add($$4);
      }

      return $$1;
   }

   private void saveDisplaySlots(CompoundTag pCompound) {
      CompoundTag $$1 = new CompoundTag();

      for (DisplaySlot $$2 : DisplaySlot.values()) {
         Objective $$3 = this.scoreboard.getDisplayObjective($$2);
         if ($$3 != null) {
            $$1.putString($$2.getSerializedName(), $$3.getName());
         }
      }

      if (!$$1.isEmpty()) {
         pCompound.put("DisplaySlots", $$1);
      }
   }

   private ListTag saveObjectives(HolderLookup.Provider pLevelRegistry) {
      ListTag $$1 = new ListTag();

      for (Objective $$3 : this.scoreboard.getObjectives()) {
         CompoundTag $$4 = new CompoundTag();
         $$4.putString("Name", $$3.getName());
         $$4.putString("CriteriaName", $$3.getCriteria().getName());
         $$4.putString("DisplayName", Component.Serializer.toJson($$3.getDisplayName(), pLevelRegistry));
         $$4.putString("RenderType", $$3.getRenderType().getId());
         $$4.putBoolean("display_auto_update", $$3.displayAutoUpdate());
         NumberFormat $$5 = $$3.numberFormat();
         if ($$5 != null) {
            NumberFormatTypes.CODEC
               .encodeStart(pLevelRegistry.createSerializationContext(NbtOps.INSTANCE), $$5)
               .ifSuccess(p_309378_ -> $$4.put("format", p_309378_));
         }

         $$1.add($$4);
      }

      return $$1;
   }
}
