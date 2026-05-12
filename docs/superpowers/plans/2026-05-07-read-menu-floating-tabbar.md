# Read Menu Floating Tabbar Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the reading screen bottom menu with a floating main tabbar island, a right-to-left secondary settings bar, and upward-expanding settings panels while keeping the top bar unchanged.

**Architecture:** Keep the work inside the existing `ReadMenu` view and `view_read_menu.xml` binding so current callbacks, menu visibility, and read-page tap behavior continue to work. The layout will contain mutually exclusive bottom states: main tabbar, secondary settings bar, and an optional expanded panel above the active bar.

**Tech Stack:** Android XML layouts/drawables/animations, Kotlin custom view logic, ViewBinding, existing Legado `ReadMenu.CallBack` methods, Gradle Android build checks.

---

## File Structure

- Modify `app/src/main/res/layout/view_read_menu.xml`
  - Replace the existing bottom `LinearLayout` body with a floating bottom container.
  - Preserve IDs used by existing Kotlin where possible: `bottom_menu`, `ll_brightness`, `seek_read_page`, `tv_pre`, `tv_next`, `ll_catalog`, `ll_read_aloud`, `ll_font`, `ll_setting`.
  - Add IDs for new UI state: `fl_expanded_panel`, `main_tab_bar`, `settings_tab_bar`, `ll_settings_back`, `ll_settings_layout`, `ll_settings_theme`, `ll_settings_page`, `ll_settings_more`, `panel_layout`, `panel_theme`, `panel_page`, `panel_more`.
- Modify `app/src/main/java/io/legado/app/ui/book/read/ReadMenu.kt`
  - Add state management for main tabbar, secondary settings bar, and expanded panels.
  - Keep top-bar color, title, and animation logic intact.
  - Route complex settings to existing callbacks where first-pass inline controls are too risky.
- Create `app/src/main/res/drawable/bg_read_menu_island.xml`
  - Rounded floating island background for the main and secondary bars.
- Create `app/src/main/res/drawable/bg_read_menu_panel.xml`
  - Rounded top-corner panel background for upward expanded panels.
- Create `app/src/main/res/drawable/bg_read_menu_panel_button.xml`
  - Light rounded button background for panel shortcuts.
- Create `app/src/main/res/anim/anim_readbook_bar_in_from_right.xml`
  - Right-to-left replacement animation for the secondary settings bar.
- Create `app/src/main/res/anim/anim_readbook_bar_out_to_right.xml`
  - Reverse animation for returning to the main tabbar.
- Create `app/src/main/res/anim/anim_readbook_panel_in_up.xml`
  - Upward panel expansion animation.
- Create `app/src/main/res/anim/anim_readbook_panel_out_down.xml`
  - Downward panel dismissal animation.
- Test by running `./gradlew.bat :app:assembleAppDebug`.

User instruction: do not commit. This plan intentionally omits commit steps.

---

### Task 1: Add Menu Shape And Animation Resources

**Files:**
- Create: `app/src/main/res/drawable/bg_read_menu_island.xml`
- Create: `app/src/main/res/drawable/bg_read_menu_panel.xml`
- Create: `app/src/main/res/drawable/bg_read_menu_panel_button.xml`
- Create: `app/src/main/res/anim/anim_readbook_bar_in_from_right.xml`
- Create: `app/src/main/res/anim/anim_readbook_bar_out_to_right.xml`
- Create: `app/src/main/res/anim/anim_readbook_panel_in_up.xml`
- Create: `app/src/main/res/anim/anim_readbook_panel_out_down.xml`

- [ ] **Step 1: Create island background**

Create `app/src/main/res/drawable/bg_read_menu_island.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <solid android:color="@color/background_menu" />
    <corners android:radius="28dp" />
    <padding
        android:bottom="6dp"
        android:left="8dp"
        android:right="8dp"
        android:top="6dp" />
</shape>
```

- [ ] **Step 2: Create expanded panel background**

Create `app/src/main/res/drawable/bg_read_menu_panel.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <solid android:color="@color/background_menu" />
    <corners
        android:topLeftRadius="28dp"
        android:topRightRadius="28dp" />
    <padding
        android:bottom="16dp"
        android:left="20dp"
        android:right="20dp"
        android:top="18dp" />
</shape>
```

