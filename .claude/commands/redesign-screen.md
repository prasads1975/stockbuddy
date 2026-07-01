# Redesign Screen — StockBuddy Android

Redesign or generate an Android screen for StockBuddy based on an HTML sample.
The project context, stack, constraints, and all resource tokens are in `CLAUDE.md` — read it first.

---

## Inputs

The user will provide the command in this format:

```
/redesign-screen <screen description> | <html-sample-path>
```

**Examples:**
```
/redesign-screen home screen with 4 tiles | docs/html-screens/home.html
/redesign-screen individual item linking form | docs/html-screens/linking.html
/redesign-screen inventory scanning screen | docs/html-screens/scanning.html
```

From the screen description, Claude should:
1. Search the project structure (`ui/` folder) to find the matching Fragment/Activity
2. Confirm the match to the user before proceeding — e.g. "I found `HomeFragment` + `fragment_home.xml` — is this the screen you mean?"
3. Wait for user confirmation before reading files or generating anything
4. If multiple matches are found, list them and ask the user to pick one
5. If no match is found, tell the user and ask for clarification

From the html-sample-path, Claude should:
- Read the file at `docs/html-screens/<filename>.html`
- If the path is missing, ask: "Which HTML sample should this screen match? (files available in `docs/html-screens/`)"

Do not proceed to analysis or code generation until both the screen and HTML sample are confirmed.

---

## Step 0 — Deep Read of Existing Code

Before analyzing the HTML, fully understand the existing screen by reading:

**Layout file** (`res/layout/fragment_<screen_name>.xml`):
- Every view, its ID, type, and current attributes
- Current hierarchy and grouping of elements
- Existing color, style, and drawable references

**Fragment/Activity** (`<ScreenName>Fragment.kt`):
- All LiveData/StateFlow observers and what UI they update
- All click listeners and what they trigger
- Navigation calls and their destinations
- Any dialog/bottom sheet already created and its trigger
- Any adapter or RecyclerView setup

**ViewModel** (`<ScreenName>ViewModel.kt`):
- Every exposed LiveData/StateFlow and what it represents
- Every public function and what it does
- Repository calls being made

**Only after fully reading all three files**, proceed to Step 1.
The goal is to understand the complete current behaviour so that:
- No existing working logic is accidentally removed
- New UI elements are wired correctly to existing ViewModel functions
- Dialogs are triggered from the right place in the Fragment
- State that already exists in the ViewModel is reused, not duplicated

---

## Step 1 — Analyze the HTML Sample (Visual Fidelity First)

Your primary goal is a **pixel-close visual match** to the HTML sample.
Extract every visual detail precisely — do not approximate or substitute unless
a resource is genuinely unavailable:

**Layout & Structure:**
- Identify every distinct section/region (header, body, footer, overlays)
- Note exact element order top-to-bottom, left-to-right
- Map CSS layout to Android equivalents:
    - `flexbox column` → `LinearLayout vertical` or `ConstraintLayout vertical chain`
    - `flexbox row` → `LinearLayout horizontal` or `ConstraintLayout horizontal chain`
    - `grid` → `GridLayout` or `RecyclerView` with `GridLayoutManager`
    - `position: fixed/sticky` → separate toolbar or persistent bar outside scroll

**Spacing & Sizing (extract exact values):**
- Read every `padding`, `margin`, `gap`, `width`, `height` value from CSS
- Convert px → dp (divide by screen density; assume 1px = 1dp if density unknown)
- Note `border-radius` → `app:cornerRadius`
- Note `box-shadow` → `app:elevation` on MaterialCardView
- Note `border` → `app:strokeWidth` + `app:strokeColor`

**Colors (extract exact values):**
- Read every `background-color`, `color`, `border-color` from CSS
- Map to the nearest `@color/` token from `colors.xml`
- If no token matches within reasonable tolerance, flag it and ask the user
- Never silently substitute a different color

**Typography (extract exact values):**
- Read `font-size`, `font-weight`, `line-height`, `letter-spacing`
- Map to the nearest MD3 `?attr/textAppearance*` style
- Note text alignment (`text-align: center` → `android:gravity="center"`)

