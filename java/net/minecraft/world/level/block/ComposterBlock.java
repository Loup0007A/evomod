package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ComposterBlock extends Block implements WorldlyContainerHolder {
   public static final MapCodec<ComposterBlock> CODEC = simpleCodec(ComposterBlock::new);
   public static final int READY = 8;
   public static final int MIN_LEVEL = 0;
   public static final int MAX_LEVEL = 7;
   public static final IntegerProperty LEVEL = BlockStateProperties.LEVEL_COMPOSTER;
   public static final Object2FloatMap<ItemLike> COMPOSTABLES = new Object2FloatOpenHashMap();
   private static final int AABB_SIDE_THICKNESS = 2;
   private static final VoxelShape OUTER_SHAPE = Shapes.block();
   private static final VoxelShape[] SHAPES = Util.make(new VoxelShape[9], p_51967_ -> {
      for (int $$1 = 0; $$1 < 8; $$1++) {
         p_51967_[$$1] = Shapes.join(OUTER_SHAPE, Block.box(2.0, (double)Math.max(2, 1 + $$1 * 2), 2.0, 14.0, 16.0, 14.0), BooleanOp.ONLY_FIRST);
      }

      p_51967_[8] = p_51967_[7];
   });

   @Override
   public MapCodec<ComposterBlock> codec() {
      return CODEC;
   }

   public static void bootStrap() {
      COMPOSTABLES.defaultReturnValue(-1.0F);
      float $$0 = 0.3F;
      float $$1 = 0.5F;
      float $$2 = 0.65F;
      float $$3 = 0.85F;
      float $$4 = 1.0F;
      add(0.3F, Items.JUNGLE_LEAVES);
      add(0.3F, Items.OAK_LEAVES);
      add(0.3F, Items.SPRUCE_LEAVES);
      add(0.3F, Items.DARK_OAK_LEAVES);
      add(0.3F, Items.ACACIA_LEAVES);
      add(0.3F, Items.CHERRY_LEAVES);
      add(0.3F, Items.BIRCH_LEAVES);
      add(0.3F, Items.AZALEA_LEAVES);
      add(0.3F, Items.MANGROVE_LEAVES);
      add(0.3F, Items.OAK_SAPLING);
      add(0.3F, Items.SPRUCE_SAPLING);
      add(0.3F, Items.BIRCH_SAPLING);
      add(0.3F, Items.JUNGLE_SAPLING);
      add(0.3F, Items.ACACIA_SAPLING);
      add(0.3F, Items.CHERRY_SAPLING);
      add(0.3F, Items.DARK_OAK_SAPLING);
      add(0.3F, Items.MANGROVE_PROPAGULE);
      add(0.3F, Items.BEETROOT_SEEDS);
      add(0.3F, Items.DRIED_KELP);
      add(0.3F, Items.SHORT_GRASS);
      add(0.3F, Items.KELP);
      add(0.3F, Items.MELON_SEEDS);
      add(0.3F, Items.PUMPKIN_SEEDS);
      add(0.3F, Items.SEAGRASS);
      add(0.3F, Items.SWEET_BERRIES);
      add(0.3F, Items.GLOW_BERRIES);
      add(0.3F, Items.WHEAT_SEEDS);
      add(0.3F, Items.MOSS_CARPET);
      add(0.3F, Items.PINK_PETALS);
      add(0.3F, Items.SMALL_DRIPLEAF);
      add(0.3F, Items.HANGING_ROOTS);
      add(0.3F, Items.MANGROVE_ROOTS);
      add(0.3F, Items.TORCHFLOWER_SEEDS);
      add(0.3F, Items.PITCHER_POD);
      add(0.5F, Items.DRIED_KELP_BLOCK);
      add(0.5F, Items.TALL_GRASS);
      add(0.5F, Items.FLOWERING_AZALEA_LEAVES);
      add(0.5F, Items.CACTUS);
      add(0.5F, Items.SUGAR_CANE);
      add(0.5F, Items.VINE);
      add(0.5F, Items.NETHER_SPROUTS);
      add(0.5F, Items.WEEPING_VINES);
      add(0.5F, Items.TWISTING_VINES);
      add(0.5F, Items.MELON_SLICE);
      add(0.5F, Items.GLOW_LICHEN);
      add(0.65F, Items.SEA_PICKLE);
      add(0.65F, Items.LILY_PAD);
      add(0.65F, Items.PUMPKIN);
      add(0.65F, Items.CARVED_PUMPKIN);
      add(0.65F, Items.MELON);
      add(0.65F, Items.APPLE);
      add(0.65F, Items.BEETROOT);
      add(0.65F, Items.CARROT);
      add(0.65F, Items.COCOA_BEANS);
      add(0.65F, Items.POTATO);
      add(0.65F, Items.WHEAT);
      add(0.65F, Items.BROWN_MUSHROOM);
      add(0.65F, Items.RED_MUSHROOM);
      add(0.65F, Items.MUSHROOM_STEM);
      add(0.65F, Items.CRIMSON_FUNGUS);
      add(0.65F, Items.WARPED_FUNGUS);
      add(0.65F, Items.NETHER_WART);
      add(0.65F, Items.CRIMSON_ROOTS);
      add(0.65F, Items.WARPED_ROOTS);
      add(0.65F, Items.SHROOMLIGHT);
      add(0.65F, Items.DANDELION);
      add(0.65F, Items.POPPY);
      add(0.65F, Items.BLUE_ORCHID);
      add(0.65F, Items.ALLIUM);
      add(0.65F, Items.AZURE_BLUET);
      add(0.65F, Items.RED_TULIP);
      add(0.65F, Items.ORANGE_TULIP);
      add(0.65F, Items.WHITE_TULIP);
      add(0.65F, Items.PINK_TULIP);
      add(0.65F, Items.OXEYE_DAISY);
      add(0.65F, Items.CORNFLOWER);
      add(0.65F, Items.LILY_OF_THE_VALLEY);
      add(0.65F, Items.WITHER_ROSE);
      add(0.65F, Items.FERN);
      add(0.65F, Items.SUNFLOWER);
      add(0.65F, Items.LILAC);
      add(0.65F, Items.ROSE_BUSH);
      add(0.65F, Items.PEONY);
      add(0.65F, Items.LARGE_FERN);
      add(0.65F, Items.SPORE_BLOSSOM);
      add(0.65F, Items.AZALEA);
      add(0.65F, Items.MOSS_BLOCK);
      add(0.65F, Items.BIG_DRIPLEAF);
      add(0.85F, Items.HAY_BLOCK);
      add(0.85F, Items.BROWN_MUSHROOM_BLOCK);
      add(0.85F, Items.RED_MUSHROOM_BLOCK);
      add(0.85F, Items.NETHER_WART_BLOCK);
      add(0.85F, Items.WARPED_WART_BLOCK);
      add(0.85F, Items.FLOWERING_AZALEA);
      add(0.85F, Items.BREAD);
      add(0.85F, Items.BAKED_POTATO);
      add(0.85F, Items.COOKIE);
      add(0.85F, Items.TORCHFLOWER);
      add(0.85F, Items.PITCHER_PLANT);
      add(1.0F, Items.CAKE);
      add(1.0F, Items.PUMPKIN_PIE);
   }

   private static void add(float pChance, ItemLike pItem) {
      COMPOSTABLES.put(pItem.asItem(), pChance);
   }

   public ComposterBlock(BlockBehaviour.Properties p_51919_) {
      super(p_51919_);
      this.registerDefaultState(this.stateDefinition.any().setValue(LEVEL, Integer.valueOf(0)));
   }

   public static void handleFill(Level pLevel, BlockPos pPos, boolean pSuccess) {
      BlockState $$3 = pLevel.getBlockState(pPos);
      pLevel.playLocalSound(pPos, pSuccess ? SoundEvents.COMPOSTER_FILL_SUCCESS : SoundEvents.COMPOSTER_FILL, SoundSource.BLOCKS, 1.0F, 1.0F, false);
      double $$4 = $$3.getShape(pLevel, pPos).max(Direction.Axis.Y, 0.5, 0.5) + 0.03125;
      double $$5 = 0.13125F;
      double $$6 = 0.7375F;
      RandomSource $$7 = pLevel.getRandom();

      for (int $$8 = 0; $$8 < 10; $$8++) {
         double $$9 = $$7.nextGaussian() * 0.02;
         double $$10 = $$7.nextGaussian() * 0.02;
         double $$11 = $$7.nextGaussian() * 0.02;
         pLevel.addParticle(
            ParticleTypes.COMPOSTER,
            (double)pPos.getX() + 0.13125F + 0.7375F * (double)$$7.nextFloat(),
            (double)pPos.getY() + $$4 + (double)$$7.nextFloat() * (1.0 - $$4),
            (double)pPos.getZ() + 0.13125F + 0.7375F * (double)$$7.nextFloat(),
            $$9,
            $$10,
            $$11
         );
      }
   }

   @Override
   protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      return SHAPES[pState.getValue(LEVEL)];
   }

   @Override
   protected VoxelShape getInteractionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
      return OUTER_SHAPE;
   }

   @Override
   protected VoxelShape getCollisionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      return SHAPES[0];
   }

   @Override
   protected void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {
      if (pState.getValue(LEVEL) == 7) {
         pLevel.scheduleTick(pPos, pState.getBlock(), 20);
      }
   }

   @Override
   protected ItemInteractionResult useItemOn(
      ItemStack pStack, BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHitResult
   ) {
      int $$7 = pState.getValue(LEVEL);
      if ($$7 < 8 && COMPOSTABLES.containsKey(pStack.getItem())) {
         if ($$7 < 7 && !pLevel.isClientSide) {
            BlockState $$8 = addItem(pPlayer, pState, pLevel, pPos, pStack);
            pLevel.levelEvent(1500, pPos, pState != $$8 ? 1 : 0);
            pPlayer.awardStat(Stats.ITEM_USED.get(pStack.getItem()));
            pStack.consume(1, pPlayer);
         }

         return ItemInteractionResult.sidedSuccess(pLevel.isClientSide);
      } else {
         return super.useItemOn(pStack, pState, pLevel, pPos, pPlayer, pHand, pHitResult);
      }
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, BlockHitResult pHitResult) {
      int $$5 = pState.getValue(LEVEL);
      if ($$5 == 8) {
         extractProduce(pPlayer, pState, pLevel, pPos);
         return InteractionResult.sidedSuccess(pLevel.isClientSide);
      } else {
         return InteractionResult.PASS;
      }
   }

   public static BlockState insertItem(Entity pEntity, BlockState pState, ServerLevel pLevel, ItemStack pStack, BlockPos pPos) {
      int $$5 = pState.getValue(LEVEL);
      if ($$5 < 7 && COMPOSTABLES.containsKey(pStack.getItem())) {
         BlockState $$6 = addItem(pEntity, pState, pLevel, pPos, pStack);
         pStack.shrink(1);
         return $$6;
      } else {
         return pState;
      }
   }

   public static BlockState extractProduce(Entity pEntity, BlockState pState, Level pLevel, BlockPos pPos) {
      if (!pLevel.isClientSide) {
         Vec3 $$4 = Vec3.atLowerCornerWithOffset(pPos, 0.5, 1.01, 0.5).offsetRandom(pLevel.random, 0.7F);
         ItemEntity $$5 = new ItemEntity(pLevel, $$4.x(), $$4.y(), $$4.z(), new ItemStack(Items.BONE_MEAL));
         $$5.setDefaultPickUpDelay();
         pLevel.addFreshEntity($$5);
      }

      BlockState $$6 = empty(pEntity, pState, pLevel, pPos);
      pLevel.playSound(null, pPos, SoundEvents.COMPOSTER_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
      return $$6;
   }

   static BlockState empty(@Nullable Entity pEntity, BlockState pState, LevelAccessor pLevel, BlockPos pPos) {
      BlockState $$4 = pState.setValue(LEVEL, Integer.valueOf(0));
      pLevel.setBlock(pPos, $$4, 3);
      pLevel.gameEvent(GameEvent.BLOCK_CHANGE, pPos, GameEvent.Context.of(pEntity, $$4));
      return $$4;
   }

   static BlockState addItem(@Nullable Entity pEntity, BlockState pState, LevelAccessor pLevel, BlockPos pPos, ItemStack pStack) {
      int $$5 = pState.getValue(LEVEL);
      float $$6 = COMPOSTABLES.getFloat(pStack.getItem());
      if (($$5 != 0 || !($$6 > 0.0F)) && !(pLevel.getRandom().nextDouble() < (double)$$6)) {
         return pState;
      } else {
         int $$7 = $$5 + 1;
         BlockState $$8 = pState.setValue(LEVEL, Integer.valueOf($$7));
         pLevel.setBlock(pPos, $$8, 3);
         pLevel.gameEvent(GameEvent.BLOCK_CHANGE, pPos, GameEvent.Context.of(pEntity, $$8));
         if ($$7 == 7) {
            pLevel.scheduleTick(pPos, pState.getBlock(), 20);
         }

         return $$8;
      }
   }

   @Override
   protected void tick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
      if (pState.getValue(LEVEL) == 7) {
         pLevel.setBlock(pPos, pState.cycle(LEVEL), 3);
         pLevel.playSound(null, pPos, SoundEvents.COMPOSTER_READY, SoundSource.BLOCKS, 1.0F, 1.0F);
      }
   }

   @Override
   protected boolean hasAnalogOutputSignal(BlockState pState) {
      return true;
   }

   @Override
   protected int getAnalogOutputSignal(BlockState pBlockState, Level pLevel, BlockPos pPos) {
      return pBlockState.getValue(LEVEL);
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
      pBuilder.add(LEVEL);
   }

   @Override
   protected boolean isPathfindable(BlockState pState, PathComputationType pPathComputationType) {
      return false;
   }

   @Override
   public WorldlyContainer getContainer(BlockState pState, LevelAccessor pLevel, BlockPos pPos) {
      int $$3 = pState.getValue(LEVEL);
      if ($$3 == 8) {
         return new ComposterBlock.OutputContainer(pState, pLevel, pPos, new ItemStack(Items.BONE_MEAL));
      } else {
         return (WorldlyContainer)($$3 < 7 ? new ComposterBlock.InputContainer(pState, pLevel, pPos) : new ComposterBlock.EmptyContainer());
      }
   }

   static class EmptyContainer extends SimpleContainer implements WorldlyContainer {
      public EmptyContainer() {
         super(0);
      }

      @Override
      public int[] getSlotsForFace(Direction pSide) {
         return new int[0];
      }

      @Override
      public boolean canPlaceItemThroughFace(int pIndex, ItemStack pItemStack, @Nullable Direction pDirection) {
         return false;
      }

      @Override
      public boolean canTakeItemThroughFace(int pIndex, ItemStack pStack, Direction pDirection) {
         return false;
      }
   }

   static class InputContainer extends SimpleContainer implements WorldlyContainer {
      private final BlockState state;
      private final LevelAccessor level;
      private final BlockPos pos;
      private boolean changed;

      public InputContainer(BlockState pState, LevelAccessor pLevel, BlockPos pPos) {
         super(1);
         this.state = pState;
         this.level = pLevel;
         this.pos = pPos;
      }

      @Override
      public int getMaxStackSize() {
         return 1;
      }

      @Override
      public int[] getSlotsForFace(Direction pSide) {
         return pSide == Direction.UP ? new int[]{0} : new int[0];
      }

      @Override
      public boolean canPlaceItemThroughFace(int pIndex, ItemStack pItemStack, @Nullable Direction pDirection) {
         return !this.changed && pDirection == Direction.UP && ComposterBlock.COMPOSTABLES.containsKey(pItemStack.getItem());
      }

      @Override
      public boolean canTakeItemThroughFace(int pIndex, ItemStack pStack, Direction pDirection) {
         return false;
      }

      @Override
      public void setChanged() {
         ItemStack $$0 = this.getItem(0);
         if (!$$0.isEmpty()) {
            this.changed = true;
            BlockState $$1 = ComposterBlock.addItem(null, this.state, this.level, this.pos, $$0);
            this.level.levelEvent(1500, this.pos, $$1 != this.state ? 1 : 0);
            this.removeItemNoUpdate(0);
         }
      }
   }

   static class OutputContainer extends SimpleContainer implements WorldlyContainer {
      private final BlockState state;
      private final LevelAccessor level;
      private final BlockPos pos;
      private boolean changed;

      public OutputContainer(BlockState pState, LevelAccessor pLevel, BlockPos pPos, ItemStack pStack) {
         super(pStack);
         this.state = pState;
         this.level = pLevel;
         this.pos = pPos;
      }

      @Override
      public int getMaxStackSize() {
         return 1;
      }

      @Override
      public int[] getSlotsForFace(Direction pSide) {
         return pSide == Direction.DOWN ? new int[]{0} : new int[0];
      }

      @Override
      public boolean canPlaceItemThroughFace(int pIndex, ItemStack pItemStack, @Nullable Direction pDirection) {
         return false;
      }

      @Override
      public boolean canTakeItemThroughFace(int pIndex, ItemStack pStack, Direction pDirection) {
         return !this.changed && pDirection == Direction.DOWN && pStack.is(Items.BONE_MEAL);
      }

      @Override
      public void setChanged() {
         ComposterBlock.empty(null, this.state, this.level, this.pos);
         this.changed = true;
      }
   }
}
