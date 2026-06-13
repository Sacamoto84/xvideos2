# Unsorted hardcoded colors — audit (for Figma `Unsorted/Misc`)

**Date:** 2026-06-13
**Source:** scan of `Color(0x…)` literals in `app/src/main/java/**/*.kt`.

## Summary

- Distinct color literals total: **149**
- Covered by token collections (`Theme.*` / `Legacy/PornHub` / `Material`): **53**
- **Uncovered (→ `Unsorted/Misc`): 96** — listed below.

These 96 are hardcoded one-off colors not in the theme token system. In the Figma
file they become collection **`Unsorted/Misc`**, variables named `misc/#HEX`, each
with `description = <file>:<line>`. (Creation was started but blocked by the Figma
MCP Starter tool-call limit — resume when limits reset / plan upgraded.)

## Uncovered colors (hex → first occurrence)

| # | Hex | First occurrence |
|---|---|---|
| 1 | #101014 | AppLockActivity.kt:183 |
| 2 | #FFE800 | AppLockActivity.kt:202 |
| 3 | #FF7A7A | AppLockActivity.kt:241 |
| 4 | #090909 | common/collectionDB/ui/DaialogNewCollection.kt:74 |
| 5 | #363636 | common/collectionDB/ui/DaialogNewCollection.kt:89 |
| 6 | #232323 | common/collectionDB/ui/DaialogNewCollection.kt:95 |
| 7 | #E4E4E4 | common/settings/ui/DialogButton.kt:57 |
| 8 | #474747 | common/settings/ui/DialogButton.kt:71 |
| 9 | #CCCCCC | common/settings/ui/DialogButton.kt:79 |
| 10 | #01B671 | common/snackbar/SnackBarEvent.kt:140 |
| 11 | #E5553A | common/snackbar/SnackBarEvent.kt:141 |
| 12 | #4276FE | common/snackbar/SnackBarEvent.kt:142 |
| 13 | #FF8E0C | common/snackbar/SnackBarEvent.kt:143 |
| 14 | #43C558 | common/snackbar/SnackBarEvent.kt:189 |
| 15 | #FD6969 | common/snackbar/SnackBarEvent.kt:190 |
| 16 | #BDDBFD | common/snackbar/SnackBarEvent.kt:191 |
| 17 | #22C55C | common/snackbar/SnackBarEvent.kt:213 |
| 18 | #141414 | common/theme/Theme.kt:63 (commented grayLevel) |
| 19 | #353535 | common/theme/Theme.kt:66 (commented grayLevel) |
| 20 | #414141 | common/theme/Theme.kt:69 (commented grayLevel) |
| 21 | #C5C8C6 | common/theme/Theme.kt:71 (commented grayLevel) |
| 22 | #45687A | common/theme/Theme.kt:72 (commented grayLevel) |
| 23 | #202020 | common/urlVideoImage/UrlVideoLite.kt:141 |
| 24 | #B8B5B5 | common/util/shimmerEffect.kt:36 |
| 25 | #8F8B8B | common/util/shimmerEffect.kt:37 |
| 26 | #222222 | common/videoplayer/model/constant.kt:7 |
| 27 | #FED766 | common/videoplayer/model/constant.kt:8 |
| 28 | #1B1B1B | HapticDemoScreen.kt:88 |
| 29 | #B0B0B0 | HapticDemoScreen.kt:111 |
| 30 | #2A2A2A | HapticDemoScreen.kt:117 |
| 31 | #4A3B00 | HapticDemoScreen.kt:145 |
| 32 | #3A3A3A | HapticDemoScreen.kt:166 |
| 33 | #BFBFBF | HapticDemoScreen.kt:182 |
| 34 | #4A4A4A | l/.../saved/collection/CollectionsGrid.kt:164 |
| 35 | #938F99 | l/.../saved/likes/L_ScreenSavedLikesTab.kt:110 |
| 36 | #484848 | l/ui/screens/L_ScreenLogin.kt:144 |
| 37 | #B8B7B7 | l/ui/screens/L_ScreenLogin.kt:146 |
| 38 | #888888 | l/ui/screens/L_ScreenLogin.kt:148 |
| 39 | #6552A5 | l/.../screenAlbum/atom/AlbumDialogDeleteAlbum.kt:46 |
| 40 | #EBE6EE | l/.../screenAlbum/atom/AlbumDialogDeleteAlbum.kt:62 |
| 41 | #FFC857 | l/ui/screens/screenAlbum/ScreenAlbum.kt:365 |
| 42 | #3E3E3E | l/.../screenAlbumList/atom/AlbumListPageSelector.kt:138 |
| 43 | #373737 | l/.../screenAlbumList/atom/AlbumListPageSelector.kt:139 |
| 44 | #2D2D2D | l/.../screenAlbumList/atom/AlbumListPageSelector.kt:146 |
| 45 | #434343 | l/.../screenAlbumList/bottomBar/AlbumListBottomBar.kt:54 |
| 46 | #171717 | l/.../screenAlbumList/molecule/filter/atom/StyleGenresTags.kt:16 |
| 47 | #242424 | StyleGenresTags.kt:17 |
| 48 | #606060 | StyleGenresTags.kt:21 |
| 49 | #F4F4F4 | StyleGenresTags.kt:22 |
| 50 | #D0D0D0 | StyleGenresTags.kt:23 |
| 51 | #FF7468 | StyleGenresTags.kt:24 |
| 52 | #2D4D2F | StyleGenresTags.kt:26 |
| 53 | #85CE6D | StyleGenresTags.kt:27 |
| 54 | #F0FFE9 | StyleGenresTags.kt:28 |
| 55 | #63373A | StyleGenresTags.kt:29 |
| 56 | #FF8277 | StyleGenresTags.kt:30 |
| 57 | #FFE1DD | StyleGenresTags.kt:31 |
| 58 | #1D1D1D | StyleGenresTags.kt:32 |
| 59 | #0C94FF | l/ui/screens/screenAlbumList/ScreenAlbumList.kt:174 |
| 60 | #A3A3A3 | ScreenAlbumList.kt:214 |
| 61 | #B3B3B3 | ScreenAlbumList.kt:215 |
| 62 | #181818 | l/.../screenFullScreen/L_FullScreenImage.kt:249 |
| 63 | #8AB4F8 | L_FullScreenImage.kt:718 |
| 64 | #2C2C2C | MainActivity.kt:294 |
| 65 | #565656 | MainActivity.kt:420 |
| 66 | #F1EDF4 | r/common/expand_menu_video/ExpandMenuVideo.kt:132 |
| 67 | #48454E | r/common/expand_menu_video/ExpandMenuVideoTags.kt:39 |
| 68 | #757575 | r/common/search/CustomBasicTextFieldContent.kt:249 |
| 69 | #CDECFB | r/common/video/CanvasTimeDurationLine.kt:64 |
| 70 | #137CBD | CanvasTimeDurationLine.kt:65 |
| 71 | #73CDF1 | CanvasTimeDurationLine.kt:66 |
| 72 | #909090 | CanvasTimeDurationLine.kt:150 |
| 73 | #E73538 | CanvasTimeDurationLine.kt:186 |
| 74 | #8BC34A | CanvasTimeDurationLine.kt:204 |
| 75 | #9E9DA9 | r/.../saved/tab/R_Screen_CreatorsTab.kt:243 |
| 76 | #AAAAAA | R_Screen_CreatorsTab.kt:282 |
| 77 | #0F0F0F | r/ui/niche/ScreenNiche.kt:189 |
| 78 | #505050 | r/ui/niche/ScreenNiche.kt:197 |
| 79 | #3D3C53 | r/ui/profile/atom/RedProfileUserImage.kt:151 |
| 80 | #923117 | r/ui/top_this_week/state/ErrorState.kt:25 |
| 81 | #0F9960 | screenRoot/RootSnackbarHost.kt:43 |
| 82 | #D13913 | screenRoot/RootSnackbarHost.kt:44 |
| 83 | #FFFBFE | ui/theme/Theme.kt:30 (Material light scheme) |
| 84 | #1C1B1F | ui/theme/Theme.kt:35 (Material light scheme) |
| 85 | #3D3F4A | x/.../bottomKeyboard/KeyboardNumber.kt:45 |
| 86 | #DEE1EF | KeyboardNumber.kt:49 |
| 87 | #5A5D6C | KeyboardNumber.kt:52 |
| 88 | #FF7043 | KeyboardNumber.kt:193 |
| 89 | #FF9000 | x/.../bottomKeyboard/MenuDot.kt:48 |
| 90 | #23242A | MenuDot.kt:97 |
| 91 | #FF6B6B | x/screens/favorites/FavoritesDeleteDialog.kt:44 |
| 92 | #2E2E2E | FavoritesDeleteDialog.kt:52 |
| 93 | #787878 | x/screens/tags/ScreenTags.kt:34 |
| 94 | #DE2600 | x/screens/videoplayer/atom/ComposeTags.kt:39 |
| 95 | #D9D9D9 | ComposeTags.kt:54 |
| 96 | #040404 | x/screens/videoplayer/ScreenX_LocalVideoPlayer.kt:43 |

## Notes

- #18–22 are inside a **commented-out** `grayLevel` block in `Theme.kt` — dead, keep
  out of `Unsorted/Misc` if filtering to live UI only.
- #83–84 belong to the Material light `XvideosTheme` scheme (rarely active).
- Several share semantics with existing tokens (e.g. greys near `tabLevel*`) but are
  literal duplicates — captured separately to stay faithful to "as-is".
