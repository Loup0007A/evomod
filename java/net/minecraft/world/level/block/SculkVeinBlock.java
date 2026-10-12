package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.Collection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

public class SculkVeinBlock extends MultifaceBlock implements SculkBehaviour, SimpleWaterloggedBlock {
   public static final MapCodec<SculkVeinBlock> CODEC = simpleCodec(SculkVeinBlock::new);
   private static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
   private final MultifaceSpreader veinSpreader = new MultifaceSpreader(new SculkVeinBlock.SculkVeinSpreaderConfig(MultifaceSpreader.DEFAULT_SPREAD_ORDER));
   private final MultifaceSpreader sameSpaceSpreader = new MultifaceSpreader(
      new SculkVeinBlock.SculkVeinSpreaderConfig(MultifaceSpreader.SpreadType.SAME_POSITION)
   );

   @Override
   public MapCodec<SculkVeinBlock> codec() {
      return CODEC;
   }

   public SculkVeinBlock(BlockBehaviour.Properties p_222353_) {
      super(p_222353_);
      this.registerDefaultState(this.defaultBlockState().setValue(WATERLOGGED, Boolean.valueOf(false)));
   }

   @Override
   public MultifaceSpreader getSpreader() {
      return this.veinSpreader;
   }

   public MultifaceSpreader getSameSpaceSpreader() {
      return this.sameSpaceSpreader;
   }

   public static boolean regrow(LevelAccessor pLevel, BlockPos pPos, BlockState pState, Collection<Direction> pDirections) {
      boolean $$4 = false;
      BlockState $$5 = Blocks.SCULK_VEIN.defaultBlockState();

      for (Direction $$6 : pDirections) {
         BlockPos $$7 = pPos.relative($$6);
         if (canAttachTo(pLevel, $$6, $$7, pLevel.getBlockState($$7))) {
            $$5 = $$5.setValue(getFaceProperty($$6), Boolean.valueOf(true));
            $$4 = true;
         }
      }

      if (!$$4) {
         return false;
      } else {
         if (!pState.getFluidState().isEmpty()) {
            $$5 = $$5.setValue(WATERLOGGED, Boolean.valueOf(true));
         }

         pLevel.setBlock(pPos, $$5, 3);
         return true;
      }
   }

   @Override
   public void onDischarged(LevelAccessor pLevel, BlockState pState, BlockPos pPos, RandomSource pRandom) {
      if (pState.is(this)) {
         for (Direction $$4 : DIRECTIONS) {
            BooleanProperty $$5 = getFaceProperty($$4);
            if (pState.getValue($$5) && pLevel.getBlockState(pPos.relative($$4)).is(Blocks.SCULK)) {
               pState = pState.setValue($$5, Boolean.valueOf(false));
            }
         }

         if (!hasAnyFace(pState)) {
            FluidState $$6 = pLevel.getFluidState(pPos);
            pState = ($$6.isEmpty() ? Blocks.AIR : Blocks.WATER).defaultBlockState();
         }

         pLevel.setBlock(pPos, pState, 3);
         SculkBehaviour.super.onDischarged(pLevel, pState, pPos, pRandom);
      }
   }

   @Override
   public int attemptUseCharge(
      SculkSpreader.ChargeCursor pCursor, LevelAccessor pLevel, BlockPos pPos, RandomSource pRandom, SculkSpreader pSpreader, boolean pShouldConvertBlocks
   ) {
      if (pShouldConvertBlocks && this.attemptPlaceSculk(pSpreader, pLevel, pCursor.getPos(), pRandom)) {
         return pCursor.getCharge() - 1;
      } else {
         return pRandom.nextInt(pSpreader.chargeDecayRate()) == 0 ? Mth.floor((float)pCursor.getCharge() * 0.5F) : pCursor.getCharge();
      }
   }

