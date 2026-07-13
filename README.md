# Elendheim Spark

A mix-and-match idea generator you curate yourself.

You build **wheels** of raw material - domains, mechanics, moods, twists,
whatever you want - and tap to **collide** one pick from each wheel into a
fresh creative prompt. Because the wheels hold your words, every collision
sounds like your taste, not a generic prompt list.

**This app does three things:**

- **Collide** - tap to roll one pick from each of your wheels into a new idea.
  Lock the picks you like, reroll the rest, or mutate a single wheel to nudge a
  nearly-great idea one axis at a time.
- **Keep** - save the sparks you love to a local vault, add notes and tags,
  favourite them, and search back through everything.
- **Own** - curate your own decks and wheels, and export your whole idea engine
  to a single file you control. No cloud, no accounts, no tracking.

That's it. Simple, fast, yours.

## Built for

- **Dark mode first** - a calm dark-gray and soft-red look, always.
- **Accessibility from the start** - text scaling, high-contrast mode, reduce
  motion, colourblind-friendly wheel colours with name labels, screen-reader
  announcements, larger tap targets, haptics, and a line-by-line result option.
- **Privacy by design** - there is no internet permission. Your ideas never
  leave the device unless you export them yourself.

## Tech

- Kotlin + Jetpack Compose
- Room (SQLite) for local storage
- kotlinx.serialization for the export/import file format
- Storage Access Framework for one-tap export/import to your own files
- Min SDK 26

The collision engine and data layer know nothing about the UI: pure logic lives
in `engine/` and `model/`, rendering in `ui/`.

## Building

Open the project in Android Studio, or from the command line:

```
./gradlew assembleDebug      # build the debug APK
./gradlew test               # run the unit tests (engine + export round-trip)
```

You will need the Android SDK installed (Android Studio sets this up for you).

## Licence

Released under the [MIT Licence](LICENSE).
