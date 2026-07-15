package com.elendheim.spark.ui.collider

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.ChangeHistory
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Square
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.window.Dialog
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elendheim.spark.model.Pick
import com.elendheim.spark.model.Wheel
import com.elendheim.spark.settings.SparkSettings
import com.elendheim.spark.ui.common.asInline
import com.elendheim.spark.ui.common.asSpoken
import com.elendheim.spark.ui.common.toColorOrDefault
import com.elendheim.spark.ui.theme.LocalSparkPalette
import com.elendheim.spark.ui.theme.colorblindFriendlyWheelColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Distinct shapes used in colourblind mode, so a wheel is told apart by its
// symbol, not only its colour.
private val wheelSymbols = listOf(
    Icons.Filled.Circle,
    Icons.Filled.Square,
    Icons.Filled.Star,
    Icons.Filled.Favorite,
    Icons.Filled.Bolt,
    Icons.Filled.ChangeHistory
)

/**
 * Wires the randomize screen to its view-model and shows a small confirmation
 * when an idea is saved.
 */
@Composable
fun ColliderRoute() {
    val vm: ColliderViewModel = viewModel(factory = ColliderViewModel.Factory)
    val state by vm.uiState.collectAsState()
    val context = LocalContext.current

    ColliderScreen(
        state = state,
        onSelectDeck = vm::selectDeck,
        onCollide = vm::collide,
        onToggleExclude = vm::toggleExclude,
        onSave = {
            vm.saveCurrent(System.currentTimeMillis()) {
                Toast.makeText(context, "Saved to vault", Toast.LENGTH_SHORT).show()
            }
        },
        onRandomDeck = vm::pickRandomDeck,
        onRestoreHistory = vm::restoreFromHistory,
        onClearHistory = vm::clearHistory,
        onRandomizePick = vm::randomizePick,
        onCustomPick = vm::setCustomPick
    )
}

/**
 * The home of the app: pick a deck, tap RANDOMIZE, and read the idea. Switch off
 * the wheels you don't want, tap a pick to change just that one, and save the
 * sparks. Rolls animate like a slot machine and settle with a satisfying click.
 */
