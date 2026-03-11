package com.iseids.farmingUtils.extensions

import com.iseids.farmingUtils.Constants
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.block.data.Ageable
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable

fun ItemStack.isHoe(): Boolean = type.isHoe()

fun ItemStack.isAxe(): Boolean = type.isAxe()

fun Material.isHoe(): Boolean = this in Constants.HOES

fun Material.isAxe(): Boolean = this in Constants.AXES

fun Block.isSugarCaneOrBamboo(): Boolean = type in Constants.SUGAR_CANE_AND_BAMBOO

fun Material.isCrop(): Boolean = this in Constants.CROPS

fun Block.isFullyGrown(): Boolean {
    val ageable = blockData as? Ageable ?: return false
    return ageable.age >= ageable.maximumAge
}

fun Block.harvest() {
    breakNaturally()
}

fun Material.getSeedMaterial(): Material {
    return when (this) {
        Material.WHEAT -> Material.WHEAT_SEEDS
        Material.CARROTS -> Material.CARROT
        Material.POTATOES -> Material.POTATO
        Material.BEETROOTS -> Material.BEETROOT_SEEDS
        Material.NETHER_WART -> Material.NETHER_WART
        else -> Material.AIR
    }
}

fun ItemStack.decreaseDurability(player: Player, amount: Int = 1) {
    val damageableMeta = itemMeta as? Damageable ?: return
    damageableMeta.damage += amount
    itemMeta = damageableMeta

    if (damageableMeta.damage >= type.maxDurability) {
        player.inventory.removeItem(this)
        player.world.playSound(player.location, Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f)
    }
}
