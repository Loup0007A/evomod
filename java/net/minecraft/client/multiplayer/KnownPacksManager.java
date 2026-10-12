package net.minecraft.client.multiplayer;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.KnownPack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class KnownPacksManager {
   private final PackRepository repository = ServerPacksSource.createVanillaTrustedRepository();
   private final Map<KnownPack, String> knownPackToId;

   public KnownPacksManager() {
      this.repository.reload();
      Builder<KnownPack, String> $$0 = ImmutableMap.builder();
      this.repository.getAvailablePacks().forEach(p_334709_ -> {
         PackLocationInfo $$2 = p_334709_.location();
         $$2.knownPackInfo().ifPresent(p_333246_ -> $$0.put(p_333246_, $$2.id()));
      });
      this.knownPackToId = $$0.build();
   }

   public List<KnownPack> trySelectingPacks(List<KnownPack> pPacks) {
      List<KnownPack> $$1 = new ArrayList<>(pPacks.size());
      List<String> $$2 = new ArrayList<>(pPacks.size());

      for (KnownPack $$3 : pPacks) {
         String $$4 = this.knownPackToId.get($$3);
         if ($$4 != null) {
            $$2.add($$4);
            $$1.add($$3);
         }
      }

      this.repository.setSelected($$2);
      return $$1;
   }

   public CloseableResourceManager createResourceManager() {
      List<PackResources> $$0 = this.repository.openAllSelected();
      return new MultiPackResourceManager(PackType.SERVER_DATA, $$0);
   }
}
