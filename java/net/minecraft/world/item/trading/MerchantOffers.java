package net.minecraft.world.item.trading;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

public class MerchantOffers extends ArrayList<MerchantOffer> {
   public static final Codec<MerchantOffers> CODEC = MerchantOffer.CODEC.listOf().fieldOf("Recipes").xmap(MerchantOffers::new, Function.identity()).codec();
   public static final StreamCodec<RegistryFriendlyByteBuf, MerchantOffers> STREAM_CODEC = MerchantOffer.STREAM_CODEC
      .apply(ByteBufCodecs.collection(MerchantOffers::new));

   public MerchantOffers() {
   }

   private MerchantOffers(int p_220323_) {
      super(p_220323_);
   }

   private MerchantOffers(Collection<MerchantOffer> p_331802_) {
      super(p_331802_);
   }

   @Nullable
   public MerchantOffer getRecipeFor(ItemStack pStackA, ItemStack pStackB, int pIndex) {
      if (pIndex > 0 && pIndex < this.size()) {
         MerchantOffer $$3 = this.get(pIndex);
         return $$3.satisfiedBy(pStackA, pStackB) ? $$3 : null;
      } else {
         for (int $$4 = 0; $$4 < this.size(); $$4++) {
            MerchantOffer $$5 = this.get($$4);
            if ($$5.satisfiedBy(pStackA, pStackB)) {
               return $$5;
            }
         }

         return null;
      }
   }

   public MerchantOffers copy() {
      MerchantOffers $$0 = new MerchantOffers(this.size());

      for (MerchantOffer $$1 : this) {
         $$0.add($$1.copy());
      }

      return $$0;
   }
}
