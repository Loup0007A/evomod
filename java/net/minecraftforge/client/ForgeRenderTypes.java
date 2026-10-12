package net.minecraftforge.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.util.NonNullLazy;
import net.minecraftforge.common.util.NonNullSupplier;

public enum ForgeRenderTypes {
   ITEM_LAYERED_SOLID(() -> getItemLayeredSolid(TextureAtlas.LOCATION_BLOCKS)),
   ITEM_LAYERED_CUTOUT(() -> getItemLayeredCutout(TextureAtlas.LOCATION_BLOCKS)),
   ITEM_LAYERED_CUTOUT_MIPPED(() -> getItemLayeredCutoutMipped(TextureAtlas.LOCATION_BLOCKS)),
   ITEM_LAYERED_TRANSLUCENT(() -> getItemLayeredTranslucent(TextureAtlas.LOCATION_BLOCKS)),
   ITEM_UNSORTED_TRANSLUCENT(() -> getUnsortedTranslucent(TextureAtlas.LOCATION_BLOCKS)),
   ITEM_UNLIT_TRANSLUCENT(() -> getUnlitTranslucent(TextureAtlas.LOCATION_BLOCKS)),
   ITEM_UNSORTED_UNLIT_TRANSLUCENT(() -> getUnlitTranslucent(TextureAtlas.LOCATION_BLOCKS, false)),
   TRANSLUCENT_ON_PARTICLES_TARGET(() -> getTranslucentParticlesTarget(TextureAtlas.LOCATION_BLOCKS));

   public static boolean enableTextTextureLinearFiltering = false;
   private final NonNullSupplier<RenderType> renderTypeSupplier;

   public static RenderType getItemLayeredSolid(ResourceLocation textureLocation) {
      return ForgeRenderTypes.Internal.LAYERED_ITEM_SOLID.apply(textureLocation);
   }

   public static RenderType getItemLayeredCutout(ResourceLocation textureLocation) {
      return ForgeRenderTypes.Internal.LAYERED_ITEM_CUTOUT.apply(textureLocation);
   }

   public static RenderType getItemLayeredCutoutMipped(ResourceLocation textureLocation) {
      return ForgeRenderTypes.Internal.LAYERED_ITEM_CUTOUT_MIPPED.apply(textureLocation);
   }

   public static RenderType getItemLayeredTranslucent(ResourceLocation textureLocation) {
      return ForgeRenderTypes.Internal.LAYERED_ITEM_TRANSLUCENT.apply(textureLocation);
   }

   public static RenderType getUnsortedTranslucent(ResourceLocation textureLocation) {
      return ForgeRenderTypes.Internal.UNSORTED_TRANSLUCENT.apply(textureLocation);
   }

   public static RenderType getUnlitTranslucent(ResourceLocation textureLocation) {
      return ForgeRenderTypes.Internal.UNLIT_TRANSLUCENT_SORTED.apply(textureLocation);
   }

   public static RenderType getUnlitTranslucent(ResourceLocation textureLocation, boolean sortingEnabled) {
      return (sortingEnabled ? ForgeRenderTypes.Internal.UNLIT_TRANSLUCENT_SORTED : ForgeRenderTypes.Internal.UNLIT_TRANSLUCENT_UNSORTED)
         .apply(textureLocation);
   }

   public static RenderType getEntityCutoutMipped(ResourceLocation textureLocation) {
      return ForgeRenderTypes.Internal.LAYERED_ITEM_CUTOUT_MIPPED.apply(textureLocation);
   }

   public static RenderType getText(ResourceLocation locationIn) {
      return ForgeRenderTypes.Internal.TEXT.apply(locationIn);
   }

   public static RenderType getTextIntensity(ResourceLocation locationIn) {
      return ForgeRenderTypes.Internal.TEXT_INTENSITY.apply(locationIn);
   }

   public static RenderType getTextPolygonOffset(ResourceLocation locationIn) {
      return ForgeRenderTypes.Internal.TEXT_POLYGON_OFFSET.apply(locationIn);
   }

   public static RenderType getTextIntensityPolygonOffset(ResourceLocation locationIn) {
      return ForgeRenderTypes.Internal.TEXT_INTENSITY_POLYGON_OFFSET.apply(locationIn);
   }

   public static RenderType getTextSeeThrough(ResourceLocation locationIn) {
      return ForgeRenderTypes.Internal.TEXT_SEETHROUGH.apply(locationIn);
   }