- [ ] **Step 3: Create panel shortcut button background**

Create `app/src/main/res/drawable/bg_read_menu_panel_button.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <solid android:color="@color/transparent10" />
    <corners android:radius="14dp" />
    <stroke
        android:width="1dp"
        android:color="@color/transparent30" />
    <padding
        android:bottom="10dp"
        android:left="12dp"
        android:right="12dp"
        android:top="10dp" />
</shape>
```

- [ ] **Step 4: Create right-to-left bar animation**

Create `app/src/main/res/anim/anim_readbook_bar_in_from_right.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<translate xmlns:android="http://schemas.android.com/apk/res/android"
    android:duration="180"
    android:fromXDelta="100%"
    android:interpolator="@android:interpolator/decelerate_quad"
    android:toXDelta="0%" />
```

- [ ] **Step 5: Create reverse bar animation**

Create `app/src/main/res/anim/anim_readbook_bar_out_to_right.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<translate xmlns:android="http://schemas.android.com/apk/res/android"
    android:duration="160"
    android:fromXDelta="0%"
    android:interpolator="@android:interpolator/accelerate_quad"
    android:toXDelta="100%" />
```

- [ ] **Step 6: Create upward panel animation**

Create `app/src/main/res/anim/anim_readbook_panel_in_up.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<translate xmlns:android="http://schemas.android.com/apk/res/android"
    android:duration="180"
    android:fromYDelta="100%"
    android:interpolator="@android:interpolator/decelerate_quad"
    android:toYDelta="0%" />
```

- [ ] **Step 7: Create downward panel animation**

Create `app/src/main/res/anim/anim_readbook_panel_out_down.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<translate xmlns:android="http://schemas.android.com/apk/res/android"
    android:duration="150"
    android:fromYDelta="0%"
    android:interpolator="@android:interpolator/accelerate_quad"
    android:toYDelta="100%" />
```

---

### Task 2: Replace Bottom Menu Layout With Main Tabbar, Secondary Bar, And Panels

**Files:**
- Modify: `app/src/main/res/layout/view_read_menu.xml`

- [ ] **Step 1: Replace the current `bottom_menu` block**

In `view_read_menu.xml`, replace the existing `<LinearLayout android:id="@+id/bottom_menu" ...>` block with this structure. Keep the existing `title_bar` and `ll_brightness` blocks unchanged.

