package net.minecraftforge.common.data;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.Tags;
import net.minecraftforge.registries.ForgeRegistries;

public final class ForgeItemTagsProvider extends ItemTagsProvider {
   public ForgeItemTagsProvider(
      PackOutput output,
      CompletableFuture<HolderLookup.Provider> lookupProvider,
      CompletableFuture<TagsProvider.TagLookup<Block>> blockTagProvider,
      ExistingFileHelper existingFileHelper
   ) {
      super(output, lookupProvider, blockTagProvider, "forge", existingFileHelper);
   }

   @Override
   public void addTags(HolderLookup.Provider lookupProvider) {
      this.copy(Tags.Blocks.BARRELS, Tags.Items.BARRELS);
      this.copy(Tags.Blocks.BARRELS_WOODEN, Tags.Items.BARRELS_WOODEN);
      this.tag(Tags.Items.BONES).add(Items.BONE);
      this.copy(Tags.Blocks.BOOKSHELVES, Tags.Items.BOOKSHELVES);
      this.copy(Tags.Blocks.CHESTS, Tags.Items.CHESTS);
      this.copy(Tags.Blocks.CHESTS_ENDER, Tags.Items.CHESTS_ENDER);
      this.copy(Tags.Blocks.CHESTS_TRAPPED, Tags.Items.CHESTS_TRAPPED);
      this.copy(Tags.Blocks.CHESTS_WOODEN, Tags.Items.CHESTS_WOODEN);
      this.copy(Tags.Blocks.COBBLESTONE, Tags.Items.COBBLESTONE);
      this.copy(Tags.Blocks.COBBLESTONE_NORMAL, Tags.Items.COBBLESTONE_NORMAL);
      this.copy(Tags.Blocks.COBBLESTONE_INFESTED, Tags.Items.COBBLESTONE_INFESTED);
      this.copy(Tags.Blocks.COBBLESTONE_MOSSY, Tags.Items.COBBLESTONE_MOSSY);
      this.copy(Tags.Blocks.COBBLESTONE_DEEPSLATE, Tags.Items.COBBLESTONE_DEEPSLATE);
      this.tag(Tags.Items.CROPS)
         .addTags(
            new TagKey[]{Tags.Items.CROPS_BEETROOT, Tags.Items.CROPS_CARROT, Tags.Items.CROPS_NETHER_WART, Tags.Items.CROPS_POTATO, Tags.Items.CROPS_WHEAT}
         );
      this.tag(Tags.Items.CROPS_BEETROOT).add(Items.BEETROOT);
      this.tag(Tags.Items.CROPS_CARROT).add(Items.CARROT);
      this.tag(Tags.Items.CROPS_NETHER_WART).add(Items.NETHER_WART);
      this.tag(Tags.Items.CROPS_POTATO).add(Items.POTATO);
      this.tag(Tags.Items.CROPS_WHEAT).add(Items.WHEAT);
      this.tag(Tags.Items.DUSTS).addTags(new TagKey[]{Tags.Items.DUSTS_GLOWSTONE, Tags.Items.DUSTS_PRISMARINE, Tags.Items.DUSTS_REDSTONE});
      this.tag(Tags.Items.DUSTS_GLOWSTONE).add(Items.GLOWSTONE_DUST);
      this.tag(Tags.Items.DUSTS_PRISMARINE).add(Items.PRISMARINE_SHARD);
      this.tag(Tags.Items.DUSTS_REDSTONE).add(Items.REDSTONE);
      this.addColored(xva$0 -> rec$.addTags(new TagKey[]{xva$0}), Tags.Items.DYES, "{color}_dye");
      this.tag(Tags.Items.EGGS).add(Items.EGG);
      this.tag(Tags.Items.ENCHANTING_FUELS).addTag(Tags.Items.GEMS_LAPIS);
      this.copy(Tags.Blocks.END_STONES, Tags.Items.END_STONES);
      this.tag(Tags.Items.ENDER_PEARLS).add(Items.ENDER_PEARL);
      this.tag(Tags.Items.FEATHERS).add(Items.FEATHER);
      this.copy(Tags.Blocks.FENCE_GATES, Tags.Items.FENCE_GATES);
      this.copy(Tags.Blocks.FENCE_GATES_WOODEN, Tags.Items.FENCE_GATES_WOODEN);
      this.copy(Tags.Blocks.FENCES, Tags.Items.FENCES);
      this.copy(Tags.Blocks.FENCES_NETHER_BRICK, Tags.Items.FENCES_NETHER_BRICK);
      this.copy(Tags.Blocks.FENCES_WOODEN, Tags.Items.FENCES_WOODEN);
      this.tag(Tags.Items.GEMS)
         .addTags(
            new TagKey[]{
               Tags.Items.GEMS_AMETHYST,
               Tags.Items.GEMS_DIAMOND,
               Tags.Items.GEMS_EMERALD,
               Tags.Items.GEMS_LAPIS,
               Tags.Items.GEMS_PRISMARINE,
               Tags.Items.GEMS_QUARTZ
            }
         );
      this.tag(Tags.Items.GEMS_AMETHYST).add(Items.AMETHYST_SHARD);
      this.tag(Tags.Items.GEMS_DIAMOND).add(Items.DIAMOND);
      this.tag(Tags.Items.GEMS_EMERALD).add(Items.EMERALD);
      this.tag(Tags.Items.GEMS_LAPIS).add(Items.LAPIS_LAZULI);
      this.tag(Tags.Items.GEMS_PRISMARINE).add(Items.PRISMARINE_CRYSTALS);
      this.tag(Tags.Items.GEMS_QUARTZ).add(Items.QUARTZ);
      this.copy(Tags.Blocks.GLASS, Tags.Items.GLASS);
      this.copy(Tags.Blocks.GLASS_TINTED, Tags.Items.GLASS_TINTED);
      this.copy(Tags.Blocks.GLASS_SILICA, Tags.Items.GLASS_SILICA);
      this.copyColored(Tags.Blocks.GLASS, Tags.Items.GLASS);
      this.copy(Tags.Blocks.GLASS_PANES, Tags.Items.GLASS_PANES);
      this.copyColored(Tags.Blocks.GLASS_PANES, Tags.Items.GLASS_PANES);
      this.copy(Tags.Blocks.GRAVEL, Tags.Items.GRAVEL);
      this.tag(Tags.Items.GUNPOWDER).add(Items.GUNPOWDER);
      this.tag(Tags.Items.HEADS)
         .add(Items.CREEPER_HEAD, Items.DRAGON_HEAD, Items.PLAYER_HEAD, Items.SKELETON_SKULL, Items.WITHER_SKELETON_SKULL, Items.ZOMBIE_HEAD);
      this.tag(Tags.Items.INGOTS)
         .addTags(
            new TagKey[]{
               Tags.Items.INGOTS_BRICK,
               Tags.Items.INGOTS_COPPER,
               Tags.Items.INGOTS_GOLD,
               Tags.Items.INGOTS_IRON,
               Tags.Items.INGOTS_NETHERITE,
               Tags.Items.INGOTS_NETHER_BRICK
            }
         );
      this.tag(Tags.Items.INGOTS_BRICK).add(Items.BRICK);
      this.tag(Tags.Items.INGOTS_COPPER).add(Items.COPPER_INGOT);
      this.tag(Tags.Items.INGOTS_GOLD).add(Items.GOLD_INGOT);
      this.tag(Tags.Items.INGOTS_IRON).add(Items.IRON_INGOT);
      this.tag(Tags.Items.INGOTS_NETHERITE).add(Items.NETHERITE_INGOT);
      this.tag(Tags.Items.INGOTS_NETHER_BRICK).add(Items.NETHER_BRICK);
      this.tag(Tags.Items.LEATHER).add(Items.LEATHER);
      this.tag(Tags.Items.MUSHROOMS).add(Items.BROWN_MUSHROOM, Items.RED_MUSHROOM);
      this.tag(Tags.Items.NETHER_STARS).add(Items.NETHER_STAR);
      this.copy(Tags.Blocks.NETHERRACK, Tags.Items.NETHERRACK);
      this.tag(Tags.Items.NUGGETS).addTags(new TagKey[]{Tags.Items.NUGGETS_IRON, Tags.Items.NUGGETS_GOLD});
      this.tag(Tags.Items.NUGGETS_IRON).add(Items.IRON_NUGGET);
      this.tag(Tags.Items.NUGGETS_GOLD).add(Items.GOLD_NUGGET);
      this.copy(Tags.Blocks.OBSIDIAN, Tags.Items.OBSIDIAN);
      this.copy(Tags.Blocks.ORE_BEARING_GROUND_DEEPSLATE, Tags.Items.ORE_BEARING_GROUND_DEEPSLATE);
      this.copy(Tags.Blocks.ORE_BEARING_GROUND_NETHERRACK, Tags.Items.ORE_BEARING_GROUND_NETHERRACK);
      this.copy(Tags.Blocks.ORE_BEARING_GROUND_STONE, Tags.Items.ORE_BEARING_GROUND_STONE);
      this.copy(Tags.Blocks.ORE_RATES_DENSE, Tags.Items.ORE_RATES_DENSE);
      this.copy(Tags.Blocks.ORE_RATES_SINGULAR, Tags.Items.ORE_RATES_SINGULAR);
      this.copy(Tags.Blocks.ORE_RATES_SPARSE, Tags.Items.ORE_RATES_SPARSE);
      this.copy(Tags.Blocks.ORES, Tags.Items.ORES);
      this.copy(Tags.Blocks.ORES_COAL, Tags.Items.ORES_COAL);
      this.copy(Tags.Blocks.ORES_COPPER, Tags.Items.ORES_COPPER);
      this.copy(Tags.Blocks.ORES_DIAMOND, Tags.Items.ORES_DIAMOND);
      this.copy(Tags.Blocks.ORES_EMERALD, Tags.Items.ORES_EMERALD);
      this.copy(Tags.Blocks.ORES_GOLD, Tags.Items.ORES_GOLD);
      this.copy(Tags.Blocks.ORES_IRON, Tags.Items.ORES_IRON);
      this.copy(Tags.Blocks.ORES_LAPIS, Tags.Items.ORES_LAPIS);
      this.copy(Tags.Blocks.ORES_QUARTZ, Tags.Items.ORES_QUARTZ);
      this.copy(Tags.Blocks.ORES_REDSTONE, Tags.Items.ORES_REDSTONE);
      this.copy(Tags.Blocks.ORES_NETHERITE_SCRAP, Tags.Items.ORES_NETHERITE_SCRAP);
      this.copy(Tags.Blocks.ORES_IN_GROUND_DEEPSLATE, Tags.Items.ORES_IN_GROUND_DEEPSLATE);
      this.copy(Tags.Blocks.ORES_IN_GROUND_NETHERRACK, Tags.Items.ORES_IN_GROUND_NETHERRACK);
      this.copy(Tags.Blocks.ORES_IN_GROUND_STONE, Tags.Items.ORES_IN_GROUND_STONE);
      this.tag(Tags.Items.RAW_MATERIALS).addTags(new TagKey[]{Tags.Items.RAW_MATERIALS_COPPER, Tags.Items.RAW_MATERIALS_GOLD, Tags.Items.RAW_MATERIALS_IRON});
      this.tag(Tags.Items.RAW_MATERIALS_COPPER).add(Items.RAW_COPPER);
      this.tag(Tags.Items.RAW_MATERIALS_GOLD).add(Items.RAW_GOLD);
      this.tag(Tags.Items.RAW_MATERIALS_IRON).add(Items.RAW_IRON);
      this.tag(Tags.Items.RODS).addTags(new TagKey[]{Tags.Items.RODS_BLAZE, Tags.Items.RODS_WOODEN});
      this.tag(Tags.Items.RODS_BLAZE).add(Items.BLAZE_ROD);
      this.tag(Tags.Items.RODS_WOODEN).add(Items.STICK);
      this.copy(Tags.Blocks.SAND, Tags.Items.SAND);
      this.copy(Tags.Blocks.SAND_COLORLESS, Tags.Items.SAND_COLORLESS);
      this.copy(Tags.Blocks.SAND_RED, Tags.Items.SAND_RED);
      this.copy(Tags.Blocks.SANDSTONE, Tags.Items.SANDSTONE);
      this.tag(Tags.Items.SEEDS).addTags(new TagKey[]{Tags.Items.SEEDS_BEETROOT, Tags.Items.SEEDS_MELON, Tags.Items.SEEDS_PUMPKIN, Tags.Items.SEEDS_WHEAT});
      this.tag(Tags.Items.SEEDS_BEETROOT).add(Items.BEETROOT_SEEDS);
      this.tag(Tags.Items.SEEDS_MELON).add(Items.MELON_SEEDS);
      this.tag(Tags.Items.SEEDS_PUMPKIN).add(Items.PUMPKIN_SEEDS);
      this.tag(Tags.Items.SEEDS_WHEAT).add(Items.WHEAT_SEEDS);
      this.tag(Tags.Items.SHEARS).add(Items.SHEARS);
      this.tag(Tags.Items.SLIMEBALLS).add(Items.SLIME_BALL);
      this.copy(Tags.Blocks.STAINED_GLASS, Tags.Items.STAINED_GLASS);
      this.copy(Tags.Blocks.STAINED_GLASS_PANES, Tags.Items.STAINED_GLASS_PANES);
      this.copy(Tags.Blocks.STONE, Tags.Items.STONE);
      this.copy(Tags.Blocks.STORAGE_BLOCKS, Tags.Items.STORAGE_BLOCKS);
      this.copy(Tags.Blocks.STORAGE_BLOCKS_AMETHYST, Tags.Items.STORAGE_BLOCKS_AMETHYST);
      this.copy(Tags.Blocks.STORAGE_BLOCKS_COAL, Tags.Items.STORAGE_BLOCKS_COAL);
      this.copy(Tags.Blocks.STORAGE_BLOCKS_COPPER, Tags.Items.STORAGE_BLOCKS_COPPER);
      this.copy(Tags.Blocks.STORAGE_BLOCKS_DIAMOND, Tags.Items.STORAGE_BLOCKS_DIAMOND);
      this.copy(Tags.Blocks.STORAGE_BLOCKS_EMERALD, Tags.Items.STORAGE_BLOCKS_EMERALD);
      this.copy(Tags.Blocks.STORAGE_BLOCKS_GOLD, Tags.Items.STORAGE_BLOCKS_GOLD);
      this.copy(Tags.Blocks.STORAGE_BLOCKS_IRON, Tags.Items.STORAGE_BLOCKS_IRON);
      this.copy(Tags.Blocks.STORAGE_BLOCKS_LAPIS, Tags.Items.STORAGE_BLOCKS_LAPIS);
      this.copy(Tags.Blocks.STORAGE_BLOCKS_QUARTZ, Tags.Items.STORAGE_BLOCKS_QUARTZ);
      this.copy(Tags.Blocks.STORAGE_BLOCKS_REDSTONE, Tags.Items.STORAGE_BLOCKS_REDSTONE);
      this.copy(Tags.Blocks.STORAGE_BLOCKS_RAW_COPPER, Tags.Items.STORAGE_BLOCKS_RAW_COPPER);
      this.copy(Tags.Blocks.STORAGE_BLOCKS_RAW_GOLD, Tags.Items.STORAGE_BLOCKS_RAW_GOLD);
      this.copy(Tags.Blocks.STORAGE_BLOCKS_RAW_IRON, Tags.Items.STORAGE_BLOCKS_RAW_IRON);
      this.copy(Tags.Blocks.STORAGE_BLOCKS_NETHERITE, Tags.Items.STORAGE_BLOCKS_NETHERITE);
      this.tag(Tags.Items.STRING).add(Items.STRING);
      this.tag(Tags.Items.TOOLS_SHIELDS).add(Items.SHIELD);
      this.tag(Tags.Items.TOOLS_BOWS).add(Items.BOW);
      this.tag(Tags.Items.TOOLS_CROSSBOWS).add(Items.CROSSBOW);
      this.tag(Tags.Items.TOOLS_FISHING_RODS).add(Items.FISHING_ROD);
      this.tag(Tags.Items.TOOLS_TRIDENTS).add(Items.TRIDENT);
      this.tag(Tags.Items.TOOLS)
         .addTags(new TagKey[]{ItemTags.SWORDS, ItemTags.AXES, ItemTags.PICKAXES, ItemTags.SHOVELS, ItemTags.HOES})
         .addTags(
            new TagKey[]{Tags.Items.TOOLS_SHIELDS, Tags.Items.TOOLS_BOWS, Tags.Items.TOOLS_CROSSBOWS, Tags.Items.TOOLS_FISHING_RODS, Tags.Items.TOOLS_TRIDENTS}
         );
      this.tag(Tags.Items.ARMORS_HELMETS)
         .add(
            Items.LEATHER_HELMET,
            Items.TURTLE_HELMET,
            Items.CHAINMAIL_HELMET,
            Items.IRON_HELMET,
            Items.GOLDEN_HELMET,
            Items.DIAMOND_HELMET,
            Items.NETHERITE_HELMET
         );
      this.tag(Tags.Items.ARMORS_CHESTPLATES)
         .add(
            Items.LEATHER_CHESTPLATE,
            Items.CHAINMAIL_CHESTPLATE,
            Items.IRON_CHESTPLATE,
            Items.GOLDEN_CHESTPLATE,
            Items.DIAMOND_CHESTPLATE,
            Items.NETHERITE_CHESTPLATE
         );
      this.tag(Tags.Items.ARMORS_LEGGINGS)
         .add(Items.LEATHER_LEGGINGS, Items.CHAINMAIL_LEGGINGS, Items.IRON_LEGGINGS, Items.GOLDEN_LEGGINGS, Items.DIAMOND_LEGGINGS, Items.NETHERITE_LEGGINGS);
      this.tag(Tags.Items.ARMORS_BOOTS)
         .add(Items.LEATHER_BOOTS, Items.CHAINMAIL_BOOTS, Items.IRON_BOOTS, Items.GOLDEN_BOOTS, Items.DIAMOND_BOOTS, Items.NETHERITE_BOOTS);
      this.tag(Tags.Items.ARMORS)
         .addTags(new TagKey[]{Tags.Items.ARMORS_HELMETS, Tags.Items.ARMORS_CHESTPLATES, Tags.Items.ARMORS_LEGGINGS, Tags.Items.ARMORS_BOOTS});
   }

