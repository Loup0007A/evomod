package net.minecraft.client.resources.sounds;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.Validate;

@OnlyIn(Dist.CLIENT)
public class SoundEventRegistrationSerializer implements JsonDeserializer<SoundEventRegistration> {
   private static final FloatProvider DEFAULT_FLOAT = ConstantFloat.of(1.0F);

   public SoundEventRegistration deserialize(JsonElement p_119827_, Type p_119828_, JsonDeserializationContext p_119829_) throws JsonParseException {
      JsonObject $$3 = GsonHelper.convertToJsonObject(p_119827_, "entry");
      boolean $$4 = GsonHelper.getAsBoolean($$3, "replace", false);
      String $$5 = GsonHelper.getAsString($$3, "subtitle", null);
      List<Sound> $$6 = this.getSounds($$3);
      return new SoundEventRegistration($$6, $$4, $$5);
   }

   private List<Sound> getSounds(JsonObject pObject) {
      List<Sound> $$1 = Lists.newArrayList();
      if (pObject.has("sounds")) {
         JsonArray $$2 = GsonHelper.getAsJsonArray(pObject, "sounds");

         for (int $$3 = 0; $$3 < $$2.size(); $$3++) {
            JsonElement $$4 = $$2.get($$3);
            if (GsonHelper.isStringValue($$4)) {
               ResourceLocation $$5 = ResourceLocation.parse(GsonHelper.convertToString($$4, "sound"));
               $$1.add(new Sound($$5, DEFAULT_FLOAT, DEFAULT_FLOAT, 1, Sound.Type.FILE, false, false, 16));
            } else {
               $$1.add(this.getSound(GsonHelper.convertToJsonObject($$4, "sound")));
            }
         }
      }

      return $$1;
   }

   private Sound getSound(JsonObject pObject) {
      ResourceLocation $$1 = ResourceLocation.parse(GsonHelper.getAsString(pObject, "name"));
      Sound.Type $$2 = this.getType(pObject, Sound.Type.FILE);
      float $$3 = GsonHelper.getAsFloat(pObject, "volume", 1.0F);
      Validate.isTrue($$3 > 0.0F, "Invalid volume", new Object[0]);
      float $$4 = GsonHelper.getAsFloat(pObject, "pitch", 1.0F);
      Validate.isTrue($$4 > 0.0F, "Invalid pitch", new Object[0]);
      int $$5 = GsonHelper.getAsInt(pObject, "weight", 1);
      Validate.isTrue($$5 > 0, "Invalid weight", new Object[0]);
      boolean $$6 = GsonHelper.getAsBoolean(pObject, "preload", false);
      boolean $$7 = GsonHelper.getAsBoolean(pObject, "stream", false);
      int $$8 = GsonHelper.getAsInt(pObject, "attenuation_distance", 16);
      return new Sound($$1, ConstantFloat.of($$3), ConstantFloat.of($$4), $$5, $$2, $$7, $$6, $$8);
   }

   private Sound.Type getType(JsonObject pObject, Sound.Type pDefaultValue) {
      Sound.Type $$2 = pDefaultValue;
      if (pObject.has("type")) {
         $$2 = Sound.Type.getByName(GsonHelper.getAsString(pObject, "type"));
         Validate.notNull($$2, "Invalid type", new Object[0]);
      }

      return $$2;
   }
}
