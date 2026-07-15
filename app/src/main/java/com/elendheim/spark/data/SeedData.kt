package com.elendheim.spark.data

import com.elendheim.spark.model.Deck
import com.elendheim.spark.model.Entry
import com.elendheim.spark.model.Wheel

/**
 * The starter decks shipped so the app sparks on first launch.
 *
 * A blank app is intimidating; these decks show what good wheels look like and
 * are fun immediately. Every entry is fully editable and removable -> this is a
 * starting palette, not a fixed menu. Each wheel ships with a generous list so
 * the combinations feel endless from the first tap.
 *
 * Nothing here touches the database. It just builds model objects; the
 * repository decides when to write them in (only on a fresh install).
 */
object SeedData {

    /** A small, colourblind-considerate chip palette. */
    private val palette = listOf(
        "#E0555A", "#4FA6D9", "#E0A44F", "#6FBF73", "#B588E0", "#E07FB0"
    )

    private fun wheel(name: String, colorIndex: Int, items: List<String>): Wheel =
        Wheel(
            id = newId(),
            name = name,
            colorHex = palette[colorIndex % palette.size],
            entries = items.map { Entry(id = newId(), text = it) }
        )

    private fun deck(name: String, wheels: List<Wheel>, createdAt: Long): Pair<Deck, List<Wheel>> {
        val d = Deck(id = newId(), name = name, wheelIds = wheels.map { it.id }, createdAt = createdAt)
        return d to wheels
    }

