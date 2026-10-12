package net.minecraft.network.chat;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapDecoder;
import com.mojang.serialization.MapEncoder;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.contents.KeybindContents;
import net.minecraft.network.chat.contents.NbtContents;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.ScoreContents;
import net.minecraft.network.chat.contents.SelectorContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.StringRepresentable;

public class ComponentSerialization {
   public static final Codec<Component> CODEC = Codec.recursive("Component", ComponentSerialization::createCodec);
   public static final StreamCodec<RegistryFriendlyByteBuf, Component> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(CODEC);
   public static final StreamCodec<RegistryFriendlyByteBuf, Optional<Component>> OPTIONAL_STREAM_CODEC = STREAM_CODEC.apply(ByteBufCodecs::optional);
   public static final StreamCodec<RegistryFriendlyByteBuf, Component> TRUSTED_STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistriesTrusted(CODEC);
   public static final StreamCodec<RegistryFriendlyByteBuf, Optional<Component>> TRUSTED_OPTIONAL_STREAM_CODEC = TRUSTED_STREAM_CODEC.apply(
      ByteBufCodecs::optional
   );
   public static final StreamCodec<ByteBuf, Component> TRUSTED_CONTEXT_FREE_STREAM_CODEC = ByteBufCodecs.fromCodecTrusted(CODEC);
   public static final Codec<Component> FLAT_CODEC = flatCodec(Integer.MAX_VALUE);

   public static Codec<Component> flatCodec(int pMaxSize) {
      final Codec<String> $$1 = Codec.string(0, pMaxSize);
      return new Codec<Component>() {
         public <T> DataResult<Pair<Component, T>> decode(DynamicOps<T> p_334494_, T p_334478_) {
            DynamicOps<JsonElement> $$2 = asJsonOps(p_334494_);
            return $$1.decode(p_334494_, p_334478_).flatMap(p_328894_ -> {
               try {
                  JsonElement $$2x = JsonParser.parseString((String)p_328894_.getFirst());
                  return ComponentSerialization.CODEC.parse($$2, $$2x).map(p_334008_ -> Pair.of(p_334008_, p_328894_.getSecond()));
               } catch (JsonParseException var3x) {
                  return DataResult.error(var3x::getMessage);
               }
            });
         }

         public <T> DataResult<T> encode(Component p_330654_, DynamicOps<T> p_330879_, T p_336296_) {
            DynamicOps<JsonElement> $$3 = asJsonOps(p_330879_);
            return ComponentSerialization.CODEC.encodeStart($$3, p_330654_).flatMap(p_332275_ -> {
               try {
                  return $$1.encodeStart(p_330879_, GsonHelper.toStableString(p_332275_));
               } catch (IllegalArgumentException var4x) {
                  return DataResult.error(var4x::getMessage);
               }
            });
         }

         private static <T> DynamicOps<JsonElement> asJsonOps(DynamicOps<T> p_331374_) {
            return (DynamicOps<JsonElement>)(p_331374_ instanceof RegistryOps<T> $$1 ? $$1.withParent(JsonOps.INSTANCE) : JsonOps.INSTANCE);
         }
      };
   }

   private static MutableComponent createFromList(List<Component> p_312708_) {
      MutableComponent $$1 = p_312708_.get(0).copy();

      for (int $$2 = 1; $$2 < p_312708_.size(); $$2++) {
         $$1.append(p_312708_.get($$2));
      }

      return $$1;
   }

   public static <T extends StringRepresentable, E> MapCodec<E> createLegacyComponentMatcher(
      T[] pTypes, Function<T, MapCodec<? extends E>> pCodecGetter, Function<E, T> pTypeGetter, String pTypeFieldName
   ) {
      MapCodec<E> $$4 = new ComponentSerialization.FuzzyCodec<>(
         Stream.<T>of(pTypes).map(pCodecGetter).toList(), p_312251_ -> (MapEncoder<? extends E>)pCodecGetter.apply(pTypeGetter.apply(p_312251_))
      );
      Codec<T> $$5 = StringRepresentable.fromValues((Supplier<T[]>)(() -> pTypes));
      MapCodec<E> $$6 = $$5.dispatchMap(pTypeFieldName, pTypeGetter, pCodecGetter);
      MapCodec<E> $$7 = new ComponentSerialization.StrictEither(pTypeFieldName, $$6, $$4);
      return ExtraCodecs.orCompressed($$7, $$6);
   }

