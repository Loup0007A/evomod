package net.minecraftforge.client.extensions;

import com.mojang.blaze3d.vertex.VertexFormatElement;
import java.util.List;

public interface IForgeVertexFormat {
   List<VertexFormatElement> getElements();

   default boolean hasPosition() {
      return this.getElements().stream().anyMatch(e -> e.usage() == VertexFormatElement.Usage.POSITION);
   }

   default boolean hasNormal() {
      return this.getElements().stream().anyMatch(e -> e.usage() == VertexFormatElement.Usage.NORMAL);
   }

   default boolean hasColor() {
      return this.getElements().stream().anyMatch(e -> e.usage() == VertexFormatElement.Usage.COLOR);
   }

   default boolean hasUV(int which) {
      for (VertexFormatElement e : this.getElements()) {
         if (e.usage() == VertexFormatElement.Usage.UV && e.index() == which) {
            return true;
         }
      }

      return false;
   }
}
