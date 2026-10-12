package net.minecraft.util.datafix.fixes;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.TagParser;
import net.minecraft.util.Mth;
import net.minecraft.util.datafix.schemas.NamespacedSchema;
import org.slf4j.Logger;

public class ParticleUnflatteningFix extends DataFix {
   private static final Logger LOGGER = LogUtils.getLogger();

   public ParticleUnflatteningFix(Schema pOutputSchema) {
      super(pOutputSchema, true);
   }

   protected TypeRewriteRule makeRule() {
      Type<?> $$0 = this.getInputSchema().getType(References.PARTICLE);
      Type<?> $$1 = this.getOutputSchema().getType(References.PARTICLE);
      return this.writeFixAndRead("ParticleUnflatteningFix", $$0, $$1, this::fix);
   }

   private <T> Dynamic<T> fix(Dynamic<T> p_329773_) {
      Optional<String> $$1 = p_329773_.asString().result();
      if ($$1.isEmpty()) {
         return p_329773_;
      } else {
         String $$2 = $$1.get();
         String[] $$3 = $$2.split(" ", 2);
         String $$4 = NamespacedSchema.ensureNamespaced($$3[0]);
         Dynamic<T> $$5 = p_329773_.createMap(Map.of(p_329773_.createString("type"), p_329773_.createString($$4)));

         return switch ($$4) {
            case "minecraft:item" -> $$3.length > 1 ? this.updateItem($$5, $$3[1]) : $$5;
            case "minecraft:block", "minecraft:block_marker", "minecraft:falling_dust", "minecraft:dust_pillar" -> $$3.length > 1
            ? this.updateBlock($$5, $$3[1])
            : $$5;
            case "minecraft:dust" -> $$3.length > 1 ? this.updateDust($$5, $$3[1]) : $$5;
            case "minecraft:dust_color_transition" -> $$3.length > 1 ? this.updateDustTransition($$5, $$3[1]) : $$5;
            case "minecraft:sculk_charge" -> $$3.length > 1 ? this.updateSculkCharge($$5, $$3[1]) : $$5;
            case "minecraft:vibration" -> $$3.length > 1 ? this.updateVibration($$5, $$3[1]) : $$5;
            case "minecraft:shriek" -> $$3.length > 1 ? this.updateShriek($$5, $$3[1]) : $$5;
            default -> $$5;
         };
      }
   }

   private <T> Dynamic<T> updateItem(Dynamic<T> pTag, String pItem) {
      int $$2 = pItem.indexOf("{");
      Dynamic<T> $$3 = pTag.createMap(Map.of(pTag.createString("Count"), pTag.createInt(1)));
      if ($$2 == -1) {
         $$3 = $$3.set("id", pTag.createString(pItem));
      } else {
         $$3 = $$3.set("id", pTag.createString(pItem.substring(0, $$2)));
         CompoundTag $$4 = parseTag(pItem.substring($$2));
         if ($$4 != null) {
            $$3 = $$3.set("tag", new Dynamic(NbtOps.INSTANCE, $$4).convert(pTag.getOps()));
         }
      }

      return pTag.set("item", $$3);
   }

   @Nullable
   private static CompoundTag parseTag(String pTag) {
      try {
         return TagParser.parseTag(pTag);
      } catch (Exception var2) {
         LOGGER.warn("Failed to parse tag: {}", pTag, var2);
         return null;
      }
   }

   private <T> Dynamic<T> updateBlock(Dynamic<T> pTag, String pBlock) {
      int $$2 = pBlock.indexOf("[");
      Dynamic<T> $$3 = pTag.emptyMap();
      if ($$2 == -1) {
         $$3 = $$3.set("Name", pTag.createString(NamespacedSchema.ensureNamespaced(pBlock)));
      } else {
         $$3 = $$3.set("Name", pTag.createString(NamespacedSchema.ensureNamespaced(pBlock.substring(0, $$2))));
         Map<Dynamic<T>, Dynamic<T>> $$4 = parseBlockProperties(pTag, pBlock.substring($$2));
         if (!$$4.isEmpty()) {
            $$3 = $$3.set("Properties", pTag.createMap($$4));
         }
      }

      return pTag.set("block_state", $$3);
   }

