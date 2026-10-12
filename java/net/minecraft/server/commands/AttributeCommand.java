package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.Dynamic3CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public class AttributeCommand {
   private static final DynamicCommandExceptionType ERROR_NOT_LIVING_ENTITY = new DynamicCommandExceptionType(
      p_308625_ -> Component.translatableEscape("commands.attribute.failed.entity", p_308625_)
   );
   private static final Dynamic2CommandExceptionType ERROR_NO_SUCH_ATTRIBUTE = new Dynamic2CommandExceptionType(
      (p_308616_, p_308617_) -> Component.translatableEscape("commands.attribute.failed.no_attribute", p_308616_, p_308617_)
   );
   private static final Dynamic3CommandExceptionType ERROR_NO_SUCH_MODIFIER = new Dynamic3CommandExceptionType(
      (p_308629_, p_308630_, p_308631_) -> Component.translatableEscape("commands.attribute.failed.no_modifier", p_308630_, p_308629_, p_308631_)
   );
   private static final Dynamic3CommandExceptionType ERROR_MODIFIER_ALREADY_PRESENT = new Dynamic3CommandExceptionType(
      (p_308626_, p_308627_, p_308628_) -> Component.translatableEscape("commands.attribute.failed.modifier_already_present", p_308628_, p_308627_, p_308626_)
   );

   public static void register(CommandDispatcher<CommandSourceStack> pDispatcher, CommandBuildContext pContext) {
      pDispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("attribute").requires(p_212441_ -> p_212441_.hasPermission(2)))
            .then(
               Commands.argument("target", EntityArgument.entity())
                  .then(
                     ((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument(
                                 "attribute", ResourceArgument.resource(pContext, Registries.ATTRIBUTE)
                              )
                              .then(
                                 ((LiteralArgumentBuilder)Commands.literal("get")
                                       .executes(
                                          p_248109_ -> getAttributeValue(
                                                (CommandSourceStack)p_248109_.getSource(),
                                                EntityArgument.getEntity(p_248109_, "target"),
                                                ResourceArgument.getAttribute(p_248109_, "attribute"),
                                                1.0
                                             )
                                       ))
                                    .then(
                                       Commands.argument("scale", DoubleArgumentType.doubleArg())
                                          .executes(
                                             p_248104_ -> getAttributeValue(
                                                   (CommandSourceStack)p_248104_.getSource(),
                                                   EntityArgument.getEntity(p_248104_, "target"),
                                                   ResourceArgument.getAttribute(p_248104_, "attribute"),
                                                   DoubleArgumentType.getDouble(p_248104_, "scale")
                                                )
                                          )
                                    )
                              ))
                           .then(
                              ((LiteralArgumentBuilder)Commands.literal("base")
                                    .then(
                                       Commands.literal("set")
                                          .then(
                                             Commands.argument("value", DoubleArgumentType.doubleArg())
                                                .executes(
                                                   p_248102_ -> setAttributeBase(
                                                         (CommandSourceStack)p_248102_.getSource(),
                                                         EntityArgument.getEntity(p_248102_, "target"),
                                                         ResourceArgument.getAttribute(p_248102_, "attribute"),
                                                         DoubleArgumentType.getDouble(p_248102_, "value")
                                                      )
                                                )
                                          )
                                    ))
                                 .then(
                                    ((LiteralArgumentBuilder)Commands.literal("get")
                                          .executes(
                                             p_248112_ -> getAttributeBase(
                                                   (CommandSourceStack)p_248112_.getSource(),
                                                   EntityArgument.getEntity(p_248112_, "target"),
                                                   ResourceArgument.getAttribute(p_248112_, "attribute"),
                                                   1.0
                                                )
                                          ))
                                       .then(
                                          Commands.argument("scale", DoubleArgumentType.doubleArg())
                                             .executes(
                                                p_248106_ -> getAttributeBase(
                                                      (CommandSourceStack)p_248106_.getSource(),
                                                      EntityArgument.getEntity(p_248106_, "target"),
                                                      ResourceArgument.getAttribute(p_248106_, "attribute"),
                                                      DoubleArgumentType.getDouble(p_248106_, "scale")
                                                   )
                                             )
                                       )
                                 )
                           ))
                        .then(
                           ((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("modifier")
                                    .then(
                                       Commands.literal("add")
                                          .then(
                                             Commands.argument("id", ResourceLocationArgument.id())
                                                .then(
                                                   ((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument(
                                                               "value", DoubleArgumentType.doubleArg()
                                                            )
                                                            .then(
                                                               Commands.literal("add_value")
                                                                  .executes(
                                                                     p_341134_ -> addModifier(
                                                                           (CommandSourceStack)p_341134_.getSource(),
                                                                           EntityArgument.getEntity(p_341134_, "target"),
                                                                           ResourceArgument.getAttribute(p_341134_, "attribute"),
                                                                           ResourceLocationArgument.getId(p_341134_, "id"),
                                                                           DoubleArgumentType.getDouble(p_341134_, "value"),
                                                                           AttributeModifier.Operation.ADD_VALUE
                                                                        )
                                                                  )
                                                            ))
                                                         .then(
                                                            Commands.literal("add_multiplied_base")
                                                               .executes(
                                                                  p_341141_ -> addModifier(
                                                                        (CommandSourceStack)p_341141_.getSource(),
                                                                        EntityArgument.getEntity(p_341141_, "target"),
                                                                        ResourceArgument.getAttribute(p_341141_, "attribute"),
                                                                        ResourceLocationArgument.getId(p_341141_, "id"),
                                                                        DoubleArgumentType.getDouble(p_341141_, "value"),
                                                                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                                                                     )
                                                               )
                                                         ))
                                                      .then(
                                                         Commands.literal("add_multiplied_total")
                                                            .executes(
                                                               p_341139_ -> addModifier(
                                                                     (CommandSourceStack)p_341139_.getSource(),
                                                                     EntityArgument.getEntity(p_341139_, "target"),
                                                                     ResourceArgument.getAttribute(p_341139_, "attribute"),
                                                                     ResourceLocationArgument.getId(p_341139_, "id"),
                                                                     DoubleArgumentType.getDouble(p_341139_, "value"),
                                                                     AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                                                                  )
                                                            )
                                                      )
                                                )
                                          )
                                    ))
                                 .then(
                                    Commands.literal("remove")
                                       .then(
                                          Commands.argument("id", ResourceLocationArgument.id())
                                             .executes(
                                                p_341140_ -> removeModifier(
                                                      (CommandSourceStack)p_341140_.getSource(),
                                                      EntityArgument.getEntity(p_341140_, "target"),
                                                      ResourceArgument.getAttribute(p_341140_, "attribute"),
                                                      ResourceLocationArgument.getId(p_341140_, "id")
                                                   )
                                             )
                                       )
                                 ))
                              .then(
                                 Commands.literal("value")
                                    .then(
                                       Commands.literal("get")
                                          .then(
                                             ((RequiredArgumentBuilder)Commands.argument("id", ResourceLocationArgument.id())
                                                   .executes(
                                                      p_341142_ -> getAttributeModifier(
                                                            (CommandSourceStack)p_341142_.getSource(),
                                                            EntityArgument.getEntity(p_341142_, "target"),
                                                            ResourceArgument.getAttribute(p_341142_, "attribute"),
                                                            ResourceLocationArgument.getId(p_341142_, "id"),
                                                            1.0
                                                         )
                                                   ))
                                                .then(
                                                   Commands.argument("scale", DoubleArgumentType.doubleArg())
                                                      .executes(
                                                         p_341135_ -> getAttributeModifier(
                                                               (CommandSourceStack)p_341135_.getSource(),
                                                               EntityArgument.getEntity(p_341135_, "target"),
                                                               ResourceArgument.getAttribute(p_341135_, "attribute"),
                                                               ResourceLocationArgument.getId(p_341135_, "id"),
                                                               DoubleArgumentType.getDouble(p_341135_, "scale")
                                                            )
                                                      )
                                                )
                                          )
                                    )
                              )
                        )
                  )
            )
      );
   }

   private static AttributeInstance getAttributeInstance(Entity pEntity, Holder<Attribute> pAttribute) throws CommandSyntaxException {
      AttributeInstance $$2 = getLivingEntity(pEntity).getAttributes().getInstance(pAttribute);
      if ($$2 == null) {
         throw ERROR_NO_SUCH_ATTRIBUTE.create(pEntity.getName(), getAttributeDescription(pAttribute));
      } else {
         return $$2;
      }
   }

   private static LivingEntity getLivingEntity(Entity pTarget) throws CommandSyntaxException {
      if (!(pTarget instanceof LivingEntity)) {
         throw ERROR_NOT_LIVING_ENTITY.create(pTarget.getName());
      } else {
         return (LivingEntity)pTarget;
      }
   }

   private static LivingEntity getEntityWithAttribute(Entity pEntity, Holder<Attribute> pAttribute) throws CommandSyntaxException {
      LivingEntity $$2 = getLivingEntity(pEntity);
      if (!$$2.getAttributes().hasAttribute(pAttribute)) {
         throw ERROR_NO_SUCH_ATTRIBUTE.create(pEntity.getName(), getAttributeDescription(pAttribute));
      } else {
         return $$2;
      }
   }

   private static int getAttributeValue(CommandSourceStack pSource, Entity pEntity, Holder<Attribute> pAttribute, double pScale) throws CommandSyntaxException {
      LivingEntity $$4 = getEntityWithAttribute(pEntity, pAttribute);
      double $$5 = $$4.getAttributeValue(pAttribute);
      pSource.sendSuccess(
         () -> Component.translatable("commands.attribute.value.get.success", getAttributeDescription(pAttribute), pEntity.getName(), $$5), false
      );
      return (int)($$5 * pScale);
   }

   private static int getAttributeBase(CommandSourceStack pSource, Entity pEntity, Holder<Attribute> pAttribute, double pScale) throws CommandSyntaxException {
      LivingEntity $$4 = getEntityWithAttribute(pEntity, pAttribute);
      double $$5 = $$4.getAttributeBaseValue(pAttribute);
      pSource.sendSuccess(
         () -> Component.translatable("commands.attribute.base_value.get.success", getAttributeDescription(pAttribute), pEntity.getName(), $$5), false
      );
      return (int)($$5 * pScale);
   }

   private static int getAttributeModifier(CommandSourceStack pSource, Entity pEntity, Holder<Attribute> pAttribute, ResourceLocation pId, double pScale) throws CommandSyntaxException {
      LivingEntity $$5 = getEntityWithAttribute(pEntity, pAttribute);
      AttributeMap $$6 = $$5.getAttributes();
      if (!$$6.hasModifier(pAttribute, pId)) {
         throw ERROR_NO_SUCH_MODIFIER.create(pEntity.getName(), getAttributeDescription(pAttribute), pId);
      } else {
         double $$7 = $$6.getModifierValue(pAttribute, pId);
         pSource.sendSuccess(
            () -> Component.translatable(
                  "commands.attribute.modifier.value.get.success", Component.translationArg(pId), getAttributeDescription(pAttribute), pEntity.getName(), $$7
               ),
            false
         );
         return (int)($$7 * pScale);
      }
   }

   private static int setAttributeBase(CommandSourceStack pSource, Entity pEntity, Holder<Attribute> pAttribute, double pValue) throws CommandSyntaxException {
      getAttributeInstance(pEntity, pAttribute).setBaseValue(pValue);
      pSource.sendSuccess(
         () -> Component.translatable("commands.attribute.base_value.set.success", getAttributeDescription(pAttribute), pEntity.getName(), pValue), false
      );
      return 1;
   }

   private static int addModifier(
      CommandSourceStack pSource, Entity pEntity, Holder<Attribute> pAttribute, ResourceLocation pId, double pAmount, AttributeModifier.Operation pOperation
   ) throws CommandSyntaxException {
      AttributeInstance $$6 = getAttributeInstance(pEntity, pAttribute);
      AttributeModifier $$7 = new AttributeModifier(pId, pAmount, pOperation);
      if ($$6.hasModifier(pId)) {
         throw ERROR_MODIFIER_ALREADY_PRESENT.create(pEntity.getName(), getAttributeDescription(pAttribute), pId);
      } else {
         $$6.addPermanentModifier($$7);
         pSource.sendSuccess(
            () -> Component.translatable(
                  "commands.attribute.modifier.add.success", Component.translationArg(pId), getAttributeDescription(pAttribute), pEntity.getName()
               ),
            false
         );
         return 1;
      }
   }

   private static int removeModifier(CommandSourceStack pSource, Entity pEntity, Holder<Attribute> pAttribute, ResourceLocation pId) throws CommandSyntaxException {
      AttributeInstance $$4 = getAttributeInstance(pEntity, pAttribute);
      if ($$4.removeModifier(pId)) {
         pSource.sendSuccess(
            () -> Component.translatable(
                  "commands.attribute.modifier.remove.success", Component.translationArg(pId), getAttributeDescription(pAttribute), pEntity.getName()
               ),
            false
         );
         return 1;
      } else {
         throw ERROR_NO_SUCH_MODIFIER.create(pEntity.getName(), getAttributeDescription(pAttribute), pId);
      }
   }

   private static Component getAttributeDescription(Holder<Attribute> pAttribute) {
      return Component.translatable(pAttribute.value().getDescriptionId());
   }
}