   public static RenderType getTextIntensitySeeThrough(ResourceLocation locationIn) {
      return ForgeRenderTypes.Internal.TEXT_INTENSITY_SEETHROUGH.apply(locationIn);
   }

   public static RenderType getTranslucentParticlesTarget(ResourceLocation locationIn) {
      return ForgeRenderTypes.Internal.TRANSLUCENT_PARTICLES_TARGET.apply(locationIn);
   }

   private ForgeRenderTypes(NonNullSupplier<RenderType> renderTypeSupplier) {
      this.renderTypeSupplier = NonNullLazy.of(renderTypeSupplier);
   }

   public RenderType get() {
      return this.renderTypeSupplier.get();
   }

   private static class CustomizableTextureState extends RenderStateShard.TextureStateShard {
      private CustomizableTextureState(ResourceLocation resLoc, Supplier<Boolean> blur, Supplier<Boolean> mipmap) {
         super(resLoc, blur.get(), mipmap.get());
         this.setupState = () -> {
            this.blur = blur.get();
            this.mipmap = mipmap.get();
            TextureManager texturemanager = Minecraft.getInstance().getTextureManager();
            texturemanager.getTexture(resLoc).setFilter(this.blur, this.mipmap);
            RenderSystem.setShaderTexture(0, resLoc);
         };
      }
   }

   private static class Internal extends RenderType {
      private static final RenderStateShard.ShaderStateShard RENDERTYPE_ENTITY_TRANSLUCENT_UNLIT_SHADER = new RenderStateShard.ShaderStateShard(
         ForgeHooksClient.ClientEvents::getEntityTranslucentUnlitShader
      );
      public static Function<ResourceLocation, RenderType> UNSORTED_TRANSLUCENT = Util.memoize(ForgeRenderTypes.Internal::unsortedTranslucent);
      private static final BiFunction<ResourceLocation, Boolean, RenderType> ENTITY_TRANSLUCENT = Util.memoize(
         (p_173227_, p_173228_) -> {
            RenderType.CompositeState rendertype$compositestate = RenderType.CompositeState.builder()
               .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
               .setTextureState(new RenderStateShard.TextureStateShard(p_173227_, false, false))
               .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
               .setCullState(NO_CULL)
               .setLightmapState(LIGHTMAP)
               .setOverlayState(OVERLAY)
               .createCompositeState(p_173228_);
            return create("entity_translucent", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, true, true, rendertype$compositestate);
         }
      );
      public static Function<ResourceLocation, RenderType> UNLIT_TRANSLUCENT_SORTED = Util.memoize(tex -> unlitTranslucent(tex, true));
      public static Function<ResourceLocation, RenderType> UNLIT_TRANSLUCENT_UNSORTED = Util.memoize(tex -> unlitTranslucent(tex, false));
      public static Function<ResourceLocation, RenderType> LAYERED_ITEM_SOLID = Util.memoize(ForgeRenderTypes.Internal::layeredItemSolid);
      public static Function<ResourceLocation, RenderType> LAYERED_ITEM_CUTOUT = Util.memoize(ForgeRenderTypes.Internal::layeredItemCutout);
      public static Function<ResourceLocation, RenderType> LAYERED_ITEM_CUTOUT_MIPPED = Util.memoize(ForgeRenderTypes.Internal::layeredItemCutoutMipped);
      public static Function<ResourceLocation, RenderType> LAYERED_ITEM_TRANSLUCENT = Util.memoize(ForgeRenderTypes.Internal::layeredItemTranslucent);
      public static Function<ResourceLocation, RenderType> TEXT = Util.memoize(ForgeRenderTypes.Internal::getText);
      public static Function<ResourceLocation, RenderType> TEXT_INTENSITY = Util.memoize(ForgeRenderTypes.Internal::getTextIntensity);
      public static Function<ResourceLocation, RenderType> TEXT_POLYGON_OFFSET = Util.memoize(ForgeRenderTypes.Internal::getTextPolygonOffset);
      public static Function<ResourceLocation, RenderType> TEXT_INTENSITY_POLYGON_OFFSET = Util.memoize(
         ForgeRenderTypes.Internal::getTextIntensityPolygonOffset
      );
      public static Function<ResourceLocation, RenderType> TEXT_SEETHROUGH = Util.memoize(ForgeRenderTypes.Internal::getTextSeeThrough);
      public static Function<ResourceLocation, RenderType> TEXT_INTENSITY_SEETHROUGH = Util.memoize(ForgeRenderTypes.Internal::getTextIntensitySeeThrough);
      public static Function<ResourceLocation, RenderType> TRANSLUCENT_PARTICLES_TARGET = Util.memoize(ForgeRenderTypes.Internal::getTranslucentParticlesTarget);

