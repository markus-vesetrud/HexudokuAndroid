package com.example.hexudoku

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
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
            HexSudokuTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    MainContent()
                }
            }
        }
    }
}

@Composable
fun MainContent() {
    LockScreenOrientation(orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
    val navController = rememberNavController()
    var boardModel: BoardModel? by remember {
        mutableStateOf(null)
    }

    val backToMenuFunction = { completed: Boolean ->
        if (completed) {
            boardModel = null
        }
        navController.navigate("main menu") {
            popUpTo("main menu") { inclusive = true }
        }
    }
    
    NavHost(navController = navController, startDestination = "main menu") {
        composable("board") {
            // Be aware! Here I assert that boardModel is not null, so ensure
            // the boardModel has an appropriate value before navigating to board
            if (boardModel == null) {
                navController.navigate("main menu") {
                    popUpTo("main menu") { inclusive = true }
                }
            }
            CustomColumn {
                BoardView(
                    // Using the screenWidth directly makes the app misbehave when the width > the height
                    // This has been fixed by disabling landscape mode
                    boardSize = LocalConfiguration.current.screenWidthDp.dp.value,
                    boardModel = boardModel!!,
                    backToMenu = backToMenuFunction)
            }
        }
        composable("main menu") {
            CustomColumn {
                Spacer(modifier = Modifier.height(50.dp))
                HexButton(onClick = {
                    navController.navigate("difficulty menu") },
                    text = "How to play"
                )
                Spacer(modifier = Modifier.height(24.dp))
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
                HexButton(
                    onClick = { navController.navigate("about menu") },
                    text = "About"
                )
            }
        }
        composable("difficulty menu") {
            CustomColumn {
                Spacer(modifier = Modifier.height(50.dp))
                HexButton(
                    onClick = {
                        boardModel = BoardModel(null, null, 15)
                        navController.navigate("board")
                    },
                    text = "Short"
                )

                HexButton(
                    onClick = {
                        boardModel = BoardModel(null, null, 25)
                        navController.navigate("board")
                    },
                    text = "Medium"
                )

                HexButton(
                    onClick = {
                        boardModel = BoardModel(null, null, 35)
                        navController.navigate("board")
                    },
                    text = "Long"
                )

                HexButton(
                    onClick = {
                        boardModel = BoardModel(null, null, 50)
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
        composable("about") {
            CustomColumn {
                Text(
                    modifier = Modifier.width(200.dp),
                    textAlign = TextAlign.Center,
                    text = "This app showcases a variant of sudoku that makes use of hexagons instead of squares."
                )
                HexButton(onClick = { navController.navigate("main menu") {
                    popUpTo("main menu") { inclusive = true }
                } }, text = "Back")
            }
        }
        composable("how to play") {
            CustomColumn {
                // TODO Show examples of the board with lines along the "rows" and "columns"
                Text(text = "Work in progress")
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
        Text(text = "Hexudoku", fontSize = 40.sp)
        content()
    }
}

@Composable
fun LockScreenOrientation(orientation: Int) {
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

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}