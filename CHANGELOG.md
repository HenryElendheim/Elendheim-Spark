# Changelog

All notable changes to Elendheim Spark are recorded here. Versions run from
v1.0 upward; small changes bump the minor number (v1.1, v1.2...) and big ones
step toward the next whole version.

## v1.6 - Elendheim Spark

- **New app logo** - a four-point spark with nested red/orange/gold/white
  diamonds and a small companion dot. The launcher icon and the in-app splash
  mark both use it.

## v1.5 - Elendheim Spark

New look and a lot more to play with.

- **Very dark red theme** replaces the dark gray throughout.
- **No duplicate saves.** Once an idea is in the vault you can't save it again
  until you change it (reroll or edit a pick). The bookmark fills in, like a
  ticked box, to show it's already saved.
- **Way more starter content** - every wheel now ships with around 50 entries,
  plus two new decks (Story Seeds and Art Prompts) with their own wheels.
- **Bulk delete in the vault** - tap Select, choose several, and delete them at
  once.
- **Editor entries are alphabetical** now (the up/down arrows are gone); wheels
  keep their manual ordering.
- **Dice glow** - the deck flickers with a soft glow on each option as it rolls.

## v1.4 - Elendheim Spark

More control, more accessibility, more polish.

- **Red RANDOMIZE button** - the label now reads in the brand red instead of
  near-black.
- **Slot animation no longer replays** when you come back from another tab.
- **Colourblind symbols in the editor too**, matching the randomize screen.
- **Search entries inside a wheel** in the editor, and a cleaner, more readable
  entry list.
- **Wheels button for big decks** - past 8 wheels the chips move into a
  searchable on/off list (case-insensitive).
- **Limits you can tune** - up to 15 wheels per deck and 250 entries per wheel,
  each lowerable with a slider in Settings.
- **Glow on the roll click** - each line flashes for a split second as it lands.
- **Dice no longer rolls for you** and its animation is tidier.
- **Vault filters are single-select** - pick All, Favorites, or a deck and you
  see exactly that. Decks now live behind a searchable "Decks" button.
- Renamed the last "collide" wording to "roll", and **removed entry weighting**
  (the field and its setting) since it went unused.

## v1.3 - Elendheim Spark

A big accessibility and polish pass on the whole flow.

- **Collide is now "Randomize".** The tab and button are renamed, and the
  selected tab now has a solid red highlight so it is obvious where you are.
- **Switch wheels off instead of locking.** Tapping a wheel chip now removes it
  from the mix entirely -> it greys out with a line through its name and does not
  appear in the result. The lock icon is gone; chips are just names.
- **Slot-machine rolls.** Randomize spins each line through random values and
  clicks into place, one after another. Honours reduce-motion.
- **Dice animation, and it no longer rolls for you.** The dice flickers through
  decks and reveals the landed one with a small bounce; you then press Randomize
  yourself.
- **Colourblind mode uses symbols.** Each wheel gets a distinct shape, so meaning
  never rests on colour.
- **Empty your recents** from inside the Recents list.
- **Prettier vault.** Each saved idea shows a name (its deck and save number) you
  can rename, with a cleaner card. Favorites now sits right next to All.
- Tidied the Settings menu (removed the redundant "add ideas" and "manage decks"
  shortcuts; the Editor tab already covers them).

## v1.2 - Elendheim Spark

More control over each idea, and a clearer shuffle.

- **Tap any pick to change just that one.** Tapping a line in the result now
  lets you either randomize that single wheel again or type your own value in
  its place, leaving the rest untouched. This replaces the old mutate button
  with something you aim exactly where you want.
- **Random-deck dice, top-right.** The dice moved up to the top-right corner
  next to the deck name and is clearly a "jump to a random deck" control.

## v1.1 - Elendheim Spark

Calmer, easier to read, less at once.

- **Set how many ideas to mix.** A new control on the Collide screen caps how
  many wheels collide at once, so you get just the number you want instead of
  everything firing together.
- **Readable results.** Each pick now sits on its own line, generously spaced
  and colour-coded to its wheel, instead of clumping into a wall of text.
- **Recents on demand.** The recent-rolls list is tucked behind a "Recents"
  button; it keeps your last 30 rolls, dropping the oldest as new ones arrive.
- **Simpler deck switching.** The top now shows your current deck as a button;
  tap it for a searchable list of your decks instead of swiping a tab strip.

## v1.0 - Elendheim Spark

The first release. A mix-and-match idea generator you curate yourself.

- **Collide** - roll one pick from each of your wheels into a fresh idea, with
  per-wheel lock, reroll, single-wheel mutate, and a recent-history strip.
- **Vault** - save ideas locally, add notes and tags, favourite, search and
  filter, and share as plain text.
- **Editor** - build your own decks, wheels and entries, with bulk paste-add
  and reordering.
- **Export / import** - your whole idea engine in one JSON file via the system
  file picker, with merge or full-restore, plus a Markdown export of the vault.
- **Settings & accessibility** - text scaling, high-contrast, reduce motion,
  colourblind-friendly wheel colours with labels, screen-reader announcements,
  larger tap targets, haptics, line-by-line result, entry weighting and a
  default deck.
- **Private by design** - no internet permission, no accounts, no tracking.
- Ships with three starter decks (App Ideas, Song Prompts, Game Mechanics) so
  it sparks on first launch.
