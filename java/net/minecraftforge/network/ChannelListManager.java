package net.minecraftforge.network;

import com.mojang.logging.LogUtils;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import net.minecraft.ResourceLocationException;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.network.ChannelRegistrationChangeEvent;
import net.minecraftforge.event.network.CustomPayloadEvent;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.slf4j.Logger;

@Internal
public class ChannelListManager {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final ResourceLocation NAME = ResourceLocation.fromNamespaceAndPath("forge", "channel_registration");
   static final Channel<CustomPacketPayload> CHANNEL = ChannelBuilder.named(NAME)
      .optional()
      .payloadChannel()
      .any()
      .bidirectional()
      .add(ChannelListManager.Register.TYPE, ChannelListManager.Register.CODEC, ChannelListManager::register)
      .add(ChannelListManager.Unregister.TYPE, ChannelListManager.Unregister.CODEC, ChannelListManager::unregister)
      .build();

   private static void register(ChannelListManager.Register payload, CustomPayloadEvent.Context ctx) {
      updateFrom(ctx, payload.channels(), ChannelRegistrationChangeEvent.Type.REGISTER);
      ctx.setPacketHandled(true);
      if (ctx.isClientSide()) {
         addChannels(ctx.getConnection());
      }
   }

   private static void unregister(ChannelListManager.Unregister payload, CustomPayloadEvent.Context ctx) {
      updateFrom(ctx, payload.channels(), ChannelRegistrationChangeEvent.Type.UNREGISTER);
      ctx.setPacketHandled(true);
   }

   public static void addChannels(Connection connection) {
      addChannels(connection, NetworkRegistry.buildRegisterList());
   }

   public static void addChannels(Connection connection, ResourceLocation... channels) {
      addChannels(connection, Arrays.asList(channels));
   }

   public static void addChannels(Connection connection, Collection<ResourceLocation> channels) {
      NetworkContext list = NetworkContext.get(connection);
      HashSet<String> toSend = new HashSet<>();

      for (ResourceLocation channel : channels) {
         if (list.sentChannels.add(channel)) {
            toSend.add(channel.toString());
         }
      }

      if (!toSend.isEmpty()) {
         CHANNEL.send(new ChannelListManager.Register(toSend.stream().sorted().toList()), connection);
      }
   }

   public static void removeChannels(Connection connection, ResourceLocation... channels) {
      removeChannels(connection, Arrays.asList(channels));
   }

   public static void removeChannels(Connection connection, Collection<ResourceLocation> channels) {
      NetworkContext list = NetworkContext.get(connection);
      HashSet<String> toSend = new HashSet<>();

      for (ResourceLocation channel : channels) {
         if (list.sentChannels.remove(channel)) {
            toSend.add(channel.toString());
         }
      }

      if (!toSend.isEmpty()) {
         CHANNEL.send(new ChannelListManager.Unregister(toSend.stream().toList()), connection);
      }
   }

   private static void encode(FriendlyByteBuf buf, List<String> channels) {
      for (String c : channels) {
         buf.writeBytes(c.toString().getBytes(StandardCharsets.UTF_8));
         buf.writeByte(0);
      }
   }

   private static List<String> decode(FriendlyByteBuf buffer) {
      byte[] data = new byte[Math.max(buffer.readableBytes(), 0)];
      buffer.readBytes(data);
      ArrayList<String> channels = new ArrayList<>();
      int last = 0;

      for (int cur = 0; cur < data.length; cur++) {
         if (data[cur] == 0) {
            channels.add(new String(data, last, cur - last, StandardCharsets.UTF_8));
            last = cur + 1;
         }
      }

      if (last < data.length) {
         channels.add(new String(data, last, data.length - last, StandardCharsets.UTF_8));
      }

      return Collections.unmodifiableList(channels);
   }

   private static void updateFrom(CustomPayloadEvent.Context source, List<String> channels, ChannelRegistrationChangeEvent.Type changeType) {
      HashSet<ResourceLocation> changed = new HashSet<>();

      for (String channel : channels) {
         if (!channel.isEmpty()) {
            try {
               changed.add(ResourceLocation.parse(channel));
            } catch (ResourceLocationException var9) {
               LOGGER.warn("Invalid channel name received: {}. Ignoring", channel);
            }
         }
      }

      ForgeEventFactory.onChannelRegistrationChange(source.getConnection(), changeType, changed);
      NetworkContext list = NetworkContext.get(source.getConnection());

      for (ResourceLocation channelx : changed) {
         boolean fire = changeType == ChannelRegistrationChangeEvent.Type.UNREGISTER ? list.remoteChannels.remove(channelx) : list.remoteChannels.add(channelx);
         if (fire) {
            NetworkInstance target = NetworkRegistry.findTarget(channelx);
            if (target != null) {
               target.registrationChange(channelx, changeType == ChannelRegistrationChangeEvent.Type.REGISTER);
            }
         }
      }
   }

   private static record Register(List<String> channels) implements CustomPacketPayload {
      private static final CustomPacketPayload.Type<ChannelListManager.Register> TYPE = CustomPacketPayload.createType("register");
      private static final StreamCodec<FriendlyByteBuf, ChannelListManager.Register> CODEC = StreamCodec.of(
         (buf, v) -> ChannelListManager.encode(buf, v.channels), buf -> new ChannelListManager.Register(ChannelListManager.decode(buf))
      );

      @Override
      public CustomPacketPayload.Type<ChannelListManager.Register> type() {
         return TYPE;
      }
   }

   private static record Unregister(List<String> channels) implements CustomPacketPayload {
      private static final CustomPacketPayload.Type<ChannelListManager.Unregister> TYPE = CustomPacketPayload.createType("unregister");
      private static final StreamCodec<FriendlyByteBuf, ChannelListManager.Unregister> CODEC = StreamCodec.of(
         (buf, v) -> ChannelListManager.encode(buf, v.channels), buf -> new ChannelListManager.Unregister(ChannelListManager.decode(buf))
      );

      @Override
      public CustomPacketPayload.Type<ChannelListManager.Unregister> type() {
         return TYPE;
      }
   }
}
