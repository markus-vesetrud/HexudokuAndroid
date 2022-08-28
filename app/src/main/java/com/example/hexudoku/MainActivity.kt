package com.example.hexudoku

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                    // BoardView(400f, BoardModel(null, null, 3))
                    MainContent()
                }
            }
        }
    }
}

@Composable
fun MainContent() {
    var viewState by remember { mutableStateOf("menu") }
    var boardModel: BoardModel? by remember {
        mutableStateOf(null)
    }

    val backToMenuFunction = { completed: Boolean ->
        if (completed) {
            boardModel = null
        }
        viewState = "menu"
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "Hexudoku", fontSize = 38.sp)
        when (viewState) {
            "board" -> {
                // Be aware! Here I assert that boardModel is not null, so ensure
                // the boardModel has an appropriate value before setting viewState = "board"
                BoardView(400f, boardModel!!, backToMenu = backToMenuFunction)
            }

            "menu" -> {
                Spacer(modifier = Modifier.height(50.dp))
                HexButton(onClick = { viewState = "howToPlay" }, text = "How to play")
                Spacer(modifier = Modifier.height(24.dp))
                HexButton(onClick = { viewState = "difficulty" }, text = "New Game")
                HexButton(onClick = { viewState = "board" }, text = "Continue", enabled = boardModel != null)
                Spacer(modifier = Modifier.height(24.dp))
                HexButton(onClick = { viewState = "about" }, text = "About")


            }

            "difficulty" -> {
                Spacer(modifier = Modifier.height(50.dp))
                HexButton(onClick = {
                    boardModel = BoardModel(null, null, 15)
                    viewState = "board"
                }, text = "Short")

                HexButton(onClick = {
                    boardModel = BoardModel(null, null, 25)
                    viewState = "board"
                }, text = "Medium")

                HexButton(onClick = {
                    boardModel = BoardModel(null, null, 35)
                    viewState = "board"
                }, text = "Long")

                HexButton(onClick = {
                    boardModel = BoardModel(null, null, 50)
                    viewState = "board"
                }, text = "Max")
                
                Spacer(modifier = Modifier.height(24.dp))

                HexButton(onClick = {
                    viewState = "menu"
                }, text = "Back")
            }
            
            "about" -> {
                Text(modifier = Modifier.width(200.dp), text =
                "This app showcases a variant of sudoku that makes use of hexagons instead of squares."
                )
            }
            
            "howToPlay" -> {
                // TODO Show examples of the board with lines along the "rows" and "columns"
                Text(text = "Work in progress")
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

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    HexSudokuTheme {

    }
}