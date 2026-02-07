## ReelAware

Mindful Instagram Usage Awareness App for Android

ReelAware is an Android application designed to increase awareness of Instagram Reels usage through session-based tracking and minimal, non intrusive notifications.
The app focuses on awareness rather than restriction, allowing users to understand their usage patterns without blocking access or enforcing limits.

----

## Features

- Session-based Instagram usage tracking

- Usage threshold notifications:

- First alert at 15 minutes

- Subsequent alerts every +10 minutes

- Single persistent notification that updates usage time (no notification spam)

- Cognitive awareness message based on session duration

- Real-time tracking status indicator:

- ON when usage access is granted

- OFF when usage access is not granted

- Automatic daily reset at midnight

- Battery optimized background monitoring using WorkManager

----

## How It Works

1.The user grants Android Usage Access permission.

2.The user manually sets the app’s battery usage to Unrestricted to ensure reliable background execution.

3.ReelAware monitors only Instagram foreground usage.

4.A usage session begins when Instagram usage increases.

5.Notifications are triggered based on actual session duration, not app launches.

6.The same notification is updated as usage crosses thresholds (15 → 25 → 35 minutes ......)

----

## Technical Overview

- Language: Java

- Platform: Android (API level 26 and above)

- Background Processing: WorkManager

- Usage Tracking: UsageStatsManager

- Data Storage: SharedPreferences

- UI Framework: XML Material Design principles.

----

## Design 

<> No dark patterns

<> No forced restrictions

<> No excessive notifications

<> Focused on self awareness and behavioral insight

----

## Version

v1.0✨
Initial release featuring session based Instagram usage awareness and background monitoring.

----

## Inspiration

The idea for ReelAware was inspired by the **Digital Wellbeing features on Google Pixel devices**.  
This project is an independent implementation, built from scratch with a different internal design and execution approach.

----


## Future Enhancements

- Support for multiple social media apps

- Daily and weekly usage analytics

- Material You dynamic theming

- Play Store release preparation

## AUTHOR 🪶

- Jaswanth Suresh
- Student and Web and Android Developer 
- This project was built to explore Android system APIs, background task scheduling, and real world usage analytics while promoting mindful technology use.
