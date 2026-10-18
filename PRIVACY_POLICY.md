# HISN Browser Privacy Policy

Last updated: October 18, 2026

## Summary

HISN is designed as a local browser. The project does not include a developer-operated browsing-history server, analytics SDK, advertising SDK, or a feature that intentionally uploads a user's browser history, bookmarks, or typed search terms to the developer.

## Data stored on the device

- Regular bookmarks and browsing history are stored in a local Room database.
- Preferences, the selected search engine, allowlist, theme, language and start-page shortcuts are stored in Android Preferences DataStore.
- Android WebView manages website cookies, cache, local storage and session state as websites use them.
- Downloaded files are saved by Android DownloadManager in the user's public Downloads folder after the user initiates a download.

## Private browsing

Private browsing opens a separate Android process with its own WebView data directory. Normal browsing history is not recorded from that session. When the private Activity finishes, HISN asks WebView to remove cookies, website storage and cache. This is a practical privacy feature, not a guarantee of complete anonymity. Websites, network administrators and internet service providers may still observe traffic. Files the user explicitly downloads remain in Downloads.

## Network requests

Web pages and the selected search engine necessarily receive the requests the user makes. HTTPS certificate errors are cancelled. HTTP pages may be opened, but HTTP traffic is not encrypted. A local host-based ad/tracker blocker may intercept requests matching its bundled list; it cannot guarantee that every advertisement or tracker is blocked.

## Website permissions

The current version denies website camera and microphone permission requests by default. File upload selectors are forwarded to Android's file picker when supported by the page and operating system.

## Controls

Users may remove individual bookmarks and history records, clear all regular history, and clear cached WebView content from Settings. Clearing cache does not clear cookies or saved website logins. Android settings can also be used to clear application data.

## Children's privacy

HISN does not knowingly collect personal data on behalf of the developer. Parents and guardians should supervise website access and any content downloaded through the browser.

## Changes and contact

This policy may be updated as the application changes. Project contact: https://iegy.net/

**Designed & Developed by Mohammed Hussein | iegy.net**
