package dev.strubbelkopp.puyoapple.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.strubbelkopp.puyoapple.entity.HeadpattingEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(HeldItemRenderer.class)
public class HeldItemRendererMixin {

    @Unique
    private boolean headpattingState = false;
    @Unique
    private float headpatProgress = 0.0F;
    @Unique
    private float lastHeadpatProgress = 0.0F;

    @Shadow @Final
    private EntityRenderDispatcher entityRenderDispatcher;
    @Shadow @Final
    private MinecraftClient client;

    @Inject(method = "renderFirstPersonItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderArmHoldingItem(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IFFLnet/minecraft/util/Arm;)V"))
    private void renderHeadpattingArm(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        this.headpattingState = ((HeadpattingEntity) player).puyoApple$getHeadpattingState();

        if (this.headpattingState) {
            this.lastHeadpatProgress = this.headpatProgress;
            this.headpatProgress = getHeadpatProgress(tickDelta);

            float f = (player.getMainArm() == Arm.RIGHT) ? 1.0F : -1.0F;
            matrices.translate(f * 0.64000005F, -0.6F, -0.71999997F);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(f * 45.0F));
            matrices.translate(f * -1.0F, 3.6F, 3.5F);
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(f * 120.0F));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(200.0F));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(f * -135.0F));
            matrices.translate(f * 5.6F, 0.0F, 0.0F);

            float j = -0.4F * MathHelper.sin(headpatProgress * (float) Math.PI);
            matrices.translate(-0.2F, 0.1F, -0.1F);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-15.0F));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-20.0F));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(f * j * 25.0F));

            final float SCALE = 1.2F;
            matrices.scale(SCALE, SCALE, SCALE);

            AbstractClientPlayerEntity abstractClientPlayerEntity = this.client.player;
            RenderSystem.setShaderTexture(0, abstractClientPlayerEntity.getSkinTexture());
            PlayerEntityRenderer playerEntityRenderer = (PlayerEntityRenderer) this.entityRenderDispatcher.getRenderer(abstractClientPlayerEntity);
            if (player.getMainArm() == Arm.RIGHT) {
                playerEntityRenderer.renderRightArm(matrices, vertexConsumers, light, abstractClientPlayerEntity);
            } else {
                playerEntityRenderer.renderLeftArm(matrices, vertexConsumers, light, abstractClientPlayerEntity);
            }
        }
    }

    @Unique
    private float getHeadpatProgress(float tickDelta) {
        float f = this.headpatProgress - this.lastHeadpatProgress;
        if (f < 0.0F) {
            ++f;
        }
        return this.lastHeadpatProgress + f * tickDelta + 0.025F;
    }

    @WrapWithCondition(method = "renderFirstPersonItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderArmHoldingItem(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IFFLnet/minecraft/util/Arm;)V"))
    private boolean skipDefaultHandRenderWhenHeadpatting(HeldItemRenderer instance, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, float equipProgress, float swingProgress, Arm arm) {
        return !this.headpattingState;
    }
}