@Composable
fun ColliderScreen(
    state: ColliderUiState,
    onSelectDeck: (String) -> Unit,
    onCollide: () -> Unit,
    onToggleExclude: (String) -> Unit,
    onSave: () -> Unit,
    onRandomDeck: () -> Unit,
    onRestoreHistory: (List<Pick>) -> Unit,
    onClearHistory: () -> Unit,
    onRandomizePick: (String) -> Unit,
    onCustomPick: (String, String) -> Unit
) {
    val palette = LocalSparkPalette.current
    val settings = state.settings
    val haptics = LocalHapticFeedback.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    val touch = if (settings.largerTapTargets) 64.dp else 48.dp

    var showDeckPicker by remember { mutableStateOf(false) }
    var showRecents by remember { mutableStateOf(false) }
    var editingPick by remember { mutableStateOf<Pick?>(null) }

    // Dice animation: while rolling, the deck name flickers through random decks
    // and the real answer is not shown until it settles.
    var diceDisplay by remember { mutableStateOf<String?>(null) }
    var diceRolling by remember { mutableStateOf(false) }
    var deckSettleNonce by remember { mutableStateOf(0L) }

    fun rollDice() {
        if (diceRolling) return
        val names = state.decks.map { it.name }
        if (names.size < 2 || settings.reduceMotion) { onRandomDeck(); return }
        scope.launch {
            diceRolling = true
            for (i in 0 until 14) {
                diceDisplay = names.random()
                delay(45L + i * 8L)   // slow down toward the end
            }
            onRandomDeck()            // now actually pick the deck (no auto-roll)
            diceDisplay = null        // reveal the real landed deck
            deckSettleNonce += 1      // trigger the settle bounce
            if (settings.haptics) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            diceRolling = false
        }
    }

    // When a result lands, optionally buzz and speak it for TalkBack.
    LaunchedEffect(state.landedNonce) {
        if (state.landedNonce == 0L || state.current.isEmpty()) return@LaunchedEffect
        if (settings.haptics) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        if (settings.announceResult) view.announceForAccessibility(state.current.asSpoken())
    }

    val deckLabel = diceDisplay ?: (state.selectedDeck?.name ?: "No deck")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // --- Top row: current deck (tap to switch) + the random-deck dice ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DeckPickerButton(
                deckName = deckLabel,
                settleNonce = deckSettleNonce,
                onClick = { if (!diceRolling) showDeckPicker = true },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.size(10.dp))
            DiceButton(enabled = state.decks.size > 1 && !diceRolling, onClick = { rollDice() })
        }

        Spacer(Modifier.size(14.dp))

        // --- Wheel chips: tap to switch a wheel off (it leaves the mix) ---
        if (state.wheels.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.wheels.forEachIndexed { index, wheel ->
                    WheelChip(
                        wheel = wheel,
                        index = index,
                        excluded = wheel.name in state.excludedWheelNames,
                        colorblind = settings.colorblindPalette,
                        showLabel = settings.showWheelLabels,
                        touch = touch,
                        onToggle = { onToggleExclude(wheel.name) }
                    )
                }
            }
        }

        // --- The result: the payoff, one pick per line and colour/symbol-coded ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            ResultDisplay(
                picks = state.current,
                wheels = state.wheels,
                landedNonce = state.landedNonce,
                settings = settings,
                onText = palette.onBackground,
                muted = palette.onSurfaceMuted,
                onPickTap = { editingPick = it }
            )
        }

        // --- Controls: save on the left, recents on the right ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SecondaryAction(
                icon = Icons.Outlined.BookmarkAdd,
                description = "Save this idea to the vault",
                enabled = state.current.isNotEmpty(),
                touch = touch,
                onClick = {
                    if (settings.haptics) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSave()
                }
            )
            Spacer(Modifier.weight(1f))
            RecentsButton(
                count = state.history.size,
                enabled = state.history.isNotEmpty(),
                onClick = { showRecents = true }
            )
        }

        Spacer(Modifier.size(12.dp))

        // --- The big RANDOMIZE button ---
        CollideButton(
            label = if (state.current.isEmpty()) "RANDOMIZE" else "AGAIN",
            enabled = state.wheels.any { it.name !in state.excludedWheelNames },
            accent = palette.accent,
            accentPressed = palette.accentPressed,
            tall = settings.largerTapTargets,
            onClick = onCollide
        )
    }

    // --- Overlays ---
    if (showDeckPicker) {
        DeckPickerDialog(
            decks = state.decks.map { it.id to it.name },
            selectedId = state.selectedDeck?.id,
            onPick = { onSelectDeck(it); showDeckPicker = false },
            onDismiss = { showDeckPicker = false }
        )
    }
    if (showRecents) {
        RecentsDialog(
            history = state.history,
            onRestore = { onRestoreHistory(it); showRecents = false },
            onClear = { onClearHistory(); showRecents = false },
            onDismiss = { showRecents = false }
        )
    }
    editingPick?.let { pick ->
        PickActionDialog(
            pick = pick,
            accent = palette.accent,
            onRandomize = { onRandomizePick(pick.wheelName); editingPick = null },
            onCustom = { text -> onCustomPick(pick.wheelName, text); editingPick = null },
            onDismiss = { editingPick = null }
        )
    }
}

/**
 * The current deck as a tappable button. During a dice roll the parent feeds it
 * flickering random names; when it settles it gives a small bounce.
 */
@Composable
private fun DeckPickerButton(
    deckName: String,
    settleNonce: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalSparkPalette.current
    val scale = remember { Animatable(1f) }
    LaunchedEffect(settleNonce) {
        if (settleNonce == 0L) return@LaunchedEffect
        scale.snapTo(1.08f)
        scale.animateTo(1f, animationSpec = tween(180))
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .scale(scale.value)
            .clip(RoundedCornerShape(20.dp))
            .background(palette.surfaceElevated)
            .border(1.dp, palette.outline, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .semantics { contentDescription = "Deck: $deckName. Tap to switch decks." }
    ) {
        Text(
            text = deckName,
            color = palette.onBackground,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = Modifier.weight(1f, fill = false)
        )
        Spacer(Modifier.size(6.dp))
        Icon(Icons.Filled.ExpandMore, contentDescription = null, tint = palette.onSurfaceMuted, modifier = Modifier.size(20.dp))
    }
}

/**
 * The random-deck dice, top-right. Jumps to a random deck (it does not roll the
 * result for you). Accent-ringed so it clearly means "shuffle to another deck".
 */
@Composable
private fun DiceButton(enabled: Boolean, onClick: () -> Unit) {
    val palette = LocalSparkPalette.current
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(palette.surfaceElevated)
            .border(1.5.dp, palette.accent, CircleShape)
            .alpha(if (enabled) 1f else 0.4f)
            .clickable(enabled = enabled) { onClick() }
            .semantics { contentDescription = "Random deck: jump to a random deck" },
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Filled.Casino, contentDescription = null, tint = palette.accent, modifier = Modifier.size(24.dp))
    }
}

