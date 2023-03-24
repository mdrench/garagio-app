package net.pelennor.garagio.ui

import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.MaterialTheme.colors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Brush.Companion.sweepGradient
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import net.pelennor.garagio.R
import net.pelennor.garagio.data.GaragioDoorState
import net.pelennor.garagio.ui.theme.GaragioTheme

@Composable
fun GaragioApp(
    garagioViewModel: GaragioViewModel,
    modifier: Modifier = Modifier,
) {
    val uiStatus by garagioViewModel.uiStatus.collectAsStateWithLifecycle()

    if (garagioViewModel.showAccountSettingsDialog)
        AccountSettingsDialog(
            garagioViewModel,
            onCancel = { garagioViewModel.cancelAccountSettings() },
            onSave = { garagioViewModel.trySaveAccountSettings() }
        )
    else if (garagioViewModel.showErrorDialog)
        ErrorDialog(
            garagioViewModel.savErrorDesc,
            garagioViewModel.savError,
            onDismiss = { garagioViewModel.closeErrorDialog() }
        )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    if (uiStatus.isError)
                        IconButton(
                            onClick = { garagioViewModel.openErrorDialog() },
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Error,
                                contentDescription = stringResource(R.string.error_button)
                            )
                        }
                    IconButton(
                        onClick = { garagioViewModel.openAccountSettings() },
                        enabled = garagioViewModel.enAccountSettings
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.settings_button)
                        )
                    }
                }
            )
        },
        backgroundColor = colors.background
    ) {
        Surface(
            modifier = modifier
                .fillMaxSize()
                .padding(it)
        ) {
            HomeScreen(uiStatus,
                { garagioViewModel.openDoor0() }, { garagioViewModel.closeDoor0() },
                { garagioViewModel.openDoor1() }, { garagioViewModel.closeDoor1() })
        }
    }
}

@Composable
fun ErrorDialog(
    @StringRes errorDesc: Int,
    error: String,
    onDismiss: ()->Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            backgroundColor = colors.primaryVariant,
            elevation = 8.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(8.dp),
            ) {
                // title
                Text(
                    text = stringResource(R.string.error),
                    style = MaterialTheme.typography.h5,
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .fillMaxWidth()
                )
                // error description and details
                Text(
                    text = stringResource(errorDesc),
                    modifier = Modifier
                        .padding(bottom = 4.dp)
                        .fillMaxWidth()
                )
                if (error.isNotBlank())
                    Text(
                        text = error,
                        modifier = Modifier.fillMaxWidth()
                    )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                ) {
                    Spacer(Modifier.weight(2.5f))
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(3f)
                    ) {
                        Text(stringResource(R.string.error_close_button))
                    }
                    Spacer(Modifier.weight(2.5f))
                }
            }
        }
    }
}

@Composable
fun AccountSettingsDialog(
    garagioViewModel: GaragioViewModel,
    onCancel: ()->Unit,
    onSave: ()->Unit,
) {
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(dismissOnClickOutside = false)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            backgroundColor = colors.primaryVariant,
            elevation = 8.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(8.dp),
            ) {
                val focusManager = LocalFocusManager.current
                val focusRequester = remember { FocusRequester() }
                var passwordVisible by rememberSaveable { mutableStateOf(false) }
                val passwordEditable = garagioViewModel.updAccountPassword != null

                // title
                Text(
                    text = stringResource(R.string.settings),
                    style = MaterialTheme.typography.h5,
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .fillMaxWidth()
                )
                // text input boxes
                OutlinedTextField(
                    modifier = Modifier
                        .padding(16.dp, 4.dp)
                        .fillMaxWidth(),
                    value = garagioViewModel.updAccountBaseUrl,
                    isError = garagioViewModel.errAccountBaseUrl,
                    onValueChange = {
                        garagioViewModel.updAccountBaseUrl = it
                        garagioViewModel.errAccountBaseUrl = false
                    },
                    label = { Text(stringResource(R.string.settings_base_url)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    )
                )
                OutlinedTextField(
                    modifier = Modifier
                        .padding(16.dp, 4.dp)
                        .fillMaxWidth(),
                    value = garagioViewModel.updAccountUsername,
                    onValueChange = { garagioViewModel.updAccountUsername = it },
                    label = { Text(stringResource(R.string.settings_username)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        autoCorrect = false,
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    )
                )
                LaunchedEffect(passwordEditable) { if (passwordEditable) focusRequester.requestFocus() }
                OutlinedTextField(
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .padding(16.dp, 4.dp)
                        .fillMaxWidth(),
                    value = garagioViewModel.updAccountPassword ?: "**********",
                    enabled = passwordEditable,
                    onValueChange = { garagioViewModel.updAccountPassword = it },
                    label = { Text(stringResource(R.string.settings_password)) },
                    singleLine = true,
                    visualTransformation = if (passwordVisible && passwordEditable) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    ),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        disabledTrailingIconColor = colors.onSurface.copy(alpha = TextFieldDefaults.IconOpacity),
                    ),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (passwordEditable)
                                    passwordVisible = !passwordVisible
                                else
                                    garagioViewModel.updAccountPassword = ""
                            }
                        ) {
                            Icon(
                                imageVector = if (!passwordEditable) Icons.Filled.Edit
                                    else if (passwordVisible) Icons.Filled.Visibility
                                    else Icons.Filled.VisibilityOff,
                                contentDescription = stringResource(
                                    if (!passwordEditable) R.string.settings_edit_password_button
                                    else if (passwordVisible) R.string.settings_hide_password_button
                                    else R.string.settings_show_password_button
                                )
                            )
                        }
                    }
                )
                // button bar
                Row(
                    modifier = Modifier
                        .padding(24.dp, 8.dp)
                        .fillMaxWidth()
                ) {
                    Button(
                        onClick = onCancel,
                        modifier = Modifier.weight(3f),
                    ) {
                        Text(stringResource(R.string.settings_cancel_button))
                    }
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = onSave,
                        modifier = Modifier.weight(3f),
                        colors = ButtonDefaults.buttonColors(backgroundColor = colors.secondary),
                    ) {
                        Text(stringResource(R.string.settings_save_button))
                    }
                }
            }
        }
    }
}

