package net.minecraftforge.event;

import javax.annotation.Nullable;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraftforge.eventbus.api.Event;
import org.jetbrains.annotations.ApiStatus.Internal;

public abstract class GatherComponentsEvent extends Event {
   private final DataComponentMap.Builder components = DataComponentMap.builder();
   private final DataComponentMap originalComponents;
   private final Object owner;

   protected GatherComponentsEvent(Object owner, DataComponentMap originalComponents) {
      this.originalComponents = originalComponents;
      this.owner = owner;
   }

   public <T> void register(DataComponentType<T> componentType, @Nullable T value) {
      this.components.set(componentType, value);
   }

   @Internal
   public DataComponentMap getDataComponentMap() {
      return this.components.build();
   }

   public DataComponentMap getOriginalComponentMap() {
      return this.originalComponents;
   }

   public Object getOwner() {
      return this.owner;
   }

   public static class Item extends GatherComponentsEvent {
      public Item(net.minecraft.world.item.Item item, DataComponentMap dataComponents) {
         super(item, dataComponents);
      }

      public net.minecraft.world.item.Item getOwner() {
         return (net.minecraft.world.item.Item)super.getOwner();
      }
   }
}
