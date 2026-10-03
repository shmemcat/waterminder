package io.github.shmemcat.waterminder.ui

import android.app.TimePickerDialog
import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.shmemcat.waterminder.WaterminderViewModel
import io.github.shmemcat.waterminder.reminders.ReminderPolicy
import io.github.shmemcat.waterminder.reminders.ReminderSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun WaterminderApp(model: WaterminderViewModel, onAllowNotifications: () -> Unit, onNotificationSettings: () -> Unit) {
    WaterminderTheme {
        val palette = LocalPalette.current
        var settingsOpen by rememberSaveable { mutableStateOf(false) }
        val snackbar = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        BackHandler(settingsOpen) { settingsOpen = false }
        Scaffold(containerColor = palette.Paper, snackbarHost = { SnackbarHost(snackbar) }, contentWindowInsets = WindowInsets.safeDrawing) { inset ->
            Box(Modifier.fillMaxSize().padding(inset), contentAlignment = Alignment.TopCenter) {
                Column(Modifier.widthIn(max = 480.dp).fillMaxSize().padding(horizontal = 24.dp)) {
                    Header(settingsOpen, onBack = { settingsOpen = false }, onSettings = { settingsOpen = true })
                    if (settingsOpen) {
                        SettingsScreen(model.settings, model.notificationsAllowed, model::update, onAllowNotifications, onNotificationSettings, onTest = {
                            val sent = model.testReminder()
                            scope.launch {
                                snackbar.showSnackbar(if (sent) "Test nudge in 10 seconds. Lock your screen to try it." else if (ReminderPolicy.isQuiet(Instant.now(), model.settings, ZoneId.systemDefault())) "Shh, it's quiet hours. Try again after they end." else "Allow notifications first, then try again.")
                            }
                        }, onInvalidTime = { scope.launch { snackbar.showSnackbar("Choose different start and end times.") } })
                    } else {
                        HomeScreen(model, onAllowNotifications)
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(settings: Boolean, onBack: () -> Unit, onSettings: () -> Unit) {
    val palette = LocalPalette.current
    Row(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 12.dp).heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        if (settings) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp).offset(x = (-12).dp)) {
                DoodleIcon(Doodle.Back, Modifier.size(24.dp).semantics { contentDescription = "Back to plant" })
            }
            Text("your rhythm", fontSize = 20.sp, fontWeight = FontWeight.Medium)
        } else {
            DoodleIcon(Doodle.Drop, Modifier.size(29.dp))
            Spacer(Modifier.width(9.dp))
            Text("waterminder", fontSize = 22.sp, fontWeight = FontWeight.Medium, letterSpacing = (-.6).sp)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onSettings, modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(palette.SettingsSurface)) {
                DoodleIcon(Doodle.Settings, Modifier.size(24.dp).semantics { contentDescription = "Reminder settings" })
            }
        }
    }
}

@Composable
private fun HomeScreen(model: WaterminderViewModel, onAllowNotifications: () -> Unit) {
    val palette = LocalPalette.current
    val haptics = LocalHapticFeedback.current
    val watering = remember { Animatable(1f) }
    var happy by remember { mutableStateOf(false) }
    var now by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(Unit) { while (true) { now = Instant.now(); delay(30_000) } }
    LaunchedEffect(model.wateringEvent) {
        if (model.wateringEvent > 0) {
            happy = true
            watering.snapTo(0f)
            watering.animateTo(1f, tween(2100))
            delay(2500)
            happy = false
        }
    }
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(24.dp))
        Text(if (happy) "a sip for you.\na splash for your plant." else "a little water.\na little better.", fontFamily = FontFamily.Serif, fontSize = 34.sp, lineHeight = 40.sp, letterSpacing = (-.8).sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(if (happy) "Look at you, taking care of yourself." else "Small sips. A softer kind of habit.", color = palette.Muted, fontSize = 14.sp, textAlign = TextAlign.Center)
        PlantIllustration(watering.value, Modifier.fillMaxWidth().height(267.dp))
        Text(if (happy) "a little happier already" else "you grow at your own pace", color = palette.Muted, fontSize = 13.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(27.dp))
        ReminderCard(model.settings, model.notificationsAllowed, now, onToggle = { enabled ->
            if (enabled && !model.notificationsAllowed) onAllowNotifications() else model.update(model.settings.copy(enabled = enabled))
        })
        Spacer(Modifier.height(16.dp))
        Button(onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
            model.drink()
        }, modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp), shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = palette.Sage, contentColor = Palette.Ink)) {
            DoodleIcon(Doodle.Glass, Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Text(if (happy) "You & your plant say thanks" else "I drank water", fontSize = 16.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(16.dp))
        Text("just a gentle nudge. no keeping score.", color = palette.Muted, fontSize = 12.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ReminderCard(settings: ReminderSettings, allowed: Boolean, now: Instant, onToggle: (Boolean) -> Unit) {
    val palette = LocalPalette.current
    val context = LocalContext.current
    val active = settings.enabled && allowed && settings.nextAt > 0
    val quiet = ReminderPolicy.isQuiet(now, settings, ZoneId.systemDefault())
    val label = when {
        settings.enabled && !allowed -> "Notifications need permission"
        !active -> "Reminders are off"
        quiet -> "Resting during quiet hours"
        else -> "Next little nudge"
    }
    val value = if (active) {
        val time = Instant.ofEpochMilli(settings.nextAt).atZone(ZoneId.systemDefault())
        val date = now.atZone(ZoneId.systemDefault()).toLocalDate()
        val prefix = if (time.toLocalDate() > date) "tomorrow, " else "around "
        prefix + time.toLocalTime().format(timeFormat(DateFormat.is24HourFormat(context)))
    } else if (settings.enabled && !allowed) "Allow your little nudges" else "Start your rhythm"
    Surface(color = palette.Card, shape = RoundedCornerShape(22.dp), border = BorderStroke(1.dp, palette.Line), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DoodleIcon(if (quiet && active) Doodle.Moon else Doodle.Clock, Modifier.size(18.dp))
                        Spacer(Modifier.width(7.dp))
                        Text(label, fontSize = 12.sp, color = palette.Muted)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(value, fontSize = 20.sp, fontWeight = FontWeight.Medium)
                }
                Switch(checked = settings.enabled && allowed, onCheckedChange = onToggle, modifier = Modifier.semantics { contentDescription = "Enable water reminders" }, colors = switchColors())
            }
            Spacer(Modifier.height(9.dp))
            Text("every ${intervalLabel(settings.intervalMinutes)}" + if (settings.quietEnabled) " · quiet ${shortTime(settings.quietStart)}–${shortTime(settings.quietEnd)}" else " · quiet hours off", fontSize = 12.sp, color = palette.Muted)
        }
    }
}



