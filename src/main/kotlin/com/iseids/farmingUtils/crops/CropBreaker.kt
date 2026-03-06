package com.iseids.farmingUtils.crops

import com.iseids.farmingUtils.extensions.decreaseDurability
import java.util.ArrayDeque
import kotlin.math.max
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

object CropBreaker {
    private const val MAX_CONNECTED_BLOCKS = 2048
    private val NEIGHBOR_OFFSETS: Array<IntArray> = arrayOf(
        intArrayOf(1, 0),
        intArrayOf(-1, 0),
        intArrayOf(0, 1),
        intArrayOf(0, -1),
        intArrayOf(1, 1),
        intArrayOf(-1, 1),
        intArrayOf(1, -1),
        intArrayOf(-1, -1),
    )

    fun breakConnectedCrops(startBlock: Block, axe: ItemStack, player: Player) {
        val material = startBlock.type
        val visited: MutableSet<Block> = linkedSetOf()
        val blocksToBreak: MutableList<Block> = mutableListOf()
        val queue: ArrayDeque<Block> = ArrayDeque()

        queue.add(startBlock)
        while (queue.isNotEmpty() && blocksToBreak.size < MAX_CONNECTED_BLOCKS) {
            val currentBlock = queue.removeFirst()
            if (!visited.add(currentBlock) || currentBlock.type != material) {
                continue
            }

            blocksToBreak += currentBlock

            for (offset in NEIGHBOR_OFFSETS) {
                val adjacent = currentBlock.getRelative(offset[0], 0, offset[1])
                if (adjacent.type == material && adjacent !in visited) {
                    queue.add(adjacent)
                }
            }
        }

        blocksToBreak.forEach { it.breakNaturally() }
        if (blocksToBreak.isNotEmpty()) {
            axe.decreaseDurability(player, max(1, blocksToBreak.size / 5))
        }
    }
}
