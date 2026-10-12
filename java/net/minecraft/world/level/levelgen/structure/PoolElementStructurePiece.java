package net.minecraft.world.level.levelgen.structure;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.pools.JigsawJunction;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.slf4j.Logger;

public class PoolElementStructurePiece extends StructurePiece {
   private static final Logger LOGGER = LogUtils.getLogger();
   protected final StructurePoolElement element;
   protected BlockPos position;
   private final int groundLevelDelta;
   protected final Rotation rotation;
   private final List<JigsawJunction> junctions = Lists.newArrayList();
   private final StructureTemplateManager structureTemplateManager;
   private final LiquidSettings liquidSettings;

   public PoolElementStructurePiece(
      StructureTemplateManager pStructureTemplateManager,
      StructurePoolElement pElement,
      BlockPos pPosition,
      int pGroundLevelDelta,
      Rotation pRotation,
      BoundingBox pBoundingBox,
      LiquidSettings pLiquidSettings
   ) {
      super(StructurePieceType.JIGSAW, 0, pBoundingBox);
      this.structureTemplateManager = pStructureTemplateManager;
      this.element = pElement;
      this.position = pPosition;
      this.groundLevelDelta = pGroundLevelDelta;
      this.rotation = pRotation;
      this.liquidSettings = pLiquidSettings;
   }

   public PoolElementStructurePiece(StructurePieceSerializationContext pContext, CompoundTag pTag) {
      super(StructurePieceType.JIGSAW, pTag);
      this.structureTemplateManager = pContext.structureTemplateManager();
      this.position = new BlockPos(pTag.getInt("PosX"), pTag.getInt("PosY"), pTag.getInt("PosZ"));
      this.groundLevelDelta = pTag.getInt("ground_level_delta");
      DynamicOps<Tag> $$2 = pContext.registryAccess().createSerializationContext(NbtOps.INSTANCE);
      this.element = (StructurePoolElement)StructurePoolElement.CODEC
         .parse($$2, pTag.getCompound("pool_element"))
         .getPartialOrThrow(p_341909_ -> new IllegalStateException("Invalid pool element found: " + p_341909_));
      this.rotation = Rotation.valueOf(pTag.getString("rotation"));
      this.boundingBox = this.element.getBoundingBox(this.structureTemplateManager, this.position, this.rotation);
      ListTag $$3 = pTag.getList("junctions", 10);
      this.junctions.clear();
      $$3.forEach(p_204943_ -> this.junctions.add(JigsawJunction.deserialize(new Dynamic($$2, p_204943_))));
      this.liquidSettings = LiquidSettings.CODEC.parse(NbtOps.INSTANCE, pTag.get("liquid_settings")).result().orElse(JigsawStructure.DEFAULT_LIQUID_SETTINGS);
   }

   @Override
   protected void addAdditionalSaveData(StructurePieceSerializationContext pContext, CompoundTag pTag) {
      pTag.putInt("PosX", this.position.getX());
      pTag.putInt("PosY", this.position.getY());
      pTag.putInt("PosZ", this.position.getZ());
      pTag.putInt("ground_level_delta", this.groundLevelDelta);
      DynamicOps<Tag> $$2 = pContext.registryAccess().createSerializationContext(NbtOps.INSTANCE);
      StructurePoolElement.CODEC.encodeStart($$2, this.element).resultOrPartial(LOGGER::error).ifPresent(p_163125_ -> pTag.put("pool_element", p_163125_));
      pTag.putString("rotation", this.rotation.name());
      ListTag $$3 = new ListTag();

      for (JigsawJunction $$4 : this.junctions) {
         $$3.add((Tag)$$4.serialize($$2).getValue());
      }

      pTag.put("junctions", $$3);
      if (this.liquidSettings != JigsawStructure.DEFAULT_LIQUID_SETTINGS) {
         pTag.put("liquid_settings", (Tag)LiquidSettings.CODEC.encodeStart(NbtOps.INSTANCE, this.liquidSettings).getOrThrow());
      }
   }

   @Override
   public void postProcess(
      WorldGenLevel pLevel,
      StructureManager pStructureManager,
      ChunkGenerator pGenerator,
      RandomSource pRandom,
      BoundingBox pBox,
      ChunkPos pChunkPos,
      BlockPos pPos
   ) {
      this.place(pLevel, pStructureManager, pGenerator, pRandom, pBox, pPos, false);
   }

   public void place(
      WorldGenLevel pLevel,
      StructureManager pStructureManager,
      ChunkGenerator pGenerator,
      RandomSource pRandom,
      BoundingBox pBox,
      BlockPos pPos,
      boolean pKeepJigsaws
   ) {
      this.element
         .place(
            this.structureTemplateManager,
            pLevel,
            pStructureManager,
            pGenerator,
            this.position,
            pPos,
            this.rotation,
            pBox,
            pRandom,
            this.liquidSettings,
            pKeepJigsaws
         );
   }

   @Override
   public void move(int pX, int pY, int pZ) {
      super.move(pX, pY, pZ);
      this.position = this.position.offset(pX, pY, pZ);
   }

   @Override
   public Rotation getRotation() {
      return this.rotation;
   }

   @Override
   public String toString() {
      return String.format(Locale.ROOT, "<%s | %s | %s | %s>", this.getClass().getSimpleName(), this.position, this.rotation, this.element);
   }

   public StructurePoolElement getElement() {
      return this.element;
   }

   public BlockPos getPosition() {
      return this.position;
   }

   public int getGroundLevelDelta() {
      return this.groundLevelDelta;
   }

   public void addJunction(JigsawJunction pJunction) {
      this.junctions.add(pJunction);
   }

   public List<JigsawJunction> getJunctions() {
      return this.junctions;
   }
}
