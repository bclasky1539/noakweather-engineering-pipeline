# Contributing to NoakWeather Engineering Pipeline

This guide describes how changes are made to this repository: the workflow from
issue to merge, the build and test scripts, and the coding conventions the
parsers follow.

## Repository layout

| Path                    | Contents                                                                                                                                                   |
|-------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `noakweather-platform/` | Multi-module Maven platform: `weather-common`, `weather-ingestion`, `weather-processing`, `weather-storage`, `weather-analytics`, `weather-infrastructure` |
| `glue-jobs/`            | PySpark ETL scripts (AWS Glue)                                                                                                                             |
| `noakweather-legacy/`   | Legacy application, frozen at v0.0.5. Never modified, and never touched by version-bump automation                                                         |
| `scripts/`              | Build, test, packaging and version scripts                                                                                                                 |
| `scripts/uat/`          | UAT and discovery scripts                                                                                                                                  |

## Toolchain

- Java 17 (CI also builds on Java 21)
- Maven multi-module reactor
- JUnit 5, Testcontainers (Docker required for the full suite), WireMock
- SonarCloud, with the quality gate enforced in CI; SonarLint in the IDE for early feedback
- JaCoCo for coverage, checked locally with `diff-cover` (conda env `devtools`)

## Workflow

### 1. One issue, one branch, one pull request

- Every change starts from a GitHub issue.
- Create a branch from an up-to-date `main` with one of these prefixes:
  `feature/`, `fix/`, `test/`, `docs/`, `refactor/`. Issue numbers are not put
  in branch names.
- Work on one issue per branch and per pull request.

### 2. Commits

- Use conventional commit messages (`feat`, `fix`, `refactor`, `test`, `docs`,
  `chore`) with a scope, e.g. `fix(processing): ...`.
- Separate the summary from the body with a blank line. The body explains what
  changed and why.
- Reference the issue with `Refs #N`.
- Keep commits focused: a fix found along the way gets its own commit.

### 3. Test as you go

Run commands from the repository root.

1. Run the test class for each changed class on its own until it passes:
   ```zsh
   mvn test -pl noakweather-platform/<module> -am -Dtest=<Class>Test -Dsurefire.failIfNoSpecifiedTests=false
   ```
2. Reformat changed files in the IDE.
3. Run the full build and test suite to catch knock-on effects in other modules:
   ```zsh
   scripts/wethb.sh   # mvn validate + mvn clean compile
   scripts/wetht.sh   # full test suite (Docker/Testcontainers)
   ```
4. Check coverage of the changed lines. Stage new files first (`git add`) so
   `diff-cover` sees them:
   ```zsh
   git fetch origin
   conda run -n devtools diff-cover noakweather-platform/<module>/target/site/jacoco/jacoco.xml \
     --compare-branch=origin/main \
     --src-roots noakweather-platform/<module>/src/main/java \
     --fail-under=80
   ```
   The SonarCloud gate requires at least 80% coverage on new code; the project
   aims for 100% of changed lines.

### 4. Version and changelog

At the end of the branch work, just before pushing:

1. Bump the version with `scripts/wethv.sh`:
    - **minor** for a feature or a change to parsed data (e.g. 1.28.0 → 1.29.0);
    - **patch** for a refactor or a fix with no data impact (e.g. 1.29.0 → 1.29.1).
2. Add a CHANGELOG.md entry. Headings use this format:
   ```
   Version 1.29.1-SNAPSHOT - November 3, 2026
   ```
3. Commit the version bump and the changelog together.

### 5. Pull request and merge

- The pull request description summarizes the change and references the issue
  with `Refs #N`.
- CI must pass on both Java versions and the SonarCloud quality gate must pass
  with no new issues.
- After merging, update the local `main`:
  ```zsh
  git checkout main
  git pull origin main
  ```
  Branches are kept, not deleted, locally and on GitHub.

### 6. Closing issues

- An issue whose change affects parsed data moves to **Pending UAT** after the
  merge and is closed only after the change has been verified against live data
  in a UAT round.
- An issue with no data impact is closed when its pull request is merged.

## UAT

- UAT rounds run on the long-lived `test/uat-worldwide-metar-ingestion` branch:
  merge `main` in, run `wethb.sh`, `wetht.sh` and `wethp.sh`, then
  `scripts/uat/wethuat_discover_stations.sh` and `scripts/uat/wethuat_metar_ingest.sh`.
- Each UAT fix adds a watch pattern (`label|regex`, with the issue number in the
  label) to `scripts/uat/wethuat_discover_stations.sh`. Test the regex with
  `echo` and `grep -E` before adding it.
- Tests added on the UAT branch are permanent tests: the branch is merged into
  `main` by pull request after each round.

## Coding conventions

### General

- Prefer Java records and explicit, lightweight dependencies; no Spring Boot.
- Check a new dependency for known vulnerabilities before adding it.
- Keep Sonar cognitive complexity low: extract helpers from long or loop-based
  handlers rather than suppressing the rule.
- Regular expressions: use lookaheads such as `(?=\s|$)` instead of a trailing
  `\s+` (a group can end the report), and possessive quantifiers to avoid
  backtracking.

### Parsers (`weather.processing.parser.noaa`)

- **One parsing mechanism.** Report groups (METAR main body, METAR remarks, TAF
  forecast periods) are parsed by the group parsing engine (`SectionParser`,
  `SectionPolicy`, `GroupHandler`). New groups are added as handlers, not as
  separate parsing loops.
- **Helpers throw, handlers decide.** A parse helper never returns `null` for
  bad data; it throws `InvalidObservationException` (wrapping any
  `NumberFormatException`). The handler, or the engine, decides what happens to
  the group: rejected groups are recorded as parse issues, and remark text is
  left for `freeText`.
- **Rejected versus failed.** Bad data in a report is a *rejection*: expected,
  logged at DEBUG and counted. Any other exception is a *failure*, i.e. a parser
  bug, logged at ERROR.
- **Golden-output tests.** `NoaaMetarParserGoldenOutputTest` and
  `NoaaTafParserGoldenOutputTest` guard parsed output. When a change is meant to
  alter the output, re-record with `-Dgolden.record=true` and commit the
  expected-file diff with the change.

### Logging

- Use SLF4J placeholders (`{}`), never string concatenation, in log messages.
- Wrap a debug call in `if (LOGGER.isDebugEnabled())` **only** when an argument
  is computed: a method call (`getSummary()`, `matcher.group(0)`, record
  accessors), `String.format`, or string building. Pass plain variables without
  a guard.
- A group rejected as bad data is logged through `recordRejected` (remark
  handlers) or by the group parsing engine, on the shared category
  `weather.processing.parser.noaa.Rejected` at DEBUG. Never log a rejection with
  a parser-level debug line, and never at WARN.
- Code inside a debug guard only runs when DEBUG is enabled, so test it: use
  `LogCapture` to enable DEBUG for the logger and assert on the message.

The convention is also documented on `NoaaAviationWeatherParser`.