```xml
<FrameLayout
    android:id="@+id/bottom_menu"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:clipChildren="false"
    android:clipToPadding="false"
    android:paddingStart="16dp"
    android:paddingEnd="16dp"
    android:paddingBottom="16dp"
    app:layout_constraintBottom_toBottomOf="parent">

    <FrameLayout
        android:id="@+id/fl_expanded_panel"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_gravity="bottom"
        android:layout_marginBottom="82dp"
        android:background="@drawable/bg_read_menu_panel"
        android:visibility="gone">

        <LinearLayout
            android:id="@+id/panel_layout"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:visibility="gone">

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="@string/compose_type"
                android:textStyle="bold"
                android:textSize="18sp" />

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="14dp"
                android:orientation="horizontal">

                <TextView
                    android:id="@+id/tv_pre"
                    android:layout_width="wrap_content"
                    android:layout_height="44dp"
                    android:background="@drawable/bg_read_menu_panel_button"
                    android:gravity="center"
                    android:text="@string/previous_chapter" />

                <io.legado.app.lib.theme.view.ThemeSeekBar
                    android:id="@+id/seek_read_page"
                    android:layout_width="0dp"
                    android:layout_height="44dp"
                    android:layout_gravity="center_vertical"
                    android:layout_marginHorizontal="12dp"
                    android:layout_weight="1" />

                <TextView
                    android:id="@+id/tv_next"
                    android:layout_width="wrap_content"
                    android:layout_height="44dp"
                    android:background="@drawable/bg_read_menu_panel_button"
                    android:gravity="center"
                    android:text="@string/next_chapter" />
            </LinearLayout>
        </LinearLayout>

        <LinearLayout
            android:id="@+id/panel_theme"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:visibility="gone">

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="@string/theme_config"
                android:textStyle="bold"
                android:textSize="18sp" />

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="14dp"
                android:orientation="horizontal">

                <TextView
                    android:id="@+id/panel_theme_read_style"
                    android:layout_width="0dp"
                    android:layout_height="48dp"
                    android:layout_marginEnd="8dp"
                    android:layout_weight="1"
                    android:background="@drawable/bg_read_menu_panel_button"
                    android:gravity="center"
                    android:text="@string/read_config" />

                <TextView
                    android:id="@+id/panel_theme_night"
                    android:layout_width="0dp"
                    android:layout_height="48dp"
                    android:layout_marginStart="8dp"
                    android:layout_weight="1"
                    android:background="@drawable/bg_read_menu_panel_button"
                    android:gravity="center"
                    android:text="@string/dark_theme" />
            </LinearLayout>
        </LinearLayout>

        <LinearLayout
            android:id="@+id/panel_page"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:visibility="gone">

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="@string/read_style_page"
                android:textStyle="bold"
                android:textSize="18sp" />

            <TextView
                android:id="@+id/panel_page_anim"
                android:layout_width="match_parent"
                android:layout_height="48dp"
                android:layout_marginTop="14dp"
                android:background="@drawable/bg_read_menu_panel_button"
                android:gravity="center"
                android:text="@string/page_anim" />
        </LinearLayout>

        <LinearLayout
            android:id="@+id/panel_more"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:visibility="gone">

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="@string/more_menu"
                android:textStyle="bold"
                android:textSize="18sp" />

            <TextView
                android:id="@+id/panel_more_settings"
                android:layout_width="match_parent"
                android:layout_height="48dp"
                android:layout_marginTop="14dp"
                android:background="@drawable/bg_read_menu_panel_button"
                android:gravity="center"
                android:text="@string/setting" />
        </LinearLayout>
    </FrameLayout>

    <LinearLayout
        android:id="@+id/main_tab_bar"
        android:layout_width="match_parent"
        android:layout_height="64dp"
        android:layout_gravity="bottom"
        android:background="@drawable/bg_read_menu_island"
        android:gravity="center"
        android:orientation="horizontal">

        <LinearLayout
            android:id="@+id/ll_catalog"
            android:layout_width="0dp"
            android:layout_height="match_parent"
            android:layout_weight="1"
            android:background="?android:attr/selectableItemBackgroundBorderless"
            android:clickable="true"
            android:focusable="true"
            android:gravity="center"
            android:orientation="vertical">

            <androidx.appcompat.widget.AppCompatImageView
                android:id="@+id/iv_catalog"
                android:layout_width="24dp"
                android:layout_height="24dp"
                android:contentDescription="@string/chapter_list"
                android:src="@drawable/ic_toc" />

            <TextView
                android:id="@+id/tv_catalog"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="@string/chapter_list"
                android:textSize="12sp" />
        </LinearLayout>

        <LinearLayout
            android:id="@+id/ll_read_aloud"
            android:layout_width="0dp"
            android:layout_height="match_parent"
            android:layout_weight="1"
            android:background="?android:attr/selectableItemBackgroundBorderless"
            android:clickable="true"
            android:focusable="true"
            android:gravity="center"
            android:orientation="vertical">

            <androidx.appcompat.widget.AppCompatImageView
                android:id="@+id/iv_read_aloud"
                android:layout_width="24dp"
                android:layout_height="24dp"
                android:contentDescription="@string/read_aloud"
                android:src="@drawable/ic_read_aloud" />

            <TextView
                android:id="@+id/tv_read_aloud"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="@string/read_aloud"
                android:textSize="12sp" />
        </LinearLayout>

        <LinearLayout
            android:id="@+id/ll_font"
            android:layout_width="0dp"
            android:layout_height="match_parent"
            android:layout_weight="1"
            android:background="?android:attr/selectableItemBackgroundBorderless"
            android:clickable="true"
            android:focusable="true"
            android:gravity="center"
            android:orientation="vertical">

            <androidx.appcompat.widget.AppCompatImageView
                android:id="@+id/iv_font"
                android:layout_width="24dp"
                android:layout_height="24dp"
                android:contentDescription="@string/interface_setting"
                android:src="@drawable/ic_interface_setting" />

            <TextView
                android:id="@+id/tv_font"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="@string/interface_setting"
                android:textSize="12sp" />
        </LinearLayout>

        <LinearLayout
            android:id="@+id/ll_setting"
            android:layout_width="0dp"
            android:layout_height="match_parent"
            android:layout_weight="1"
            android:background="?android:attr/selectableItemBackgroundBorderless"
            android:clickable="true"
            android:focusable="true"
            android:gravity="center"
            android:orientation="vertical">

            <androidx.appcompat.widget.AppCompatImageView
                android:id="@+id/iv_setting"
                android:layout_width="24dp"
                android:layout_height="24dp"
                android:contentDescription="@string/setting"
                android:src="@drawable/ic_settings" />

            <TextView
                android:id="@+id/tv_setting"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="@string/setting"
                android:textSize="12sp" />
        </LinearLayout>
    </LinearLayout>

    <LinearLayout
        android:id="@+id/settings_tab_bar"
        android:layout_width="match_parent"
        android:layout_height="64dp"
        android:layout_gravity="bottom"
        android:background="@drawable/bg_read_menu_island"
        android:gravity="center"
        android:orientation="horizontal"
        android:visibility="gone">

        <LinearLayout
            android:id="@+id/ll_settings_back"
            android:layout_width="0dp"
            android:layout_height="match_parent"
            android:layout_weight="1"
            android:background="?android:attr/selectableItemBackgroundBorderless"
            android:clickable="true"
            android:focusable="true"
            android:gravity="center"
            android:orientation="vertical">

            <androidx.appcompat.widget.AppCompatImageView
                android:id="@+id/iv_settings_back"
                android:layout_width="24dp"
                android:layout_height="24dp"
                android:contentDescription="@string/text_return"
                android:src="@drawable/ic_arrow_back" />
        </LinearLayout>

        <TextView
            android:id="@+id/ll_settings_layout"
            android:layout_width="0dp"
            android:layout_height="match_parent"
            android:layout_weight="1"
            android:background="?android:attr/selectableItemBackgroundBorderless"
            android:clickable="true"
            android:focusable="true"
            android:gravity="center"
            android:text="@string/compose_type"
            android:textSize="12sp" />

        <TextView
            android:id="@+id/ll_settings_theme"
            android:layout_width="0dp"
            android:layout_height="match_parent"
            android:layout_weight="1"
            android:background="?android:attr/selectableItemBackgroundBorderless"
            android:clickable="true"
            android:focusable="true"
            android:gravity="center"
            android:text="@string/theme_config"
            android:textSize="12sp" />

        <TextView
            android:id="@+id/ll_settings_page"
            android:layout_width="0dp"
            android:layout_height="match_parent"
            android:layout_weight="1"
            android:background="?android:attr/selectableItemBackgroundBorderless"
            android:clickable="true"
            android:focusable="true"
            android:gravity="center"
            android:text="@string/read_style_page"
            android:textSize="12sp" />

        <TextView
            android:id="@+id/ll_settings_more"
            android:layout_width="0dp"
            android:layout_height="match_parent"
            android:layout_weight="1"
            android:background="?android:attr/selectableItemBackgroundBorderless"
            android:clickable="true"
            android:focusable="true"
            android:gravity="center"
            android:text="@string/more_menu"
            android:textSize="12sp" />
    </LinearLayout>
</FrameLayout>
```

