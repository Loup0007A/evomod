package com.mojang.realmsclient.gui;

import com.mojang.realmsclient.dto.RealmsNews;
import com.mojang.realmsclient.util.RealmsPersistence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RealmsNewsManager {
   private final RealmsPersistence newsLocalStorage;
   private boolean hasUnreadNews;
   private String newsLink;

   public RealmsNewsManager(RealmsPersistence pNewsLocalStorage) {
      this.newsLocalStorage = pNewsLocalStorage;
      RealmsPersistence.RealmsPersistenceData $$1 = pNewsLocalStorage.read();
      this.hasUnreadNews = $$1.hasUnreadNews;
      this.newsLink = $$1.newsLink;
   }

   public boolean hasUnreadNews() {
      return this.hasUnreadNews;
   }

   public String newsLink() {
      return this.newsLink;
   }

   public void updateUnreadNews(RealmsNews pRealmsNews) {
      RealmsPersistence.RealmsPersistenceData $$1 = this.updateNewsStorage(pRealmsNews);
      this.hasUnreadNews = $$1.hasUnreadNews;
      this.newsLink = $$1.newsLink;
   }

   private RealmsPersistence.RealmsPersistenceData updateNewsStorage(RealmsNews pRealmsNews) {
      RealmsPersistence.RealmsPersistenceData $$1 = new RealmsPersistence.RealmsPersistenceData();
      $$1.newsLink = pRealmsNews.newsLink;
      RealmsPersistence.RealmsPersistenceData $$2 = this.newsLocalStorage.read();
      boolean $$3 = $$1.newsLink == null || $$1.newsLink.equals($$2.newsLink);
      if ($$3) {
         return $$2;
      } else {
         $$1.hasUnreadNews = true;
         this.newsLocalStorage.save($$1);
         return $$1;
      }
   }
}
