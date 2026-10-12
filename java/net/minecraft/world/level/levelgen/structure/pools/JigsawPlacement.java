package net.minecraft.world.level.levelgen.structure.pools;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.Pools;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.SequencedPriorityIterator;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.block.JigsawBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.commons.lang3.mutable.MutableObject;
import org.slf4j.Logger;

public class JigsawPlacement {
   static final Logger LOGGER = LogUtils.getLogger();

   public static Optional<Structure.GenerationStub> addPieces(
      Structure.GenerationContext pContext,
      Holder<StructureTemplatePool> pStartPool,
      Optional<ResourceLocation> pStartJigsawName,
      int pMaxDepth,
      BlockPos pPos,
      boolean pUseExpansionHack,
      Optional<Heightmap.Types> pProjectStartToHeightmap,
      int pMaxDistanceFromCenter,
      PoolAliasLookup pAliasLookup,
      DimensionPadding pDimensionPadding,
      LiquidSettings pLiquidSettings
   ) {
      RegistryAccess $$11 = pContext.registryAccess();
      ChunkGenerator $$12 = pContext.chunkGenerator();
      StructureTemplateManager $$13 = pContext.structureTemplateManager();
      LevelHeightAccessor $$14 = pContext.heightAccessor();
      WorldgenRandom $$15 = pContext.random();
      Registry<StructureTemplatePool> $$16 = $$11.registryOrThrow(Registries.TEMPLATE_POOL);
      Rotation $$17 = Rotation.getRandom($$15);
      StructureTemplatePool $$18 = pStartPool.unwrapKey()
         .flatMap(p_309329_ -> $$16.getOptional(pAliasLookup.lookup((ResourceKey<StructureTemplatePool>)p_309329_)))
         .orElse(pStartPool.value());
      StructurePoolElement $$19 = $$18.getRandomTemplate($$15);
      if ($$19 == EmptyPoolElement.INSTANCE) {
         return Optional.empty();
      } else {
         BlockPos $$22;
         if (pStartJigsawName.isPresent()) {
            ResourceLocation $$20 = pStartJigsawName.get();
            Optional<BlockPos> $$21 = getRandomNamedJigsaw($$19, $$20, pPos, $$17, $$13, $$15);
            if ($$21.isEmpty()) {
               LOGGER.error(
                  "No starting jigsaw {} found in start pool {}",
                  $$20,
                  pStartPool.unwrapKey().map(p_248484_ -> p_248484_.location().toString()).orElse("<unregistered>")
               );
               return Optional.empty();
            }

            $$22 = $$21.get();
         } else {
            $$22 = pPos;
         }

         Vec3i $$24 = $$22.subtract(pPos);
         BlockPos $$25 = pPos.subtract($$24);
         PoolElementStructurePiece $$26 = new PoolElementStructurePiece(
            $$13, $$19, $$25, $$19.getGroundLevelDelta(), $$17, $$19.getBoundingBox($$13, $$25, $$17), pLiquidSettings
         );
         BoundingBox $$27 = $$26.getBoundingBox();
         int $$28 = ($$27.maxX() + $$27.minX()) / 2;
         int $$29 = ($$27.maxZ() + $$27.minZ()) / 2;
         int $$30;
         if (pProjectStartToHeightmap.isPresent()) {
            $$30 = pPos.getY() + $$12.getFirstFreeHeight($$28, $$29, pProjectStartToHeightmap.get(), $$14, pContext.randomState());
         } else {
            $$30 = $$25.getY();
         }

         int $$32 = $$27.minY() + $$26.getGroundLevelDelta();
         $$26.move(0, $$30 - $$32, 0);
         int $$33 = $$30 + $$24.getY();
         return Optional.of(
            new Structure.GenerationStub(
               new BlockPos($$28, $$33, $$29),
               (Consumer<StructurePiecesBuilder>)(p_341930_ -> {
                  List<PoolElementStructurePiece> $$18x = Lists.newArrayList();
                  $$18x.add($$26);
                  if (pMaxDepth > 0) {
                     AABB $$19x = new AABB(
                        (double)($$28 - pMaxDistanceFromCenter),
                        (double)Math.max($$33 - pMaxDistanceFromCenter, $$14.getMinBuildHeight() + pDimensionPadding.bottom()),
                        (double)($$29 - pMaxDistanceFromCenter),
                        (double)($$28 + pMaxDistanceFromCenter + 1),
                        (double)Math.min($$33 + pMaxDistanceFromCenter + 1, $$14.getMaxBuildHeight() - pDimensionPadding.top()),
                        (double)($$29 + pMaxDistanceFromCenter + 1)
                     );
                     VoxelShape $$20 = Shapes.join(Shapes.create($$19x), Shapes.create(AABB.of($$27)), BooleanOp.ONLY_FIRST);
                     addPieces(
                        pContext.randomState(), pMaxDepth, pUseExpansionHack, $$12, $$13, $$14, $$15, $$16, $$26, $$18x, $$20, pAliasLookup, pLiquidSettings
                     );
                     $$18x.forEach(p_341930_::addPiece);
                  }
               })
            )
         );
      }
   }