- [ ] **Step 2: Update brightness constraints**

In the existing `ll_brightness` element, keep:

```xml
app:layout_constraintBottom_toTopOf="@+id/bottom_menu"
```

This preserves the current brightness control positioning above the bottom menu.

---

### Task 3: Add ReadMenu State Management

**Files:**
- Modify: `app/src/main/java/io/legado/app/ui/book/read/ReadMenu.kt`

- [ ] **Step 1: Add state fields and animation properties near existing animation fields**

Add this inside `ReadMenu` after `private var isMenuOutAnimating = false`:

```kotlin
private enum class BottomBarState {
    Main,
    Settings
}

private enum class ExpandedPanel {
    Layout,
    Theme,
    Page,
    More
}

private var bottomBarState = BottomBarState.Main
private var expandedPanel: ExpandedPanel? = null
private val settingsBarIn: Animation by lazy {
    loadAnimation(context, R.anim.anim_readbook_bar_in_from_right)
}
private val settingsBarOut: Animation by lazy {
    loadAnimation(context, R.anim.anim_readbook_bar_out_to_right)
}
private val panelIn: Animation by lazy {
    loadAnimation(context, R.anim.anim_readbook_panel_in_up)
}
private val panelOut: Animation by lazy {
    loadAnimation(context, R.anim.anim_readbook_panel_out_down)
}
```

