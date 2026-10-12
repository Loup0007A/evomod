package com.mojang.realmsclient.gui.screens;

import com.mojang.realmsclient.RealmsAvailability;
import com.mojang.realmsclient.dto.RealmsNotification;
import com.mojang.realmsclient.gui.RealmsDataFetcher;
import com.mojang.realmsclient.gui.task.DataFetcher;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RealmsNotificationsScreen extends RealmsScreen {
   private static final ResourceLocation UNSEEN_NOTIFICATION_SPRITE = ResourceLocation.withDefaultNamespace("icon/unseen_notification");
   private static final ResourceLocation NEWS_SPRITE = ResourceLocation.withDefaultNamespace("icon/news");
   private static final ResourceLocation INVITE_SPRITE = ResourceLocation.withDefaultNamespace("icon/invite");
   private static final ResourceLocation TRIAL_AVAILABLE_SPRITE = ResourceLocation.withDefaultNamespace("icon/trial_available");
   private final CompletableFuture<Boolean> validClient = RealmsAvailability.get().thenApply(p_296063_ -> p_296063_.type() == RealmsAvailability.Type.SUCCESS);
   @Nullable
   private DataFetcher.Subscription realmsDataSubscription;
   @Nullable
   private RealmsNotificationsScreen.DataFetcherConfiguration currentConfiguration;
   private volatile int numberOfPendingInvites;
   private static boolean trialAvailable;
   private static boolean hasUnreadNews;
   private static boolean hasUnseenNotifications;
   private final RealmsNotificationsScreen.DataFetcherConfiguration showAll = new RealmsNotificationsScreen.DataFetcherConfiguration() {
      @Override
      public DataFetcher.Subscription initDataFetcher(RealmsDataFetcher p_301294_) {
         DataFetcher.Subscription $$1 = p_301294_.dataFetcher.createSubscription();
         RealmsNotificationsScreen.this.addNewsAndInvitesSubscriptions(p_301294_, $$1);
         RealmsNotificationsScreen.this.addNotificationsSubscriptions(p_301294_, $$1);
         return $$1;
      }

      @Override
      public boolean showOldNotifications() {
         return true;
      }
   };
   private final RealmsNotificationsScreen.DataFetcherConfiguration onlyNotifications = new RealmsNotificationsScreen.DataFetcherConfiguration() {
      @Override
      public DataFetcher.Subscription initDataFetcher(RealmsDataFetcher p_275318_) {
         DataFetcher.Subscription $$1 = p_275318_.dataFetcher.createSubscription();
         RealmsNotificationsScreen.this.addNotificationsSubscriptions(p_275318_, $$1);
         return $$1;
      }

      @Override
      public boolean showOldNotifications() {
         return false;
      }
   };

   public RealmsNotificationsScreen() {
      super(GameNarrator.NO_TITLE);
   }

   @Override
   public void init() {
      if (this.realmsDataSubscription != null) {
         this.realmsDataSubscription.forceUpdate();
      }
   }

   @Override
   public void added() {
      super.added();
      this.minecraft.realmsDataFetcher().notificationsTask.reset();
   }

   @Nullable
   private RealmsNotificationsScreen.DataFetcherConfiguration getConfiguration() {
      boolean $$0 = this.inTitleScreen() && this.validClient.getNow(false);
      if (!$$0) {
         return null;
      } else {
         return this.getRealmsNotificationsEnabled() ? this.showAll : this.onlyNotifications;
      }
   }

   @Override
   public void tick() {
      RealmsNotificationsScreen.DataFetcherConfiguration $$0 = this.getConfiguration();
      if (!Objects.equals(this.currentConfiguration, $$0)) {
         this.currentConfiguration = $$0;
         if (this.currentConfiguration != null) {
            this.realmsDataSubscription = this.currentConfiguration.initDataFetcher(this.minecraft.realmsDataFetcher());
         } else {
            this.realmsDataSubscription = null;
         }
      }

      if (this.realmsDataSubscription != null) {
         this.realmsDataSubscription.tick();
      }
   }

   private boolean getRealmsNotificationsEnabled() {
      return this.minecraft.options.realmsNotifications().get();
   }

   private boolean inTitleScreen() {
      return this.minecraft.screen instanceof TitleScreen;
   }

   @Override
   public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
      super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
      if (this.validClient.getNow(false)) {
         this.drawIcons(pGuiGraphics);
      }
   }

   @Override
   public void renderBackground(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
   }

   private void drawIcons(GuiGraphics pGuiGraphics) {
      int $$1 = this.numberOfPendingInvites;
      int $$2 = 24;
      int $$3 = this.height / 4 + 48;
      int $$4 = this.width / 2 + 100;
      int $$5 = $$3 + 48 + 2;
      int $$6 = $$4 - 3;
      if (hasUnseenNotifications) {
         pGuiGraphics.blitSprite(UNSEEN_NOTIFICATION_SPRITE, $$6 - 12, $$5 + 3, 10, 10);
         $$6 -= 16;
      }

      if (this.currentConfiguration != null && this.currentConfiguration.showOldNotifications()) {
         if (hasUnreadNews) {
            pGuiGraphics.blitSprite(NEWS_SPRITE, $$6 - 14, $$5 + 1, 14, 14);
            $$6 -= 16;
         }

         if ($$1 != 0) {
            pGuiGraphics.blitSprite(INVITE_SPRITE, $$6 - 14, $$5 + 1, 14, 14);
            $$6 -= 16;
         }

         if (trialAvailable) {
            pGuiGraphics.blitSprite(TRIAL_AVAILABLE_SPRITE, $$6 - 10, $$5 + 4, 8, 8);
         }
      }
   }

   void addNewsAndInvitesSubscriptions(RealmsDataFetcher pDataFetcher, DataFetcher.Subscription pSubscription) {
      pSubscription.subscribe(pDataFetcher.pendingInvitesTask, p_239521_ -> this.numberOfPendingInvites = p_239521_);
      pSubscription.subscribe(pDataFetcher.trialAvailabilityTask, p_239494_ -> trialAvailable = p_239494_);
      pSubscription.subscribe(pDataFetcher.newsTask, p_238946_ -> {
         pDataFetcher.newsManager.updateUnreadNews(p_238946_);
         hasUnreadNews = pDataFetcher.newsManager.hasUnreadNews();
      });
   }

   void addNotificationsSubscriptions(RealmsDataFetcher pDataFetcher, DataFetcher.Subscription pSubscription) {
      pSubscription.subscribe(pDataFetcher.notificationsTask, p_274637_ -> {
         hasUnseenNotifications = false;

         for (RealmsNotification $$1 : p_274637_) {
            if (!$$1.seen()) {
               hasUnseenNotifications = true;
               break;
            }
         }
      });
   }

   @OnlyIn(Dist.CLIENT)
   interface DataFetcherConfiguration {
      DataFetcher.Subscription initDataFetcher(RealmsDataFetcher pDataFetcher);

      boolean showOldNotifications();
   }
}