   private static Codec<Component> createCodec(Codec<Component> p_310353_) {
      ComponentContents.Type<?>[] $$1 = new ComponentContents.Type[]{
         PlainTextContents.TYPE, TranslatableContents.TYPE, KeybindContents.TYPE, ScoreContents.TYPE, SelectorContents.TYPE, NbtContents.TYPE
      };
      MapCodec<ComponentContents> $$2 = createLegacyComponentMatcher($$1, ComponentContents.Type::codec, ComponentContents::type, "type");
      Codec<Component> $$3 = RecordCodecBuilder.create(
         p_326064_ -> p_326064_.group(
                  $$2.forGetter(Component::getContents),
                  ExtraCodecs.nonEmptyList(p_310353_.listOf()).optionalFieldOf("extra", List.of()).forGetter(Component::getSiblings),
                  Style.Serializer.MAP_CODEC.forGetter(Component::getStyle)
               )
               .apply(p_326064_, MutableComponent::new)
      );
      return Codec.either(Codec.either(Codec.STRING, ExtraCodecs.nonEmptyList(p_310353_.listOf())), $$3)
         .xmap(
            p_312362_ -> (Component)p_312362_.map(
                  p_310114_ -> (Component)p_310114_.map(Component::literal, ComponentSerialization::createFromList), p_310523_ -> p_310523_
               ),
            p_312558_ -> {
               String $$1x = p_312558_.tryCollapseToString();
               return $$1x != null ? Either.left(Either.left($$1x)) : Either.right(p_312558_);
            }
         );
   }

   static class FuzzyCodec<T> extends MapCodec<T> {
      private final List<MapCodec<? extends T>> codecs;
      private final Function<T, MapEncoder<? extends T>> encoderGetter;

      public FuzzyCodec(List<MapCodec<? extends T>> pCodecs, Function<T, MapEncoder<? extends T>> pEncoderGetter) {
         this.codecs = pCodecs;
         this.encoderGetter = pEncoderGetter;
      }

      public <S> DataResult<T> decode(DynamicOps<S> pOps, MapLike<S> pInput) {
         for (MapDecoder<? extends T> $$2 : this.codecs) {
            DataResult<? extends T> $$3 = $$2.decode(pOps, pInput);
            if ($$3.result().isPresent()) {
               return (DataResult<T>)$$3;
            }
         }

         return DataResult.error(() -> "No matching codec found");
      }

      public <S> RecordBuilder<S> encode(T pInput, DynamicOps<S> pOps, RecordBuilder<S> pPrefix) {
         MapEncoder<T> $$3 = (MapEncoder<T>)this.encoderGetter.apply(pInput);
         return $$3.encode(pInput, pOps, pPrefix);
      }

      public <S> Stream<S> keys(DynamicOps<S> pOps) {
         return this.codecs.stream().flatMap(p_310919_ -> p_310919_.keys(pOps)).distinct();
      }

      public String toString() {
         return "FuzzyCodec[" + this.codecs + "]";
      }
   }

   static class StrictEither<T> extends MapCodec<T> {
      private final String typeFieldName;
      private final MapCodec<T> typed;
      private final MapCodec<T> fuzzy;

      public StrictEither(String pTypeFieldName, MapCodec<T> pTyped, MapCodec<T> pFuzzy) {
         this.typeFieldName = pTypeFieldName;
         this.typed = pTyped;
         this.fuzzy = pFuzzy;
      }

      public <O> DataResult<T> decode(DynamicOps<O> pOps, MapLike<O> pInput) {
         return pInput.get(this.typeFieldName) != null ? this.typed.decode(pOps, pInput) : this.fuzzy.decode(pOps, pInput);
      }

      public <O> RecordBuilder<O> encode(T pInput, DynamicOps<O> pOps, RecordBuilder<O> pPrefix) {
         return this.fuzzy.encode(pInput, pOps, pPrefix);
      }

      public <T1> Stream<T1> keys(DynamicOps<T1> pOps) {
         return Stream.concat(this.typed.keys(pOps), this.fuzzy.keys(pOps)).distinct();
      }
   }
}