**Icons:**
- Identify every icon in the HTML (class name, SVG content, or image src)
- Map to the nearest drawable from the inventory in `CLAUDE.md`
- Note icon size from CSS (`width`/`height`) → `android:layout_width/height`
- If no drawable matches, flag it — do not substitute silently

**Component Patterns:**
- Identify every interactive element (buttons, inputs, toggles, chips, tabs)
- Note button style (filled, outlined, text) → map to MD3 button style
- Note input style (outlined, filled) → `TextInputLayout` style attribute
- Note any bottom sheets, dialogs, FABs, snackbars

**Modal Dialogs & Overlays (treat as first-class screen elements):**
- Identify every modal, dialog, bottom sheet, or overlay in the HTML
- For each one, extract:
    - Trigger condition (which button/action opens it)
    - Content (title, message, input fields, buttons)
    - Dismiss behaviour (cancel button, back press, outside tap)
    - Confirmation/destructive action and what it does
- Each dialog becomes its own output:
    - Simple alert/confirm → `MaterialAlertDialogBuilder` inline in Fragment
    - Complex content (form fields, lists) → `DialogFragment` with its own layout XML
    - Sliding panels → `BottomSheetDialogFragment` with its own layout XML

---

## Step 1b — Study Existing Screens for Consistency

Read 2-3 already-finalized screens from the project to extract established conventions:
- `res/layout/fragment_home.xml` — tile layout, spacing, toolbar style
- `res/layout/fragment_individual_linking.xml` — form layout, input fields, button placement
- `res/layout/fragment_results.xml` — list layout, status colors, empty state handling

Extract and carry forward:
- Toolbar height, title style, back button usage
- Card elevation, corner radius, stroke width
- Button placement (bottom of screen vs inline)
- Input field style (outlined vs filled `TextInputLayout`)
- RecyclerView item layout conventions
- Empty state layout pattern (icon + message + action)
- FAB usage and placement
- Section header style
- How status colors are applied (icon + text, never color alone — NFR-11)

**The HTML sample defines what to show and how it looks.**
**The existing screens define shared chrome conventions only (toolbar, nav, status bar).**
Only override the HTML design where it conflicts with shared chrome — and always tell the user.

---

## Step 2 — Cross-check with SRS and MVP

Before writing any code:
1. Read `docs/srs/RFID_App_SRS_C72.md` — confirm which FR/NFR this screen satisfies
2. Read `docs/srs/StockBuddy_MVP_Demo_Scope.md` — confirm every element is in MVP scope
3. Flag any HTML element that has **no matching FR** — ask the user before including it
4. Flag any HTML element that is **explicitly out of MVP scope** — exclude it and tell the user

---

## Step 3 — Ask Before Proceeding if Any of These Are Unclear

**Stop and ask the user** if:
- A new ViewModel is needed vs binding to an existing one
- Navigation destination(s) from this screen are not obvious from the HTML
- The HTML shows data that doesn't map clearly to an existing entity or DAO
- An icon in the HTML has no match in the drawable inventory
- A color in the HTML has no close match in `colors.xml`
- A dialog's trigger condition or dismiss behaviour is ambiguous
- A dialog contains a form — clarify whether it needs its own ViewModel function or reuses an existing one

Do not guess on any of the above — one wrong assumption cascades into multiple files.

---

## Step 4 — Generate the Screen

Read the existing layout and Fragment/Activity files first, then modify in place.
Produce updated versions of:

### `res/layout/fragment_<screen_name>.xml` (or `activity_`)
- Root: `ConstraintLayout` (preferred) or `LinearLayout` for simple vertical stacks
- Widgets: MaterialComponents only (`MaterialButton`, `TextInputLayout`, `MaterialCardView`, `RecyclerView`, etc.)
- Colors: `@color/` tokens only — no hardcoded hex
- Text styles: `?attr/textAppearance*` MD3 attributes or existing styles from `themes.xml`
- Icons: `@drawable/ic_*` from the existing inventory
- Spacing: inline dp values (8dp / 16dp / 24dp convention)
- Strings: `@string/` references only — add new entries to `strings.xml` if needed

