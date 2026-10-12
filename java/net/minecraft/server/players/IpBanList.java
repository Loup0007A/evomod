package net.minecraft.server.players;

import com.google.gson.JsonObject;
import java.io.File;
import java.net.SocketAddress;
import javax.annotation.Nullable;

public class IpBanList extends StoredUserList<String, IpBanListEntry> {
   public IpBanList(File pFile) {
      super(pFile);
   }

   @Override
   protected StoredUserEntry<String> createEntry(JsonObject pEntryData) {
      return new IpBanListEntry(pEntryData);
   }

   public boolean isBanned(SocketAddress pAddress) {
      String $$1 = this.getIpFromAddress(pAddress);
      return this.contains($$1);
   }

   public boolean isBanned(String pAddress) {
      return this.contains(pAddress);
   }

   @Nullable
   public IpBanListEntry get(SocketAddress pAddress) {
      String $$1 = this.getIpFromAddress(pAddress);
      return this.get($$1);
   }

   private String getIpFromAddress(SocketAddress pAddress) {
      String $$1 = pAddress.toString();
      if ($$1.contains("/")) {
         $$1 = $$1.substring($$1.indexOf(47) + 1);
      }

      if ($$1.contains(":")) {
         $$1 = $$1.substring(0, $$1.indexOf(58));
      }

      return $$1;
   }
}
