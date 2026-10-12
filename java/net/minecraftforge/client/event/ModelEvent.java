package net.minecraftforge.client.event;

import com.google.common.base.Preconditions;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.geometry.IGeometryLoader;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.event.IModBusEvent;
import org.jetbrains.annotations.ApiStatus.Internal;

public abstract class ModelEvent extends Event {
   @Internal
   protected ModelEvent() {
   }

   public static class BakingCompleted extends ModelEvent implements IModBusEvent {
      private final ModelManager modelManager;
      private final Map<ModelResourceLocation, BakedModel> models;
      private final ModelBakery modelBakery;

      @Internal
      public BakingCompleted(ModelManager modelManager, Map<ModelResourceLocation, BakedModel> models, ModelBakery modelBakery) {
         this.modelManager = modelManager;
         this.models = models;
         this.modelBakery = modelBakery;
      }

      public ModelManager getModelManager() {
         return this.modelManager;
      }

      public Map<ModelResourceLocation, BakedModel> getModels() {
         return this.models;
      }

      public ModelBakery getModelBakery() {
         return this.modelBakery;
      }
   }

   public static class ModifyBakingResult extends ModelEvent implements IModBusEvent {
      private final Map<ModelResourceLocation, BakedModel> models;
      private final ModelBakery modelBakery;

      @Internal
      public ModifyBakingResult(Map<ModelResourceLocation, BakedModel> models, ModelBakery modelBakery) {
         this.models = models;
         this.modelBakery = modelBakery;
      }

      public Map<ModelResourceLocation, BakedModel> getModels() {
         return this.models;
      }

      public ModelBakery getModelBakery() {
         return this.modelBakery;
      }
   }

   public static class RegisterAdditional extends ModelEvent implements IModBusEvent {
      private final Set<ModelResourceLocation> models;

      @Internal
      public RegisterAdditional(Set<ModelResourceLocation> models) {
         this.models = models;
      }

      public void register(ModelResourceLocation model) {
         this.models.add(model);
      }
   }

   public static class RegisterGeometryLoaders extends ModelEvent implements IModBusEvent {
      private final Map<ResourceLocation, IGeometryLoader<?>> loaders;

      @Internal
      public RegisterGeometryLoaders(Map<ResourceLocation, IGeometryLoader<?>> loaders) {
         this.loaders = loaders;
      }

      public void register(String name, IGeometryLoader<?> loader) {
         ResourceLocation key = ResourceLocation.fromNamespaceAndPath(ModLoadingContext.get().getActiveNamespace(), name);
         Preconditions.checkArgument(!this.loaders.containsKey(key), "Geometry loader already registered: " + key);
         this.loaders.put(key, loader);
      }
   }
}
