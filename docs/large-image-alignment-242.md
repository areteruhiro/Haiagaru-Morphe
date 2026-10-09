# ChMate 0.8.10.241 / 0.8.10.242 dev: large-image draft alignment

The original APK's `Lo/zzbfo;->c(Matrix,I,I,F)` builds a transform for both
full-resolution bitmaps and reduced-resolution draft bitmaps. The draft path in
`onDraw` passes `mScale * originalImageWidth / draftBitmap.getWidth()` as its
bitmap scale. That scale correctly enlarges the draft pixels, but the helper
also uses it to translate half of `mImageWidth` / `mImageHeight`, which are
original-content dimensions (swapped for rotation). Consequently the draft
centre is displaced when its resolution differs from the original content.

241 has the same arithmetic and draft caller in
`Lo/getCallingPackage;->d(Matrix,I,I,F)`, also with five locals. Both versions
use the same correction helper; only the native class/method selection differs.

The 241/242 patch corrects the final screen-space translation by
`rotatedContentSize * (mScale - bitmapScale) / 2` on each axis. The bitmap pixel
scale, rotation, scrolling margins and normal full-resolution drawing remain
unchanged. No bitmap allocation, reflection or new image download is needed.
The patch checks the target signature, local register layout and size fields
before adding the call; it does not alter 191/226.

`scripts/VerifyLargeImageAlignment.java` checks the draft centre algebra,
rotated dimensions, unchanged full-resolution path and invalid input guards.
Compile/run with the Android SDK's android.jar on the classpath.

Status: mathematical regression test, Android bundle build and patch application
to the original 242 APK passed. Verification APK:
`build/verification/image-rotation/chmate242-large-image-alignment.apk`.
242 was subsequently installed on XIG05 and the user confirmed that the reported
image's displacement was fixed. Source image: https://i.imgur.com/StehzZT.jpeg
(1257x16384). Original symptom: https://i.imgur.com/NPyIeoe.jpeg .
241's matching native calculation and preview caller have been inspected;
the bundle build and patch application to the original 241 APK passed.
Verification APK: `build/verification/image-rotation/chmate241-large-image-alignment.apk`.
241 still requires visual device verification.
