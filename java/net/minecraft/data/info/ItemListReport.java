package net.minecraft.data.info;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.RegistryOps;

public class ItemListReport implements DataProvider {
   private final PackOutput output;
   private final CompletableFuture<HolderLookup.Provider> registries;

   public ItemListReport(PackOutput pOutput, CompletableFuture<HolderLookup.Provider> pRegistries) {
      this.output = pOutput;
      this.registries = pRegistries;
   }

   @Override
   public CompletableFuture<?> run(CachedOutput pOutput) {
      Path $$1 = this.output.getOutputFolder(PackOutput.Target.REPORTS).resolve("items.json");
      return this.registries
         .thenCompose(
            p_334112_ -> {
               JsonObject $$3 = new JsonObject();
               RegistryOps<JsonElement> $$4 = p_334112_.createSerializationContext(JsonOps.INSTANCE);
               p_334112_.lookupOrThrow(Registries.ITEM)
                  .listElements()
                  .forEach(
                     p_341012_ -> {
                        JsonObject $$3x = new JsonObject();
                        $$3x.add(
                           "components",
                           (JsonElement)DataComponentMap.CODEC
                              .encodeStart($$4, p_341012_.value().components())
                              .getOrThrow(p_341009_ -> new IllegalStateException("Failed to encode components: " + p_341009_))
                        );
                        $$3.add(p_341012_.getRegisteredName(), $$3x);
                     }
                  );
               return DataProvider.saveStable(pOutput, $$3, $$1);
            }
         );
   }

   @Override
   public final String getName() {
      return "Item List";
   }
}