@Composable
private fun SettingsScreen(settings: ReminderSettings, notificationsAllowed: Boolean, onChange: (ReminderSettings) -> Unit, onAllow: () -> Unit, onNotificationSettings: () -> Unit, onTest: () -> Unit, onInvalidTime: () -> Unit) {
    val palette = LocalPalette.current
    val context = LocalContext.current
    var custom by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(top = 22.dp, bottom = 30.dp)) {
        Text("Make it your rhythm.", fontFamily = FontFamily.Serif, fontSize = 30.sp, letterSpacing = (-.7).sp)
        Spacer(Modifier.height(8.dp))
        Text("A few small settings. Then go live your day.", color = palette.Muted, fontSize = 14.sp)
        Spacer(Modifier.height(34.dp))
        SectionTitle(Doodle.Clock, "Remind me every")
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(60, 120, 180, 240).forEach { minutes ->
                val selected = settings.intervalMinutes == minutes
                OutlinedButton(onClick = { onChange(settings.copy(intervalMinutes = minutes)) }, modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(14.dp), contentPadding = PaddingValues(0.dp), border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) palette.Ink else palette.Line), colors = ButtonDefaults.outlinedButtonColors(containerColor = if (selected) palette.SelectionSurface else palette.Paper, contentColor = palette.Ink)) {
                    Text("${minutes / 60} hr", fontSize = 15.sp, fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal)
                }
            }
        }
        TextButton(onClick = { custom = true }, contentPadding = PaddingValues(horizontal = 0.dp), modifier = Modifier.heightIn(min = 48.dp)) {
            Text(if (settings.intervalMinutes in listOf(60,120,180,240)) "Or choose your own interval" else "Custom interval: ${intervalLabel(settings.intervalMinutes)}", color = palette.Muted, fontSize = 13.sp)
        }
        DividerLine()
        SettingToggle(Doodle.Moon, "Quiet hours", "Let you and your plant rest.", settings.quietEnabled) { onChange(settings.copy(quietEnabled = it)) }
        if (settings.quietEnabled) {
            Row(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 23.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TimeButton("From", settings.quietStart, Modifier.weight(1f)) {
                    TimePickerDialog(context, { _, hour, minute ->
                        val value = hour * 60 + minute
                        if (value == settings.quietEnd) onInvalidTime() else onChange(settings.copy(quietStart = value))
                    }, settings.quietStart / 60, settings.quietStart % 60, DateFormat.is24HourFormat(context)).show()
                }
                TimeButton("Until", settings.quietEnd, Modifier.weight(1f)) {
                    TimePickerDialog(context, { _, hour, minute ->
                        val value = hour * 60 + minute
                        if (value == settings.quietStart) onInvalidTime() else onChange(settings.copy(quietEnd = value))
                    }, settings.quietEnd / 60, settings.quietEnd % 60, DateFormat.is24HourFormat(context)).show()
                }
            }
        }
        DividerLine()
        SettingToggle(Doodle.Sun, "Light up my screen", "A little hello, even when it's locked.", settings.wakeScreen) { onChange(settings.copy(wakeScreen = it)) }
        Text("Screen wake depends on your phone. Keep lock-screen notifications enabled in Android settings.", fontSize = 12.sp, lineHeight = 18.sp, color = palette.Muted, modifier = Modifier.padding(start = 36.dp, bottom = 23.dp))
        DividerLine()
        Spacer(Modifier.height(22.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            DoodleIcon(Doodle.Bell, Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(if (notificationsAllowed) "Your nudges are allowed" else "Allow a little nudge", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text(if (notificationsAllowed) "Android notification settings" else "Waterminder needs notification permission.", fontSize = 12.sp, color = palette.Muted, modifier = Modifier.padding(top = 4.dp))
            }
        }
        TextButton(onClick = if (notificationsAllowed) onNotificationSettings else onAllow, contentPadding = PaddingValues(start = 36.dp)) { Text(if (notificationsAllowed) "Open notification settings" else "Allow notifications", fontSize = 13.sp) }
        OutlinedButton(onClick = onTest, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp).padding(top = 2.dp), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, palette.Line)) {
            Text("Send a test nudge", fontSize = 14.sp)
        }
        Spacer(Modifier.height(22.dp))
        Text("Reminders are approximate and may arrive a little later while your phone saves battery. Quiet hours always stay quiet.", fontSize = 12.sp, lineHeight = 18.sp, color = palette.Muted)
    }
    if (custom) {
        var minutes by remember { mutableFloatStateOf(settings.intervalMinutes.toFloat()) }
        AlertDialog(onDismissRequest = { custom = false }, title = { Text("Your own rhythm") }, text = {
            Column {
                Text("Every ${intervalLabel(minutes.roundToInt())}", fontSize = 20.sp)
                Slider(value = minutes, onValueChange = { minutes = (it / 15).roundToInt() * 15f }, valueRange = 30f..480f, steps = 29, modifier = Modifier.semantics { contentDescription = "Reminder interval in minutes" })
                Text("30 minutes to 8 hours", color = palette.Muted, fontSize = 13.sp)
            }
        }, confirmButton = { TextButton(onClick = { onChange(settings.copy(intervalMinutes = minutes.roundToInt())); custom = false }) { Text("Set interval") } }, dismissButton = { TextButton(onClick = { custom = false }) { Text("Cancel") } }, containerColor = palette.Paper)
    }
}

