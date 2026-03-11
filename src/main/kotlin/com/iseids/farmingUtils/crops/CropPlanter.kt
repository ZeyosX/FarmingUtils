package com.iseids.farmingUtils.crops

import java.util.ArrayDeque
import kotlin.math.min
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.PlayerInventory
import org.bukkit.inventory.meta.Damageable
import java.util.EnumMap

object CropPlanter {
    private const val MAX_CONNECTED_FARMLANDS = 8192
    private val FARMLAND_OFFSETS: Array<IntArray> = arrayOf(
        intArrayOf(1, 0),
        intArrayOf(-1, 0),
        intArrayOf(0, 1),
        intArrayOf(0, -1),
    )

    private val SEED_TO_CROP: Map<Material, Material> = mapOf(
        Material.WHEAT_SEEDS to Material.WHEAT,
        Material.CARROT to Material.CARROTS,
        Material.POTATO to Material.POTATOES,
        Material.BEETROOT_SEEDS to Material.BEETROOTS,
    )

    fun plantConnectedFarmland(player: Player, startFarmland: Block, hoe: ItemStack): Int {
        if (startFarmland.type != Material.FARMLAND) {
            return 0
        }

        val inventory = player.inventory
        val availableSeeds = countPlantableSeeds(inventory)
        if (availableSeeds.isEmpty()) {
            return 0
        }

        val selectedSeed = selectSeedType(inventory, availableSeeds) ?: return 0
        val cropType = SEED_TO_CROP[selectedSeed] ?: return 0

        val durabilityBudget = getDurabilityBudget(hoe)
        if (durabilityBudget <= 0) {
            return 0
        }

        val planted = plantAcrossConnectedFarmland(
            startFarmland = startFarmland,
            cropType = cropType,
            maxPlants = min(availableSeeds.getOrDefault(selectedSeed, 0), durabilityBudget),
        )

        if (planted <= 0) {
            return 0
        }

        removeSeeds(inventory, selectedSeed, planted)
        applyDurabilityWithoutBreaking(hoe, planted)
        return planted
    }

    private fun plantAcrossConnectedFarmland(startFarmland: Block, cropType: Material, maxPlants: Int): Int {
        if (maxPlants <= 0) {
            return 0
        }

        val queue: ArrayDeque<Block> = ArrayDeque()
        val visited: MutableSet<Block> = HashSet()
        var planted = 0

        queue.add(startFarmland)
        while (queue.isNotEmpty() && visited.size < MAX_CONNECTED_FARMLANDS && planted < maxPlants) {
            val current = queue.removeFirst()
            if (!visited.add(current) || current.type != Material.FARMLAND) {
                continue
            }

            val above = current.getRelative(0, 1, 0)
            if (above.type == Material.AIR) {
                above.type = cropType
                planted++
                if (planted >= maxPlants) {
                    break
                }
            }

            for (offset in FARMLAND_OFFSETS) {
                val adjacent = current.getRelative(offset[0], 0, offset[1])
                if (adjacent.type == Material.FARMLAND && adjacent !in visited) {
                    queue.add(adjacent)
                }
            }
        }

        return planted
    }

    private fun countPlantableSeeds(inventory: PlayerInventory): MutableMap<Material, Int> {
        val totals: MutableMap<Material, Int> = EnumMap(Material::class.java)
        for (item in inventory.storageContents) {
            val stack = item ?: continue
            if (stack.type !in SEED_TO_CROP) {
                continue
            }

            totals[stack.type] = totals.getOrDefault(stack.type, 0) + stack.amount
        }
        return totals
    }

    private fun selectSeedType(inventory: PlayerInventory, totals: Map<Material, Int>): Material? {
        val offhandType = inventory.itemInOffHand.type
        if (offhandType in SEED_TO_CROP && totals.getOrDefault(offhandType, 0) > 0) {
            return offhandType
        }

        for (item in inventory.storageContents) {
            val stack = item ?: continue
            if (stack.type in SEED_TO_CROP && totals.getOrDefault(stack.type, 0) > 0) {
                return stack.type
            }
        }

        return null
    }

    private fun removeSeeds(inventory: PlayerInventory, seedType: Material, amount: Int) {
        var remaining = amount
        if (remaining <= 0) {
            return
        }

        val contents = inventory.storageContents
        for (slot in contents.indices) {
            val stack = contents[slot] ?: continue
            if (stack.type != seedType) {
                continue
            }

            val used = min(stack.amount, remaining)
            val left = stack.amount - used
            if (left <= 0) {
                contents[slot] = null
            } else {
                stack.amount = left
                contents[slot] = stack
            }

            remaining -= used
            if (remaining <= 0) {
                break
            }
        }

        inventory.storageContents = contents
    }

    private fun getDurabilityBudget(hoe: ItemStack): Int {
        val meta = hoe.itemMeta as? Damageable ?: return Int.MAX_VALUE
        if (meta.isUnbreakable) {
            return Int.MAX_VALUE
        }

        val maxDurability = hoe.type.maxDurability.toInt()
        if (maxDurability <= 0) {
            return Int.MAX_VALUE
        }

        val remaining = maxDurability - meta.damage
        return (remaining - 1).coerceAtLeast(0)
    }

    private fun applyDurabilityWithoutBreaking(hoe: ItemStack, used: Int) {
        if (used <= 0) {
            return
        }

        val meta = hoe.itemMeta as? Damageable ?: return
        if (meta.isUnbreakable) {
            return
        }

        val maxDurability = hoe.type.maxDurability.toInt()
        if (maxDurability <= 0) {
            return
        }

        val targetDamage = min(meta.damage + used, maxDurability - 1)
        meta.damage = targetDamage
        hoe.itemMeta = meta
    }
}
