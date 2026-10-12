package net.minecraftforge.common.crafting.conditions;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.DataResult.Error;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.resources.DelegatingOps;

public class ConditionCodec {
   public static <T> Codec<T> checkingDecode(Codec<T> normal, Supplier<T> _default) {
      return checkingDecode(normal, _default, "forge:condition");
   }

   public static <T> Codec<T> checkingDecode(Codec<T> normal, Supplier<T> _default, String key) {
      return Codec.of(normal, new ConditionCodec.UnwrapDecoder<>(wrap(normal, key), _default));
   }

   public static <T> Decoder<Optional<T>> wrap(Decoder<T> normal) {
      return wrap(normal, "forge:condition");
   }

   public static <T> Decoder<Optional<T>> wrap(Decoder<T> normal, String key) {
      return new ConditionCodec.OptionalConditionalDecoder<>(normal, key);
   }

   public static <T> ICondition.IContext getContext(DynamicOps<T> ops) {
      ICondition.IContext ret = null;
      if (ops instanceof DelegatingOps<?> dele) {
         ret = dele.getContext(ICondition.IContext.KEY);
      }

      return ret == null ? ICondition.IContext.EMPTY : ret;
   }

   private static record OptionalConditionalDecoder<A>(Decoder<A> normal, String key) implements Decoder<Optional<A>> {
      public <T> DataResult<Pair<Optional<A>, T>> decode(DynamicOps<T> ops, T input) {
         DataResult<T> conditionRaw = ops.get(input, this.key);
         if (!conditionRaw.error().isPresent() && conditionRaw.result().isPresent()) {
            DataResult<ICondition> conditionDecoded = ICondition.CODEC.parse(ops, conditionRaw.result().get());
            if (conditionDecoded.error().isPresent()) {
               return DataResult.error(() -> ((Error)conditionDecoded.error().get()).message());
            } else {
               ICondition condition = (ICondition)conditionDecoded.result().get();
               return !condition.test(ConditionCodec.getContext(ops), ops)
                  ? DataResult.success(Pair.of(Optional.empty(), ops.empty()))
                  : this.normal.decode(ops, input).map(p -> p.mapFirst(Optional::of));
            }
         } else {
            return this.normal.decode(ops, input).map(p -> p.mapFirst(Optional::of));
         }
      }
   }

   private static record UnwrapDecoder<A>(Decoder<Optional<A>> normal, Supplier<A> _default) implements Decoder<A> {
      public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
         DataResult<Pair<Optional<A>, T>> ret = this.normal.decode(ops, input);
         return ret.result().isPresent() && ((Optional)((Pair)ret.result().get()).getFirst()).isPresent()
            ? ret.map(p -> p.mapFirst(Optional::get))
            : ret.map(p -> p.mapFirst(e -> this._default.get()));
      }
   }
}
