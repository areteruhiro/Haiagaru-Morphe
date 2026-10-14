# X query-format image attachments

Public links such as
`https://pbs.twimg.com/media/HUL_2IXaEAAne0T?format=png&name=900x900`
are recognized by the shared native attachment completion hooks (191 dev,
226 dev, 241, 242 dev). `TwimgImageUrls` supplies an extension-bearing HTTPS
URL while retaining query parameters, including the requested image size.
PNG, JPG, JPEG, GIF and WebP are supported. Supported incomplete HTTP URL forms
are repaired only for the exact `pbs.twimg.com` host and `/media/` path.

This uses the existing image auto-fetch switch. The response text remains
unchanged; no post metadata request or login is required for direct images.
Native attachment URLs are normalized and deduplicated with appended URLs;
cached thumbnail projections are invalidated when they change. X metadata
thumbnail URLs also receive the same normalization.

Validation: `VerifyTwimgImageUrls.java` covers formats, requested sizes,
incomplete URLs, duplicates and invalid hosts/queries. The actual append logic
is covered by `VerifyImgurAttachmentDedup.java`. An HTTP HEAD request for the
reported example's extension-bearing URL returned 200 and image/png.
Actual device display has not yet been confirmed.
