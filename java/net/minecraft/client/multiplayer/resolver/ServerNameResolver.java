package net.minecraft.client.multiplayer.resolver;

import com.google.common.annotations.VisibleForTesting;
import java.util.Optional;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ServerNameResolver {
   public static final ServerNameResolver DEFAULT = new ServerNameResolver(
      ServerAddressResolver.SYSTEM, ServerRedirectHandler.createDnsSrvRedirectHandler(), AddressCheck.createFromService()
   );
   private final ServerAddressResolver resolver;
   private final ServerRedirectHandler redirectHandler;
   private final AddressCheck addressCheck;

   @VisibleForTesting
   ServerNameResolver(ServerAddressResolver pResolver, ServerRedirectHandler pRedirectHandler, AddressCheck pAddressCheck) {
      this.resolver = pResolver;
      this.redirectHandler = pRedirectHandler;
      this.addressCheck = pAddressCheck;
   }

   public Optional<ResolvedServerAddress> resolveAddress(ServerAddress pServerAddress) {
      Optional<ResolvedServerAddress> $$1 = this.resolver.resolve(pServerAddress);
      if ((!$$1.isPresent() || this.addressCheck.isAllowed($$1.get())) && this.addressCheck.isAllowed(pServerAddress)) {
         Optional<ServerAddress> $$2 = this.redirectHandler.lookupRedirect(pServerAddress);
         if ($$2.isPresent()) {
            $$1 = this.resolver.resolve($$2.get()).filter(this.addressCheck::isAllowed);
         }

         return $$1;
      } else {
         return Optional.empty();
      }
   }
}
