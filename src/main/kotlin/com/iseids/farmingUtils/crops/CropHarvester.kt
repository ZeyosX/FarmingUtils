package com.iseids.farmingUtils.crops

import com.iseids.farmingUtils.extensions.decreaseDurability
import com.iseids.farmingUtils.extensions.getSeedMaterial
import com.iseids.farmingUtils.extensions.harvest
import com.iseids.farmingUtils.extensions.isCrop
import com.iseids.farmingUtils.extensions.isFullyGrown
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.PlayerInventory
import java.util.EnumMap
import kotlin.math.min

object CropHarvester {
    fun harvestAndReplant(player: Player, baseBlock: Block, hoe: ItemStack, radius: Int = 0) {
        val baseType = baseBlock.type
        if (!baseType.isCrop()) {
            return
        }

        val world = baseBlock.world
        val baseX = baseBlock.x
        val baseY = baseBlock.y
        val baseZ = baseBlock.z

        val availableSeeds: MutableMap<Material, Int> = countAvailableSeeds(player.inventory)
        val consumedSeeds: MutableMap<Material, Int> = EnumMap(Material::class.java)
        var harvestedCount = 0

        for (x in -radius..radius) {
            for (z in -radius..radius) {
                val targetBlock = world.getBlockAt(baseX + x, baseY, baseZ + z)
                val targetType = targetBlock.type

                if (targetType.isCrop() && targetBlock.isFullyGrown()) {
                    harvestedCount++
                    targetBlock.harvest()

                    val seedType = targetType.getSeedMaterial()
                    val available = availableSeeds.getOrDefault(seedType, 0)
                    if (available > 0) {
                        targetBlock.type = targetType
                        availableSeeds[seedType] = available - 1
                        consumedSeeds[seedType] = consumedSeeds.getOrDefault(seedType, 0) + 1
                    }
                }
            }
        }

        if (consumedSeeds.isNotEmpty()) {
            removeSeeds(player.inventory, consumedSeeds)
        }

        if (harvestedCount > 0 && hoe.type != Material.AIR) {
            hoe.decreaseDurability(player)
        }
    }

    private fun countAvailableSeeds(inventory: PlayerInventory): MutableMap<Material, Int> {
        val counts: MutableMap<Material, Int> = EnumMap(Material::class.java)
        for (item in inventory.storageContents) {
            val stack = item ?: continue
            counts[stack.type] = counts.getOrDefault(stack.type, 0) + stack.amount
        }
        return counts
    }

    private fun removeSeeds(inventory: PlayerInventory, seedsToRemove: Map<Material, Int>) {
        if (seedsToRemove.isEmpty()) {
            return
        }

        val remaining = seedsToRemove.toMutableMap()
        val contents = inventory.storageContents
        for (slot in contents.indices) {
            val stack = contents[slot] ?: continue
            val needed = remaining[stack.type] ?: continue

            val taken = min(stack.amount, needed)
            val after = stack.amount - taken
            if (after <= 0) {
                contents[slot] = null
            } else {
                stack.amount = after
                contents[slot] = stack
            }

            val stillNeeded = needed - taken
            if (stillNeeded <= 0) {
                remaining.remove(stack.type)
            } else {
                remaining[stack.type] = stillNeeded
            }

            if (remaining.isEmpty()) {
                break
            }
        }

        inventory.storageContents = contents
    }
}
