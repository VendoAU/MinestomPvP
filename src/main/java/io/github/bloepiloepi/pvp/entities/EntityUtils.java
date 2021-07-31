package io.github.bloepiloepi.pvp.entities;

import io.github.bloepiloepi.pvp.damage.CustomDamageType;
import io.github.bloepiloepi.pvp.enchantment.EnchantmentUtils;
import io.github.bloepiloepi.pvp.enchantment.enchantments.ProtectionEnchantment;
import io.github.bloepiloepi.pvp.enums.Tool;
import net.minestom.server.entity.*;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.entity.metadata.LivingEntityMeta;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.potion.Potion;
import net.minestom.server.potion.PotionEffect;
import net.minestom.server.potion.TimedPotion;
import net.minestom.server.utils.Position;
import net.minestom.server.utils.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public class EntityUtils {
	
	public static boolean hasEffect(Entity entity, PotionEffect type) {
		return entity.getActiveEffects().stream().anyMatch((effect) -> effect.getPotion().getEffect() == type);
	}
	
	public static Potion getEffect(Entity entity, PotionEffect type) {
		for (TimedPotion potion : entity.getActiveEffects()) {
			if (potion.getPotion().getEffect() == type) {
				return potion.getPotion();
			}
		}
		
		return new Potion(type, (byte) 0, 0);
	}
	
	public static void setOnFireForSeconds(LivingEntity entity, int seconds) {
		int ticks = seconds * 20;
		ticks = ProtectionEnchantment.transformFireDuration(entity, ticks);
		
		//FIXME this makes fire duration lower if it was higher than current seconds
		entity.setFireForDuration(ticks);
	}
	
	public static boolean damage(Entity entity, DamageType type, float amount) {
		if (entity instanceof LivingEntity) {
			return ((LivingEntity) entity).damage(type, amount);
		}
		
		return false;
	}
	
	public static boolean blockedByShield(LivingEntity entity, CustomDamageType type) {
		Entity damager = type.getEntity();
		boolean piercing = false;
		if (damager instanceof EntityProjectile) {
			if (damager.getData().<Byte>get("pierceLevel") > 0) {
				piercing = true;
			}
		}
		
		if (!type.bypassesArmor() && !piercing && isBlocking(entity)) {
			Position attackerPos = type.getPosition();
			if (attackerPos != null) {
				Position entityPos = entity.getPosition();
				
				Vector attackerPosVector = attackerPos.toVector();
				Vector entityRotation = entityPos.getDirection();
				Vector attackerDirection = entityPos.toVector().subtract(attackerPosVector).normalize();
				attackerDirection.setY(0);
				
				return attackerDirection.dot(entityRotation) < 0.0D;
			}
		}
		
		return false;
	}
	
	public static boolean isBlocking(LivingEntity entity) {
		LivingEntityMeta meta = (LivingEntityMeta) entity.getEntityMeta();
		
		if (meta.isHandActive()) {
			return entity.getItemInHand(meta.getActiveHand()).getMaterial() == Material.SHIELD;
		}
		
		return false;
	}
	
	public static void takeShieldHit(LivingEntity entity, LivingEntity attacker, boolean applyKnockback) {
		if (applyKnockback) {
			Position entityPos = entity.getPosition();
			Position attackerPos = attacker.getPosition();
			attacker.takeKnockback(0.5F, attackerPos.getX() - entityPos.getX(), attackerPos.getZ() - entityPos.getZ());
		}
		
		if (!(entity instanceof Player)) return;
		
		Tool tool = Tool.fromMaterial(attacker.getItemInMainHand().getMaterial());
		if (tool != null && tool.isAxe()) {
			disableShield((Player) entity, true); //For some reason the vanilla server always passes true
		}
	}
	
	public static void disableShield(Player player, boolean sprinting) {
		float chance = 0.25F + (float) EnchantmentUtils.getBlockEfficiency(player) * 0.05F;
		if (sprinting) {
			chance += 0.75F;
		}
		
		if (ThreadLocalRandom.current().nextFloat() < chance) {
			Tracker.setCooldown(player, Material.SHIELD, 100);
			
			//Shield disable status
			player.triggerStatus((byte) 30);
			player.triggerStatus((byte) 9);
			
			Player.Hand hand = player.getEntityMeta().getActiveHand();
			player.refreshActiveHand(false, hand == Player.Hand.OFF, false);
		}
	}
	
	public static Iterable<ItemStack> getArmorItems(LivingEntity entity) {
		List<ItemStack> list = new ArrayList<>();
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			if (slot.isArmor()) {
				list.add(entity.getEquipment(slot));
			}
		}
		
		return list;
	}
	
	public static void addExhaustion(Player player, float exhaustion) {
		if (!player.isInvulnerable() && player.getGameMode().canTakeDamage() && player.isOnline()) {
			Tracker.hungerManager.get(player.getUuid()).addExhaustion(exhaustion);
		}
	}
	
	//TODO needs improving
	public static boolean isClimbing(Player player) {
		if (player.getGameMode() == GameMode.SPECTATOR) return false;
		
		switch (Objects.requireNonNull(player.getInstance()).getBlock(player.getPosition().toBlockPosition())) {
			case LADDER:
			case VINE:
			case TWISTING_VINES:
			case TWISTING_VINES_PLANT:
			case WEEPING_VINES:
			case WEEPING_VINES_PLANT:
			case ACACIA_TRAPDOOR:
			case BIRCH_TRAPDOOR:
			case CRIMSON_TRAPDOOR:
			case DARK_OAK_TRAPDOOR:
			case IRON_TRAPDOOR:
			case JUNGLE_TRAPDOOR:
			case OAK_TRAPDOOR:
			case SPRUCE_TRAPDOOR:
			case WARPED_TRAPDOOR:
				return true;
			default:
				return false;
		}
	}
	
	public static double getBodyY(Entity entity, double heightScale) {
		return entity.getPosition().getY() + entity.getBoundingBox().getHeight() * heightScale;
	}
	
	public static boolean hasPotionEffect(LivingEntity entity, PotionEffect effect) {
		return entity.getActiveEffects().stream()
				.map((potion) -> potion.getPotion().getEffect())
				.anyMatch((potionEffect) -> potionEffect == effect);
	}
}
