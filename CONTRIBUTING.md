# Contributing to BharatScan

Thanks for considering a contribution. This guide keeps changes consistent and easy to review.

## Getting Started
1. Fork the repo and create a feature branch.
2. Set up the project as described in `README.md`.
3. Make a focused change (one feature or fix per PR when possible).

## Development Guidelines
- Follow existing Kotlin/Compose patterns and naming conventions.
- Keep UI changes consistent with the current visual language.
- Prefer small, composable functions for UI and domain logic.
- Add or update tests when behavior changes.

## Testing
Run at least the relevant tests before opening a PR:

```bash
./gradlew :app:test
./gradlew :app:connectedAndroidTest
```

If a test is not applicable or requires special hardware, note that in the PR description.

## Commit And PRs
- Write clear, descriptive commit messages.
- Describe what changed, why, and how it was tested.
- Include screenshots or screen recordings for UI changes where possible.

## License
By contributing, you agree that your contributions will be licensed under GPL-3.0-or-later, consistent with `LICENSE_HEADER` and the project license.
