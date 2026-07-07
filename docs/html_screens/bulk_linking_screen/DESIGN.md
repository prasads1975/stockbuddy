---
name: Rugged Utility
colors:
  surface: '#faf9fc'
  surface-dim: '#dad9dc'
  surface-bright: '#faf9fc'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f4f3f6'
  surface-container: '#eeedf0'
  surface-container-high: '#e9e8ea'
  surface-container-highest: '#e3e2e5'
  on-surface: '#1a1c1e'
  on-surface-variant: '#43474d'
  inverse-surface: '#2f3033'
  inverse-on-surface: '#f1f0f3'
  outline: '#73777e'
  outline-variant: '#c3c7ce'
  surface-tint: '#446180'
  primary: '#002440'
  on-primary: '#ffffff'
  primary-container: '#1b3a57'
  on-primary-container: '#87a4c6'
  inverse-primary: '#acc9ed'
  secondary: '#496177'
  on-secondary: '#ffffff'
  secondary-container: '#c9e2fd'
  on-secondary-container: '#4d657b'
  tertiary: '#321f00'
  on-tertiary: '#ffffff'
  tertiary-container: '#4d3304'
  on-tertiary-container: '#c19b63'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#d0e4ff'
  primary-fixed-dim: '#acc9ed'
  on-primary-fixed: '#001d35'
  on-primary-fixed-variant: '#2b4967'
  secondary-fixed: '#cce5ff'
  secondary-fixed-dim: '#b0c9e3'
  on-secondary-fixed: '#011d31'
  on-secondary-fixed-variant: '#31495e'
  tertiary-fixed: '#ffddb0'
  tertiary-fixed-dim: '#e9c085'
  on-tertiary-fixed: '#291800'
  on-tertiary-fixed-variant: '#5e4112'
  background: '#faf9fc'
  on-background: '#1a1c1e'
  surface-variant: '#e3e2e5'
  status-available: '#2E7D32'
  status-missing: '#C62828'
  status-excess: '#F9A825'
  surface-industrial: '#F8F9FA'
  border-subtle: '#E0E2E5'
typography:
  display-count:
    fontFamily: Inter
    fontSize: 48px
    fontWeight: '700'
    lineHeight: 56px
    letterSpacing: -1px
  headline-page:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
  title-card:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '600'
    lineHeight: 24px
  body-data:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-metadata:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  label-cta:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
    letterSpacing: 0.5px
  label-caption:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
rounded:
  sm: 0.125rem
  DEFAULT: 0.25rem
  md: 0.375rem
  lg: 0.5rem
  xl: 0.75rem
  full: 9999px
spacing:
  touch-target-min: 48dp
  action-height-primary: 56dp
  gutter: 16px
  margin-mobile: 16px
  stack-gap: 8px
  thumb-zone-bottom: 40%
---

## Brand & Style

The design system is engineered for **Industrial Reliability** and **Ergonomic Pragmatism**. It is specifically optimized for high-pressure retail and warehouse environments where speed, accuracy, and physical constraints (one-handed operation) are paramount. The aesthetic follows a **Corporate/Modern** style built on Material Design 3 (MD3) principles but hardened for utility.

The UI must evoke a sense of professional competence. It avoids decorative flourishes in favor of high-contrast visibility, technical clarity, and "live" data responsiveness. The system is designed to perform under harsh showroom lighting on 6-inch rugged handheld devices, ensuring that hardware status and inventory counts are legible at a glance.

**Key Design Principles:**
- **Utility-First:** Every element must serve a functional purpose; information density is managed to prevent cognitive load.
- **Rugged Ergonomics:** Layouts are dictated by the "Thumb-zone," placing high-frequency actions within easy reach.
- **Graceful Hardware Awareness:** The UI dynamically adjusts based on hardware availability (RFID/Barcode), providing clear feedback when sensors are offline.

## Colors

The color palette is anchored by a professional **Slate Blue** primary color, chosen for its authoritative and stable feel. The system utilizes a light-mode default to ensure maximum legibility against the "clean whites/grays" of retail showroom environments.

