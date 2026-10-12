package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import net.minecraft.world.level.block.state.pattern.BlockPatternBuilder;
import net.minecraft.world.level.block.state.predicate.BlockStatePredicate;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

public class CarvedPumpkinBlock extends HorizontalDirectionalBlock {
   public static final MapCodec<CarvedPumpkinBlock> CODEC = simpleCodec(CarvedPumpkinBlock::new);
   public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
   @Nullable
   private BlockPattern snowGolemBase;
   @Nullable
   private BlockPattern snowGolemFull;
   @Nullable
   private BlockPattern ironGolemBase;
   @Nullable
   private BlockPattern ironGolemFull;
   private static final Predicate<BlockState> PUMPKINS_PREDICATE = p_51396_ -> p_51396_ != null
         && (p_51396_.is(Blocks.CARVED_PUMPKIN) || p_51396_.is(Blocks.JACK_O_LANTERN));

   @Override
   public MapCodec<? extends CarvedPumpkinBlock> codec() {
      return CODEC;
   }

   public CarvedPumpkinBlock(BlockBehaviour.Properties p_51375_) {
      super(p_51375_);
      this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
   }

   @Override
   protected void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {
      if (!pOldState.is(pState.getBlock())) {
         this.trySpawnGolem(pLevel, pPos);
      }
   }

   public boolean canSpawnGolem(LevelReader pLevel, BlockPos pPos) {
      return this.getOrCreateSnowGolemBase().find(pLevel, pPos) != null || this.getOrCreateIronGolemBase().find(pLevel, pPos) != null;
   }

   private void trySpawnGolem(Level pLevel, BlockPos pPos) {
      BlockPattern.BlockPatternMatch $$2 = this.getOrCreateSnowGolemFull().find(pLevel, pPos);
      if ($$2 != null) {
         SnowGolem $$3 = EntityType.SNOW_GOLEM.create(pLevel);
         if ($$3 != null) {
            spawnGolemInWorld(pLevel, $$2, $$3, $$2.getBlock(0, 2, 0).getPos());
         }
      } else {
         BlockPattern.BlockPatternMatch $$4 = this.getOrCreateIronGolemFull().find(pLevel, pPos);
         if ($$4 != null) {
            IronGolem $$5 = EntityType.IRON_GOLEM.create(pLevel);
            if ($$5 != null) {
               $$5.setPlayerCreated(true);
               spawnGolemInWorld(pLevel, $$4, $$5, $$4.getBlock(1, 2, 0).getPos());
            }
         }
      }
   }

   private static void spawnGolemInWorld(Level pLevel, BlockPattern.BlockPatternMatch pPatternMatch, Entity pGolem, BlockPos pPos) {
      clearPatternBlocks(pLevel, pPatternMatch);
      pGolem.moveTo((double)pPos.getX() + 0.5, (double)pPos.getY() + 0.05, (double)pPos.getZ() + 0.5, 0.0F, 0.0F);
      pLevel.addFreshEntity(pGolem);

      for (ServerPlayer $$4 : pLevel.getEntitiesOfClass(ServerPlayer.class, pGolem.getBoundingBox().inflate(5.0))) {
         CriteriaTriggers.SUMMONED_ENTITY.trigger($$4, pGolem);
      }

      updatePatternBlocks(pLevel, pPatternMatch);
   }

   public static void clearPatternBlocks(Level pLevel, BlockPattern.BlockPatternMatch pPatternMatch) {
      for (int $$2 = 0; $$2 < pPatternMatch.getWidth(); $$2++) {
         for (int $$3 = 0; $$3 < pPatternMatch.getHeight(); $$3++) {
            BlockInWorld $$4 = pPatternMatch.getBlock($$2, $$3, 0);
            pLevel.setBlock($$4.getPos(), Blocks.AIR.defaultBlockState(), 2);
            pLevel.levelEvent(2001, $$4.getPos(), Block.getId($$4.getState()));
         }
      }
   }

   public static void updatePatternBlocks(Level pLevel, BlockPattern.BlockPatternMatch pPatternMatch) {
      for (int $$2 = 0; $$2 < pPatternMatch.getWidth(); $$2++) {
         for (int $$3 = 0; $$3 < pPatternMatch.getHeight(); $$3++) {
            BlockInWorld $$4 = pPatternMatch.getBlock($$2, $$3, 0);
            pLevel.blockUpdated($$4.getPos(), Blocks.AIR);
         }
      }
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext pContext) {
      return this.defaultBlockState().setValue(FACING, pContext.getHorizontalDirection().getOpposite());
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
      pBuilder.add(FACING);
   }

   private BlockPattern getOrCreateSnowGolemBase() {
      if (this.snowGolemBase == null) {
         this.snowGolemBase = BlockPatternBuilder.start()
            .aisle(" ", "#", "#")
            .where('#', BlockInWorld.hasState(BlockStatePredicate.forBlock(Blocks.SNOW_BLOCK)))
            .build();
      }

      return this.snowGolemBase;
   }

   private BlockPattern getOrCreateSnowGolemFull() {
      if (this.snowGolemFull == null) {
         this.snowGolemFull = BlockPatternBuilder.start()
            .aisle("^", "#", "#")
            .where('^', BlockInWorld.hasState(PUMPKINS_PREDICATE))
            .where('#', BlockInWorld.hasState(BlockStatePredicate.forBlock(Blocks.SNOW_BLOCK)))
            .build();
      }

      return this.snowGolemFull;
   }

   private BlockPattern getOrCreateIronGolemBase() {
      if (this.ironGolemBase == null) {
         this.ironGolemBase = BlockPatternBuilder.start()
            .aisle("~ ~", "###", "~#~")
            .where('#', BlockInWorld.hasState(BlockStatePredicate.forBlock(Blocks.IRON_BLOCK)))
            .where('~', p_284869_ -> p_284869_.getState().isAir())
            .build();
      }

      return this.ironGolemBase;
   }

   private BlockPattern getOrCreateIronGolemFull() {
      if (this.ironGolemFull == null) {
         this.ironGolemFull = BlockPatternBuilder.start()
            .aisle("~^~", "###", "~#~")
            .where('^', BlockInWorld.hasState(PUMPKINS_PREDICATE))
            .where('#', BlockInWorld.hasState(BlockStatePredicate.forBlock(Blocks.IRON_BLOCK)))
            .where('~', p_284868_ -> p_284868_.getState().isAir())
            .build();
      }

      return this.ironGolemFull;
   }
}
