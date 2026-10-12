package net.minecraft.util.datafix.fixes;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.List.ListType;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.apache.commons.lang3.mutable.MutableInt;

public class ChunkProtoTickListFix extends DataFix {
   private static final int SECTION_WIDTH = 16;
   private static final ImmutableSet<String> ALWAYS_WATERLOGGED = ImmutableSet.of(
      "minecraft:bubble_column", "minecraft:kelp", "minecraft:kelp_plant", "minecraft:seagrass", "minecraft:tall_seagrass"
   );

   public ChunkProtoTickListFix(Schema pOutputSchema) {
      super(pOutputSchema, false);
   }

   protected TypeRewriteRule makeRule() {
      Type<?> $$0 = this.getInputSchema().getType(References.CHUNK);
      OpticFinder<?> $$1 = $$0.findField("Level");
      OpticFinder<?> $$2 = $$1.type().findField("Sections");
      OpticFinder<?> $$3 = ((ListType)$$2.type()).getElement().finder();
      OpticFinder<?> $$4 = $$3.type().findField("block_states");
      OpticFinder<?> $$5 = $$3.type().findField("biomes");
      OpticFinder<?> $$6 = $$4.type().findField("palette");
      OpticFinder<?> $$7 = $$1.type().findField("TileTicks");
      return this.fixTypeEverywhereTyped(
         "ChunkProtoTickListFix",
         $$0,
         p_185002_ -> p_185002_.updateTyped(
               $$1,
               p_185010_ -> {
                  p_185010_ = p_185010_.update(
                     DSL.remainderFinder(),
                     p_185078_ -> (Dynamic)DataFixUtils.orElse(
                           p_185078_.get("LiquidTicks").result().map(p_185072_ -> p_185078_.set("fluid_ticks", p_185072_).remove("LiquidTicks")), p_185078_
                        )
                  );
                  Dynamic<?> $$7x = (Dynamic<?>)p_185010_.get(DSL.remainderFinder());
                  MutableInt $$8 = new MutableInt();
                  Int2ObjectMap<Supplier<ChunkProtoTickListFix.PoorMansPalettedContainer>> $$9 = new Int2ObjectArrayMap();
                  p_185010_.getOptionalTyped($$2)
                     .ifPresent(
                        p_185018_ -> p_185018_.getAllTyped($$3)
                              .forEach(
                                 p_185025_ -> {
                                    Dynamic<?> $$6x = (Dynamic<?>)p_185025_.get(DSL.remainderFinder());
                                    int $$7xx = $$6x.get("Y").asInt(Integer.MAX_VALUE);
                                    if ($$7xx != Integer.MAX_VALUE) {
                                       if (p_185025_.getOptionalTyped($$5).isPresent()) {
                                          $$8.setValue(Math.min($$7xx, $$8.getValue()));
                                       }

                                       p_185025_.getOptionalTyped($$4)
                                          .ifPresent(
                                             p_185064_ -> $$9.put(
                                                   $$7xx,
                                                   Suppliers.memoize(
                                                      () -> {
                                                         List<? extends Dynamic<?>> $$2x = p_185064_.getOptionalTyped($$6)
                                                            .map(
                                                               p_326564_ -> p_326564_.write()
                                                                     .result()
                                                                     .map(p_185076_ -> p_185076_.asList(Function.identity()))
                                                                     .orElse(Collections.emptyList())
                                                            )
                                                            .orElse(Collections.emptyList());
                                                         long[] $$3x = ((Dynamic)p_185064_.get(DSL.remainderFinder())).get("data").asLongStream().toArray();
                                                         return new ChunkProtoTickListFix.PoorMansPalettedContainer($$2x, $$3x);
                                                      }
                                                   )
                                                )
                                          );
                                    }
                                 }
                              )
                     );
                  byte $$10 = $$8.getValue().byteValue();
                  p_185010_ = p_185010_.update(DSL.remainderFinder(), p_184991_ -> p_184991_.update("yPos", p_185067_ -> p_185067_.createByte($$10)));
                  if (!p_185010_.getOptionalTyped($$7).isPresent() && !$$7x.get("fluid_ticks").result().isPresent()) {
                     int $$11 = $$7x.get("xPos").asInt(0);
                     int $$12 = $$7x.get("zPos").asInt(0);
                     Dynamic<?> $$13 = this.makeTickList($$7x, $$9, $$10, $$11, $$12, "LiquidsToBeTicked", ChunkProtoTickListFix::getLiquid);
                     Dynamic<?> $$14 = this.makeTickList($$7x, $$9, $$10, $$11, $$12, "ToBeTicked", ChunkProtoTickListFix::getBlock);
                     Optional<? extends Pair<? extends Typed<?>, ?>> $$15 = $$7.type().readTyped($$14).result();
                     if ($$15.isPresent()) {
                        p_185010_ = p_185010_.set($$7, (Typed)$$15.get().getFirst());
                     }

                     return p_185010_.update(
                        DSL.remainderFinder(), p_185035_ -> p_185035_.remove("ToBeTicked").remove("LiquidsToBeTicked").set("fluid_ticks", $$13)
                     );
                  } else {
                     return p_185010_;
                  }
               }
            )
      );
   }

