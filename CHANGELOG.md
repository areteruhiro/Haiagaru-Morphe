# Haiagaru changelog

## 1.7.0 (Official release)

- Consolidate the improvements since 1.5.2: Hissi/Kyodemo viewers, copy actions, themes, gestures and Wacchoi search.
- Add MEGA/local backup and foreground startup synchronization, Edge archive search, toolbar filters and compatibility fixes.
- Preserve Wacchoi labels, case and symbols; improve explicit analysis requests without automatic request loops.
- Repair legacy BE link spans on 191/226 and add optional external TXT body replacement on 191/226/241/242.
- Publish the Android MPP and align stable/prerelease bundle sources to 1.7.0.
- See [Japanese release notes](release-notes-1.7.0.md) for the complete user-facing changes, limitations and acknowledgments.

## 1.6.8 (Pre-release)

- Prevent startup crashes from incompatible package names by requiring ChMate's original package name as the prefix.
- Keep the current thread-list screen alive after marking all unread counts as read instead of recreating the activity.

## 1.6.7 (Pre-release)

- Fix ChMate 0.8.10.242 dev crashing when the filter row is displayed. Do not measure a ComposeView before it is attached to a window; request layout from the parent instead.
- Includes the Haiagaru 1.6.6 pre-release changes.

## 1.6.6 (Pre-release)

- Make Hissi checker viewer toolbar buttons individually configurable.
- Repair hidden quick-filter row measurements on ChMate 191, 226, 241, and 242.
- Apply the configured horizontal swipe navigation to the Edge archive viewer.
- Preserve Edge `L20`-style prefixes in Wacchoi searches.
- Normalize legacy BE icon URLs and avoid duplicate icons in old posts.
- Add read-thread sorting scope choices and refine short-list alignment.
- Add “mark all unread as read” to the board thread-list toolbar.
