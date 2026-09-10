# FitBrief

FitBrief is a Kotlin/Jetpack Compose MVVM Android app that turns Health Connect data into private, plain-language fitness summaries. Health data and AI inference stay on the device.

## Features

- Health Connect availability detection with an install/update path when the provider is unavailable.
- Granular, partial-grant-safe read permissions for steps, distance, active and total calories, exercise, heart rate, and sleep.
- Selectable ranges: Today, 7 days, and 30 days.
- Aggregate cards for steps, distance, calories, exercise, average heart rate, and sleep.
- Metric detail screens with selectable dates and charts:
  - intraday heart-rate samples and range indicators
  - nightly sleep duration in hours
  - daily distance, calories, exercise, and steps
- Activity timeline that ignores zero-value records and groups events around observed activity cadence and spikes rather than a fixed minute interval.
- AI insight and timeline summaries shown directly on the dashboard.
- Optional WorkManager notification scheduling for a daily FitBrief reminder.

The app requests background and history access so scheduled work can read Health Connect data beyond the default history window. Health Connect may require those permissions to be enabled separately in system settings.

## Offline AI

`Summarizer` is selected at runtime in this order:

1. **ML Kit GenAI Prompt API** using Gemini Nano/AICore when the device reports the feature as available. Downloadable and downloading states are surfaced before inference.
2. **LiteRT-LM** through the optional reflection backend with a local Gemma 3 1B 4-bit model.
3. A deterministic on-device template backend when no generative runtime is available, so raw aggregates remain usable.

Prompts contain clean aggregates and summarized activity windows, never raw Health Connect records. Once Gemini Nano or the LiteRT-LM model is ready, summaries work in airplane mode. The only permitted network operation is the optional first-use LiteRT-LM model download.

## Build

The project uses Kotlin, Compose, Coroutines/Flow, Health Connect, Navigation Compose, and WorkManager.

```bash
./gradlew assembleDebug
```

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
5. Refresh FitBrief, switch between Today/7 days/30 days, and open metric cards to inspect daily graphs and timelines.
6. Revoke individual permissions in Health Connect to verify partial-grant and empty-data states.

For background testing, grant background/history access, enable notifications, and allow the scheduled WorkManager job to run. Android may defer periodic work according to battery and background-execution policy.

## Privacy and safety

FitBrief never uploads Health Connect data. AI output is descriptive and motivational, not medical advice or a diagnosis. The UI includes a privacy note and disclaimer; consult a qualified healthcare professional for medical concerns.
