# Changelog

All notable changes, per phase. Format: Keep a Changelog; commits follow Conventional Commits.

## [Unreleased] — Phase 0: Foundation

### Added
- Gradle multi-module project with `build-logic` convention plugins and a pinned version catalog.
- Three apps (FoxDrop, FoxDrop Rider, FoxDrop Business): branded animated splash, first-run intro, onboarding,
  location explainer, animated Log In ⇄ Sign Up with phone OTP, adaptive home shells.
- Design system: brand theme (light/dark), Poppins, spacing/shape/motion tokens with "Remove animations" support,
  buttons with loading → ✓ morph, OTP field, skeletons, empty/error states, pill tabs, navigation shell.
- Encrypted session storage (DataStore + Tink + Android Keystore) behind supabase-kt.
- Launcher icons and splash vector generated from `reference/logos` (`tools/assets/make_assets.py`).
- Supabase local config with test OTPs; CI workflows for Android, backend and secret scanning.
- 77 unit/ViewModel/screenshot tests and 51 screenshot baselines.
