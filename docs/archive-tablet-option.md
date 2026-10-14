# Archive thread tablet navigation

Haiagaru settings now include `エッヂ過去ログのスレをタブレットモードを解除して開く`.
The preference `archiveThreadStandalone` defaults to true, preserving existing
navigation. On sends the archive-return marker, so OpenUrlActivity sets
`forwardTabletHome=false` and the thread opens as a child screen. Back can return
to archive search. Off omits the marker and leaves ChMate's native tablet setting
and forwarding behavior unchanged. It does not turn tablet mode on for users
whose native setting is off, or modify the global tablet preference.

Native TabletHomeActivity is singleTask, so native tablet forwarding may remove
the archive search activity from the back stack. The setting explains this.
The choice applies to both dedicated archive result links and archive-site
thread links handled in archive mode, not ordinary Hissi thread links.

Shared extension implementation covers 191 dev, 226 dev, 241 and 242 dev.
`VerifyArchiveTabletOption.java` tests the actual navigation method and preference
getter with isolated stubs. Actual tablet screen validation remains pending.