    /**
     * Build all starter content. Returns the decks and the flat list of every
     * wheel across them. [now] is passed in so seeding stays deterministic.
     */
    fun build(now: Long): Pair<List<Deck>, List<Wheel>> {
        val decks = mutableListOf<Deck>()
        val wheels = mutableListOf<Wheel>()

        fun add(built: Pair<Deck, List<Wheel>>) {
            decks += built.first
            wheels += built.second
        }

        // --- App Ideas ---
        add(deck("App Ideas", listOf(
            wheel("Domain", 0, listOf(
                "music", "photos", "notes", "health", "focus", "money", "home", "travel",
                "pets", "sound", "language", "games", "reflection", "the two-of-us",
                "Nothing-phone", "cooking", "sleep", "reading", "fitness", "meditation",
                "gardening", "budgeting", "journaling", "podcasts", "weather", "commuting",
                "friendships", "dating", "parenting", "study", "coding", "drawing",
                "writing", "movies", "collecting", "cleaning", "shopping", "recycling",
                "birds", "stars", "dreams", "habits", "goals", "memories", "recipes",
                "plants", "coffee", "running", "cycling", "swimming", "hiking", "chores"
            )),
            wheel("Mechanic", 1, listOf(
                "track over time", "mix & match", "generate from a seed", "one-tap capture",
                "decay unless revisited", "tap-to-reveal", "streak/tally", "roulette/random",
                "timer/countdown", "compare two things", "curated collection", "guided sequence",
                "swipe to sort", "voice-only input", "photo-first", "map it out", "rank them",
                "vote daily", "shuffle deck", "spaced repetition", "before/after", "checklist",
                "mood ring", "heat map", "wheel of fortune", "drag to reorder", "pinch to zoom",
                "shake to reset", "long-press to hide", "tag and filter", "auto-group",
                "milestone badges", "daily prompt", "weekly digest", "quiet log", "one number",
                "progress bar", "flip card", "scratch to reveal", "countdown to zero",
                "grow a garden", "fill a jar", "connect the dots", "build a streak",
                "unlock as you go", "surprise reward", "gentle nudge", "no-goal mode",
                "start anywhere", "finish anytime", "pick up where you left"
            )),
            wheel("Twist", 2, listOf(
                "in your note-name format", "no cloud at all", "Elendian flavor",
                "brutally minimal", "only works while walking", "resurfaces old entries",
                "fades over time", "dark-gray/soft-red brand", "one screen only",
                "weirdly personal", "secretly trains a skill", "works offline forever",
                "no accounts ever", "one tap a day", "gets harder each week", "rewards patience",
                "punishes streaks", "only at night", "only before noon", "handwritten feel",
                "typewriter sounds", "grayscale until earned", "unlocks a color", "haiku-only",
                "swearing encouraged", "grandparent-friendly", "kid-safe", "for one person only",
                "shareable as a card", "prints to paper", "reads aloud", "whispers back",
                "keeps a secret", "forgets on purpose", "never notifies", "notifies once",
                "hides the numbers", "shows only trends", "celebrates failure", "no undo",
                "everything is undo", "single file you own", "export is the point",
                "works on a potato", "loads instantly", "no loading screens", "no menus",
                "gesture-only", "one big button", "made in a weekend", "will outlive the trend"
            ))
        ), now))

        // --- Song Prompts ---
        add(deck("Song Prompts", listOf(
            wheel("Feeling", 3, listOf(
                "avoidance", "restless hope", "quiet grief", "defiance", "homesickness",
                "numb", "tender", "unraveling", "steady", "haunted", "reckless joy",
                "slow dread", "relief", "envy", "forgiveness", "stubborn pride", "loneliness",
                "wonder", "resentment", "gratitude", "panic", "contentment", "regret",
                "infatuation", "burnout", "spite", "nostalgia", "anticipation", "shame",
                "tenderness", "rage", "peace", "longing", "boredom", "hope against hope",
                "cold fury", "warmth", "isolation", "belonging", "grief that lifts",
                "guilt", "release", "obsession", "calm before", "aftermath", "small triumph",
                "quiet defeat", "second wind", "surrender", "resolve", "wistfulness"
            )),
            wheel("Image", 4, listOf(
                "a cold window", "an empty chair", "headlights on a wall", "a packed bag",
                "static on a radio", "a frozen field", "a phone that won't ring",
                "a door left open", "a coat still on the hook", "an unmade bed",
                "rain on a bus window", "a dial tone", "a half-drunk coffee", "a locked gate",
                "a flickering porch light", "a train pulling away", "a ring left on a sink",
                "footprints in snow", "a burnt-out sign", "a payphone", "a kitchen at 3am",
                "a suitcase by the door", "a photograph face-down", "a dying houseplant",
                "a highway at dawn", "a stopped clock", "a spare key", "a full ashtray",
                "a red balloon", "a wedding dress in plastic", "a broken swing", "a note in a pocket",
                "a car that won't start", "a moth at a lamp", "an unsent text", "a dog waiting",
                "a candle burned down", "a cracked screen", "a last cigarette", "an open field",
                "a motel sign", "a fogged mirror", "an old mixtape", "a missed call",
                "a cold radiator", "the last stair", "a folded letter", "a spare room",
                "a light left on", "a lighthouse", "an empty platform"
            )),
            wheel("Move", 5, listOf(
                "reframe the feeling as installed from outside", "concrete image over statement",
                "repeat a line that shifts meaning", "a bridge that turns the whole song",
                "name a person who isn't there", "answer a question you never asked",
                "start at the end", "let the chorus contradict the verse", "whisper the loudest part",
                "count something small", "address the weather", "quote a stranger",
                "break the fourth wall", "leave the sentence unfinished", "make a list",
                "turn a cliche inside out", "sing the stage direction", "describe a smell",
                "give a color a job", "trade blame for a fact", "zoom out to the map",
                "zoom in to a hand", "let a sound become a word", "repeat and then refuse",
                "promise then withdraw", "ask permission", "confess a small lie", "name the room",
                "let the season do the crying", "make the title a warning", "end on a question",
                "spell it out then take it back", "let silence be the hook", "keep one word from the title out",
                "turn a place into a person", "turn a person into a place", "use the wrong tense on purpose",
                "borrow a prayer's shape", "answer with a memory", "undercut the swell",
                "hold a note past comfort", "change who 'you' is", "let the last line reopen it",
                "make a small thing enormous", "make an enormous thing small", "repeat the first line last",
                "let the music lie", "tell it to a child", "tell it to yourself", "don't resolve"
            )),
            wheel("Constraint", 0, listOf(
                "three notes only", "no drums till 1:00", "tempo locked at 67", "one-word chorus",
                "whole song in second person", "no rhymes at all", "only internal rhymes",
                "under two minutes", "one chord", "no chorus", "a cappella intro",
                "one breath per line", "no 'I'", "no 'you'", "present tense only", "past tense only",
                "no adjectives", "title never sung", "ends where it starts", "key change at the bridge",
                "half-time outro", "spoken bridge", "only questions", "no metaphors",
                "all metaphor", "under 50 words", "one verse, sung twice", "no snare",
                "hummed hook", "field recording underneath", "count-in you keep", "false ending",
                "one syllable words only", "written in an hour", "no editing allowed",
                "swap verse and chorus melodies", "modulate up a semitone", "drop the bass for the last chorus",
                "whisper verses, belt chorus", "one long take", "no click track", "start mid-sentence",
                "the outro is the intro", "a round you sing alone", "only major sevenths",
                "no cymbals", "sing it in the car", "record it at night", "keep the first take",
                "let it fall apart at the end"
            ))
        ), now + 1))

        // --- Game Mechanics ---
        add(deck("Game Mechanics", listOf(
            wheel("Core loop", 1, listOf(
                "turn-based combat", "deck-building", "idle/incremental", "roguelike run",
                "life-sim tick", "resource conversion", "tower defense", "match-three",
                "base building", "auto-battler", "grid tactics", "card drafting", "bullet hell",
                "puzzle-platformer", "farming cycle", "trading loop", "stealth patrol",
                "rhythm timing", "physics sandbox", "survival crafting", "dungeon crawl",
                "city planning", "party management", "gacha pull", "clicker escalation",
                "route planning", "worker placement", "tile laying", "hand management",
                "engine building", "push your luck", "area control", "deduction", "drafting draft",
                "bag builder", "dice placement", "loop of loops", "gather-craft-fight",
                "explore-fight-loot", "plant-wait-harvest", "buy-low-sell-high", "sneak-hack-escape",
                "score-attack", "time-attack", "combo chaining", "territory painting",
                "day/night shift", "hunger clock", "one more turn", "risk then bank"
            )),
            wheel("Mix axis", 2, listOf(
                "weapon x offhand", "status apply x consume", "trait x mutation", "risk x reward",
                "time x reversal", "light x sound", "heat x cold", "order x chaos", "growth x decay",
                "speed x weight", "range x cost", "noise x stealth", "luck x skill", "size x fragility",
                "color x meaning", "shape x function", "memory x forgetting", "gravity x float",
                "day x night", "life x mana", "attack x terrain", "element x element", "class x curse",
                "hunger x sleep", "faith x doubt", "gold x guilt", "fame x heat", "power x sanity",
                "distance x time", "crop x weather", "tool x material", "spell x gesture",
                "card x position", "unit x formation", "map x fog", "loot x weight",
                "combo x cooldown", "shield x charge", "poison x heal", "reflect x absorb",
                "summon x sacrifice", "build x burn", "hide x seek", "buy x break",
                "grow x prune", "aim x recoil", "jump x dash", "block x parry", "steal x share",
                "bet x fold"
            )),
            wheel("Hook", 3, listOf(
                "a death-message left for others", "charge-up telegraphs", "fair RNG enemies",
                "generational inheritance", "emergent culture", "a god who can rewind",
                "permadeath with legacy", "asynchronous ghosts", "a shared world clock",
                "npcs remember you", "one-life leaderboards", "your loss helps others",
                "the map is the enemy", "weather that fights back", "a rival that levels with you",
                "notes hidden by players", "a town you slowly rebuild", "pets that evolve",
                "a language you decode", "seasons that reset the meta", "a boss made of your mistakes",
                "the tutorial lies", "a merchant with a grudge", "co-op with a stranger's echo",
                "a diary that writes itself", "enemies that mourn", "loot that ages", "a curse you gift",
                "a world that heals when you leave", "achievements nobody else has", "a map you draw",
                "silence as a mechanic", "trust as a resource", "reputation you can't see",
                "a timer only you know", "the villain narrates", "a helper who wants out",
                "your reflection plays too", "a debt the town owes you", "a key that opens people",
                "one shared inventory", "the score judges you", "a mentor who fades",
                "an ending you unlock by losing", "a rumor system", "a market you crash",
                "a garden that remembers", "a shadow that copies you", "a name you earn",
                "a door only cowards find"
            )),
            wheel("Twist", 4, listOf(
                "wordless (no text)", "art-free but juicy", "single-file", "everything is data not hardcode",
                "breeds like evolution", "one more turn", "no fail state", "only a fail state",
                "plays itself if you leave", "controls invert over time", "the ui is the boss",
                "money is health", "time is currency", "you play the map", "you play the weather",
                "the tutorial never ends", "no save, only memory", "every run rewrites lore",
                "the enemy writes the rules", "you lose to win", "co-op only", "solo forever",
                "one button", "no buttons", "voice controlled", "played in one sitting",
                "played over a year", "resets every midnight", "shared across all players",
                "different each device", "the credits are a level", "the menu is the game",
                "silence wins", "kindness is a weapon", "greed is the trap", "patience is a stat",
                "the timer is fake", "the score is a lie", "you are the villain", "everyone is the hero",
                "no tutorials at all", "figure it out", "readable in a day", "unbeatable on purpose",
                "beatable by anyone", "the world ages you", "you inherit a save", "leave a message",
                "delete to finish", "keep to lose"
            ))
        ), now + 2))

        // --- Story Seeds (new) ---
        add(deck("Story Seeds", listOf(
            wheel("Character", 0, listOf(
                "a retired locksmith", "a lighthouse keeper's kid", "a disgraced chef",
                "a night-shift nurse", "a con artist gone straight", "a grieving beekeeper",
                "a teenage forger", "an ex-astronaut", "a small-town undertaker", "a failed magician",
                "a translator who lies", "a repo woman", "a hermit botanist", "a washed-up boxer",
                "a runaway heir", "a border-town DJ", "a clockmaker with tremors", "a cartographer",
                "a ghostwriter", "a storm chaser", "a monk who doubts", "a pawn-shop owner",
                "a stunt double", "a tarot reader", "a coroner poet", "a bus driver at dawn",
                "a museum guard", "a beekeeper's rival", "a shy arsonist", "a retired spy",
                "a piano tuner", "a lonely sysadmin", "a widowed farmer", "a teen medium",
                "a disbarred lawyer", "a diner regular", "a lighthouse painter", "a river guide",
                "a taxidermist", "a fortune teller's son", "a jazz drummer", "a locksmith's apprentice",
                "a former child star", "a soft-spoken bouncer", "a grief counselor", "a hoarder",
                "a stargazer", "a bell ringer", "a puppet maker", "a retired thief"
            )),
            wheel("Setting", 1, listOf(
                "a town that floods yearly", "a mall after closing", "a mountain monastery",
                "a sinking cruise ship", "a border checkpoint", "a snowed-in cabin",
                "a failing amusement park", "a research station", "a desert motel",
                "a city that never sleeps", "an island with one road", "a subway at 4am",
                "a haunted vineyard", "a lighthouse in fog", "a farm in drought",
                "a hospital during a storm", "a ferry between nowhere", "a ghost town revived",
                "a cathedral under repair", "a diner on route 9", "a submarine", "a rooftop garden",
                "a prison library", "a radio station", "a carnival packing up", "a quarantine ward",
                "a bunker", "a floating market", "a train that never stops", "a company town",
                "a monastery brewery", "a tide-pool coast", "a data center", "a wax museum",
                "a border river", "a mining camp", "a retirement home", "an orchard at harvest",
                "a lightship", "a corn maze", "a fog-bound harbor", "a mountain pass",
                "a drive-in theater", "a bell tower", "a greenhouse in winter", "a salt flat",
                "a canal town", "a border bar", "a rooftop pool", "a last gas station"
            )),
            wheel("Conflict", 2, listOf(
                "a debt comes due", "a secret twin appears", "the water is rising",
                "a promise breaks", "someone must confess", "the map is wrong",
                "a stranger knows too much", "the money is fake", "the cure has a cost",
                "the letter arrives late", "two loyalties collide", "a lie must be maintained",
                "the witness recants", "the deadline moves up", "the only exit is guarded",
                "a favor is called in", "the inheritance has a catch", "the alibi cracks",
                "the storm cuts them off", "the past shows up", "a child goes missing",
                "the vote is tied", "the key doesn't fit", "the plan needs a traitor",
                "the town wants them gone", "the truth would help no one", "the clock is wrong",
                "a rival returns", "the offer is too good", "the door won't open",
                "a body is found", "the phone lines are down", "the transplant is denied",
                "the wrong person is blamed", "the transfer is refused", "the last train leaves",
                "the well runs dry", "a name is on a list", "the will is contested",
                "the shipment is late", "an old score resurfaces", "the passport is flagged",
                "the harvest fails", "the bridge is out", "a confession is recorded",
                "the loan is called", "the transplant list moves", "a stranger claims the room",
                "the tide traps them", "the vote is bought"
            )),
            wheel("Twist", 3, listOf(
                "told backwards", "narrated by the dead", "everyone is lying a little",
                "the villain is right", "no one is coming to help", "the hero fails and it's fine",
                "the setting is the antagonist", "a kindness ruins everything", "the letter was never sent",
                "the twin was imagined", "it's all one long night", "the rescue is the trap",
                "the confession changes nothing", "the map was a memory", "the debt was already paid",
                "the child was the witness", "the storm was a blessing", "the exit was open the whole time",
                "the stranger was expected", "the money was never real", "the cure was the disease",
                "the past never happened", "the town was right", "the plan worked too well",
                "the traitor was loyal", "the deadline was fake", "the door led back",
                "the body got up", "the vote didn't matter", "the key opened a person",
                "the truth set no one free", "the promise kept itself", "the favor was a curse",
                "the offer was a test", "the alibi was true", "the witness was the killer",
                "the water saved them", "the name was theirs", "the will was a forgery",
                "the harvest was sabotage", "the score was settled long ago", "the passport was a gift",
                "the bridge was never there", "the recording was blank", "the loan forgave itself",
                "the list was a lie", "the room was always theirs", "the tide gave them time",
                "nothing was bought", "it happens again tomorrow"
            ))
        ), now + 3))

        // --- Art Prompts (new) ---
        add(deck("Art Prompts", listOf(
            wheel("Subject", 0, listOf(
                "a sleeping fox", "a cracked teacup", "a city in the rain", "a lone astronaut",
                "an old lighthouse", "a koi pond", "a paper crane", "a rusted key", "a wolf and moon",
                "a greenhouse", "a violin", "a snail's shell", "a mountain village", "a jellyfish",
                "a broken clock", "a lantern", "a stack of books", "a cat in a window",
                "a fishing boat", "a hummingbird", "a chess set", "a candle flame", "a bicycle",
                "a raven", "a tide pool", "a cup of coffee", "a dandelion", "a train station",
                "a spiral staircase", "a origami tiger", "a whale", "a compass", "a bonfire",
                "a mushroom ring", "a wind-up toy", "a swing set", "a full bookshelf",
                "a moth", "a stormy sea", "a hot-air balloon", "a cracked mirror", "a music box",
                "a garden gate", "a stag", "a market stall", "a pocket watch", "a treehouse",
                "a comet", "a stray dog", "an empty theater"
            )),
            wheel("Style", 1, listOf(
                "flat vector", "watercolor wash", "cel-shaded", "pixel art", "line art",
                "impressionist", "art deco", "brutalist", "cyberpunk neon", "ukiyo-e",
                "charcoal sketch", "low-poly", "stained glass", "papercut layers", "risograph",
                "gouache", "ink and wash", "vaporwave", "art nouveau", "woodcut",
                "chalk pastel", "isometric", "collage", "silhouette", "blueprint",
                "storybook", "noir", "minimal one-line", "double exposure", "glitch",
                "claymation", "cross-hatch", "graffiti", "surrealist", "photoreal",
                "comic panel", "etching", "mosaic", "neon sign", "duotone", "halftone",
                "embroidery", "sticker sheet", "oil impasto", "sumi-e", "cutout animation",
                "concept sketch", "tattoo flash", "medieval manuscript", "chibi"
            )),
            wheel("Palette", 2, listOf(
                "muted earth tones", "neon on black", "pastel dawn", "monochrome blue",
                "warm autumn", "cold winter", "sepia", "candy pop", "forest greens",
                "sunset gradient", "grayscale + one red", "teal and orange", "desert sand",
                "deep sea", "cherry blossom", "storm gray", "gold and navy", "coral reef",
                "midnight purple", "lemon and slate", "rust and cream", "moss and stone",
                "ice and ash", "berry and wine", "olive and mustard", "ocean at dusk",
                "peach and mint", "charcoal and gold", "sage and clay", "plum and cream",
                "arctic blues", "ember reds", "meadow greens", "fog whites", "amber lamplight",
                "cool moonlight", "warm candlelight", "acid green", "dusty rose", "slate and brass",
                "sea foam", "lavender fields", "burnt sienna", "cobalt and cream", "honey and ink",
                "smoke and rose", "pine and snow", "brick and sky", "wheat and denim", "black and gold"
            )),
            wheel("Constraint", 3, listOf(
                "two colors only", "no straight lines", "one continuous line", "fits in a circle",
                "left-right symmetry", "negative space does the work", "no black", "no white",
                "made of dots", "square canvas", "extreme close-up", "tiny in a big frame",
                "only triangles", "from above", "from below", "reflected in water", "at golden hour",
                "in silhouette", "cropped at the edge", "one light source", "in the rain",
                "under a spotlight", "backlit", "fog everywhere", "torn edges", "on graph paper",
                "as a stamp", "as a poster", "as a coin", "in three panels", "no faces",
                "hands only", "eyes only", "half in shadow", "seen through a window",
                "reflected twice", "at night", "at noon", "in motion blur", "frozen mid-air",
                "as a pattern", "one repeated shape", "no ground line", "floating", "upside down",
                "seen from far away", "made of type", "with a hidden object", "on a curve", "in a grid"
            ))
        ), now + 4))

        return decks to wheels
    }
}
