package io.legado.app.ui.book.manga

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader
import com.bumptech.glide.request.target.Target.SIZE_ORIGINAL
import com.bumptech.glide.util.FixedPreloadSizeProvider
import com.qmdeve.liquidglass.widget.LiquidGlassView
import io.legado.app.BuildConfig
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.BookType
import io.legado.app.constant.EventBus
import io.legado.app.constant.PageAnim
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.BookProgress
import io.legado.app.data.entities.BookSource
import io.legado.app.databinding.ActivityMangaBinding
import io.legado.app.databinding.ViewLoadMoreBinding
import io.legado.app.help.book.isImage
import io.legado.app.help.book.removeType
import io.legado.app.help.config.AppConfig
import io.legado.app.help.source.getSourceType
import io.legado.app.help.storage.Backup
import io.legado.app.lib.dialogs.alert
import io.legado.app.model.ReadManga
import io.legado.app.receiver.NetworkChangedListener
import io.legado.app.ui.book.changesource.ChangeBookSourceDialog
import io.legado.app.ui.book.info.BookInfoActivity
import io.legado.app.ui.book.manga.config.MangaColorFilterConfig
import io.legado.app.ui.book.manga.config.MangaColorFilterDialog
import io.legado.app.ui.book.manga.config.MangaEpaperDialog
import io.legado.app.ui.book.manga.config.MangaFooterConfig
import io.legado.app.ui.book.manga.config.MangaFooterSettingDialog
import io.legado.app.ui.book.manga.entities.BaseMangaPage
import io.legado.app.ui.book.manga.entities.MangaPage
import io.legado.app.ui.book.manga.recyclerview.MangaAdapter
import io.legado.app.ui.book.manga.recyclerview.MangaLayoutManager
import io.legado.app.ui.book.manga.recyclerview.ScrollTimer
import io.legado.app.ui.book.read.MangaMenu
import io.legado.app.ui.book.read.ReaderBottomGlassStyle
import io.legado.app.ui.book.read.ReadBookActivity.Companion.RESULT_DELETED
import io.legado.app.ui.book.read.setMinimapChapterNavigationClickListener
import io.legado.app.ui.book.toc.TocActivityResult
import io.legado.app.ui.browser.WebViewActivity
import io.legado.app.ui.widget.number.NumberPickerDialog
import io.legado.app.ui.widget.recycler.LoadMoreView
import io.legado.app.utils.GSON
import io.legado.app.utils.NetworkUtils
import io.legado.app.utils.StartActivityContract
import io.legado.app.utils.canScroll
import io.legado.app.utils.dpToPx
import io.legado.app.utils.fastBinarySearch
import io.legado.app.utils.findCenterViewPosition
import io.legado.app.utils.fromJsonObject
import io.legado.app.utils.getCompatColor
import io.legado.app.utils.gone
import io.legado.app.utils.observeEvent
import io.legado.app.utils.showDialogFragment
import io.legado.app.utils.startActivity
import io.legado.app.utils.toastOnUi
import io.legado.app.utils.toggleSystemBar
import io.legado.app.utils.viewbindingdelegate.viewBinding
import io.legado.app.utils.visible
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DecimalFormat
import kotlin.math.ceil

