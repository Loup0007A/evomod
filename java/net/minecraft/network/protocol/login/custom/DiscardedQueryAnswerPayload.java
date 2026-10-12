package net.minecraft.network.protocol.login.custom;

import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;

public record DiscardedQueryAnswerPayload(@Nullable FriendlyByteBuf data, Consumer<FriendlyByteBuf> encoder) implements CustomQueryAnswerPayload {
   public static final DiscardedQueryAnswerPayload INSTANCE = new DiscardedQueryAnswerPayload();

   public DiscardedQueryAnswerPayload() {
      this(null, buf -> {
      });
   }

   @Override
   public void write(FriendlyByteBuf p_299186_) {
      if (this.data != null) {
         p_299186_.writeBytes(this.data.slice());
      } else {
         this.encoder.accept(p_299186_);
      }
   }
}
