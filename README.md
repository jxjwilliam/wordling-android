# Wordling — Offline Vocabulary Listening for Android

A compact native Android app that randomly plays English vocabulary entries from `termvault.txt`, followed by their Mandarin meanings. The vocabulary and prerecorded audio are bundled in the app, so playback works offline.

## Features

- 486 English–Chinese terms from `termvault.txt`
- Random order without repeats until the current round finishes
- English pronunciation followed by a Mandarin meaning
- Bundled audio; no network, microphone, online translation, or runtime voice API
- Play/pause, replay, next/previous controls in the app, on supported headphones, and through Android media controls

## Build

Requirements: JDK 17, Android SDK Platform 36, Android Build Tools 36.0.0.

```sh
./gradlew assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`. The app supports Android 8.0 (API 26) and newer.

## 中文简介

Wordling 是一个离线 Android 词汇听力应用。它从内置的 `termvault.txt` 中随机播放英文词条和普通话释义。词库与发音音频均随应用安装；支持耳机和锁屏媒体控制。
