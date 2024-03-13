package dev.strubbelkopp.puyoapple.mixin;

import dev.strubbelkopp.puyoapple.PuyoApple;
import dev.strubbelkopp.puyoapple.entity.HeadpattingEntity;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity implements HeadpattingEntity {

    @Shadow public abstract float getMovementSpeed();

    @Unique
    private boolean headPattingState = false;
    @Unique
    private int lastHeadpatTick = 0;
    @Unique
    private boolean hasAppliedSpeedDebuff = false;

    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void tickHeadpatting(CallbackInfo ci) {
        MinecraftServer server = this.getServer();

        if (server != null && this.headPattingState) {
            if (server.getTicks() - this.lastHeadpatTick > 5) { // when over 5 ticks since last interaction, stop patting
                this.headPattingState = false;
                PacketByteBuf buf = PacketByteBufs.create();
                buf.writeBoolean(false);
                ServerPlayNetworking.send((ServerPlayerEntity)(Object) this, PuyoApple.HEADPAT_STATE_PACKET_ID, buf);
            }
        }
    }

    //todo why this no work????
    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void applySpeedDebuff(CallbackInfo ci) {
        MinecraftServer server = this.getServer();
        final float MOVEMENT_MULTIPLIER = 0.25F;

        if (server != null && this.headPattingState) {
            if (!hasAppliedSpeedDebuff) {
                this.setVelocity(this.getVelocity().multiply(MOVEMENT_MULTIPLIER));
                this.hasAppliedSpeedDebuff = true;
                System.out.println("slow");
            }

            if (server.getTicks() - this.lastHeadpatTick > 5) {
                this.setVelocity(this.getVelocity().multiply(1 / MOVEMENT_MULTIPLIER));
                this.hasAppliedSpeedDebuff = false;
                System.out.println("normal");
            }
        }
    }

    @Override
    public void puyoApple$setHeadpattingState(boolean state) {
        this.headPattingState = state;
    }

    public void puyoApple$setLastHeadpatTick(int lastHeadpatTick) {
        this.lastHeadpatTick = lastHeadpatTick;
    }

    public boolean puyoApple$getHeadpattingState() {
        return this.headPattingState;
    }
}