   private static Optional<BlockPos> getRandomNamedJigsaw(
      StructurePoolElement pElement,
      ResourceLocation pStartJigsawName,
      BlockPos pPos,
      Rotation pRotation,
      StructureTemplateManager pStructureTemplateManager,
      WorldgenRandom pRandom
   ) {
      List<StructureTemplate.StructureBlockInfo> $$6 = pElement.getShuffledJigsawBlocks(pStructureTemplateManager, pPos, pRotation, pRandom);
      Optional<BlockPos> $$7 = Optional.empty();

      for (StructureTemplate.StructureBlockInfo $$8 : $$6) {
         ResourceLocation $$9 = ResourceLocation.tryParse(Objects.requireNonNull($$8.nbt(), () -> $$8 + " nbt was null").getString("name"));
         if (pStartJigsawName.equals($$9)) {
            $$7 = Optional.of($$8.pos());
            break;
         }
      }

      return $$7;
   }

   private static void addPieces(
      RandomState pRandomState,
      int pMaxDepth,
      boolean pUseExpansionHack,
      ChunkGenerator pChunkGenerator,
      StructureTemplateManager pStructureTemplateManager,
      LevelHeightAccessor pLevel,
      RandomSource pRandom,
      Registry<StructureTemplatePool> pPools,
      PoolElementStructurePiece pStartPiece,
      List<PoolElementStructurePiece> pPieces,
      VoxelShape pFree,
      PoolAliasLookup pAliasLookup,
      LiquidSettings pLiquidSettings
   ) {
      JigsawPlacement.Placer $$13 = new JigsawPlacement.Placer(pPools, pMaxDepth, pChunkGenerator, pStructureTemplateManager, pPieces, pRandom);
      $$13.tryPlacingChildren(pStartPiece, new MutableObject(pFree), 0, pUseExpansionHack, pLevel, pRandomState, pAliasLookup, pLiquidSettings);

      while ($$13.placing.hasNext()) {
         JigsawPlacement.PieceState $$14 = (JigsawPlacement.PieceState)$$13.placing.next();
         $$13.tryPlacingChildren($$14.piece, $$14.free, $$14.depth, pUseExpansionHack, pLevel, pRandomState, pAliasLookup, pLiquidSettings);
      }
   }

   public static boolean generateJigsaw(
      ServerLevel pLevel, Holder<StructureTemplatePool> pStartPool, ResourceLocation pStartJigsawName, int pMaxDepth, BlockPos pPos, boolean pKeepJigsaws
   ) {
      ChunkGenerator $$6 = pLevel.getChunkSource().getGenerator();
      StructureTemplateManager $$7 = pLevel.getStructureManager();
      StructureManager $$8 = pLevel.structureManager();
      RandomSource $$9 = pLevel.getRandom();
      Structure.GenerationContext $$10 = new Structure.GenerationContext(
         pLevel.registryAccess(),
         $$6,
         $$6.getBiomeSource(),
         pLevel.getChunkSource().randomState(),
         $$7,
         pLevel.getSeed(),
         new ChunkPos(pPos),
         pLevel,
         p_227255_ -> true
      );
      Optional<Structure.GenerationStub> $$11 = addPieces(
         $$10,
         pStartPool,
         Optional.of(pStartJigsawName),
         pMaxDepth,
         pPos,
         false,
         Optional.empty(),
         128,
         PoolAliasLookup.EMPTY,
         JigsawStructure.DEFAULT_DIMENSION_PADDING,
         JigsawStructure.DEFAULT_LIQUID_SETTINGS
      );
      if ($$11.isPresent()) {
         StructurePiecesBuilder $$12 = $$11.get().getPiecesBuilder();

         for (StructurePiece $$13 : $$12.build().pieces()) {
            if ($$13 instanceof PoolElementStructurePiece $$14) {
               $$14.place(pLevel, $$8, $$6, $$9, BoundingBox.infinite(), pPos, pKeepJigsaws);
            }
         }

         return true;
      } else {
         return false;
      }
   }

   static record PieceState(PoolElementStructurePiece piece, MutableObject<VoxelShape> free, int depth) {
   }

   static final class Placer {
      private final Registry<StructureTemplatePool> pools;
      private final int maxDepth;
      private final ChunkGenerator chunkGenerator;
      private final StructureTemplateManager structureTemplateManager;
      private final List<? super PoolElementStructurePiece> pieces;
      private final RandomSource random;
      final SequencedPriorityIterator<JigsawPlacement.PieceState> placing = new SequencedPriorityIterator<>();

