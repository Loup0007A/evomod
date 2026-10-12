package net.minecraft.world;

import javax.annotation.Nullable;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

public interface RandomizableContainer extends Container {
   String LOOT_TABLE_TAG = "LootTable";
   String LOOT_TABLE_SEED_TAG = "LootTableSeed";

   @Nullable
   ResourceKey<LootTable> getLootTable();

   void setLootTable(@Nullable ResourceKey<LootTable> pLootTable);

   default void setLootTable(ResourceKey<LootTable> pLootTable, long pSeed) {
      this.setLootTable(pLootTable);
      this.setLootTableSeed(pSeed);
   }

   long getLootTableSeed();

   void setLootTableSeed(long pSeed);

   BlockPos getBlockPos();

   @Nullable
   Level getLevel();

   static void setBlockEntityLootTable(BlockGetter pLevel, RandomSource pRandom, BlockPos pPs, ResourceKey<LootTable> pLootTable) {
      if (pLevel.getBlockEntity(pPs) instanceof RandomizableContainer $$5) {
         $$5.setLootTable(pLootTable, pRandom.nextLong());
      }
   }

   default boolean tryLoadLootTable(CompoundTag pTag) {
      if (pTag.contains("LootTable", 8)) {
         this.setLootTable(ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.parse(pTag.getString("LootTable"))));
         if (pTag.contains("LootTableSeed", 4)) {
            this.setLootTableSeed(pTag.getLong("LootTableSeed"));
         } else {
            this.setLootTableSeed(0L);
         }

         return true;
      } else {
         return false;
      }
   }

   default boolean trySaveLootTable(CompoundTag pTag) {
      ResourceKey<LootTable> $$1 = this.getLootTable();
      if ($$1 == null) {
         return false;
      } else {
         pTag.putString("LootTable", $$1.location().toString());
         long $$2 = this.getLootTableSeed();
         if ($$2 != 0L) {
            pTag.putLong("LootTableSeed", $$2);
         }

         return true;
      }
   }

   default void unpackLootTable(@Nullable Player pPlayer) {
      Level $$1 = this.getLevel();
      BlockPos $$2 = this.getBlockPos();
      ResourceKey<LootTable> $$3 = this.getLootTable();
      if ($$3 != null && $$1 != null && $$1.getServer() != null) {
         LootTable $$4 = $$1.getServer().reloadableRegistries().getLootTable($$3);
         if (pPlayer instanceof ServerPlayer) {
            CriteriaTriggers.GENERATE_LOOT.trigger((ServerPlayer)pPlayer, $$3);
         }

         this.setLootTable(null);
         LootParams.Builder $$5 = new LootParams.Builder((ServerLevel)$$1).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf($$2));
         if (pPlayer != null) {
            $$5.withLuck(pPlayer.getLuck()).withParameter(LootContextParams.THIS_ENTITY, pPlayer);
         }

         $$4.fill(this, $$5.create(LootContextParamSets.CHEST), this.getLootTableSeed());
      }
   }
}
