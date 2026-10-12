package net.minecraftforge.common.loot;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LootModifierManager extends SimpleJsonResourceReloadListener {
   private static final Logger LOGGER = LogManager.getLogger();
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
   private static final String FOLDER = "loot_modifiers";
   private final RegistryAccess registries;
   private Map<ResourceLocation, IGlobalLootModifier> modifiers = ImmutableMap.of();

   public LootModifierManager(RegistryAccess registries) {
      super(GSON, "loot_modifiers");
      this.registries = registries;
   }

   @Override
   protected Map<ResourceLocation, JsonElement> prepare(ResourceManager resources, ProfilerFiller profilerFiller) {
      ResourceLocation path = ResourceLocation.fromNamespaceAndPath("forge", "loot_modifiers/global_loot_modifiers.json");
      List<ResourceLocation> toLoad = new ArrayList<>();

      for (Resource resource : resources.getResourceStack(path)) {
         try (BufferedReader reader = resource.openAsReader()) {
            JsonObject json = GsonHelper.fromJson(GSON, reader, JsonObject.class);
            if (GsonHelper.getAsBoolean(json, "replace", false)) {
               toLoad.clear();
            }

            for (JsonElement entry : GsonHelper.getAsJsonArray(json, "entries")) {
               ResourceLocation loc = ResourceLocation.parse(entry.getAsString());
               toLoad.remove(loc);
               toLoad.add(loc);
            }
         } catch (IOException | RuntimeException var14) {
            LOGGER.error("Couldn't read global loot modifier list {} in data pack {}", path, resource.sourcePackId(), var14);
         }
      }

      Map<ResourceLocation, JsonElement> ret = super.prepare(resources, profilerFiller);
      ret.keySet().removeIf(k -> !toLoad.contains(k));
      return ret;
   }

   protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager resourceManagerIn, ProfilerFiller profilerIn) {
      Builder<ResourceLocation, IGlobalLootModifier> builder = ImmutableMap.builder();
      RegistryOps<JsonElement> ops = this.registries.createSerializationContext(JsonOps.INSTANCE);
      resources.forEach(
         (location, json) -> IGlobalLootModifier.DIRECT_CODEC
               .parse(ops, json)
               .ifError(error -> LOGGER.warn("Could not decode GlobalLootModifier with json id {} - error: {}", location, error.message()))
               .ifSuccess(modifier -> builder.put(location, modifier))
      );
      this.modifiers = builder.build();
   }

   public Collection<IGlobalLootModifier> getAllLootMods() {
      return this.modifiers.values();
   }
}
