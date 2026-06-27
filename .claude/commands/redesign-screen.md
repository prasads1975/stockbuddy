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

## Step 1 — Analyze the HTML Sample

Extract from the HTML:
- Layout structure (sections, cards, lists, grids, toolbars)
- Colors used → map to nearest token from `colors.xml`
- Typography (sizes, weights) → map to MD3 TextAppearance styles
- Icons → map to existing drawables listed in `CLAUDE.md`; only flag if none match
- Component patterns (buttons, inputs, chips, bottom sheets, FABs, etc.)
- Spacing (margins/padding) → use inline dp values (no dimens.xml)

---

## Step 1b — Study Existing Screens for Consistency

Before analyzing the HTML, read 2-3 already-finalized screens from the project to extract
the established layout conventions. Good reference screens to read:
- `res/layout/fragment_home.xml` — tile layout, spacing, toolbar style
- `res/layout/fragment_individual_linking.xml` — form layout, input fields, button placement
- `res/layout/fragment_results.xml` — list layout, status colors, empty state handling

From these, extract and carry forward:
- Toolbar height, title style, back button usage
- Card elevation, corner radius, stroke width
- Button placement (bottom of screen vs inline)
- Input field style (outlined vs filled `TextInputLayout`)
- RecyclerView item layout conventions
- Empty state layout pattern (icon + message + action)
- FAB usage and placement
- Section header style
- How status colors are applied (icon + text, never color alone — NFR-11)

**The HTML sample defines what to show. The existing screens define how it should look.**
When the two conflict, match the existing screen style and note the deviation to the user.

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

### `<ScreenName>ViewModel.kt` — only if changes are needed
- Manual constructor injection (no Hilt/Dagger)
- Expose `LiveData` or `StateFlow` for all UI state
- Business logic delegated to Repository layer — no direct DAO calls

### `res/values/strings.xml` — additions only
- Any new string used in the layout

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

// STRINGS: add to res/values/strings.xml
<new string entries only>
```

Then a short summary:
- Which FR/NFR requirements this screen satisfies
- Any MVP scope decisions (what was included, what was excluded and why)
- Any assumptions made
- Any deviations from the HTML sample (where existing screen style took precedence)
- Any open questions still remaining

---

## HTML Sample:

$ARGUMENTS