package net.minecraft.world.level.block;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringUtil;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BaseCommandBlock;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.slf4j.Logger;

public class CommandBlock extends BaseEntityBlock implements GameMasterBlock {
   public static final MapCodec<CommandBlock> CODEC = RecordCodecBuilder.mapCodec(
      p_312773_ -> p_312773_.group(Codec.BOOL.fieldOf("automatic").forGetter(p_311238_ -> p_311238_.automatic), propertiesCodec())
            .apply(p_312773_, CommandBlock::new)
   );
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final DirectionProperty FACING = DirectionalBlock.FACING;
   public static final BooleanProperty CONDITIONAL = BlockStateProperties.CONDITIONAL;
   private final boolean automatic;

   @Override
   public MapCodec<CommandBlock> codec() {
      return CODEC;
   }

   public CommandBlock(boolean p_153081_, BlockBehaviour.Properties p_153080_) {
      super(p_153080_);
      this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(CONDITIONAL, Boolean.valueOf(false)));
      this.automatic = p_153081_;
   }

   @Override
   public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
      CommandBlockEntity $$2 = new CommandBlockEntity(pPos, pState);
      $$2.setAutomatic(this.automatic);
      return $$2;
   }

   @Override
   protected void neighborChanged(BlockState pState, Level pLevel, BlockPos pPos, Block pBlock, BlockPos pFromPos, boolean pIsMoving) {
      if (!pLevel.isClientSide) {
         if (pLevel.getBlockEntity(pPos) instanceof CommandBlockEntity $$7) {
            boolean $$8 = pLevel.hasNeighborSignal(pPos);
            boolean $$9 = $$7.isPowered();
            $$7.setPowered($$8);
            if (!$$9 && !$$7.isAutomatic() && $$7.getMode() != CommandBlockEntity.Mode.SEQUENCE) {
               if ($$8) {
                  $$7.markConditionMet();
                  pLevel.scheduleTick(pPos, this, 1);
               }
            }
         }
      }
   }

   @Override
   protected void tick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
      if (pLevel.getBlockEntity(pPos) instanceof CommandBlockEntity $$5) {
         BaseCommandBlock $$6 = $$5.getCommandBlock();
         boolean $$7 = !StringUtil.isNullOrEmpty($$6.getCommand());
         CommandBlockEntity.Mode $$8 = $$5.getMode();
         boolean $$9 = $$5.wasConditionMet();
         if ($$8 == CommandBlockEntity.Mode.AUTO) {
            $$5.markConditionMet();
            if ($$9) {
               this.execute(pState, pLevel, pPos, $$6, $$7);
            } else if ($$5.isConditional()) {
               $$6.setSuccessCount(0);
            }

            if ($$5.isPowered() || $$5.isAutomatic()) {
               pLevel.scheduleTick(pPos, this, 1);
            }
         } else if ($$8 == CommandBlockEntity.Mode.REDSTONE) {
            if ($$9) {
               this.execute(pState, pLevel, pPos, $$6, $$7);
            } else if ($$5.isConditional()) {
               $$6.setSuccessCount(0);
            }
         }

         pLevel.updateNeighbourForOutputSignal(pPos, this);
      }
   }

   private void execute(BlockState pState, Level pLevel, BlockPos pPos, BaseCommandBlock pLogic, boolean pCanTrigger) {
      if (pCanTrigger) {
         pLogic.performCommand(pLevel);
      } else {
         pLogic.setSuccessCount(0);
      }

      executeChain(pLevel, pPos, pState.getValue(FACING));
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, BlockHitResult pHitResult) {
      BlockEntity $$5 = pLevel.getBlockEntity(pPos);
      if ($$5 instanceof CommandBlockEntity && pPlayer.canUseGameMasterBlocks()) {
         pPlayer.openCommandBlock((CommandBlockEntity)$$5);
         return InteractionResult.sidedSuccess(pLevel.isClientSide);
      } else {
         return InteractionResult.PASS;
      }
   }

   @Override
   protected boolean hasAnalogOutputSignal(BlockState pState) {
      return true;
   }

   @Override
   protected int getAnalogOutputSignal(BlockState pBlockState, Level pLevel, BlockPos pPos) {
      BlockEntity $$3 = pLevel.getBlockEntity(pPos);
      return $$3 instanceof CommandBlockEntity ? ((CommandBlockEntity)$$3).getCommandBlock().getSuccessCount() : 0;
   }

   @Override
   public void setPlacedBy(Level pLevel, BlockPos pPos, BlockState pState, LivingEntity pPlacer, ItemStack pStack) {
      if (pLevel.getBlockEntity(pPos) instanceof CommandBlockEntity $$6) {
         BaseCommandBlock $$7 = $$6.getCommandBlock();
         if (!pLevel.isClientSide) {
            if (!pStack.has(DataComponents.BLOCK_ENTITY_DATA)) {
               $$7.setTrackOutput(pLevel.getGameRules().getBoolean(GameRules.RULE_SENDCOMMANDFEEDBACK));
               $$6.setAutomatic(this.automatic);
            }

            boolean $$8 = pLevel.hasNeighborSignal(pPos);
            $$6.setPowered($$8);
         }
      }
   }

   @Override
   protected RenderShape getRenderShape(BlockState pState) {
      return RenderShape.MODEL;
   }

   @Override
   protected BlockState rotate(BlockState pState, Rotation pRotation) {
      return pState.setValue(FACING, pRotation.rotate(pState.getValue(FACING)));
   }

   @Override
   protected BlockState mirror(BlockState pState, Mirror pMirror) {
      return pState.rotate(pMirror.getRotation(pState.getValue(FACING)));
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
      pBuilder.add(FACING, CONDITIONAL);
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext pContext) {
      return this.defaultBlockState().setValue(FACING, pContext.getNearestLookingDirection().getOpposite());
   }

   private static void executeChain(Level pLevel, BlockPos pPos, Direction pDirection) {
      BlockPos.MutableBlockPos $$3 = pPos.mutable();
      GameRules $$4 = pLevel.getGameRules();
      int $$5 = $$4.getInt(GameRules.RULE_MAX_COMMAND_CHAIN_LENGTH);

      while ($$5-- > 0) {
         $$3.move(pDirection);
         BlockState $$6 = pLevel.getBlockState($$3);
         Block $$7 = $$6.getBlock();
         if (!$$6.is(Blocks.CHAIN_COMMAND_BLOCK)
            || !(pLevel.getBlockEntity($$3) instanceof CommandBlockEntity $$9)
            || $$9.getMode() != CommandBlockEntity.Mode.SEQUENCE) {
            break;
         }

         if ($$9.isPowered() || $$9.isAutomatic()) {
            BaseCommandBlock $$10 = $$9.getCommandBlock();
            if ($$9.markConditionMet()) {
               if (!$$10.performCommand(pLevel)) {
                  break;
               }

               pLevel.updateNeighbourForOutputSignal($$3, $$7);
            } else if ($$9.isConditional()) {
               $$10.setSuccessCount(0);
            }
         }

         pDirection = $$6.getValue(FACING);
      }

      if ($$5 <= 0) {
         int $$11 = Math.max($$4.getInt(GameRules.RULE_MAX_COMMAND_CHAIN_LENGTH), 0);
         LOGGER.warn("Command Block chain tried to execute more than {} steps!", $$11);
      }
   }
}
