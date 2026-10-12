package net.minecraft.world.entity.ai.behavior.declarative;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.datafixers.kinds.IdF;
import com.mojang.datafixers.kinds.K1;
import com.mojang.datafixers.kinds.OptionalBox;
import com.mojang.datafixers.util.Function3;
import com.mojang.datafixers.util.Function4;
import com.mojang.datafixers.util.Unit;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.OneShot;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public class BehaviorBuilder<E extends LivingEntity, M> implements App<BehaviorBuilder.Mu<E>, M> {
   private final BehaviorBuilder.TriggerWithResult<E, M> trigger;

   public static <E extends LivingEntity, M> BehaviorBuilder<E, M> unbox(App<BehaviorBuilder.Mu<E>, M> pApp) {
      return (BehaviorBuilder<E, M>)pApp;
   }

   public static <E extends LivingEntity> BehaviorBuilder.Instance<E> instance() {
      return new BehaviorBuilder.Instance<>();
   }

   public static <E extends LivingEntity> OneShot<E> create(
      Function<BehaviorBuilder.Instance<E>, ? extends App<BehaviorBuilder.Mu<E>, Trigger<E>>> pInitializer
   ) {
      final BehaviorBuilder.TriggerWithResult<E, Trigger<E>> $$1 = get((App<BehaviorBuilder.Mu<E>, Trigger<E>>)pInitializer.apply(instance()));
      return new OneShot<E>() {
         @Override
         public boolean trigger(ServerLevel p_259385_, E p_260003_, long p_259194_) {
            Trigger<E> $$3 = $$1.tryTrigger(p_259385_, p_260003_, p_259194_);
            return $$3 == null ? false : $$3.trigger(p_259385_, p_260003_, p_259194_);
         }

         @Override
         public String debugString() {
            return "OneShot[" + $$1.debugString() + "]";
         }

         @Override
         public String toString() {
            return this.debugString();
         }
      };
   }

   public static <E extends LivingEntity> OneShot<E> sequence(Trigger<? super E> pPredicateTrigger, Trigger<? super E> pTrigger) {
      return create(p_259495_ -> p_259495_.group(p_259495_.ifTriggered(pPredicateTrigger)).apply(p_259495_, p_260322_ -> pTrigger::trigger));
   }

   public static <E extends LivingEntity> OneShot<E> triggerIf(Predicate<E> pPredicate, OneShot<? super E> pTrigger) {
      return sequence(triggerIf(pPredicate), pTrigger);
   }

   public static <E extends LivingEntity> OneShot<E> triggerIf(Predicate<E> pPredicate) {
      return create(p_260353_ -> p_260353_.point((p_259280_, p_259428_, p_259845_) -> pPredicate.test(p_259428_)));
   }

   public static <E extends LivingEntity> OneShot<E> triggerIf(BiPredicate<ServerLevel, E> pPredicate) {
      return create(p_260191_ -> p_260191_.point((p_259079_, p_259093_, p_260140_) -> pPredicate.test(p_259079_, p_259093_)));
   }

   static <E extends LivingEntity, M> BehaviorBuilder.TriggerWithResult<E, M> get(App<BehaviorBuilder.Mu<E>, M> pApp) {
      return unbox(pApp).trigger;
   }

   BehaviorBuilder(BehaviorBuilder.TriggerWithResult<E, M> pTrigger) {
      this.trigger = pTrigger;
   }

   static <E extends LivingEntity, M> BehaviorBuilder<E, M> create(BehaviorBuilder.TriggerWithResult<E, M> pTrigger) {
      return new BehaviorBuilder<>(pTrigger);
   }

   static final class Constant<E extends LivingEntity, A> extends BehaviorBuilder<E, A> {
      Constant(A pValue) {
         this(pValue, () -> "C[" + pValue + "]");
      }

      Constant(final A pValue, final Supplier<String> pName) {
         super(new BehaviorBuilder.TriggerWithResult<E, A>() {
            @Override
            public A tryTrigger(ServerLevel p_259561_, E p_259467_, long p_259297_) {
               return pValue;
            }

            @Override
            public String debugString() {
               return pName.get();
            }

            @Override
            public String toString() {
               return this.debugString();
            }
         });
      }
   }

   public static final class Instance<E extends LivingEntity> implements Applicative<BehaviorBuilder.Mu<E>, BehaviorBuilder.Instance.Mu<E>> {
      public <Value> Optional<Value> tryGet(MemoryAccessor<com.mojang.datafixers.kinds.OptionalBox.Mu, Value> pMemory) {
         return OptionalBox.unbox(pMemory.value());
      }

      public <Value> Value get(MemoryAccessor<com.mojang.datafixers.kinds.IdF.Mu, Value> pMemory) {
         return (Value)IdF.get(pMemory.value());
      }

      public <Value> BehaviorBuilder<E, MemoryAccessor<com.mojang.datafixers.kinds.OptionalBox.Mu, Value>> registered(MemoryModuleType<Value> pMemoryType) {
         return new BehaviorBuilder.PureMemory<>(new MemoryCondition.Registered<>(pMemoryType));
      }

      public <Value> BehaviorBuilder<E, MemoryAccessor<com.mojang.datafixers.kinds.IdF.Mu, Value>> present(MemoryModuleType<Value> pMemoryType) {
         return new BehaviorBuilder.PureMemory<>(new MemoryCondition.Present<>(pMemoryType));
      }

      public <Value> BehaviorBuilder<E, MemoryAccessor<com.mojang.datafixers.kinds.Const.Mu<Unit>, Value>> absent(MemoryModuleType<Value> pMemoryType) {
         return new BehaviorBuilder.PureMemory<>(new MemoryCondition.Absent<>(pMemoryType));
      }

      public BehaviorBuilder<E, Unit> ifTriggered(Trigger<? super E> pTrigger) {
         return new BehaviorBuilder.TriggerWrapper<>(pTrigger);
      }

      public <A> BehaviorBuilder<E, A> point(A pValue) {
         return new BehaviorBuilder.Constant<>(pValue);
      }

      public <A> BehaviorBuilder<E, A> point(Supplier<String> pName, A pValue) {
         return new BehaviorBuilder.Constant<>(pValue, pName);
      }

      public <A, R> Function<App<BehaviorBuilder.Mu<E>, A>, App<BehaviorBuilder.Mu<E>, R>> lift1(App<BehaviorBuilder.Mu<E>, Function<A, R>> pBehavior) {
         return p_259751_ -> {
            final BehaviorBuilder.TriggerWithResult<E, A> $$2 = (BehaviorBuilder.TriggerWithResult<E, A>)BehaviorBuilder.get(
               (App<BehaviorBuilder.Mu<E>, M>)p_259751_
            );
            final BehaviorBuilder.TriggerWithResult<E, Function<A, R>> $$3 = BehaviorBuilder.get(pBehavior);
            return BehaviorBuilder.create(new BehaviorBuilder.TriggerWithResult<E, R>() {
               @Override
               public R tryTrigger(ServerLevel p_259603_, E p_260233_, long p_259654_) {
                  A $$3 = (A)$$2.tryTrigger(p_259603_, p_260233_, p_259654_);
                  if ($$3 == null) {
                     return null;
                  } else {
                     Function<A, R> $$4 = (Function<A, R>)$$3.tryTrigger(p_259603_, p_260233_, p_259654_);
                     return (R)($$4 == null ? null : $$4.apply($$3));
                  }
               }

               @Override
               public String debugString() {
                  return $$3.debugString() + " * " + $$2.debugString();
               }

               @Override
               public String toString() {
                  return this.debugString();
               }
            });
         };
      }

      public <T, R> BehaviorBuilder<E, R> map(final Function<? super T, ? extends R> pMapper, App<BehaviorBuilder.Mu<E>, T> pBehavior) {
         final BehaviorBuilder.TriggerWithResult<E, T> $$2 = (BehaviorBuilder.TriggerWithResult<E, T>)BehaviorBuilder.get(
            (App<BehaviorBuilder.Mu<E>, M>)pBehavior
         );
         return BehaviorBuilder.create(new BehaviorBuilder.TriggerWithResult<E, R>() {
            @Override
            public R tryTrigger(ServerLevel p_259755_, E p_259656_, long p_259300_) {
               T $$3 = $$2.tryTrigger(p_259755_, p_259656_, p_259300_);
               return (R)($$3 == null ? null : pMapper.apply($$3));
            }

            @Override
            public String debugString() {
               return $$2.debugString() + ".map[" + pMapper + "]";
            }

            @Override
            public String toString() {
               return this.debugString();
            }
         });
      }

      public <A, B, R> BehaviorBuilder<E, R> ap2(
         App<BehaviorBuilder.Mu<E>, BiFunction<A, B, R>> pMapper, App<BehaviorBuilder.Mu<E>, A> pBehavior1, App<BehaviorBuilder.Mu<E>, B> pBehavior2
      ) {
         final BehaviorBuilder.TriggerWithResult<E, A> $$3 = (BehaviorBuilder.TriggerWithResult<E, A>)BehaviorBuilder.get(
            (App<BehaviorBuilder.Mu<E>, M>)pBehavior1
         );
         final BehaviorBuilder.TriggerWithResult<E, B> $$4 = (BehaviorBuilder.TriggerWithResult<E, B>)BehaviorBuilder.get(
            (App<BehaviorBuilder.Mu<E>, M>)pBehavior2
         );
         final BehaviorBuilder.TriggerWithResult<E, BiFunction<A, B, R>> $$5 = BehaviorBuilder.get(pMapper);
         return BehaviorBuilder.create(new BehaviorBuilder.TriggerWithResult<E, R>() {
            @Override
            public R tryTrigger(ServerLevel p_259274_, E p_259817_, long p_259820_) {
               A $$3 = $$3.tryTrigger(p_259274_, p_259817_, p_259820_);
               if ($$3 == null) {
                  return null;
               } else {
                  B $$4 = $$4.tryTrigger(p_259274_, p_259817_, p_259820_);
                  if ($$4 == null) {
                     return null;
                  } else {
                     BiFunction<A, B, R> $$5 = $$5.tryTrigger(p_259274_, p_259817_, p_259820_);
                     return $$5 == null ? null : $$5.apply($$3, $$4);
                  }
               }
            }

            @Override
            public String debugString() {
               return $$5.debugString() + " * " + $$3.debugString() + " * " + $$4.debugString();
            }

            @Override
            public String toString() {
               return this.debugString();
            }
         });
      }

      public <T1, T2, T3, R> BehaviorBuilder<E, R> ap3(
         App<BehaviorBuilder.Mu<E>, Function3<T1, T2, T3, R>> pMapper,
         App<BehaviorBuilder.Mu<E>, T1> pBehavior1,
         App<BehaviorBuilder.Mu<E>, T2> pBehavior2,
         App<BehaviorBuilder.Mu<E>, T3> pBehavior3
      ) {
         final BehaviorBuilder.TriggerWithResult<E, T1> $$4 = (BehaviorBuilder.TriggerWithResult<E, T1>)BehaviorBuilder.get(
            (App<BehaviorBuilder.Mu<E>, M>)pBehavior1
         );
         final BehaviorBuilder.TriggerWithResult<E, T2> $$5 = (BehaviorBuilder.TriggerWithResult<E, T2>)BehaviorBuilder.get(
            (App<BehaviorBuilder.Mu<E>, M>)pBehavior2
         );
         final BehaviorBuilder.TriggerWithResult<E, T3> $$6 = (BehaviorBuilder.TriggerWithResult<E, T3>)BehaviorBuilder.get(
            (App<BehaviorBuilder.Mu<E>, M>)pBehavior3
         );
         final BehaviorBuilder.TriggerWithResult<E, Function3<T1, T2, T3, R>> $$7 = BehaviorBuilder.get(pMapper);
         return BehaviorBuilder.create(new BehaviorBuilder.TriggerWithResult<E, R>() {
            @Override
            public R tryTrigger(ServerLevel p_259096_, E p_260221_, long p_259035_) {
               T1 $$3 = $$4.tryTrigger(p_259096_, p_260221_, p_259035_);
               if ($$3 == null) {
                  return null;
               } else {
                  T2 $$4 = $$5.tryTrigger(p_259096_, p_260221_, p_259035_);
                  if ($$4 == null) {
                     return null;
                  } else {
                     T3 $$5 = $$6.tryTrigger(p_259096_, p_260221_, p_259035_);
                     if ($$5 == null) {
                        return null;
                     } else {
                        Function3<T1, T2, T3, R> $$6 = $$7.tryTrigger(p_259096_, p_260221_, p_259035_);
                        return (R)($$6 == null ? null : $$6.apply($$3, $$4, $$5));
                     }
                  }
               }
            }

            @Override
            public String debugString() {
               return $$7.debugString() + " * " + $$4.debugString() + " * " + $$5.debugString() + " * " + $$6.debugString();
            }

            @Override
            public String toString() {
               return this.debugString();
            }
         });
      }

      public <T1, T2, T3, T4, R> BehaviorBuilder<E, R> ap4(
         App<BehaviorBuilder.Mu<E>, Function4<T1, T2, T3, T4, R>> pMapper,
         App<BehaviorBuilder.Mu<E>, T1> pBehavior1,
         App<BehaviorBuilder.Mu<E>, T2> pBehavior2,
         App<BehaviorBuilder.Mu<E>, T3> pBehavior3,
         App<BehaviorBuilder.Mu<E>, T4> pBehavior4
      ) {
         final BehaviorBuilder.TriggerWithResult<E, T1> $$5 = (BehaviorBuilder.TriggerWithResult<E, T1>)BehaviorBuilder.get(
            (App<BehaviorBuilder.Mu<E>, M>)pBehavior1
         );
         final BehaviorBuilder.TriggerWithResult<E, T2> $$6 = (BehaviorBuilder.TriggerWithResult<E, T2>)BehaviorBuilder.get(
            (App<BehaviorBuilder.Mu<E>, M>)pBehavior2
         );
         final BehaviorBuilder.TriggerWithResult<E, T3> $$7 = (BehaviorBuilder.TriggerWithResult<E, T3>)BehaviorBuilder.get(
            (App<BehaviorBuilder.Mu<E>, M>)pBehavior3
         );
         final BehaviorBuilder.TriggerWithResult<E, T4> $$8 = (BehaviorBuilder.TriggerWithResult<E, T4>)BehaviorBuilder.get(
            (App<BehaviorBuilder.Mu<E>, M>)pBehavior4
         );
         final BehaviorBuilder.TriggerWithResult<E, Function4<T1, T2, T3, T4, R>> $$9 = BehaviorBuilder.get(pMapper);
         return BehaviorBuilder.create(new BehaviorBuilder.TriggerWithResult<E, R>() {
            @Override
            public R tryTrigger(ServerLevel p_259537_, E p_259581_, long p_259423_) {
               T1 $$3 = $$5.tryTrigger(p_259537_, p_259581_, p_259423_);
               if ($$3 == null) {
                  return null;
               } else {
                  T2 $$4 = $$6.tryTrigger(p_259537_, p_259581_, p_259423_);
                  if ($$4 == null) {
                     return null;
                  } else {
                     T3 $$5 = $$7.tryTrigger(p_259537_, p_259581_, p_259423_);
                     if ($$5 == null) {
                        return null;
                     } else {
                        T4 $$6 = $$8.tryTrigger(p_259537_, p_259581_, p_259423_);
                        if ($$6 == null) {
                           return null;
                        } else {
                           Function4<T1, T2, T3, T4, R> $$7 = $$9.tryTrigger(p_259537_, p_259581_, p_259423_);
                           return (R)($$7 == null ? null : $$7.apply($$3, $$4, $$5, $$6));
                        }
                     }
                  }
               }
            }

            @Override
            public String debugString() {
               return $$9.debugString() + " * " + $$5.debugString() + " * " + $$6.debugString() + " * " + $$7.debugString() + " * " + $$8.debugString();
            }

            @Override
            public String toString() {
               return this.debugString();
            }
         });
      }

      static final class Mu<E extends LivingEntity> implements com.mojang.datafixers.kinds.Applicative.Mu {
         private Mu() {
         }
      }
   }

   public static final class Mu<E extends LivingEntity> implements K1 {
   }

   static final class PureMemory<E extends LivingEntity, F extends K1, Value> extends BehaviorBuilder<E, MemoryAccessor<F, Value>> {
      PureMemory(final MemoryCondition<F, Value> pMemoryCondition) {
         super(new BehaviorBuilder.TriggerWithResult<E, MemoryAccessor<F, Value>>() {
            public MemoryAccessor<F, Value> tryTrigger(ServerLevel p_259899_, E p_259558_, long p_259793_) {
               Brain<?> $$3 = p_259558_.getBrain();
               Optional<Value> $$4 = $$3.getMemoryInternal(pMemoryCondition.memory());
               return $$4 == null ? null : pMemoryCondition.createAccessor($$3, $$4);
            }

            @Override
            public String debugString() {
               return "M[" + pMemoryCondition + "]";
            }

            @Override
            public String toString() {
               return this.debugString();
            }
         });
      }
   }

   interface TriggerWithResult<E extends LivingEntity, R> {
      @Nullable
      R tryTrigger(ServerLevel pLevel, E pEntity, long pGameTime);

      String debugString();
   }

   static final class TriggerWrapper<E extends LivingEntity> extends BehaviorBuilder<E, Unit> {
      TriggerWrapper(final Trigger<? super E> pTrigger) {
         super(new BehaviorBuilder.TriggerWithResult<E, Unit>() {
            @Nullable
            public Unit tryTrigger(ServerLevel p_259397_, E p_260169_, long p_259155_) {
               return pTrigger.trigger(p_259397_, p_260169_, p_259155_) ? Unit.INSTANCE : null;
            }

            @Override
            public String debugString() {
               return "T[" + pTrigger + "]";
            }
         });
      }
   }
}
