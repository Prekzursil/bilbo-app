## Description

<!--
Explain what this PR does and why. Be concise but thorough.
- What problem does it solve or what feature does it add?
- Are there any important decisions or trade-offs you made?
- Is there anything reviewers should pay special attention to?
-->

## Type of Change

<!-- Check all that apply. -->

- [ ] 🐛 Bug fix (non-breaking change that fixes an issue)
- [ ] ✨ New feature (non-breaking change that adds functionality)
- [ ] 💥 Breaking change (fix or feature that changes existing behavior in a way that could break existing usage)
- [ ] 🔧 Refactor (code restructuring without behavior change)
- [ ] 📝 Documentation update
- [ ] 🧪 Test improvement (adding or improving test coverage)
- [ ] ⚙️ CI/CD change (workflow or tooling update)
- [ ] 🔒 Security fix

## Related Issues

<!--
Link related issues using keywords so they close automatically on merge.
Example: Closes #42, Related to #37
-->

Closes #

## Screenshots / Screen Recording

<!--
For any UI changes, include before/after screenshots or a screen recording.
Drag and drop images or videos directly into this field.
Remove this section if the PR does not include UI changes.
-->

| Before | After |
|--------|-------|
| _paste screenshot_ | _paste screenshot_ |

## Testing

<!--
Describe how you tested this change:
- Which manual test cases did you run?
- Were there automated tests added or modified?
- Did you test on both Android and iOS (if applicable)?
- Did you test both build flavors (playstore / github) for Android changes?
-->

## Checklist

<!-- Check each item as you complete it. PRs that skip these steps will be asked to address them. -->

- [ ] My branch follows the naming convention (`feature/`, `fix/`, `docs/`, `refactor/`, etc.)
- [ ] My commits follow [Conventional Commits](https://www.conventionalcommits.org/) format
- [ ] `./gradlew detekt` passes with no new violations
- [ ] `./gradlew :shared:allTests` passes locally
- [ ] `./gradlew :androidApp:assembleGithubDebug` builds cleanly
- [ ] SwiftLint passes for any iOS-touching changes (`swiftlint lint`)
- [ ] I have added or updated tests for my changes (where applicable)
- [ ] I have updated documentation (README, KDoc, inline comments) where needed
- [ ] I have NOT hardcoded any secrets, API keys, or credentials
- [ ] Screenshots or recordings are attached for any UI changes
- [ ] CI is passing (or I have explained why a failure is unrelated)
