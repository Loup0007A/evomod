package net.minecraft.client;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.Util;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.IForgeKeyMapping;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyMappingLookup;
import net.minecraftforge.client.settings.KeyModifier;
import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class KeyMapping implements Comparable<KeyMapping>, IForgeKeyMapping {
   private static final Map<String, KeyMapping> ALL = Maps.newHashMap();
   private static final KeyMappingLookup MAP = new KeyMappingLookup();
   private static final Set<String> CATEGORIES = Sets.newHashSet();
   public static final String CATEGORY_MOVEMENT = "key.categories.movement";
   public static final String CATEGORY_MISC = "key.categories.misc";
   public static final String CATEGORY_MULTIPLAYER = "key.categories.multiplayer";
   public static final String CATEGORY_GAMEPLAY = "key.categories.gameplay";
   public static final String CATEGORY_INVENTORY = "key.categories.inventory";
   public static final String CATEGORY_INTERFACE = "key.categories.ui";
   public static final String CATEGORY_CREATIVE = "key.categories.creative";
   private static final Map<String, Integer> CATEGORY_SORT_ORDER = Util.make(Maps.newHashMap(), p_90845_ -> {
      p_90845_.put("key.categories.movement", 1);
      p_90845_.put("key.categories.gameplay", 2);
      p_90845_.put("key.categories.inventory", 3);
      p_90845_.put("key.categories.creative", 4);
      p_90845_.put("key.categories.multiplayer", 5);
      p_90845_.put("key.categories.ui", 6);
      p_90845_.put("key.categories.misc", 7);
   });
   private final String name;
   private final InputConstants.Key defaultKey;
   private final String category;
   private InputConstants.Key key;
   boolean isDown;
   private int clickCount;
   private KeyModifier keyModifierDefault = KeyModifier.NONE;
   private KeyModifier keyModifier = KeyModifier.NONE;
   private IKeyConflictContext keyConflictContext = KeyConflictContext.UNIVERSAL;

   public static void click(InputConstants.Key pKey) {
      for (KeyMapping keymapping : MAP.getAll(pKey)) {
         if (keymapping != null) {
            keymapping.clickCount++;
         }
      }
   }

   public static void set(InputConstants.Key pKey, boolean pHeld) {
      for (KeyMapping keymapping : MAP.getAll(pKey)) {
         if (keymapping != null) {
            keymapping.setDown(pHeld);
         }
      }
   }

   public static void setAll() {
      for (KeyMapping keymapping : ALL.values()) {
         if (keymapping.key.getType() == InputConstants.Type.KEYSYM && keymapping.key.getValue() != InputConstants.UNKNOWN.getValue()) {
            keymapping.setDown(InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), keymapping.key.getValue()));
         }
      }
   }

   public static void releaseAll() {
      for (KeyMapping keymapping : ALL.values()) {
         keymapping.release();
      }
   }

   public static void resetToggleKeys() {
      for (KeyMapping keymapping : ALL.values()) {
         if (keymapping instanceof ToggleKeyMapping togglekeymapping) {
            togglekeymapping.reset();
         }
      }
   }

   public static void resetMapping() {
      MAP.clear();

      for (KeyMapping keymapping : ALL.values()) {
         MAP.put(keymapping.key, keymapping);
      }
   }

   public KeyMapping(String pName, int pKeyCode, String pCategory) {
      this(pName, InputConstants.Type.KEYSYM, pKeyCode, pCategory);
   }

   public KeyMapping(String pName, InputConstants.Type pType, int pKeyCode, String pCategory) {
      this.name = pName;
      this.key = pType.getOrCreate(pKeyCode);
      this.defaultKey = this.key;
      this.category = pCategory;
      ALL.put(pName, this);
      MAP.put(this.key, this);
      CATEGORIES.add(pCategory);
   }

   public boolean isDown() {
      return this.isDown && this.isConflictContextAndModifierActive();
   }

   public String getCategory() {
      return this.category;
   }

   public boolean consumeClick() {
      if (this.clickCount == 0) {
         return false;
      } else {
         this.clickCount--;
         return true;
      }
   }

   private void release() {
      this.clickCount = 0;
      this.setDown(false);
   }

   public String getName() {
      return this.name;
   }

   public InputConstants.Key getDefaultKey() {
      return this.defaultKey;
   }

   public void setKey(InputConstants.Key pKey) {
      this.key = pKey;
   }

   public int compareTo(KeyMapping p_90841_) {
      return this.category.equals(p_90841_.category) ? I18n.get(this.name).compareTo(I18n.get(p_90841_.name)) : compareSort(this.category, p_90841_.category);
   }

   private static int compareSort(String c1, String c2) {
      Integer o1 = CATEGORY_SORT_ORDER.get(c1);
      Integer o2 = CATEGORY_SORT_ORDER.get(c2);
      if (o1 == null && o2 != null) {
         return 1;
      } else if (o1 != null && o2 == null) {
         return -1;
      } else {
         return o1 == null && o2 == null ? I18n.get(c1).compareTo(I18n.get(c2)) : o1.compareTo(o2);
      }
   }

   public static Supplier<Component> createNameSupplier(String pKey) {
      KeyMapping keymapping = ALL.get(pKey);
      return keymapping == null ? () -> Component.translatable(pKey) : keymapping::getTranslatedKeyMessage;
   }

   public boolean same(KeyMapping pBinding) {
      if (this.getKeyConflictContext().conflicts(pBinding.getKeyConflictContext()) || pBinding.getKeyConflictContext().conflicts(this.getKeyConflictContext())) {
         KeyModifier keyModifier = this.getKeyModifier();
         KeyModifier otherKeyModifier = pBinding.getKeyModifier();
         if (keyModifier.matches(pBinding.getKey()) || otherKeyModifier.matches(this.getKey())) {
            return true;
         }

         if (this.getKey().equals(pBinding.getKey())) {
            return keyModifier == otherKeyModifier
               || this.getKeyConflictContext().conflicts(KeyConflictContext.IN_GAME)
                  && (keyModifier == KeyModifier.NONE || otherKeyModifier == KeyModifier.NONE);
         }
      }

      return this.key.equals(pBinding.key);
   }

   public boolean isUnbound() {
      return this.key.equals(InputConstants.UNKNOWN);
   }

   public boolean matches(int pKeysym, int pScancode) {
      return pKeysym == InputConstants.UNKNOWN.getValue()
         ? this.key.getType() == InputConstants.Type.SCANCODE && this.key.getValue() == pScancode
         : this.key.getType() == InputConstants.Type.KEYSYM && this.key.getValue() == pKeysym;
   }

   public boolean matchesMouse(int pKey) {
      return this.key.getType() == InputConstants.Type.MOUSE && this.key.getValue() == pKey;
   }

   public Component getTranslatedKeyMessage() {
      return this.getKeyModifier().getCombinedName(this.key, () -> this.key.getDisplayName());
   }

   public boolean isDefault() {
      return this.key.equals(this.defaultKey) && this.getKeyModifier() == this.getDefaultKeyModifier();
   }

   public String saveString() {
      return this.key.getName();
   }

   public void setDown(boolean pValue) {
      this.isDown = pValue;
   }

   public KeyMapping(String description, IKeyConflictContext keyConflictContext, InputConstants.Type inputType, int keyCode, String category) {
      this(description, keyConflictContext, inputType.getOrCreate(keyCode), category);
   }

   public KeyMapping(String description, IKeyConflictContext keyConflictContext, InputConstants.Key keyCode, String category) {
      this(description, keyConflictContext, KeyModifier.NONE, keyCode, category);
   }

   public KeyMapping(
      String description, IKeyConflictContext keyConflictContext, KeyModifier keyModifier, InputConstants.Type inputType, int keyCode, String category
   ) {
      this(description, keyConflictContext, keyModifier, inputType.getOrCreate(keyCode), category);
   }

   public KeyMapping(String description, IKeyConflictContext keyConflictContext, KeyModifier keyModifier, InputConstants.Key keyCode, String category) {
      this.name = description;
      this.key = keyCode;
      this.defaultKey = keyCode;
      this.category = category;
      this.keyConflictContext = keyConflictContext;
      this.keyModifier = keyModifier;
      this.keyModifierDefault = keyModifier;
      if (this.keyModifier.matches(keyCode)) {
         this.keyModifier = KeyModifier.NONE;
      }

      ALL.put(description, this);
      MAP.put(keyCode, this);
      CATEGORIES.add(category);
   }

   @Override
   public InputConstants.Key getKey() {
      return this.key;
   }

   @Override
   public void setKeyConflictContext(IKeyConflictContext keyConflictContext) {
      this.keyConflictContext = keyConflictContext;
   }

   @Override
   public IKeyConflictContext getKeyConflictContext() {
      return this.keyConflictContext;
   }

   @Override
   public KeyModifier getDefaultKeyModifier() {
      return this.keyModifierDefault;
   }

   @Override
   public KeyModifier getKeyModifier() {
      return this.keyModifier;
   }

   @Override
   public void setKeyModifierAndCode(@Nullable KeyModifier keyModifier, InputConstants.Key keyCode) {
      MAP.remove(this);
      if (keyModifier == null) {
         keyModifier = KeyModifier.getModifier(this.key);
      }

      if (keyModifier == null || keyCode == InputConstants.UNKNOWN || KeyModifier.isKeyCodeModifier(keyCode)) {
         keyModifier = KeyModifier.NONE;
      }

      this.key = keyCode;
      this.keyModifier = keyModifier;
      MAP.put(keyCode, this);
   }
}
