package net.minecraft.util.datafix;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.DataFixerBuilder;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.DataFixerBuilder.Result;
import com.mojang.datafixers.schemas.Schema;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.util.datafix.fixes.AbstractArrowPickupFix;
import net.minecraft.util.datafix.fixes.AddFlagIfNotPresentFix;
import net.minecraft.util.datafix.fixes.AddNewChoices;
import net.minecraft.util.datafix.fixes.AdvancementsFix;
import net.minecraft.util.datafix.fixes.AdvancementsRenameFix;
import net.minecraft.util.datafix.fixes.AreaEffectCloudPotionFix;
import net.minecraft.util.datafix.fixes.AttributeModifierIdFix;
import net.minecraft.util.datafix.fixes.AttributesRename;
import net.minecraft.util.datafix.fixes.BannerEntityCustomNameToOverrideComponentFix;
import net.minecraft.util.datafix.fixes.BannerPatternFormatFix;
import net.minecraft.util.datafix.fixes.BedItemColorFix;
import net.minecraft.util.datafix.fixes.BeehiveFieldRenameFix;
import net.minecraft.util.datafix.fixes.BiomeFix;
import net.minecraft.util.datafix.fixes.BitStorageAlignFix;
import net.minecraft.util.datafix.fixes.BlendingDataFix;
import net.minecraft.util.datafix.fixes.BlendingDataRemoveFromNetherEndFix;
import net.minecraft.util.datafix.fixes.BlockEntityBannerColorFix;
import net.minecraft.util.datafix.fixes.BlockEntityBlockStateFix;
import net.minecraft.util.datafix.fixes.BlockEntityCustomNameToComponentFix;
import net.minecraft.util.datafix.fixes.BlockEntityIdFix;
import net.minecraft.util.datafix.fixes.BlockEntityJukeboxFix;
import net.minecraft.util.datafix.fixes.BlockEntityKeepPacked;
import net.minecraft.util.datafix.fixes.BlockEntityRenameFix;
import net.minecraft.util.datafix.fixes.BlockEntityShulkerBoxColorFix;
import net.minecraft.util.datafix.fixes.BlockEntitySignDoubleSidedEditableTextFix;
import net.minecraft.util.datafix.fixes.BlockEntitySignTextStrictJsonFix;
import net.minecraft.util.datafix.fixes.BlockEntityUUIDFix;
import net.minecraft.util.datafix.fixes.BlockNameFlatteningFix;
import net.minecraft.util.datafix.fixes.BlockPosFormatAndRenamesFix;
import net.minecraft.util.datafix.fixes.BlockRenameFix;
import net.minecraft.util.datafix.fixes.BlockStateStructureTemplateFix;
import net.minecraft.util.datafix.fixes.CatTypeFix;
import net.minecraft.util.datafix.fixes.CauldronRenameFix;
import net.minecraft.util.datafix.fixes.CavesAndCliffsRenames;
import net.minecraft.util.datafix.fixes.ChestedHorsesInventoryZeroIndexingFix;
import net.minecraft.util.datafix.fixes.ChunkBedBlockEntityInjecterFix;
import net.minecraft.util.datafix.fixes.ChunkBiomeFix;
import net.minecraft.util.datafix.fixes.ChunkDeleteIgnoredLightDataFix;
import net.minecraft.util.datafix.fixes.ChunkDeleteLightFix;
import net.minecraft.util.datafix.fixes.ChunkHeightAndBiomeFix;
import net.minecraft.util.datafix.fixes.ChunkLightRemoveFix;
import net.minecraft.util.datafix.fixes.ChunkPalettedStorageFix;
import net.minecraft.util.datafix.fixes.ChunkProtoTickListFix;
import net.minecraft.util.datafix.fixes.ChunkRenamesFix;
import net.minecraft.util.datafix.fixes.ChunkStatusFix;
import net.minecraft.util.datafix.fixes.ChunkStatusFix2;
import net.minecraft.util.datafix.fixes.ChunkStructuresTemplateRenameFix;
import net.minecraft.util.datafix.fixes.ChunkToProtochunkFix;
import net.minecraft.util.datafix.fixes.ColorlessShulkerEntityFix;
import net.minecraft.util.datafix.fixes.CriteriaRenameFix;
import net.minecraft.util.datafix.fixes.DecoratedPotFieldRenameFix;
import net.minecraft.util.datafix.fixes.DropInvalidSignDataFix;
import net.minecraft.util.datafix.fixes.DyeItemRenameFix;
import net.minecraft.util.datafix.fixes.EffectDurationFix;
import net.minecraft.util.datafix.fixes.EmptyItemInHotbarFix;
import net.minecraft.util.datafix.fixes.EmptyItemInVillagerTradeFix;
import net.minecraft.util.datafix.fixes.EntityArmorStandSilentFix;
import net.minecraft.util.datafix.fixes.EntityBlockStateFix;
import net.minecraft.util.datafix.fixes.EntityBrushableBlockFieldsRenameFix;
import net.minecraft.util.datafix.fixes.EntityCatSplitFix;
import net.minecraft.util.datafix.fixes.EntityCodSalmonFix;
import net.minecraft.util.datafix.fixes.EntityCustomNameToComponentFix;
import net.minecraft.util.datafix.fixes.EntityElderGuardianSplitFix;
import net.minecraft.util.datafix.fixes.EntityEquipmentToArmorAndHandFix;
import net.minecraft.util.datafix.fixes.EntityGoatMissingStateFix;
import net.minecraft.util.datafix.fixes.EntityHealthFix;
import net.minecraft.util.datafix.fixes.EntityHorseSaddleFix;
import net.minecraft.util.datafix.fixes.EntityHorseSplitFix;
import net.minecraft.util.datafix.fixes.EntityIdFix;
import net.minecraft.util.datafix.fixes.EntityItemFrameDirectionFix;
import net.minecraft.util.datafix.fixes.EntityMinecartIdentifiersFix;
import net.minecraft.util.datafix.fixes.EntityPaintingFieldsRenameFix;
import net.minecraft.util.datafix.fixes.EntityPaintingItemFrameDirectionFix;
import net.minecraft.util.datafix.fixes.EntityPaintingMotiveFix;
import net.minecraft.util.datafix.fixes.EntityProjectileOwnerFix;
import net.minecraft.util.datafix.fixes.EntityPufferfishRenameFix;
import net.minecraft.util.datafix.fixes.EntityRavagerRenameFix;
import net.minecraft.util.datafix.fixes.EntityRedundantChanceTagsFix;
import net.minecraft.util.datafix.fixes.EntityRidingToPassengersFix;
import net.minecraft.util.datafix.fixes.EntityShulkerColorFix;
import net.minecraft.util.datafix.fixes.EntityShulkerRotationFix;
import net.minecraft.util.datafix.fixes.EntitySkeletonSplitFix;
import net.minecraft.util.datafix.fixes.EntityStringUuidFix;
import net.minecraft.util.datafix.fixes.EntityTheRenameningFix;
import net.minecraft.util.datafix.fixes.EntityTippedArrowFix;
import net.minecraft.util.datafix.fixes.EntityUUIDFix;
import net.minecraft.util.datafix.fixes.EntityVariantFix;
import net.minecraft.util.datafix.fixes.EntityWolfColorFix;
import net.minecraft.util.datafix.fixes.EntityZombieSplitFix;
import net.minecraft.util.datafix.fixes.EntityZombieVillagerTypeFix;
import net.minecraft.util.datafix.fixes.EntityZombifiedPiglinRenameFix;
import net.minecraft.util.datafix.fixes.FeatureFlagRemoveFix;
import net.minecraft.util.datafix.fixes.FilteredBooksFix;
import net.minecraft.util.datafix.fixes.FilteredSignsFix;
import net.minecraft.util.datafix.fixes.FixProjectileStoredItem;
import net.minecraft.util.datafix.fixes.ForcePoiRebuild;
import net.minecraft.util.datafix.fixes.FurnaceRecipeFix;
import net.minecraft.util.datafix.fixes.GoatHornIdFix;
import net.minecraft.util.datafix.fixes.GossipUUIDFix;
import net.minecraft.util.datafix.fixes.HeightmapRenamingFix;
import net.minecraft.util.datafix.fixes.HorseBodyArmorItemFix;
import net.minecraft.util.datafix.fixes.IglooMetadataRemovalFix;
import net.minecraft.util.datafix.fixes.ItemBannerColorFix;
import net.minecraft.util.datafix.fixes.ItemCustomNameToComponentFix;
import net.minecraft.util.datafix.fixes.ItemIdFix;
import net.minecraft.util.datafix.fixes.ItemLoreFix;
import net.minecraft.util.datafix.fixes.ItemPotionFix;
import net.minecraft.util.datafix.fixes.ItemRemoveBlockEntityTagFix;
import net.minecraft.util.datafix.fixes.ItemRenameFix;
import net.minecraft.util.datafix.fixes.ItemShulkerBoxColorFix;
import net.minecraft.util.datafix.fixes.ItemSpawnEggFix;
import net.minecraft.util.datafix.fixes.ItemStackComponentizationFix;
import net.minecraft.util.datafix.fixes.ItemStackCustomNameToOverrideComponentFix;
import net.minecraft.util.datafix.fixes.ItemStackEnchantmentNamesFix;
import net.minecraft.util.datafix.fixes.ItemStackMapIdFix;
import net.minecraft.util.datafix.fixes.ItemStackSpawnEggFix;
import net.minecraft.util.datafix.fixes.ItemStackTheFlatteningFix;
import net.minecraft.util.datafix.fixes.ItemStackUUIDFix;
import net.minecraft.util.datafix.fixes.ItemWaterPotionFix;
import net.minecraft.util.datafix.fixes.ItemWrittenBookPagesStrictJsonFix;
import net.minecraft.util.datafix.fixes.JigsawPropertiesFix;
import net.minecraft.util.datafix.fixes.JigsawRotationFix;
import net.minecraft.util.datafix.fixes.JukeboxTicksSinceSongStartedFix;
import net.minecraft.util.datafix.fixes.LeavesFix;
import net.minecraft.util.datafix.fixes.LegacyDragonFightFix;
import net.minecraft.util.datafix.fixes.LevelDataGeneratorOptionsFix;
import net.minecraft.util.datafix.fixes.LevelFlatGeneratorInfoFix;
import net.minecraft.util.datafix.fixes.LevelLegacyWorldGenSettingsFix;
import net.minecraft.util.datafix.fixes.LevelUUIDFix;
import net.minecraft.util.datafix.fixes.LodestoneCompassComponentFix;
import net.minecraft.util.datafix.fixes.MapBannerBlockPosFormatFix;
import net.minecraft.util.datafix.fixes.MapIdFix;
import net.minecraft.util.datafix.fixes.MemoryExpiryDataFix;
import net.minecraft.util.datafix.fixes.MissingDimensionFix;
import net.minecraft.util.datafix.fixes.MobEffectIdFix;
import net.minecraft.util.datafix.fixes.MobSpawnerEntityIdentifiersFix;
import net.minecraft.util.datafix.fixes.NamedEntityFix;
import net.minecraft.util.datafix.fixes.NamespacedTypeRenameFix;
import net.minecraft.util.datafix.fixes.NewVillageFix;
import net.minecraft.util.datafix.fixes.ObjectiveDisplayNameFix;
import net.minecraft.util.datafix.fixes.ObjectiveRenderTypeFix;
import net.minecraft.util.datafix.fixes.OminousBannerBlockEntityRenameFix;
import net.minecraft.util.datafix.fixes.OminousBannerRenameFix;
import net.minecraft.util.datafix.fixes.OptionsAccessibilityOnboardFix;
import net.minecraft.util.datafix.fixes.OptionsAddTextBackgroundFix;
import net.minecraft.util.datafix.fixes.OptionsAmbientOcclusionFix;
import net.minecraft.util.datafix.fixes.OptionsForceVBOFix;
import net.minecraft.util.datafix.fixes.OptionsKeyLwjgl3Fix;
import net.minecraft.util.datafix.fixes.OptionsKeyTranslationFix;
import net.minecraft.util.datafix.fixes.OptionsLowerCaseLanguageFix;
import net.minecraft.util.datafix.fixes.OptionsMenuBlurrinessFix;
import net.minecraft.util.datafix.fixes.OptionsProgrammerArtFix;
import net.minecraft.util.datafix.fixes.OptionsRenameFieldFix;
import net.minecraft.util.datafix.fixes.OverreachingTickFix;
import net.minecraft.util.datafix.fixes.ParticleUnflatteningFix;
import net.minecraft.util.datafix.fixes.PlayerHeadBlockProfileFix;
import net.minecraft.util.datafix.fixes.PlayerUUIDFix;
import net.minecraft.util.datafix.fixes.PoiTypeRemoveFix;
import net.minecraft.util.datafix.fixes.PoiTypeRenameFix;
import net.minecraft.util.datafix.fixes.PrimedTntBlockStateFixer;
import net.minecraft.util.datafix.fixes.ProjectileStoredWeaponFix;
import net.minecraft.util.datafix.fixes.RandomSequenceSettingsFix;
import net.minecraft.util.datafix.fixes.RecipesFix;
import net.minecraft.util.datafix.fixes.RecipesRenameningFix;
import net.minecraft.util.datafix.fixes.RedstoneWireConnectionsFix;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.util.datafix.fixes.RemapChunkStatusFix;
import net.minecraft.util.datafix.fixes.RemoveEmptyItemInBrushableBlockFix;
import net.minecraft.util.datafix.fixes.RemoveGolemGossipFix;
import net.minecraft.util.datafix.fixes.RenameEnchantmentsFix;
import net.minecraft.util.datafix.fixes.RenamedCoralFansFix;
import net.minecraft.util.datafix.fixes.RenamedCoralFix;
import net.minecraft.util.datafix.fixes.ReorganizePoi;
import net.minecraft.util.datafix.fixes.SavedDataFeaturePoolElementFix;
import net.minecraft.util.datafix.fixes.SavedDataUUIDFix;
import net.minecraft.util.datafix.fixes.ScoreboardDisplaySlotFix;
import net.minecraft.util.datafix.fixes.SpawnerDataFix;
import net.minecraft.util.datafix.fixes.StatsCounterFix;
import net.minecraft.util.datafix.fixes.StatsRenameFix;
import net.minecraft.util.datafix.fixes.StriderGravityFix;
import net.minecraft.util.datafix.fixes.StructureReferenceCountFix;
import net.minecraft.util.datafix.fixes.StructureSettingsFlattenFix;
import net.minecraft.util.datafix.fixes.StructuresBecomeConfiguredFix;
import net.minecraft.util.datafix.fixes.TeamDisplayNameFix;
import net.minecraft.util.datafix.fixes.TippedArrowPotionToItemFix;
import net.minecraft.util.datafix.fixes.TrappedChestBlockEntityFix;
import net.minecraft.util.datafix.fixes.TrialSpawnerConfigFix;
import net.minecraft.util.datafix.fixes.VariantRenameFix;
import net.minecraft.util.datafix.fixes.VillagerDataFix;
import net.minecraft.util.datafix.fixes.VillagerFollowRangeFix;
import net.minecraft.util.datafix.fixes.VillagerRebuildLevelAndXpFix;
import net.minecraft.util.datafix.fixes.VillagerTradeFix;
import net.minecraft.util.datafix.fixes.WallPropertyFix;
import net.minecraft.util.datafix.fixes.WeaponSmithChestLootTableFix;
import net.minecraft.util.datafix.fixes.WorldGenSettingsDisallowOldCustomWorldsFix;
import net.minecraft.util.datafix.fixes.WorldGenSettingsFix;
import net.minecraft.util.datafix.fixes.WorldGenSettingsHeightAndBiomeFix;
import net.minecraft.util.datafix.fixes.WriteAndReadFix;
import net.minecraft.util.datafix.fixes.ZombieVillagerRebuildXpFix;
import net.minecraft.util.datafix.schemas.NamespacedSchema;
import net.minecraft.util.datafix.schemas.V100;
import net.minecraft.util.datafix.schemas.V102;
import net.minecraft.util.datafix.schemas.V1022;
import net.minecraft.util.datafix.schemas.V106;
import net.minecraft.util.datafix.schemas.V107;
import net.minecraft.util.datafix.schemas.V1125;
import net.minecraft.util.datafix.schemas.V135;
import net.minecraft.util.datafix.schemas.V143;
import net.minecraft.util.datafix.schemas.V1451;
import net.minecraft.util.datafix.schemas.V1451_1;
import net.minecraft.util.datafix.schemas.V1451_2;
import net.minecraft.util.datafix.schemas.V1451_3;
import net.minecraft.util.datafix.schemas.V1451_4;
import net.minecraft.util.datafix.schemas.V1451_5;
import net.minecraft.util.datafix.schemas.V1451_6;
import net.minecraft.util.datafix.schemas.V1460;
import net.minecraft.util.datafix.schemas.V1466;
import net.minecraft.util.datafix.schemas.V1470;
import net.minecraft.util.datafix.schemas.V1481;
import net.minecraft.util.datafix.schemas.V1483;
import net.minecraft.util.datafix.schemas.V1486;
import net.minecraft.util.datafix.schemas.V1510;
import net.minecraft.util.datafix.schemas.V1800;
import net.minecraft.util.datafix.schemas.V1801;
import net.minecraft.util.datafix.schemas.V1904;
import net.minecraft.util.datafix.schemas.V1906;
import net.minecraft.util.datafix.schemas.V1909;
import net.minecraft.util.datafix.schemas.V1920;
import net.minecraft.util.datafix.schemas.V1928;
import net.minecraft.util.datafix.schemas.V1929;
import net.minecraft.util.datafix.schemas.V1931;
import net.minecraft.util.datafix.schemas.V2100;
import net.minecraft.util.datafix.schemas.V2501;
import net.minecraft.util.datafix.schemas.V2502;
import net.minecraft.util.datafix.schemas.V2505;
import net.minecraft.util.datafix.schemas.V2509;
import net.minecraft.util.datafix.schemas.V2519;
import net.minecraft.util.datafix.schemas.V2522;
import net.minecraft.util.datafix.schemas.V2551;
import net.minecraft.util.datafix.schemas.V2568;
import net.minecraft.util.datafix.schemas.V2571;
import net.minecraft.util.datafix.schemas.V2684;
import net.minecraft.util.datafix.schemas.V2686;
import net.minecraft.util.datafix.schemas.V2688;
import net.minecraft.util.datafix.schemas.V2704;
import net.minecraft.util.datafix.schemas.V2707;
import net.minecraft.util.datafix.schemas.V2831;
import net.minecraft.util.datafix.schemas.V2832;
import net.minecraft.util.datafix.schemas.V2842;
import net.minecraft.util.datafix.schemas.V3076;
import net.minecraft.util.datafix.schemas.V3078;
import net.minecraft.util.datafix.schemas.V3081;
import net.minecraft.util.datafix.schemas.V3082;
import net.minecraft.util.datafix.schemas.V3083;
import net.minecraft.util.datafix.schemas.V3202;
import net.minecraft.util.datafix.schemas.V3203;
import net.minecraft.util.datafix.schemas.V3204;
import net.minecraft.util.datafix.schemas.V3325;
import net.minecraft.util.datafix.schemas.V3326;
import net.minecraft.util.datafix.schemas.V3327;
import net.minecraft.util.datafix.schemas.V3328;
import net.minecraft.util.datafix.schemas.V3438;
import net.minecraft.util.datafix.schemas.V3448;
import net.minecraft.util.datafix.schemas.V3682;
import net.minecraft.util.datafix.schemas.V3683;
import net.minecraft.util.datafix.schemas.V3685;
import net.minecraft.util.datafix.schemas.V3689;
import net.minecraft.util.datafix.schemas.V3799;
import net.minecraft.util.datafix.schemas.V3807;
import net.minecraft.util.datafix.schemas.V3808;
import net.minecraft.util.datafix.schemas.V3808_1;
import net.minecraft.util.datafix.schemas.V3808_2;
import net.minecraft.util.datafix.schemas.V3816;
import net.minecraft.util.datafix.schemas.V3818;
import net.minecraft.util.datafix.schemas.V3818_3;
import net.minecraft.util.datafix.schemas.V3818_4;
import net.minecraft.util.datafix.schemas.V3818_5;
import net.minecraft.util.datafix.schemas.V3825;
import net.minecraft.util.datafix.schemas.V3938;
import net.minecraft.util.datafix.schemas.V501;
import net.minecraft.util.datafix.schemas.V700;
import net.minecraft.util.datafix.schemas.V701;
import net.minecraft.util.datafix.schemas.V702;
import net.minecraft.util.datafix.schemas.V703;
import net.minecraft.util.datafix.schemas.V704;
import net.minecraft.util.datafix.schemas.V705;
import net.minecraft.util.datafix.schemas.V808;
import net.minecraft.util.datafix.schemas.V99;

