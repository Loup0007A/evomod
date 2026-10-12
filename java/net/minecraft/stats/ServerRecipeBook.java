package net.minecraft.stats;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.ResourceLocationException;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.protocol.game.ClientboundRecipePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import org.slf4j.Logger;

public class ServerRecipeBook extends RecipeBook {
   public static final String RECIPE_BOOK_TAG = "recipeBook";
   private static final Logger LOGGER = LogUtils.getLogger();

   public int addRecipes(Collection<RecipeHolder<?>> pRecipes, ServerPlayer pPlayer) {
      List<ResourceLocation> $$2 = Lists.newArrayList();
      int $$3 = 0;

      for (RecipeHolder<?> $$4 : pRecipes) {
         ResourceLocation $$5 = $$4.id();
         if (!this.known.contains($$5) && !$$4.value().isSpecial()) {
            this.add($$5);
            this.addHighlight($$5);
            $$2.add($$5);
            CriteriaTriggers.RECIPE_UNLOCKED.trigger(pPlayer, $$4);
            $$3++;
         }
      }

      if ($$2.size() > 0) {
         this.sendRecipes(ClientboundRecipePacket.State.ADD, pPlayer, $$2);
      }

      return $$3;
   }

   public int removeRecipes(Collection<RecipeHolder<?>> pRecipes, ServerPlayer pPlayer) {
      List<ResourceLocation> $$2 = Lists.newArrayList();
      int $$3 = 0;

      for (RecipeHolder<?> $$4 : pRecipes) {
         ResourceLocation $$5 = $$4.id();
         if (this.known.contains($$5)) {
            this.remove($$5);
            $$2.add($$5);
            $$3++;
         }
      }

      this.sendRecipes(ClientboundRecipePacket.State.REMOVE, pPlayer, $$2);
      return $$3;
   }

   private void sendRecipes(ClientboundRecipePacket.State pState, ServerPlayer pPlayer, List<ResourceLocation> pRecipes) {
      pPlayer.connection.send(new ClientboundRecipePacket(pState, pRecipes, Collections.emptyList(), this.getBookSettings()));
   }

   public CompoundTag toNbt() {
      CompoundTag $$0 = new CompoundTag();
      this.getBookSettings().write($$0);
      ListTag $$1 = new ListTag();

      for (ResourceLocation $$2 : this.known) {
         $$1.add(StringTag.valueOf($$2.toString()));
      }

      $$0.put("recipes", $$1);
      ListTag $$3 = new ListTag();

      for (ResourceLocation $$4 : this.highlight) {
         $$3.add(StringTag.valueOf($$4.toString()));
      }

      $$0.put("toBeDisplayed", $$3);
      return $$0;
   }

   public void fromNbt(CompoundTag pTag, RecipeManager pRecipeManager) {
      this.setBookSettings(RecipeBookSettings.read(pTag));
      ListTag $$2 = pTag.getList("recipes", 8);
      this.loadRecipes($$2, this::add, pRecipeManager);
      ListTag $$3 = pTag.getList("toBeDisplayed", 8);
      this.loadRecipes($$3, this::addHighlight, pRecipeManager);
   }

   private void loadRecipes(ListTag pTags, Consumer<RecipeHolder<?>> pRecipeConsumer, RecipeManager pRecipeManager) {
      for (int $$3 = 0; $$3 < pTags.size(); $$3++) {
         String $$4 = pTags.getString($$3);

         try {
            ResourceLocation $$5 = ResourceLocation.parse($$4);
            Optional<RecipeHolder<?>> $$6 = pRecipeManager.byKey($$5);
            if ($$6.isEmpty()) {
               LOGGER.error("Tried to load unrecognized recipe: {} removed now.", $$5);
            } else {
               pRecipeConsumer.accept($$6.get());
            }
         } catch (ResourceLocationException var8) {
            LOGGER.error("Tried to load improperly formatted recipe: {} removed now.", $$4);
         }
      }
   }

   public void sendInitialRecipeBook(ServerPlayer pPlayer) {
      pPlayer.connection.send(new ClientboundRecipePacket(ClientboundRecipePacket.State.INIT, this.known, this.highlight, this.getBookSettings()));
   }
}
