package com.iseids.farmingUtils.crops

import com.iseids.farmingUtils.config.VeinMiningSettings
import com.iseids.farmingUtils.extensions.decreaseDurability
import com.iseids.farmingUtils.extensions.isAxe
import java.util.ArrayDeque
import kotlin.math.max
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

object VeinMiner {
    private val NEIGHBOR_OFFSETS: Array<IntArray> = arrayOf(
        intArrayOf(1, 0, 0),
        intArrayOf(-1, 0, 0),
        intArrayOf(0, 0, 1),
        intArrayOf(0, 0, -1),
        intArrayOf(1, 0, 1),
        intArrayOf(-1, 0, 1),
        intArrayOf(1, 0, -1),
        intArrayOf(-1, 0, -1),
        intArrayOf(0, 1, 0),
        intArrayOf(0, -1, 0),
    )

    fun shouldTrigger(player: Player, tool: ItemStack, block: Block, settings: VeinMiningSettings): Boolean {
        if (!settings.enabled) {
            return false
        }
        if (settings.requireSneaking && !player.isSneaking) {
            return false
        }
        if (!tool.isAxe()) {
            return false
        }
        return settings.supports(block.type)
    }

    fun breakConnectedBlocks(startBlock: Block, axe: ItemStack, player: Player, settings: VeinMiningSettings): Int {
        val blocksToBreak = findConnectedBlocks(startBlock, settings)
            .sortedWith(compareByDescending<Block> { it.y }.thenBy { it.x }.thenBy { it.z })

        blocksToBreak.forEach { it.breakNaturally(axe) }
        if (blocksToBreak.isNotEmpty()) {
            axe.decreaseDurability(player, max(1, blocksToBreak.size / 5))
        }

        return blocksToBreak.size
    }

    internal fun findConnectedBlocks(startBlock: Block, settings: VeinMiningSettings): List<Block> {
        val visited: MutableSet<Block> = linkedSetOf()
        val blocksToBreak: MutableList<Block> = mutableListOf()
        val queue: ArrayDeque<Block> = ArrayDeque()
        val material = startBlock.type

        if (!settings.supports(material)) {
            return emptyList()
        }

        queue.add(startBlock)
        while (queue.isNotEmpty() && blocksToBreak.size < settings.maxBlocks) {
            val currentBlock = queue.removeFirst()
            if (!visited.add(currentBlock) || currentBlock.type != material) {
                continue
            }

            blocksToBreak += currentBlock

            for (offset in NEIGHBOR_OFFSETS) {
                val adjacent = currentBlock.getRelative(offset[0], offset[1], offset[2])
                if (adjacent.type == material && adjacent !in visited) {
                    queue.add(adjacent)
                }
            }
        }

        return blocksToBreak
    }
}