- [ ] **Step 2: Add bottom state helper methods before `bindEvent()`**

Add these methods:

```kotlin
private fun showMainTabBar(anim: Boolean = !AppConfig.isEInkMode) = binding.run {
    hideExpandedPanel(anim = false)
    bottomBarState = BottomBarState.Main
    settingsTabBar.gone()
    mainTabBar.visible()
    if (anim) {
        settingsTabBar.startAnimation(settingsBarOut)
    }
}

private fun showSettingsTabBar(anim: Boolean = !AppConfig.isEInkMode) = binding.run {
    hideExpandedPanel(anim = false)
    bottomBarState = BottomBarState.Settings
    mainTabBar.gone()
    settingsTabBar.visible()
    if (anim) {
        settingsTabBar.startAnimation(settingsBarIn)
    }
}

private fun toggleExpandedPanel(panel: ExpandedPanel) {
    if (expandedPanel == panel && binding.flExpandedPanel.isVisible) {
        hideExpandedPanel()
        return
    }
    expandedPanel = panel
    binding.panelLayout.gone(panel != ExpandedPanel.Layout)
    binding.panelTheme.gone(panel != ExpandedPanel.Theme)
    binding.panelPage.gone(panel != ExpandedPanel.Page)
    binding.panelMore.gone(panel != ExpandedPanel.More)
    binding.flExpandedPanel.visible()
    if (!AppConfig.isEInkMode) {
        binding.flExpandedPanel.startAnimation(panelIn)
    }
}

private fun hideExpandedPanel(anim: Boolean = !AppConfig.isEInkMode) {
    if (!binding.flExpandedPanel.isVisible) {
        expandedPanel = null
        return
    }
    expandedPanel = null
    if (anim) {
        panelOut.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) = Unit
            override fun onAnimationRepeat(animation: Animation) = Unit
            override fun onAnimationEnd(animation: Animation) {
                binding.flExpandedPanel.gone()
                panelOut.setAnimationListener(null)
            }
        })
        binding.flExpandedPanel.startAnimation(panelOut)
    } else {
        binding.flExpandedPanel.gone()
    }
}

private fun handleBackgroundDismiss() {
    when {
        binding.flExpandedPanel.isVisible -> hideExpandedPanel()
        bottomBarState == BottomBarState.Settings -> showMainTabBar()
        else -> runMenuOut()
    }
}
```

- [ ] **Step 3: Reset bottom state when the menu opens**

In `runMenuIn`, after:

```kotlin
binding.bottomMenu.visible()
```

add:

```kotlin
showMainTabBar(anim = false)
```

- [ ] **Step 4: Hide new state when the menu closes**

In `menuOutListener.onAnimationEnd`, after:

```kotlin
binding.bottomMenu.invisible()
```

add:

```kotlin
binding.mainTabBar.visible()
binding.settingsTabBar.gone()
binding.flExpandedPanel.gone()
bottomBarState = BottomBarState.Main
expandedPanel = null
```

- [ ] **Step 5: Route background tap through layered dismiss**

Replace both existing background click assignments:

```kotlin
binding.vwMenuBg.setOnClickListener { runMenuOut() }
```

with:

```kotlin
binding.vwMenuBg.setOnClickListener { handleBackgroundDismiss() }
```

The two locations are inside `menuInListener.onAnimationEnd` and `bindEvent()`.

