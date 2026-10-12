package net.minecraftforge.resource;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.fml.Logging;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoader;
import net.minecraftforge.fml.ModLoadingStage;
import net.minecraftforge.fml.ModLoadingWarning;
import net.minecraftforge.forgespi.language.IModFileInfo;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraftforge.forgespi.locating.IModFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public class ResourcePackLoader {
   private static final Logger LOGGER = LogManager.getLogger();

   public static void loadResourcePacks(PackRepository resourcePacks, boolean client) {
      resourcePacks.addPackFinder(repo -> findPacks(repo, client));
   }

   public static List<String> getPackNames() {
      return ModList.get()
         .applyForEachModFile(mf -> "mod:" + ((IModInfo)mf.getModInfos().get(0)).getModId())
         .filter(n -> !n.equals("mod:minecraft"))
         .collect(Collectors.toList());
   }

   public static <V> Comparator<Entry<String, V>> getSorter() {
      List<String> order = new ArrayList<>();
      order.add("vanilla");
      order.add("mod_resources");
      ModList.get()
         .getModFiles()
         .stream()
         .filter(mf -> mf.requiredLanguageLoaders().stream().noneMatch(ls -> ls.languageName().equals("minecraft")))
         .map(e -> ((IModInfo)e.getMods().get(0)).getModId())
         .map(e -> "mod:" + e)
         .forEach(order::add);
      Object2IntMap<String> order_f = new Object2IntOpenHashMap(order.size());

      for (int x = 0; x < order.size(); x++) {
         order_f.put(order.get(x), x);
      }

      return (e1, e2) -> {
         String s1 = e1.getKey();
         String s2 = e2.getKey();
         int i1 = order_f.getOrDefault(s1, -1);
         int i2 = order_f.getOrDefault(s2, -1);
         if (i1 == i2 && i1 == -1) {
            return s1.compareTo(s2);
         } else if (i1 == -1) {
            return 1;
         } else {
            return i2 == -1 ? -1 : i2 - i1;
         }
      };
   }

   private static void findPacks(Consumer<Pack> packAcceptor, boolean client) {
      PackType type = client ? PackType.CLIENT_RESOURCES : PackType.SERVER_DATA;
      int version = SharedConstants.getCurrentVersion().getPackVersion(type);
      ArrayList<PackResources> hiddenPacks = new ArrayList<>();

      for (IModFileInfo mod : ModList.get().getModFiles()) {
         if (!mod.requiredLanguageLoaders().stream().anyMatch(ls -> ls.languageName().equals("minecraft"))) {
            IModFile file = mod.getFile();
            Path root = file.findResource(new String[]{""});
            PathPackResources.PathResourcesSupplier supplier = new PathPackResources.PathResourcesSupplier(root);
            IModInfo modinfo = (IModInfo)file.getModInfos().get(0);
            String name = "mod:" + modinfo.getModId();
            PackLocationInfo info = new PackLocationInfo(name, Component.literal(file.getFileName()), PackSource.DEFAULT, Optional.empty());
            Pack.Metadata meta = Pack.readPackMetadata(info, supplier, version);
            Pack pack = null;
            if (meta != null) {
               pack = new Pack(info, supplier, meta, new PackSelectionConfig(false, Pack.Position.BOTTOM, false));
            }

            if (pack == null) {
               ModLoader.get().addWarning(new ModLoadingWarning(modinfo, ModLoadingStage.ERROR, "fml.modloading.brokenresources", new Object[]{file}));
            } else {
               LOGGER.debug(Logging.CORE, "Generating PackInfo named {} for mod file {}", name, file.getFilePath());
               if (client && !mod.showAsResourcePack()) {
                  hiddenPacks.add(pack.open());
               } else {
                  packAcceptor.accept(pack);
               }
            }
         }
      }

      if (!hiddenPacks.isEmpty()) {
         PackLocationInfo infox = new PackLocationInfo(
            "mod_resources", Component.translatable("fml.resources.modresources", hiddenPacks.size()), PackSource.DEFAULT, Optional.empty()
         );
         DelegatingPackResources delegating = new DelegatingPackResources(
            infox, new PackMetadataSection(Component.translatable("fml.resources.modresources", hiddenPacks.size()), version, Optional.empty()), hiddenPacks
         );
         Pack.ResourcesSupplier supplierx = delegating.supplier();
         PackLocationInfo wrapper = new PackLocationInfo("mod_resources", Component.literal("Mod Resources"), PackSource.DEFAULT, Optional.empty());
         PackSelectionConfig wrapperCfg = new PackSelectionConfig(true, Pack.Position.BOTTOM, false);
         Pack modResourcesPack = Pack.readMetaAndCreate(wrapper, supplierx, type, wrapperCfg);
         packAcceptor.accept(modResourcesPack);
      }
   }
}
