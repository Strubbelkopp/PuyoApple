package dev.strubbelkopp.puyoapple.client;

import dev.strubbelkopp.puyoapple.PuyoApple;
import dev.strubbelkopp.puyoapple.entity.AppleEntity;
import dev.strubbelkopp.puyoapple.entity.HeadpattingEntity;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.model.GeoModel;

public class PuyoAppleClient implements ClientModInitializer {

    public static GeoModel<AppleEntity> APPLE_MODEL = new DefaultedEntityGeoModel<>(new Identifier(PuyoApple.MOD_ID, "apple"));

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(PuyoApple.APPLE_ENTITY, AppleEntityRenderer::new);
        ClientPlayNetworking.registerGlobalReceiver(PuyoApple.HEADPAT_STATE_PACKET_ID, (client, handler, buf, responseSender) -> {
            if (client.player != null) ((HeadpattingEntity) client.player).puyoApple$setHeadpattingState(buf.readBoolean());
        });
    }
}
