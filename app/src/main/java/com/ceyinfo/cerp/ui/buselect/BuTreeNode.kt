package com.ceyinfo.cerp.ui.buselect

import com.ceyinfo.cerp.data.model.BusinessUnit

data class BuTreeNode(
    val bu: BusinessUnit,
    val depth: Int,
    val hasChildren: Boolean,
    var isExpanded: Boolean = false
)

object BuTreeBuilder {

    fun buildTree(flatList: List<BusinessUnit>): List<BuTreeNode> {
        val byParent = flatList.groupBy { it.parentId }
        val roots = flatList.filter { it.parentId == null || !flatList.any { p -> p.id == it.parentId } }
            .sortedWith(compareBy({ it.levelOrder ?: 99 }, { it.name }))

        val result = mutableListOf<BuTreeNode>()
        for (root in roots) {
            addNodeRecursive(root, 0, byParent, flatList, result, expandAll = true)
        }
        return result
    }

    private fun addNodeRecursive(
        bu: BusinessUnit,
        depth: Int,
        byParent: Map<String?, List<BusinessUnit>>,
        allBus: List<BusinessUnit>,
        result: MutableList<BuTreeNode>,
        expandAll: Boolean
    ) {
        val children = byParent[bu.id]
            ?.sortedWith(compareBy({ it.levelOrder ?: 99 }, { it.name }))
            ?: emptyList()
        val hasChildren = children.isNotEmpty()

        result.add(BuTreeNode(bu, depth, hasChildren, isExpanded = expandAll && hasChildren))

        if (expandAll && hasChildren) {
            for (child in children) {
                addNodeRecursive(child, depth + 1, byParent, allBus, result, expandAll)
            }
        }
    }

    fun toggleNode(
        nodes: List<BuTreeNode>,
        position: Int,
        allBus: List<BusinessUnit>
    ): List<BuTreeNode> {
        val mutableNodes = nodes.toMutableList()
        val node = mutableNodes[position]

        if (!node.hasChildren) return mutableNodes

        if (node.isExpanded) {
            // Collapse: remove all descendants
            mutableNodes[position] = node.copy(isExpanded = false)
            val removeFrom = position + 1
            var removeCount = 0
            for (i in removeFrom until mutableNodes.size) {
                if (mutableNodes[i].depth > node.depth) {
                    removeCount++
                } else {
                    break
                }
            }
            for (i in 0 until removeCount) {
                mutableNodes.removeAt(removeFrom)
            }
        } else {
            // Expand: insert direct children
            mutableNodes[position] = node.copy(isExpanded = true)
            val byParent = allBus.groupBy { it.parentId }
            val children = byParent[node.bu.id]
                ?.sortedWith(compareBy({ it.levelOrder ?: 99 }, { it.name }))
                ?: emptyList()

            val childNodes = children.mapIndexed { _, child ->
                val grandChildren = byParent[child.id] ?: emptyList()
                BuTreeNode(child, node.depth + 1, grandChildren.isNotEmpty(), isExpanded = false)
            }
            mutableNodes.addAll(position + 1, childNodes)
        }

        return mutableNodes
    }
}
