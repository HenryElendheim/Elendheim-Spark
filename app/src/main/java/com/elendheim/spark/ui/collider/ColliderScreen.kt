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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AutoAwesome
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

/**
 * Wires the collide screen to its view-model and shows a small confirmation when
 * an idea is saved. This is what the navigation shell calls.
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
        onMutate = { vm.mutate() },
        onToggleLock = vm::toggleLock,
        onSave = {
            vm.saveCurrent(System.currentTimeMillis()) {
                Toast.makeText(context, "Saved to vault", Toast.LENGTH_SHORT).show()
            }
        },
        onSurprise = vm::surpriseMe,
        onRestoreHistory = vm::restoreFromHistory,
        onSetMixLimit = vm::setMixLimit
    )
}

/**
 * The home of the app: pick a deck, tap COLLIDE, and read the idea. Lock the
 * wheels you like, reroll or mutate the rest, and save the sparks.
 *
 * Accessibility is wired straight into this screen: the result is announced to
 * screen readers when it lands, motion can be switched off, tap targets can be
 * grown, wheel meaning always carries a name label, and haptics are optional.
 */
@Composable
fun ColliderScreen(
    state: ColliderUiState,
    onSelectDeck: (String) -> Unit,
    onCollide: () -> Unit,
    onMutate: () -> Unit,
    onToggleLock: (String) -> Unit,
    onSave: () -> Unit,
    onSurprise: () -> Unit,
    onRestoreHistory: (List<Pick>) -> Unit,
    onSetMixLimit: (Int) -> Unit
) {
    val palette = LocalSparkPalette.current
    val settings = state.settings
    val haptics = LocalHapticFeedback.current
    val view = LocalView.current

    // Larger-tap-target toggle grows the interactive controls for motor accessibility.
    val touch = if (settings.largerTapTargets) 64.dp else 48.dp

    // Overlays: the deck picker and the recents list are opened on demand so the
    // main screen stays calm and uncluttered.
    var showDeckPicker by remember { mutableStateOf(false) }
    var showRecents by remember { mutableStateOf(false) }

    val totalWheels = state.selectedDeck?.wheelIds?.size ?: 0

    // When a collision lands, optionally buzz and speak the result for TalkBack.
    LaunchedEffect(state.landedNonce) {
        if (state.landedNonce == 0L || state.current.isEmpty()) return@LaunchedEffect
        if (settings.haptics) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        if (settings.announceResult) view.announceForAccessibility(state.current.asSpoken())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // --- Top row: current deck (tap to switch) + how many to mix ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DeckPickerButton(
                deckName = state.selectedDeck?.name ?: "No deck",
                onClick = { showDeckPicker = true },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.size(10.dp))
            MixStepper(
                active = state.wheels.size,
                total = totalWheels,
                onLess = { onSetMixLimit((state.wheels.size - 1).coerceAtLeast(1)) },
                onMore = {
                    val next = state.wheels.size + 1
                    onSetMixLimit(if (next >= totalWheels) 0 else next)
                }
            )
        }

        Spacer(Modifier.size(14.dp))

        // --- Wheel chips (name + colour + lock) ---
        if (state.wheels.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.wheels.forEach { wheel ->
                    WheelChip(
                        wheel = wheel,
                        locked = wheel.name in state.lockedWheelNames,
                        colorblind = settings.colorblindPalette,
                        showLabel = settings.showWheelLabels,
                        touch = touch,
                        onToggleLock = { onToggleLock(wheel.name) }
                    )
                }
            }
        }

        // --- The result: the payoff, one pick per line and colour-coded ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            ResultDisplay(
                picks = state.current,
                landedNonce = state.landedNonce,
                settings = settings,
                colorFor = { name -> resultColor(name, state.wheels, settings.colorblindPalette, palette.accent) },
                onText = palette.onBackground,
                muted = palette.onSurfaceMuted
            )
        }

        // --- The controls ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mutate: reroll one wheel only.
            SecondaryAction(
                icon = Icons.Outlined.AutoAwesome,
                description = "Mutate: reroll one wheel only",
                enabled = state.current.isNotEmpty(),
                touch = touch,
                onClick = onMutate
            )
            // Save to vault.
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
            // Surprise me: random deck + collide.
            SecondaryAction(
                icon = Icons.Filled.Casino,
                description = "Surprise me: random deck and collide",
                enabled = state.decks.isNotEmpty(),
                touch = touch,
                onClick = onSurprise
            )

            Spacer(Modifier.weight(1f))

            // Recents: tucked behind a button so the screen stays calm.
            RecentsButton(
                count = state.history.size,
                enabled = state.history.isNotEmpty(),
                onClick = { showRecents = true }
            )
        }

        Spacer(Modifier.size(12.dp))

        // --- The big COLLIDE button ---
        CollideButton(
            label = if (state.current.isEmpty()) "COLLIDE" else "REROLL",
            enabled = state.wheels.isNotEmpty(),
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
            colorFor = { name -> resultColor(name, state.wheels, settings.colorblindPalette, palette.accent) },
            onRestore = { onRestoreHistory(it); showRecents = false },
            onDismiss = { showRecents = false }
        )
    }
}

