package net.minecraft.data.structures;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import net.minecraft.DetectedVersion;
import net.minecraft.SharedConstants;
import net.minecraft.data.CachedOutput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.Bootstrap;

public class SnbtDatafixer {
   public static void main(String[] pArgs) throws IOException {
      SharedConstants.setVersion(DetectedVersion.BUILT_IN);
      Bootstrap.bootStrap();

      for (String $$1 : pArgs) {
         updateInDirectory($$1);
      }
   }

   private static void updateInDirectory(String pPath) throws IOException {
      try (Stream<Path> $$1 = Files.walk(Paths.get(pPath))) {
         $$1.filter(p_300817_ -> p_300817_.toString().endsWith(".snbt")).forEach(p_298886_ -> {
            try {
               String $$1x = Files.readString(p_298886_);
               CompoundTag $$2 = NbtUtils.snbtToStructure($$1x);
               CompoundTag $$3 = StructureUpdater.update(p_298886_.toString(), $$2);
               NbtToSnbt.writeSnbt(CachedOutput.NO_CACHE, p_298886_, NbtUtils.structureToSnbt($$3));
            } catch (IOException | CommandSyntaxException var4) {
               throw new RuntimeException(var4);
            }
         });
      }
   }
}
