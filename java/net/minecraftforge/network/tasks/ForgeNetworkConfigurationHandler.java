package net.minecraftforge.network.tasks;

import net.minecraftforge.event.network.GatherLoginConfigurationTasksEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.ConnectionType;
import net.minecraftforge.network.NetworkContext;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public class ForgeNetworkConfigurationHandler {
   @SubscribeEvent
   public void gatherInit(GatherLoginConfigurationTasksEvent event) {
      NetworkContext ctx = NetworkContext.get(event.getConnection());
      if (ctx.getType() == ConnectionType.MODDED) {
         event.addTask(new RegisterChannelsTask());
         event.addTask(new ModVersionsTask());
         event.addTask(new ChannelVersionsTask());
         event.addTask(new SyncRegistriesTask());
         event.addTask(new SyncConfigTask());
      }
   }
}