   private void addColored(Consumer<TagKey<Item>> consumer, TagKey<Item> group, String pattern) {
      String prefix = group.location().getPath().toUpperCase(Locale.ENGLISH) + "_";

      for (DyeColor color : DyeColor.values()) {
         ResourceLocation key = ResourceLocation.fromNamespaceAndPath("minecraft", pattern.replace("{color}", color.getName()));
         TagKey<Item> tag = this.getForgeItemTag(prefix + color.getName());
         Item item = ForgeRegistries.ITEMS.getValue(key);
         if (item == null || item == Items.AIR) {
            throw new IllegalStateException("Unknown vanilla item: " + key.toString());
         }

         this.tag(tag).add(item);
         consumer.accept(tag);
      }
   }

   private void copyColored(TagKey<Block> blockGroup, TagKey<Item> itemGroup) {
      String blockPre = blockGroup.location().getPath().toUpperCase(Locale.ENGLISH) + "_";
      String itemPre = itemGroup.location().getPath().toUpperCase(Locale.ENGLISH) + "_";

      for (DyeColor color : DyeColor.values()) {
         TagKey<Block> from = this.getForgeBlockTag(blockPre + color.getName());
         TagKey<Item> to = this.getForgeItemTag(itemPre + color.getName());
         this.copy(from, to);
      }

      this.copy(this.getForgeBlockTag(blockPre + "colorless"), this.getForgeItemTag(itemPre + "colorless"));
   }

   private TagKey<Block> getForgeBlockTag(String name) {
      try {
         name = name.toUpperCase(Locale.ENGLISH);
         return (TagKey<Block>)Tags.Blocks.class.getDeclaredField(name).get(null);
      } catch (IllegalAccessException | NoSuchFieldException | SecurityException | IllegalArgumentException var3) {
         throw new IllegalStateException(Tags.Blocks.class.getName() + " is missing tag name: " + name);
      }
   }

   private TagKey<Item> getForgeItemTag(String name) {
      try {
         name = name.toUpperCase(Locale.ENGLISH);
         return (TagKey<Item>)Tags.Items.class.getDeclaredField(name).get(null);
      } catch (IllegalAccessException | NoSuchFieldException | SecurityException | IllegalArgumentException var3) {
         throw new IllegalStateException(Tags.Items.class.getName() + " is missing tag name: " + name);
      }
   }

   @Override
   public String getName() {
      return "Forge Item Tags";
   }
}
