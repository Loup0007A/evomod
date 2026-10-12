package net.minecraft.client;

import com.google.common.collect.ImmutableMap;
import com.google.common.math.LongMath;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2BooleanFunction;
import java.io.Reader;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class PeriodicNotificationManager
   extends SimplePreparableReloadListener<Map<String, List<PeriodicNotificationManager.Notification>>>
   implements AutoCloseable {
   private static final Codec<Map<String, List<PeriodicNotificationManager.Notification>>> CODEC = Codec.unboundedMap(
      Codec.STRING,
      RecordCodecBuilder.create(
            p_205303_ -> p_205303_.group(
                     Codec.LONG.optionalFieldOf("delay", 0L).forGetter(PeriodicNotificationManager.Notification::delay),
                     Codec.LONG.fieldOf("period").forGetter(PeriodicNotificationManager.Notification::period),
                     Codec.STRING.fieldOf("title").forGetter(PeriodicNotificationManager.Notification::title),
                     Codec.STRING.fieldOf("message").forGetter(PeriodicNotificationManager.Notification::message)
                  )
                  .apply(p_205303_, PeriodicNotificationManager.Notification::new)
         )
         .listOf()
   );
   private static final Logger LOGGER = LogUtils.getLogger();
   private final ResourceLocation notifications;
   private final Object2BooleanFunction<String> selector;
   @Nullable
   private Timer timer;
   @Nullable
   private PeriodicNotificationManager.NotificationTask notificationTask;

   public PeriodicNotificationManager(ResourceLocation pNotifications, Object2BooleanFunction<String> pSelector) {
      this.notifications = pNotifications;
      this.selector = pSelector;
   }

   protected Map<String, List<PeriodicNotificationManager.Notification>> prepare(ResourceManager pResourceManager, ProfilerFiller pProfiler) {
      try {
         Map var4;
         try (Reader $$2 = pResourceManager.openAsReader(this.notifications)) {
            var4 = (Map)CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader($$2)).result().orElseThrow();
         }

         return var4;
      } catch (Exception var8) {
         LOGGER.warn("Failed to load {}", this.notifications, var8);
         return ImmutableMap.of();
      }
   }

   protected void apply(Map<String, List<PeriodicNotificationManager.Notification>> pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
      List<PeriodicNotificationManager.Notification> $$3 = pObject.entrySet()
         .stream()
         .filter(p_205316_ -> (Boolean)this.selector.apply(p_205316_.getKey()))
         .map(Entry::getValue)
         .flatMap(Collection::stream)
         .collect(Collectors.toList());
      if ($$3.isEmpty()) {
         this.stopTimer();
      } else if ($$3.stream().anyMatch(p_205326_ -> p_205326_.period == 0L)) {
         Util.logAndPauseIfInIde("A periodic notification in " + this.notifications + " has a period of zero minutes");
         this.stopTimer();
      } else {
         long $$4 = this.calculateInitialDelay($$3);
         long $$5 = this.calculateOptimalPeriod($$3, $$4);
         if (this.timer == null) {
            this.timer = new Timer();
         }

         if (this.notificationTask == null) {
            this.notificationTask = new PeriodicNotificationManager.NotificationTask($$3, $$4, $$5);
         } else {
            this.notificationTask = this.notificationTask.reset($$3, $$5);
         }

         this.timer.scheduleAtFixedRate(this.notificationTask, TimeUnit.MINUTES.toMillis($$4), TimeUnit.MINUTES.toMillis($$5));
      }
   }

   @Override
   public void close() {
      this.stopTimer();
   }

   private void stopTimer() {
      if (this.timer != null) {
         this.timer.cancel();
      }
   }

   private long calculateOptimalPeriod(List<PeriodicNotificationManager.Notification> pNotifications, long pDelay) {
      return pNotifications.stream().mapToLong(p_205298_ -> {
         long $$2 = p_205298_.delay - pDelay;
         return LongMath.gcd($$2, p_205298_.period);
      }).reduce(LongMath::gcd).orElseThrow(() -> new IllegalStateException("Empty notifications from: " + this.notifications));
   }

   private long calculateInitialDelay(List<PeriodicNotificationManager.Notification> pNotifications) {
      return pNotifications.stream().mapToLong(p_205305_ -> p_205305_.delay).min().orElse(0L);
   }

   @OnlyIn(Dist.CLIENT)
   public static record Notification(long delay, long period, String title, String message) {
      public Notification(final long delay, final long period, final String title, final String message) {
         this.delay = delay != 0L ? delay : period;
         this.period = period;
         this.title = title;
         this.message = message;
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class NotificationTask extends TimerTask {
      private final Minecraft minecraft = Minecraft.getInstance();
      private final List<PeriodicNotificationManager.Notification> notifications;
      private final long period;
      private final AtomicLong elapsed;

      public NotificationTask(List<PeriodicNotificationManager.Notification> pNotifications, long pElapsed, long pPeriod) {
         this.notifications = pNotifications;
         this.period = pPeriod;
         this.elapsed = new AtomicLong(pElapsed);
      }

      public PeriodicNotificationManager.NotificationTask reset(List<PeriodicNotificationManager.Notification> pNotifications, long pPeriod) {
         this.cancel();
         return new PeriodicNotificationManager.NotificationTask(pNotifications, this.elapsed.get(), pPeriod);
      }

      @Override
      public void run() {
         long $$0 = this.elapsed.getAndAdd(this.period);
         long $$1 = this.elapsed.get();

         for (PeriodicNotificationManager.Notification $$2 : this.notifications) {
            if ($$0 >= $$2.delay) {
               long $$3 = $$0 / $$2.period;
               long $$4 = $$1 / $$2.period;
               if ($$3 != $$4) {
                  this.minecraft
                     .execute(
                        () -> SystemToast.add(
                              Minecraft.getInstance().getToasts(),
                              SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                              Component.translatable($$2.title, $$3),
                              Component.translatable($$2.message, $$3)
                           )
                     );
                  return;
               }
            }
         }
      }
   }
}
