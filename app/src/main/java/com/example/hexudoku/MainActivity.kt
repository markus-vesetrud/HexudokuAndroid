package com.example.hexudoku

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.hexudoku.ui.theme.HexSudokuTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {

            // Settings:
            val (showHint, onShowHintChange) = remember {
                mutableStateOf(true)
            }
            val (showTimer, onShowTimerChange) = remember {
                mutableStateOf(false)
            }
            val themeOptions = listOf("dark", "light", "system")
            val (selectedTheme, onThemeSelect) = remember {
                mutableStateOf(themeOptions[2])
            }

            HexSudokuTheme(darkTheme = when(selectedTheme) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }) {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    MainContent(
                        showHint, onShowHintChange,
                        showTimer, onShowTimerChange,
                        themeOptions, selectedTheme, onThemeSelect
                    )
                }
            }
        }
    }
}

@Composable
fun MainContent(
    showHint: Boolean, onShowHintChange: (Boolean) -> Unit,
    showTimer: Boolean, onShowTimerChange: (Boolean) -> Unit,
    themeOptions: List<String>, selectedTheme: String, onThemeSelect: (String) -> Unit
) {
    LockScreenOrientation(orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
    val navController = rememberNavController()
    var boardModel: BoardModel? by remember {
        mutableStateOf(null)
    }
    var timeSpent by remember {
        mutableStateOf(0)
    }

    val backToMenuFunction = { completed: Boolean, ticks: Int ->
        timeSpent = ticks
        navController.navigate("main menu") {
            popUpTo("main menu") { inclusive = true }
        }
        if (completed) {
            boardModel = null
            timeSpent = 0
        }
    }

    
    NavHost(navController = navController, startDestination = "main menu") {
        composable("board") {
            CustomColumn {
                if (boardModel != null) {
                    BoardView(
                        // Using the screenWidth directly makes the app misbehave when the width > the height
                        // This has been fixed by disabling landscape mode
                        boardSize = LocalConfiguration.current.screenWidthDp.dp.value,
                        boardModel = boardModel!!,
                        backToMenu = backToMenuFunction,
                        showHint = showHint,
                        showTimer = showTimer,
                        timeSpent = timeSpent
                    )
                }

            }
        }
        composable("main menu") {
            CustomColumn {
                Spacer(modifier = Modifier.height(50.dp))
                HexButton(
                    onClick = { navController.navigate("difficulty menu") },
                    text = "New Game"
                )
                HexButton(
                    onClick = { navController.navigate("board") },
                    text = "Continue",
                    enabled = boardModel != null
                )
                Spacer(modifier = Modifier.height(24.dp))
                HexButton(onClick = {
                    navController.navigate("how to play") },
                    text = "How to play"
                )
                HexButton(
                    onClick = { navController.navigate("about") },
                    text = "About"
                )
                HexButton(
                    onClick = { navController.navigate("settings") },
                    text = "Settings"
                )
            }
        }
        composable("difficulty menu") {
            CustomColumn {
                Spacer(modifier = Modifier.height(50.dp))
                HexButton(
                    onClick = {
                        boardModel = BoardModel(null, null, 15, 1328)
                        navController.navigate("board")
                    },
                    text = "Short"
                )
                HexButton(
                    onClick = {
                        boardModel = BoardModel(null, null, 25, 1)
                        navController.navigate("board")
                    },
                    text = "Medium"
                )
                HexButton(
                    onClick = {
                        boardModel = BoardModel(null, null, 35, 1)
                        navController.navigate("board")
                    },
                    text = "Long"
                )
                HexButton(
                    onClick = {
                        boardModel = BoardModel(null, null, 50, 1)
                        navController.navigate("board")
                    },
                    text = "Max"
                )
                Spacer(modifier = Modifier.height(24.dp))
                HexButton(onClick = { navController.navigate("main menu") {
                    popUpTo("main menu") { inclusive = true }
                } }, text = "Back")
            }
        }
        composable("how to play") {
            CustomColumn {
                var tutorialNr by remember {
                    mutableStateOf(0)
                }
                if (tutorialNr == 0) {
                    Spacer(modifier = Modifier.height(50.dp))
                }
                Text(
                    modifier = Modifier.width(300.dp),
                    textAlign = TextAlign.Center,
                    text = when (tutorialNr) {
                        1 -> {stringResource(R.string.Tutorial1)}
                        2 -> {stringResource(R.string.Tutorial2)}
                        3 -> {stringResource(R.string.Tutorial3)}
                        4 -> {stringResource(R.string.Tutorial4)}
                        else -> {stringResource(R.string.Tutorial0)}
                    }
                )
                if (tutorialNr != 0) {
                    TutorialBoard(boardSize = LocalConfiguration.current.screenWidthDp.dp.value, step = tutorialNr)
                }

                Row {
                    HexButton(
                        onClick = {
                            if (tutorialNr == 0) {
                                navController.navigate("main menu") {
                                    popUpTo("main menu") { inclusive = true }
                                }
                            } else {
                                tutorialNr -= 1
                            }
                                  },
                        text = "Back"
                    )
                    HexButton(
                        onClick = {
                            if (tutorialNr == 4) {
                                navController.navigate("main menu") {
                                    popUpTo("main menu") { inclusive = true }
                                }
                            } else {
                                tutorialNr += 1
                            }
                        },
                        text = "Next"
                    )
                }
            }
        }
        composable("about") {
            CustomColumn {
                Spacer(modifier = Modifier.height(20.dp))
                Text(text = "About", fontSize = 24.sp)
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    modifier = Modifier.width(300.dp),
                    textAlign = TextAlign.Center,
                    text = stringResource(R.string.About)
                )
                Spacer(modifier = Modifier.height(24.dp))
                HexButton(onClick = { navController.navigate("main menu") {
                    popUpTo("main menu") { inclusive = true }
                } }, text = "Back")
            }
        }
        composable("settings") {
            CustomColumn {
                Spacer(modifier = Modifier.height(50.dp))
                Text(text = "Settings", fontSize = 24.sp)
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Show hint button")
                    Checkbox(checked = showHint, onCheckedChange = onShowHintChange)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Show timer")
                    Checkbox(checked = showTimer, onCheckedChange = onShowTimerChange)
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Dark mode select")
                    Column() {
                        themeOptions.forEach { text ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.selectable(
                                    selected = (text == selectedTheme),
                                    onClick = { onThemeSelect(text) }
                                )
                            ) {
                                RadioButton(
                                    selected = (text == selectedTheme),
                                    onClick = { onThemeSelect(text) }
                                )
                                Text(text = stringResource(when(text) {
                                    "light" -> R.string.setting_light
                                    "dark" -> R.string.setting_dark
                                    else -> R.string.setting_system
                                }))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                HexButton(onClick = { navController.navigate("main menu") {
                    popUpTo("main menu") { inclusive = true }
                } }, text = "Back")
            }
        }

    }
}

@Composable
fun HexButton(onClick: () -> Unit, text: String, enabled: Boolean = true) {
    Button(onClick = onClick, enabled = enabled, shape = CutCornerShape(50),
        modifier = Modifier
            .width(150.dp)
            .padding(4.dp)) {
        Text(text = text)
    }
}

@Composable
fun CustomColumn(modifier: Modifier = Modifier.fillMaxSize(), content: @Composable () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
        modifier = modifier
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = stringResource(R.string.app_name), fontSize = 40.sp)
        content()
    }
}

@Composable
fun LockScreenOrientation(orientation: Int) {
    fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

    val context = LocalContext.current
    DisposableEffect(Unit) {
        val activity = context.findActivity() ?: return@DisposableEffect onDispose {}
        val originalOrientation = activity.requestedOrientation
        activity.requestedOrientation = orientation
        onDispose {
            // restore original orientation when view disappears
            activity.requestedOrientation = originalOrientation
        }
    }
}