@Composable
fun HomeScreen(
    uiStatus: GaragioUiStatus,
    openDoor0: suspend ()->Boolean,
    closeDoor0: suspend ()->Boolean,
    openDoor1: suspend ()->Boolean,
    closeDoor1: suspend ()->Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(top = 8.dp)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        GarageDoorCard(R.string.garage_door_1, uiStatus.door0State, openDoor0, closeDoor0)
        GarageDoorCard(R.string.garage_door_2, uiStatus.door1State, openDoor1, closeDoor1)
        TemperatureCard(uiStatus.temperature)
    }
}

@Composable
fun GaragioCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = MaterialTheme.shapes.medium,
        backgroundColor = colors.primary,
        elevation = 8.dp,
    ) {
        content()
    }
}

enum class LongPressState { Blocked, Idle, Pressing, Pressed }

@Composable
fun GarageDoorCard(
    @StringRes name: Int,
    doorState: GaragioDoorState,
    openDoor: suspend ()->Boolean,
    closeDoor: suspend ()->Boolean,
    modifier: Modifier = Modifier
) {
    // alt text for door state
    val altText = when (doorState) {
        GaragioDoorState.Unknown -> R.string.garage_door_state_unknown
        GaragioDoorState.Closed -> R.string.garage_door_state_closed
        GaragioDoorState.Open -> R.string.garage_door_state_open
        GaragioDoorState.Closing -> R.string.garage_door_state_closing
        GaragioDoorState.Opening -> R.string.garage_door_state_opening
    }

    // door state icon animation
    val iconAnim = rememberInfiniteTransition()
    val iconAnimState by iconAnim.animateValue(
        initialValue = if (doorState == GaragioDoorState.Opening) 3 else 0,
        targetValue = if (doorState == GaragioDoorState.Closing) 3 else 0,
        typeConverter = Int.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    val painter = when (doorState) {
        GaragioDoorState.Unknown -> painterResource(R.drawable.garage_unknown_black)
        GaragioDoorState.Closed -> painterResource(R.drawable.garage_closed_black)
        GaragioDoorState.Open -> painterResource(R.drawable.garage_open_black)
        GaragioDoorState.Closing, GaragioDoorState.Opening -> when (iconAnimState) {
            0 -> painterResource(R.drawable.garage_25_black)
            1 -> painterResource(R.drawable.garage_50_black)
            else -> painterResource(R.drawable.garage_75_black)
        }
    }

    // really long press animation
    var pressed by remember { mutableStateOf(false) }
    var lpState by remember { mutableStateOf(LongPressState.Blocked) }
    val pressingAnim = remember { Animatable(0f) }
    val pressedAnim = animateColorAsState(
        if (lpState == LongPressState.Pressed) colors.primary else colors.primaryVariant,
        animationSpec = tween(250)
    ) {
        if (lpState == LongPressState.Pressed)
            lpState = when (doorState) {
                GaragioDoorState.Open, GaragioDoorState.Closed -> LongPressState.Idle
                else -> LongPressState.Blocked
            }
    }
    val brush = when (lpState) {
        LongPressState.Pressed -> SolidColor(pressedAnim.value)
        else -> Brush.horizontalGradient(
            0.0f to colors.primaryVariant,
            (pressingAnim.value-0.2f).coerceIn(0.0f, 1.0f) to colors.primaryVariant,
            (pressingAnim.value-0.1f).coerceIn(0.0f, 1.0f) to colors.secondary,
            (pressingAnim.value-0.0f).coerceIn(0.0f, 1.0f) to colors.primary,
            1.0f to colors.primary,
        )
    }
    val animModifier = Modifier
        .background(brush = brush)
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    if (lpState == LongPressState.Idle && !pressed) {
                        lpState = LongPressState.Pressing
                        lpState = if (tryAwaitRelease()
                            && lpState == LongPressState.Pressing
                            && pressingAnim.value == 1.2f
                        ) {
                            pressed = true
                            LongPressState.Pressed
                        } else
                            LongPressState.Idle
                    }
                }
            )
        }

    // command-in-progress animation
    val spinnerAnim = remember { Animatable(0f)}
    val angle = spinnerAnim.value.plus(0.75f).mod(1.0f)
    val iconBorder = if (!pressed) Modifier else
        Modifier.border(3.dp, if (angle<0.1)
            sweepGradient( // 0.0 <= value < 0.1 ; 1 >= alpha > 0
                      0.0f to colors.secondary.copy(alpha = 1-10*angle),
                angle+0.0f to colors.secondary,
                angle+0.1f to colors.secondary.copy(alpha = 0f),
                angle+0.9f to colors.secondary.copy(alpha = 0f),
                      1.0f to colors.secondary.copy(alpha = 1-10*angle),
            )
        else if (angle<=0.9)
            sweepGradient( // 0.1 <= value <= 0.9
                angle-0.1f to colors.secondary.copy(alpha = 0f),
                angle+0.0f to colors.secondary,
                angle+0.1f to colors.secondary.copy(alpha = 0f),
            )
        else
            sweepGradient( // 0.9 < value <= 1.0 ; 0 < alpha <= 1
                      0.0f to colors.secondary.copy(alpha = 10*angle-9),
                angle-0.9f to colors.secondary.copy(alpha = 0f),
                angle-0.1f to colors.secondary.copy(alpha = 0f),
                angle+0.0f to colors.secondary,
                      1.0f to colors.secondary.copy(alpha = 10*angle-9),
            )
            , RoundedCornerShape(8.dp))

    LaunchedEffect(lpState) {
        if (lpState == LongPressState.Pressing && pressingAnim.value<1.2f)
            pressingAnim.animateTo(1.2f, tween(2400 - (2000 * pressingAnim.value).toInt(), easing = EaseIn))
        else if (lpState == LongPressState.Idle && pressingAnim.value>0f)
            pressingAnim.animateTo(0f, tween((200 * pressingAnim.value).toInt(), easing = EaseOut))
        else if (lpState == LongPressState.Pressed)
            pressingAnim.snapTo(0f)
    }

    LaunchedEffect(doorState) {
        when (lpState) {
            LongPressState.Pressed -> Unit
            else -> lpState = when (doorState) {
                GaragioDoorState.Open, GaragioDoorState.Closed -> LongPressState.Idle
                else -> LongPressState.Blocked
            }
        }
    }

    val context = LocalContext.current

    LaunchedEffect(pressed) {
        if (pressed) {
            launch {
                spinnerAnim.animateTo(1.0f, infiniteRepeatable(tween(1_000, easing = LinearEasing)))
            }
            when (doorState) {
                GaragioDoorState.Open -> if (!closeDoor())
                    Toast.makeText(context, R.string.close_door_failed, Toast.LENGTH_LONG).show()
                GaragioDoorState.Closed -> if (!openDoor())
                    Toast.makeText(context, R.string.open_door_failed, Toast.LENGTH_LONG).show()
                else -> {}
            }
            pressed = false
            launch {
                spinnerAnim.snapTo(0f)
            }
        }
    }

    GaragioCard(modifier = modifier) {
        Row(
            modifier = animModifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(name),
                style = MaterialTheme.typography.h6,
                modifier = Modifier.padding(16.dp),
            )
            Spacer(Modifier.weight(1f))
            Image(
                painter = painter,
                contentDescription = stringResource(altText),
                colorFilter = ColorFilter.tint(colors.secondary),
                modifier = Modifier
                    .padding(16.dp, 8.dp)
                    .size(64.dp)
                    .then(iconBorder),
            )
        }
    }
}

@Composable
fun TemperatureCard(
    temperature: Float?,
    modifier: Modifier = Modifier
) {
    GaragioCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.temperature),
                style = MaterialTheme.typography.h6,
                modifier = Modifier.padding(16.dp),
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = if (temperature == null)
                    stringResource(R.string.temp_not_available)
                else
                    stringResource(R.string.degrees_f, temperature),
                color = colors.secondary,
                style = MaterialTheme.typography.h4,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    val status = GaragioUiStatus(GaragioDoorState.Closed, GaragioDoorState.Open, -3.1415f)

    GaragioTheme {
        HomeScreen(status, { false }, { false }, { false }, { false })
    }
}

@Preview(showBackground = true)
@Composable
fun ErrorDialogPreview() {
    GaragioTheme {
        ErrorDialog(R.string.error_other_error_desc, "This is a composable preview only, with no real functionality.", onDismiss = {})
    }
}
