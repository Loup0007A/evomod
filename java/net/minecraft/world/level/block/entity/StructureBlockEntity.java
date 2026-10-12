package net.minecraft.world.level.block.entity;

import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.ResourceLocationException;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.StructureBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.StructureMode;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockRotProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

public class StructureBlockEntity extends BlockEntity {
   private static final int SCAN_CORNER_BLOCKS_RANGE = 5;
   public static final int MAX_OFFSET_PER_AXIS = 48;
   public static final int MAX_SIZE_PER_AXIS = 48;
   public static final String AUTHOR_TAG = "author";
   @Nullable
   private ResourceLocation structureName;
   private String author = "";
   private String metaData = "";
   private BlockPos structurePos = new BlockPos(0, 1, 0);
   private Vec3i structureSize = Vec3i.ZERO;
   private Mirror mirror = Mirror.NONE;
   private Rotation rotation = Rotation.NONE;
   private StructureMode mode;
   private boolean ignoreEntities = true;
   private boolean powered;
   private boolean showAir;
   private boolean showBoundingBox = true;
   private float integrity = 1.0F;
   private long seed;

   public StructureBlockEntity(BlockPos pPos, BlockState pBlockState) {
      super(BlockEntityType.STRUCTURE_BLOCK, pPos, pBlockState);
      this.mode = pBlockState.getValue(StructureBlock.MODE);
   }

