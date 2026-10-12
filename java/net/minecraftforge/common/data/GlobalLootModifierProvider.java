package net.minecraftforge.common.data;

import com.google.common.collect.ImmutableList.Builder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import cpw.mods.modlauncher.api.LamdbaExceptionUtils;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.loot.IGlobalLootModifier;

public abstract class GlobalLootModifierProvider implements DataProvider {
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private final PackOutput output;
   private final String modid;
   private final CompletableFuture<HolderLookup.Provider> registries;
   private final Map<String, IGlobalLootModifier> toSerialize = new HashMap<>();
   private boolean replace = false;

   public GlobalLootModifierProvider(PackOutput output, String modid, CompletableFuture<HolderLookup.Provider> registries) {
      this.output = output;
      this.modid = modid;
      this.registries = registries;
   }

   protected void replacing() {
      this.replace = true;
   }

   protected abstract void start(HolderLookup.Provider var1);

   @Override
   public CompletableFuture<?> run(CachedOutput cache) {
      return this.registries.thenCompose(p -> this.run(cache, p));
   }

   private CompletableFuture<?> run(CachedOutput cache, HolderLookup.Provider registries) {
      this.start(registries);
      Path forgePath = this.output.getOutputFolder(PackOutput.Target.DATA_PACK).resolve("forge/loot_modifiers/global_loot_modifiers.json");
      Path modifierFolderPath = this.output.getOutputFolder(PackOutput.Target.DATA_PACK).resolve(this.modid).resolve("loot_modifiers");
      List<ResourceLocation> entries = new ArrayList<>();
      Builder<CompletableFuture<?>> futuresBuilder = new Builder();
      RegistryOps<JsonElement> ops = registries.createSerializationContext(JsonOps.INSTANCE);
      Codec<IGlobalLootModifier> codec = IGlobalLootModifier.DIRECT_CODEC;
      this.toSerialize.forEach(LamdbaExceptionUtils.rethrowBiConsumer((name, instance) -> {
         JsonElement json = (JsonElement)codec.encodeStart(ops, instance).getOrThrow();
         entries.add(ResourceLocation.fromNamespaceAndPath(this.modid, name));
         Path modifierPath = modifierFolderPath.resolve(name + ".json");
         futuresBuilder.add(DataProvider.saveStable(cache, json, modifierPath));
      }));
      JsonObject forgeJson = new JsonObject();
      forgeJson.addProperty("replace", this.replace);
      forgeJson.add("entries", GSON.toJsonTree(entries.stream().map(ResourceLocation::toString).collect(Collectors.toList())));
      futuresBuilder.add(DataProvider.saveStable(cache, forgeJson, forgePath));
      return CompletableFuture.allOf((CompletableFuture<?>[])futuresBuilder.build().toArray(CompletableFuture[]::new));
   }

   public <T extends IGlobalLootModifier> void add(String modifier, T instance) {
      this.toSerialize.put(modifier, instance);
   }

   @Override
   public String getName() {
      return "Global Loot Modifiers : " + this.modid;
   }
}