---

### Task 4: Rebind Bottom Actions To New Bars And Panels

**Files:**
- Modify: `app/src/main/java/io/legado/app/ui/book/read/ReadMenu.kt`

- [ ] **Step 1: Keep main tabbar actions**

In `bindEvent()`, keep these existing actions unchanged:

```kotlin
llCatalog.setOnClickListener {
    runMenuOut {
        callBack.openChapterList()
    }
}

llReadAloud.setOnClickListener {
    runMenuOut {
        callBack.onClickReadAloud()
    }
}

llReadAloud.onLongClick {
    runMenuOut {
        callBack.showReadAloudDialog()
    }
}
```

- [ ] **Step 2: Change interface/layout item to secondary bar switch**

Replace the current `llFont.setOnClickListener` body with:

```kotlin
llFont.setOnClickListener {
    showSettingsTabBar()
}
```

- [ ] **Step 3: Keep settings item as direct more settings**

Keep or restore the existing settings action:

```kotlin
llSetting.setOnClickListener {
    runMenuOut {
        callBack.showMoreSetting()
    }
}
```

- [ ] **Step 4: Add secondary bar actions**

Add these inside `bindEvent()` after the main tabbar listeners:

```kotlin
llSettingsBack.setOnClickListener {
    showMainTabBar()
}

llSettingsLayout.setOnClickListener {
    toggleExpandedPanel(ExpandedPanel.Layout)
}

llSettingsTheme.setOnClickListener {
    toggleExpandedPanel(ExpandedPanel.Theme)
}

llSettingsPage.setOnClickListener {
    toggleExpandedPanel(ExpandedPanel.Page)
}

llSettingsMore.setOnClickListener {
    toggleExpandedPanel(ExpandedPanel.More)
}
```

- [ ] **Step 5: Add expanded panel shortcut actions**

Add these inside `bindEvent()`:

```kotlin
panelThemeReadStyle.setOnClickListener {
    hideExpandedPanel(anim = false)
    val pageBgColor = kotlin.runCatching {
        Color.parseColor(ReadBookConfig.durConfig.curBgStr())
    }.getOrDefault(bgColor)
    val pageTextColor = ReadBookConfig.durConfig.curTextColor()
    titleBar.setBackgroundColor(pageBgColor)
    titleBar.setTextColor(pageTextColor)
    titleBar.setColorFilter(pageTextColor)
    titleBarAddition.gone()
    bottomMenu.invisible()
    llBrightness.invisible()
    callBack.showReadStyle()
}

panelThemeNight.setOnClickListener {
    AppConfig.isNightTheme = !AppConfig.isNightTheme
    ThemeConfig.applyDayNight(context)
}

panelPageAnim.setOnClickListener {
    runMenuOut {
        activity?.let { owner ->
            if (owner is BaseReadBookActivity) {
                owner.showPageAnimConfig {
                    ReadBook.loadContent(resetPageOffset = false)
                }
            }
        }
    }
}

panelMoreSettings.setOnClickListener {
    runMenuOut {
        callBack.showMoreSetting()
    }
}
```

If `ReadBook.loadContent(resetPageOffset = false)` is not available from this scope during compilation, replace the success block with:

```kotlin
postEvent(EventBus.UP_CONFIG, arrayListOf(5))
```

and add imports for `io.legado.app.constant.EventBus` and `io.legado.app.utils.postEvent`.

---

### Task 5: Apply Colors And Navigation Padding To New Elements

**Files:**
- Modify: `app/src/main/java/io/legado/app/ui/book/read/ReadMenu.kt`

- [ ] **Step 1: Set island and panel backgrounds in `initView()`**

In `initView()`, replace the old bottom background handling:

```kotlin
if (AppConfig.isEInkMode) {
    titleBar.setBackgroundResource(R.drawable.bg_eink_border_bottom)
    llBottomBg.setBackgroundResource(R.drawable.bg_eink_border_top)
} else {
    llBottomBg.setBackgroundColor(bgColor)
}
```

with:

