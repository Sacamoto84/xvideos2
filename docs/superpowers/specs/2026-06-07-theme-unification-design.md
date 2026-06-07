# Theme Unification — Design Spec

**Date:** 2026-06-07
**Status:** Approved (design), pending implementation plan

## Problem

The app has two custom theme objects with overlapping and inconsistent content:

- `ThemeRed` (`r/common/ThemeRed.kt`) — backgrounds, tab-level shades, accent colors, borders, Poppins/DM-sans fonts. Consumed by **R and X**.
- `ThemeL` (`l/theme/ThemeL.kt`) — grayscale palette, accents, `Type{}` text styles, `ExpandMenu`, fonts. Consumed by **L and X**.
- There is **no `ThemeX`**; the X feature borrows from both `ThemeRed` and `ThemeL`.
- `ui/theme/Theme.kt` is the Material `XvideosTheme` — unrelated, **out of scope**.

Result: background colors and fonts are duplicated, and the X feature has no home for its own tokens.

## Goal

Introduce a single umbrella `Theme` object that holds the **shared** tokens, with per-feature objects (`ThemeR`, `ThemeL`, `ThemeX`) for **unique** tokens, reachable as `Theme.R`, `Theme.L`, `Theme.X`.

Decided split (per user):

- **Shared (`Theme`):** background colors only.
- **Unique (`ThemeR`/`ThemeL`/`ThemeX`):** selection/accent colors, fonts, font sizes/text styles.

## Structure

New package `com.client.xvideos.common.theme`, one focused file per object:

```kotlin
// ThemeR.kt
object ThemeR { /* unique R */ }
// ThemeL.kt
object ThemeL { /* unique L */ }
// ThemeX.kt
object ThemeX { /* unique X */ }

// Theme.kt
object Theme {
    // --- shared: backgrounds only ---
    val background        = Color(0xFF212121)   // main screen background
    val backgroundAppRoot = Color(0xFF262626)   // App root only
    val tabLevel0         = Color(0xFF212121)   // bar/tab backgrounds (X/R/L)
    val tabLevel1         = Color(0xFF282828)
    val tabLevel2         = Color(0xFF333333)
    val tabLevel3         = Color(0xFF444444)

    // --- access to unique ---
    val R = ThemeR
    val L = ThemeL
    val X = ThemeX
}
```

Access pattern: `Theme.background` (shared), `Theme.R.colorYellow`, `Theme.L.Type.body`, `Theme.X...`.

Separate top-level objects (not nested) so each file stays small and focused; `Theme` only aggregates references plus shared background tokens.

## Token placement

### Theme (shared — backgrounds)
| Token | Value | Source today |
|---|---|---|
| `background` | `0xFF212121` | `ThemeRed.colorCommonBackground2`, `ThemeL.greyBackground` |
| `backgroundAppRoot` | `0xFF262626` | `ThemeL.grey6` (only at App root) |
| `tabLevel0` | `0xFF212121` | `ThemeRed.colorTabLevel0` |
| `tabLevel1` | `0xFF282828` | `ThemeRed.colorTabLevel1` |
| `tabLevel2` | `0xFF333333` | `ThemeRed.colorTabLevel2` |
| `tabLevel3` | `0xFF444444` | `ThemeRed.colorTabLevel3` |

> `background` and `tabLevel0` share the hex `0xFF212121` but stay distinct semantic tokens.

### ThemeR (unique R)
`colorCommonBackground` (0xFF303030, legacy component bg), `colorBottomBarDivider`, `colorYellow`, `colorBlue`, `colorRed`, `colorTextGray`, `colorBorderSelect` (= `Theme.tabLevel3`), `colorBorderGray`, `fontFamilyPopinsRegular/Medium/SemiBold/Bold/ExtraBold`, `fontFamilyDMsanss`.