/**
 * One wheel as a chip. Tap to switch it off: it greys out with a line through
 * its name so it is obvious it is no longer in the mix. No lock icon -> just the
 * name, plus a colour dot or (in colourblind mode) a distinct symbol.
 */
@Composable
private fun WheelChip(
    wheel: Wheel,
    index: Int,
    excluded: Boolean,
    colorblind: Boolean,
    showLabel: Boolean,
    touch: androidx.compose.ui.unit.Dp,
    onToggle: () -> Unit
) {
    val palette = LocalSparkPalette.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .heightIn(min = touch)
            .clip(RoundedCornerShape(12.dp))
            .background(palette.surface)
            .border(1.dp, palette.outline, RoundedCornerShape(12.dp))
            .alpha(if (excluded) 0.5f else 1f)
            .clickable { onToggle() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .semantics {
                contentDescription = if (excluded) {
                    "${wheel.name} wheel, switched off. Tap to include it."
                } else {
                    "${wheel.name} wheel, on. Tap to switch it off."
                }
            }
    ) {
        WheelGlyph(index = index, colorblind = colorblind, hex = wheel.colorHex, sizeDp = 12)
        if (showLabel) {
            Text(
                text = wheel.name,
                color = if (excluded) palette.onSurfaceMuted else palette.onBackground,
                fontSize = 14.sp,
                textDecoration = if (excluded) TextDecoration.LineThrough else TextDecoration.None
            )
        }
    }
}

/**
 * The result: one pick per line, colour/symbol-coded and generously spaced. Each
 * line slot-machines through random values then clicks into place, and tapping a
 * line opens its editor.
 */
