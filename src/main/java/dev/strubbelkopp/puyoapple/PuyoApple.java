package dev.strubbelkopp.puyoapple;

import dev.strubbelkopp.puyoapple.entity.AppleEntity;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PuyoApple implements ModInitializer {

	public static final String MOD_ID = "puyoapple";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final EntityType<AppleEntity> APPLE_ENTITY = Registry.register(
			Registries.ENTITY_TYPE, new Identifier(MOD_ID, "apple"),
			FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, AppleEntity::new)
					.dimensions(EntityDimensions.fixed(0.6F, 2.0F)).build());

	public static final Item APPLE_SPAWN_EGG = Registry.register(
			Registries.ITEM, new Identifier(MOD_ID, "apple_spawn_egg"),
			new SpawnEggItem(APPLE_ENTITY, 0xE4D7BE, 0xB2564D, new FabricItemSettings()));
	public static final Item APPLE_SLICE = Registry.register(
			Registries.ITEM, new Identifier(MOD_ID, "apple_slice"),
			new Item(new FabricItemSettings().food(new FoodComponent.Builder().hunger(1).saturationModifier(0.1F).build())));

	public static final Identifier HEADPAT_STATE_PACKET_ID = new Identifier(MOD_ID, "headpat_state_packet");

	public static final EntityAttributeModifier HEADPAT_SPEED_DEBUFF = new EntityAttributeModifier(
			"headpat_speed_debuff", 0.01D, EntityAttributeModifier.Operation.MULTIPLY_TOTAL);

	public static final Identifier YIPPIE_SOUND_ID = new Identifier(MOD_ID, "yippie");
	public static SoundEvent YIPPIE_SOUND_EVENT = SoundEvent.of(YIPPIE_SOUND_ID);

	@Override
	public void onInitialize() {
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.SPAWN_EGGS).register(itemGroup -> itemGroup.add(APPLE_SPAWN_EGG));
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.FOOD_AND_DRINK).register(itemGroup -> itemGroup.addAfter(Items.APPLE, APPLE_SLICE));
		FabricDefaultAttributeRegistry.register(APPLE_ENTITY, AppleEntity.setAttributes());
		Registry.register(Registries.SOUND_EVENT, YIPPIE_SOUND_ID, YIPPIE_SOUND_EVENT);
	}
}