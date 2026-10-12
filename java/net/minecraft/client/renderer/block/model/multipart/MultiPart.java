package net.minecraft.client.renderer.block.model.multipart;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.block.model.BlockModelDefinition;
import net.minecraft.client.renderer.block.model.MultiVariant;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.MultiPartBakedModel;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MultiPart implements UnbakedModel {
   private final StateDefinition<Block, BlockState> definition;
   private final List<Selector> selectors;

   public MultiPart(StateDefinition<Block, BlockState> pDefinition, List<Selector> pSelectors) {
      this.definition = pDefinition;
      this.selectors = pSelectors;
   }

   public List<Selector> getSelectors() {
      return this.selectors;
   }

   public Set<MultiVariant> getMultiVariants() {
      Set<MultiVariant> $$0 = Sets.newHashSet();

      for (Selector $$1 : this.selectors) {
         $$0.add($$1.getVariant());
      }

      return $$0;
   }

   @Override
   public boolean equals(Object pOther) {
      if (this == pOther) {
         return true;
      } else {
         return !(pOther instanceof MultiPart $$1) ? false : Objects.equals(this.definition, $$1.definition) && Objects.equals(this.selectors, $$1.selectors);
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.definition, this.selectors);
   }

   @Override
   public Collection<ResourceLocation> getDependencies() {
      return this.getSelectors().stream().flatMap(p_111969_ -> p_111969_.getVariant().getDependencies().stream()).collect(Collectors.toSet());
   }

   @Override
   public void resolveParents(Function<ResourceLocation, UnbakedModel> pResolver) {
      this.getSelectors().forEach(p_247936_ -> p_247936_.getVariant().resolveParents(pResolver));
   }

   @Nullable
   @Override
   public BakedModel bake(ModelBaker pBaker, Function<Material, TextureAtlasSprite> pSpriteGetter, ModelState pState) {
      MultiPartBakedModel.Builder $$3 = new MultiPartBakedModel.Builder();

      for (Selector $$4 : this.getSelectors()) {
         BakedModel $$5 = $$4.getVariant().bake(pBaker, pSpriteGetter, pState);
         if ($$5 != null) {
            $$3.add($$4.getPredicate(this.definition), $$5);
         }
      }

      return $$3.build();
   }

   @OnlyIn(Dist.CLIENT)
   public static class Deserializer implements JsonDeserializer<MultiPart> {
      private final BlockModelDefinition.Context context;

      public Deserializer(BlockModelDefinition.Context pContext) {
         this.context = pContext;
      }

      public MultiPart deserialize(JsonElement pJson, Type pType, JsonDeserializationContext pJsonContext) throws JsonParseException {
         return new MultiPart(this.context.getDefinition(), this.getSelectors(pJsonContext, pJson.getAsJsonArray()));
      }

      private List<Selector> getSelectors(JsonDeserializationContext pJsonContext, JsonArray pElements) {
         List<Selector> $$2 = Lists.newArrayList();

         for (JsonElement $$3 : pElements) {
            $$2.add((Selector)pJsonContext.deserialize($$3, Selector.class));
         }

         return $$2;
      }
   }
}
