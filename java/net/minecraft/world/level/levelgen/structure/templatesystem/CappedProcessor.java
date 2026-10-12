package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIterator;
import java.util.List;
import java.util.stream.IntStream;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.ServerLevelAccessor;

public class CappedProcessor extends StructureProcessor {
   public static final MapCodec<CappedProcessor> CODEC = RecordCodecBuilder.mapCodec(
      p_277598_ -> p_277598_.group(
               StructureProcessorType.SINGLE_CODEC.fieldOf("delegate").forGetter(p_277456_ -> p_277456_.delegate),
               IntProvider.POSITIVE_CODEC.fieldOf("limit").forGetter(p_277680_ -> p_277680_.limit)
            )
            .apply(p_277598_, CappedProcessor::new)
   );
   private final StructureProcessor delegate;
   private final IntProvider limit;

   public CappedProcessor(StructureProcessor p_277972_, IntProvider p_277402_) {
      this.delegate = p_277972_;
      this.limit = p_277402_;
   }

   @Override
   protected StructureProcessorType<?> getType() {
      return StructureProcessorType.CAPPED;
   }

   @Override
   public final List<StructureTemplate.StructureBlockInfo> finalizeProcessing(
      ServerLevelAccessor pServerLevel,
      BlockPos pOffset,
      BlockPos pPos,
      List<StructureTemplate.StructureBlockInfo> pOriginalBlockInfos,
      List<StructureTemplate.StructureBlockInfo> pProcessedBlockInfos,
      StructurePlaceSettings pSettings
   ) {
      if (this.limit.getMaxValue() != 0 && !pProcessedBlockInfos.isEmpty()) {
         if (pOriginalBlockInfos.size() != pProcessedBlockInfos.size()) {
            Util.logAndPauseIfInIde(
               "Original block info list not in sync with processed list, skipping processing. Original size: "
                  + pOriginalBlockInfos.size()
                  + ", Processed size: "
                  + pProcessedBlockInfos.size()
            );
            return pProcessedBlockInfos;
         } else {
            RandomSource $$6 = RandomSource.create(pServerLevel.getLevel().getSeed()).forkPositional().at(pOffset);
            int $$7 = Math.min(this.limit.sample($$6), pProcessedBlockInfos.size());
            if ($$7 < 1) {
               return pProcessedBlockInfos;
            } else {
               IntArrayList $$8 = Util.toShuffledList(IntStream.range(0, pProcessedBlockInfos.size()), $$6);
               IntIterator $$9 = $$8.intIterator();
               int $$10 = 0;

               while ($$9.hasNext() && $$10 < $$7) {
                  int $$11 = $$9.nextInt();
                  StructureTemplate.StructureBlockInfo $$12 = pOriginalBlockInfos.get($$11);
                  StructureTemplate.StructureBlockInfo $$13 = pProcessedBlockInfos.get($$11);
                  StructureTemplate.StructureBlockInfo $$14 = this.delegate.processBlock(pServerLevel, pOffset, pPos, $$12, $$13, pSettings);
                  if ($$14 != null && !$$13.equals($$14)) {
                     $$10++;
                     pProcessedBlockInfos.set($$11, $$14);
                  }
               }

               return pProcessedBlockInfos;
            }
         }
      } else {
         return pProcessedBlockInfos;
      }
   }
}
