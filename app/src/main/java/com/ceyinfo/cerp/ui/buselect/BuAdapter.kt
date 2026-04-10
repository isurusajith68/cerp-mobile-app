package com.ceyinfo.cerp.ui.buselect

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.RotateAnimation
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.ceyinfo.cerp.R
import com.ceyinfo.cerp.data.model.BusinessUnit
import com.ceyinfo.cerp.databinding.ItemBusinessUnitBinding

class BuAdapter(
    private val allBus: List<BusinessUnit>,
    private val onSelect: (BusinessUnit) -> Unit,
    private val onToggle: (Int) -> Unit
) : RecyclerView.Adapter<BuAdapter.ViewHolder>() {

    private var nodes: List<BuTreeNode> = emptyList()

    fun submitNodes(newNodes: List<BuTreeNode>) {
        nodes = newNodes
        notifyDataSetChanged()
    }

    override fun getItemCount() = nodes.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBusinessUnitBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(nodes[position])
    }

    inner class ViewHolder(
        private val binding: ItemBusinessUnitBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(node: BuTreeNode) {
            val ctx = binding.root.context
            val bu = node.bu
            val dp = ctx.resources.displayMetrics.density

            // ── Indent based on tree depth ──
            val indentPx = (node.depth * 24 * dp).toInt()
            binding.spacerIndent.layoutParams = binding.spacerIndent.layoutParams.apply {
                width = indentPx
            }

            // ── Name (bold for parents, normal for leaves) ──
            binding.tvBuName.text = bu.name
            binding.tvBuName.paint.isFakeBoldText = node.hasChildren

            // ── Level badge ──
            val levelText = bu.level?.replaceFirstChar { it.uppercase() } ?: "Unit"
            binding.tvBuLevel.text = levelText
            val levelColor = getLevelColor(bu.level)
            applyColor(binding.tvBuLevel.background, levelColor, ctx, 6f * dp)

            // ── Level bar color ──
            applyColor(binding.barLevel.background, levelColor, ctx, 2f * dp)

            // ── Expand/collapse or leaf dot ──
            if (node.hasChildren) {
                binding.ivExpand.visibility = View.VISIBLE
                binding.dotLeaf.visibility = View.GONE
                // Rotate arrow: 0 = collapsed (right), -180 = expanded (down)
                binding.ivExpand.rotation = if (node.isExpanded) -180f else 0f
                binding.ivExpand.setColorFilter(ContextCompat.getColor(ctx, levelColor))

                // Show child count
                val childCount = allBus.count { it.parentId == bu.id }
                if (childCount > 0) {
                    binding.tvChildCount.text = "$childCount sub"
                    binding.tvChildCount.visibility = View.VISIBLE
                } else {
                    binding.tvChildCount.visibility = View.GONE
                }
            } else {
                binding.ivExpand.visibility = View.GONE
                binding.dotLeaf.visibility = View.VISIBLE
                binding.tvChildCount.visibility = View.GONE
                applyColor(binding.dotLeaf.background, levelColor, ctx, 4f * dp)
            }

            // ── Code ──
            if (!bu.code.isNullOrEmpty()) {
                binding.tvBuCode.text = bu.code
                binding.tvBuCode.visibility = View.VISIBLE
            } else {
                binding.tvBuCode.visibility = View.GONE
            }

            // ── Select button color by level ──
            applyColor(binding.ivSelect.background, levelColor, ctx, 14f * dp)

            // ── Click handlers ──
            // Tap expand/collapse area (left side) toggles tree
            binding.ivExpand.setOnClickListener {
                animateArrow(binding.ivExpand, node.isExpanded)
                onToggle(adapterPosition)
            }

            // Tap select button selects the BU
            binding.ivSelect.setOnClickListener { onSelect(bu) }

            // Tap card: if parent → toggle, if leaf → select
            binding.cardRoot.setOnClickListener {
                if (node.hasChildren) {
                    animateArrow(binding.ivExpand, node.isExpanded)
                    onToggle(adapterPosition)
                } else {
                    onSelect(bu)
                }
            }
        }

        private fun animateArrow(view: View, currentlyExpanded: Boolean) {
            val from = if (currentlyExpanded) -180f else 0f
            val to = if (currentlyExpanded) 0f else -180f
            val anim = RotateAnimation(from, to, view.width / 2f, view.height / 2f).apply {
                duration = 200
                fillAfter = true
            }
            view.startAnimation(anim)
        }

        private fun applyColor(drawable: android.graphics.drawable.Drawable?, colorRes: Int, ctx: android.content.Context, radius: Float) {
            val bg = drawable as? GradientDrawable ?: return
            bg.setColor(ContextCompat.getColor(ctx, colorRes))
            bg.cornerRadius = radius
        }
    }

    companion object {
        fun getLevelColor(level: String?): Int {
            return when (level?.lowercase()) {
                "organization" -> R.color.level_org
                "division" -> R.color.level_division
                "project" -> R.color.level_project
                "site" -> R.color.level_site
                else -> R.color.primary
            }
        }
    }
}
