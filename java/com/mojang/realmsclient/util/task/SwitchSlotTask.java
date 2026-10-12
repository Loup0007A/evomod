package com.mojang.realmsclient.util.task;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.exception.RetryCallException;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class SwitchSlotTask extends LongRunningTask {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Component TITLE = Component.translatable("mco.minigame.world.slot.screen.title");
   private final long realmId;
   private final int slot;
   private final Runnable callback;

   public SwitchSlotTask(long pWorldId, int pSlot, Runnable pCallback) {
      this.realmId = pWorldId;
      this.slot = pSlot;
      this.callback = pCallback;
   }

   @Override
   public void run() {
      RealmsClient $$0 = RealmsClient.create();

      for (int $$1 = 0; $$1 < 25; $$1++) {
         try {
            if (this.aborted()) {
               return;
            }

            if ($$0.switchSlot(this.realmId, this.slot)) {
               this.callback.run();
               break;
            }
         } catch (RetryCallException var4) {
            if (this.aborted()) {
               return;
            }

            pause((long)var4.delaySeconds);
         } catch (Exception var5) {
            if (this.aborted()) {
               return;
            }

            LOGGER.error("Couldn't switch world!");
            this.error(var5);
         }
      }
   }

   @Override
   public Component getTitle() {
      return TITLE;
   }
}