   @Override
   protected void saveAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
      super.saveAdditional(pTag, pRegistries);
      pTag.putString("name", this.getStructureName());
      pTag.putString("author", this.author);
      pTag.putString("metadata", this.metaData);
      pTag.putInt("posX", this.structurePos.getX());
      pTag.putInt("posY", this.structurePos.getY());
      pTag.putInt("posZ", this.structurePos.getZ());
      pTag.putInt("sizeX", this.structureSize.getX());
      pTag.putInt("sizeY", this.structureSize.getY());
      pTag.putInt("sizeZ", this.structureSize.getZ());
      pTag.putString("rotation", this.rotation.toString());
      pTag.putString("mirror", this.mirror.toString());
      pTag.putString("mode", this.mode.toString());
      pTag.putBoolean("ignoreEntities", this.ignoreEntities);
      pTag.putBoolean("powered", this.powered);
      pTag.putBoolean("showair", this.showAir);
      pTag.putBoolean("showboundingbox", this.showBoundingBox);
      pTag.putFloat("integrity", this.integrity);
      pTag.putLong("seed", this.seed);
   }

   @Override
   protected void loadAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
      super.loadAdditional(pTag, pRegistries);
      this.setStructureName(pTag.getString("name"));
      this.author = pTag.getString("author");
      this.metaData = pTag.getString("metadata");
      int $$2 = Mth.clamp(pTag.getInt("posX"), -48, 48);
      int $$3 = Mth.clamp(pTag.getInt("posY"), -48, 48);
      int $$4 = Mth.clamp(pTag.getInt("posZ"), -48, 48);
      this.structurePos = new BlockPos($$2, $$3, $$4);
      int $$5 = Mth.clamp(pTag.getInt("sizeX"), 0, 48);
      int $$6 = Mth.clamp(pTag.getInt("sizeY"), 0, 48);
      int $$7 = Mth.clamp(pTag.getInt("sizeZ"), 0, 48);
      this.structureSize = new Vec3i($$5, $$6, $$7);

      try {
         this.rotation = Rotation.valueOf(pTag.getString("rotation"));
      } catch (IllegalArgumentException var12) {
         this.rotation = Rotation.NONE;
      }

      try {
         this.mirror = Mirror.valueOf(pTag.getString("mirror"));
      } catch (IllegalArgumentException var11) {
         this.mirror = Mirror.NONE;
      }

      try {
         this.mode = StructureMode.valueOf(pTag.getString("mode"));
      } catch (IllegalArgumentException var10) {
         this.mode = StructureMode.DATA;
      }

      this.ignoreEntities = pTag.getBoolean("ignoreEntities");
      this.powered = pTag.getBoolean("powered");
      this.showAir = pTag.getBoolean("showair");
      this.showBoundingBox = pTag.getBoolean("showboundingbox");
      if (pTag.contains("integrity")) {
         this.integrity = pTag.getFloat("integrity");
      } else {
         this.integrity = 1.0F;
      }

      this.seed = pTag.getLong("seed");
      this.updateBlockState();
   }

   private void updateBlockState() {
      if (this.level != null) {
         BlockPos $$0 = this.getBlockPos();
         BlockState $$1 = this.level.getBlockState($$0);
         if ($$1.is(Blocks.STRUCTURE_BLOCK)) {
            this.level.setBlock($$0, $$1.setValue(StructureBlock.MODE, this.mode), 2);
         }
      }
   }

   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   @Override
   public CompoundTag getUpdateTag(HolderLookup.Provider pRegistries) {
      return this.saveCustomOnly(pRegistries);
   }

   public boolean usedBy(Player pPlayer) {
      if (!pPlayer.canUseGameMasterBlocks()) {
         return false;
      } else {
         if (pPlayer.getCommandSenderWorld().isClientSide) {
            pPlayer.openStructureBlock(this);
         }

         return true;
      }
   }

   public String getStructureName() {
      return this.structureName == null ? "" : this.structureName.toString();
   }

   public boolean hasStructureName() {
      return this.structureName != null;
   }

   public void setStructureName(@Nullable String pStructureName) {
      this.setStructureName(StringUtil.isNullOrEmpty(pStructureName) ? null : ResourceLocation.tryParse(pStructureName));
   }

   public void setStructureName(@Nullable ResourceLocation pStructureName) {
      this.structureName = pStructureName;
   }

   public void createdBy(LivingEntity pAuthor) {
      this.author = pAuthor.getName().getString();
   }

   public BlockPos getStructurePos() {
      return this.structurePos;
   }

   public void setStructurePos(BlockPos pStructurePos) {
      this.structurePos = pStructurePos;
   }

   public Vec3i getStructureSize() {
      return this.structureSize;
   }

   public void setStructureSize(Vec3i pStructureSize) {
      this.structureSize = pStructureSize;
   }

   public Mirror getMirror() {
      return this.mirror;
   }

   public void setMirror(Mirror pMirror) {
      this.mirror = pMirror;
   }

   public Rotation getRotation() {
      return this.rotation;
   }

   public void setRotation(Rotation pRotation) {
      this.rotation = pRotation;
   }

   public String getMetaData() {
      return this.metaData;
   }

   public void setMetaData(String pMetaData) {
      this.metaData = pMetaData;
   }

   public StructureMode getMode() {
      return this.mode;
   }

   public void setMode(StructureMode pMode) {
      this.mode = pMode;
      BlockState $$1 = this.level.getBlockState(this.getBlockPos());
      if ($$1.is(Blocks.STRUCTURE_BLOCK)) {
         this.level.setBlock(this.getBlockPos(), $$1.setValue(StructureBlock.MODE, pMode), 2);
      }
   }

   public boolean isIgnoreEntities() {
      return this.ignoreEntities;
   }

   public void setIgnoreEntities(boolean pIgnoreEntities) {
      this.ignoreEntities = pIgnoreEntities;
   }

   public float getIntegrity() {
      return this.integrity;
   }

   public void setIntegrity(float pIntegrity) {
      this.integrity = pIntegrity;
   }

   public long getSeed() {
      return this.seed;
   }

   public void setSeed(long pSeed) {
      this.seed = pSeed;
   }

   public boolean detectSize() {
      if (this.mode != StructureMode.SAVE) {
         return false;
      } else {
         BlockPos $$0 = this.getBlockPos();
         int $$1 = 80;
         BlockPos $$2 = new BlockPos($$0.getX() - 80, this.level.getMinBuildHeight(), $$0.getZ() - 80);
         BlockPos $$3 = new BlockPos($$0.getX() + 80, this.level.getMaxBuildHeight() - 1, $$0.getZ() + 80);
         Stream<BlockPos> $$4 = this.getRelatedCorners($$2, $$3);
         return calculateEnclosingBoundingBox($$0, $$4).filter(p_155790_ -> {
            int $$2x = p_155790_.maxX() - p_155790_.minX();
            int $$3x = p_155790_.maxY() - p_155790_.minY();
            int $$4x = p_155790_.maxZ() - p_155790_.minZ();
            if ($$2x > 1 && $$3x > 1 && $$4x > 1) {
               this.structurePos = new BlockPos(p_155790_.minX() - $$0.getX() + 1, p_155790_.minY() - $$0.getY() + 1, p_155790_.minZ() - $$0.getZ() + 1);
               this.structureSize = new Vec3i($$2x - 1, $$3x - 1, $$4x - 1);
               this.setChanged();
               BlockState $$5 = this.level.getBlockState($$0);
               this.level.sendBlockUpdated($$0, $$5, $$5, 3);
               return true;
            } else {
               return false;
            }
         }).isPresent();
      }
   }

   private Stream<BlockPos> getRelatedCorners(BlockPos pMinPos, BlockPos pMaxPos) {
      return BlockPos.betweenClosedStream(pMinPos, pMaxPos)
         .filter(p_272561_ -> this.level.getBlockState(p_272561_).is(Blocks.STRUCTURE_BLOCK))
         .map(this.level::getBlockEntity)
         .filter(p_155802_ -> p_155802_ instanceof StructureBlockEntity)
         .map(p_155785_ -> (StructureBlockEntity)p_155785_)
         .filter(p_155787_ -> p_155787_.mode == StructureMode.CORNER && Objects.equals(this.structureName, p_155787_.structureName))
         .map(BlockEntity::getBlockPos);
   }

   private static Optional<BoundingBox> calculateEnclosingBoundingBox(BlockPos pPos, Stream<BlockPos> pRelatedCorners) {
      Iterator<BlockPos> $$2 = pRelatedCorners.iterator();
      if (!$$2.hasNext()) {
         return Optional.empty();
      } else {
         BlockPos $$3 = $$2.next();
         BoundingBox $$4 = new BoundingBox($$3);
         if ($$2.hasNext()) {
            $$2.forEachRemaining($$4::encapsulate);
         } else {
            $$4.encapsulate(pPos);
         }

         return Optional.of($$4);
      }
   }

   public boolean saveStructure() {
      return this.mode != StructureMode.SAVE ? false : this.saveStructure(true);
   }

   public boolean saveStructure(boolean pWriteToDisk) {
      if (this.structureName == null) {
         return false;
      } else {
         BlockPos $$1 = this.getBlockPos().offset(this.structurePos);
         ServerLevel $$2 = (ServerLevel)this.level;
         StructureTemplateManager $$3 = $$2.getStructureManager();

         StructureTemplate $$4;
         try {
            $$4 = $$3.getOrCreate(this.structureName);
         } catch (ResourceLocationException var8) {
            return false;
         }

         $$4.fillFromWorld(this.level, $$1, this.structureSize, !this.ignoreEntities, Blocks.STRUCTURE_VOID);
         $$4.setAuthor(this.author);
         if (pWriteToDisk) {
            try {
               return $$3.save(this.structureName);
            } catch (ResourceLocationException var7) {
               return false;
            }
         } else {
            return true;
         }
      }
   }

   public static RandomSource createRandom(long pSeed) {
      return pSeed == 0L ? RandomSource.create(Util.getMillis()) : RandomSource.create(pSeed);
   }

   public boolean placeStructureIfSameSize(ServerLevel pLevel) {
      if (this.mode == StructureMode.LOAD && this.structureName != null) {
         StructureTemplate $$1 = pLevel.getStructureManager().get(this.structureName).orElse(null);
         if ($$1 == null) {
            return false;
         } else if ($$1.getSize().equals(this.structureSize)) {
            this.placeStructure(pLevel, $$1);
            return true;
         } else {
            this.loadStructureInfo($$1);
            return false;
         }
      } else {
         return false;
      }
   }

   public boolean loadStructureInfo(ServerLevel pLevel) {
      StructureTemplate $$1 = this.getStructureTemplate(pLevel);
      if ($$1 == null) {
         return false;
      } else {
         this.loadStructureInfo($$1);
         return true;
      }
   }

   private void loadStructureInfo(StructureTemplate pStructureTemplate) {
      this.author = !StringUtil.isNullOrEmpty(pStructureTemplate.getAuthor()) ? pStructureTemplate.getAuthor() : "";
      this.structureSize = pStructureTemplate.getSize();
      this.setChanged();
   }

   public void placeStructure(ServerLevel pLevel) {
      StructureTemplate $$1 = this.getStructureTemplate(pLevel);
      if ($$1 != null) {
         this.placeStructure(pLevel, $$1);
      }
   }

   @Nullable
   private StructureTemplate getStructureTemplate(ServerLevel pLevel) {
      return this.structureName == null ? null : pLevel.getStructureManager().get(this.structureName).orElse(null);
   }

   private void placeStructure(ServerLevel pLevel, StructureTemplate pStructureTemplate) {
      this.loadStructureInfo(pStructureTemplate);
      StructurePlaceSettings $$2 = new StructurePlaceSettings().setMirror(this.mirror).setRotation(this.rotation).setIgnoreEntities(this.ignoreEntities);
      if (this.integrity < 1.0F) {
         $$2.clearProcessors().addProcessor(new BlockRotProcessor(Mth.clamp(this.integrity, 0.0F, 1.0F))).setRandom(createRandom(this.seed));
      }

      BlockPos $$3 = this.getBlockPos().offset(this.structurePos);
      pStructureTemplate.placeInWorld(pLevel, $$3, $$3, $$2, createRandom(this.seed), 2);
   }

   public void unloadStructure() {
      if (this.structureName != null) {
         ServerLevel $$0 = (ServerLevel)this.level;
         StructureTemplateManager $$1 = $$0.getStructureManager();
         $$1.remove(this.structureName);
      }
   }

   public boolean isStructureLoadable() {
      if (this.mode == StructureMode.LOAD && !this.level.isClientSide && this.structureName != null) {
         ServerLevel $$0 = (ServerLevel)this.level;
         StructureTemplateManager $$1 = $$0.getStructureManager();

         try {
            return $$1.get(this.structureName).isPresent();
         } catch (ResourceLocationException var4) {
            return false;
         }
      } else {
         return false;
      }
   }

   public boolean isPowered() {
      return this.powered;
   }

   public void setPowered(boolean pPowered) {
      this.powered = pPowered;
   }

   public boolean getShowAir() {
      return this.showAir;
   }

   public void setShowAir(boolean pShowAir) {
      this.showAir = pShowAir;
   }

   public boolean getShowBoundingBox() {
      return this.showBoundingBox;
   }

   public void setShowBoundingBox(boolean pShowBoundingBox) {
      this.showBoundingBox = pShowBoundingBox;
   }

   public static enum UpdateType {
      UPDATE_DATA,
      SAVE_AREA,
      LOAD_AREA,
      SCAN_AREA;
   }
}
