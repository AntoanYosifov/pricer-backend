## What
<!-- The change in a few lines. The title already says it in one. -->

## Why
<!-- The problem or decision behind it. Skip if the linked issue covers it. -->

Closes #

## How it was tested
<!-- Tests added or run, fixtures used, manual checks. "Not tested" is an allowed answer; say why. -->

## Checklist
- [ ] Tests cover the changed behaviour, and `./gradlew build` passes locally
- [ ] No secrets and no scraped price data committed
- [ ] Migrations are append-only — a new `V<n>__*.sql`, no edits to an applied one
- [ ] New Spring Boot technology? Its `spring-boot-<tech>` module is added too
- [ ] Docs / ADR updated if a decision changed
