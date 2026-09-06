package dev.sealbreaker.bosses.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Spike S2: a two-cube GeckoLib entity with an idle loop and an attack. The attack is server state: right-clicking
 * (or {@link #attack()} from a command or a test) sets a synced flag for {@link #ATTACK_TICKS}; every client's
 * animation controller reads that flag and plays the attack, so all clients see the same thing at the same tick.
 * GeckoLib's own {@code triggerAnim} packet is the alternative; the synced flag is chosen because the server
 * can be game-tested and the flag is what the boss AI will set anyway. Not content: the first boss gets its own
 * design pass (PRD 3.2, docs/design/).
 */
public final class SpikeDummy extends PathfinderMob implements GeoEntity {
    /** How long the attack flag stays up; the attack animation is 20 ticks long, so this is data-shaped, not tuned. */
    public static final int ATTACK_TICKS = 20;
    private static final EntityDataAccessor<Boolean> ATTACKING = SynchedEntityData.defineId(SpikeDummy.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("attack");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int attackTicksLeft;

    public SpikeDummy(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMobAttributes().add(Attributes.MAX_HEALTH, 40.0).add(Attributes.MOVEMENT_SPEED, 0.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new LookAtPlayerGoal(this, Player.class, 8.0f));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ATTACKING, false);
    }

    public boolean isAttacking() {
        return this.entityData.get(ATTACKING);
    }

    /** Server side: raises the synced attack flag for one animation's worth of ticks. */
    public void attack() {
        this.attackTicksLeft = ATTACK_TICKS;
        this.entityData.set(ATTACKING, true);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide() && this.attackTicksLeft > 0 && --this.attackTicksLeft == 0) {
            this.entityData.set(ATTACKING, false);
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (!this.level().isClientSide()) {
            this.attack();
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<SpikeDummy>("main", 2, test -> {
            if (test.animatable().isAttacking()) {
                return test.setAndContinue(ATTACK);
            }
            return test.setAndContinue(IDLE);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
