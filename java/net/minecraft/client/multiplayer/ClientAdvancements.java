package net.minecraft.client.multiplayer;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.AdvancementTree;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.AdvancementToast;
import net.minecraft.client.telemetry.WorldSessionTelemetryManager;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import net.minecraft.network.protocol.game.ServerboundSeenAdvancementsPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ClientAdvancements {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Minecraft minecraft;
   private final WorldSessionTelemetryManager telemetryManager;
   private final AdvancementTree tree = new AdvancementTree();
   private final Map<AdvancementHolder, AdvancementProgress> progress = new Object2ObjectOpenHashMap();
   @Nullable
   private ClientAdvancements.Listener listener;
   @Nullable
   private AdvancementHolder selectedTab;

   public ClientAdvancements(Minecraft pMinecraft, WorldSessionTelemetryManager pTelemetryManager) {
      this.minecraft = pMinecraft;
      this.telemetryManager = pTelemetryManager;
   }

   public void update(ClientboundUpdateAdvancementsPacket pPacket) {
      if (pPacket.shouldReset()) {
         this.tree.clear();
         this.progress.clear();
      }

      this.tree.remove(pPacket.getRemoved());
      this.tree.addAll(pPacket.getAdded());

      for (Entry<ResourceLocation, AdvancementProgress> $$1 : pPacket.getProgress().entrySet()) {
         AdvancementNode $$2 = this.tree.get($$1.getKey());
         if ($$2 != null) {
            AdvancementProgress $$3 = $$1.getValue();
            $$3.update($$2.advancement().requirements());
            this.progress.put($$2.holder(), $$3);
            if (this.listener != null) {
               this.listener.onUpdateAdvancementProgress($$2, $$3);
            }

            if (!pPacket.shouldReset() && $$3.isDone()) {
               if (this.minecraft.level != null) {
                  this.telemetryManager.onAdvancementDone(this.minecraft.level, $$2.holder());
               }

               Optional<DisplayInfo> $$4 = $$2.advancement().display();
               if ($$4.isPresent() && $$4.get().shouldShowToast()) {
                  this.minecraft.getToasts().addToast(new AdvancementToast($$2.holder()));
               }
            }
         } else {
            LOGGER.warn("Server informed client about progress for unknown advancement {}", $$1.getKey());
         }
      }
   }

   public AdvancementTree getTree() {
      return this.tree;
   }

   public void setSelectedTab(@Nullable AdvancementHolder pAdvancement, boolean pTellServer) {
      ClientPacketListener $$2 = this.minecraft.getConnection();
      if ($$2 != null && pAdvancement != null && pTellServer) {
         $$2.send(ServerboundSeenAdvancementsPacket.openedTab(pAdvancement));
      }

      if (this.selectedTab != pAdvancement) {
         this.selectedTab = pAdvancement;
         if (this.listener != null) {
            this.listener.onSelectedTabChanged(pAdvancement);
         }
      }
   }

   public void setListener(@Nullable ClientAdvancements.Listener pListener) {
      this.listener = pListener;
      this.tree.setListener(pListener);
      if (pListener != null) {
         this.progress.forEach((p_297914_, p_300709_) -> {
            AdvancementNode $$3 = this.tree.get(p_297914_);
            if ($$3 != null) {
               pListener.onUpdateAdvancementProgress($$3, p_300709_);
            }
         });
         pListener.onSelectedTabChanged(this.selectedTab);
      }
   }

   @Nullable
   public AdvancementHolder get(ResourceLocation pId) {
      AdvancementNode $$1 = this.tree.get(pId);
      return $$1 != null ? $$1.holder() : null;
   }

   @OnlyIn(Dist.CLIENT)
   public interface Listener extends AdvancementTree.Listener {
      void onUpdateAdvancementProgress(AdvancementNode pAdvancement, AdvancementProgress pAdvancementProgress);

      void onSelectedTabChanged(@Nullable AdvancementHolder pAdvancement);
   }
}
