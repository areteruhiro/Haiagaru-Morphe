# Haiagaru changelog

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
