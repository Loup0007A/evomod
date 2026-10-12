package net.minecraft.advancements;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

public class AdvancementTree {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Map<ResourceLocation, AdvancementNode> nodes = new Object2ObjectOpenHashMap();
   private final Set<AdvancementNode> roots = new ObjectLinkedOpenHashSet();
   private final Set<AdvancementNode> tasks = new ObjectLinkedOpenHashSet();
   @Nullable
   private AdvancementTree.Listener listener;

   private void remove(AdvancementNode pNode) {
      for (AdvancementNode $$1 : pNode.children()) {
         this.remove($$1);
      }

      LOGGER.info("Forgot about advancement {}", pNode.holder());
      this.nodes.remove(pNode.holder().id());
      if (pNode.parent() == null) {
         this.roots.remove(pNode);
         if (this.listener != null) {
            this.listener.onRemoveAdvancementRoot(pNode);
         }
      } else {
         this.tasks.remove(pNode);
         if (this.listener != null) {
            this.listener.onRemoveAdvancementTask(pNode);
         }
      }
   }

   public void remove(Set<ResourceLocation> pAdvancements) {
      for (ResourceLocation $$1 : pAdvancements) {
         AdvancementNode $$2 = this.nodes.get($$1);
         if ($$2 == null) {
            LOGGER.warn("Told to remove advancement {} but I don't know what that is", $$1);
         } else {
            this.remove($$2);
         }
      }
   }

   public void addAll(Collection<AdvancementHolder> pAdvancements) {
      List<AdvancementHolder> $$1 = new ArrayList<>(pAdvancements);

      while (!$$1.isEmpty()) {
         if (!$$1.removeIf(this::tryInsert)) {
            LOGGER.error("Couldn't load advancements: {}", $$1);
            break;
         }
      }

      LOGGER.info("Loaded {} advancements", this.nodes.size());
   }

   private boolean tryInsert(AdvancementHolder p_300067_) {
      Optional<ResourceLocation> $$1 = p_300067_.value().parent();
      AdvancementNode $$2 = $$1.map(this.nodes::get).orElse(null);
      if ($$2 == null && $$1.isPresent()) {
         return false;
      } else {
         AdvancementNode $$3 = new AdvancementNode(p_300067_, $$2);
         if ($$2 != null) {
            $$2.addChild($$3);
         }

         this.nodes.put(p_300067_.id(), $$3);
         if ($$2 == null) {
            this.roots.add($$3);
            if (this.listener != null) {
               this.listener.onAddAdvancementRoot($$3);
            }
         } else {
            this.tasks.add($$3);
            if (this.listener != null) {
               this.listener.onAddAdvancementTask($$3);
            }
         }

         return true;
      }
   }

   public void clear() {
      this.nodes.clear();
      this.roots.clear();
      this.tasks.clear();
      if (this.listener != null) {
         this.listener.onAdvancementsCleared();
      }
   }

   public Iterable<AdvancementNode> roots() {
      return this.roots;
   }

   public Collection<AdvancementNode> nodes() {
      return this.nodes.values();
   }

   @Nullable
   public AdvancementNode get(ResourceLocation pId) {
      return this.nodes.get(pId);
   }

   @Nullable
   public AdvancementNode get(AdvancementHolder pAdvancement) {
      return this.nodes.get(pAdvancement.id());
   }

   public void setListener(@Nullable AdvancementTree.Listener pListener) {
      this.listener = pListener;
      if (pListener != null) {
         for (AdvancementNode $$1 : this.roots) {
            pListener.onAddAdvancementRoot($$1);
         }

         for (AdvancementNode $$2 : this.tasks) {
            pListener.onAddAdvancementTask($$2);
         }
      }
   }

   public interface Listener {
      void onAddAdvancementRoot(AdvancementNode pAdvancement);

      void onRemoveAdvancementRoot(AdvancementNode pAdvancement);

      void onAddAdvancementTask(AdvancementNode pAdvancement);

      void onRemoveAdvancementTask(AdvancementNode pAdvancement);

      void onAdvancementsCleared();
   }
}
