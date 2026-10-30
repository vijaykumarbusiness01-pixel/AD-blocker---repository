UNIVERSAL AD BLOCKER PRO — ANDROID MVP

APK build without installing Android Studio:
1. Create/sign in to a GitHub account.
2. Create a NEW repository. Public or private is okay.
3. Upload all files and folders from this extracted project into the repository root. IMPORTANT: include the hidden .github folder and its workflows/android.yml file.
4. Open the repository's Actions tab.
5. If GitHub asks to enable Actions, enable it.
6. Select "Build Android APK" on the left.
7. Press "Run workflow" > "Run workflow".
8. Wait for the run to finish with a green check.
9. Open that completed run. At the bottom, find Artifacts.
10. Download "UniversalAdBlockerPRO-debug-apk" (this is an artifact ZIP).
11. Extract the artifact ZIP. It contains app-debug.apk.
12. Send app-debug.apk to your Android phone and install it. Allow "Install unknown apps" for the app used to open it.
13. Open Universal Ad Blocker PRO, tap START PROTECTION, and approve Android's VPN permission prompt.

If the workflow fails:
- Open the failed run and click the red failed step.
- Copy the first error message and share it for troubleshooting.
- Do not install an APK unless the build completed successfully.

LIMITATIONS:
This is an early DNS-filter MVP, not a finished universal ad remover. It blocks a limited list of known ad/tracker domains. It may not block YouTube video ads, in-app sponsored content, same-domain ads, encrypted DNS bypasses, IPv6/TCP DNS, or all redirects. Test carefully. If an app/site stops loading, stop protection.