class ReadMangaActivity : VMBaseActivity<ActivityMangaBinding, ReadMangaViewModel>(),
    ReadManga.Callback, ChangeBookSourceDialog.CallBack, MangaMenu.CallBack,
    MangaColorFilterDialog.Callback, ScrollTimer.ScrollCallback, MangaEpaperDialog.Callback {

    private val mLayoutManager by lazy {
        MangaLayoutManager(this)
    }
    private val mAdapter: MangaAdapter by lazy {
        MangaAdapter(this)
    }

    private val mSizeProvider by lazy {
        FixedPreloadSizeProvider<Any>(resources.displayMetrics.widthPixels, SIZE_ORIGINAL)
    }

    private val mPagerSnapHelper: PagerSnapHelper by lazy {
        PagerSnapHelper()
    }
    private val boundMangaMinimapGlassViewIds = hashSetOf<Int>()
    private var pendingMangaProgressMinimapLayoutSync = false

    private lateinit var mMangaFooterConfig: MangaFooterConfig
    private val mLabelBuilder by lazy { StringBuilder() }

    private var mMenu: Menu? = null

    private var mRecyclerViewPreloader: RecyclerViewPreloader<Any>? = null

    private val networkChangedListener by lazy {
        NetworkChangedListener(this)
    }

    private var justInitData: Boolean = false
    private var syncDialog: AlertDialog? = null
    private val mScrollTimer by lazy {
        ScrollTimer(this, binding.recyclerView, lifecycleScope).apply {
            setSpeed(mangaAutoPageSpeed)
        }
    }
    private var enableAutoScrollPage = false
    private var enableAutoScroll = false
    private val mLinearInterpolator by lazy {
        LinearInterpolator()
    }

    private val loadMoreView by lazy {
        LoadMoreView(this).apply {
            setBackgroundColor(getCompatColor(R.color.book_ant_10))
            setLoadingColor(R.color.white)
            setLoadingTextColor(R.color.white)
        }
    }

    //打开目录返回选择章节返回结果
    private val tocActivity = registerForActivityResult(TocActivityResult()) {
        it?.let {
            viewModel.openChapter(it[0] as Int, it[1] as Int)
        }
    }
    private val bookInfoActivity =
        registerForActivityResult(StartActivityContract(BookInfoActivity::class.java)) {
            if (it.resultCode == RESULT_OK) {
                setResult(RESULT_DELETED)
                super.finish()
            } else {
                ReadManga.loadOrUpContent()
            }
        }
    override val binding by viewBinding(ActivityMangaBinding::inflate)
    override val viewModel by viewModels<ReadMangaViewModel>()
    private val loadingViewVisible get() = binding.flLoading.isVisible
    private val df by lazy {
        DecimalFormat("0.0%")
    }
    private val mangaPageAnim: Int?
        get() = ReadManga.book?.config?.mangaPageAnim
    private val mangaHorizontalScroll: Boolean
        get() = when (mangaPageAnim) {
            PageAnim.coverPageAnim,
            PageAnim.linkedCoverPageAnim,
            PageAnim.slidePageAnim,
            PageAnim.simulationPageAnim,
            PageAnim.noAnim -> true

            PageAnim.scrollPageAnim -> false
            else -> ReadManga.book?.config?.mangaHorizontalScroll ?: AppConfig.enableMangaHorizontalScroll
        }
    private val mangaDisablePageAnim: Boolean
        get() = when (mangaPageAnim) {
            PageAnim.coverPageAnim,
            PageAnim.linkedCoverPageAnim,
            PageAnim.slidePageAnim,
            PageAnim.simulationPageAnim,
            PageAnim.scrollPageAnim -> false

            PageAnim.noAnim -> true
            else -> ReadManga.book?.config?.mangaDisablePageAnim ?: AppConfig.disableMangaPageAnim
        }
    private val mangaDisableHorizontalPageSnap: Boolean
        get() = when (mangaPageAnim) {
            PageAnim.coverPageAnim,
            PageAnim.linkedCoverPageAnim,
            PageAnim.slidePageAnim,
            PageAnim.simulationPageAnim -> false

            PageAnim.noAnim -> true
            else -> ReadManga.book?.config?.mangaDisableHorizontalPageSnap
                ?: AppConfig.disableHorizontalPageSnap
        }
    private val mangaDisableClickScroll: Boolean
        get() = ReadManga.book?.config?.mangaDisableClickScroll ?: AppConfig.disableClickScroll
    private val mangaDisableScale: Boolean
        get() = ReadManga.book?.config?.mangaDisableScale ?: AppConfig.disableMangaScale
    private val mangaAutoPageSpeed: Int
        get() = ReadManga.book?.config?.mangaAutoPageSpeed ?: AppConfig.mangaAutoPageSpeed

    override fun onCreate(savedInstanceState: Bundle?) {
        upLayoutInDisplayCutoutMode()
        super.onCreate(savedInstanceState)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        ReadManga.register(this)
        upSystemUiVisibility(false)
        initRecyclerView()
        bindMangaProgressMinimap()
        binding.tvRetry.setOnClickListener {
            binding.llLoading.isVisible = true
            binding.llRetry.isGone = true
            ReadManga.loadOrUpContent()
        }
        binding.pbLoading.isVisible = !AppConfig.isEInkMode
        mAdapter.addFooterView {
            ViewLoadMoreBinding.bind(loadMoreView)
        }
        loadMoreView.setOnClickListener {
            if (!loadMoreView.isLoading && ReadManga.hasNextChapter) {
                loadMoreView.startLoad()
                ReadManga.loadOrUpContent()
            }
        }
        loadMoreView.gone()
        mMangaFooterConfig =
            GSON.fromJsonObject<MangaFooterConfig>(AppConfig.mangaFooterConfig).getOrNull()
                ?: MangaFooterConfig()
    }

    override fun observeLiveBus() {
        observeEvent<MangaFooterConfig>(EventBus.UP_MANGA_CONFIG) {
            mMangaFooterConfig = it
            val item = mAdapter.getItem(binding.recyclerView.findCenterViewPosition())
            upInfoBar(item)
        }
    }

    private fun initRecyclerView() {
        val mangaColorFilter =
            GSON.fromJsonObject<MangaColorFilterConfig>(AppConfig.mangaColorFilter).getOrNull()
                ?: MangaColorFilterConfig()
        mAdapter.run {
            setMangaImageColorFilter(mangaColorFilter)
            enableMangaEInk(AppConfig.enableMangaEInk, AppConfig.mangaEInkThreshold)
            enableGray(AppConfig.enableMangaGray)
        }
        setHorizontalScroll(mangaHorizontalScroll)
        binding.recyclerView.run {
            adapter = mAdapter
            itemAnimator = null
            layoutManager = mLayoutManager
            setHasFixedSize(true)
            setDisableClickScroll(mangaDisableClickScroll)
            setDisableMangaScale(mangaDisableScale)
            setRecyclerViewPreloader(AppConfig.mangaPreDownloadNum)
            setPreScrollListener { _, _, _, position ->
                if (mAdapter.isNotEmpty()) {
                    val item = mAdapter.getItem(position)
                    if (item is BaseMangaPage) {
                        if (ReadManga.durChapterIndex < item.chapterIndex) {
                            ReadManga.moveToNextChapter()
                        } else if (ReadManga.durChapterIndex > item.chapterIndex) {
                            ReadManga.moveToPrevChapter()
                        } else {
                            ReadManga.durChapterPos = item.index
                            ReadManga.curPageChanged()
                        }
                        if (item is MangaPage) {
                            updateMangaProgressMinimap()
                            binding.mangaMenu.upBookView()
                            upInfoBar(item)
                        }
                    }
                }
            }
        }
        binding.webtoonFrame.run {
            onTouchMiddle {
                if (!binding.mangaMenu.isVisible && !loadingViewVisible) {
                    binding.mangaMenu.runMenuIn()
                }
            }
            onNextPage {
                scrollToNext()
            }
            onPrevPage {
                scrollToPrev()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        viewModel.initData(intent) {
            applyBookMangaReadConfig()
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        viewModel.initData(intent) {
            applyBookMangaReadConfig()
        }
        justInitData = true
    }

    override fun upContent() {
        lifecycleScope.launch {
            binding.mangaMenu.upBookView()
            val data = withContext(IO) { ReadManga.mangaContents }
            val pos = data.pos
            val list = data.items
            val curFinish = data.curFinish
            val nextFinish = data.nextFinish
            mAdapter.submitList(list) {
                if (loadingViewVisible && curFinish) {
                    binding.infobar.isVisible = true
                    upInfoBar(list[pos])
                    mLayoutManager.scrollToPositionWithOffset(pos, 0)
                    binding.flLoading.isGone = true
                    loadMoreView.visible()
                    updateMangaProgressMinimap()
                    binding.mangaMenu.upBookView()
                }

                if (curFinish) {
                    if (!ReadManga.hasNextChapter) {
                        loadMoreView.noMore("暂无章节了！")
                    } else if (nextFinish) {
                        loadMoreView.stopLoad()
                    } else {
                        loadMoreView.startLoad()
                    }
                }
            }
        }
    }

    private fun upInfoBar(page: Any?) {
        if (page !is MangaPage) {
            return
        }
        val chapterIndex = page.chapterIndex
        val chapterSize = page.chapterSize
        val chapterPos = page.index
        val imageCount = page.imageCount
        val chapterName = page.mChapterName
        mMangaFooterConfig.run {
            mLabelBuilder.clear()
            binding.infobar.isGone = hideFooter
            binding.infobar.textInfoAlignment = footerOrientation

            if (!hideChapterName) {
                mLabelBuilder.append(chapterName).append(" ")
            }

            if (!hidePageNumber) {
                if (!hidePageNumberLabel) {
                    mLabelBuilder.append(getString(R.string.manga_check_page_number))
                }
                mLabelBuilder.append("${chapterPos + 1}/${imageCount}").append(" ")
            }

            if (!hideChapter) {
                if (!hideChapterLabel) {
                    mLabelBuilder.append(getString(R.string.manga_check_chapter))
                }
                mLabelBuilder.append("${chapterIndex + 1}/${chapterSize}").append(" ")
            }

            if (!hideProgressRatio) {
                if (!hideProgressRatioLabel) {
                    mLabelBuilder.append(getString(R.string.manga_check_progress))
                }
                val percent = if (chapterSize == 0 || imageCount == 0 && chapterIndex == 0) {
                    "0.0%"
                } else if (imageCount == 0) {
                    df.format((chapterIndex + 1.0f) / chapterSize.toDouble())
                } else {
                    var percent =
                        df.format(
                            chapterIndex * 1.0f / chapterSize + 1.0f /
                                    chapterSize * (chapterPos + 1) / imageCount.toDouble()
                        )
                    if (percent == "100.0%" && (chapterIndex + 1 != chapterSize || chapterPos + 1 != imageCount)) {
                        percent = "99.9%"
                    }
                    percent
                }
                mLabelBuilder.append(percent)
            }
        }
        binding.infobar.update(
            if (mLabelBuilder.isEmpty()) "" else mLabelBuilder.toString()
        )
    }

    private fun bindMangaProgressMinimap() {
        binding.mangaProgressMinimap.onProgressChanging = ::previewMangaProgressMinimap
        binding.mangaProgressMinimap.onProgressChanged = ::commitMangaProgressMinimap
        binding.btnMangaMinimapPrevious.setMinimapChapterNavigationClickListener(binding.tvMangaMinimapPrevious) {
            ReadManga.moveToPrevChapter(true)
        }
        binding.btnMangaMinimapNext.setMinimapChapterNavigationClickListener(binding.tvMangaMinimapNext) {
            ReadManga.moveToNextChapter(true)
        }
    }

    private fun setupMangaMinimapControlGlass() {
        val glassViews = listOf(
            binding.mangaMinimapPreviousGlassView,
            binding.mangaMinimapNextGlassView
        )
        if (!ViewCompat.isLaidOut(binding.webtoonFrame) || glassViews.any { !ViewCompat.isLaidOut(it) }) {
            binding.mangaProgressMinimapPanel.post {
                if (binding.mangaProgressMinimapPanel.isVisible) {
                    setupMangaMinimapControlGlass()
                }
            }
            return
        }
        val glassLevel = ReaderBottomGlassStyle.glassLevel()
        val cornerRadius = 28f.dpToPx()
        setupMangaMinimapControlGlassView(
            button = binding.btnMangaMinimapPrevious,
            glassView = binding.mangaMinimapPreviousGlassView,
            shellOverlay = binding.mangaMinimapPreviousShellOverlay,
            glassLevel = glassLevel,
            cornerRadius = cornerRadius
        )
        setupMangaMinimapControlGlassView(
            button = binding.btnMangaMinimapNext,
            glassView = binding.mangaMinimapNextGlassView,
            shellOverlay = binding.mangaMinimapNextShellOverlay,
            glassLevel = glassLevel,
            cornerRadius = cornerRadius
        )
    }

    private fun setupMangaMinimapControlGlassView(
        button: View,
        glassView: LiquidGlassView,
        shellOverlay: View,
        glassLevel: Float,
        cornerRadius: Float
    ) {
        button.clipToOutline = false
        button.background = ReaderBottomGlassStyle.fallbackShell(this, glassLevel, cornerRadius)
        shellOverlay.background = ReaderBottomGlassStyle.shell(this, glassLevel, cornerRadius)
        val shouldBind = !boundMangaMinimapGlassViewIds.contains(glassView.id)
        if (ReaderBottomGlassStyle.configureLiquidGlass(
            liquidGlassView = glassView,
            target = binding.webtoonFrame,
            cornerRadius = cornerRadius,
            bindTarget = shouldBind,
            glassLevel = glassLevel
        )) {
            boundMangaMinimapGlassViewIds.add(glassView.id)
        }
    }

    private fun updateMangaProgressMinimap(show: Boolean = binding.mangaMenu.isVisible) {
        val imageUrls = currentMangaImageUrls()
        val pageCount = imageUrls.size
        if (show) {
            binding.mangaProgressMinimap.updatePages(imageUrls, ReadManga.book?.origin, ReadManga.durChapterPos)
        } else {
            binding.mangaProgressMinimap.updateProgress(pageCount, ReadManga.durChapterPos)
        }
        binding.mangaProgressMinimapPanel.gone(!show || pageCount <= 1)
        if (!show || pageCount <= 1) {
            return
        }
        if (!mangaProgressMinimapMenuChromeReady()) {
            binding.mangaProgressMinimapPanel.gone()
            binding.mangaProgressMinimapPanel.post {
                if (binding.mangaMenu.isVisible) {
                    updateMangaProgressMinimap(show = true)
                }
            }
            return
        }
        val preservePanelPosition = binding.mangaProgressMinimap.shouldPreservePanelPosition()
        setupMangaMinimapControlGlass()
        if (!preservePanelPosition && !constrainMangaProgressMinimapPanel()) {
            binding.mangaProgressMinimapPanel.gone()
        }
    }

    private fun scheduleMangaProgressMinimapStableSync() {
        if (pendingMangaProgressMinimapLayoutSync) {
            return
        }
        pendingMangaProgressMinimapLayoutSync = true
        binding.mangaProgressMinimapPanel.post {
            binding.mangaProgressMinimapPanel.post {
                pendingMangaProgressMinimapLayoutSync = false
                if (binding.mangaMenu.isVisible &&
                    binding.mangaProgressMinimapPanel.isVisible &&
                    !binding.mangaProgressMinimap.shouldPreservePanelPosition()
                ) {
                    constrainMangaProgressMinimapPanel()
                }
            }
        }
    }

    private fun mangaProgressMinimapMenuChromeReady(): Boolean {
        val topBar = binding.mangaMenu.findViewById<View>(R.id.title_bar_shell)
        return topBar?.isVisible == true && topBar.height > 0
    }

    private fun constrainMangaProgressMinimapPanel(): Boolean {
        val root = binding.root
        if (root.height <= 0) {
            root.post {
                updateMangaProgressMinimap(show = true)
            }
            return true
        }
        val gap = 8.dpToPx()
        val topLimit = minimapTopLimit(
            root = root,
            topBar = binding.mangaMenu.findViewById(R.id.title_bar_shell),
            fallbackTop = 96.dpToPx(),
            gap = gap
        )
        val bottomLimit = minimapBottomLimit(
            root = root,
            bottomBar = binding.mangaMenu.findViewById(R.id.bottom_menu),
            fallbackBottom = 80.dpToPx(),
            gap = gap
        )
        val availableHeight = (bottomLimit - topLimit).coerceAtLeast(0)
        val controlsTopMargin = (binding.mangaProgressMinimapControls.layoutParams as? ViewGroup.MarginLayoutParams)
            ?.topMargin ?: 0
        val controlsHeight = binding.mangaProgressMinimapControls.height
            .takeIf { it > 0 }
            ?: 70.dpToPx()
        val maxMinimapHeight = availableHeight - controlsTopMargin - controlsHeight
        val minimumMinimapHeight = 96.dpToPx()
        if (maxMinimapHeight < minimumMinimapHeight) {
            return false
        }
        val minimapHeight = binding.mangaProgressMinimap.desiredHeightWithin(maxMinimapHeight)
        val panelHeight = minimapHeight + controlsTopMargin + controlsHeight
        binding.mangaProgressMinimap.setMaxAvailableHeight(maxMinimapHeight)
        binding.mangaProgressMinimapPanel.updateLayoutParams<FrameLayout.LayoutParams> {
            gravity = Gravity.END or Gravity.TOP
            topMargin = centeredMinimapPanelTopMargin(topLimit, availableHeight, panelHeight)
            height = ViewGroup.LayoutParams.WRAP_CONTENT
        }
        return true
    }

    private fun centeredMinimapPanelTopMargin(topLimit: Int, availableHeight: Int, panelHeight: Int): Int {
        return topLimit + ((availableHeight - panelHeight).coerceAtLeast(0) / 2)
    }

    private fun minimapTopLimit(root: View, topBar: View?, fallbackTop: Int, gap: Int): Int {
        val topBarBottom = topBar
            ?.takeIf { it.isVisible && it.height > 0 }
            ?.let { viewTopInRoot(root, it) + it.height }
            ?: fallbackTop
        return (topBarBottom + gap).coerceIn(0, root.height)
    }

    private fun minimapBottomLimit(root: View, bottomBar: View?, fallbackBottom: Int, gap: Int): Int {
        val bottomBarTop = bottomBar
            ?.takeIf { it.isVisible && it.height > 0 }
            ?.let { viewTopInRoot(root, it) }
            ?: (root.height - fallbackBottom)
        return (bottomBarTop - gap).coerceIn(0, root.height)
    }

    private fun viewTopInRoot(root: View, view: View): Int {
        val rootLocation = IntArray(2)
        val viewLocation = IntArray(2)
        root.getLocationOnScreen(rootLocation)
        view.getLocationOnScreen(viewLocation)
        return viewLocation[1] - rootLocation[1]
    }

    private fun previewMangaProgressMinimap(ratio: Float) {
        scrollToMangaProgress(ratio, commit = false)
    }

    private fun commitMangaProgressMinimap(ratio: Float) {
        scrollToMangaProgress(ratio, commit = true)
        updateMangaProgressMinimap(show = true)
        binding.mangaProgressMinimap.pinProgressRatio(ratio)
    }

    private fun scrollToMangaProgress(ratio: Float, commit: Boolean) {
        val pageCount = currentMangaPageCount()
        if (pageCount <= 0) {
            return
        }
        val maxPage = (pageCount - 1).coerceAtLeast(0)
        val targetPage = (ratio.coerceIn(0f, 1f) * maxPage).toInt().coerceIn(0, maxPage)
        scrollToMangaPage(targetPage, commit)
    }

    private fun scrollToMangaPage(index: Int, commit: Boolean) {
        val pageCount = currentMangaPageCount()
        if (pageCount <= 0) {
            return
        }
        val targetIndex = index.coerceIn(0, pageCount - 1)
        val itemPos = adapterPositionForMangaPage(targetIndex)
        if (itemPos <= -1) {
            return
        }
        mLayoutManager.scrollToPositionWithOffset(itemPos, 0)
        upInfoBar(mAdapter.getItem(itemPos))
        ReadManga.durChapterPos = targetIndex
        updateMangaProgressMinimap()
        if (commit) {
            ReadManga.curPageChanged()
            ReadManga.saveRead(true)
        }
    }

    private fun adapterPositionForMangaPage(index: Int): Int {
        val durChapterIndex = ReadManga.durChapterIndex
        return mAdapter.getItems().fastBinarySearch {
            val page = it as? BaseMangaPage ?: error("unknown item type")
            val chapterDelta = page.chapterIndex - durChapterIndex
            if (chapterDelta != 0) {
                chapterDelta
            } else {
                page.index - index
            }
        }
    }

    private fun currentMangaPageCount(): Int {
        return ReadManga.curMangaChapter?.imageCount ?: 0
    }

    private fun currentMangaImageUrls(): List<String> {
        return ReadManga.curMangaChapter?.pages?.filterIsInstance<MangaPage>()
            ?.map { it.mImageUrl }
            .orEmpty()
    }

    override fun onResume() {
        super.onResume()
        networkChangedListener.register()
        networkChangedListener.onNetworkChanged = {
            // 当网络是可用状态且无需初始化时同步进度（初始化中已有同步进度逻辑）
            if (AppConfig.syncBookProgressPlus && NetworkUtils.isAvailable() && !justInitData && ReadManga.inBookshelf) {
                ReadManga.syncProgress({ progress -> sureNewProgress(progress) })
            }
        }
        if (enableAutoScrollPage) {
            mScrollTimer.isEnabledPage = true
        }
        if (enableAutoScroll) {
            mScrollTimer.isEnabled = true
        }
    }

    override fun onPause() {
        super.onPause()
        if (ReadManga.inBookshelf) {
            ReadManga.saveRead()
            if (!BuildConfig.DEBUG) {
                if (AppConfig.syncBookProgressPlus) {
                    ReadManga.syncProgress()
                } else {
                    ReadManga.uploadProgress()
                }
            }
        }
        if (!BuildConfig.DEBUG) {
            Backup.autoBack(this)
        }
        ReadManga.cancelPreDownloadTask()
        networkChangedListener.unRegister()
        mScrollTimer.isEnabledPage = false
        mScrollTimer.isEnabled = false
    }

    override fun loadFail(msg: String, retry: Boolean) {
        lifecycleScope.launch {
            if (loadingViewVisible) {
                binding.llLoading.isGone = true
                binding.llRetry.isVisible = true
                binding.tvRetry.isVisible = retry
                binding.tvMsg.text = msg
            } else {
                loadMoreView.error(null, "加载失败，点击重试")
            }
        }
    }

    override fun onDestroy() {
        ReadManga.unregister(this)
        super.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        Glide.get(this).clearMemory()
    }

    override fun sureNewProgress(progress: BookProgress) {
        syncDialog?.dismiss()
        syncDialog = alert(R.string.get_book_progress) {
            setMessage(R.string.cloud_progress_exceeds_current)
            okButton {
                ReadManga.setProgress(progress)
            }
            noButton()
        }
    }

    override fun showLoading() {
        lifecycleScope.launch {
            binding.flLoading.isVisible = true
        }
    }

    override fun startLoad() {
        lifecycleScope.launch {
            loadMoreView.startLoad()
        }
    }

    override fun scrollBy(distance: Int) {
        if (!binding.recyclerView.canScroll(1)) {
            return
        }
        val time = ceil(16f / distance * 10000).toInt()
        binding.recyclerView.smoothScrollBy(10000, 10000, mLinearInterpolator, time)
    }

    override fun scrollPage() {
        scrollToNext()
    }

    override val oldBook: Book?
        get() = ReadManga.book

    override fun changeTo(source: BookSource, book: Book, toc: List<BookChapter>) {
        if (book.isImage) {
            binding.flLoading.isVisible = true
            viewModel.changeTo(book, toc)
        } else {
            toastOnUi("所选择的源不是漫画源")
        }
    }

    override fun updateColorFilter(config: MangaColorFilterConfig) {
        mAdapter.setMangaImageColorFilter(config)
        updateWindowBrightness(config.l)
    }

    @SuppressLint("StringFormatMatches")
    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.book_manga, menu)
        upMenu(menu)
        binding.mangaMenu.refreshMenuColorFilter()
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        upMenu(menu)
        return super.onPrepareOptionsMenu(menu)
    }

    /**
     * 菜单
     */
    @SuppressLint("StringFormatMatches", "NotifyDataSetChanged")
    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_change_source -> {
                binding.mangaMenu.runMenuOut()
                ReadManga.book?.let {
                    showDialogFragment(ChangeBookSourceDialog(it.name, it.author))
                }
            }

            R.id.menu_catalog -> {
                ReadManga.book?.let {
                    tocActivity.launch(it.bookUrl)
                }
            }

            R.id.menu_refresh -> {
                binding.flLoading.isVisible = true
                ReadManga.book?.let {
                    viewModel.refreshContentDur(it)
                }
            }

            R.id.menu_login -> {
                showLogin()
            }

            R.id.menu_pre_manga_number -> {
                showNumberPickerDialog(
                    0,
                    getString(R.string.pre_download),
                    AppConfig.mangaPreDownloadNum
                ) {
                    AppConfig.mangaPreDownloadNum = it
                    item.title = getString(R.string.pre_download_m, it)
                    setRecyclerViewPreloader(it)
                }
            }

            R.id.menu_disable_manga_scale -> {
                item.isChecked = !item.isChecked
                updateBookMangaReadConfig {
                    mangaPageAnim = null
                    mangaDisableScale = item.isChecked
                }
                setDisableMangaScale(item.isChecked)
            }

            R.id.menu_disable_click_scroll -> {
                item.isChecked = !item.isChecked
                updateBookMangaReadConfig {
                    mangaPageAnim = null
                    mangaDisableClickScroll = item.isChecked
                }
                setDisableClickScroll(item.isChecked)
            }

            R.id.menu_enable_auto_page -> {
                if (enableAutoScrollPage) {
                    setAutoPageEnabled(false)
                } else {
                    showNumberPickerDialog(
                        1, getString(R.string.setting_manga_auto_page_speed),
                        mangaAutoPageSpeed
                    ) {
                        updateBookMangaReadConfig {
                            mangaAutoPageSpeed = it
                        }
                        mScrollTimer.setSpeed(it)
                        setAutoPageEnabled(true)
                        binding.mangaMenu.runMenuOut()
                    }
                }
            }

            R.id.menu_manga_auto_page_speed -> {
                showNumberPickerDialog(
                    1, getString(R.string.setting_manga_auto_page_speed),
                    mangaAutoPageSpeed
                ) {
                    updateBookMangaReadConfig {
                        mangaAutoPageSpeed = it
                    }
                    item.title = getString(R.string.manga_auto_page_speed, it)
                    mScrollTimer.setSpeed(it)
                    if (enableAutoScrollPage) {
                        mScrollTimer.isEnabledPage = true
                    }
                    binding.mangaMenu.runMenuOut()
                }
            }

            R.id.menu_manga_footer_config -> {
                showDialogFragment(MangaFooterSettingDialog())
            }

            R.id.menu_enable_horizontal_scroll -> {
                item.isChecked = !item.isChecked
                updateBookMangaReadConfig {
                    mangaPageAnim = null
                    mangaHorizontalScroll = item.isChecked
                }
                mMenu?.findItem(R.id.menu_disable_horizontal_page_snap)?.isVisible =
                    item.isChecked && !mangaDisablePageAnim
                setHorizontalScroll(item.isChecked)
                mAdapter.notifyDataSetChanged()
            }

            R.id.menu_manga_color_filter -> {
                binding.mangaMenu.runMenuOut()
                showDialogFragment(MangaColorFilterDialog())
            }

            R.id.menu_enable_auto_scroll -> {
                if (enableAutoScroll) {
                    setAutoScrollEnabled(false)
                } else {
                    showNumberPickerDialog(
                        1, getString(R.string.setting_manga_auto_page_speed),
                        mangaAutoPageSpeed
                    ) {
                        updateBookMangaReadConfig {
                            mangaAutoPageSpeed = it
                        }
                        mScrollTimer.setSpeed(it)
                        setAutoScrollEnabled(true)
                        binding.mangaMenu.runMenuOut()
                    }
                }
            }

            R.id.menu_hide_manga_title -> {
                item.isChecked = !item.isChecked
                AppConfig.hideMangaTitle = item.isChecked
                ReadManga.loadContent()
            }

            R.id.menu_epaper_manga -> {
                if (AppConfig.enableMangaEInk) {
                    AppConfig.enableMangaEInk = false
                    mAdapter.enableMangaEInk(false, AppConfig.mangaEInkThreshold)
                    mMenu?.let { upMenu(it) }
                } else {
                    showDialogFragment(MangaEpaperDialog(enableOnConfirm = true))
                }
            }

            R.id.menu_epaper_manga_setting -> {
                showDialogFragment(MangaEpaperDialog())
            }

            R.id.menu_disable_horizontal_page_snap -> {
                item.isChecked = !item.isChecked
                updateBookMangaReadConfig {
                    mangaPageAnim = null
                    mangaDisableHorizontalPageSnap = item.isChecked
                }
                if (item.isChecked) {
                    mPagerSnapHelper.attachToRecyclerView(null)
                } else {
                    mPagerSnapHelper.attachToRecyclerView(binding.recyclerView)
                }
            }

            R.id.menu_gray_manga -> {
                item.isChecked = !item.isChecked
                AppConfig.enableMangaGray = item.isChecked
                mMenu?.findItem(R.id.menu_epaper_manga)?.isChecked = false
                AppConfig.enableMangaEInk = false
                mMenu?.findItem(R.id.menu_epaper_manga_setting)?.isVisible = false
                mAdapter.enableGray(item.isChecked)
            }
        }
        return super.onCompatOptionsItemSelected(item)
    }

    override fun openBookInfoActivity() {
        ReadManga.book?.let {
            bookInfoActivity.launch {
                putExtra("name", it.name)
                putExtra("author", it.author)
            }
        }
    }

    override fun showLogin() {
        val url = ReadManga.curMangaChapter?.chapter?.getAbsoluteURL()
            ?.takeIf { it.isNotBlank() }
            ?: return
        startActivity<WebViewActivity> {
            val bookSource = ReadManga.bookSource
            putExtra("title", ReadManga.curMangaChapter?.chapter?.title ?: ReadManga.book?.name)
            putExtra("url", url)
            putExtra("sourceOrigin", bookSource?.bookSourceUrl)
            putExtra("sourceName", bookSource?.bookSourceName)
            putExtra("sourceType", bookSource?.getSourceType())
        }
    }

    override fun upSystemUiVisibility(menuIsVisible: Boolean) {
        toggleSystemBar(menuIsVisible)
        updateMangaProgressMinimap(menuIsVisible)
        if (menuIsVisible) {
            scheduleMangaProgressMinimapStableSync()
        }
        if (enableAutoScroll) {
            mScrollTimer.isEnabled = !menuIsVisible
        }
        if (enableAutoScrollPage) {
            mScrollTimer.isEnabledPage = !menuIsVisible
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        val action = event.action
        val isDown = action == 0

        if (keyCode == KeyEvent.KEYCODE_MENU) {
            if (isDown && !binding.mangaMenu.canShowMenu) {
                binding.mangaMenu.runMenuIn()
                return true
            }
            if (!isDown && !binding.mangaMenu.canShowMenu) {
                binding.mangaMenu.canShowMenu = true
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun setRecyclerViewPreloader(maxPreload: Int) {
        if (mRecyclerViewPreloader != null) {
            binding.recyclerView.removeOnScrollListener(mRecyclerViewPreloader!!)
        }
        mRecyclerViewPreloader = RecyclerViewPreloader(
            Glide.with(this), mAdapter, mSizeProvider, maxPreload
        )
        binding.recyclerView.addOnScrollListener(mRecyclerViewPreloader!!)
    }

    private fun setHorizontalScroll(enable: Boolean) {
        mAdapter.isHorizontal = enable
        if (enable) {
            if (!enableAutoScroll) {
                if (mangaDisableHorizontalPageSnap || mangaDisablePageAnim) {
                    mPagerSnapHelper.attachToRecyclerView(null)
                } else {
                    mPagerSnapHelper.attachToRecyclerView(binding.recyclerView)
                }
            }
            mLayoutManager.orientation = LinearLayoutManager.HORIZONTAL
        } else {
            mPagerSnapHelper.attachToRecyclerView(null)
            mLayoutManager.orientation = LinearLayoutManager.VERTICAL
        }
    }

    private fun setAutoPageEnabled(enable: Boolean) {
        enableAutoScrollPage = enable
        enableAutoScroll = false
        mScrollTimer.isEnabledPage = enable
        mScrollTimer.isEnabled = false
        mMenu?.let { upMenu(it) }
    }

    private fun setAutoScrollEnabled(enable: Boolean) {
        enableAutoScroll = enable
        enableAutoScrollPage = false
        mScrollTimer.isEnabled = enable
        mScrollTimer.isEnabledPage = false
        if (enable) {
            mPagerSnapHelper.attachToRecyclerView(null)
        } else if (mangaHorizontalScroll) {
            mPagerSnapHelper.attachToRecyclerView(binding.recyclerView)
        }
        mMenu?.let { upMenu(it) }
    }

    @SuppressLint("StringFormatMatches")
    private fun upMenu(menu: Menu) {
        this.mMenu = menu
        menu.findItem(R.id.menu_pre_manga_number).title =
            getString(R.string.pre_download_m, AppConfig.mangaPreDownloadNum)
        menu.findItem(R.id.menu_disable_manga_scale).isChecked = mangaDisableScale
        menu.findItem(R.id.menu_disable_click_scroll).isChecked = mangaDisableClickScroll
        menu.findItem(R.id.menu_enable_auto_page).isChecked = enableAutoScrollPage
        menu.findItem(R.id.menu_enable_auto_scroll).isChecked = enableAutoScroll
        menu.findItem(R.id.menu_manga_auto_page_speed).title =
            getString(R.string.manga_auto_page_speed, mangaAutoPageSpeed)
        menu.findItem(R.id.menu_manga_auto_page_speed).isVisible =
            enableAutoScrollPage || enableAutoScroll
        menu.findItem(R.id.menu_enable_horizontal_scroll).isChecked =
            mangaHorizontalScroll
        menu.findItem(R.id.menu_hide_manga_title).isChecked = AppConfig.hideMangaTitle
        menu.findItem(R.id.menu_epaper_manga).isChecked = AppConfig.enableMangaEInk
        menu.findItem(R.id.menu_epaper_manga_setting).isVisible = AppConfig.enableMangaEInk
        menu.findItem(R.id.menu_disable_horizontal_page_snap).run {
            isVisible = mangaHorizontalScroll && !mangaDisablePageAnim
            isChecked = mangaDisableHorizontalPageSnap || mangaDisablePageAnim
        }
        menu.findItem(R.id.menu_login)?.isVisible =
            ReadManga.bookSource != null
        menu.findItem(R.id.menu_gray_manga).isChecked = AppConfig.enableMangaGray
    }

    private fun applyBookMangaReadConfig() {
        setHorizontalScroll(mangaHorizontalScroll)
        setDisableClickScroll(mangaDisableClickScroll)
        setDisableMangaScale(mangaDisableScale)
        mScrollTimer.setSpeed(mangaAutoPageSpeed)
        mMenu?.let { upMenu(it) }
    }

    private fun updateBookMangaReadConfig(block: Book.ReadConfig.() -> Unit) {
        val book = ReadManga.book ?: return
        book.config.block()
        lifecycleScope.launch(IO) {
            book.save()
        }
    }

    private fun setDisableMangaScale(disable: Boolean) {
        binding.webtoonFrame.disableMangaScale = disable
        binding.recyclerView.disableMangaScale = disable
        if (disable) {
            binding.recyclerView.resetZoom()
        }
    }

    private fun setDisableClickScroll(disable: Boolean) {
        binding.webtoonFrame.disabledClickScroll = disable
    }

    private fun upLayoutInDisplayCutoutMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }

    private fun scrollToNext() {
        scrollPageTo(1)
    }

    private fun scrollToPrev() {
        scrollPageTo(-1)
    }

    private fun scrollPageTo(direction: Int) {
        if (!binding.recyclerView.canScroll(direction)) {
            return
        }
        var dx = 0
        var dy = 0
        if (mangaHorizontalScroll) {
            dx = binding.recyclerView.run {
                width - paddingStart - paddingEnd
            }
        } else {
            dy = binding.recyclerView.run {
                height - paddingTop - paddingBottom
            }
        }
        dx *= direction
        dy *= direction
        if (mangaDisablePageAnim) {
            binding.recyclerView.scrollBy(dx, dy)
        } else {
            binding.recyclerView.smoothScrollBy(dx, dy)
        }
    }

    private fun showNumberPickerDialog(
        min: Int,
        title: String,
        initValue: Int,
        callback: (Int) -> Unit,
    ) {
        NumberPickerDialog(this)
            .setTitle(title)
            .setMaxValue(9999)
            .setMinValue(min)
            .setValue(initValue)
            .show {
                callback.invoke(it)
            }
    }

    override fun finish() {
        val book = ReadManga.book ?: return super.finish()

        if (ReadManga.inBookshelf) {
            return super.finish()
        }

        if (!AppConfig.showAddToShelfAlert) {
            viewModel.removeFromBookshelf { super.finish() }
        } else {
            alert(title = getString(R.string.add_to_bookshelf)) {
                setMessage(getString(R.string.check_add_bookshelf, book.name))
                okButton {
                    ReadManga.book?.removeType(BookType.notShelf)
                    ReadManga.book?.save()
                    ReadManga.inBookshelf = true
                    setResult(RESULT_OK)
                }
                noButton { viewModel.removeFromBookshelf { super.finish() } }
            }
        }
    }

    fun updateWindowBrightness(brightness: Int) {
        val layoutParams = window.attributes
        val normalizedBrightness = brightness.toFloat() / 255.0f
        layoutParams.screenBrightness = normalizedBrightness.coerceIn(0f, 1f)
        window.attributes = layoutParams
        // 强制刷新屏幕
        window.decorView.postInvalidate()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                scrollToPrev()
                return true
            }

            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                scrollToNext()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun previewEpaper(enable: Boolean, value: Int) {
        if (enable) {
            mAdapter.enableMangaEInk(true, value)
        } else {
            mAdapter.enableMangaEInk(false, value)
        }
    }

    override fun restoreEpaper(enable: Boolean, value: Int) {
        mAdapter.enableMangaEInk(enable, value)
    }

    override fun enableEpaper(value: Int) {
        AppConfig.enableMangaEInk = true
        AppConfig.enableMangaGray = false
        mAdapter.enableMangaEInk(true, value)
        mMenu?.let { upMenu(it) }
        binding.mangaMenu.runMenuOut()
    }

    override fun onEpaperSettingConfirmed() {
        mMenu?.let { upMenu(it) }
        binding.mangaMenu.runMenuOut()
    }
}