      private Internal(
         String name, VertexFormat fmt, VertexFormat.Mode glMode, int size, boolean doCrumbling, boolean depthSorting, Runnable onEnable, Runnable onDisable
      ) {
         super(name, fmt, glMode, size, doCrumbling, depthSorting, onEnable, onDisable);
         throw new IllegalStateException("This class must not be instantiated");
      }

      private static RenderType unsortedTranslucent(ResourceLocation textureLocation) {
         boolean sortingEnabled = false;
         RenderType.CompositeState renderState = RenderType.CompositeState.builder()
            .setShaderState(RenderType.RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
            .setTextureState(new RenderStateShard.TextureStateShard(textureLocation, false, false))
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
            .setCullState(NO_CULL)
            .setLightmapState(LIGHTMAP)
            .setOverlayState(OVERLAY)
            .createCompositeState(true);
         return create("forge_entity_unsorted_translucent", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, true, false, renderState);
      }

      private static RenderType unlitTranslucent(ResourceLocation textureLocation, boolean sortingEnabled) {
         RenderType.CompositeState renderState = RenderType.CompositeState.builder()
            .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_UNLIT_SHADER)
            .setTextureState(new RenderStateShard.TextureStateShard(textureLocation, false, false))
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
            .setCullState(NO_CULL)
            .setLightmapState(LIGHTMAP)
            .setOverlayState(OVERLAY)
            .createCompositeState(true);
         return create("forge_entity_unlit_translucent", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, true, sortingEnabled, renderState);
      }

      private static RenderType layeredItemSolid(ResourceLocation locationIn) {
         RenderType.CompositeState rendertype$state = RenderType.CompositeState.builder()
            .setShaderState(RenderType.RENDERTYPE_ENTITY_SOLID_SHADER)
            .setTextureState(new RenderStateShard.TextureStateShard(locationIn, false, false))
            .setTransparencyState(NO_TRANSPARENCY)
            .setLightmapState(LIGHTMAP)
            .setOverlayState(OVERLAY)
            .createCompositeState(true);
         return create("forge_item_entity_solid", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, true, false, rendertype$state);
      }

      private static RenderType layeredItemCutout(ResourceLocation locationIn) {
         RenderType.CompositeState rendertype$state = RenderType.CompositeState.builder()
            .setShaderState(RenderType.RENDERTYPE_ENTITY_CUTOUT_SHADER)
            .setTextureState(new RenderStateShard.TextureStateShard(locationIn, false, false))
            .setTransparencyState(NO_TRANSPARENCY)
            .setLightmapState(LIGHTMAP)
            .setOverlayState(OVERLAY)
            .createCompositeState(true);
         return create("forge_item_entity_cutout", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, true, false, rendertype$state);
      }

      private static RenderType layeredItemCutoutMipped(ResourceLocation locationIn) {
         RenderType.CompositeState rendertype$state = RenderType.CompositeState.builder()
            .setShaderState(RenderType.RENDERTYPE_ENTITY_SMOOTH_CUTOUT_SHADER)
            .setTextureState(new RenderStateShard.TextureStateShard(locationIn, false, true))
            .setTransparencyState(NO_TRANSPARENCY)
            .setLightmapState(LIGHTMAP)
            .setOverlayState(OVERLAY)
            .createCompositeState(true);
         return create("forge_item_entity_cutout_mipped", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, true, false, rendertype$state);
      }

