package net.minecraftforge.registries;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public class RegistryBuilder<T> {
   private static final int MAX_ID = 2147483646;
   private ResourceLocation registryName;
   private ResourceLocation optionalDefaultKey;
   private int minId = 0;
   private int maxId = 2147483646;
   private final List<IForgeRegistry.AddCallback<T>> addCallback = new ArrayList<>();
   private final List<IForgeRegistry.ClearCallback<T>> clearCallback = new ArrayList<>();
   private final List<IForgeRegistry.CreateCallback<T>> createCallback = new ArrayList<>();
   private final List<IForgeRegistry.ValidateCallback<T>> validateCallback = new ArrayList<>();
   private final List<IForgeRegistry.BakeCallback<T>> bakeCallback = new ArrayList<>();
   private boolean saveToDisc = true;
   private boolean sync = true;
   private boolean allowOverrides = true;
   private boolean allowModifications = false;
   private boolean hasWrapper = false;
   private IForgeRegistry.MissingFactory<T> missingFactory;
   private final Set<ResourceLocation> legacyNames = new HashSet<>();
   @Nullable
   private Function<T, Holder.Reference<T>> intrusiveHolderCallback = null;

   public static <T> RegistryBuilder<T> of() {
      return new RegistryBuilder<>();
   }

   public static <T> RegistryBuilder<T> of(String name) {
      return of(ResourceLocation.parse(name));
   }

   public static <T> RegistryBuilder<T> of(ResourceLocation name) {
      return new RegistryBuilder<T>().setName(name);
   }

   public RegistryBuilder<T> setName(ResourceLocation name) {
      this.registryName = name;
      return this;
   }

   public RegistryBuilder<T> setIDRange(int min, int max) {
      this.minId = Math.max(min, 0);
      this.maxId = Math.min(max, 2147483646);
      return this;
   }

   public RegistryBuilder<T> setMaxID(int max) {
      return this.setIDRange(0, max);
   }

   public RegistryBuilder<T> setDefaultKey(ResourceLocation key) {
      this.optionalDefaultKey = key;
      return this;
   }

   public RegistryBuilder<T> addCallback(Object inst) {
      if (inst instanceof IForgeRegistry.AddCallback) {
         this.add((IForgeRegistry.AddCallback<T>)inst);
      }

      if (inst instanceof IForgeRegistry.ClearCallback) {
         this.add((IForgeRegistry.ClearCallback<T>)inst);
      }

      if (inst instanceof IForgeRegistry.CreateCallback) {
         this.add((IForgeRegistry.CreateCallback<T>)inst);
      }

      if (inst instanceof IForgeRegistry.ValidateCallback) {
         this.add((IForgeRegistry.ValidateCallback<T>)inst);
      }

      if (inst instanceof IForgeRegistry.BakeCallback) {
         this.add((IForgeRegistry.BakeCallback<T>)inst);
      }

      if (inst instanceof IForgeRegistry.MissingFactory) {
         this.set((IForgeRegistry.MissingFactory<T>)inst);
      }

      return this;
   }

   public RegistryBuilder<T> add(IForgeRegistry.AddCallback<T> add) {
      this.addCallback.add(add);
      return this;
   }

   public RegistryBuilder<T> onAdd(IForgeRegistry.AddCallback<T> add) {
      return this.add(add);
   }

   public RegistryBuilder<T> add(IForgeRegistry.ClearCallback<T> clear) {
      this.clearCallback.add(clear);
      return this;
   }

   public RegistryBuilder<T> onClear(IForgeRegistry.ClearCallback<T> clear) {
      return this.add(clear);
   }

   public RegistryBuilder<T> add(IForgeRegistry.CreateCallback<T> create) {
      this.createCallback.add(create);
      return this;
   }

   public RegistryBuilder<T> onCreate(IForgeRegistry.CreateCallback<T> create) {
      return this.add(create);
   }

   public RegistryBuilder<T> add(IForgeRegistry.ValidateCallback<T> validate) {
      this.validateCallback.add(validate);
      return this;
   }

   public RegistryBuilder<T> onValidate(IForgeRegistry.ValidateCallback<T> validate) {
      return this.add(validate);
   }

   public RegistryBuilder<T> add(IForgeRegistry.BakeCallback<T> bake) {
      this.bakeCallback.add(bake);
      return this;
   }

   public RegistryBuilder<T> onBake(IForgeRegistry.BakeCallback<T> bake) {
      return this.add(bake);
   }

   public RegistryBuilder<T> set(IForgeRegistry.MissingFactory<T> missing) {
      this.missingFactory = missing;
      return this;
   }

   public RegistryBuilder<T> missing(IForgeRegistry.MissingFactory<T> missing) {
      return this.set(missing);
   }

   public RegistryBuilder<T> disableSaving() {
      this.saveToDisc = false;
      return this;
   }

   public RegistryBuilder<T> disableSync() {
      this.sync = false;
      return this;
   }

   public RegistryBuilder<T> disableOverrides() {
      this.allowOverrides = false;
      return this;
   }

   public RegistryBuilder<T> allowModification() {
      this.allowModifications = true;
      return this;
   }

   RegistryBuilder<T> hasWrapper() {
      this.hasWrapper = true;
      return this;
   }

   public RegistryBuilder<T> legacyName(String name) {
      return this.legacyName(ResourceLocation.parse(name));
   }

   public RegistryBuilder<T> legacyName(ResourceLocation name) {
      this.legacyNames.add(name);
      return this;
   }

   RegistryBuilder<T> intrusiveHolderCallback(Function<T, Holder.Reference<T>> intrusiveHolderCallback) {
      this.intrusiveHolderCallback = intrusiveHolderCallback;
      return this;
   }

   public RegistryBuilder<T> hasTags() {
      this.hasWrapper();
      return this;
   }

   IForgeRegistry<T> create() {
      if (this.hasWrapper) {
         GameData.WrapperFactory<T> wrapper = GameData.createWrapperFactory(this.getDefault() != null);
         this.addCallback.addFirst(wrapper);
         this.createCallback.addFirst(wrapper);
      }

      return RegistryManager.ACTIVE.createRegistry(this.registryName, this);
   }

   @Nullable
   public IForgeRegistry.AddCallback<T> getAdd() {
      if (this.addCallback.isEmpty()) {
         return null;
      } else if (this.addCallback.size() == 1) {
         return this.addCallback.getFirst();
      } else {
         List<IForgeRegistry.AddCallback<T>> tmp = this.addCallback;
         return (owner, stage, id, key, obj, old) -> {
            for (IForgeRegistry.AddCallback<T> cb : tmp) {
               cb.onAdd(owner, stage, id, key, obj, old);
            }
         };
      }
   }

   @Nullable
   public IForgeRegistry.ClearCallback<T> getClear() {
      if (this.clearCallback.isEmpty()) {
         return null;
      } else if (this.clearCallback.size() == 1) {
         return this.clearCallback.getFirst();
      } else {
         List<IForgeRegistry.ClearCallback<T>> tmp = this.clearCallback;
         return (owner, stage) -> {
            for (IForgeRegistry.ClearCallback<T> cb : tmp) {
               cb.onClear(owner, stage);
            }
         };
      }
   }

   @Nullable
   public IForgeRegistry.CreateCallback<T> getCreate() {
      if (this.createCallback.isEmpty()) {
         return null;
      } else if (this.createCallback.size() == 1) {
         return this.createCallback.getFirst();
      } else {
         List<IForgeRegistry.CreateCallback<T>> tmp = this.createCallback;
         return (owner, stage) -> {
            for (IForgeRegistry.CreateCallback<T> cb : tmp) {
               cb.onCreate(owner, stage);
            }
         };
      }
   }

   @Nullable
   public IForgeRegistry.ValidateCallback<T> getValidate() {
      if (this.validateCallback.isEmpty()) {
         return null;
      } else if (this.validateCallback.size() == 1) {
         return this.validateCallback.getFirst();
      } else {
         List<IForgeRegistry.ValidateCallback<T>> tmp = this.validateCallback;
         return (owner, stage, id, key, obj) -> {
            for (IForgeRegistry.ValidateCallback<T> cb : tmp) {
               cb.onValidate(owner, stage, id, key, obj);
            }
         };
      }
   }

   @Nullable
   public IForgeRegistry.BakeCallback<T> getBake() {
      if (this.bakeCallback.isEmpty()) {
         return null;
      } else if (this.bakeCallback.size() == 1) {
         return this.bakeCallback.getFirst();
      } else {
         List<IForgeRegistry.BakeCallback<T>> tmp = this.bakeCallback;
         return (owner, stage) -> {
            for (IForgeRegistry.BakeCallback<T> cb : tmp) {
               cb.onBake(owner, stage);
            }
         };
      }
   }

   @Nullable
   public ResourceLocation getDefault() {
      return this.optionalDefaultKey;
   }

   public int getMinId() {
      return this.minId;
   }

   public int getMaxId() {
      return this.maxId;
   }

   public boolean getAllowOverrides() {
      return this.allowOverrides;
   }

   public boolean getAllowModifications() {
      return this.allowModifications;
   }

   @Nullable
   public IForgeRegistry.MissingFactory<T> getMissingFactory() {
      return this.missingFactory;
   }

   public boolean getSaveToDisc() {
      return this.saveToDisc;
   }

   public boolean getSync() {
      return this.sync;
   }

   public Set<ResourceLocation> getLegacyNames() {
      return this.legacyNames;
   }

   Function<T, Holder.Reference<T>> getIntrusiveHolderCallback() {
      return this.intrusiveHolderCallback;
   }

   boolean getHasWrapper() {
      return this.hasWrapper;
   }
}
