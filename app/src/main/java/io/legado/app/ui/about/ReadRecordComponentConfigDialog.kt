package io.legado.app.ui.about

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.R
import io.legado.app.databinding.DialogReadRecordComponentsBinding
import io.legado.app.databinding.ItemReadRecordComponentBinding
import io.legado.app.lib.theme.UiCorner
import io.legado.app.ui.widget.recycler.ItemTouchCallback
import io.legado.app.utils.applyTint
import io.legado.app.utils.dpToPx

object ReadRecordComponentConfigDialog {

    fun show(
        context: Context,
        initialItems: List<ReadRecordComponentItem>,
        onSaved: (List<ReadRecordComponentItem>) -> Unit
    ) {
        val binding = DialogReadRecordComponentsBinding.inflate(LayoutInflater.from(context))
        val adapter = ComponentAdapter(context, initialItems.map { it.copy() }.toMutableList())
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = adapter
        val itemTouchCallback = ItemTouchCallback(adapter).apply {
            isCanDrag = true
        }
        ItemTouchHelper(itemTouchCallback).attachToRecyclerView(binding.recyclerView)
        val dialog = AlertDialog.Builder(context)
            .setView(binding.root)
            .setPositiveButton(android.R.string.ok, null)
            .setNegativeButton(android.R.string.cancel, null)
            .create()
        dialog.setOnShowListener {
            dialog.applyTint()
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
                val items = adapter.items.map { it.copy() }
                if (items.none { it.enabled }) {
                    items.firstOrNull()?.enabled = true
                }
                onSaved(items)
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private class ComponentAdapter(
        private val context: Context,
        val items: MutableList<ReadRecordComponentItem>
    ) : RecyclerView.Adapter<ComponentAdapter.ComponentViewHolder>(), ItemTouchCallback.Callback {

        private val panelColor by lazy { ContextCompat.getColor(context, R.color.background_card) }
        private val pressedColor by lazy { ContextCompat.getColor(context, R.color.background_menu) }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ComponentViewHolder {
            val binding = ItemReadRecordComponentBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return ComponentViewHolder(binding)
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: ComponentViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun swap(srcPosition: Int, targetPosition: Int): Boolean {
            if (srcPosition !in items.indices || targetPosition !in items.indices) return false
            val item = items.removeAt(srcPosition)
            items.add(targetPosition, item)
            notifyItemMoved(srcPosition, targetPosition)
            return true
        }

        inner class ComponentViewHolder(
            private val binding: ItemReadRecordComponentBinding
        ) : RecyclerView.ViewHolder(binding.root) {

            fun bind(item: ReadRecordComponentItem) = with(binding) {
                root.background = UiCorner.actionSelector(
                    panelColor,
                    pressedColor,
                    UiCorner.panelRadius(context)
                )
                tvTitle.text = context.getString(item.type.titleRes)
                tvSubtitle.text = context.getString(item.type.hintRes)
                cbEnabled.setOnCheckedChangeListener(null)
                cbEnabled.isChecked = item.enabled
                cbEnabled.setOnCheckedChangeListener { _, checked ->
                    item.enabled = checked
                }
                root.setOnClickListener {
                    cbEnabled.isChecked = !cbEnabled.isChecked
                }
                ivDrag.setColorFilter(ContextCompat.getColor(context, R.color.secondaryText))
                applyPreview(item)
            }

            private fun applyPreview(item: ReadRecordComponentItem) = with(binding) {
                val backgrounds = listOf(previewA, previewB, previewC)
                backgrounds.forEachIndexed { index, view ->
                    view.background = buildReadRecordPreviewBackground(
                        context,
                        0.9f + index * 0.08f
                    )
                }
                previewA.layoutParams.width = 0
                previewA.layoutParams.height = 28.dpToPx()
                previewB.layoutParams.width = 0
                previewB.layoutParams.height = 28.dpToPx()
                previewC.layoutParams.width = 0
                previewC.layoutParams.height = 28.dpToPx()
                previewA.visibility = View.VISIBLE
                previewB.visibility = View.VISIBLE
                previewC.visibility = View.VISIBLE
                when (item.type) {
                    ReadRecordComponentType.OVERVIEW -> {
                        previewA.layoutParams.width = 0
                    }
                    ReadRecordComponentType.HEATMAP -> {
                        previewA.layoutParams.height = 34.dpToPx()
                    }
                    ReadRecordComponentType.RECENT_BOOKS,
                    ReadRecordComponentType.DAILY_RECORDS,
                    ReadRecordComponentType.READ_RANK -> {
                        previewA.layoutParams.height = 22.dpToPx()
                        previewB.layoutParams.height = 22.dpToPx()
                        previewC.layoutParams.height = 22.dpToPx()
                        previewB.visibility = View.VISIBLE
                        previewC.visibility = View.VISIBLE
                    }
                    ReadRecordComponentType.RECENT_COVERS -> {
                        previewA.layoutParams.height = 44.dpToPx()
                        previewB.layoutParams.height = 44.dpToPx()
                        previewC.layoutParams.height = 44.dpToPx()
                        previewB.visibility = View.VISIBLE
                        previewC.visibility = View.VISIBLE
                    }
                    ReadRecordComponentType.GOAL_CARD -> {
                        previewA.layoutParams.height = 40.dpToPx()
                        previewB.layoutParams.height = 16.dpToPx()
                        previewC.visibility = View.GONE
                    }
                }
                backgrounds.forEach { it.requestLayout() }
            }
        }
    }
}
