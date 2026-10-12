package net.minecraft.tags;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.extensions.IForgeRawTagBuilder;

public class TagBuilder implements IForgeRawTagBuilder {
   private final List<TagEntry> entries = new ArrayList<>();
   private final List<TagEntry> removeEntries = new ArrayList<>();
   private boolean replace = false;

   public static TagBuilder create() {
      return new TagBuilder();
   }

   public List<TagEntry> build() {
      return List.copyOf(this.entries);
   }

   public TagBuilder add(TagEntry pEntry) {
      this.entries.add(pEntry);
      return this;
   }

   public TagBuilder addElement(ResourceLocation pElementLocation) {
      return this.add(TagEntry.element(pElementLocation));
   }

   public TagBuilder addOptionalElement(ResourceLocation pElementLocation) {
      return this.add(TagEntry.optionalElement(pElementLocation));
   }

   public TagBuilder addTag(ResourceLocation pTagLocation) {
      return this.add(TagEntry.tag(pTagLocation));
   }

   public TagBuilder addOptionalTag(ResourceLocation pTagLocation) {
      return this.add(TagEntry.optionalTag(pTagLocation));
   }

   public Stream<TagEntry> getRemoveEntries() {
      return this.removeEntries.stream();
   }

   public TagBuilder remove(TagEntry entry) {
      this.removeEntries.add(entry);
      return this;
   }

   public TagBuilder replace(boolean value) {
      this.replace = value;
      return this;
   }

   public boolean isReplace() {
      return this.replace;
   }
}
