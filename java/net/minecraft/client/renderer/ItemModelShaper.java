package net.minecraft.client.renderer;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ItemModelShaper {
   public final Int2ObjectMap<ModelResourceLocation> shapes = new Int2ObjectOpenHashMap(256);
   private final Int2ObjectMap<BakedModel> shapesCache = new Int2ObjectOpenHashMap(256);
   private final ModelManager modelManager;

   public ItemModelShaper(ModelManager pModelManager) {
      this.modelManager = pModelManager;
   }

   public BakedModel getItemModel(ItemStack pStack) {
      BakedModel $$1 = this.getItemModel(pStack.getItem());
      return $$1 == null ? this.modelManager.getMissingModel() : $$1;
   }

   @Nullable
   public BakedModel getItemModel(Item pItem) {
      return (BakedModel)this.shapesCache.get(getIndex(pItem));
   }

   private static int getIndex(Item pItem) {
      return Item.getId(pItem);
   }

   public void register(Item pItem, ModelResourceLocation pModelLocation) {
      this.shapes.put(getIndex(pItem), pModelLocation);
   }

   public ModelManager getModelManager() {
      return this.modelManager;
   }

   public void rebuildCache() {
      this.shapesCache.clear();
      ObjectIterator var1 = this.shapes.entrySet().iterator();

      while (var1.hasNext()) {
         Entry<Integer, ModelResourceLocation> $$0 = (Entry<Integer, ModelResourceLocation>)var1.next();
         this.shapesCache.put($$0.getKey(), this.modelManager.getModel($$0.getValue()));
      }
   }
}