@Composable
private fun ResultDisplay(
    picks: List<Pick>,
    wheels: List<Wheel>,
    landedNonce: Long,
    settings: SparkSettings,
    onText: Color,
    muted: Color,
    onPickTap: (Pick) -> Unit
) {
    if (picks.isEmpty()) {
        Text(
            text = "Tap RANDOMIZE to spark an idea.",
            color = muted,
            fontSize = 18.sp,
            textAlign = TextAlign.Center
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        picks.forEachIndexed { lineIndex, pick ->
            val wheelIndex = wheels.indexOfFirst { it.name == pick.wheelName }.coerceAtLeast(0)
            val candidates = wheels.firstOrNull { it.name == pick.wheelName }?.entries?.map { it.text } ?: emptyList()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onPickTap(pick) }
                    .padding(vertical = 4.dp)
                    .semantics { contentDescription = "${pick.wheelName}: ${pick.text}. Tap to change this one." }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WheelGlyph(index = wheelIndex, colorblind = settings.colorblindPalette, hex = candidateColor(wheels, pick.wheelName), sizeDp = 12)
                    if (settings.showWheelLabels) {
                        Text(
                            text = pick.wheelName,
                            color = glyphColor(wheelIndex, settings.colorblindPalette, candidateColor(wheels, pick.wheelName)),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Spacer(Modifier.size(4.dp))
                key(pick.wheelName) {
                    SlotPickText(
                        finalText = pick.text,
                        candidates = candidates,
                        landedNonce = landedNonce,
                        reduceMotion = settings.reduceMotion,
                        staggerIndex = lineIndex,
                        color = onText
                    )
                }
            }
        }
    }
}

/**
 * One result line that slot-machines through random candidate values before
 * settling on the final pick with a small "click" bounce. Honours reduce-motion.
 */
@Composable
private fun SlotPickText(
    finalText: String,
    candidates: List<String>,
    landedNonce: Long,
    reduceMotion: Boolean,
    staggerIndex: Int,
    color: Color
) {
    var shown by remember { mutableStateOf(finalText) }
    val scale = remember { Animatable(1f) }

    LaunchedEffect(landedNonce, finalText) {
        if (reduceMotion || candidates.size <= 1) {
            shown = finalText
            return@LaunchedEffect
        }
        // Lines settle one after another for a satisfying cascade.
        delay(staggerIndex * 90L)
        for (i in 0 until 10) {
            shown = candidates.random()
            delay(45L + i * 14L)   // ease-out: slower toward the end
        }
        shown = finalText
        scale.snapTo(1.12f)
        scale.animateTo(1f, animationSpec = tween(160))
    }

    Text(
        text = shown,
        color = color,
        fontSize = 26.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 30.sp,
        modifier = Modifier.scale(scale.value)
    )
}

/** A colour dot, or a distinct symbol in colourblind mode. */
@Composable
private fun WheelGlyph(index: Int, colorblind: Boolean, hex: String, sizeDp: Int) {
    if (colorblind) {
        Icon(
            imageVector = wheelSymbols[index % wheelSymbols.size],
            contentDescription = null,
            tint = glyphColor(index, true, hex),
            modifier = Modifier.size(sizeDp.dp)
        )
    } else {
        Box(
            Modifier
                .size(sizeDp.dp)
                .clip(CircleShape)
                .background(hex.toColorOrDefault(Color(0xFFE0555A)))
        )
    }
}

/** A searchable, scrollable deck chooser. */
@Composable
private fun DeckPickerDialog(
    decks: List<Pair<String, String>>,
    selectedId: String?,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalSparkPalette.current
    var query by remember { mutableStateOf("") }
    val filtered = decks.filter { it.second.contains(query.trim(), ignoreCase = true) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(palette.surface)
                .padding(20.dp)
        ) {
            Text("Choose a deck", color = palette.onBackground, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.size(12.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                placeholder = { Text("Search decks") },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Search your decks" }
            )
            Spacer(Modifier.size(10.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (filtered.isEmpty()) {
                    Text("No decks match.", color = palette.onSurfaceMuted, fontSize = 14.sp, modifier = Modifier.padding(8.dp))
                }
                filtered.forEach { (id, name) ->
                    val isSel = id == selectedId
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSel) palette.surfaceElevated else Color.Transparent)
                            .clickable { onPick(id) }
                            .padding(12.dp)
                            .semantics { contentDescription = if (isSel) "$name, current deck" else "Switch to $name" }
                    ) {
                        Text(name, color = palette.onBackground, fontSize = 16.sp, modifier = Modifier.weight(1f))
                        if (isSel) Icon(Icons.Filled.Check, contentDescription = null, tint = palette.accent, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

/** The "Recents" button. The list itself stays hidden until you ask for it. */
@Composable
private fun RecentsButton(count: Int, enabled: Boolean, onClick: () -> Unit) {
    val palette = LocalSparkPalette.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(palette.surfaceElevated)
            .border(1.dp, palette.outline, RoundedCornerShape(20.dp))
            .alpha(if (enabled) 1f else 0.4f)
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .semantics { contentDescription = "Recents. $count recent rolls. Tap to open." }
    ) {
        Icon(Icons.Outlined.History, contentDescription = null, tint = palette.onBackground, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(6.dp))
        Text(
            text = if (count > 0) "Recents ($count)" else "Recents",
            color = palette.onBackground,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/** The recents list: up to 30 past rolls. Tap one to bring it back, or empty the list. */
@Composable
private fun RecentsDialog(
    history: List<List<Pick>>,
    onRestore: (List<Pick>) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalSparkPalette.current
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(palette.surface)
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Recents", color = palette.onBackground, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text("${history.size}/30", color = palette.onSurfaceMuted, fontSize = 13.sp)
            }
            Spacer(Modifier.size(12.dp))
            if (history.isEmpty()) {
                Text("No recent rolls yet.", color = palette.onSurfaceMuted, fontSize = 14.sp, modifier = Modifier.padding(8.dp))
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    history.forEach { picks ->
                        Text(
                            text = picks.asInline(),
                            color = palette.onBackground,
                            fontSize = 14.sp,
                            maxLines = 2,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(palette.surfaceElevated)
                                .clickable { onRestore(picks) }
                                .padding(12.dp)
                                .semantics { contentDescription = "Recent idea: ${picks.asSpoken()}. Tap to bring it back." }
                        )
                    }
                }
            }
            Spacer(Modifier.size(12.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (history.isNotEmpty()) {
                    TextButton(onClick = onClear) { Text("Clear all", color = palette.accent) }
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text("Close", color = palette.onBackground) }
            }
        }
    }
}

/**
 * The per-pick editor: reroll just that one wheel, or type your own value in its
 * place. Tapping any line of the result opens this.
 */
@Composable
private fun PickActionDialog(
    pick: Pick,
    accent: Color,
    onRandomize: () -> Unit,
    onCustom: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalSparkPalette.current
    var custom by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf(pick.text) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(palette.surface)
                .padding(20.dp)
        ) {
            Text(pick.wheelName, color = accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.size(6.dp))
            Text(pick.text, color = palette.onBackground, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.size(16.dp))

            if (!custom) {
                PickOptionRow(icon = Icons.Filled.Casino, label = "Randomize this one", onClick = onRandomize)
                Spacer(Modifier.size(8.dp))
                PickOptionRow(icon = Icons.Filled.Edit, label = "Enter your own", onClick = { text = pick.text; custom = true })
                Spacer(Modifier.size(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = palette.onSurfaceMuted) }
                }
            } else {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    label = { Text("Your own value") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Type your own value for ${pick.wheelName}" }
                )
                Spacer(Modifier.size(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { custom = false }) { Text("Back", color = palette.onSurfaceMuted) }
                    TextButton(onClick = { onCustom(text) }, enabled = text.isNotBlank()) { Text("Use it", color = palette.accent) }
                }
            }
        }
    }
}

/** One tappable option row inside the per-pick editor. */
@Composable
private fun PickOptionRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    val palette = LocalSparkPalette.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(palette.surfaceElevated)
            .clickable { onClick() }
            .padding(14.dp)
            .semantics { contentDescription = label }
    ) {
        Icon(icon, contentDescription = null, tint = palette.accent, modifier = Modifier.size(20.dp))
        Text(label, color = palette.onBackground, fontSize = 16.sp)
    }
}

