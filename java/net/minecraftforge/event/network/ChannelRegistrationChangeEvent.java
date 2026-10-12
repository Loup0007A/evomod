package net.minecraftforge.event.network;

import java.util.Collections;
import java.util.Set;
import net.minecraft.network.Connection;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.Event;

public class ChannelRegistrationChangeEvent extends Event {
   private final Connection source;
   private final ChannelRegistrationChangeEvent.Type changeType;
   private final Set<ResourceLocation> channels;

   public ChannelRegistrationChangeEvent(Connection source, ChannelRegistrationChangeEvent.Type changeType, Set<ResourceLocation> channels) {
      this.source = source;
      this.changeType = changeType;
      this.channels = Collections.unmodifiableSet(channels);
   }

   public ChannelRegistrationChangeEvent.Type getType() {
      return this.changeType;
   }

   public Connection getSource() {
      return this.source;
   }

   public Set<ResourceLocation> getChannels() {
      return this.channels;
   }

   public static enum Type {
      REGISTER,
      UNREGISTER;
   }
}
