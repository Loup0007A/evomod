package net.minecraft.network.chat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.BitSet;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import org.apache.commons.lang3.StringUtils;

public class FilterMask {
   public static final Codec<FilterMask> CODEC = StringRepresentable.fromEnum(FilterMask.Type::values).dispatch(FilterMask::type, FilterMask.Type::codec);
   public static final FilterMask FULLY_FILTERED = new FilterMask(new BitSet(0), FilterMask.Type.FULLY_FILTERED);
   public static final FilterMask PASS_THROUGH = new FilterMask(new BitSet(0), FilterMask.Type.PASS_THROUGH);
   public static final Style FILTERED_STYLE = Style.EMPTY
      .withColor(ChatFormatting.DARK_GRAY)
      .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("chat.filtered")));
   static final MapCodec<FilterMask> PASS_THROUGH_CODEC = MapCodec.unit(PASS_THROUGH);
   static final MapCodec<FilterMask> FULLY_FILTERED_CODEC = MapCodec.unit(FULLY_FILTERED);
   static final MapCodec<FilterMask> PARTIALLY_FILTERED_CODEC = ExtraCodecs.BIT_SET.xmap(FilterMask::new, FilterMask::mask).fieldOf("value");
   private static final char HASH = '#';
   private final BitSet mask;
   private final FilterMask.Type type;

   private FilterMask(BitSet pMask, FilterMask.Type pType) {
      this.mask = pMask;
      this.type = pType;
   }

   private FilterMask(BitSet p_253780_) {
      this.mask = p_253780_;
      this.type = FilterMask.Type.PARTIALLY_FILTERED;
   }

   public FilterMask(int pSize) {
      this(new BitSet(pSize), FilterMask.Type.PARTIALLY_FILTERED);
   }

   private FilterMask.Type type() {
      return this.type;
   }

   private BitSet mask() {
      return this.mask;
   }

   public static FilterMask read(FriendlyByteBuf pBuffer) {
      FilterMask.Type $$1 = pBuffer.readEnum(FilterMask.Type.class);

      return switch ($$1) {
         case PASS_THROUGH -> PASS_THROUGH;
         case FULLY_FILTERED -> FULLY_FILTERED;
         case PARTIALLY_FILTERED -> new FilterMask(pBuffer.readBitSet(), FilterMask.Type.PARTIALLY_FILTERED);
      };
   }

   public static void write(FriendlyByteBuf pBuffer, FilterMask pMask) {
      pBuffer.writeEnum(pMask.type);
      if (pMask.type == FilterMask.Type.PARTIALLY_FILTERED) {
         pBuffer.writeBitSet(pMask.mask);
      }
   }

   public void setFiltered(int pBitIndex) {
      this.mask.set(pBitIndex);
   }

   @Nullable
   public String apply(String pText) {
      return switch (this.type) {
         case PASS_THROUGH -> pText;
         case FULLY_FILTERED -> null;
         case PARTIALLY_FILTERED -> {
            char[] $$1 = pText.toCharArray();

            for (int $$2 = 0; $$2 < $$1.length && $$2 < this.mask.length(); $$2++) {
               if (this.mask.get($$2)) {
                  $$1[$$2] = '#';
               }
            }

            yield new String($$1);
         }
      };
   }

   @Nullable
   public Component applyWithFormatting(String pText) {
      return switch (this.type) {
         case PASS_THROUGH -> Component.literal(pText);
         case FULLY_FILTERED -> null;
         case PARTIALLY_FILTERED -> {
            MutableComponent $$1 = Component.empty();
            int $$2 = 0;
            boolean $$3 = this.mask.get(0);

            while (true) {
               int $$4 = $$3 ? this.mask.nextClearBit($$2) : this.mask.nextSetBit($$2);
               $$4 = $$4 < 0 ? pText.length() : $$4;
               if ($$4 == $$2) {
                  yield $$1;
               }

               if ($$3) {
                  $$1.append(Component.literal(StringUtils.repeat('#', $$4 - $$2)).withStyle(FILTERED_STYLE));
               } else {
                  $$1.append(pText.substring($$2, $$4));
               }

               $$3 = !$$3;
               $$2 = $$4;
            }
         }
      };
   }

   public boolean isEmpty() {
      return this.type == FilterMask.Type.PASS_THROUGH;
   }

   public boolean isFullyFiltered() {
      return this.type == FilterMask.Type.FULLY_FILTERED;
   }

   @Override
   public boolean equals(Object pOther) {
      if (this == pOther) {
         return true;
      } else if (pOther != null && this.getClass() == pOther.getClass()) {
         FilterMask $$1 = (FilterMask)pOther;
         return this.mask.equals($$1.mask) && this.type == $$1.type;
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      int $$0 = this.mask.hashCode();
      return 31 * $$0 + this.type.hashCode();
   }

   static enum Type implements StringRepresentable {
      PASS_THROUGH("pass_through", () -> FilterMask.PASS_THROUGH_CODEC),
      FULLY_FILTERED("fully_filtered", () -> FilterMask.FULLY_FILTERED_CODEC),
      PARTIALLY_FILTERED("partially_filtered", () -> FilterMask.PARTIALLY_FILTERED_CODEC);

      private final String serializedName;
      private final Supplier<MapCodec<FilterMask>> codec;

      private Type(final String pSerializedName, final Supplier<MapCodec<FilterMask>> pCodec) {
         this.serializedName = pSerializedName;
         this.codec = pCodec;
      }

      @Override
      public String getSerializedName() {
         return this.serializedName;
      }

      private MapCodec<FilterMask> codec() {
         return this.codec.get();
      }
   }
}
