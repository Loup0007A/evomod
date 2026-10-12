package net.minecraftforge.common;

import net.minecraft.server.TickTask;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.loot.LootModifierManager;
import net.minecraftforge.common.util.LogicalSidedProvider;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.network.ConnectionStartEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.filters.NetworkFilters;
import net.minecraftforge.server.command.ConfigCommand;
import net.minecraftforge.server.command.ForgeCommand;

public class ForgeInternalHandler {
   private static LootModifierManager INSTANCE;

   @SubscribeEvent(
      priority = EventPriority.HIGH
   )
   public void onEntityJoinWorld(EntityJoinLevelEvent event) {
      Entity entity = event.getEntity();
      if (entity.getClass().equals(ItemEntity.class)) {
         ItemStack stack = ((ItemEntity)entity).getItem();
         Item item = stack.getItem();
         if (item.hasCustomEntity(stack)) {
            Entity newEntity = item.createEntity(event.getLevel(), entity, stack);
            if (newEntity != null) {
               entity.discard();
               event.setCanceled(true);
               BlockableEventLoop<? super TickTask> executor = LogicalSidedProvider.WORKQUEUE
                  .get(event.getLevel().isClientSide ? LogicalSide.CLIENT : LogicalSide.SERVER);
               executor.tell(new TickTask(0, () -> event.getLevel().addFreshEntity(newEntity)));
            }
         }
      }
   }

   @SubscribeEvent
   public void onServerTick(TickEvent.ServerTickEvent.Pre event) {
      WorldWorkerManager.tick(true);
   }

   @SubscribeEvent
   public void onServerTick(TickEvent.ServerTickEvent.Post event) {
      WorldWorkerManager.tick(false);
   }

   @SubscribeEvent
   public void onChunkUnload(ChunkEvent.Unload event) {
      if (!event.getLevel().isClientSide()) {
         FarmlandWaterManager.removeTickets(event.getChunk());
      }
   }

   @SubscribeEvent
   public void playerLogin(PlayerEvent.PlayerLoggedInEvent event) {
      UsernameCache.setUsername(event.getEntity().getUUID(), event.getEntity().getGameProfile().getName());
   }

   @SubscribeEvent
   public void tagsUpdated(TagsUpdatedEvent event) {
      if (event.shouldUpdateStaticData()) {
         ForgeHooks.updateBurns();
      }
   }

   @SubscribeEvent
   public void onCommandsRegister(RegisterCommandsEvent event) {
      new ForgeCommand(event.getDispatcher());
      ConfigCommand.register(event.getDispatcher());
   }

   @SubscribeEvent
   public void onResourceReload(AddReloadListenerEvent event) {
      INSTANCE = new LootModifierManager(event.getRegistryAccess());
      event.addListener(INSTANCE);
   }

   static LootModifierManager getLootModifierManager() {
      if (INSTANCE == null) {
         throw new IllegalStateException("Can not retrieve LootModifierManager until resources have loaded once.");
      } else {
         return INSTANCE;
      }
   }

   @SubscribeEvent
   public void resourceReloadListeners(AddReloadListenerEvent event) {
      event.addListener(CreativeModeTabRegistry.getReloadListener());
   }

   @SubscribeEvent(
      priority = EventPriority.HIGHEST
   )
   public void builtinMobSpawnBlocker(EntityJoinLevelEvent event) {
      if (event.getEntity() instanceof Mob mob && mob.isSpawnCancelled()) {
         event.setCanceled(true);
      }
   }

   @SubscribeEvent
   public void onConnectionStart(ConnectionStartEvent event) {
      NetworkFilters.injectIfNecessary(event.getConnection());
   }
}
