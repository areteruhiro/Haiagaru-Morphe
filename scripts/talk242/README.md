# Talk 242 regression checks

Run from the repository root:

```powershell
javac -d build/verification/talk242-tests extensions/chmate/src/main/java/app/morphe/extension/chmate/TalkPostCompatibility.java scripts/talk242/o/*.java scripts/talk242/VerifyTalk242.java
java -cp build/verification/talk242-tests VerifyTalk242
javac -cp C:/Users/narum/Downloads/morphe-desktop-1.13.0-all.jar -d build/verification/talk242-hook-tests scripts/VerifyTalk242Hooks.java
java -cp 'build/verification/talk242-hook-tests;C:/Users/narum/Downloads/morphe-desktop-1.13.0-all.jar' VerifyTalk242Hooks <patched-242-apk>
```

Fixtures verify scoped state normalization, cache invalidation on repeated calls,
unwrapped exceptions, timestamp conversion without mutating arguments, overflow,
ordinary reflection fallback, and the decoded 242 arithmetic for 100003 CPU values.
These checks do not prove server acceptance. Run the existing `scripts/talk243`
fixtures as a regression check too.
