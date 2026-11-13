# App resources

## Run requirements

| What | Needed? | Why |
|---|---|---|
| JDK 17 | Yes | Android Gradle Plugin runtime |
| Android SDK Platform 36 | Yes | Compile target |
| Android Build Tools 36.0.0 | Yes | Package and sign APK |
| Gradle 9.6 | Yes | Build system |
| Internet on the phone | No | `termvault.txt` and paired audio are bundled locally |
| Android speech recognition service | No | There is no voice input feature |
| Audio playback | Yes | Plays bundled English/Mandarin M4A clips |
| Microphone / API key / Docker / database | No | Not used |

## Dependencies

No third-party app libraries. The app reads the bundled `termvault.txt` list and plays one matching M4A clip for each bilingual entry through Android's media player.