@Composable
private fun SettingToggle(icon: Doodle, title: String, subtitle: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    val palette = LocalPalette.current
    Row(Modifier.fillMaxWidth().padding(vertical = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        DoodleIcon(icon, Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = palette.Muted, fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 5.dp))
        }
        Spacer(Modifier.width(6.dp))
        Switch(checked = checked, onCheckedChange = onToggle, modifier = Modifier.semantics { contentDescription = title }, colors = switchColors())
    }
}

@Composable
private fun switchColors(): SwitchColors {
    val palette = LocalPalette.current
    return SwitchDefaults.colors(checkedThumbColor = palette.ToggleThumb, checkedTrackColor = palette.ToggleTrack, checkedBorderColor = palette.ToggleTrack, uncheckedTrackColor = palette.Line, uncheckedThumbColor = palette.IdleThumb, uncheckedBorderColor = palette.Line)
}

@Composable
private fun SectionTitle(icon: Doodle, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        DoodleIcon(icon, Modifier.size(24.dp)); Spacer(Modifier.width(12.dp))
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun TimeButton(label: String, minutes: Int, modifier: Modifier, onClick: () -> Unit) {
    val palette = LocalPalette.current
    Surface(modifier.clip(RoundedCornerShape(16.dp)).clickable(role = Role.Button, onClick = onClick), color = palette.Card, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, palette.Line)) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 13.dp)) {
            Text(label, color = palette.Muted, fontSize = 12.sp)
            Text(LocalTime.ofSecondOfDay(minutes * 60L).format(timeFormat(DateFormat.is24HourFormat(LocalContext.current))), fontSize = 20.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun DividerLine() = HorizontalDivider(color = LocalPalette.current.Line)

private fun timeFormat(is24: Boolean): DateTimeFormatter = DateTimeFormatter.ofPattern(if (is24) "HH:mm" else "h:mm a")
private fun shortTime(minutes: Int): String = LocalTime.ofSecondOfDay(minutes * 60L).format(DateTimeFormatter.ofPattern(if (minutes % 60 == 0) "h a" else "h:mm a"))
fun intervalLabel(minutes: Int): String = when {
    minutes < 60 -> "$minutes min"
    minutes % 60 == 0 -> "${minutes / 60} ${if (minutes == 60) "hour" else "hours"}"
    else -> "${minutes / 60} hr ${minutes % 60} min"
}