   private static <T> Map<Dynamic<T>, Dynamic<T>> parseBlockProperties(Dynamic<T> pTag, String pProperties) {
      try {
         Map<Dynamic<T>, Dynamic<T>> $$2 = new HashMap<>();
         StringReader $$3 = new StringReader(pProperties);
         $$3.expect('[');
         $$3.skipWhitespace();

         while ($$3.canRead() && $$3.peek() != ']') {
            $$3.skipWhitespace();
            String $$4 = $$3.readString();
            $$3.skipWhitespace();
            $$3.expect('=');
            $$3.skipWhitespace();
            String $$5 = $$3.readString();
            $$3.skipWhitespace();
            $$2.put(pTag.createString($$4), pTag.createString($$5));
            if ($$3.canRead()) {
               if ($$3.peek() != ',') {
                  break;
               }

               $$3.skip();
            }
         }

         $$3.expect(']');
         return $$2;
      } catch (Exception var6) {
         LOGGER.warn("Failed to parse block properties: {}", pProperties, var6);
         return Map.of();
      }
   }

   private static <T> Dynamic<T> readVector(Dynamic<T> pTag, StringReader pReader) throws CommandSyntaxException {
      float $$2 = pReader.readFloat();
      pReader.expect(' ');
      float $$3 = pReader.readFloat();
      pReader.expect(' ');
      float $$4 = pReader.readFloat();
      return pTag.createList(Stream.of($$2, $$3, $$4).map(pTag::createFloat));
   }

   private <T> Dynamic<T> updateDust(Dynamic<T> pTag, String pOptions) {
      try {
         StringReader $$2 = new StringReader(pOptions);
         Dynamic<T> $$3 = readVector(pTag, $$2);
         $$2.expect(' ');
         float $$4 = $$2.readFloat();
         return pTag.set("color", $$3).set("scale", pTag.createFloat($$4));
      } catch (Exception var6) {
         LOGGER.warn("Failed to parse particle options: {}", pOptions, var6);
         return pTag;
      }
   }

   private <T> Dynamic<T> updateDustTransition(Dynamic<T> pTag, String pOptions) {
      try {
         StringReader $$2 = new StringReader(pOptions);
         Dynamic<T> $$3 = readVector(pTag, $$2);
         $$2.expect(' ');
         float $$4 = $$2.readFloat();
         $$2.expect(' ');
         Dynamic<T> $$5 = readVector(pTag, $$2);
         return pTag.set("from_color", $$3).set("to_color", $$5).set("scale", pTag.createFloat($$4));
      } catch (Exception var7) {
         LOGGER.warn("Failed to parse particle options: {}", pOptions, var7);
         return pTag;
      }
   }

   private <T> Dynamic<T> updateSculkCharge(Dynamic<T> pTag, String pOptions) {
      try {
         StringReader $$2 = new StringReader(pOptions);
         float $$3 = $$2.readFloat();
         return pTag.set("roll", pTag.createFloat($$3));
      } catch (Exception var5) {
         LOGGER.warn("Failed to parse particle options: {}", pOptions, var5);
         return pTag;
      }
   }

   private <T> Dynamic<T> updateVibration(Dynamic<T> pTag, String pOptions) {
      try {
         StringReader $$2 = new StringReader(pOptions);
         float $$3 = (float)$$2.readDouble();
         $$2.expect(' ');
         float $$4 = (float)$$2.readDouble();
         $$2.expect(' ');
         float $$5 = (float)$$2.readDouble();
         $$2.expect(' ');
         int $$6 = $$2.readInt();
         Dynamic<T> $$7 = pTag.createIntList(IntStream.of(Mth.floor($$3), Mth.floor($$4), Mth.floor($$5)));
         Dynamic<T> $$8 = pTag.createMap(Map.of(pTag.createString("type"), pTag.createString("minecraft:block"), pTag.createString("pos"), $$7));
         return pTag.set("destination", $$8).set("arrival_in_ticks", pTag.createInt($$6));
      } catch (Exception var10) {
         LOGGER.warn("Failed to parse particle options: {}", pOptions, var10);
         return pTag;
      }
   }

   private <T> Dynamic<T> updateShriek(Dynamic<T> pTag, String pOptions) {
      try {
         StringReader $$2 = new StringReader(pOptions);
         int $$3 = $$2.readInt();
         return pTag.set("delay", pTag.createInt($$3));
      } catch (Exception var5) {
         LOGGER.warn("Failed to parse particle options: {}", pOptions, var5);
         return pTag;
      }
   }
}