public class DataFixers {
   private static final BiFunction<Integer, Schema, Schema> SAME = Schema::new;
   private static final BiFunction<Integer, Schema, Schema> SAME_NAMESPACED = NamespacedSchema::new;
   private static final Result DATA_FIXER = createFixerUpper();
   public static final int BLENDING_VERSION = 3441;

   private DataFixers() {
   }

   public static DataFixer getDataFixer() {
      return DATA_FIXER.fixer();
   }

   private static Result createFixerUpper() {
      DataFixerBuilder $$0 = new DataFixerBuilder(SharedConstants.getCurrentVersion().getDataVersion().getVersion());
      addFixers($$0);
      return $$0.build();
   }

   public static CompletableFuture<?> optimize(Set<TypeReference> pReferences) {
      if (pReferences.isEmpty()) {
         return CompletableFuture.completedFuture(null);
      } else {
         Executor $$1 = Executors.newSingleThreadExecutor(
            new ThreadFactoryBuilder().setNameFormat("Datafixer Bootstrap").setDaemon(true).setPriority(1).build()
         );
         return DATA_FIXER.optimize(pReferences, $$1);
      }
   }

   private static void addFixers(DataFixerBuilder pBuilder) {
      pBuilder.addSchema(99, V99::new);
      Schema $$1 = pBuilder.addSchema(100, V100::new);
      pBuilder.addFixer(new EntityEquipmentToArmorAndHandFix($$1, true));
      Schema $$2 = pBuilder.addSchema(101, SAME);
      pBuilder.addFixer(new BlockEntitySignTextStrictJsonFix($$2, false));
      Schema $$3 = pBuilder.addSchema(102, V102::new);
      pBuilder.addFixer(new ItemIdFix($$3, true));
      pBuilder.addFixer(new ItemPotionFix($$3, false));
      Schema $$4 = pBuilder.addSchema(105, SAME);
      pBuilder.addFixer(new ItemSpawnEggFix($$4, true));
      Schema $$5 = pBuilder.addSchema(106, V106::new);
      pBuilder.addFixer(new MobSpawnerEntityIdentifiersFix($$5, true));
      Schema $$6 = pBuilder.addSchema(107, V107::new);
      pBuilder.addFixer(new EntityMinecartIdentifiersFix($$6));
      Schema $$7 = pBuilder.addSchema(108, SAME);
      pBuilder.addFixer(new EntityStringUuidFix($$7, true));
      Schema $$8 = pBuilder.addSchema(109, SAME);
      pBuilder.addFixer(new EntityHealthFix($$8, true));
      Schema $$9 = pBuilder.addSchema(110, SAME);
      pBuilder.addFixer(new EntityHorseSaddleFix($$9, true));
      Schema $$10 = pBuilder.addSchema(111, SAME);
      pBuilder.addFixer(new EntityPaintingItemFrameDirectionFix($$10, true));
      Schema $$11 = pBuilder.addSchema(113, SAME);
      pBuilder.addFixer(new EntityRedundantChanceTagsFix($$11, true));
      Schema $$12 = pBuilder.addSchema(135, V135::new);
      pBuilder.addFixer(new EntityRidingToPassengersFix($$12, true));
      Schema $$13 = pBuilder.addSchema(143, V143::new);
      pBuilder.addFixer(new EntityTippedArrowFix($$13, true));
      Schema $$14 = pBuilder.addSchema(147, SAME);
      pBuilder.addFixer(new EntityArmorStandSilentFix($$14, true));
      Schema $$15 = pBuilder.addSchema(165, SAME);
      pBuilder.addFixer(new ItemWrittenBookPagesStrictJsonFix($$15, true));
      Schema $$16 = pBuilder.addSchema(501, V501::new);
      pBuilder.addFixer(new AddNewChoices($$16, "Add 1.10 entities fix", References.ENTITY));
      Schema $$17 = pBuilder.addSchema(502, SAME);
      pBuilder.addFixer(
         ItemRenameFix.create(
            $$17,
            "cooked_fished item renamer",
            p_14533_ -> Objects.equals(NamespacedSchema.ensureNamespaced(p_14533_), "minecraft:cooked_fished") ? "minecraft:cooked_fish" : p_14533_
         )
      );
      pBuilder.addFixer(new EntityZombieVillagerTypeFix($$17, false));
      Schema $$18 = pBuilder.addSchema(505, SAME);
      pBuilder.addFixer(new OptionsForceVBOFix($$18, false));
      Schema $$19 = pBuilder.addSchema(700, V700::new);
      pBuilder.addFixer(new EntityElderGuardianSplitFix($$19, true));
      Schema $$20 = pBuilder.addSchema(701, V701::new);
      pBuilder.addFixer(new EntitySkeletonSplitFix($$20, true));
      Schema $$21 = pBuilder.addSchema(702, V702::new);
      pBuilder.addFixer(new EntityZombieSplitFix($$21));
      Schema $$22 = pBuilder.addSchema(703, V703::new);
      pBuilder.addFixer(new EntityHorseSplitFix($$22, true));
      Schema $$23 = pBuilder.addSchema(704, V704::new);
      pBuilder.addFixer(new BlockEntityIdFix($$23, true));
      Schema $$24 = pBuilder.addSchema(705, V705::new);
      pBuilder.addFixer(new EntityIdFix($$24, true));
      Schema $$25 = pBuilder.addSchema(804, SAME_NAMESPACED);
      pBuilder.addFixer(new ItemBannerColorFix($$25, true));
      Schema $$26 = pBuilder.addSchema(806, SAME_NAMESPACED);
      pBuilder.addFixer(new ItemWaterPotionFix($$26, false));
      Schema $$27 = pBuilder.addSchema(808, V808::new);
      pBuilder.addFixer(new AddNewChoices($$27, "added shulker box", References.BLOCK_ENTITY));
      Schema $$28 = pBuilder.addSchema(808, 1, SAME_NAMESPACED);
      pBuilder.addFixer(new EntityShulkerColorFix($$28, false));
      Schema $$29 = pBuilder.addSchema(813, SAME_NAMESPACED);
      pBuilder.addFixer(new ItemShulkerBoxColorFix($$29, false));
      pBuilder.addFixer(new BlockEntityShulkerBoxColorFix($$29, false));
      Schema $$30 = pBuilder.addSchema(816, SAME_NAMESPACED);
      pBuilder.addFixer(new OptionsLowerCaseLanguageFix($$30, false));
      Schema $$31 = pBuilder.addSchema(820, SAME_NAMESPACED);
      pBuilder.addFixer(ItemRenameFix.create($$31, "totem item renamer", createRenamer("minecraft:totem", "minecraft:totem_of_undying")));
      Schema $$32 = pBuilder.addSchema(1022, V1022::new);
      pBuilder.addFixer(new WriteAndReadFix($$32, "added shoulder entities to players", References.PLAYER));
      Schema $$33 = pBuilder.addSchema(1125, V1125::new);
      pBuilder.addFixer(new ChunkBedBlockEntityInjecterFix($$33, true));
      pBuilder.addFixer(new BedItemColorFix($$33, false));
      Schema $$34 = pBuilder.addSchema(1344, SAME_NAMESPACED);
      pBuilder.addFixer(new OptionsKeyLwjgl3Fix($$34, false));
      Schema $$35 = pBuilder.addSchema(1446, SAME_NAMESPACED);
      pBuilder.addFixer(new OptionsKeyTranslationFix($$35, false));
      Schema $$36 = pBuilder.addSchema(1450, SAME_NAMESPACED);
      pBuilder.addFixer(new BlockStateStructureTemplateFix($$36, false));
      Schema $$37 = pBuilder.addSchema(1451, V1451::new);
      pBuilder.addFixer(new AddNewChoices($$37, "AddTrappedChestFix", References.BLOCK_ENTITY));
      Schema $$38 = pBuilder.addSchema(1451, 1, V1451_1::new);
      pBuilder.addFixer(new ChunkPalettedStorageFix($$38, true));
      Schema $$39 = pBuilder.addSchema(1451, 2, V1451_2::new);
      pBuilder.addFixer(new BlockEntityBlockStateFix($$39, true));
      Schema $$40 = pBuilder.addSchema(1451, 3, V1451_3::new);
      pBuilder.addFixer(new EntityBlockStateFix($$40, true));
      pBuilder.addFixer(new ItemStackMapIdFix($$40, false));
      Schema $$41 = pBuilder.addSchema(1451, 4, V1451_4::new);
      pBuilder.addFixer(new BlockNameFlatteningFix($$41, true));
      pBuilder.addFixer(new ItemStackTheFlatteningFix($$41, false));
      Schema $$42 = pBuilder.addSchema(1451, 5, V1451_5::new);
      pBuilder.addFixer(
         new ItemRemoveBlockEntityTagFix(
            $$42,
            false,
            Set.of(
               "minecraft:note_block",
               "minecraft:flower_pot",
               "minecraft:dandelion",
               "minecraft:poppy",
               "minecraft:blue_orchid",
               "minecraft:allium",
               "minecraft:azure_bluet",
               "minecraft:red_tulip",
               "minecraft:orange_tulip",
               "minecraft:white_tulip",
               "minecraft:pink_tulip",
               "minecraft:oxeye_daisy",
               "minecraft:cactus",
               "minecraft:brown_mushroom",
               "minecraft:red_mushroom",
               "minecraft:oak_sapling",
               "minecraft:spruce_sapling",
               "minecraft:birch_sapling",
               "minecraft:jungle_sapling",
               "minecraft:acacia_sapling",
               "minecraft:dark_oak_sapling",
               "minecraft:dead_bush",
               "minecraft:fern"
            )
         )
      );
      pBuilder.addFixer(new AddNewChoices($$42, "RemoveNoteBlockFlowerPotFix", References.BLOCK_ENTITY));
      pBuilder.addFixer(new ItemStackSpawnEggFix($$42, false, "minecraft:spawn_egg"));
      pBuilder.addFixer(new EntityWolfColorFix($$42, false));
      pBuilder.addFixer(new BlockEntityBannerColorFix($$42, false));
      pBuilder.addFixer(new LevelFlatGeneratorInfoFix($$42, false));
      Schema $$43 = pBuilder.addSchema(1451, 6, V1451_6::new);
      pBuilder.addFixer(new StatsCounterFix($$43, true));
      pBuilder.addFixer(new BlockEntityJukeboxFix($$43, false));
      Schema $$44 = pBuilder.addSchema(1451, 7, SAME_NAMESPACED);
      pBuilder.addFixer(new VillagerTradeFix($$44));
      Schema $$45 = pBuilder.addSchema(1456, SAME_NAMESPACED);
      pBuilder.addFixer(new EntityItemFrameDirectionFix($$45, false));
      Schema $$46 = pBuilder.addSchema(1458, SAME_NAMESPACED);
      pBuilder.addFixer(new EntityCustomNameToComponentFix($$46, false));
      pBuilder.addFixer(new ItemCustomNameToComponentFix($$46, false));
      pBuilder.addFixer(new BlockEntityCustomNameToComponentFix($$46, false));
      Schema $$47 = pBuilder.addSchema(1460, V1460::new);
      pBuilder.addFixer(new EntityPaintingMotiveFix($$47, false));
      Schema $$48 = pBuilder.addSchema(1466, V1466::new);
      pBuilder.addFixer(new AddNewChoices($$48, "Add DUMMY block entity", References.BLOCK_ENTITY));
      pBuilder.addFixer(new ChunkToProtochunkFix($$48, true));
      Schema $$49 = pBuilder.addSchema(1470, V1470::new);
      pBuilder.addFixer(new AddNewChoices($$49, "Add 1.13 entities fix", References.ENTITY));
      Schema $$50 = pBuilder.addSchema(1474, SAME_NAMESPACED);
      pBuilder.addFixer(new ColorlessShulkerEntityFix($$50, false));
      pBuilder.addFixer(
         BlockRenameFix.create(
            $$50,
            "Colorless shulker block fixer",
            p_14531_ -> Objects.equals(NamespacedSchema.ensureNamespaced(p_14531_), "minecraft:purple_shulker_box") ? "minecraft:shulker_box" : p_14531_
         )
      );
      pBuilder.addFixer(
         ItemRenameFix.create(
            $$50,
            "Colorless shulker item fixer",
            p_14516_ -> Objects.equals(NamespacedSchema.ensureNamespaced(p_14516_), "minecraft:purple_shulker_box") ? "minecraft:shulker_box" : p_14516_
         )
      );
      Schema $$51 = pBuilder.addSchema(1475, SAME_NAMESPACED);
      pBuilder.addFixer(
         BlockRenameFix.create(
            $$51, "Flowing fixer", createRenamer(ImmutableMap.of("minecraft:flowing_water", "minecraft:water", "minecraft:flowing_lava", "minecraft:lava"))
         )
      );
      Schema $$52 = pBuilder.addSchema(1480, SAME_NAMESPACED);
      pBuilder.addFixer(BlockRenameFix.create($$52, "Rename coral blocks", createRenamer(RenamedCoralFix.RENAMED_IDS)));
      pBuilder.addFixer(ItemRenameFix.create($$52, "Rename coral items", createRenamer(RenamedCoralFix.RENAMED_IDS)));
      Schema $$53 = pBuilder.addSchema(1481, V1481::new);
      pBuilder.addFixer(new AddNewChoices($$53, "Add conduit", References.BLOCK_ENTITY));
      Schema $$54 = pBuilder.addSchema(1483, V1483::new);
      pBuilder.addFixer(new EntityPufferfishRenameFix($$54, true));
      pBuilder.addFixer(ItemRenameFix.create($$54, "Rename pufferfish egg item", createRenamer(EntityPufferfishRenameFix.RENAMED_IDS)));
      Schema $$55 = pBuilder.addSchema(1484, SAME_NAMESPACED);
      pBuilder.addFixer(
         ItemRenameFix.create(
            $$55,
            "Rename seagrass items",
            createRenamer(ImmutableMap.of("minecraft:sea_grass", "minecraft:seagrass", "minecraft:tall_sea_grass", "minecraft:tall_seagrass"))
         )
      );
      pBuilder.addFixer(
         BlockRenameFix.create(
            $$55,
            "Rename seagrass blocks",
            createRenamer(ImmutableMap.of("minecraft:sea_grass", "minecraft:seagrass", "minecraft:tall_sea_grass", "minecraft:tall_seagrass"))
         )
      );
      pBuilder.addFixer(new HeightmapRenamingFix($$55, false));
      Schema $$56 = pBuilder.addSchema(1486, V1486::new);
      pBuilder.addFixer(new EntityCodSalmonFix($$56, true));
      pBuilder.addFixer(ItemRenameFix.create($$56, "Rename cod/salmon egg items", createRenamer(EntityCodSalmonFix.RENAMED_EGG_IDS)));
      Schema $$57 = pBuilder.addSchema(1487, SAME_NAMESPACED);
      pBuilder.addFixer(
         ItemRenameFix.create(
            $$57,
            "Rename prismarine_brick(s)_* blocks",
            createRenamer(
               ImmutableMap.of(
                  "minecraft:prismarine_bricks_slab",
                  "minecraft:prismarine_brick_slab",
                  "minecraft:prismarine_bricks_stairs",
                  "minecraft:prismarine_brick_stairs"
               )
            )
         )
      );
      pBuilder.addFixer(
         BlockRenameFix.create(
            $$57,
            "Rename prismarine_brick(s)_* items",
            createRenamer(
               ImmutableMap.of(
                  "minecraft:prismarine_bricks_slab",
                  "minecraft:prismarine_brick_slab",
                  "minecraft:prismarine_bricks_stairs",
                  "minecraft:prismarine_brick_stairs"
               )
            )
         )
      );
      Schema $$58 = pBuilder.addSchema(1488, SAME_NAMESPACED);
      pBuilder.addFixer(
         BlockRenameFix.create(
            $$58, "Rename kelp/kelptop", createRenamer(ImmutableMap.of("minecraft:kelp_top", "minecraft:kelp", "minecraft:kelp", "minecraft:kelp_plant"))
         )
      );
      pBuilder.addFixer(ItemRenameFix.create($$58, "Rename kelptop", createRenamer("minecraft:kelp_top", "minecraft:kelp")));
      pBuilder.addFixer(new NamedEntityFix($$58, false, "Command block block entity custom name fix", References.BLOCK_ENTITY, "minecraft:command_block") {
         @Override
         protected Typed<?> fix(Typed<?> p_14541_) {
            return p_14541_.update(DSL.remainderFinder(), EntityCustomNameToComponentFix::fixTagCustomName);
         }
      });
      pBuilder.addFixer(new NamedEntityFix($$58, false, "Command block minecart custom name fix", References.ENTITY, "minecraft:commandblock_minecart") {
         @Override
         protected Typed<?> fix(Typed<?> p_14549_) {
            return p_14549_.update(DSL.remainderFinder(), EntityCustomNameToComponentFix::fixTagCustomName);
         }
      });
      pBuilder.addFixer(new IglooMetadataRemovalFix($$58, false));
      Schema $$59 = pBuilder.addSchema(1490, SAME_NAMESPACED);
      pBuilder.addFixer(BlockRenameFix.create($$59, "Rename melon_block", createRenamer("minecraft:melon_block", "minecraft:melon")));
      pBuilder.addFixer(
         ItemRenameFix.create(
            $$59,
            "Rename melon_block/melon/speckled_melon",
            createRenamer(
               ImmutableMap.of(
                  "minecraft:melon_block",
                  "minecraft:melon",
                  "minecraft:melon",
                  "minecraft:melon_slice",
                  "minecraft:speckled_melon",
                  "minecraft:glistering_melon_slice"
               )
            )
         )
      );
      Schema $$60 = pBuilder.addSchema(1492, SAME_NAMESPACED);
      pBuilder.addFixer(new ChunkStructuresTemplateRenameFix($$60, false));
      Schema $$61 = pBuilder.addSchema(1494, SAME_NAMESPACED);
      pBuilder.addFixer(new ItemStackEnchantmentNamesFix($$61, false));
      Schema $$62 = pBuilder.addSchema(1496, SAME_NAMESPACED);
      pBuilder.addFixer(new LeavesFix($$62, false));
      Schema $$63 = pBuilder.addSchema(1500, SAME_NAMESPACED);
      pBuilder.addFixer(new BlockEntityKeepPacked($$63, false));
      Schema $$64 = pBuilder.addSchema(1501, SAME_NAMESPACED);
      pBuilder.addFixer(new AdvancementsFix($$64, false));
      Schema $$65 = pBuilder.addSchema(1502, SAME_NAMESPACED);
      pBuilder.addFixer(new NamespacedTypeRenameFix($$65, "Recipes fix", References.RECIPE, createRenamer(RecipesFix.RECIPES)));
      Schema $$66 = pBuilder.addSchema(1506, SAME_NAMESPACED);
      pBuilder.addFixer(new LevelDataGeneratorOptionsFix($$66, false));
      Schema $$67 = pBuilder.addSchema(1510, V1510::new);
      pBuilder.addFixer(BlockRenameFix.create($$67, "Block renamening fix", createRenamer(EntityTheRenameningFix.RENAMED_BLOCKS)));
      pBuilder.addFixer(ItemRenameFix.create($$67, "Item renamening fix", createRenamer(EntityTheRenameningFix.RENAMED_ITEMS)));
      pBuilder.addFixer(new NamespacedTypeRenameFix($$67, "Recipes renamening fix", References.RECIPE, createRenamer(RecipesRenameningFix.RECIPES)));
      pBuilder.addFixer(new EntityTheRenameningFix($$67, true));
      pBuilder.addFixer(
         new StatsRenameFix(
            $$67,
            "SwimStatsRenameFix",
            ImmutableMap.of("minecraft:swim_one_cm", "minecraft:walk_on_water_one_cm", "minecraft:dive_one_cm", "minecraft:walk_under_water_one_cm")
         )
      );
      Schema $$68 = pBuilder.addSchema(1514, SAME_NAMESPACED);
      pBuilder.addFixer(new ObjectiveDisplayNameFix($$68, false));
      pBuilder.addFixer(new TeamDisplayNameFix($$68, false));
      pBuilder.addFixer(new ObjectiveRenderTypeFix($$68, false));
      Schema $$69 = pBuilder.addSchema(1515, SAME_NAMESPACED);
      pBuilder.addFixer(BlockRenameFix.create($$69, "Rename coral fan blocks", createRenamer(RenamedCoralFansFix.RENAMED_IDS)));
      Schema $$70 = pBuilder.addSchema(1624, SAME_NAMESPACED);
      pBuilder.addFixer(new TrappedChestBlockEntityFix($$70, false));
      Schema $$71 = pBuilder.addSchema(1800, V1800::new);
      pBuilder.addFixer(new AddNewChoices($$71, "Added 1.14 mobs fix", References.ENTITY));
      pBuilder.addFixer(ItemRenameFix.create($$71, "Rename dye items", createRenamer(DyeItemRenameFix.RENAMED_IDS)));
      Schema $$72 = pBuilder.addSchema(1801, V1801::new);
      pBuilder.addFixer(new AddNewChoices($$72, "Added Illager Beast", References.ENTITY));
      Schema $$73 = pBuilder.addSchema(1802, SAME_NAMESPACED);
      pBuilder.addFixer(
         BlockRenameFix.create(
            $$73,
            "Rename sign blocks & stone slabs",
            createRenamer(
               ImmutableMap.of(
                  "minecraft:stone_slab",
                  "minecraft:smooth_stone_slab",
                  "minecraft:sign",
                  "minecraft:oak_sign",
                  "minecraft:wall_sign",
                  "minecraft:oak_wall_sign"
               )
            )
         )
      );
      pBuilder.addFixer(
         ItemRenameFix.create(
            $$73,
            "Rename sign item & stone slabs",
            createRenamer(ImmutableMap.of("minecraft:stone_slab", "minecraft:smooth_stone_slab", "minecraft:sign", "minecraft:oak_sign"))
         )
      );
      Schema $$74 = pBuilder.addSchema(1803, SAME_NAMESPACED);
      pBuilder.addFixer(new ItemLoreFix($$74, false));
      Schema $$75 = pBuilder.addSchema(1904, V1904::new);
      pBuilder.addFixer(new AddNewChoices($$75, "Added Cats", References.ENTITY));
      pBuilder.addFixer(new EntityCatSplitFix($$75, false));
      Schema $$76 = pBuilder.addSchema(1905, SAME_NAMESPACED);
      pBuilder.addFixer(new ChunkStatusFix($$76, false));
      Schema $$77 = pBuilder.addSchema(1906, V1906::new);
      pBuilder.addFixer(new AddNewChoices($$77, "Add POI Blocks", References.BLOCK_ENTITY));
      Schema $$78 = pBuilder.addSchema(1909, V1909::new);
      pBuilder.addFixer(new AddNewChoices($$78, "Add jigsaw", References.BLOCK_ENTITY));
      Schema $$79 = pBuilder.addSchema(1911, SAME_NAMESPACED);
      pBuilder.addFixer(new ChunkStatusFix2($$79, false));
      Schema $$80 = pBuilder.addSchema(1914, SAME_NAMESPACED);
      pBuilder.addFixer(new WeaponSmithChestLootTableFix($$80, false));
      Schema $$81 = pBuilder.addSchema(1917, SAME_NAMESPACED);
      pBuilder.addFixer(new CatTypeFix($$81, false));
      Schema $$82 = pBuilder.addSchema(1918, SAME_NAMESPACED);
      pBuilder.addFixer(new VillagerDataFix($$82, "minecraft:villager"));
      pBuilder.addFixer(new VillagerDataFix($$82, "minecraft:zombie_villager"));
      Schema $$83 = pBuilder.addSchema(1920, V1920::new);
      pBuilder.addFixer(new NewVillageFix($$83, false));
      pBuilder.addFixer(new AddNewChoices($$83, "Add campfire", References.BLOCK_ENTITY));
      Schema $$84 = pBuilder.addSchema(1925, SAME_NAMESPACED);
      pBuilder.addFixer(new MapIdFix($$84, false));
      Schema $$85 = pBuilder.addSchema(1928, V1928::new);
      pBuilder.addFixer(new EntityRavagerRenameFix($$85, true));
      pBuilder.addFixer(ItemRenameFix.create($$85, "Rename ravager egg item", createRenamer(EntityRavagerRenameFix.RENAMED_IDS)));
      Schema $$86 = pBuilder.addSchema(1929, V1929::new);
      pBuilder.addFixer(new AddNewChoices($$86, "Add Wandering Trader and Trader Llama", References.ENTITY));
      Schema $$87 = pBuilder.addSchema(1931, V1931::new);
      pBuilder.addFixer(new AddNewChoices($$87, "Added Fox", References.ENTITY));
      Schema $$88 = pBuilder.addSchema(1936, SAME_NAMESPACED);
      pBuilder.addFixer(new OptionsAddTextBackgroundFix($$88, false));
      Schema $$89 = pBuilder.addSchema(1946, SAME_NAMESPACED);
      pBuilder.addFixer(new ReorganizePoi($$89, false));
      Schema $$90 = pBuilder.addSchema(1948, SAME_NAMESPACED);
      pBuilder.addFixer(new OminousBannerRenameFix($$90));
      Schema $$91 = pBuilder.addSchema(1953, SAME_NAMESPACED);
      pBuilder.addFixer(new OminousBannerBlockEntityRenameFix($$91, false));
      Schema $$92 = pBuilder.addSchema(1955, SAME_NAMESPACED);
      pBuilder.addFixer(new VillagerRebuildLevelAndXpFix($$92, false));
      pBuilder.addFixer(new ZombieVillagerRebuildXpFix($$92, false));
      Schema $$93 = pBuilder.addSchema(1961, SAME_NAMESPACED);
      pBuilder.addFixer(new ChunkLightRemoveFix($$93, false));
      Schema $$94 = pBuilder.addSchema(1963, SAME_NAMESPACED);
      pBuilder.addFixer(new RemoveGolemGossipFix($$94, false));
      Schema $$95 = pBuilder.addSchema(2100, V2100::new);
      pBuilder.addFixer(new AddNewChoices($$95, "Added Bee and Bee Stinger", References.ENTITY));
      pBuilder.addFixer(new AddNewChoices($$95, "Add beehive", References.BLOCK_ENTITY));
      pBuilder.addFixer(
         new NamespacedTypeRenameFix($$95, "Rename sugar recipe", References.RECIPE, createRenamer("minecraft:sugar", "minecraft:sugar_from_sugar_cane"))
      );
      pBuilder.addFixer(
         new AdvancementsRenameFix(
            $$95, false, "Rename sugar recipe advancement", createRenamer("minecraft:recipes/misc/sugar", "minecraft:recipes/misc/sugar_from_sugar_cane")
         )
      );
      Schema $$96 = pBuilder.addSchema(2202, SAME_NAMESPACED);
      pBuilder.addFixer(new ChunkBiomeFix($$96, false));
      Schema $$97 = pBuilder.addSchema(2209, SAME_NAMESPACED);
      UnaryOperator<String> $$98 = createRenamer("minecraft:bee_hive", "minecraft:beehive");
      pBuilder.addFixer(ItemRenameFix.create($$97, "Rename bee_hive item to beehive", $$98));
      pBuilder.addFixer(new PoiTypeRenameFix($$97, "Rename bee_hive poi to beehive", $$98));
      pBuilder.addFixer(BlockRenameFix.create($$97, "Rename bee_hive block to beehive", $$98));
      Schema $$99 = pBuilder.addSchema(2211, SAME_NAMESPACED);
      pBuilder.addFixer(new StructureReferenceCountFix($$99, false));
      Schema $$100 = pBuilder.addSchema(2218, SAME_NAMESPACED);
      pBuilder.addFixer(new ForcePoiRebuild($$100, false));
      Schema $$101 = pBuilder.addSchema(2501, V2501::new);
      pBuilder.addFixer(new FurnaceRecipeFix($$101, true));
      Schema $$102 = pBuilder.addSchema(2502, V2502::new);
      pBuilder.addFixer(new AddNewChoices($$102, "Added Hoglin", References.ENTITY));
      Schema $$103 = pBuilder.addSchema(2503, SAME_NAMESPACED);
      pBuilder.addFixer(new WallPropertyFix($$103, false));
      pBuilder.addFixer(
         new AdvancementsRenameFix(
            $$103, false, "Composter category change", createRenamer("minecraft:recipes/misc/composter", "minecraft:recipes/decorations/composter")
         )
      );
      Schema $$104 = pBuilder.addSchema(2505, V2505::new);
      pBuilder.addFixer(new AddNewChoices($$104, "Added Piglin", References.ENTITY));
      pBuilder.addFixer(new MemoryExpiryDataFix($$104, "minecraft:villager"));
      Schema $$105 = pBuilder.addSchema(2508, SAME_NAMESPACED);
      pBuilder.addFixer(
         ItemRenameFix.create(
            $$105,
            "Renamed fungi items to fungus",
            createRenamer(ImmutableMap.of("minecraft:warped_fungi", "minecraft:warped_fungus", "minecraft:crimson_fungi", "minecraft:crimson_fungus"))
         )
      );
      pBuilder.addFixer(
         BlockRenameFix.create(
            $$105,
            "Renamed fungi blocks to fungus",
            createRenamer(ImmutableMap.of("minecraft:warped_fungi", "minecraft:warped_fungus", "minecraft:crimson_fungi", "minecraft:crimson_fungus"))
         )
      );
      Schema $$106 = pBuilder.addSchema(2509, V2509::new);
      pBuilder.addFixer(new EntityZombifiedPiglinRenameFix($$106));
      pBuilder.addFixer(ItemRenameFix.create($$106, "Rename zombie pigman egg item", createRenamer(EntityZombifiedPiglinRenameFix.RENAMED_IDS)));
      Schema $$107 = pBuilder.addSchema(2511, SAME_NAMESPACED);
      pBuilder.addFixer(new EntityProjectileOwnerFix($$107));
      Schema $$108 = pBuilder.addSchema(2514, SAME_NAMESPACED);
      pBuilder.addFixer(new EntityUUIDFix($$108));
      pBuilder.addFixer(new BlockEntityUUIDFix($$108));
      pBuilder.addFixer(new PlayerUUIDFix($$108));
      pBuilder.addFixer(new LevelUUIDFix($$108));
      pBuilder.addFixer(new SavedDataUUIDFix($$108));
      pBuilder.addFixer(new ItemStackUUIDFix($$108));
      Schema $$109 = pBuilder.addSchema(2516, SAME_NAMESPACED);
      pBuilder.addFixer(new GossipUUIDFix($$109, "minecraft:villager"));
      pBuilder.addFixer(new GossipUUIDFix($$109, "minecraft:zombie_villager"));
      Schema $$110 = pBuilder.addSchema(2518, SAME_NAMESPACED);
      pBuilder.addFixer(new JigsawPropertiesFix($$110, false));
      pBuilder.addFixer(new JigsawRotationFix($$110, false));
      Schema $$111 = pBuilder.addSchema(2519, V2519::new);
      pBuilder.addFixer(new AddNewChoices($$111, "Added Strider", References.ENTITY));
      Schema $$112 = pBuilder.addSchema(2522, V2522::new);
      pBuilder.addFixer(new AddNewChoices($$112, "Added Zoglin", References.ENTITY));
      Schema $$113 = pBuilder.addSchema(2523, SAME_NAMESPACED);
      pBuilder.addFixer(
         new AttributesRename(
            $$113,
            "Attribute renames",
            createRenamerNoNamespace(
               ImmutableMap.builder()
                  .put("generic.maxHealth", "minecraft:generic.max_health")
                  .put("Max Health", "minecraft:generic.max_health")
                  .put("zombie.spawnReinforcements", "minecraft:zombie.spawn_reinforcements")
                  .put("Spawn Reinforcements Chance", "minecraft:zombie.spawn_reinforcements")
                  .put("horse.jumpStrength", "minecraft:horse.jump_strength")
                  .put("Jump Strength", "minecraft:horse.jump_strength")
                  .put("generic.followRange", "minecraft:generic.follow_range")
                  .put("Follow Range", "minecraft:generic.follow_range")
                  .put("generic.knockbackResistance", "minecraft:generic.knockback_resistance")
                  .put("Knockback Resistance", "minecraft:generic.knockback_resistance")
                  .put("generic.movementSpeed", "minecraft:generic.movement_speed")
                  .put("Movement Speed", "minecraft:generic.movement_speed")
                  .put("generic.flyingSpeed", "minecraft:generic.flying_speed")
                  .put("Flying Speed", "minecraft:generic.flying_speed")
                  .put("generic.attackDamage", "minecraft:generic.attack_damage")
                  .put("generic.attackKnockback", "minecraft:generic.attack_knockback")
                  .put("generic.attackSpeed", "minecraft:generic.attack_speed")
                  .put("generic.armorToughness", "minecraft:generic.armor_toughness")
                  .build()
            )
         )
      );
      Schema $$114 = pBuilder.addSchema(2527, SAME_NAMESPACED);
      pBuilder.addFixer(new BitStorageAlignFix($$114));
      Schema $$115 = pBuilder.addSchema(2528, SAME_NAMESPACED);
      pBuilder.addFixer(
         ItemRenameFix.create(
            $$115,
            "Rename soul fire torch and soul fire lantern",
            createRenamer(ImmutableMap.of("minecraft:soul_fire_torch", "minecraft:soul_torch", "minecraft:soul_fire_lantern", "minecraft:soul_lantern"))
         )
      );
      pBuilder.addFixer(
         BlockRenameFix.create(
            $$115,
            "Rename soul fire torch and soul fire lantern",
            createRenamer(
               ImmutableMap.of(
                  "minecraft:soul_fire_torch",
                  "minecraft:soul_torch",
                  "minecraft:soul_fire_wall_torch",
                  "minecraft:soul_wall_torch",
                  "minecraft:soul_fire_lantern",
                  "minecraft:soul_lantern"
               )
            )
         )
      );
      Schema $$116 = pBuilder.addSchema(2529, SAME_NAMESPACED);
      pBuilder.addFixer(new StriderGravityFix($$116, false));
      Schema $$117 = pBuilder.addSchema(2531, SAME_NAMESPACED);
      pBuilder.addFixer(new RedstoneWireConnectionsFix($$117));
      Schema $$118 = pBuilder.addSchema(2533, SAME_NAMESPACED);
      pBuilder.addFixer(new VillagerFollowRangeFix($$118));
      Schema $$119 = pBuilder.addSchema(2535, SAME_NAMESPACED);
      pBuilder.addFixer(new EntityShulkerRotationFix($$119));
      Schema $$120 = pBuilder.addSchema(2538, SAME_NAMESPACED);
      pBuilder.addFixer(new LevelLegacyWorldGenSettingsFix($$120));
      Schema $$121 = pBuilder.addSchema(2550, SAME_NAMESPACED);
      pBuilder.addFixer(new WorldGenSettingsFix($$121));
      Schema $$122 = pBuilder.addSchema(2551, V2551::new);
      pBuilder.addFixer(new WriteAndReadFix($$122, "add types to WorldGenData", References.WORLD_GEN_SETTINGS));
      Schema $$123 = pBuilder.addSchema(2552, SAME_NAMESPACED);
      pBuilder.addFixer(
         new NamespacedTypeRenameFix($$123, "Nether biome rename", References.BIOME, createRenamer("minecraft:nether", "minecraft:nether_wastes"))
      );
      Schema $$124 = pBuilder.addSchema(2553, SAME_NAMESPACED);
      pBuilder.addFixer(new NamespacedTypeRenameFix($$124, "Biomes fix", References.BIOME, createRenamer(BiomeFix.BIOMES)));
      Schema $$125 = pBuilder.addSchema(2558, SAME_NAMESPACED);
      pBuilder.addFixer(new MissingDimensionFix($$125, false));
      pBuilder.addFixer(new OptionsRenameFieldFix($$125, false, "Rename swapHands setting", "key_key.swapHands", "key_key.swapOffhand"));
      Schema $$126 = pBuilder.addSchema(2568, V2568::new);
      pBuilder.addFixer(new AddNewChoices($$126, "Added Piglin Brute", References.ENTITY));
      Schema $$127 = pBuilder.addSchema(2571, V2571::new);
      pBuilder.addFixer(new AddNewChoices($$127, "Added Goat", References.ENTITY));
      Schema $$128 = pBuilder.addSchema(2679, SAME_NAMESPACED);
      pBuilder.addFixer(new CauldronRenameFix($$128, false));
      Schema $$129 = pBuilder.addSchema(2680, SAME_NAMESPACED);
      pBuilder.addFixer(ItemRenameFix.create($$129, "Renamed grass path item to dirt path", createRenamer("minecraft:grass_path", "minecraft:dirt_path")));
      pBuilder.addFixer(BlockRenameFix.create($$129, "Renamed grass path block to dirt path", createRenamer("minecraft:grass_path", "minecraft:dirt_path")));
      Schema $$130 = pBuilder.addSchema(2684, V2684::new);
      pBuilder.addFixer(new AddNewChoices($$130, "Added Sculk Sensor", References.BLOCK_ENTITY));
      Schema $$131 = pBuilder.addSchema(2686, V2686::new);
      pBuilder.addFixer(new AddNewChoices($$131, "Added Axolotl", References.ENTITY));
      Schema $$132 = pBuilder.addSchema(2688, V2688::new);
      pBuilder.addFixer(new AddNewChoices($$132, "Added Glow Squid", References.ENTITY));
      pBuilder.addFixer(new AddNewChoices($$132, "Added Glow Item Frame", References.ENTITY));
      Schema $$133 = pBuilder.addSchema(2690, SAME_NAMESPACED);
      ImmutableMap<String, String> $$134 = ImmutableMap.builder()
         .put("minecraft:weathered_copper_block", "minecraft:oxidized_copper_block")
         .put("minecraft:semi_weathered_copper_block", "minecraft:weathered_copper_block")
         .put("minecraft:lightly_weathered_copper_block", "minecraft:exposed_copper_block")
         .put("minecraft:weathered_cut_copper", "minecraft:oxidized_cut_copper")
         .put("minecraft:semi_weathered_cut_copper", "minecraft:weathered_cut_copper")
         .put("minecraft:lightly_weathered_cut_copper", "minecraft:exposed_cut_copper")
         .put("minecraft:weathered_cut_copper_stairs", "minecraft:oxidized_cut_copper_stairs")
         .put("minecraft:semi_weathered_cut_copper_stairs", "minecraft:weathered_cut_copper_stairs")
         .put("minecraft:lightly_weathered_cut_copper_stairs", "minecraft:exposed_cut_copper_stairs")
         .put("minecraft:weathered_cut_copper_slab", "minecraft:oxidized_cut_copper_slab")
         .put("minecraft:semi_weathered_cut_copper_slab", "minecraft:weathered_cut_copper_slab")
         .put("minecraft:lightly_weathered_cut_copper_slab", "minecraft:exposed_cut_copper_slab")
         .put("minecraft:waxed_semi_weathered_copper", "minecraft:waxed_weathered_copper")
         .put("minecraft:waxed_lightly_weathered_copper", "minecraft:waxed_exposed_copper")
         .put("minecraft:waxed_semi_weathered_cut_copper", "minecraft:waxed_weathered_cut_copper")
         .put("minecraft:waxed_lightly_weathered_cut_copper", "minecraft:waxed_exposed_cut_copper")
         .put("minecraft:waxed_semi_weathered_cut_copper_stairs", "minecraft:waxed_weathered_cut_copper_stairs")
         .put("minecraft:waxed_lightly_weathered_cut_copper_stairs", "minecraft:waxed_exposed_cut_copper_stairs")
         .put("minecraft:waxed_semi_weathered_cut_copper_slab", "minecraft:waxed_weathered_cut_copper_slab")
         .put("minecraft:waxed_lightly_weathered_cut_copper_slab", "minecraft:waxed_exposed_cut_copper_slab")
         .build();
      pBuilder.addFixer(ItemRenameFix.create($$133, "Renamed copper block items to new oxidized terms", createRenamer($$134)));
      pBuilder.addFixer(BlockRenameFix.create($$133, "Renamed copper blocks to new oxidized terms", createRenamer($$134)));
      Schema $$135 = pBuilder.addSchema(2691, SAME_NAMESPACED);
      ImmutableMap<String, String> $$136 = ImmutableMap.builder()
         .put("minecraft:waxed_copper", "minecraft:waxed_copper_block")
         .put("minecraft:oxidized_copper_block", "minecraft:oxidized_copper")
         .put("minecraft:weathered_copper_block", "minecraft:weathered_copper")
         .put("minecraft:exposed_copper_block", "minecraft:exposed_copper")
         .build();
      pBuilder.addFixer(ItemRenameFix.create($$135, "Rename copper item suffixes", createRenamer($$136)));
      pBuilder.addFixer(BlockRenameFix.create($$135, "Rename copper blocks suffixes", createRenamer($$136)));
      Schema $$137 = pBuilder.addSchema(2693, SAME_NAMESPACED);
      pBuilder.addFixer(new AddFlagIfNotPresentFix($$137, References.WORLD_GEN_SETTINGS, "has_increased_height_already", false));
      Schema $$138 = pBuilder.addSchema(2696, SAME_NAMESPACED);
      ImmutableMap<String, String> $$139 = ImmutableMap.builder()
         .put("minecraft:grimstone", "minecraft:deepslate")
         .put("minecraft:grimstone_slab", "minecraft:cobbled_deepslate_slab")
         .put("minecraft:grimstone_stairs", "minecraft:cobbled_deepslate_stairs")
         .put("minecraft:grimstone_wall", "minecraft:cobbled_deepslate_wall")
         .put("minecraft:polished_grimstone", "minecraft:polished_deepslate")
         .put("minecraft:polished_grimstone_slab", "minecraft:polished_deepslate_slab")
         .put("minecraft:polished_grimstone_stairs", "minecraft:polished_deepslate_stairs")
         .put("minecraft:polished_grimstone_wall", "minecraft:polished_deepslate_wall")
         .put("minecraft:grimstone_tiles", "minecraft:deepslate_tiles")
         .put("minecraft:grimstone_tile_slab", "minecraft:deepslate_tile_slab")
         .put("minecraft:grimstone_tile_stairs", "minecraft:deepslate_tile_stairs")
         .put("minecraft:grimstone_tile_wall", "minecraft:deepslate_tile_wall")
         .put("minecraft:grimstone_bricks", "minecraft:deepslate_bricks")
         .put("minecraft:grimstone_brick_slab", "minecraft:deepslate_brick_slab")
         .put("minecraft:grimstone_brick_stairs", "minecraft:deepslate_brick_stairs")
         .put("minecraft:grimstone_brick_wall", "minecraft:deepslate_brick_wall")
         .put("minecraft:chiseled_grimstone", "minecraft:chiseled_deepslate")
         .build();
      pBuilder.addFixer(ItemRenameFix.create($$138, "Renamed grimstone block items to deepslate", createRenamer($$139)));
      pBuilder.addFixer(BlockRenameFix.create($$138, "Renamed grimstone blocks to deepslate", createRenamer($$139)));
      Schema $$140 = pBuilder.addSchema(2700, SAME_NAMESPACED);
      pBuilder.addFixer(
         BlockRenameFix.create(
            $$140,
            "Renamed cave vines blocks",
            createRenamer(ImmutableMap.of("minecraft:cave_vines_head", "minecraft:cave_vines", "minecraft:cave_vines_body", "minecraft:cave_vines_plant"))
         )
      );
      Schema $$141 = pBuilder.addSchema(2701, SAME_NAMESPACED);
      pBuilder.addFixer(new SavedDataFeaturePoolElementFix($$141));
      Schema $$142 = pBuilder.addSchema(2702, SAME_NAMESPACED);
      pBuilder.addFixer(new AbstractArrowPickupFix($$142));
      Schema $$143 = pBuilder.addSchema(2704, V2704::new);
      pBuilder.addFixer(new AddNewChoices($$143, "Added Goat", References.ENTITY));
      Schema $$144 = pBuilder.addSchema(2707, V2707::new);
      pBuilder.addFixer(new AddNewChoices($$144, "Added Marker", References.ENTITY));
      pBuilder.addFixer(new AddFlagIfNotPresentFix($$144, References.WORLD_GEN_SETTINGS, "has_increased_height_already", true));
      Schema $$145 = pBuilder.addSchema(2710, SAME_NAMESPACED);
      pBuilder.addFixer(
         new StatsRenameFix($$145, "Renamed play_one_minute stat to play_time", ImmutableMap.of("minecraft:play_one_minute", "minecraft:play_time"))
      );
      Schema $$146 = pBuilder.addSchema(2717, SAME_NAMESPACED);
      pBuilder.addFixer(
         ItemRenameFix.create(
            $$146, "Rename azalea_leaves_flowers", createRenamer(ImmutableMap.of("minecraft:azalea_leaves_flowers", "minecraft:flowering_azalea_leaves"))
         )
      );
      pBuilder.addFixer(
         BlockRenameFix.create(
            $$146, "Rename azalea_leaves_flowers items", createRenamer(ImmutableMap.of("minecraft:azalea_leaves_flowers", "minecraft:flowering_azalea_leaves"))
         )
      );
      Schema $$147 = pBuilder.addSchema(2825, SAME_NAMESPACED);
      pBuilder.addFixer(new AddFlagIfNotPresentFix($$147, References.WORLD_GEN_SETTINGS, "has_increased_height_already", false));
      Schema $$148 = pBuilder.addSchema(2831, V2831::new);
      pBuilder.addFixer(new SpawnerDataFix($$148));
      Schema $$149 = pBuilder.addSchema(2832, V2832::new);
      pBuilder.addFixer(new WorldGenSettingsHeightAndBiomeFix($$149));
      pBuilder.addFixer(new ChunkHeightAndBiomeFix($$149));
      Schema $$150 = pBuilder.addSchema(2833, SAME_NAMESPACED);
      pBuilder.addFixer(new WorldGenSettingsDisallowOldCustomWorldsFix($$150));
      Schema $$151 = pBuilder.addSchema(2838, SAME_NAMESPACED);
      pBuilder.addFixer(new NamespacedTypeRenameFix($$151, "Caves and Cliffs biome renames", References.BIOME, createRenamer(CavesAndCliffsRenames.RENAMES)));
      Schema $$152 = pBuilder.addSchema(2841, SAME_NAMESPACED);
      pBuilder.addFixer(new ChunkProtoTickListFix($$152));
      Schema $$153 = pBuilder.addSchema(2842, V2842::new);
      pBuilder.addFixer(new ChunkRenamesFix($$153));
      Schema $$154 = pBuilder.addSchema(2843, SAME_NAMESPACED);
      pBuilder.addFixer(new OverreachingTickFix($$154));
      pBuilder.addFixer(
         new NamespacedTypeRenameFix($$154, "Remove Deep Warm Ocean", References.BIOME, createRenamer("minecraft:deep_warm_ocean", "minecraft:warm_ocean"))
      );
      Schema $$155 = pBuilder.addSchema(2846, SAME_NAMESPACED);
      pBuilder.addFixer(
         new AdvancementsRenameFix(
            $$155,
            false,
            "Rename some C&C part 2 advancements",
            createRenamer(
               ImmutableMap.of(
                  "minecraft:husbandry/play_jukebox_in_meadows",
                  "minecraft:adventure/play_jukebox_in_meadows",
                  "minecraft:adventure/caves_and_cliff",
                  "minecraft:adventure/fall_from_world_height",
                  "minecraft:adventure/ride_strider_in_overworld_lava",
                  "minecraft:nether/ride_strider_in_overworld_lava"
               )
            )
         )
      );
      Schema $$156 = pBuilder.addSchema(2852, SAME_NAMESPACED);
      pBuilder.addFixer(new WorldGenSettingsDisallowOldCustomWorldsFix($$156));
      Schema $$157 = pBuilder.addSchema(2967, SAME_NAMESPACED);
      pBuilder.addFixer(new StructureSettingsFlattenFix($$157));
      Schema $$158 = pBuilder.addSchema(2970, SAME_NAMESPACED);
      pBuilder.addFixer(new StructuresBecomeConfiguredFix($$158));
      Schema $$159 = pBuilder.addSchema(3076, V3076::new);
      pBuilder.addFixer(new AddNewChoices($$159, "Added Sculk Catalyst", References.BLOCK_ENTITY));
      Schema $$160 = pBuilder.addSchema(3077, SAME_NAMESPACED);
      pBuilder.addFixer(new ChunkDeleteIgnoredLightDataFix($$160));
      Schema $$161 = pBuilder.addSchema(3078, V3078::new);
      pBuilder.addFixer(new AddNewChoices($$161, "Added Frog", References.ENTITY));
      pBuilder.addFixer(new AddNewChoices($$161, "Added Tadpole", References.ENTITY));
      pBuilder.addFixer(new AddNewChoices($$161, "Added Sculk Shrieker", References.BLOCK_ENTITY));
      Schema $$162 = pBuilder.addSchema(3081, V3081::new);
      pBuilder.addFixer(new AddNewChoices($$162, "Added Warden", References.ENTITY));
      Schema $$163 = pBuilder.addSchema(3082, V3082::new);
      pBuilder.addFixer(new AddNewChoices($$163, "Added Chest Boat", References.ENTITY));
      Schema $$164 = pBuilder.addSchema(3083, V3083::new);
      pBuilder.addFixer(new AddNewChoices($$164, "Added Allay", References.ENTITY));
      Schema $$165 = pBuilder.addSchema(3084, SAME_NAMESPACED);
      pBuilder.addFixer(
         new NamespacedTypeRenameFix(
            $$165,
            "game_event_renames_3084",
            References.GAME_EVENT_NAME,
            createRenamer(
               ImmutableMap.builder()
                  .put("minecraft:block_press", "minecraft:block_activate")
                  .put("minecraft:block_switch", "minecraft:block_activate")
                  .put("minecraft:block_unpress", "minecraft:block_deactivate")
                  .put("minecraft:block_unswitch", "minecraft:block_deactivate")
                  .put("minecraft:drinking_finish", "minecraft:drink")
                  .put("minecraft:elytra_free_fall", "minecraft:elytra_glide")
                  .put("minecraft:entity_damaged", "minecraft:entity_damage")
                  .put("minecraft:entity_dying", "minecraft:entity_die")
                  .put("minecraft:entity_killed", "minecraft:entity_die")
                  .put("minecraft:mob_interact", "minecraft:entity_interact")
                  .put("minecraft:ravager_roar", "minecraft:entity_roar")
                  .put("minecraft:ring_bell", "minecraft:block_change")
                  .put("minecraft:shulker_close", "minecraft:container_close")
                  .put("minecraft:shulker_open", "minecraft:container_open")
                  .put("minecraft:wolf_shaking", "minecraft:entity_shake")
                  .build()
            )
         )
      );
      Schema $$166 = pBuilder.addSchema(3086, SAME_NAMESPACED);
      pBuilder.addFixer(
         new EntityVariantFix(
            $$166, "Change cat variant type", References.ENTITY, "minecraft:cat", "CatType", Util.make(new Int2ObjectOpenHashMap(), p_216528_ -> {
               p_216528_.defaultReturnValue("minecraft:tabby");
               p_216528_.put(0, "minecraft:tabby");
               p_216528_.put(1, "minecraft:black");
               p_216528_.put(2, "minecraft:red");
               p_216528_.put(3, "minecraft:siamese");
               p_216528_.put(4, "minecraft:british");
               p_216528_.put(5, "minecraft:calico");
               p_216528_.put(6, "minecraft:persian");
               p_216528_.put(7, "minecraft:ragdoll");
               p_216528_.put(8, "minecraft:white");
               p_216528_.put(9, "minecraft:jellie");
               p_216528_.put(10, "minecraft:all_black");
            })::get
         )
      );
      ImmutableMap<String, String> $$167 = ImmutableMap.builder()
         .put("textures/entity/cat/tabby.png", "minecraft:tabby")
         .put("textures/entity/cat/black.png", "minecraft:black")
         .put("textures/entity/cat/red.png", "minecraft:red")
         .put("textures/entity/cat/siamese.png", "minecraft:siamese")
         .put("textures/entity/cat/british_shorthair.png", "minecraft:british")
         .put("textures/entity/cat/calico.png", "minecraft:calico")
         .put("textures/entity/cat/persian.png", "minecraft:persian")
         .put("textures/entity/cat/ragdoll.png", "minecraft:ragdoll")
         .put("textures/entity/cat/white.png", "minecraft:white")
         .put("textures/entity/cat/jellie.png", "minecraft:jellie")
         .put("textures/entity/cat/all_black.png", "minecraft:all_black")
         .build();
      pBuilder.addFixer(
         new CriteriaRenameFix(
            $$166, "Migrate cat variant advancement", "minecraft:husbandry/complete_catalogue", p_216517_ -> (String)$$167.getOrDefault(p_216517_, p_216517_)
         )
      );
      Schema $$168 = pBuilder.addSchema(3087, SAME_NAMESPACED);
      pBuilder.addFixer(
         new EntityVariantFix(
            $$168, "Change frog variant type", References.ENTITY, "minecraft:frog", "Variant", Util.make(new Int2ObjectOpenHashMap(), p_216519_ -> {
               p_216519_.put(0, "minecraft:temperate");
               p_216519_.put(1, "minecraft:warm");
               p_216519_.put(2, "minecraft:cold");
            })::get
         )
      );
      Schema $$169 = pBuilder.addSchema(3090, SAME_NAMESPACED);
      pBuilder.addFixer(new EntityPaintingFieldsRenameFix($$169));
      Schema $$170 = pBuilder.addSchema(3093, SAME_NAMESPACED);
      pBuilder.addFixer(new EntityGoatMissingStateFix($$170));
      Schema $$171 = pBuilder.addSchema(3094, SAME_NAMESPACED);
      pBuilder.addFixer(new GoatHornIdFix($$171));
      Schema $$172 = pBuilder.addSchema(3097, SAME_NAMESPACED);
      pBuilder.addFixer(new FilteredBooksFix($$172));
      pBuilder.addFixer(new FilteredSignsFix($$172));
      Map<String, String> $$173 = Map.of("minecraft:british", "minecraft:british_shorthair");
      pBuilder.addFixer(new VariantRenameFix($$172, "Rename british shorthair", References.ENTITY, "minecraft:cat", $$173));
      pBuilder.addFixer(
         new CriteriaRenameFix(
            $$172,
            "Migrate cat variant advancement for british shorthair",
            "minecraft:husbandry/complete_catalogue",
            p_216531_ -> $$173.getOrDefault(p_216531_, p_216531_)
         )
      );
      pBuilder.addFixer(new PoiTypeRemoveFix($$172, "Remove unpopulated villager PoI types", Set.of("minecraft:unemployed", "minecraft:nitwit")::contains));
      Schema $$174 = pBuilder.addSchema(3108, SAME_NAMESPACED);
      pBuilder.addFixer(new BlendingDataRemoveFromNetherEndFix($$174));
      Schema $$175 = pBuilder.addSchema(3201, SAME_NAMESPACED);
      pBuilder.addFixer(new OptionsProgrammerArtFix($$175));
      Schema $$176 = pBuilder.addSchema(3202, V3202::new);
      pBuilder.addFixer(new AddNewChoices($$176, "Added Hanging Sign", References.BLOCK_ENTITY));
      Schema $$177 = pBuilder.addSchema(3203, V3203::new);
      pBuilder.addFixer(new AddNewChoices($$177, "Added Camel", References.ENTITY));
      Schema $$178 = pBuilder.addSchema(3204, V3204::new);
      pBuilder.addFixer(new AddNewChoices($$178, "Added Chiseled Bookshelf", References.BLOCK_ENTITY));
      Schema $$179 = pBuilder.addSchema(3209, SAME_NAMESPACED);
      pBuilder.addFixer(new ItemStackSpawnEggFix($$179, false, "minecraft:pig_spawn_egg"));
      Schema $$180 = pBuilder.addSchema(3214, SAME_NAMESPACED);
      pBuilder.addFixer(new OptionsAmbientOcclusionFix($$180));
      Schema $$181 = pBuilder.addSchema(3319, SAME_NAMESPACED);
      pBuilder.addFixer(new OptionsAccessibilityOnboardFix($$181));
      Schema $$182 = pBuilder.addSchema(3322, SAME_NAMESPACED);
      pBuilder.addFixer(new EffectDurationFix($$182));
      Schema $$183 = pBuilder.addSchema(3325, V3325::new);
      pBuilder.addFixer(new AddNewChoices($$183, "Added displays", References.ENTITY));
      Schema $$184 = pBuilder.addSchema(3326, V3326::new);
      pBuilder.addFixer(new AddNewChoices($$184, "Added Sniffer", References.ENTITY));
      Schema $$185 = pBuilder.addSchema(3327, V3327::new);
      pBuilder.addFixer(new AddNewChoices($$185, "Archaeology", References.BLOCK_ENTITY));
      Schema $$186 = pBuilder.addSchema(3328, V3328::new);
      pBuilder.addFixer(new AddNewChoices($$186, "Added interaction", References.ENTITY));
      Schema $$187 = pBuilder.addSchema(3438, V3438::new);
      pBuilder.addFixer(
         BlockEntityRenameFix.create(
            $$187, "Rename Suspicious Sand to Brushable Block", createRenamer("minecraft:suspicious_sand", "minecraft:brushable_block")
         )
      );
      pBuilder.addFixer(new EntityBrushableBlockFieldsRenameFix($$187));
      pBuilder.addFixer(
         ItemRenameFix.create(
            $$187,
            "Pottery shard renaming",
            createRenamer(
               ImmutableMap.of(
                  "minecraft:pottery_shard_archer",
                  "minecraft:archer_pottery_shard",
                  "minecraft:pottery_shard_prize",
                  "minecraft:prize_pottery_shard",
                  "minecraft:pottery_shard_arms_up",
                  "minecraft:arms_up_pottery_shard",
                  "minecraft:pottery_shard_skull",
                  "minecraft:skull_pottery_shard"
               )
            )
         )
      );
      pBuilder.addFixer(new AddNewChoices($$187, "Added calibrated sculk sensor", References.BLOCK_ENTITY));
      Schema $$188 = pBuilder.addSchema(3439, SAME_NAMESPACED);
      pBuilder.addFixer(new BlockEntitySignDoubleSidedEditableTextFix($$188, "Updated sign text format for Signs", "minecraft:sign"));
      pBuilder.addFixer(new BlockEntitySignDoubleSidedEditableTextFix($$188, "Updated sign text format for Hanging Signs", "minecraft:hanging_sign"));
      Schema $$189 = pBuilder.addSchema(3440, SAME_NAMESPACED);
      pBuilder.addFixer(
         new NamespacedTypeRenameFix(
            $$189,
            "Replace experimental 1.20 overworld",
            References.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST,
            createRenamer("minecraft:overworld_update_1_20", "minecraft:overworld")
         )
      );
      pBuilder.addFixer(new FeatureFlagRemoveFix($$189, "Remove 1.20 feature toggle", Set.of("minecraft:update_1_20")));
      Schema $$190 = pBuilder.addSchema(3441, SAME_NAMESPACED);
      pBuilder.addFixer(new BlendingDataFix($$190));
      Schema $$191 = pBuilder.addSchema(3447, SAME_NAMESPACED);
      pBuilder.addFixer(
         ItemRenameFix.create(
            $$191,
            "Pottery shard item renaming to Pottery sherd",
            createRenamer(
               Stream.of(
                     "minecraft:angler_pottery_shard",
                     "minecraft:archer_pottery_shard",
                     "minecraft:arms_up_pottery_shard",
                     "minecraft:blade_pottery_shard",
                     "minecraft:brewer_pottery_shard",
                     "minecraft:burn_pottery_shard",
                     "minecraft:danger_pottery_shard",
                     "minecraft:explorer_pottery_shard",
                     "minecraft:friend_pottery_shard",
                     "minecraft:heart_pottery_shard",
                     "minecraft:heartbreak_pottery_shard",
                     "minecraft:howl_pottery_shard",
                     "minecraft:miner_pottery_shard",
                     "minecraft:mourner_pottery_shard",
                     "minecraft:plenty_pottery_shard",
                     "minecraft:prize_pottery_shard",
                     "minecraft:sheaf_pottery_shard",
                     "minecraft:shelter_pottery_shard",
                     "minecraft:skull_pottery_shard",
                     "minecraft:snort_pottery_shard"
                  )
                  .collect(Collectors.toMap(Function.identity(), p_280993_ -> p_280993_.replace("_pottery_shard", "_pottery_sherd")))
            )
         )
      );
      Schema $$192 = pBuilder.addSchema(3448, V3448::new);
      pBuilder.addFixer(new DecoratedPotFieldRenameFix($$192));
      Schema $$193 = pBuilder.addSchema(3450, SAME_NAMESPACED);
      pBuilder.addFixer(
         new RemapChunkStatusFix(
            $$193,
            "Remove liquid_carvers and heightmap chunk statuses",
            createRenamer(Map.of("minecraft:liquid_carvers", "minecraft:carvers", "minecraft:heightmaps", "minecraft:spawn"))
         )
      );
      Schema $$194 = pBuilder.addSchema(3451, SAME_NAMESPACED);
      pBuilder.addFixer(new ChunkDeleteLightFix($$194));
      Schema $$195 = pBuilder.addSchema(3459, SAME_NAMESPACED);
      pBuilder.addFixer(new LegacyDragonFightFix($$195));
      Schema $$196 = pBuilder.addSchema(3564, SAME_NAMESPACED);
      pBuilder.addFixer(new DropInvalidSignDataFix($$196, "Drop invalid sign datafix data", "minecraft:sign"));
      pBuilder.addFixer(new DropInvalidSignDataFix($$196, "Drop invalid hanging sign datafix data", "minecraft:hanging_sign"));
      Schema $$197 = pBuilder.addSchema(3565, SAME_NAMESPACED);
      pBuilder.addFixer(new RandomSequenceSettingsFix($$197));
      Schema $$198 = pBuilder.addSchema(3566, SAME_NAMESPACED);
      pBuilder.addFixer(new ScoreboardDisplaySlotFix($$198));
      Schema $$199 = pBuilder.addSchema(3568, SAME_NAMESPACED);
      pBuilder.addFixer(new MobEffectIdFix($$199));
      Schema $$200 = pBuilder.addSchema(3682, V3682::new);
      pBuilder.addFixer(new AddNewChoices($$200, "Added Crafter", References.BLOCK_ENTITY));
      Schema $$201 = pBuilder.addSchema(3683, V3683::new);
      pBuilder.addFixer(new PrimedTntBlockStateFixer($$201));
      Schema $$202 = pBuilder.addSchema(3685, V3685::new);
      pBuilder.addFixer(new FixProjectileStoredItem($$202));
      Schema $$203 = pBuilder.addSchema(3689, V3689::new);
      pBuilder.addFixer(new AddNewChoices($$203, "Added Breeze", References.ENTITY));
      pBuilder.addFixer(new AddNewChoices($$203, "Added Trial Spawner", References.BLOCK_ENTITY));
      Schema $$204 = pBuilder.addSchema(3692, SAME_NAMESPACED);
      UnaryOperator<String> $$205 = createRenamer(Map.of("minecraft:grass", "minecraft:short_grass"));
      pBuilder.addFixer(BlockRenameFix.create($$204, "Rename grass block to short_grass", $$205));
      pBuilder.addFixer(ItemRenameFix.create($$204, "Rename grass item to short_grass", $$205));
      Schema $$206 = pBuilder.addSchema(3799, V3799::new);
      pBuilder.addFixer(new AddNewChoices($$206, "Added Armadillo", References.ENTITY));
      Schema $$207 = pBuilder.addSchema(3800, SAME_NAMESPACED);
      UnaryOperator<String> $$208 = createRenamer(Map.of("minecraft:scute", "minecraft:turtle_scute"));
      pBuilder.addFixer(ItemRenameFix.create($$207, "Rename scute item to turtle_scute", $$208));
      Schema $$209 = pBuilder.addSchema(3803, SAME_NAMESPACED);
      pBuilder.addFixer(new RenameEnchantmentsFix($$209, "Rename sweeping enchant to sweeping_edge", Map.of("minecraft:sweeping", "minecraft:sweeping_edge")));
      Schema $$210 = pBuilder.addSchema(3807, V3807::new);
      pBuilder.addFixer(new AddNewChoices($$210, "Added Vault", References.BLOCK_ENTITY));
      Schema $$211 = pBuilder.addSchema(3807, 1, SAME_NAMESPACED);
      pBuilder.addFixer(new MapBannerBlockPosFormatFix($$211));
      Schema $$212 = pBuilder.addSchema(3808, V3808::new);
      pBuilder.addFixer(new HorseBodyArmorItemFix($$212, "minecraft:horse", "ArmorItem", true));
      Schema $$213 = pBuilder.addSchema(3808, 1, V3808_1::new);
      pBuilder.addFixer(new HorseBodyArmorItemFix($$213, "minecraft:llama", "DecorItem", false));
      Schema $$214 = pBuilder.addSchema(3808, 2, V3808_2::new);
      pBuilder.addFixer(new HorseBodyArmorItemFix($$214, "minecraft:trader_llama", "DecorItem", false));
      Schema $$215 = pBuilder.addSchema(3809, SAME_NAMESPACED);
      pBuilder.addFixer(new ChestedHorsesInventoryZeroIndexingFix($$215));
      Schema $$216 = pBuilder.addSchema(3812, SAME_NAMESPACED);
      pBuilder.addFixer(new FixWolfHealth($$216));
      Schema $$217 = pBuilder.addSchema(3813, SAME_NAMESPACED);
      pBuilder.addFixer(new BlockPosFormatAndRenamesFix($$217));
      Schema $$218 = pBuilder.addSchema(3814, SAME_NAMESPACED);
      pBuilder.addFixer(
         new AttributesRename($$218, "Rename jump strength attribute", createRenamer("minecraft:horse.jump_strength", "minecraft:generic.jump_strength"))
      );
      Schema $$219 = pBuilder.addSchema(3816, V3816::new);
      pBuilder.addFixer(new AddNewChoices($$219, "Added Bogged", References.ENTITY));
      Schema $$220 = pBuilder.addSchema(3818, V3818::new);
      pBuilder.addFixer(new BeehiveFieldRenameFix($$220));
      pBuilder.addFixer(new EmptyItemInHotbarFix($$220));
      Schema $$221 = pBuilder.addSchema(3818, 1, SAME_NAMESPACED);
      pBuilder.addFixer(new BannerPatternFormatFix($$221));
      Schema $$222 = pBuilder.addSchema(3818, 2, SAME_NAMESPACED);
      pBuilder.addFixer(new TippedArrowPotionToItemFix($$222));
      Schema $$223 = pBuilder.addSchema(3818, 3, V3818_3::new);
      pBuilder.addFixer(new WriteAndReadFix($$223, "Inject data component types", References.DATA_COMPONENTS));
      Schema $$224 = pBuilder.addSchema(3818, 4, V3818_4::new);
      pBuilder.addFixer(new ParticleUnflatteningFix($$224));
      Schema $$225 = pBuilder.addSchema(3818, 5, V3818_5::new);
      pBuilder.addFixer(new ItemStackComponentizationFix($$225));
      Schema $$226 = pBuilder.addSchema(3818, 6, SAME_NAMESPACED);
      pBuilder.addFixer(new AreaEffectCloudPotionFix($$226));
      Schema $$227 = pBuilder.addSchema(3820, SAME_NAMESPACED);
      pBuilder.addFixer(new PlayerHeadBlockProfileFix($$227));
      pBuilder.addFixer(new LodestoneCompassComponentFix($$227));
      Schema $$228 = pBuilder.addSchema(3825, V3825::new);
      pBuilder.addFixer(new ItemStackCustomNameToOverrideComponentFix($$228));
      pBuilder.addFixer(new BannerEntityCustomNameToOverrideComponentFix($$228));
      pBuilder.addFixer(new TrialSpawnerConfigFix($$228));
      pBuilder.addFixer(new AddNewChoices($$228, "Added Ominous Item Spawner", References.ENTITY));
      Schema $$229 = pBuilder.addSchema(3828, SAME_NAMESPACED);
      pBuilder.addFixer(new EmptyItemInVillagerTradeFix($$229));
      Schema $$230 = pBuilder.addSchema(3833, SAME_NAMESPACED);
      pBuilder.addFixer(new RemoveEmptyItemInBrushableBlockFix($$230));
      Schema $$231 = pBuilder.addSchema(3938, V3938::new);
      pBuilder.addFixer(new ProjectileStoredWeaponFix($$231));
      Schema $$232 = pBuilder.addSchema(3939, SAME_NAMESPACED);
      pBuilder.addFixer(new FeatureFlagRemoveFix($$232, "Remove 1.21 feature toggle", Set.of("minecraft:update_1_21")));
      Schema $$233 = pBuilder.addSchema(3943, SAME_NAMESPACED);
      pBuilder.addFixer(new OptionsMenuBlurrinessFix($$233));
      Schema $$234 = pBuilder.addSchema(3945, SAME_NAMESPACED);
      pBuilder.addFixer(new AttributeModifierIdFix($$234));
      pBuilder.addFixer(new JukeboxTicksSinceSongStartedFix($$234));
   }

   private static UnaryOperator<String> createRenamerNoNamespace(Map<String, String> pRenameMap) {
      return p_216526_ -> pRenameMap.getOrDefault(p_216526_, p_216526_);
   }

   private static UnaryOperator<String> createRenamer(Map<String, String> pRenameMap) {
      return p_326540_ -> pRenameMap.getOrDefault(NamespacedSchema.ensureNamespaced(p_326540_), p_326540_);
   }

   private static UnaryOperator<String> createRenamer(String pOldName, String pNewName) {
      return p_326538_ -> Objects.equals(NamespacedSchema.ensureNamespaced(p_326538_), pOldName) ? pNewName : p_326538_;
   }
}
