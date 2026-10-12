package net.minecraft.tags;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistrySynchronization;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.RegistryLayer;

public class TagNetworkSerialization {
   public static Map<ResourceKey<? extends Registry<?>>, TagNetworkSerialization.NetworkPayload> serializeTagsToNetwork(
      LayeredRegistryAccess<RegistryLayer> pRegistryAccess
   ) {
      return RegistrySynchronization.networkSafeRegistries(pRegistryAccess)
         .map(p_203949_ -> Pair.of(p_203949_.key(), serializeToNetwork(p_203949_.value())))
         .filter(p_326489_ -> ((TagNetworkSerialization.NetworkPayload)p_326489_.getSecond()).size() > 0)
         .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));
   }

   private static <T> TagNetworkSerialization.NetworkPayload serializeToNetwork(Registry<T> pRegistry) {
      Map<ResourceLocation, IntList> $$1 = new HashMap<>();
      pRegistry.getTags().forEach(p_326488_ -> {
         HolderSet<T> $$3 = (HolderSet<T>)p_326488_.getSecond();
         IntList $$4 = new IntArrayList($$3.size());

         for (Holder<T> $$5 : $$3) {
            if ($$5.kind() != Holder.Kind.REFERENCE) {
               throw new IllegalStateException("Can't serialize unregistered value " + $$5);
            }

            $$4.add(pRegistry.getId($$5.value()));
         }

         $$1.put(((TagKey)p_326488_.getFirst()).location(), $$4);
      });
      return new TagNetworkSerialization.NetworkPayload($$1);
   }

   static <T> void deserializeTagsFromNetwork(
      ResourceKey<? extends Registry<T>> pRegistryKey,
      Registry<T> pRegistry,
      TagNetworkSerialization.NetworkPayload pNetworkPayload,
      TagNetworkSerialization.TagOutput<T> pOutput
   ) {
      pNetworkPayload.tags.forEach((p_248278_, p_248279_) -> {
         TagKey<T> $$5 = TagKey.create(pRegistryKey, p_248278_);
         List<Holder<T>> $$6 = p_248279_.intStream().mapToObj(pRegistry::getHolder).flatMap(Optional::stream).collect(Collectors.toUnmodifiableList());
         pOutput.accept($$5, $$6);
      });
   }

   public static final class NetworkPayload {
      final Map<ResourceLocation, IntList> tags;

      NetworkPayload(Map<ResourceLocation, IntList> pTags) {
         this.tags = pTags;
      }

      public void write(FriendlyByteBuf pBuffer) {
         pBuffer.writeMap(this.tags, FriendlyByteBuf::writeResourceLocation, FriendlyByteBuf::writeIntIdList);
      }

      public static TagNetworkSerialization.NetworkPayload read(FriendlyByteBuf pBuffer) {
         return new TagNetworkSerialization.NetworkPayload(pBuffer.readMap(FriendlyByteBuf::readResourceLocation, FriendlyByteBuf::readIntIdList));
      }

      public int size() {
         return this.tags.size();
      }

      public <T> void applyToRegistry(Registry<T> pRegistry) {
         if (this.size() != 0) {
            Map<TagKey<T>, List<Holder<T>>> $$1 = new HashMap<>(this.size());
            TagNetworkSerialization.deserializeTagsFromNetwork(pRegistry.key(), pRegistry, this, $$1::put);
            pRegistry.bindTags($$1);
         }
      }
   }

   @FunctionalInterface
   public interface TagOutput<T> {
      void accept(TagKey<T> pKey, List<Holder<T>> pValues);
   }
}
