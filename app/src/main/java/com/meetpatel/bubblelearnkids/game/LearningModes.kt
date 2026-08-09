package com.meetpatel.bubblelearnkids.game

import androidx.compose.ui.graphics.Color

/**
 * The game can be played just for fun, or in one of a few gentle learning modes.
 * Every mode is the same forgiving toy underneath — nothing is ever "wrong",
 * popping any bubble simply says its name aloud, and matching the asked-for one
 * earns a little celebration.
 */
enum class GameMode { COLORS, NUMBERS, LETTERS, SHAPES, ANIMALS }

/** The words and colours each learning mode teaches. */
object LearningContent {

    data class NamedColor(val name: String, val color: Color)

    /** Bold, well-separated colours a young child can tell apart and name. */
    val colors = listOf(
        NamedColor("Red", Color(0xFFFF5252)),
        NamedColor("Orange", Color(0xFFFF9F43)),
        NamedColor("Yellow", Color(0xFFFFD32A)),
        NamedColor("Green", Color(0xFF3DDC97)),
        NamedColor("Blue", Color(0xFF54C7FC)),
        NamedColor("Purple", Color(0xFFA98BF0)),
        NamedColor("Pink", Color(0xFFFF7FB6)),
    )

    /** Counting one to twenty. */
    val numbers = (1..20).map { it.toString() }

    /** The whole alphabet, in order. */
    val letters = ('A'..'Z').map { it.toString() }

    /** A friendly word and picture for each letter — "A for Apple". Emoji come
     * from the phone's own font, so they add nothing to the app and work offline. */
    data class Word(val word: String, val emoji: String)

    val letterWords = mapOf(
        "A" to Word("Apple", "🍎"),
        "B" to Word("Ball", "⚽"),
        "C" to Word("Cat", "🐱"),
        "D" to Word("Dog", "🐶"),
        "E" to Word("Elephant", "🐘"),
        "F" to Word("Fish", "🐟"),
        "G" to Word("Grapes", "🍇"),
        "H" to Word("Hat", "🎩"),
        "I" to Word("Ice cream", "🍦"),
        "J" to Word("Juice", "🧃"),
        "K" to Word("Kite", "🪁"),
        "L" to Word("Lion", "🦁"),
        "M" to Word("Moon", "🌙"),
        "N" to Word("Nest", "🪺"),
        "O" to Word("Orange", "🍊"),
        "P" to Word("Pig", "🐷"),
        "Q" to Word("Queen", "👑"),
        "R" to Word("Rainbow", "🌈"),
        "S" to Word("Sun", "☀️"),
        "T" to Word("Tree", "🌳"),
        "U" to Word("Umbrella", "☂️"),
        "V" to Word("Van", "🚐"),
        "W" to Word("Watermelon", "🍉"),
        "X" to Word("Xylophone", "🎵"),
        "Y" to Word("Yo-yo", "🪀"),
        "Z" to Word("Zebra", "🦓"),
    )

    /** A little pile of dots for counting up a number. */
    fun numberEmoji(n: Int): String = "🔴".repeat(n.coerceIn(0, 20))

    /** The shapes to learn: a friendly name plus a glyph the phone's own font
     * renders — so shapes cost nothing and work offline like everything else. */
    data class Shape(val name: String, val glyph: String)

    val shapes = listOf(
        Shape("Circle", "●"),
        Shape("Square", "■"),
        Shape("Triangle", "▲"),
        Shape("Star", "★"),
        Shape("Heart", "♥"),
    )

    fun glyphFor(name: String): String = shapes.firstOrNull { it.name == name }?.glyph ?: name

    /** Friendly animals a small child already recognises, each with the picture
     * from the phone's own emoji font — no image files, still fully offline. */
    val animals = listOf(
        Shape("Cat", "🐱"),
        Shape("Dog", "🐶"),
        Shape("Cow", "🐮"),
        Shape("Pig", "🐷"),
        Shape("Duck", "🦆"),
        Shape("Rabbit", "🐰"),
        Shape("Bear", "🐻"),
        Shape("Lion", "🦁"),
        Shape("Monkey", "🐵"),
        Shape("Elephant", "🐘"),
        Shape("Frog", "🐸"),
        Shape("Fish", "🐟"),
        Shape("Bird", "🐦"),
        Shape("Horse", "🐴"),
    )

    fun animalFor(name: String): String = animals.firstOrNull { it.name == name }?.glyph ?: name

    /** The noise each animal makes. Spoken as the name, then the sound twice —
     * "Cow. Moo. Moo." — which is how a small child hears and copies it. */
    val animalSounds = mapOf(
        "Cat" to "Meow",
        "Dog" to "Woof",
        "Cow" to "Moo",
        "Pig" to "Oink",
        "Duck" to "Quack",
        "Rabbit" to "Squeak",
        "Bear" to "Grrr",
        "Lion" to "Roar",
        "Monkey" to "Ooh",
        "Elephant" to "Toot",
        "Frog" to "Ribbit",
        "Fish" to "Blub",
        "Bird" to "Tweet",
        "Horse" to "Neigh",
    )
}
