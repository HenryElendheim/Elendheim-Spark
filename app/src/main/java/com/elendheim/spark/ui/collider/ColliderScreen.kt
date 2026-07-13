package com.elendheim.spark.ui.collider

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.elendheim.spark.ui.common.asLines
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
        onRestoreHistory = vm::restoreFromHistory
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
    onRestoreHistory: (List<Pick>) -> Unit
) {
    val palette = LocalSparkPalette.current
    val settings = state.settings
    val haptics = LocalHapticFeedback.current
    val view = LocalView.current

    // Larger-tap-target toggle grows the interactive controls for motor accessibility.
    val touch = if (settings.largerTapTargets) 64.dp else 48.dp

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
        // --- Deck switcher ---
        DeckSwitcher(
            decks = state.decks.map { it.id to it.name },
            selectedId = state.selectedDeck?.id,
            onSelect = onSelectDeck
        )

        Spacer(Modifier.size(16.dp))

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

        // --- The result: the payoff ---
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
                accent = palette.accent,
                onText = palette.onBackground,
                muted = palette.onSurfaceMuted
            )
        }

        // --- History strip: never lose a good one ---
        if (state.history.isNotEmpty()) {
            HistoryStrip(
                history = state.history,
                muted = palette.onSurfaceMuted,
                surface = palette.surfaceElevated,
                onRestore = onRestoreHistory
            )
            Spacer(Modifier.size(12.dp))
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
}

/** A horizontally scrolling row of deck chips at the top of the screen. */
@Composable
private fun DeckSwitcher(
    decks: List<Pair<String, String>>,
    selectedId: String?,
    onSelect: (String) -> Unit
) {
    val palette = LocalSparkPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        decks.forEach { (id, name) ->
            val selected = id == selectedId
            Text(
                text = name,
                color = if (selected) palette.onBackground else palette.onSurfaceMuted,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (selected) palette.surfaceElevated else Color.Transparent)
                    .border(
                        width = if (selected) 1.dp else 0.dp,
                        color = if (selected) palette.accent else Color.Transparent,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clickable { onSelect(id) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .semantics {
                        contentDescription = if (selected) "Deck $name, selected" else "Switch to deck $name"
                    }
            )
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

/** The big result in the middle: inline "A x B x C" or stacked line-by-line. */
@Composable
private fun ResultDisplay(
    picks: List<Pick>,
    landedNonce: Long,
    settings: SparkSettings,
    accent: Color,
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

    // Mark this as a live region so TalkBack notices the change too.
    val liveModifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }

    if (settings.lineByLineResult) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = liveModifier
        ) {
            picks.asLines().forEachIndexed { index, line ->
                Text(
                    text = line,
                    color = if (index % 2 == 0) onText else accent,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.scale(scale.value)
                )
            }
        }
    } else {
        Text(
            text = picks.asInline(),
            color = onText,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = liveModifier.scale(scale.value)
        )
    }
}

/** The recent-rolls strip: tap any to bring it back so it is never lost. */
@Composable
private fun HistoryStrip(
    history: List<List<Pick>>,
    muted: Color,
    surface: Color,
    onRestore: (List<Pick>) -> Unit
) {
    Column {
        Text(text = "Recent", color = muted, fontSize = 12.sp)
        Spacer(Modifier.size(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            history.take(20).forEach { picks ->
                Text(
                    text = picks.asInline(),
                    color = muted,
                    fontSize = 13.sp,
                    maxLines = 1,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(surface)
                        .clickable { onRestore(picks) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .semantics { contentDescription = "Recent idea: ${picks.asSpoken()}. Tap to bring back." }
                )
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