      Placer(
         Registry<StructureTemplatePool> pPools,
         int pMaxDepth,
         ChunkGenerator pChunkGenerator,
         StructureTemplateManager pStructureTemplateManager,
         List<? super PoolElementStructurePiece> pPieces,
         RandomSource pRandom
      ) {
         this.pools = pPools;
         this.maxDepth = pMaxDepth;
         this.chunkGenerator = pChunkGenerator;
         this.structureTemplateManager = pStructureTemplateManager;
         this.pieces = pPieces;
         this.random = pRandom;
      }

      void tryPlacingChildren(
         PoolElementStructurePiece pPiece,
         MutableObject<VoxelShape> pFree,
         int pDepth,
         boolean pUseExpansionHack,
         LevelHeightAccessor pLevel,
         RandomState pRandom,
         PoolAliasLookup pPoolAliasLookup,
         LiquidSettings pLiquidSettings
      ) {
         StructurePoolElement $$8 = pPiece.getElement();
         BlockPos $$9 = pPiece.getPosition();
         Rotation $$10 = pPiece.getRotation();
         StructureTemplatePool.Projection $$11 = $$8.getProjection();
         boolean $$12 = $$11 == StructureTemplatePool.Projection.RIGID;
         MutableObject<VoxelShape> $$13 = new MutableObject();
         BoundingBox $$14 = pPiece.getBoundingBox();
         int $$15 = $$14.minY();

         label134:
         for (StructureTemplate.StructureBlockInfo $$16 : $$8.getShuffledJigsawBlocks(this.structureTemplateManager, $$9, $$10, this.random)) {
            Direction $$17 = JigsawBlock.getFrontFacing($$16.state());
            BlockPos $$18 = $$16.pos();
            BlockPos $$19 = $$18.relative($$17);
            int $$20 = $$18.getY() - $$15;
            int $$21 = -1;
            ResourceKey<StructureTemplatePool> $$22 = readPoolKey($$16, pPoolAliasLookup);
            Optional<? extends Holder<StructureTemplatePool>> $$23 = this.pools.getHolder($$22);
            if ($$23.isEmpty()) {
               JigsawPlacement.LOGGER.warn("Empty or non-existent pool: {}", $$22.location());
            } else {
               Holder<StructureTemplatePool> $$24 = (Holder<StructureTemplatePool>)$$23.get();
               if ($$24.value().size() == 0 && !$$24.is(Pools.EMPTY)) {
                  JigsawPlacement.LOGGER.warn("Empty or non-existent pool: {}", $$22.location());
               } else {
                  Holder<StructureTemplatePool> $$25 = $$24.value().getFallback();
                  if ($$25.value().size() == 0 && !$$25.is(Pools.EMPTY)) {
                     JigsawPlacement.LOGGER
                        .warn(
                           "Empty or non-existent fallback pool: {}",
                           $$25.unwrapKey().map(p_255599_ -> p_255599_.location().toString()).orElse("<unregistered>")
                        );
                  } else {
                     boolean $$26 = $$14.isInside($$19);
                     MutableObject<VoxelShape> $$27;
                     if ($$26) {
                        $$27 = $$13;
                        if ($$13.getValue() == null) {
                           $$13.setValue(Shapes.create(AABB.of($$14)));
                        }
                     } else {
                        $$27 = pFree;
                     }

                     List<StructurePoolElement> $$29 = Lists.newArrayList();
                     if (pDepth != this.maxDepth) {
                        $$29.addAll($$24.value().getShuffledTemplates(this.random));
                     }

                     $$29.addAll($$25.value().getShuffledTemplates(this.random));
                     int $$30 = $$16.nbt() != null ? $$16.nbt().getInt("placement_priority") : 0;

                     for (StructurePoolElement $$31 : $$29) {
                        if ($$31 == EmptyPoolElement.INSTANCE) {
                           break;
                        }

                        for (Rotation $$32 : Rotation.getShuffled(this.random)) {
                           List<StructureTemplate.StructureBlockInfo> $$33 = $$31.getShuffledJigsawBlocks(
                              this.structureTemplateManager, BlockPos.ZERO, $$32, this.random
                           );
                           BoundingBox $$34 = $$31.getBoundingBox(this.structureTemplateManager, BlockPos.ZERO, $$32);
                           int $$36;
                           if (pUseExpansionHack && $$34.getYSpan() <= 16) {
                              $$36 = $$33.stream().mapToInt(p_255598_ -> {
                                 if (!$$34.isInside(p_255598_.pos().relative(JigsawBlock.getFrontFacing(p_255598_.state())))) {
                                    return 0;
                                 } else {
                                    ResourceKey<StructureTemplatePool> $$3 = readPoolKey(p_255598_, pPoolAliasLookup);
                                    Optional<? extends Holder<StructureTemplatePool>> $$4 = this.pools.getHolder($$3);
                                    Optional<Holder<StructureTemplatePool>> $$5 = $$4.map(p_255600_ -> p_255600_.value().getFallback());
                                    int $$6 = $$4.<Integer>map(p_255596_ -> p_255596_.value().getMaxSize(this.structureTemplateManager)).orElse(0);
                                    int $$7 = $$5.<Integer>map(p_255601_ -> p_255601_.value().getMaxSize(this.structureTemplateManager)).orElse(0);
                                    return Math.max($$6, $$7);
                                 }
                              }).max().orElse(0);
                           } else {
                              $$36 = 0;
                           }

                           for (StructureTemplate.StructureBlockInfo $$37 : $$33) {
                              if (JigsawBlock.canAttach($$16, $$37)) {
                                 BlockPos $$38 = $$37.pos();
                                 BlockPos $$39 = $$19.subtract($$38);
                                 BoundingBox $$40 = $$31.getBoundingBox(this.structureTemplateManager, $$39, $$32);
                                 int $$41 = $$40.minY();
                                 StructureTemplatePool.Projection $$42 = $$31.getProjection();
                                 boolean $$43 = $$42 == StructureTemplatePool.Projection.RIGID;
                                 int $$44 = $$38.getY();
                                 int $$45 = $$20 - $$44 + JigsawBlock.getFrontFacing($$16.state()).getStepY();
                                 int $$46;
                                 if ($$12 && $$43) {
                                    $$46 = $$15 + $$45;
                                 } else {
                                    if ($$21 == -1) {
                                       $$21 = this.chunkGenerator
                                          .getFirstFreeHeight($$18.getX(), $$18.getZ(), Heightmap.Types.WORLD_SURFACE_WG, pLevel, pRandom);
                                    }

                                    $$46 = $$21 - $$44;
                                 }

                                 int $$48 = $$46 - $$41;
                                 BoundingBox $$49 = $$40.moved(0, $$48, 0);
                                 BlockPos $$50 = $$39.offset(0, $$48, 0);
                                 if ($$36 > 0) {
                                    int $$51 = Math.max($$36 + 1, $$49.maxY() - $$49.minY());
                                    $$49.encapsulate(new BlockPos($$49.minX(), $$49.minY() + $$51, $$49.minZ()));
                                 }

                                 if (!Shapes.joinIsNotEmpty((VoxelShape)$$27.getValue(), Shapes.create(AABB.of($$49).deflate(0.25)), BooleanOp.ONLY_SECOND)) {
                                    $$27.setValue(Shapes.joinUnoptimized((VoxelShape)$$27.getValue(), Shapes.create(AABB.of($$49)), BooleanOp.ONLY_FIRST));
                                    int $$52 = pPiece.getGroundLevelDelta();
                                    int $$53;
                                    if ($$43) {
                                       $$53 = $$52 - $$45;
                                    } else {
                                       $$53 = $$31.getGroundLevelDelta();
                                    }

                                    PoolElementStructurePiece $$55 = new PoolElementStructurePiece(
                                       this.structureTemplateManager, $$31, $$50, $$53, $$32, $$49, pLiquidSettings
                                    );
                                    int $$56;
                                    if ($$12) {
                                       $$56 = $$15 + $$20;
                                    } else if ($$43) {
                                       $$56 = $$46 + $$44;
                                    } else {
                                       if ($$21 == -1) {
                                          $$21 = this.chunkGenerator
                                             .getFirstFreeHeight($$18.getX(), $$18.getZ(), Heightmap.Types.WORLD_SURFACE_WG, pLevel, pRandom);
                                       }

                                       $$56 = $$21 + $$45 / 2;
                                    }

                                    pPiece.addJunction(new JigsawJunction($$19.getX(), $$56 - $$20 + $$52, $$19.getZ(), $$45, $$42));
                                    $$55.addJunction(new JigsawJunction($$18.getX(), $$56 - $$44 + $$53, $$18.getZ(), -$$45, $$11));
                                    this.pieces.add($$55);
                                    if (pDepth + 1 <= this.maxDepth) {
                                       JigsawPlacement.PieceState $$59 = new JigsawPlacement.PieceState($$55, $$27, pDepth + 1);
                                       this.placing.add($$59, $$30);
                                    }
                                    continue label134;
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      private static ResourceKey<StructureTemplatePool> readPoolKey(StructureTemplate.StructureBlockInfo pBlockInfo, PoolAliasLookup pAliasLookup) {
         CompoundTag $$2 = Objects.requireNonNull(pBlockInfo.nbt(), () -> pBlockInfo + " nbt was null");
         ResourceKey<StructureTemplatePool> $$3 = Pools.parseKey($$2.getString("pool"));
         return pAliasLookup.lookup($$3);
      }
   }
}
