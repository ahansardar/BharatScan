# BharatScan v1.1.0 Upgrade Summary (from v1.0.0)

Date: 2026-04-01

## UI/UX Refresh (Tricolor Modern)
- New India-themed visual system (saffron, navy, green accents) with warmer surfaces.
- Home, Documents, and Settings now support curated background imagery with subtle tint overlays.
- Restored tricolor top stripe across screens for brand continuity.
- Search fields and headers now sit on off-white surface layers for clearer separation from the background.

## Home Screen Enhancements
- Heritage-themed background image support with tinting.
- Stronger content separation via elevated cards and softened sections.
- Update card shown only when updates are available.

## Documents & Search
- Documents screen now supports its own themed background image with tinting and top header separation.
- OCR text is indexed and searchable across documents.

## OCR & Multilingual Support
- Expanded multilingual OCR configuration for Indian scripts (where on-device models are supported).
- Improved font fallbacks for Indian languages to avoid missing glyphs (????).

## Scanner & Editing
- Improved edge detection stability and visual guides.
- Retake action available per captured image.
- Crop experience improved with manual adjustment fallback when auto-crop fails.

## Update System
- GitHub Releases update check with manual "Check for updates" button.
- Update UI includes release notes and download progress.
- Startup update checks are supported via settings toggle.

## Tutorial Walkthrough
- In-app guided walkthrough that navigates users across Home ? Scan ? Edit ? Export ? Search.
- Replayable from Settings.

## Export & Post-Scan
- Export screen polish with India-inspired visuals.
- Default export filename prefixes with localized BharatScan branding.

## Technical Improvements
- OCR text stored per document to support search and future indexing.
- Background rendering upgraded to support image + tint + scaling.
- Cleanup of UI spacing, typography, and surface elevation consistency.

---
If you want a more formal changelog or store listing notes, tell me the target format and I’ll generate it.