   private boolean attemptPlaceSculk(SculkSpreader pSpreader, LevelAccessor pLevel, BlockPos pPos, RandomSource pRandom) {
      BlockState $$4 = pLevel.getBlockState(pPos);
      TagKey<Block> $$5 = pSpreader.replaceableBlocks();

      for (Direction $$6 : Direction.allShuffled(pRandom)) {
         if (hasFace($$4, $$6)) {
            BlockPos $$7 = pPos.relative($$6);
            BlockState $$8 = pLevel.getBlockState($$7);
            if ($$8.is($$5)) {
               BlockState $$9 = Blocks.SCULK.defaultBlockState();
               pLevel.setBlock($$7, $$9, 3);
               Block.pushEntitiesUp($$8, $$9, pLevel, $$7);
               pLevel.playSound(null, $$7, SoundEvents.SCULK_BLOCK_SPREAD, SoundSource.BLOCKS, 1.0F, 1.0F);
               this.veinSpreader.spreadAll($$9, pLevel, $$7, pSpreader.isWorldGeneration());
               Direction $$10 = $$6.getOpposite();

               for (Direction $$11 : DIRECTIONS) {
                  if ($$11 != $$10) {
                     BlockPos $$12 = $$7.relative($$11);
                     BlockState $$13 = pLevel.getBlockState($$12);
                     if ($$13.is(this)) {
                        this.onDischarged(pLevel, $$13, $$12, pRandom);
                     }
                  }
               }

               return true;
            }
         }
      }

      return false;
   }

   public static boolean hasSubstrateAccess(LevelAccessor pLevel, BlockState pState, BlockPos pPos) {
      if (!pState.is(Blocks.SCULK_VEIN)) {
         return false;
      } else {
         for (Direction $$3 : DIRECTIONS) {
            if (hasFace(pState, $$3) && pLevel.getBlockState(pPos.relative($$3)).is(BlockTags.SCULK_REPLACEABLE)) {
               return true;
            }
         }

         return false;
      }
   }

   @Override
   protected BlockState updateShape(
      BlockState pState, Direction pDirection, BlockState pNeighborState, LevelAccessor pLevel, BlockPos pPos, BlockPos pNeighborPos
   ) {
      if (pState.getValue(WATERLOGGED)) {
         pLevel.scheduleTick(pPos, Fluids.WATER, Fluids.WATER.getTickDelay(pLevel));
      }

      return super.updateShape(pState, pDirection, pNeighborState, pLevel, pPos, pNeighborPos);
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
      super.createBlockStateDefinition(pBuilder);
      pBuilder.add(WATERLOGGED);
   }

   @Override
   protected boolean canBeReplaced(BlockState pState, BlockPlaceContext pUseContext) {
      return !pUseContext.getItemInHand().is(Items.SCULK_VEIN) || super.canBeReplaced(pState, pUseContext);
   }

   @Override
   protected FluidState getFluidState(BlockState pState) {
      return pState.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(pState);
   }

   class SculkVeinSpreaderConfig extends MultifaceSpreader.DefaultSpreaderConfig {
      private final MultifaceSpreader.SpreadType[] spreadTypes;

      public SculkVeinSpreaderConfig(final MultifaceSpreader.SpreadType... pSpreadTypes) {
         super(SculkVeinBlock.this);
         this.spreadTypes = pSpreadTypes;
      }

      @Override
      public boolean stateCanBeReplaced(BlockGetter pLevel, BlockPos pPos, BlockPos pSpreadPos, Direction pDirection, BlockState pState) {
         BlockState $$5 = pLevel.getBlockState(pSpreadPos.relative(pDirection));
         if (!$$5.is(Blocks.SCULK) && !$$5.is(Blocks.SCULK_CATALYST) && !$$5.is(Blocks.MOVING_PISTON)) {
            if (pPos.distManhattan(pSpreadPos) == 2) {
               BlockPos $$6 = pPos.relative(pDirection.getOpposite());
               if (pLevel.getBlockState($$6).isFaceSturdy(pLevel, $$6, pDirection)) {
                  return false;
               }
            }

            FluidState $$7 = pState.getFluidState();
            if (!$$7.isEmpty() && !$$7.is(Fluids.WATER)) {
               return false;
            } else {
               return pState.is(BlockTags.FIRE) ? false : pState.canBeReplaced() || super.stateCanBeReplaced(pLevel, pPos, pSpreadPos, pDirection, pState);
            }
         } else {
            return false;
         }
      }

      @Override
      public MultifaceSpreader.SpreadType[] getSpreadTypes() {
         return this.spreadTypes;
      }

      @Override
      public boolean isOtherBlockValidAsSource(BlockState pOtherBlock) {
         return !pOtherBlock.is(Blocks.SCULK_VEIN);
      }
   }
}
