package net.minecraft.network.protocol.login.custom;

import java.util.function.Consumer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public record DiscardedQueryPayload(ResourceLocation id, @Nullable FriendlyByteBuf data, Consumer<FriendlyByteBuf> encoder) implements CustomQueryPayload {
   public DiscardedQueryPayload(ResourceLocation id) {
      this(id, null, buf -> {
      });
   }

   @Override
   public void write(FriendlyByteBuf p_299949_) {
      if (this.data != null) {
         p_299949_.writeBytes(this.data.slice());
      } else {
         this.encoder.accept(p_299949_);
      }
   }
}
