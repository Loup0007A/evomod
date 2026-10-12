package net.minecraft.world.entity.raid;

import com.google.common.collect.Maps;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.PoiTypeTags;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

public class Raids extends SavedData {
   private static final String RAID_FILE_ID = "raids";
   private final Map<Integer, Raid> raidMap = Maps.newHashMap();
   private final ServerLevel level;
   private int nextAvailableID;
   private int tick;

   public static SavedData.Factory<Raids> factory(ServerLevel pLevel) {
      return new SavedData.Factory<>(() -> new Raids(pLevel), (p_296865_, p_334354_) -> load(pLevel, p_296865_), DataFixTypes.SAVED_DATA_RAIDS);
   }

   public Raids(ServerLevel pLevel) {
      this.level = pLevel;
      this.nextAvailableID = 1;
      this.setDirty();
   }

   public Raid get(int pId) {
      return this.raidMap.get(pId);
   }

   public void tick() {
      this.tick++;
      Iterator<Raid> $$0 = this.raidMap.values().iterator();

      while ($$0.hasNext()) {
         Raid $$1 = $$0.next();
         if (this.level.getGameRules().getBoolean(GameRules.RULE_DISABLE_RAIDS)) {
            $$1.stop();
         }

         if ($$1.isStopped()) {
            $$0.remove();
            this.setDirty();
         } else {
            $$1.tick();
         }
      }

      if (this.tick % 200 == 0) {
         this.setDirty();
      }

      DebugPackets.sendRaids(this.level, this.raidMap.values());
   }

   public static boolean canJoinRaid(Raider pRaider, Raid pRaid) {
      return pRaider != null && pRaid != null && pRaid.getLevel() != null
         ? pRaider.isAlive()
            && pRaider.canJoinRaid()
            && pRaider.getNoActionTime() <= 2400
            && pRaider.level().dimensionType() == pRaid.getLevel().dimensionType()
         : false;
   }

   @Nullable
   public Raid createOrExtendRaid(ServerPlayer pPlayer, BlockPos pPos) {
      if (pPlayer.isSpectator()) {
         return null;
      } else if (this.level.getGameRules().getBoolean(GameRules.RULE_DISABLE_RAIDS)) {
         return null;
      } else {
         DimensionType $$2 = pPlayer.level().dimensionType();
         if (!$$2.hasRaids()) {
            return null;
         } else {
            List<PoiRecord> $$3 = this.level
               .getPoiManager()
               .getInRange(p_219845_ -> p_219845_.is(PoiTypeTags.VILLAGE), pPos, 64, PoiManager.Occupancy.IS_OCCUPIED)
               .toList();
            int $$4 = 0;
            Vec3 $$5 = Vec3.ZERO;

            for (PoiRecord $$6 : $$3) {
               BlockPos $$7 = $$6.getPos();
               $$5 = $$5.add((double)$$7.getX(), (double)$$7.getY(), (double)$$7.getZ());
               $$4++;
            }

            BlockPos $$8;
            if ($$4 > 0) {
               $$5 = $$5.scale(1.0 / (double)$$4);
               $$8 = BlockPos.containing($$5);
            } else {
               $$8 = pPos;
            }

            Raid $$10 = this.getOrCreateRaid(pPlayer.serverLevel(), $$8);
            if (!$$10.isStarted() && !this.raidMap.containsKey($$10.getId())) {
               this.raidMap.put($$10.getId(), $$10);
            }

            if (!$$10.isStarted() || $$10.getRaidOmenLevel() < $$10.getMaxRaidOmenLevel()) {
               $$10.absorbRaidOmen(pPlayer);
            }

            this.setDirty();
            return $$10;
         }
      }
   }

   private Raid getOrCreateRaid(ServerLevel pServerLevel, BlockPos pPos) {
      Raid $$2 = pServerLevel.getRaidAt(pPos);
      return $$2 != null ? $$2 : new Raid(this.getUniqueId(), pServerLevel, pPos);
   }

   public static Raids load(ServerLevel pLevel, CompoundTag pTag) {
      Raids $$2 = new Raids(pLevel);
      $$2.nextAvailableID = pTag.getInt("NextAvailableID");
      $$2.tick = pTag.getInt("Tick");
      ListTag $$3 = pTag.getList("Raids", 10);

      for (int $$4 = 0; $$4 < $$3.size(); $$4++) {
         CompoundTag $$5 = $$3.getCompound($$4);
         Raid $$6 = new Raid(pLevel, $$5);
         $$2.raidMap.put($$6.getId(), $$6);
      }

      return $$2;
   }

   @Override
   public CompoundTag save(CompoundTag pTag, HolderLookup.Provider pRegistries) {
      pTag.putInt("NextAvailableID", this.nextAvailableID);
      pTag.putInt("Tick", this.tick);
      ListTag $$2 = new ListTag();

      for (Raid $$3 : this.raidMap.values()) {
         CompoundTag $$4 = new CompoundTag();
         $$3.save($$4);
         $$2.add($$4);
      }

      pTag.put("Raids", $$2);
      return pTag;
   }

   public static String getFileId(Holder<DimensionType> pDimensionTypeHolder) {
      return pDimensionTypeHolder.is(BuiltinDimensionTypes.END) ? "raids_end" : "raids";
   }

   private int getUniqueId() {
      return ++this.nextAvailableID;
   }

   @Nullable
   public Raid getNearbyRaid(BlockPos pPos, int pDistance) {
      Raid $$2 = null;
      double $$3 = (double)pDistance;

      for (Raid $$4 : this.raidMap.values()) {
         double $$5 = $$4.getCenter().distSqr(pPos);
         if ($$4.isActive() && $$5 < $$3) {
            $$2 = $$4;
            $$3 = $$5;
         }
      }

      return $$2;
   }
}
