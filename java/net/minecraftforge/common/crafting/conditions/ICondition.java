package net.minecraftforge.common.crafting.conditions;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.ApiStatus.Internal;

public interface ICondition {
   Codec<ICondition> CODEC = Codec.lazyInitialized(
      () -> ForgeRegistries.CONDITION_SERIALIZERS.get().getCodec().dispatch(ICondition::codec, Function.identity())
   );
   String DEFAULT_FIELD = "forge:condition";
   MapCodec<Optional<ICondition>> OPTIONAL_FEILD_CODEC = CODEC.optionalFieldOf("forge:condition");
   Codec<ICondition> SAFE_CODEC = CODEC.orElse(FalseCondition.INSTANCE);

   boolean test(ICondition.IContext var1, DynamicOps<?> var2);

   MapCodec<? extends ICondition> codec();

   public interface IContext {
      @Internal
      ResourceLocation KEY = ResourceLocation.fromNamespaceAndPath("forge", "condition_context");
      ICondition.IContext EMPTY = new ICondition.IContext() {
         @Override
         public <T> Map<ResourceLocation, Collection<Holder<T>>> getAllTags(ResourceKey<? extends Registry<T>> registry) {
            return Collections.emptyMap();
         }
      };
      ICondition.IContext TAGS_INVALID = new ICondition.IContext() {
         @Override
         public <T> Map<ResourceLocation, Collection<Holder<T>>> getAllTags(ResourceKey<? extends Registry<T>> registry) {
            throw new UnsupportedOperationException("Usage of tag-based conditions is not permitted in this context!");
         }
      };

      default <T> Collection<Holder<T>> getTag(TagKey<T> key) {
         return this.getAllTags(key.registry()).getOrDefault(key.location(), Set.of());
      }

      <T> Map<ResourceLocation, Collection<Holder<T>>> getAllTags(ResourceKey<? extends Registry<T>> var1);
   }
}