**Functional Color Logic:**
- **Primary:** Used for the most critical actions (START, STOP, SAVE) and brand identity.
- **Status Semantic:** A strict adherence to Green (Available), Red (Missing), and Yellow (Excess) is used for inventory KPIs, item status chips, and connection indicators.
- **Surface:** A "Surface Industrial" gray is used for background layers to reduce glare while maintaining contrast with white card elements.
- **Accessibility:** Color is never the sole indicator of status; it is always accompanied by icons or text labels to ensure reliability in variable lighting conditions.

## Typography

This design system uses **Inter** for its exceptional legibility and neutral, technical tone. The type scale is optimized for fast reading on small, handheld screens.

**Scaling & Hierarchy:**
- **Display Count:** Used for real-time unique-tag counts during active scans; oversized for visibility from a distance.
- **Body Data:** Used for primary input fields (Barcode, RFID Tag ID) to ensure data entry accuracy.
- **Body Metadata:** Used for secondary article IDs and domain-specific fields (e.g., "Carat/Purity").
- **Label CTA:** Specifically for buttons, set in semi-bold to withstand high-density backgrounds.

## Layout & Spacing

The layout utilizes a **fluid grid** model tailored for a 6-inch portrait device. It prioritizes the "one primary action per screen" rule to minimize user error during high-intensity tasks.

**Ergonomic Rules:**
- **Thumb-Zone:** All high-frequency interactive elements (Scanners, Save buttons) are anchored within the bottom 40% of the screen.
- **Touch Targets:** A strict minimum of 48dp for secondary interactions; primary workflow buttons are elevated to 56dp.
- **Padding:** A consistent 16px outer margin ensures content does not bleed into the physical bezel of ruggedized device cases.
- **Vertical Rhythm:** Elements are stacked using an 8px base unit to maintain a compact but readable density.

## Elevation & Depth

Visual hierarchy is established through **Tonal Layering** rather than heavy shadows, ensuring clarity in high-glare environments.

- **Level 0 (Base):** The industrial surface background.
- **Level 1 (Cards):** Inventory items and result groups use flat white containers with a subtle 1px border (`border-subtle`) to define boundaries without adding visual "fuzziness."
- **Level 2 (Modals/Sheets):** Bottom sheets and secondary confirmation dialogs use a standard MD3 scrim to focus the user's attention.
- **Shadows:** Minimal, low-opacity ambient shadows are reserved exclusively for floating action buttons or primary CTAs to indicate "tap-ability" above the flat grid.

## Shapes

The design system uses **Soft (0.25rem)** roundedness. This "Semi-Square" approach reinforces the industrial, rugged personality of the application.

- **Buttons:** Use 4px (Soft) corners to maximize the visual area for the touch target.
- **Input Fields:** Use 4px rounded top corners with a bottom indicator line, adhering to MD3 filled-field standards.
- **Status Chips:** Use a full pill shape (rounded-xl) to distinguish them from interactive buttons or data cards.

## Components

**Buttons**
- **Primary Action:** 56dp height, full-width, Slate Blue background with White text. Positioned at the bottom of the screen.
- **UHF/Scan Button:** Distinctive styling, often accompanied by a "broadcast" icon to indicate hardware activation.

**Item Cards**
- Grouped by Barcode with count chips in the top right.
- Status is indicated by a colored vertical stripe on the left edge (Green/Red/Yellow).

**Input Fields**
- Mandatory fields must feature a trailing red asterisk (`*`).
- Barcode inputs include a compact trailing icon button for camera-scan activation.

**Status Chips**
- High-contrast background with dark/white text (whichever provides >7:1 contrast).
- Includes an icon (e.g., Check, Alert, Warning) to supplement the color-coded status.

**Hardware Status Bar**
- A persistent, slim bar at the top or bottom of the screen showing "Connected" (Green), "Not Connected" (Red), or "Emulator Mode" (Yellow).

**Lists**
- Use card-based layouts with clear dividers. Each list item must have a minimum height of 64dp to ensure easy scrolling and selection with a thumb.