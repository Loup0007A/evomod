package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import it.unimi.dsi.fastutil.shorts.ShortList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ChunkToProtochunkFix extends DataFix {
   private static final int NUM_SECTIONS = 16;

   public ChunkToProtochunkFix(Schema pOutputSchema, boolean pChangesType) {
      super(pOutputSchema, pChangesType);
   }

   public TypeRewriteRule makeRule() {
      return this.writeFixAndRead(
         "ChunkToProtoChunkFix",
         this.getInputSchema().getType(References.CHUNK),
         this.getOutputSchema().getType(References.CHUNK),
         p_199886_ -> p_199886_.update("Level", ChunkToProtochunkFix::fixChunkData)
      );
   }

   private static <T> Dynamic<T> fixChunkData(Dynamic<T> p_199856_) {
      boolean $$1 = p_199856_.get("TerrainPopulated").asBoolean(false);
      boolean $$2 = p_199856_.get("LightPopulated").asNumber().result().isEmpty() || p_199856_.get("LightPopulated").asBoolean(false);
      String $$3;
      if ($$1) {
         if ($$2) {
            $$3 = "mobs_spawned";
         } else {
            $$3 = "decorated";
         }
      } else {
         $$3 = "carved";
      }

      return repackTicks(repackBiomes(p_199856_)).set("Status", p_199856_.createString($$3)).set("hasLegacyStructureData", p_199856_.createBoolean(true));
   }

   private static <T> Dynamic<T> repackBiomes(Dynamic<T> pDynamic) {
      return pDynamic.update("Biomes", p_326566_ -> (Dynamic)DataFixUtils.orElse(p_326566_.asByteBufferOpt().result().map(p_199868_ -> {
            int[] $$2 = new int[256];

            for (int $$3 = 0; $$3 < $$2.length; $$3++) {
               if ($$3 < p_199868_.capacity()) {
                  $$2[$$3] = p_199868_.get($$3) & 255;
               }
            }

            return pDynamic.createIntList(Arrays.stream($$2));
         }), p_326566_));
   }

   private static <T> Dynamic<T> repackTicks(Dynamic<T> pDynamic) {
      return (Dynamic<T>)DataFixUtils.orElse(
         pDynamic.get("TileTicks")
            .asStreamOpt()
            .result()
            .map(
               p_199871_ -> {
                  List<ShortList> $$2 = IntStream.range(0, 16).mapToObj(p_199850_ -> new ShortArrayList()).collect(Collectors.toList());
                  p_199871_.forEach(p_199874_ -> {
                     int $$2x = p_199874_.get("x").asInt(0);
                     int $$3 = p_199874_.get("y").asInt(0);
                     int $$4 = p_199874_.get("z").asInt(0);
                     short $$5 = packOffsetCoordinates($$2x, $$3, $$4);
                     $$2.get($$3 >> 4).add($$5);
                  });
                  return pDynamic.remove("TileTicks")
                     .set(
                        "ToBeTicked",
                        pDynamic.createList(
                           $$2.stream()
                              .map(p_199865_ -> pDynamic.createList(p_199865_.intStream().mapToObj(p_199859_ -> pDynamic.createShort((short)p_199859_))))
                        )
                     );
               }
            ),
         pDynamic
      );
   }

   private static short packOffsetCoordinates(int pX, int pY, int pZ) {
      return (short)(pX & 15 | (pY & 15) << 4 | (pZ & 15) << 8);
   }
}
