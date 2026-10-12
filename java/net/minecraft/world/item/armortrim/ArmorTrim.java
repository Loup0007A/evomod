package net.minecraft.world.item.armortrim;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;

public class ArmorTrim implements TooltipProvider {
   public static final Codec<ArmorTrim> CODEC = RecordCodecBuilder.create(
      p_327186_ -> p_327186_.group(
               TrimMaterial.CODEC.fieldOf("material").forGetter(ArmorTrim::material),
               TrimPattern.CODEC.fieldOf("pattern").forGetter(ArmorTrim::pattern),
               Codec.BOOL.optionalFieldOf("show_in_tooltip", true).forGetter(p_327187_ -> p_327187_.showInTooltip)
            )
            .apply(p_327186_, ArmorTrim::new)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, ArmorTrim> STREAM_CODEC = StreamCodec.composite(
      TrimMaterial.STREAM_CODEC,
      ArmorTrim::material,
      TrimPattern.STREAM_CODEC,
      ArmorTrim::pattern,
      ByteBufCodecs.BOOL,
      p_327185_ -> p_327185_.showInTooltip,
      ArmorTrim::new
   );
   private static final Component UPGRADE_TITLE = Component.translatable(
         Util.makeDescriptionId("item", ResourceLocation.withDefaultNamespace("smithing_template.upgrade"))
      )
      .withStyle(ChatFormatting.GRAY);
   private final Holder<TrimMaterial> material;
   private final Holder<TrimPattern> pattern;
   private final boolean showInTooltip;
   private final Function<Holder<ArmorMaterial>, ResourceLocation> innerTexture;
   private final Function<Holder<ArmorMaterial>, ResourceLocation> outerTexture;

   private ArmorTrim(
      Holder<TrimMaterial> pMaterial,
      Holder<TrimPattern> pPattern,
      boolean pShowInTooltip,
      Function<Holder<ArmorMaterial>, ResourceLocation> pInnerTexture,
      Function<Holder<ArmorMaterial>, ResourceLocation> pOuterTexture
   ) {
      this.material = pMaterial;
      this.pattern = pPattern;
      this.showInTooltip = pShowInTooltip;
      this.innerTexture = pInnerTexture;
      this.outerTexture = pOuterTexture;
   }

   public ArmorTrim(Holder<TrimMaterial> p_336165_, Holder<TrimPattern> p_333838_, boolean p_331209_) {
      this.material = p_336165_;
      this.pattern = p_333838_;
      this.innerTexture = Util.memoize(p_327184_ -> {
         ResourceLocation $$3 = p_333838_.value().assetId();
         String $$4 = getColorPaletteSuffix(p_336165_, p_327184_);
         return $$3.withPath(p_266737_ -> "trims/models/armor/" + p_266737_ + "_leggings_" + $$4);
      });
      this.outerTexture = Util.memoize(p_327190_ -> {
         ResourceLocation $$3 = p_333838_.value().assetId();
         String $$4 = getColorPaletteSuffix(p_336165_, p_327190_);
         return $$3.withPath(p_266864_ -> "trims/models/armor/" + p_266864_ + "_" + $$4);
      });
      this.showInTooltip = p_331209_;
   }

   public ArmorTrim(Holder<TrimMaterial> pMaterial, Holder<TrimPattern> pPattern) {
      this(pMaterial, pPattern, true);
   }

   private static String getColorPaletteSuffix(Holder<TrimMaterial> pTrimMaterial, Holder<ArmorMaterial> pArmorMaterial) {
      Map<Holder<ArmorMaterial>, String> $$2 = pTrimMaterial.value().overrideArmorMaterials();
      String $$3 = $$2.get(pArmorMaterial);
      return $$3 != null ? $$3 : pTrimMaterial.value().assetName();
   }

   public boolean hasPatternAndMaterial(Holder<TrimPattern> pPattern, Holder<TrimMaterial> pMaterial) {
      return pPattern.equals(this.pattern) && pMaterial.equals(this.material);
   }

   public Holder<TrimPattern> pattern() {
      return this.pattern;
   }

   public Holder<TrimMaterial> material() {
      return this.material;
   }

   public ResourceLocation innerTexture(Holder<ArmorMaterial> pArmorMaterial) {
      return this.innerTexture.apply(pArmorMaterial);
   }

   public ResourceLocation outerTexture(Holder<ArmorMaterial> pArmorMaterial) {
      return this.outerTexture.apply(pArmorMaterial);
   }

   @Override
   public boolean equals(Object pOther) {
      return !(pOther instanceof ArmorTrim $$1)
         ? false
         : this.showInTooltip == $$1.showInTooltip && this.pattern.equals($$1.pattern) && this.material.equals($$1.material);
   }

   @Override
   public int hashCode() {
      int $$0 = this.material.hashCode();
      $$0 = 31 * $$0 + this.pattern.hashCode();
      return 31 * $$0 + (this.showInTooltip ? 1 : 0);
   }

   @Override
   public void addToTooltip(Item.TooltipContext pContext, Consumer<Component> pTooltipAdder, TooltipFlag pTooltipFlag) {
      if (this.showInTooltip) {
         pTooltipAdder.accept(UPGRADE_TITLE);
         pTooltipAdder.accept(CommonComponents.space().append(this.pattern.value().copyWithStyle(this.material)));
         pTooltipAdder.accept(CommonComponents.space().append(this.material.value().description()));
      }
   }

   public ArmorTrim withTooltip(boolean pShowInTooltip) {
      return new ArmorTrim(this.material, this.pattern, pShowInTooltip, this.innerTexture, this.outerTexture);
   }
}