### `<ScreenName>Fragment.kt` (or `Activity`)
- Kotlin + ViewBinding (never `findViewById`)
- `viewModelScope` + `LiveData`/`StateFlow` observation
- Navigation via Safe Args typed Directions/Args — never raw `Bundle`
- Follow existing Fragment patterns in the project
- Only modify sections affected by the redesign — do not refactor unrelated logic
- Wire every dialog trigger to the correct click listener
- Pass correct data into dialogs from ViewModel state — never hardcode

### `<ScreenName>ViewModel.kt` — only if changes are needed
- Manual constructor injection (no Hilt/Dagger)
- Expose `LiveData` or `StateFlow` for all UI state
- Business logic delegated to Repository layer — no direct DAO calls

### For each Modal Dialog identified in Step 1:

**Simple alert/confirm dialog** — inline in Fragment using `MaterialAlertDialogBuilder`:
```kotlin
// Example pattern
MaterialAlertDialogBuilder(requireContext())
    .setTitle(getString(R.string.dialog_title))
    .setMessage(getString(R.string.dialog_message))
    .setNegativeButton(getString(R.string.cancel)) { dialog, _ -> dialog.dismiss() }
    .setPositiveButton(getString(R.string.confirm)) { _, _ -> viewModel.onConfirm() }
    .show()
```

**Complex dialog** (form fields, lists) → `res/layout/dialog_<name>.xml` + `<Name>DialogFragment.kt`:
- Layout follows same rules as main layout (MaterialComponents, `@color/`, `@string/`)
- DialogFragment communicates back to parent via shared ViewModel or listener interface

**Bottom sheet** → `res/layout/bottom_sheet_<name>.xml` + `<Name>BottomSheetDialogFragment.kt`:
- Use `BottomSheetDialogFragment` base class
- Layout follows same rules as main layout

### `res/values/strings.xml` — additions only
- Any new string used in the layout or dialogs

---

## Step 4b — Self-Check Before Outputting

Before writing the final output, do a section-by-section comparison:

For each region of the HTML screen, verify:
- [ ] Every section/card present in HTML exists in the layout XML
- [ ] Every modal/dialog in HTML has a corresponding implementation
- [ ] Every dialog trigger is wired in the Fragment
- [ ] Element order matches top-to-bottom
- [ ] Corner radius, elevation, stroke match the HTML
- [ ] Colors match (or closest token is used and noted)
- [ ] Icon sizes match
- [ ] Button styles match (filled/outlined/text)
- [ ] Input styles match (outlined/filled)
- [ ] Spacing/padding values are close to the HTML
- [ ] Typography sizes/weights are mapped correctly
- [ ] No existing working logic has been removed

If any item fails the check, fix it before outputting.
List any items that could not be matched and explain why in the Step 5 summary.

---

## Step 5 — Output Format

For each file output:

```
// FILE: res/layout/fragment_<screen_name>.xml
<full file content>

// FILE: ui/<package>/<ScreenName>Fragment.kt
<full file content>

// FILE: ui/<package>/<ScreenName>ViewModel.kt  ← only if changed
<full file content>

// FILE: res/layout/dialog_<name>.xml  ← one block per complex dialog
<full file content>

// FILE: ui/<package>/<Name>DialogFragment.kt  ← one block per complex dialog
<full file content>

// FILE: res/layout/bottom_sheet_<name>.xml  ← one block per bottom sheet
<full file content>

// FILE: ui/<package>/<Name>BottomSheetDialogFragment.kt  ← one block per bottom sheet
<full file content>

// STRINGS: add to res/values/strings.xml
<new string entries only>
```

Then a short summary:
- Which FR/NFR requirements this screen satisfies
- Any MVP scope decisions (what was included, what was excluded and why)
- Any assumptions made
- Any deviations from the HTML sample (where existing screen style took precedence)
- Any visual elements that could not be matched exactly and why
- List of all dialogs/bottom sheets generated and their trigger conditions
- Any open questions still remaining

---

## HTML Sample:

$ARGUMENTS
