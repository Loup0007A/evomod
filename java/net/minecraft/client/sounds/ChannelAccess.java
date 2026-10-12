package net.minecraft.client.sounds;

import com.google.common.collect.Sets;
import com.mojang.blaze3d.audio.Channel;
import com.mojang.blaze3d.audio.Library;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ChannelAccess {
   private final Set<ChannelAccess.ChannelHandle> channels = Sets.newIdentityHashSet();
   final Library library;
   final Executor executor;

   public ChannelAccess(Library pLibrary, Executor pExecutor) {
      this.library = pLibrary;
      this.executor = pExecutor;
   }

   public CompletableFuture<ChannelAccess.ChannelHandle> createHandle(Library.Pool pSystemMode) {
      CompletableFuture<ChannelAccess.ChannelHandle> $$1 = new CompletableFuture<>();
      this.executor.execute(() -> {
         Channel $$2 = this.library.acquireChannel(pSystemMode);
         if ($$2 != null) {
            ChannelAccess.ChannelHandle $$3 = new ChannelAccess.ChannelHandle($$2);
            this.channels.add($$3);
            $$1.complete($$3);
         } else {
            $$1.complete(null);
         }
      });
      return $$1;
   }

   public void executeOnChannels(Consumer<Stream<Channel>> pSourceStreamConsumer) {
      this.executor.execute(() -> pSourceStreamConsumer.accept(this.channels.stream().map(p_174978_ -> p_174978_.channel).filter(Objects::nonNull)));
   }

   public void scheduleTick() {
      this.executor.execute(() -> {
         Iterator<ChannelAccess.ChannelHandle> $$0 = this.channels.iterator();

         while ($$0.hasNext()) {
            ChannelAccess.ChannelHandle $$1 = $$0.next();
            $$1.channel.updateStream();
            if ($$1.channel.stopped()) {
               $$1.release();
               $$0.remove();
            }
         }
      });
   }

   public void clear() {
      this.channels.forEach(ChannelAccess.ChannelHandle::release);
      this.channels.clear();
   }

   @OnlyIn(Dist.CLIENT)
   public class ChannelHandle {
      @Nullable
      Channel channel;
      private boolean stopped;

      public boolean isStopped() {
         return this.stopped;
      }

      public ChannelHandle(final Channel pChannel) {
         this.channel = pChannel;
      }

      public void execute(Consumer<Channel> pSoundConsumer) {
         ChannelAccess.this.executor.execute(() -> {
            if (this.channel != null) {
               pSoundConsumer.accept(this.channel);
            }
         });
      }

      public void release() {
         this.stopped = true;
         ChannelAccess.this.library.releaseChannel(this.channel);
         this.channel = null;
      }
   }
}