```kotlin
if (AppConfig.isEInkMode) {
    titleBar.setBackgroundResource(R.drawable.bg_eink_border_bottom)
} else {
    mainTabBar.backgroundTintList = bottomBackgroundList
    settingsTabBar.backgroundTintList = bottomBackgroundList
    flExpandedPanel.backgroundTintList = bottomBackgroundList
}
```

- [ ] **Step 2: Tint new main tabbar icons and labels**

In `initView()`, keep existing tint calls for:

```kotlin
ivCatalog.setColorFilter(textColor, PorterDuff.Mode.SRC_IN)
tvCatalog.setTextColor(textColor)
ivReadAloud.setColorFilter(textColor, PorterDuff.Mode.SRC_IN)
tvReadAloud.setTextColor(textColor)
ivFont.setColorFilter(textColor, PorterDuff.Mode.SRC_IN)
tvFont.setTextColor(textColor)
ivSetting.setColorFilter(textColor, PorterDuff.Mode.SRC_IN)
tvSetting.setTextColor(textColor)
```

- [ ] **Step 3: Tint secondary bar and panel text**

Add this after the existing tint calls:

```kotlin
ivSettingsBack.setColorFilter(textColor, PorterDuff.Mode.SRC_IN)
llSettingsLayout.setTextColor(textColor)
llSettingsTheme.setTextColor(textColor)
llSettingsPage.setTextColor(textColor)
llSettingsMore.setTextColor(textColor)
panelThemeReadStyle.setTextColor(textColor)
panelThemeNight.setTextColor(textColor)
panelPageAnim.setTextColor(textColor)
panelMoreSettings.setTextColor(textColor)
```

- [ ] **Step 4: Apply navigation padding to the bottom menu**

Keep the existing call:

```kotlin
applyNavigationBarPadding()
```

If the new island sits too high or too low after build/manual check, change it to:

```kotlin
bottomMenu.applyNavigationBarPadding()
```

and keep `applyNavigationBarPadding()` removed only if there is double padding.

---

### Task 6: Build And Manual Verification

**Files:**
- Verify: `app/src/main/res/layout/view_read_menu.xml`
- Verify: `app/src/main/java/io/legado/app/ui/book/read/ReadMenu.kt`

- [ ] **Step 1: Run a debug build**

Run:

```powershell
.\gradlew.bat :app:assembleAppDebug
```

Expected:

```text
BUILD SUCCESSFUL
```

- [ ] **Step 2: Fix generated binding errors if any**

If the build reports an unresolved binding ID, compare the reported property name to the XML ID. For example:

```text
Unresolved reference: panelThemeReadStyle
```

means XML must contain:

```xml
android:id="@+id/panel_theme_read_style"
```

Re-run:

```powershell
.\gradlew.bat :app:assembleAppDebug
```

- [ ] **Step 3: Manual interaction check on device or emulator**

Open a text book in the reader and verify:

```text
1. Tap the reading page: top bar appears, bottom main tabbar island appears.
2. Tap "界面": main tabbar is replaced by the secondary bar sliding from right to left.
3. Tap "排版": the layout panel expands upward.
4. Tap background: the panel closes, secondary bar remains.
5. Tap background again: secondary bar returns to main tabbar.
6. Tap background again: the full menu hides.
7. Tap "目录": chapter list still opens.
8. Tap "朗读": read aloud behavior still starts or pauses.
9. Tap "设置": more settings still opens.
10. Top title bar still shows the chapter title and keeps its original behavior.
```

---

## Self-Review Notes

- Spec coverage:
  - Bottom menu becomes floating tabbar: Task 2.
  - Secondary bar replaces main tabbar from right to left: Tasks 1, 3, 4.
  - Panels expand upward: Tasks 1, 2, 3, 4.
  - Top bar unchanged: Tasks 2 and 3 explicitly preserve `title_bar` behavior.
  - Existing callbacks reused: Task 4.
- Placeholder scan:
  - No forbidden placeholder markers remain.
  - All created files include concrete content.
- Type consistency:
  - XML IDs map to ViewBinding names used in Kotlin: `fl_expanded_panel` -> `flExpandedPanel`, `settings_tab_bar` -> `settingsTabBar`, `panel_theme_read_style` -> `panelThemeReadStyle`.