### ThemeL (unique L)
`grayLevel`, `g0`, `r0`, `b0`, `grey0`–`grey7`, `lavender`, `primaryColor`, `red`, `secondaryColor`, `textColor`, `Type{}` (all text styles/sizes), `styleTextConfigL`, `ExpandMenu`, fonts (`fontFamilyPopins*`, `fontFamilyApp`, `fontFamilyDMsanss`, `fontFamilyKarla`).

### ThemeX (unique X — new)
Created minimal. X currently uses tab backgrounds (→ `Theme.tabLevel*`) and `ThemeL.ExpandMenu`. Seed `ThemeX` with X-specific literals currently hardcoded (e.g. X dashboard expand-menu container `0xFFF2EDF7`), grow later. X may keep referencing `Theme.L.ExpandMenu` where it genuinely reuses L's menu.

## Migration (per-member, big-bang)

Replacement is **per-member**, not a blind prefix swap: shared members map to `Theme.x`, unique members to `Theme.R.x` / `Theme.L.x`.

### Replacement map
| Old | New |
|---|---|
| `ThemeRed.colorCommonBackground2` | `Theme.background` |
| `ThemeRed.colorTabLevel0` | `Theme.tabLevel0` |
| `ThemeRed.colorTabLevel1` | `Theme.tabLevel1` |
| `ThemeRed.colorTabLevel2` | `Theme.tabLevel2` |
| `ThemeRed.colorTabLevel3` | `Theme.tabLevel3` |
| `ThemeRed.colorCommonBackground` | `Theme.R.colorCommonBackground` |
| `ThemeRed.<other>` | `Theme.R.<other>` |
| `ThemeL.greyBackground` | `Theme.background` |
| `ThemeL.<other>` | `Theme.L.<other>` |

### Ordering hazard
Replace `ThemeRed.colorCommonBackground2` **before** `ThemeRed.colorCommonBackground` (the latter is a prefix of the former). Same care for `colorTabLevel` variants — match exact member names.

### Edge cases
- **App root** ([ScreenRoot.kt](../../../app/src/main/java/com/client/xvideos/screenRoot/ScreenRoot.kt)): currently `ThemeL.grey6` → `Theme.backgroundAppRoot`.
- **Settings** (`SettingsListItems.kt`): `SettingsScreenBackground = ThemeL.grey6`, `SettingsTopBarColor = ThemeL.grey5` → become `Theme.L.grey6` / `Theme.L.grey5`. Semantics unchanged (do not redirect to shared tokens).
- **Imports:** drop `import …ThemeRed` / `import …ThemeL`; add `import com.client.xvideos.common.theme.Theme`. Add `ThemeR`/`ThemeL`/`ThemeX` imports only if referenced directly (we standardize on `Theme.*`).
- After migration delete old `r/common/ThemeRed.kt` and `l/theme/ThemeL.kt`.

### Scope
~195 `ThemeRed.` usages (53 files) + ~294 `ThemeL.` usages (62 files). Mechanical but wide.

## Testing / verification

- `./gradlew :app:compileDebugKotlin` (or assembleDebug) must pass — primary gate; per-member rename errors surface as unresolved references.
- Spot-check screens for unchanged appearance: X dashboards, R explorer/profile/gifs, L album/explorer, Settings, App root.
- Grep guard: no remaining `ThemeRed.` / `ThemeL.` references; old theme files deleted.

## Risks

- **Per-member mapping mistakes** — a shared member sent to `Theme.R.` (or vice versa) compiles only if the member exists there; build catches most, visual check catches the rest.
- **Prefix collisions** (`colorCommonBackground` vs `…2`) — mitigated by ordering + exact-name matching.
- **Wide diff** — one large commit; review by build + screen spot-check.

## Out of scope

- `ui/theme/Theme.kt` Material `XvideosTheme` (name coexists in a different package; no conflict).
- Consolidating duplicated fonts into shared (user chose to keep fonts per-feature).
- Touching specialized backgrounds (niche `0x0F0F0F`, fullscreen viewers, dialogs).