   private Dynamic<?> makeTickList(
      Dynamic<?> pData,
      Int2ObjectMap<Supplier<ChunkProtoTickListFix.PoorMansPalettedContainer>> pPalette,
      byte pY,
      int pX,
      int pZ,
      String pName,
      Function<Dynamic<?>, String> pIdGetter
   ) {
      Stream<Dynamic<?>> $$7 = Stream.empty();
      List<? extends Dynamic<?>> $$8 = pData.get(pName).asList(Function.identity());

      for (int $$9 = 0; $$9 < $$8.size(); $$9++) {
         int $$10 = $$9 + pY;
         Supplier<ChunkProtoTickListFix.PoorMansPalettedContainer> $$11 = (Supplier<ChunkProtoTickListFix.PoorMansPalettedContainer>)pPalette.get($$10);
         Stream<? extends Dynamic<?>> $$12 = $$8.get($$9)
            .asStream()
            .mapToInt(p_185074_ -> p_185074_.asShort((short)-1))
            .filter(p_184993_ -> p_184993_ > 0)
            .mapToObj(p_185059_ -> this.createTick(pData, $$11, pX, $$10, pZ, p_185059_, pIdGetter));
         $$7 = Stream.concat($$7, $$12);
      }

      return pData.createList($$7);
   }

   private static String getBlock(@Nullable Dynamic<?> p_185032_) {
      return p_185032_ != null ? p_185032_.get("Name").asString("minecraft:air") : "minecraft:air";
   }

   private static String getLiquid(@Nullable Dynamic<?> p_185069_) {
      if (p_185069_ == null) {
         return "minecraft:empty";
      } else {
         String $$1 = p_185069_.get("Name").asString("");
         if ("minecraft:water".equals($$1)) {
            return p_185069_.get("Properties").get("level").asInt(0) == 0 ? "minecraft:water" : "minecraft:flowing_water";
         } else if ("minecraft:lava".equals($$1)) {
            return p_185069_.get("Properties").get("level").asInt(0) == 0 ? "minecraft:lava" : "minecraft:flowing_lava";
         } else {
            return !ALWAYS_WATERLOGGED.contains($$1) && !p_185069_.get("Properties").get("waterlogged").asBoolean(false)
               ? "minecraft:empty"
               : "minecraft:water";
         }
      }
   }

   private Dynamic<?> createTick(
      Dynamic<?> pData,
      @Nullable Supplier<ChunkProtoTickListFix.PoorMansPalettedContainer> pPalette,
      int pX,
      int pY,
      int pZ,
      int pIndex,
      Function<Dynamic<?>, String> pIdGetter
   ) {
      int $$7 = pIndex & 15;
      int $$8 = pIndex >>> 4 & 15;
      int $$9 = pIndex >>> 8 & 15;
      String $$10 = pIdGetter.apply(pPalette != null ? pPalette.get().get($$7, $$8, $$9) : null);
      return pData.createMap(
         ImmutableMap.builder()
            .put(pData.createString("i"), pData.createString($$10))
            .put(pData.createString("x"), pData.createInt(pX * 16 + $$7))
            .put(pData.createString("y"), pData.createInt(pY * 16 + $$8))
            .put(pData.createString("z"), pData.createInt(pZ * 16 + $$9))
            .put(pData.createString("t"), pData.createInt(0))
            .put(pData.createString("p"), pData.createInt(0))
            .build()
      );
   }

   public static final class PoorMansPalettedContainer {
      private static final long SIZE_BITS = 4L;
      private final List<? extends Dynamic<?>> palette;
      private final long[] data;
      private final int bits;
      private final long mask;
      private final int valuesPerLong;

      public PoorMansPalettedContainer(List<? extends Dynamic<?>> pPalette, long[] pData) {
         this.palette = pPalette;
         this.data = pData;
         this.bits = Math.max(4, ChunkHeightAndBiomeFix.ceillog2(pPalette.size()));
         this.mask = (1L << this.bits) - 1L;
         this.valuesPerLong = (char)(64 / this.bits);
      }

      @Nullable
      public Dynamic<?> get(int pX, int pY, int pZ) {
         int $$3 = this.palette.size();
         if ($$3 < 1) {
            return null;
         } else if ($$3 == 1) {
            return (Dynamic<?>)this.palette.get(0);
         } else {
            int $$4 = this.getIndex(pX, pY, pZ);
            int $$5 = $$4 / this.valuesPerLong;
            if ($$5 >= 0 && $$5 < this.data.length) {
               long $$6 = this.data[$$5];
               int $$7 = ($$4 - $$5 * this.valuesPerLong) * this.bits;
               int $$8 = (int)($$6 >> $$7 & this.mask);
               return (Dynamic<?>)($$8 >= 0 && $$8 < $$3 ? this.palette.get($$8) : null);
            } else {
               return null;
            }
         }
      }

      private int getIndex(int pX, int pY, int pZ) {
         return (pY << 4 | pZ) << 4 | pX;
      }

      public List<? extends Dynamic<?>> palette() {
         return this.palette;
      }

      public long[] data() {
         return this.data;
      }
   }
}
