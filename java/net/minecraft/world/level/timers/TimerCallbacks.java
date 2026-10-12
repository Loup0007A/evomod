package net.minecraft.world.level.timers;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

public class TimerCallbacks<C> {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final TimerCallbacks<MinecraftServer> SERVER_CALLBACKS = new TimerCallbacks<MinecraftServer>()
      .register(new FunctionCallback.Serializer())
      .register(new FunctionTagCallback.Serializer());
   private final Map<ResourceLocation, TimerCallback.Serializer<C, ?>> idToSerializer = Maps.newHashMap();
   private final Map<Class<?>, TimerCallback.Serializer<C, ?>> classToSerializer = Maps.newHashMap();

   public TimerCallbacks<C> register(TimerCallback.Serializer<C, ?> pSerializer) {
      this.idToSerializer.put(pSerializer.getId(), pSerializer);
      this.classToSerializer.put(pSerializer.getCls(), pSerializer);
      return this;
   }

   private <T extends TimerCallback<C>> TimerCallback.Serializer<C, T> getSerializer(Class<?> pClazz) {
      return (TimerCallback.Serializer<C, T>)this.classToSerializer.get(pClazz);
   }

   public <T extends TimerCallback<C>> CompoundTag serialize(T pCallback) {
      TimerCallback.Serializer<C, T> $$1 = this.getSerializer(pCallback.getClass());
      CompoundTag $$2 = new CompoundTag();
      $$1.serialize($$2, pCallback);
      $$2.putString("Type", $$1.getId().toString());
      return $$2;
   }

   @Nullable
   public TimerCallback<C> deserialize(CompoundTag pTag) {
      ResourceLocation $$1 = ResourceLocation.tryParse(pTag.getString("Type"));
      TimerCallback.Serializer<C, ?> $$2 = this.idToSerializer.get($$1);
      if ($$2 == null) {
         LOGGER.error("Failed to deserialize timer callback: {}", pTag);
         return null;
      } else {
         try {
            return $$2.deserialize(pTag);
         } catch (Exception var5) {
            LOGGER.error("Failed to deserialize timer callback: {}", pTag, var5);
            return null;
         }
      }
   }
}
