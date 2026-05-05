package io.legado.app.ui.about

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.R
import io.legado.app.databinding.ItemReadRecordCoverBinding
import io.legado.app.databinding.ItemReadRecordRankBinding
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.theme.UiCorner
import io.legado.app.lib.theme.primaryTextColor
import io.legado.app.model.BookCover
import io.legado.app.utils.ColorUtils
import io.legado.app.utils.applyTint
import io.legado.app.utils.dpToPx
import io.legado.app.utils.startActivityForBook
import io.legado.app.utils.toastOnUi

fun Context.openReadRecordBook(book: io.legado.app.data.entities.Book?) {
    if (book == null) {
        toastOnUi(getString(R.string.read_record_goal_open_missing))
        return
    }
    startActivityForBook(book)
}

fun ImageView.loadReadRecordCover(path: String?) {
    BookCover.load(context, path).into(this)
}

class ReadRecordCoverAdapter(
    private val context: Context,
    private val items: List<ReadRecentVisualItem>,
    private val onClick: (ReadRecentVisualItem) -> Unit
) : RecyclerView.Adapter<ReadRecordCoverAdapter.CoverHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CoverHolder {
        return CoverHolder(
            ItemReadRecordCoverBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: CoverHolder, position: Int) {
        holder.bind(items[position])
    }

    inner class CoverHolder(private val binding: ItemReadRecordCoverBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ReadRecentVisualItem) {
            binding.ivCover.loadReadRecordCover(item.snapshot.displayCover())
            binding.root.setOnClickListener { onClick(item) }
            binding.root.alpha = if (item.book == null) 0.72f else 1f
        }
    }
}

class ReadRecordRankAdapter(
    private val context: Context,
    private val items: List<ReadRecordRankItem>,
    private val formatDuring: (Long) -> String,
    private val onClick: (ReadRecordRankItem) -> Unit
) : RecyclerView.Adapter<ReadRecordRankAdapter.RankHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RankHolder {
        return RankHolder(
            ItemReadRecordRankBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: RankHolder, position: Int) {
        holder.bind(items[position], position)
    }

    inner class RankHolder(private val binding: ItemReadRecordRankBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ReadRecordRankItem, position: Int) {
            val name = item.book?.name ?: item.snapshot?.name.orEmpty()
            val author = item.book?.author ?: item.snapshot?.author.orEmpty()
            binding.tvName.text = name
            binding.tvMeta.text = if (author.isBlank()) {
                context.getString(R.string.read_record_rank_number, position + 1)
            } else {
                "${position + 1}. $author"
            }
            binding.tvTime.text = formatDuring(item.readTime)
            binding.ivCover.loadReadRecordCover(
                item.book?.getDisplayCover() ?: item.snapshot?.displayCover()
            )
            binding.root.alpha = if (item.book == null) 0.72f else 1f
            binding.root.setOnClickListener { onClick(item) }
        }
    }
}

object ReadRecordRankDialog {
    fun show(
        context: Context,
        items: List<ReadRecordRankItem>,
        formatDuring: (Long) -> String
    ) {
        val recyclerView = androidx.recyclerview.widget.RecyclerView(context).apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
            adapter = ReadRecordRankAdapter(context, items, formatDuring) {
                context.openReadRecordBook(it.book)
            }
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20.dpToPx(), 20.dpToPx(), 20.dpToPx(), 8.dpToPx())
            addView(
                androidx.appcompat.widget.AppCompatTextView(context).apply {
                    text = context.getString(R.string.read_record_read_rank)
                    setTextColor(context.primaryTextColor)
                    textSize = 18f
                },
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
            addView(
                recyclerView,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    420.dpToPx()
                ).apply {
                    topMargin = 14.dpToPx()
                }
            )
        }
        AlertDialog.Builder(context)
            .setView(container)
            .setPositiveButton(android.R.string.ok, null)
            .create()
            .applyTint()
            .show()
    }
}

fun Context.showReadRecordGoalDialog(
    initial: ReadRecordGoalConfig,
    onSave: (ReadRecordGoalConfig) -> Unit
) {
    alert(R.string.read_record_goal_card) {
        val container = LinearLayout(this@showReadRecordGoalDialog).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20.dpToPx(), 8.dpToPx(), 20.dpToPx(), 0)
        }
        val avatarInput = EditText(this@showReadRecordGoalDialog).apply {
            hint = getString(R.string.read_record_goal_avatar_hint)
            setText(initial.avatar.orEmpty())
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
        }
        val goalInput = EditText(this@showReadRecordGoalDialog).apply {
            hint = getString(R.string.read_record_goal_minutes)
            setText(initial.dailyGoalMinutes.toString())
            inputType = InputType.TYPE_CLASS_NUMBER
        }
        container.addView(
            androidx.appcompat.widget.AppCompatTextView(this@showReadRecordGoalDialog).apply {
                text = getString(R.string.read_record_goal_avatar)
                setTextColor(primaryTextColor)
                textSize = 14f
            }
        )
        container.addView(avatarInput)
        container.addView(
            androidx.appcompat.widget.AppCompatTextView(this@showReadRecordGoalDialog).apply {
                text = getString(R.string.read_record_goal_target)
                setTextColor(primaryTextColor)
                textSize = 14f
                setPadding(0, 12.dpToPx(), 0, 0)
            }
        )
        container.addView(goalInput)
        customView { container }
        okButton {
            val minutes = goalInput.text?.toString()?.trim()?.toIntOrNull()?.coerceAtLeast(1) ?: 120
            onSave(
                ReadRecordGoalConfig(
                    avatar = avatarInput.text?.toString()?.trim().orEmpty().ifBlank { null },
                    dailyGoalMinutes = minutes
                )
            )
        }
        cancelButton()
    }
}

fun buildReadRecordPreviewBackground(context: Context, weight: Float = 1f): GradientDrawable {
    return UiCorner.rounded(
        ColorUtils.adjustAlpha(ContextCompat.getColor(context, R.color.background_menu), 0.92f),
        UiCorner.panelRadius(context) * weight.coerceAtLeast(0.8f)
    )
}
