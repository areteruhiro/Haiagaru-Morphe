# 191: 5ch cookie confirmation fails with a multiline message

Verified on XIG05, ChMate 0.8.10.191 dev, 2026-10-09.

Frida showed zero cookies on the first request. The server returned a normal
cookie confirmation with `_X:cookie`, `feature` and the acceptance submit input.
The native `o.getCredentials.c(String)` parser returned FROM/mail/bbs/time/key/
feature/submit but dropped MESSAGE. Both static regex patterns had flags=0.
The input and attribute-value regexes use `.` without DOTALL, so a quoted
multiline MESSAGE is skipped. The native post code then takes its failure path
instead of the confirmation/merged-form retry path.

The existing runtime repair enabled DOTALL only from
`prepareLegacyTalkPostSession()`. Posting to 5ch without using Talk first never
activated it. This is not evidence of a general CookieJar failure.

With Frida enabling DOTALL on both native patterns, MESSAGE was restored and
the existing confirmation detector returned true. The user-authorized pending
post succeeded (response 371 in the currently open Haiagaru Part1 thread).
The cookie jar then saved one cookie and sent one cookie on the next request.
No cookie values, credentials or message body were included in trace output.

The permanent 191 patch replaces the two Pattern.compile(String) calls in the
shared parser's static initializer with LegacyPostFormPatterns.compile(String),
which adds DOTALL. It therefore applies before any board uses this shared
parser, including external boards; there is no destination/domain check.
Board-specific clients that do not use this parser require separate validation.
This change does not change consent handling, clear cookies or retry extra
posts. The existing Talk compatibility repair is retained.

Regression test: scripts/VerifyLegacyPostFormPatterns.java. The test reproduces
the old omission and checks LF/CRLF messages, hidden confirmation fields,
single-line values, external-destination fixtures and unchanged unrelated input syntax.

Android bundle build and application to the original 191 APK passed.
The persistent-fix APK is `build/verification/image-rotation/chmate191-cookie-form-fix.apk`;
the device success above was with Frida, not with this APK installed.
