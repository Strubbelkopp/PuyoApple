package dev.strubbelkopp.puyoapple.entity;

import dev.strubbelkopp.puyoapple.PuyoApple;
import dev.strubbelkopp.puyoapple.client.AppleEntityRenderer;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.EntityView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.network.SerializableDataTicket;
import software.bernie.geckolib.util.GeckoLibUtil;

public class AppleEntity extends TameableEntity implements GeoEntity {

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("animation.apple.idle");
    private static final RawAnimation WALK_ANIMATION = RawAnimation.begin().thenLoop("animation.apple.walk");
    private static final RawAnimation RUN_ANIMATION = RawAnimation.begin().thenLoop("animation.apple.run");
    private static final RawAnimation HEADPAT_START_ANIMATION = RawAnimation.begin().thenPlay("animation.apple.headpat_start");
    private static final RawAnimation HEADPAT_LOOP_ANIMATION = RawAnimation.begin().thenLoop("animation.apple.headpat_loop");
    private static final RawAnimation HEADPAT_END_ANIMATION = RawAnimation.begin().thenPlay("animation.apple.headpat_end");
    private static final RawAnimation EAT_SLICE_ANIMATION = RawAnimation.begin().thenPlay("animation.apple.eat_slice");
    private static final RawAnimation YIPPIE_ANIMATION = RawAnimation.begin().thenPlay("animation.apple.yippie");

    private static final SerializableDataTicket<Boolean> SYNCED_HEADPAT_STATE = GeckoLibUtil.addDataTicket(SerializableDataTicket.ofBoolean(new Identifier(PuyoApple.MOD_ID, "synced_headpat_state")));
    public static final float HEADPAT_RANGE = 1.4F;
    private boolean currentHeadpatState = false;
    private int lastInteractTick = 0;
    private static final int EATING_COOLDOWN_TICKS = (int) (1.6 * 20); // 1.6 seconds
    private int ticksSinceLastEaten = 0;
    private static final int YIPPIE_COOLDOWN_TICKS = 60 * 20; // 60 seconds
    private int ticksSinceLastYippie = 0;

