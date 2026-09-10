# FitBrief

FitBrief is a Kotlin/Jetpack Compose MVVM Android app that turns Health Connect data into private, plain-language fitness summaries. Health data and AI inference stay on the device.

## Features

- Health Connect availability detection with an install/update path when the provider is unavailable.
- Granular, partial-grant-safe read permissions for steps, distance, active and total calories, exercise, heart rate, and sleep.
- Selectable ranges: Today, Week (Monday to Sunday), and calendar Month. The selected tab is remembered, and each tab's data is cached in the ViewModel so switching back is instant; pull down to refresh the current tab.
- Aggregate cards for steps, distance, calories, exercise, average heart rate, and sleep.
- Metric detail screens with paging by day, week, or month and charts:
  - Today: intraday heart-rate samples with zone bands, and per-activity-window bars for the other metrics
  - Week and Month: one bar or point per calendar day from Health Connect daily aggregates; tap a day to drill into it
  - a heart-rate range headline (low, average, high, highest zone) and an AI insight per metric
- Activity timeline built from one event per movement period: steps and distance records seed a window, a pause of more than 10 minutes starts a new one, and calories burned are attributed to the window they overlap. Heart-rate windows use the same gap. Exercise and sleep sessions are one event each.
- The dashboard shows only notable activity: walking windows of at least 10 minutes and 500 steps, heart-rate windows of at least 10 minutes, and every exercise or sleep session. Week and Month collapse the timeline into one card per day or per week.
- AI insight card on the dashboard and one descriptive sentence per notable timeline event.
- Optional WorkManager notification scheduling for a daily FitBrief reminder.

The app requests background and history access so scheduled work can read Health Connect data beyond the default history window. Health Connect may require those permissions to be enabled separately in system settings.

## Offline AI

`Summarizer` is selected at runtime in this order:

1. **ML Kit GenAI Prompt API** using Gemini Nano/AICore when the device reports the feature as available. Downloadable and downloading states are surfaced before inference.
2. **LiteRT-LM** through the optional reflection backend with a local Gemma 3 1B 4-bit model.
3. A deterministic on-device template backend when no generative runtime is available, so raw aggregates remain usable.

Prompts are built in `summary/Prompts.kt` from structured data lines, never raw Health Connect records:

- The dashboard summary lists only the metrics that exist, each with a target scaled to the days covered (10,000 steps and 30 exercise minutes per day, 7 to 9 hours of sleep per night). A suggestion is added only when a metric with a target is clearly below it.
- Timeline sentences are descriptive only, with local times and raw values; no praise or suggestions. Windows below the notability thresholds skip the model and keep a templated sentence.
- Metric insights include daily averages and targets for multi-day ranges. Heart rate reports the lowest, average, and highest readings and has no target. Calories are always described as energy burned.

Once Gemini Nano or the LiteRT-LM model is ready, summaries work in airplane mode. The only permitted network operation is the optional first-use LiteRT-LM model download.

## Architecture

MVVM with unidirectional data flow. A single `FitBriefViewModel` exposes `StateFlow<FitBriefUiState>`; screens are stateless composables that receive state slices and emit `FitBriefEvent`s. Navigation Compose owns the back stack.

- `data/` — `HealthRepository` (implemented by `HealthConnectRepository`), `FitBriefPreferencesStore`, range and snapshot models, and `ActivityTimeline.kt` with the pure windowing and notability rules.
- `summary/` — `SummaryService` (implemented by `SummarizerFactory`), the Gemini Nano, LiteRT-LM, and template summarizers, and `Prompts.kt`.
- `notifications/` — WorkManager scheduling behind `NotificationScheduler`.
- `ui/` — one file per screen, `components/` for shared widgets, `charts/` for the Canvas graphs, and `metrics/` for pure presentation mappers (metric cards, chart series, detail header).
- `AppContainer` on `FitBriefApplication` builds the dependency graph; the ViewModel receives its collaborators through a factory, so it is unit tested with fakes.

## Build and test

The project uses Kotlin, Compose, Coroutines/Flow, Health Connect, Navigation Compose, and WorkManager.

```bash
./gradlew assembleDebug
```

```bash
./gradlew testDebugUnitTest
```

Unit tests cover the range and day-count logic, activity windowing, prompt construction, presentation mappers, and the ViewModel.

The app uses `dev.rrohaill.fitbrief`, requires API 26 or newer, and targets SDK 37. Health Connect itself is supported on Android 9/API 28 and newer; on older devices FitBrief shows an unavailable state instead of crashing.

## Configuring the LiteRT-LM model

LiteRT-LM is optional. Provide an approved model-host URL when building:

```bash
./gradlew assembleDebug \
  -PLITERT_MODEL_URL=https://your-approved-model-host/gemma-3-1b-4bit.litertlm
```

The model is downloaded only when the fallback backend is first used, stored under app-private storage, and downloaded with HTTP range requests so interrupted downloads can resume. No health data is sent to the model host. If the URL or compatible runtime is unavailable, FitBrief uses the local template backend and still displays aggregate stats.

## Health Connect setup and testing

1. Install or update the Health Connect app on a compatible Android 9+ device.
2. Install FitBrief and open the permission flow.
3. Grant any subset of the requested read permissions; FitBrief continues with the metrics that are available.
4. Populate Health Connect using its sample-data controls or a compatible test data provider.
5. Refresh FitBrief, switch between Today/Week/Month, and open metric cards to inspect the charts, page between periods, and drill into single days.
6. Revoke individual permissions in Health Connect to verify partial-grant and empty-data states.

For background testing, grant background/history access, enable notifications, and allow the scheduled WorkManager job to run. Android may defer periodic work according to battery and background-execution policy.

## Privacy and safety

FitBrief never uploads Health Connect data. AI output is descriptive and motivational, not medical advice or a diagnosis. The UI includes a privacy note and disclaimer; consult a qualified healthcare professional for medical concerns.