/** A round secondary action button (save). */
@Composable
private fun SecondaryAction(
    icon: ImageVector,
    description: String,
    enabled: Boolean,
    touch: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    val palette = LocalSparkPalette.current
    Box(
        modifier = Modifier
            .size(touch)
            .clip(CircleShape)
            .background(palette.surfaceElevated)
            .border(1.dp, palette.outline, CircleShape)
            .alpha(if (enabled) 1f else 0.4f)
            .clickable(enabled = enabled) { onClick() }
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = palette.onBackground)
    }
}

/** The signature soft-red RANDOMIZE button. */
@Composable
private fun CollideButton(
    label: String,
    enabled: Boolean,
    accent: Color,
    accentPressed: Color,
    tall: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (tall) 72.dp else 60.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) accent else accentPressed)
            .alpha(if (enabled) 1f else 0.5f)
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 18.dp)
            .semantics { contentDescription = "$label. Randomize the wheels into a new idea." },
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, color = Color(0xFF1A0B0C), fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

// --- Colour / symbol helpers ---

/** The stored colour hex for a wheel by name (falls back to the brand red). */
private fun candidateColor(wheels: List<Wheel>, name: String): String =
    wheels.firstOrNull { it.name == name }?.colorHex ?: "#E0555A"

/**
 * The tint for a glyph. In colourblind mode we use the higher-separation palette
 * by position; otherwise the wheel's own colour.
 */
private fun glyphColor(index: Int, colorblind: Boolean, hex: String): Color =
    if (colorblind) {
        colorblindFriendlyWheelColors[index % colorblindFriendlyWheelColors.size].toColorOrDefault(Color(0xFFE0555A))
    } else {
        hex.toColorOrDefault(Color(0xFFE0555A))
    }
