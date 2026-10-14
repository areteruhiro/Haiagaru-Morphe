# Imgur album thumbnail duplication

Recognize `i.imgur.com/a/ID` as an album alias alongside `imgur.com/a/ID`
and `www.imgur.com/a/ID`, including supported incomplete URL forms.

After the album resolves successfully, remove the album-page URL from the
native attachment cache and retain the resolved image URLs in insertion order
without duplicates. Failed or pending albums and unrelated direct image URLs
are preserved. The response body is unchanged. Native thumbnail projections
are invalidated when attachments change. Multiple distinct album images remain
available; this is not a one-image-per-album restriction.

This is shared by 191 dev, 226 dev, 241 and 242 dev attachment hooks.

Tests: `VerifyImgurAlbum.java` validates URL forms and metadata extraction;
`VerifyImgurAttachmentDedup.java` exercises the actual append implementation
with isolated stubs, including repeated calls, multiple images, pending/empty
results, disabled fetching and projection invalidation. Actual device-screen
confirmation is pending.
