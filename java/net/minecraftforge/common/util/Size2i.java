package net.minecraftforge.common.util;

import com.google.common.base.Preconditions;

public record Size2i(int width, int height) {
   public Size2i(int width, int height) {
      Preconditions.checkArgument(width >= 0, "width must be greater or equal 0");
      Preconditions.checkArgument(height >= 0, "height must be greater or equal 0");
      this.width = width;
      this.height = height;
   }
}
