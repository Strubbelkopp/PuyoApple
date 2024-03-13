package dev.strubbelkopp.puyoapple.client;

import dev.strubbelkopp.puyoapple.entity.AppleEntity;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShieldItem;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

public class AppleEntityRenderer extends GeoEntityRenderer<AppleEntity> {

    public static final float SCALE = 0.85F;

    private static final String LEFT_HAND = "hand_left";
    private static final String RIGHT_HAND = "hand_right";

    private ItemStack mainHandItem;
    private ItemStack offhandItem;

    public AppleEntityRenderer(EntityRendererFactory.Context context) {
        super(context, PuyoAppleClient.APPLE_MODEL);

        addRenderLayer(new BlockAndItemGeoLayer<>(this) {
            @Override @Nullable
            protected ItemStack getStackForBone(GeoBone bone, AppleEntity animatable) {
                return switch (bone.getName()) {
                    case LEFT_HAND -> animatable.isLeftHanded() ?
                            AppleEntityRenderer.this.mainHandItem : AppleEntityRenderer.this.offhandItem;
                    case RIGHT_HAND -> animatable.isLeftHanded() ?
                            AppleEntityRenderer.this.offhandItem : AppleEntityRenderer.this.mainHandItem;
                    default -> null;
                };
            }

            @Override
            protected ModelTransformationMode getTransformTypeForStack(GeoBone bone, ItemStack stack, AppleEntity animatable) {
                return switch (bone.getName()) {
                    case LEFT_HAND, RIGHT_HAND -> ModelTransformationMode.THIRD_PERSON_RIGHT_HAND;
                    default -> ModelTransformationMode.NONE;
                };
            }

            @Override
            protected void renderStackForBone(MatrixStack poseStack, GeoBone bone, ItemStack stack, AppleEntity animatable, VertexConsumerProvider bufferSource, float partialTick, int packedLight, int packedOverlay) {
                if (stack == AppleEntityRenderer.this.mainHandItem) {
                    poseStack.multiply(new Quaternionf().rotationX((float) Math.toRadians(-90F)));
                    poseStack.scale(0.7F, 0.7F, 0.7F);

                    if (stack.getItem() instanceof ShieldItem)
                        poseStack.translate(0, 0.125, -0.25);
                }
                else if (stack == AppleEntityRenderer.this.offhandItem) {
                    poseStack.multiply(new Quaternionf().rotationX((float) Math.toRadians(-90F)));
                    poseStack.scale(0.7F, 0.7F, 0.7F);

                    if (stack.getItem() instanceof ShieldItem) {
                        poseStack.translate(0, 0.125, 0.25);
                        poseStack.multiply(new Quaternionf().rotationY((float) Math.toRadians(180F)));
                    }
                }

                super.renderStackForBone(poseStack, bone, stack, animatable, bufferSource, partialTick, packedLight, packedOverlay);
            }
        });
    }

    @Override
    public void render(AppleEntity entity, float entityYaw, float partialTick, MatrixStack poseStack, VertexConsumerProvider bufferSource, int packedLight) {
        poseStack.scale(SCALE, SCALE, SCALE);

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public void preRender(MatrixStack poseStack, AppleEntity animatable, BakedGeoModel model, VertexConsumerProvider bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);

        this.mainHandItem = animatable.getMainHandStack();
        this.offhandItem = animatable.getOffHandStack();
    }
}
