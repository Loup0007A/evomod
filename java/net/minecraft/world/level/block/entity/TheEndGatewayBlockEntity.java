package net.minecraft.world.level.block.entity;

import com.mojang.logging.LogUtils;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.EndFeatures;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.EndGatewayConfiguration;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public class TheEndGatewayBlockEntity extends TheEndPortalBlockEntity {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int SPAWN_TIME = 200;
   private static final int COOLDOWN_TIME = 40;
   private static final int ATTENTION_INTERVAL = 2400;
   private static final int EVENT_COOLDOWN = 1;
   private static final int GATEWAY_HEIGHT_ABOVE_SURFACE = 10;
   private long age;
   private int teleportCooldown;
   @Nullable
   private BlockPos exitPortal;
   private boolean exactTeleport;

   public TheEndGatewayBlockEntity(BlockPos pPos, BlockState pBlockState) {
      super(BlockEntityType.END_GATEWAY, pPos, pBlockState);
   }

   @Override
   protected void saveAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
      super.saveAdditional(pTag, pRegistries);
      pTag.putLong("Age", this.age);
      if (this.exitPortal != null) {
         pTag.put("exit_portal", NbtUtils.writeBlockPos(this.exitPortal));
      }

      if (this.exactTeleport) {
         pTag.putBoolean("ExactTeleport", true);
      }
   }

   @Override
   protected void loadAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
      super.loadAdditional(pTag, pRegistries);
      this.age = pTag.getLong("Age");
      NbtUtils.readBlockPos(pTag, "exit_portal").filter(Level::isInSpawnableBounds).ifPresent(p_327323_ -> this.exitPortal = p_327323_);
      this.exactTeleport = pTag.getBoolean("ExactTeleport");
   }

   public static void beamAnimationTick(Level pLevel, BlockPos pPos, BlockState pState, TheEndGatewayBlockEntity pBlockEntity) {
      pBlockEntity.age++;
      if (pBlockEntity.isCoolingDown()) {
         pBlockEntity.teleportCooldown--;
      }
   }

   public static void portalTick(Level pLevel, BlockPos pPos, BlockState pState, TheEndGatewayBlockEntity pBlockEntity) {
      boolean $$4 = pBlockEntity.isSpawning();
      boolean $$5 = pBlockEntity.isCoolingDown();
      pBlockEntity.age++;
      if ($$5) {
         pBlockEntity.teleportCooldown--;
      } else if (pBlockEntity.age % 2400L == 0L) {
         triggerCooldown(pLevel, pPos, pState, pBlockEntity);
      }

      if ($$4 != pBlockEntity.isSpawning() || $$5 != pBlockEntity.isCoolingDown()) {
         setChanged(pLevel, pPos, pState);
      }
   }

   public boolean isSpawning() {
      return this.age < 200L;
   }

   public boolean isCoolingDown() {
      return this.teleportCooldown > 0;
   }

   public float getSpawnPercent(float pPartialTicks) {
      return Mth.clamp(((float)this.age + pPartialTicks) / 200.0F, 0.0F, 1.0F);
   }

   public float getCooldownPercent(float pPartialTicks) {
      return 1.0F - Mth.clamp(((float)this.teleportCooldown - pPartialTicks) / 40.0F, 0.0F, 1.0F);
   }

   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   @Override
   public CompoundTag getUpdateTag(HolderLookup.Provider pRegistries) {
      return this.saveCustomOnly(pRegistries);
   }

   public static void triggerCooldown(Level pLevel, BlockPos pPos, BlockState pState, TheEndGatewayBlockEntity pBlockEntity) {
      if (!pLevel.isClientSide) {
         pBlockEntity.teleportCooldown = 40;
         pLevel.blockEvent(pPos, pState.getBlock(), 1, 0);
         setChanged(pLevel, pPos, pState);
      }
   }

   @Override
   public boolean triggerEvent(int pId, int pType) {
      if (pId == 1) {
         this.teleportCooldown = 40;
         return true;
      } else {
         return super.triggerEvent(pId, pType);
      }
   }

   @Nullable
   public Vec3 getPortalPosition(ServerLevel pLevel, BlockPos pPos) {
      if (this.exitPortal == null && pLevel.dimension() == Level.END) {
         BlockPos $$2 = findOrCreateValidTeleportPos(pLevel, pPos);
         $$2 = $$2.above(10);
         LOGGER.debug("Creating portal at {}", $$2);
         spawnGatewayPortal(pLevel, $$2, EndGatewayConfiguration.knownExit(pPos, false));
         this.setExitPosition($$2, this.exactTeleport);
      }

      if (this.exitPortal != null) {
         BlockPos $$3 = this.exactTeleport ? this.exitPortal : findExitPosition(pLevel, this.exitPortal);
         return $$3.getBottomCenter();
      } else {
         return null;
      }
   }

   private static BlockPos findExitPosition(Level pLevel, BlockPos pPos) {
      BlockPos $$2 = findTallestBlock(pLevel, pPos.offset(0, 2, 0), 5, false);
      LOGGER.debug("Best exit position for portal at {} is {}", pPos, $$2);
      return $$2.above();
   }

   private static BlockPos findOrCreateValidTeleportPos(ServerLevel pLevel, BlockPos pPos) {
      Vec3 $$2 = findExitPortalXZPosTentative(pLevel, pPos);
      LevelChunk $$3 = getChunk(pLevel, $$2);
      BlockPos $$4 = findValidSpawnInChunk($$3);
      if ($$4 == null) {
         BlockPos $$5 = BlockPos.containing($$2.x + 0.5, 75.0, $$2.z + 0.5);
         LOGGER.debug("Failed to find a suitable block to teleport to, spawning an island on {}", $$5);
         pLevel.registryAccess()
            .registry(Registries.CONFIGURED_FEATURE)
            .flatMap(p_258975_ -> p_258975_.getHolder(EndFeatures.END_ISLAND))
            .ifPresent(p_256040_ -> p_256040_.value().place(pLevel, pLevel.getChunkSource().getGenerator(), RandomSource.create($$5.asLong()), $$5));
         $$4 = $$5;
      } else {
         LOGGER.debug("Found suitable block to teleport to: {}", $$4);
      }

      return findTallestBlock(pLevel, $$4, 16, true);
   }

   private static Vec3 findExitPortalXZPosTentative(ServerLevel pLevel, BlockPos pPos) {
      Vec3 $$2 = new Vec3((double)pPos.getX(), 0.0, (double)pPos.getZ()).normalize();
      int $$3 = 1024;
      Vec3 $$4 = $$2.scale(1024.0);

      for (int $$5 = 16; !isChunkEmpty(pLevel, $$4) && $$5-- > 0; $$4 = $$4.add($$2.scale(-16.0))) {
         LOGGER.debug("Skipping backwards past nonempty chunk at {}", $$4);
      }

      for (int var6 = 16; isChunkEmpty(pLevel, $$4) && var6-- > 0; $$4 = $$4.add($$2.scale(16.0))) {
         LOGGER.debug("Skipping forward past empty chunk at {}", $$4);
      }

      LOGGER.debug("Found chunk at {}", $$4);
      return $$4;
   }

   private static boolean isChunkEmpty(ServerLevel pLevel, Vec3 pPos) {
      return getChunk(pLevel, pPos).getHighestFilledSectionIndex() == -1;
   }

   private static BlockPos findTallestBlock(BlockGetter pLevel, BlockPos pPos, int pRadius, boolean pAllowBedrock) {
      BlockPos $$4 = null;

      for (int $$5 = -pRadius; $$5 <= pRadius; $$5++) {
         for (int $$6 = -pRadius; $$6 <= pRadius; $$6++) {
            if ($$5 != 0 || $$6 != 0 || pAllowBedrock) {
               for (int $$7 = pLevel.getMaxBuildHeight() - 1; $$7 > ($$4 == null ? pLevel.getMinBuildHeight() : $$4.getY()); $$7--) {
                  BlockPos $$8 = new BlockPos(pPos.getX() + $$5, $$7, pPos.getZ() + $$6);
                  BlockState $$9 = pLevel.getBlockState($$8);
                  if ($$9.isCollisionShapeFullBlock(pLevel, $$8) && (pAllowBedrock || !$$9.is(Blocks.BEDROCK))) {
                     $$4 = $$8;
                     break;
                  }
               }
            }
         }
      }

      return $$4 == null ? pPos : $$4;
   }

   private static LevelChunk getChunk(Level pLevel, Vec3 pPos) {
      return pLevel.getChunk(Mth.floor(pPos.x / 16.0), Mth.floor(pPos.z / 16.0));
   }

   @Nullable
   private static BlockPos findValidSpawnInChunk(LevelChunk pChunk) {
      ChunkPos $$1 = pChunk.getPos();
      BlockPos $$2 = new BlockPos($$1.getMinBlockX(), 30, $$1.getMinBlockZ());
      int $$3 = pChunk.getHighestSectionPosition() + 16 - 1;
      BlockPos $$4 = new BlockPos($$1.getMaxBlockX(), $$3, $$1.getMaxBlockZ());
      BlockPos $$5 = null;
      double $$6 = 0.0;

      for (BlockPos $$7 : BlockPos.betweenClosed($$2, $$4)) {
         BlockState $$8 = pChunk.getBlockState($$7);
         BlockPos $$9 = $$7.above();
         BlockPos $$10 = $$7.above(2);
         if ($$8.is(Blocks.END_STONE)
            && !pChunk.getBlockState($$9).isCollisionShapeFullBlock(pChunk, $$9)
            && !pChunk.getBlockState($$10).isCollisionShapeFullBlock(pChunk, $$10)) {
            double $$11 = $$7.distToCenterSqr(0.0, 0.0, 0.0);
            if ($$5 == null || $$11 < $$6) {
               $$5 = $$7;
               $$6 = $$11;
            }
         }
      }

      return $$5;
   }

   private static void spawnGatewayPortal(ServerLevel pLevel, BlockPos pPos, EndGatewayConfiguration pConfig) {
      Feature.END_GATEWAY.place(pConfig, pLevel, pLevel.getChunkSource().getGenerator(), RandomSource.create(), pPos);
   }

   @Override
   public boolean shouldRenderFace(Direction pFace) {
      return Block.shouldRenderFace(this.getBlockState(), this.level, this.getBlockPos(), pFace, this.getBlockPos().relative(pFace));
   }

   public int getParticleAmount() {
      int $$0 = 0;

      for (Direction $$1 : Direction.values()) {
         $$0 += this.shouldRenderFace($$1) ? 1 : 0;
      }

      return $$0;
   }

   public void setExitPosition(BlockPos pExitPortal, boolean pExactTeleport) {
      this.exactTeleport = pExactTeleport;
      this.exitPortal = pExitPortal;
      this.setChanged();
   }
}
