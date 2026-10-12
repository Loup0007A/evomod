package net.minecraft.client.resources.model;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.RenderTypeGroup;
import net.minecraftforge.client.model.data.ModelData;

@OnlyIn(Dist.CLIENT)
public class SimpleBakedModel implements BakedModel {
   protected final List<BakedQuad> unculledFaces;
   protected final Map<Direction, List<BakedQuad>> culledFaces;
   protected final boolean hasAmbientOcclusion;
   protected final boolean isGui3d;
   protected final boolean usesBlockLight;
   protected final TextureAtlasSprite particleIcon;
   protected final ItemTransforms transforms;
   protected final ItemOverrides overrides;
   protected final ChunkRenderTypeSet blockRenderTypes;
   protected final List<RenderType> itemRenderTypes;
   protected final List<RenderType> fabulousItemRenderTypes;

   @Deprecated
   public SimpleBakedModel(
      List<BakedQuad> pUnculledFaces,
      Map<Direction, List<BakedQuad>> pCulledFaces,
      boolean pHasAmbientOcclusion,
      boolean pUsesBlockLight,
      boolean pIsGui3d,
      TextureAtlasSprite pParticleIcon,
      ItemTransforms pTransforms,
      ItemOverrides pOverrides
   ) {
      this(pUnculledFaces, pCulledFaces, pHasAmbientOcclusion, pUsesBlockLight, pIsGui3d, pParticleIcon, pTransforms, pOverrides, RenderTypeGroup.EMPTY);
   }

   public SimpleBakedModel(
      List<BakedQuad> p_119489_,
      Map<Direction, List<BakedQuad>> p_119490_,
      boolean p_119491_,
      boolean p_119492_,
      boolean p_119493_,
      TextureAtlasSprite p_119494_,
      ItemTransforms p_119495_,
      ItemOverrides p_119496_,
      RenderTypeGroup renderTypes
   ) {
      this.unculledFaces = p_119489_;
      this.culledFaces = p_119490_;
      this.hasAmbientOcclusion = p_119491_;
      this.isGui3d = p_119493_;
      this.usesBlockLight = p_119492_;
      this.particleIcon = p_119494_;
      this.transforms = p_119495_;
      this.overrides = p_119496_;
      this.blockRenderTypes = !renderTypes.isEmpty() ? ChunkRenderTypeSet.of(renderTypes.block()) : null;
      this.itemRenderTypes = !renderTypes.isEmpty() ? List.of(renderTypes.entity()) : null;
      this.fabulousItemRenderTypes = !renderTypes.isEmpty() ? List.of(renderTypes.entityFabulous()) : null;
   }

   @Override
   public List<BakedQuad> getQuads(@Nullable BlockState pState, @Nullable Direction pDirection, RandomSource pRandom) {
      return pDirection == null ? this.unculledFaces : this.culledFaces.get(pDirection);
   }

   @Override
   public boolean useAmbientOcclusion() {
      return this.hasAmbientOcclusion;
   }

   @Override
   public boolean isGui3d() {
      return this.isGui3d;
   }

   @Override
   public boolean usesBlockLight() {
      return this.usesBlockLight;
   }

   @Override
   public boolean isCustomRenderer() {
      return false;
   }

   @Override
   public TextureAtlasSprite getParticleIcon() {
      return this.particleIcon;
   }

   @Override
   public ItemTransforms getTransforms() {
      return this.transforms;
   }

   @Override
   public ItemOverrides getOverrides() {
      return this.overrides;
   }

   @Override
   public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
      return this.blockRenderTypes != null ? this.blockRenderTypes : BakedModel.super.getRenderTypes(state, rand, data);
   }

   @Override
   public List<RenderType> getRenderTypes(ItemStack itemStack, boolean fabulous) {
      if (!fabulous) {
         if (this.itemRenderTypes != null) {
            return this.itemRenderTypes;
         }
      } else if (this.fabulousItemRenderTypes != null) {
         return this.fabulousItemRenderTypes;
      }

      return BakedModel.super.getRenderTypes(itemStack, fabulous);
   }

   @OnlyIn(Dist.CLIENT)
   public static class Builder {
      private final List<BakedQuad> unculledFaces = Lists.newArrayList();
      private final Map<Direction, List<BakedQuad>> culledFaces = Maps.newEnumMap(Direction.class);
      private final ItemOverrides overrides;
      private final boolean hasAmbientOcclusion;
      private TextureAtlasSprite particleIcon;
      private final boolean usesBlockLight;
      private final boolean isGui3d;
      private final ItemTransforms transforms;
      private RenderTypeGroup renderTypes = RenderTypeGroup.EMPTY;

      public Builder(BlockModel pBlockModel, ItemOverrides pOverrides, boolean pIsGui3d) {
         this(pBlockModel.hasAmbientOcclusion(), pBlockModel.getGuiLight().lightLikeBlock(), pIsGui3d, pBlockModel.getTransforms(), pOverrides);
      }

      public Builder(boolean pHasAmbientOcclusion, boolean pUsesBlockLight, boolean pIsGui3d, ItemTransforms pTransforms, ItemOverrides pOverrides) {
         for (Direction direction : Direction.values()) {
            this.culledFaces.put(direction, Lists.newArrayList());
         }

         this.overrides = pOverrides;
         this.hasAmbientOcclusion = pHasAmbientOcclusion;
         this.usesBlockLight = pUsesBlockLight;
         this.isGui3d = pIsGui3d;
         this.transforms = pTransforms;
      }

      public SimpleBakedModel.Builder addCulledFace(Direction pFacing, BakedQuad pQuad) {
         this.culledFaces.get(pFacing).add(pQuad);
         return this;
      }

      public SimpleBakedModel.Builder addUnculledFace(BakedQuad pQuad) {
         this.unculledFaces.add(pQuad);
         return this;
      }

      public SimpleBakedModel.Builder particle(TextureAtlasSprite pParticleIcon) {
         this.particleIcon = pParticleIcon;
         return this;
      }

      public SimpleBakedModel.Builder item() {
         return this;
      }

      public SimpleBakedModel.Builder renderTypes(RenderTypeGroup renderTypes) {
         this.renderTypes = renderTypes;
         return this;
      }

      public BakedModel build() {
         if (this.particleIcon == null) {
            throw new RuntimeException("Missing particle!");
         } else {
            return new SimpleBakedModel(
               this.unculledFaces,
               this.culledFaces,
               this.hasAmbientOcclusion,
               this.usesBlockLight,
               this.isGui3d,
               this.particleIcon,
               this.transforms,
               this.overrides,
               this.renderTypes
            );
         }
      }
   }
}