      private static RenderType layeredItemTranslucent(ResourceLocation locationIn) {
         RenderType.CompositeState rendertype$state = RenderType.CompositeState.builder()
            .setShaderState(RenderType.RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
            .setTextureState(new RenderStateShard.TextureStateShard(locationIn, false, false))
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
            .setLightmapState(LIGHTMAP)
            .setOverlayState(OVERLAY)
            .createCompositeState(true);
         return create("forge_item_entity_translucent_cull", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, true, true, rendertype$state);
      }

      private static RenderType getText(ResourceLocation locationIn) {
         RenderType.CompositeState rendertype$state = RenderType.CompositeState.builder()
            .setShaderState(RENDERTYPE_TEXT_SHADER)
            .setTextureState(new ForgeRenderTypes.CustomizableTextureState(locationIn, () -> ForgeRenderTypes.enableTextTextureLinearFiltering, () -> false))
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
            .setLightmapState(LIGHTMAP)
            .createCompositeState(false);
         return create("forge_text", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, VertexFormat.Mode.QUADS, 256, false, true, rendertype$state);
      }

      private static RenderType getTextIntensity(ResourceLocation locationIn) {
         RenderType.CompositeState rendertype$state = RenderType.CompositeState.builder()
            .setShaderState(RENDERTYPE_TEXT_INTENSITY_SHADER)
            .setTextureState(new ForgeRenderTypes.CustomizableTextureState(locationIn, () -> ForgeRenderTypes.enableTextTextureLinearFiltering, () -> false))
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
            .setLightmapState(LIGHTMAP)
            .createCompositeState(false);
         return create("text_intensity", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, VertexFormat.Mode.QUADS, 256, false, true, rendertype$state);
      }

      private static RenderType getTextPolygonOffset(ResourceLocation locationIn) {
         RenderType.CompositeState rendertype$state = RenderType.CompositeState.builder()
            .setShaderState(RENDERTYPE_TEXT_SHADER)
            .setTextureState(new ForgeRenderTypes.CustomizableTextureState(locationIn, () -> ForgeRenderTypes.enableTextTextureLinearFiltering, () -> false))
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
            .setLightmapState(LIGHTMAP)
            .setLayeringState(POLYGON_OFFSET_LAYERING)
            .createCompositeState(false);
         return create("text_intensity", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, VertexFormat.Mode.QUADS, 256, false, true, rendertype$state);
      }

      private static RenderType getTextIntensityPolygonOffset(ResourceLocation locationIn) {
         RenderType.CompositeState rendertype$state = RenderType.CompositeState.builder()
            .setShaderState(RENDERTYPE_TEXT_INTENSITY_SHADER)
            .setTextureState(new ForgeRenderTypes.CustomizableTextureState(locationIn, () -> ForgeRenderTypes.enableTextTextureLinearFiltering, () -> false))
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
            .setLightmapState(LIGHTMAP)
            .setLayeringState(POLYGON_OFFSET_LAYERING)
            .createCompositeState(false);
         return create("text_intensity", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, VertexFormat.Mode.QUADS, 256, false, true, rendertype$state);
      }

      private static RenderType getTextSeeThrough(ResourceLocation locationIn) {
         RenderType.CompositeState rendertype$state = RenderType.CompositeState.builder()
            .setShaderState(RENDERTYPE_TEXT_SEE_THROUGH_SHADER)
            .setTextureState(new ForgeRenderTypes.CustomizableTextureState(locationIn, () -> ForgeRenderTypes.enableTextTextureLinearFiltering, () -> false))
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
            .setLightmapState(LIGHTMAP)
            .setDepthTestState(NO_DEPTH_TEST)
            .setWriteMaskState(COLOR_WRITE)
            .createCompositeState(false);
         return create("forge_text_see_through", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, VertexFormat.Mode.QUADS, 256, false, true, rendertype$state);
      }

      private static RenderType getTextIntensitySeeThrough(ResourceLocation locationIn) {
         RenderType.CompositeState rendertype$state = RenderType.CompositeState.builder()
            .setShaderState(RENDERTYPE_TEXT_INTENSITY_SEE_THROUGH_SHADER)
            .setTextureState(new ForgeRenderTypes.CustomizableTextureState(locationIn, () -> ForgeRenderTypes.enableTextTextureLinearFiltering, () -> false))
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
            .setLightmapState(LIGHTMAP)
            .setDepthTestState(NO_DEPTH_TEST)
            .setWriteMaskState(COLOR_WRITE)
            .createCompositeState(false);
         return create("forge_text_see_through", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, VertexFormat.Mode.QUADS, 256, false, true, rendertype$state);
      }

      private static RenderType getTranslucentParticlesTarget(ResourceLocation locationIn) {
         RenderType.CompositeState rendertype$state = RenderType.CompositeState.builder()
            .setShaderState(RENDERTYPE_TRANSLUCENT_SHADER)
            .setTextureState(new RenderStateShard.TextureStateShard(locationIn, false, true))
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
            .setLightmapState(LIGHTMAP)
            .setOutputState(PARTICLES_TARGET)
            .createCompositeState(true);
         return create("forge_translucent_particles_target", DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS, 2097152, true, true, rendertype$state);
      }
   }
}
