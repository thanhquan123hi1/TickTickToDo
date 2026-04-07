# AI Agent Guidelines for TickTickToDo

This document provides instructions for AI coding agents to work productively within the TickTickToDo codebase.

## Project Overview

TickTickToDo is an Android application written primarily in Java, with some components (like App Widgets) written in Kotlin.
The app relies on standard Android Jetpack architecture components:
- **ViewModels and LiveData**: For UI state management and lifecycle awareness (e.g., `TaskViewModel.java`, `SchoolCalendarViewModel.java`).
- **Room Database**: For local data persistence (e.g., `AppDatabase`, `TaskDao`, entities like `Task` and `TaskReminder`).
- **WorkManager / Schedulers**: Background task synchronization and reminders (e.g., `SchoolCalendarSyncWorker.java`, `ReminderScheduler.java`).

## Core Architecture and Service Boundaries

- **Database / Data Layer**: Found in `hcmute.edu.vn.ticktick.database.*`. Room is used for mapping entities to SQLite. Repositories coordinate data access (e.g., `SchoolCalendarRepository.java`).
- **UI / Presentation Layer**: Found in `hcmute.edu.vn.ticktick.ui.*` and respective feature packages (e.g., `hcmute.edu.vn.ticktick.schoolcalendar.*`).
- **Reminders / Background Sync**: Found in `hcmute.edu.vn.ticktick.reminder.*`. Handles push notifications, setting up standard Android scheduled alarms/intents (`TaskReminderReceiver.java`).
- **Parsing Utilities**: Specific utilities for custom workflows, such as `VietnameseDateTimeParser.java` and `OCRTaskParser.java`, or standard format parsing (`IcsParser.java`).
- **Widgets**: Found in `hcmute.edu.vn.ticktick.widget.*`. Written in Kotlin, utilizing `TasksWidgetProvider` and `RemoteViewsService`.

## Development Conventions

- **Language Usage**: Use Java for existing UI and core logic to maintain consistency. Prefer Kotlin for new App Widgets or modern Jetpack integrations where matching existing Kotlin files. Always check the neighboring file extensions when adding a new class.
- **Dependency Management**: Standard Gradle with Kotlin DSL (`build.gradle.kts`). Version catalog is used (`gradle/libs.versions.toml`). Always check the TOML file before adding any new libraries.
- **Data Flow**: Use Repositories to handle Room calls, emit reactive streams via `LiveData` to `ViewModel`s, and observe these streams from Activities/Fragments/Dialogs.

## Important Workflows

- Run `./gradlew app:assembleDebug` to build the app APK.
- Changes to database entities (`hcmute.edu.vn.ticktick.database.*`) require generating and inspecting new Room schemas. Avoid schema-breaking changes without appropriate migration paths.
- For new reminder configurations, ensure updating the `ReminderScheduler.java` along with configuring actual receiver capabilities in `AndroidManifest.xml`.

