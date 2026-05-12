# Read Menu Floating Tabbar Design

## Goal

Update the book reading screen bottom menu so it behaves like a floating tabbar island. The top reading menu remains unchanged.

## Scope

- Replace the existing bottom menu presentation in `ReadMenu` with a floating main tabbar island.
- Keep the existing top `TitleBar`, chapter/source display, and top menu animation behavior.
- Keep the existing read page tap entry point: tapping the reading area shows the menu overlay.
- Add a secondary bar that replaces the main bottom tabbar when the user taps the main tabbar's interface/layout entry.
- Add upward-expanding panels from the secondary bar for layout, theme, page-turning, and more settings.

## Interaction

1. Tapping the reading page shows the overlay:
   - The top bar appears as it does today.
   - The bottom floating main tabbar appears as the bottom menu.
2. Tapping the main tabbar's interface/layout item switches the bottom area:
   - The secondary settings bar slides in from right to left.
   - The secondary bar replaces the main tabbar instead of stacking under it.
3. The secondary bar contains:
   - Back
   - Layout
   - Theme
   - Page
   - More
4. Tapping a secondary item expands the matching panel upward from the bottom.
5. Dismiss behavior:
   - If a panel is open, tapping the dim/background area closes the panel first.
   - If no panel is open and the secondary bar is visible, back returns to the main tabbar.
   - If the main tabbar is visible, background tap hides the full read menu overlay.

## Visual Direction

- The bottom menu uses a rounded floating island with horizontal tab items.
- The secondary bar uses the same island shape and replaces the main tabbar with a right-to-left transition.
- Expanded panels use large rounded top corners and occupy the lower portion of the screen.
- Existing theme colors and immersive read-bar behavior should continue to drive background and text colors where practical.

## Implementation Boundaries

- Primary files:
  - `app/src/main/res/layout/view_read_menu.xml`
  - `app/src/main/java/io/legado/app/ui/book/read/ReadMenu.kt`
- Add drawable or animation XML only if existing resources cannot express the new island and slide behavior.
- Do not change core reading, pagination, chapter loading, search, or text selection behavior.
- Existing callbacks should be reused for actions such as chapter list, read aloud, read style, and more settings.

## Testing

- Add focused tests for menu state transitions if the project has a suitable JVM-testable boundary.
- If the existing menu is not practical to unit test directly, verify with a build-level check and manual interaction notes.
- Validate:
  - Main tabbar opens from the read page.
  - Interface/layout item replaces the main tabbar with the secondary bar.
  - Secondary panels expand upward and dismiss in the expected order.
  - Existing top bar behavior remains unchanged.
