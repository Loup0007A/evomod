package net.minecraft.data.info;

import com.google.common.collect.UnmodifiableIterator;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import net.minecraft.Util;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockTypes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;

public class BlockListReport implements DataProvider {
   private final PackOutput output;
   private final CompletableFuture<HolderLookup.Provider> registries;

   public BlockListReport(PackOutput pOutput, CompletableFuture<HolderLookup.Provider> pRegistries) {
      this.output = pOutput;
      this.registries = pRegistries;
   }

   @Override
   public CompletableFuture<?> run(CachedOutput pOutput) {
      Path $$1 = this.output.getOutputFolder(PackOutput.Target.REPORTS).resolve("blocks.json");
      return this.registries
         .thenCompose(
            p_332877_ -> {
               JsonObject $$3 = new JsonObject();
               RegistryOps<JsonElement> $$4 = p_332877_.createSerializationContext(JsonOps.INSTANCE);
               p_332877_.lookupOrThrow(Registries.BLOCK)
                  .listElements()
                  .forEach(
                     p_331565_ -> {
                        JsonObject $$3x = new JsonObject();
                        StateDefinition<Block, BlockState> $$4x = p_331565_.value().getStateDefinition();
                        if (!$$4x.getProperties().isEmpty()) {
                           JsonObject $$5 = new JsonObject();

                           for (Property<?> $$6 : $$4x.getProperties()) {
                              JsonArray $$7 = new JsonArray();

                              for (Comparable<?> $$8 : $$6.getPossibleValues()) {
                                 $$7.add(Util.getPropertyName($$6, $$8));
                              }

                              $$5.add($$6.getName(), $$7);
                           }

                           $$3x.add("properties", $$5);
                        }

                        JsonArray $$9 = new JsonArray();
                        UnmodifiableIterator var13 = $$4x.getPossibleStates().iterator();

                        while (var13.hasNext()) {
                           BlockState $$10 = (BlockState)var13.next();
                           JsonObject $$11 = new JsonObject();
                           JsonObject $$12 = new JsonObject();

                           for (Property<?> $$13 : $$4x.getProperties()) {
                              $$12.addProperty($$13.getName(), Util.getPropertyName($$13, $$10.getValue($$13)));
                           }

                           if ($$12.size() > 0) {
                              $$11.add("properties", $$12);
                           }

                           $$11.addProperty("id", Block.getId($$10));
                           if ($$10 == p_331565_.value().defaultBlockState()) {
                              $$11.addProperty("default", true);
                           }

                           $$9.add($$11);
                        }

                        $$3x.add("states", $$9);
                        String $$14 = p_331565_.getRegisteredName();
                        JsonElement $$15 = (JsonElement)BlockTypes.CODEC
                           .codec()
                           .encodeStart($$4, p_331565_.value())
                           .getOrThrow(
                              p_334014_ -> new AssertionError("Failed to serialize block " + $$14 + " (is type registered in BlockTypes?): " + p_334014_)
                           );
                        $$3x.add("definition", $$15);
                        $$3.add($$14, $$3x);
                     }
                  );
               return DataProvider.saveStable(pOutput, $$3, $$1);
            }
         );
   }

   @Override
   public final String getName() {
      return "Block List";
   }
}
