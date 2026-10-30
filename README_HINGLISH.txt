UNIVERSAL AD BLOCKER PRO — ANDROID DNS FILTER MVP

BUILD APK WITH GITHUB ACTIONS
1. Extract this project ZIP.
2. Upload the CONTENTS (including hidden .github folder) to the root of your GitHub repository.
3. Open Actions > Build Android APK > Run workflow.
4. When the run is green, download the UniversalAdBlockerPRO-debug-apk artifact and extract it.
5. Install app-debug.apk on your Android phone.
6. Open the app, tap START PROTECTION, and approve Android's VPN permission.
7. Android shows a VPN/key icon while the local DNS filter is active. Tap STOP PROTECTION to stop.

WHAT THIS VERSION DOES
- Runs a local Android VPN interface and filters IPv4 UDP DNS requests on port 53.
- Returns NXDOMAIN for domains in its built-in ad/tracker domain list.
- Forwards other DNS queries to Cloudflare DNS (1.1.1.1).
- Shows a launcher icon, active/off status and a basic blocked DNS request counter.
- Does not route general browsing traffic through a remote VPN server.

IMPORTANT LIMITATIONS
- This is still a basic DNS-filter MVP, not a complete browser ad blocker.
- CAPTCHA is a site security check, not necessarily an ad; this app does not and should not bypass CAPTCHA.
- It cannot reliably block YouTube video ads, first-party/same-domain ads, sponsored search results, cosmetic page elements or pop-up scripts.
- Android Private DNS, browser Secure DNS/DoH, some VPNs, IPv6 and DNS-over-TLS/HTTPS may bypass this DNS filter.
- The DNS filter list is small and needs ongoing maintenance. A blocked-domain count is not proof that all ads are blocked.
- The app project does not implement a web browser or Chrome extension and therefore cannot fix a double-tab/double-site opening issue inside Chrome. That needs the relevant Chrome extension/browser code.
- Test on your device. If apps/sites fail to resolve, stop protection.
