package net.minecraft.client.resources.server;

import com.google.common.hash.HashCode;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.server.packs.DownloadQueue;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ServerPackManager {
   private final PackDownloader downloader;
   final PackLoadFeedback packLoadFeedback;
   private final PackReloadConfig reloadConfig;
   private final Runnable updateRequest;
   private ServerPackManager.PackPromptStatus packPromptStatus;
   final List<ServerPackManager.ServerPackData> packs = new ArrayList<>();

   public ServerPackManager(
      PackDownloader pDownloader,
      PackLoadFeedback pPackLoadFeedback,
      PackReloadConfig pReloadConfig,
      Runnable pUpdateRequest,
      ServerPackManager.PackPromptStatus pPackPromptStatus
   ) {
      this.downloader = pDownloader;
      this.packLoadFeedback = pPackLoadFeedback;
      this.reloadConfig = pReloadConfig;
      this.updateRequest = pUpdateRequest;
      this.packPromptStatus = pPackPromptStatus;
   }

   void registerForUpdate() {
      this.updateRequest.run();
   }

   private void markExistingPacksAsRemoved(UUID pId) {
      for (ServerPackManager.ServerPackData $$1 : this.packs) {
         if ($$1.id.equals(pId)) {
            $$1.setRemovalReasonIfNotSet(ServerPackManager.RemovalReason.SERVER_REPLACED);
         }
      }
   }

   public void pushPack(UUID pId, URL pUrl, @Nullable HashCode pHash) {
      if (this.packPromptStatus == ServerPackManager.PackPromptStatus.DECLINED) {
         this.packLoadFeedback.reportFinalResult(pId, PackLoadFeedback.FinalResult.DECLINED);
      } else {
         this.pushNewPack(pId, new ServerPackManager.ServerPackData(pId, pUrl, pHash));
      }
   }

   public void pushLocalPack(UUID pId, Path pPath) {
      if (this.packPromptStatus == ServerPackManager.PackPromptStatus.DECLINED) {
         this.packLoadFeedback.reportFinalResult(pId, PackLoadFeedback.FinalResult.DECLINED);
      } else {
         URL $$2;
         try {
            $$2 = pPath.toUri().toURL();
         } catch (MalformedURLException var5) {
            throw new IllegalStateException("Can't convert path to URL " + pPath, var5);
         }

         ServerPackManager.ServerPackData $$5 = new ServerPackManager.ServerPackData(pId, $$2, null);
         $$5.downloadStatus = ServerPackManager.PackDownloadStatus.DONE;
         $$5.path = pPath;
         this.pushNewPack(pId, $$5);
      }
   }

   private void pushNewPack(UUID pId, ServerPackManager.ServerPackData pPackData) {
      this.markExistingPacksAsRemoved(pId);
      this.packs.add(pPackData);
      if (this.packPromptStatus == ServerPackManager.PackPromptStatus.ALLOWED) {
         this.acceptPack(pPackData);
      }

      this.registerForUpdate();
   }

   private void acceptPack(ServerPackManager.ServerPackData pPackData) {
      this.packLoadFeedback.reportUpdate(pPackData.id, PackLoadFeedback.Update.ACCEPTED);
      pPackData.promptAccepted = true;
   }

   @Nullable
   private ServerPackManager.ServerPackData findPackInfo(UUID pId) {
      for (ServerPackManager.ServerPackData $$1 : this.packs) {
         if (!$$1.isRemoved() && $$1.id.equals(pId)) {
            return $$1;
         }
      }

      return null;
   }

   public void popPack(UUID pId) {
      ServerPackManager.ServerPackData $$1 = this.findPackInfo(pId);
      if ($$1 != null) {
         $$1.setRemovalReasonIfNotSet(ServerPackManager.RemovalReason.SERVER_REMOVED);
         this.registerForUpdate();
      }
   }

   public void popAll() {
      for (ServerPackManager.ServerPackData $$0 : this.packs) {
         $$0.setRemovalReasonIfNotSet(ServerPackManager.RemovalReason.SERVER_REMOVED);
      }

      this.registerForUpdate();
   }

   public void allowServerPacks() {
      this.packPromptStatus = ServerPackManager.PackPromptStatus.ALLOWED;

      for (ServerPackManager.ServerPackData $$0 : this.packs) {
         if (!$$0.promptAccepted && !$$0.isRemoved()) {
            this.acceptPack($$0);
         }
      }

      this.registerForUpdate();
   }

   public void rejectServerPacks() {
      this.packPromptStatus = ServerPackManager.PackPromptStatus.DECLINED;

      for (ServerPackManager.ServerPackData $$0 : this.packs) {
         if (!$$0.promptAccepted) {
            $$0.setRemovalReasonIfNotSet(ServerPackManager.RemovalReason.DECLINED);
         }
      }

      this.registerForUpdate();
   }

   public void resetPromptStatus() {
      this.packPromptStatus = ServerPackManager.PackPromptStatus.PENDING;
   }

   public void tick() {
      boolean $$0 = this.updateDownloads();
      if (!$$0) {
         this.triggerReloadIfNeeded();
      }

      this.cleanupRemovedPacks();
   }

   private void cleanupRemovedPacks() {
      this.packs.removeIf(p_312551_ -> {
         if (p_312551_.activationStatus != ServerPackManager.ActivationStatus.INACTIVE) {
            return false;
         } else if (p_312551_.removalReason != null) {
            PackLoadFeedback.FinalResult $$1 = p_312551_.removalReason.serverResponse;
            if ($$1 != null) {
               this.packLoadFeedback.reportFinalResult(p_312551_.id, $$1);
            }

            return true;
         } else {
            return false;
         }
      });
   }

   private void onDownload(Collection<ServerPackManager.ServerPackData> pPacks, DownloadQueue.BatchResult pBatchResult) {
      if (!pBatchResult.failed().isEmpty()) {
         for (ServerPackManager.ServerPackData $$2 : this.packs) {
            if ($$2.activationStatus != ServerPackManager.ActivationStatus.ACTIVE) {
               if (pBatchResult.failed().contains($$2.id)) {
                  $$2.setRemovalReasonIfNotSet(ServerPackManager.RemovalReason.DOWNLOAD_FAILED);
               } else {
                  $$2.setRemovalReasonIfNotSet(ServerPackManager.RemovalReason.DISCARDED);
               }
            }
         }
      }

      for (ServerPackManager.ServerPackData $$3 : pPacks) {
         Path $$4 = pBatchResult.downloaded().get($$3.id);
         if ($$4 != null) {
            $$3.downloadStatus = ServerPackManager.PackDownloadStatus.DONE;
            $$3.path = $$4;
            if (!$$3.isRemoved()) {
               this.packLoadFeedback.reportUpdate($$3.id, PackLoadFeedback.Update.DOWNLOADED);
            }
         }
      }

      this.registerForUpdate();
   }

   private boolean updateDownloads() {
      List<ServerPackManager.ServerPackData> $$0 = new ArrayList<>();
      boolean $$1 = false;

      for (ServerPackManager.ServerPackData $$2 : this.packs) {
         if (!$$2.isRemoved() && $$2.promptAccepted) {
            if ($$2.downloadStatus != ServerPackManager.PackDownloadStatus.DONE) {
               $$1 = true;
            }

            if ($$2.downloadStatus == ServerPackManager.PackDownloadStatus.REQUESTED) {
               $$2.downloadStatus = ServerPackManager.PackDownloadStatus.PENDING;
               $$0.add($$2);
            }
         }
      }

      if (!$$0.isEmpty()) {
         Map<UUID, DownloadQueue.DownloadRequest> $$3 = new HashMap<>();

         for (ServerPackManager.ServerPackData $$4 : $$0) {
            $$3.put($$4.id, new DownloadQueue.DownloadRequest($$4.url, $$4.hash));
         }

         this.downloader.download($$3, p_310750_ -> this.onDownload($$0, p_310750_));
      }

      return $$1;
   }

   private void triggerReloadIfNeeded() {
      boolean $$0 = false;
      final List<ServerPackManager.ServerPackData> $$1 = new ArrayList<>();
      final List<ServerPackManager.ServerPackData> $$2 = new ArrayList<>();

      for (ServerPackManager.ServerPackData $$3 : this.packs) {
         if ($$3.activationStatus == ServerPackManager.ActivationStatus.PENDING) {
            return;
         }

         boolean $$4 = $$3.promptAccepted && $$3.downloadStatus == ServerPackManager.PackDownloadStatus.DONE && !$$3.isRemoved();
         if ($$4 && $$3.activationStatus == ServerPackManager.ActivationStatus.INACTIVE) {
            $$1.add($$3);
            $$0 = true;
         }

         if ($$3.activationStatus == ServerPackManager.ActivationStatus.ACTIVE) {
            if (!$$4) {
               $$0 = true;
               $$2.add($$3);
            } else {
               $$1.add($$3);
            }
         }
      }

      if ($$0) {
         for (ServerPackManager.ServerPackData $$5 : $$1) {
            if ($$5.activationStatus != ServerPackManager.ActivationStatus.ACTIVE) {
               $$5.activationStatus = ServerPackManager.ActivationStatus.PENDING;
            }
         }

         for (ServerPackManager.ServerPackData $$6 : $$2) {
            $$6.activationStatus = ServerPackManager.ActivationStatus.PENDING;
         }

         this.reloadConfig.scheduleReload(new PackReloadConfig.Callbacks() {
            @Override
            public void onSuccess() {
               for (ServerPackManager.ServerPackData $$0 : $$1) {
                  $$0.activationStatus = ServerPackManager.ActivationStatus.ACTIVE;
                  if ($$0.removalReason == null) {
                     ServerPackManager.this.packLoadFeedback.reportFinalResult($$0.id, PackLoadFeedback.FinalResult.APPLIED);
                  }
               }

               for (ServerPackManager.ServerPackData $$1 : $$2) {
                  $$1.activationStatus = ServerPackManager.ActivationStatus.INACTIVE;
               }

               ServerPackManager.this.registerForUpdate();
            }

            @Override
            public void onFailure(boolean p_311939_) {
               if (!p_311939_) {
                  $$1.clear();

                  for (ServerPackManager.ServerPackData $$1 : ServerPackManager.this.packs) {
                     switch ($$1.activationStatus) {
                        case INACTIVE:
                           $$1.setRemovalReasonIfNotSet(ServerPackManager.RemovalReason.DISCARDED);
                           break;
                        case PENDING:
                           $$1.activationStatus = ServerPackManager.ActivationStatus.INACTIVE;
                           $$1.setRemovalReasonIfNotSet(ServerPackManager.RemovalReason.ACTIVATION_FAILED);
                           break;
                        case ACTIVE:
                           $$1.add($$1);
                     }
                  }

                  ServerPackManager.this.registerForUpdate();
               } else {
                  for (ServerPackManager.ServerPackData $$2 : ServerPackManager.this.packs) {
                     if ($$2.activationStatus == ServerPackManager.ActivationStatus.PENDING) {
                        $$2.activationStatus = ServerPackManager.ActivationStatus.INACTIVE;
                     }
                  }
               }
            }

            @Override
            public List<PackReloadConfig.IdAndPath> packsToLoad() {
               return $$1.stream().map(p_312955_ -> new PackReloadConfig.IdAndPath(p_312955_.id, p_312955_.path)).toList();
            }
         });
      }
   }

   @OnlyIn(Dist.CLIENT)
   static enum ActivationStatus {
      INACTIVE,
      PENDING,
      ACTIVE;
   }

   @OnlyIn(Dist.CLIENT)
   static enum PackDownloadStatus {
      REQUESTED,
      PENDING,
      DONE;
   }

   @OnlyIn(Dist.CLIENT)
   public static enum PackPromptStatus {
      PENDING,
      ALLOWED,
      DECLINED;
   }

   @OnlyIn(Dist.CLIENT)
   static enum RemovalReason {
      DOWNLOAD_FAILED(PackLoadFeedback.FinalResult.DOWNLOAD_FAILED),
      ACTIVATION_FAILED(PackLoadFeedback.FinalResult.ACTIVATION_FAILED),
      DECLINED(PackLoadFeedback.FinalResult.DECLINED),
      DISCARDED(PackLoadFeedback.FinalResult.DISCARDED),
      SERVER_REMOVED(null),
      SERVER_REPLACED(null);

      @Nullable
      final PackLoadFeedback.FinalResult serverResponse;

      private RemovalReason(@Nullable final PackLoadFeedback.FinalResult pServerResponse) {
         this.serverResponse = pServerResponse;
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class ServerPackData {
      final UUID id;
      final URL url;
      @Nullable
      final HashCode hash;
      @Nullable
      Path path;
      @Nullable
      ServerPackManager.RemovalReason removalReason;
      ServerPackManager.PackDownloadStatus downloadStatus = ServerPackManager.PackDownloadStatus.REQUESTED;
      ServerPackManager.ActivationStatus activationStatus = ServerPackManager.ActivationStatus.INACTIVE;
      boolean promptAccepted;

      ServerPackData(UUID pId, URL pUrl, @Nullable HashCode pHash) {
         this.id = pId;
         this.url = pUrl;
         this.hash = pHash;
      }

      public void setRemovalReasonIfNotSet(ServerPackManager.RemovalReason pRemovalReason) {
         if (this.removalReason == null) {
            this.removalReason = pRemovalReason;
         }
      }

      public boolean isRemoved() {
         return this.removalReason != null;
      }
   }
}