/**
 * The current deck, shown as a single tappable button. Tapping it opens a
 * searchable list of decks -> no more fiddly swiping through a tab strip.
 */
@Composable
private fun DeckPickerButton(deckName: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val palette = LocalSparkPalette.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
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
        Icon(
            imageVector = Icons.Filled.ExpandMore,
            contentDescription = null,
            tint = palette.onSurfaceMuted,
            modifier = Modifier.size(20.dp)
        )
    }
}

/** A compact "how many wheels to mix" stepper. Shows active / total, or All. */
@Composable
private fun MixStepper(active: Int, total: Int, onLess: () -> Unit, onMore: () -> Unit) {
    val palette = LocalSparkPalette.current
    val label = if (total > 0 && active >= total) "All" else "$active/$total"
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(palette.surface)
            .border(1.dp, palette.outline, RoundedCornerShape(20.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .semantics { contentDescription = "Mixing $active of $total wheels" }
    ) {
        StepButton("Mix fewer wheels", enabled = active > 1, onClick = onLess) {
            Icon(Icons.Filled.Remove, contentDescription = null, tint = palette.onBackground, modifier = Modifier.size(18.dp))
        }
        Text(
            text = label,
            color = palette.onBackground,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        StepButton("Mix more wheels", enabled = total > 0 && active < total, onClick = onMore) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = palette.onBackground, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun StepButton(description: String, enabled: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .alpha(if (enabled) 1f else 0.35f)
            .clickable(enabled = enabled) { onClick() }
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) { content() }
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

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(palette.surface)
                .padding(20.dp)
        ) {
            Text("Choose a deck", color = palette.onBackground, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.size(12.dp))
            androidx.compose.material3.OutlinedTextField(
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

/** One wheel, shown as a coloured chip with its name label and a lock toggle. */
@Composable
private fun WheelChip(
    wheel: Wheel,
    locked: Boolean,
    colorblind: Boolean,
    showLabel: Boolean,
    touch: androidx.compose.ui.unit.Dp,
    onToggleLock: () -> Unit
) {
    val palette = LocalSparkPalette.current
    // In colourblind mode, remap the stored colour onto the higher-separation set.
    val chipColor = wheelColor(wheel.colorHex, colorblind)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .heightIn(min = touch)
            .clip(RoundedCornerShape(12.dp))
            .background(palette.surface)
            .border(
                width = if (locked) 2.dp else 1.dp,
                color = if (locked) palette.accent else palette.outline,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onToggleLock() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .semantics {
                contentDescription = if (locked) {
                    "${wheel.name} wheel, locked. Tap to unlock."
                } else {
                    "${wheel.name} wheel, unlocked. Tap to lock its pick."
                }
            }
    ) {
        // Colour dot -> a visual cue, but never the only one (label follows).
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(chipColor)
        )
        if (showLabel) {
            Text(text = wheel.name, color = palette.onBackground, fontSize = 14.sp)
        }
        Icon(
            imageVector = if (locked) Icons.Filled.Lock else Icons.Filled.LockOpen,
            contentDescription = null,   // described on the parent row
            tint = if (locked) palette.accent else palette.onSurfaceMuted,
            modifier = Modifier.size(16.dp)
        )
    }
}

/**
 * The result in the middle: one pick per line, each with its wheel's colour and
 * (optionally) name, generously spaced so it is easy to read instead of a wall
 * of overlapping text. Scrolls if a mix has many wheels.
 */
@Composable
private fun ResultDisplay(
    picks: List<Pick>,
    landedNonce: Long,
    settings: SparkSettings,
    colorFor: (String) -> Color,
    onText: Color,
    muted: Color
) {
    if (picks.isEmpty()) {
        Text(
            text = "Tap COLLIDE to spark an idea.",
            color = muted,
            fontSize = 18.sp,
            textAlign = TextAlign.Center
        )
        return
    }

    // A gentle settle animation on each landing, unless motion is reduced.
    // Keyed on the nonce so it replays every time a new idea lands.
    val scale = remember { Animatable(1f) }
    LaunchedEffect(landedNonce) {
        if (settings.reduceMotion) {
            scale.snapTo(1f)
        } else {
            scale.snapTo(0.92f)
            scale.animateTo(1f, animationSpec = tween(260))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .scale(scale.value)
            // One live region on the whole result so TalkBack reads the change.
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        picks.forEach { pick ->
            Column(modifier = Modifier.fillMaxWidth()) {
                // Colour is always shown (a dot); the name label is optional.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(colorFor(pick.wheelName))
                    )
                    if (settings.showWheelLabels) {
                        Text(
                            text = pick.wheelName,
                            color = colorFor(pick.wheelName),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Spacer(Modifier.size(4.dp))
                Text(
                    text = pick.text,
                    color = onText,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 30.sp
                )
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

/** The recents list: up to 30 past rolls, newest first. Tap one to bring it back. */
@Composable
private fun RecentsDialog(
    history: List<List<Pick>>,
    colorFor: (String) -> Color,
    onRestore: (List<Pick>) -> Unit,
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
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    history.forEach { picks ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(palette.surfaceElevated)
                                .clickable { onRestore(picks) }
                                .padding(12.dp)
                                .semantics { contentDescription = "Recent idea: ${picks.asSpoken()}. Tap to bring it back." }
                        ) {
                            Box(
                                Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(colorFor(picks.firstOrNull()?.wheelName ?: ""))
                            )
                            Spacer(Modifier.size(10.dp))
                            Text(
                                text = picks.asInline(),
                                color = palette.onBackground,
                                fontSize = 14.sp,
                                maxLines = 2,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.size(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Close", color = palette.onBackground) }
            }
        }
    }
}

/** A round secondary action button (mutate, save, surprise). */
@Composable
private fun SecondaryAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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

/** The signature soft-red COLLIDE / REROLL button. */
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
            .semantics { contentDescription = "$label. Collide the wheels into a new idea." },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color(0xFF1A0B0C),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Resolve a wheel's chip colour. In colourblind mode we map the stored hue onto
 * the higher-separation palette by its position; otherwise we use the stored
 * colour directly. Either way a name label is shown, so colour is never alone.
 */
private fun wheelColor(hex: String, colorblind: Boolean): Color {
    if (!colorblind) return hex.toColorOrDefault(Color(0xFFE0555A))
    // Map onto the colourblind-friendly list deterministically from the hex.
    val index = (hex.hashCode().and(0x7FFFFFFF)) % colorblindFriendlyWheelColors.size
    return colorblindFriendlyWheelColors[index].toColorOrDefault(Color(0xFFE0555A))
}

/**
 * The colour to use for a pick, found from its wheel. Falls back to the accent
 * when the wheel is not in the current mix (e.g. an older recents entry).
 */
private fun resultColor(name: String, wheels: List<Wheel>, colorblind: Boolean, fallback: Color): Color {
    val hex = wheels.firstOrNull { it.name == name }?.colorHex ?: return fallback
    return wheelColor(hex, colorblind)
}