    public AppleEntity(EntityType<? extends TameableEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder setAttributes() {
        return PathAwareEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.2F);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new EscapeDangerGoal(this, 1.6));
        this.goalSelector.add(2, new PattingGoal(this));
        this.goalSelector.add(3, new SitGoal(this));
        this.goalSelector.add(4, new FollowOwnerGoal(this, 1.6, 10.0F, 2.0F, false));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 1.0));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(7, new LookAroundGoal(this));
    }

    @Override
    public ActionResult interactAt(PlayerEntity player, Vec3d hitPos, Hand hand) {
        ItemStack heldItem = player.getStackInHand(hand);
        boolean isMainHandFree = player.getStackInHand(Hand.MAIN_HAND).isEmpty();

        if (this.getWorld().isClient) {
            if (ticksSinceLastEaten > EATING_COOLDOWN_TICKS && !this.isNavigating() && heldItem.isOf(PuyoApple.APPLE_SLICE) && this.getHealth() < this.getMaxHealth()) {
                ticksSinceLastEaten = 0;
                return ActionResult.CONSUME;
            } else if (this.isOwner(player) && player.isSneaking()) {
                return ActionResult.CONSUME;
            }
            return ActionResult.PASS;
        } else {
            if (this.isTamed()) {
                if (ticksSinceLastEaten > EATING_COOLDOWN_TICKS && !this.isNavigating() && heldItem.isOf(PuyoApple.APPLE_SLICE) && this.getHealth() < this.getMaxHealth()) {
                    this.heal((float) heldItem.getItem().getFoodComponent().getHunger());
                    triggerAnim("controller", "eat_slice");

                    // Sets an apple slice as the active item, which gets used in tick(), plays sound / eating particles
                    this.setStackInHand(Hand.MAIN_HAND, PuyoApple.APPLE_SLICE.getDefaultStack());
                    this.setCurrentHand(Hand.MAIN_HAND);

                    if (!player.getAbilities().creativeMode) {
                        heldItem.decrement(1);
                    }
                    ticksSinceLastEaten = 0;
                    return ActionResult.SUCCESS;
                } else if (player.isSneaking() && this.isOwner(player)) {
                    this.setSitting(!this.isSitting());
                    this.jumping = false;
                    this.navigation.stop();
                    return ActionResult.SUCCESS;
                }
            }
            if (isMainHandFree && ticksSinceLastEaten > EATING_COOLDOWN_TICKS) {
                tryHeadpatting(player, hitPos);
            }
        }

        return super.interactAt(player, hitPos, hand);
    }

    public void tryHeadpatting(PlayerEntity player, Vec3d hitPos) {
        float headThreshold = 1.4F * AppleEntityRenderer.SCALE;

        if (!this.isNavigating() && hitPos.y > headThreshold && player.getPos().squaredDistanceTo(this.getPos()) < HEADPAT_RANGE * HEADPAT_RANGE) {
            this.lastInteractTick = player.getServer().getTicks();
            ((HeadpattingEntity) player).puyoApple$setLastHeadpatTick(this.lastInteractTick);

            if (!this.currentHeadpatState) {
                triggerAnim("controller", "headpat_start");

                this.currentHeadpatState = true;
                ((HeadpattingEntity) player).puyoApple$setHeadpattingState(true);
                this.setAnimData(SYNCED_HEADPAT_STATE, true);
                PacketByteBuf buf = PacketByteBufs.create();
                buf.writeBoolean(true);
                ServerPlayNetworking.send((ServerPlayerEntity) player, PuyoApple.HEADPAT_STATE_PACKET_ID, buf);
            }

            if (!this.isTamed() && this.random.nextDouble() <= 0.01D) {
                this.setOwner(player);
                this.setSitting(true);
                this.getWorld().sendEntityStatus(this, EntityStatuses.ADD_POSITIVE_PLAYER_REACTION_PARTICLES);
            }
        }
    }

    @Override
    public void tick() {
        MinecraftServer server = this.getServer();

        if (server != null) {
            if (this.currentHeadpatState) {
                if (server.getTicks() - this.lastInteractTick > 5) { // when over 5 ticks since last interaction, stop patting
                    triggerAnim("controller", "headpat_end");
                    this.currentHeadpatState = false;
                    this.setAnimData(SYNCED_HEADPAT_STATE, false);
                }
            } else if (ticksSinceLastYippie > YIPPIE_COOLDOWN_TICKS && !this.isNavigating() && this.random.nextDouble() <= 0.005D) {
                ticksSinceLastYippie = 0;
                triggerAnim("controller", "yippie");
                this.getWorld().playSound(null, this.getBlockPos(), PuyoApple.YIPPIE_SOUND_EVENT, SoundCategory.NEUTRAL, 1.0F, 1.0F);
            }

            ticksSinceLastEaten += 1;
            ticksSinceLastYippie += 1;
        }

        super.tick();
    }

    @Override
    protected void mobTick() {
        if (this.getMoveControl().isMoving()) {
            this.setSprinting(this.getMoveControl().getSpeed() > 1.0);
        } else {
            this.setSprinting(false);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 2, this::predicate)
                .triggerableAnim("headpat_start", HEADPAT_START_ANIMATION)
                .triggerableAnim("headpat_end", HEADPAT_END_ANIMATION)
                .triggerableAnim("eat_slice", EAT_SLICE_ANIMATION)
                .triggerableAnim("yippie", YIPPIE_ANIMATION));
    }

    private PlayState predicate(final AnimationState<AppleEntity> state) {
        if (state.isMoving()) {
            state.getController().setAnimation(this.isSprinting() ? RUN_ANIMATION : WALK_ANIMATION);
        } else if (Boolean.TRUE.equals(this.getAnimData(SYNCED_HEADPAT_STATE))) {
            state.getController().setAnimation(HEADPAT_LOOP_ANIMATION);
        } else {
            state.getController().setAnimation(IDLE_ANIMATION);
        }

        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        return PuyoApple.APPLE_ENTITY.create(world);
    }

    public boolean isBeingHeadpatted() {
        return this.currentHeadpatState;
    }

    @Override
    public EntityView method_48926() {
        return this.getWorld();
    }
}