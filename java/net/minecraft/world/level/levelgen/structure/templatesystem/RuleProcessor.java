package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.UnmodifiableIterator;
import com.mojang.serialization.MapCodec;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

public class RuleProcessor extends StructureProcessor {
   public static final MapCodec<RuleProcessor> CODEC = ProcessorRule.CODEC.listOf().fieldOf("rules").xmap(RuleProcessor::new, p_74306_ -> p_74306_.rules);
   private final ImmutableList<ProcessorRule> rules;

   public RuleProcessor(List<? extends ProcessorRule> p_74296_) {
      this.rules = ImmutableList.copyOf(p_74296_);
   }

   @Nullable
   @Override
   public StructureTemplate.StructureBlockInfo processBlock(
      LevelReader pLevel,
      BlockPos pOffset,
      BlockPos pPos,
      StructureTemplate.StructureBlockInfo pBlockInfo,
      StructureTemplate.StructureBlockInfo pRelativeBlockInfo,
      StructurePlaceSettings pSettings
   ) {
      RandomSource $$6 = RandomSource.create(Mth.getSeed(pRelativeBlockInfo.pos()));
      BlockState $$7 = pLevel.getBlockState(pRelativeBlockInfo.pos());
      UnmodifiableIterator var9 = this.rules.iterator();

      while (var9.hasNext()) {
         ProcessorRule $$8 = (ProcessorRule)var9.next();
         if ($$8.test(pRelativeBlockInfo.state(), $$7, pBlockInfo.pos(), pRelativeBlockInfo.pos(), pPos, $$6)) {
            return new StructureTemplate.StructureBlockInfo(pRelativeBlockInfo.pos(), $$8.getOutputState(), $$8.getOutputTag($$6, pRelativeBlockInfo.nbt()));
         }
      }

      return pRelativeBlockInfo;
   }

   @Override
   protected StructureProcessorType<?> getType() {
      return StructureProcessorType.RULE;
   }
}
