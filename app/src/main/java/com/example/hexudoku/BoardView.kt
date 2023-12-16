package com.example.hexudoku

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hexudoku.ui.theme.primaryVariant2
import kotlinx.coroutines.delay
import kotlin.math.pow
import kotlin.math.sqrt

internal fun scaleX(x: Float, hexSize: Float): Float {
    return x * hexSize * sqrt(3f) / 2f
}
internal fun scaleX(x: Int, hexSize: Float): Float {
    return scaleX(x.toFloat(), hexSize)
}

internal fun scaleY(y: Float, hexSize: Float): Float {
    return y * hexSize * 1.5f
}
internal fun scaleY(y: Int, hexSize: Float): Float {
    return scaleY(y.toFloat(), hexSize)
}

private fun calculateDistance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
    return sqrt((x1-x2).pow(2) + (y1-y2).pow(2))
}

internal fun stateToText(x: Int): String {
    return if (x <= 0 || x >= 8) { "" } else { x.toString() }
}

internal fun colorToHex(color: Color): Int {
    val alpha = (color.alpha*255).toInt()
    val red = (color.red*255).toInt()
    val green = (color.green*255).toInt()
    val blue = (color.blue*255).toInt()
    return alpha*0x1000000 + red*0x10000 + green*0x100 + blue
}

@Composable
internal fun colorArray(): Array<Color> {
    return arrayOf(
        MaterialTheme.colors.primary,
        MaterialTheme.colors.primaryVariant2,
        MaterialTheme.colors.primaryVariant,
        MaterialTheme.colors.primaryVariant2,
        MaterialTheme.colors.primaryVariant,
        MaterialTheme.colors.primaryVariant2,
        MaterialTheme.colors.primaryVariant
    )
}

@Composable
internal fun BoardView(boardSize: Float, boardModel: BoardModel?, backToMenu: (Boolean, Int) -> Unit, showHint: Boolean, showTimer: Boolean, timeSpent: Int) {
    if (boardModel == null) {
        backToMenu(false, 0)
        return
    }
    /*
    * A recomposition of the entire composable will trigger another initialization of the
    * boardModel, rather than keeping the old one. This will result in the board becoming scrambled.
    * To avoid this I added a Surface to wrap the content of this composable, so that only the
    * content inside the surface is recomposed, not the entire composable.
    */

    val hexChangeable = mutableMapOf<String, Boolean>()
    val hexTextColor = mutableMapOf<String, MutableState<Int>>()
    val board = mutableMapOf<String, MutableState<Int>>()
    val textColor: Int = colorToHex(MaterialTheme.colors.onSurface)
    val errorColor: Int = colorToHex(MaterialTheme.colors.error)
    for (hexID in BoardModel.hexIDArray) {

        hexChangeable[hexID] = (boardModel.board[hexID]!! == 0)
        hexTextColor[hexID] = remember {
            mutableStateOf(textColor)
        }

        board[hexID] = if (hexChangeable[hexID]!!) {
            remember { mutableStateOf(0) }
        } else {
            remember { mutableStateOf(boardModel.board[hexID]!!) }
        }
    }

    // hexSize is the length from the center of a hex to a corner
    // The same as the side length of the hex
    val hexSize: Float = (boardSize - 20f)/(sqrt(3f)*8f)

    val colorArray = colorArray()

    var completed by remember {
        mutableStateOf(false)
    }

    val clusterCenterIDs = arrayOf("0:0", "-1:-3", "4:-2", "5:1", "1:3", "-4:2", "-5:-1")

    // The empty surface is needed so this entire composable is not recomposed.
    Surface {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            if (completed) {
                Text(text = "Puzzle Solved!", fontSize = 26.sp, modifier = Modifier.height(40.dp))
            } else {
                Spacer(modifier = Modifier.height(40.dp))
            }

            Canvas(modifier = Modifier
                .width(boardSize.dp)
                .height(boardSize.dp)
                // .border(width = 1.dp, color = Color.Black)
                .pointerInput(Unit) {
                    // Handle click on board by calculating which hex was clicked, and cycle the number in that hex
                    detectTapGestures(onTap = { offset ->

                        var minDistance = (hexSize + 1).dp.toPx()
                        var closestHexID = ""
                        for (hexID in BoardModel.hexIDArray) {
                            val x: Float =
                                scaleX(
                                    hexID.split(":")[0].toInt(),
                                    hexSize
                                ).dp.toPx() + size.width / 2
                            val y: Float =
                                scaleY(
                                    hexID.split(":")[1].toInt(),
                                    hexSize
                                ).dp.toPx() + size.height / 2
                            val distance = calculateDistance(x, y, offset.x, offset.y)
                            if (minDistance > distance) {
                                minDistance = distance
                                closestHexID = hexID
                            }
                        }

                        if (closestHexID != "" && board[closestHexID] != null) {
                            if (!completed && hexChangeable[closestHexID]!!) {
                                board[closestHexID]!!.value = (board[closestHexID]!!.value + 1) % 8
                                boardModel.board[closestHexID] = board[closestHexID]!!.value

                                hexTextColor[closestHexID]!!.value = textColor
                            }
                        }

                        completed = boardModel.isBoardSolved()

                    })
                }) {

                // Draw the hexagons
                for (i in (0..6)) {
                    val group = BoardModel.hexIDGroups[3][i]
                    for (hexID: String in group) {
                        val x: Int = hexID.split(":")[0].toInt()
                        val y: Int = hexID.split(":")[1].toInt()

                        // Calculate the path around a hexagon
                        val hexPath = Path().apply {
                            moveTo(scaleX(x, hexSize).dp.toPx() + center.x, scaleY(y, hexSize).dp.toPx() + center.y)
                            relativeMoveTo(0f.dp.toPx(), -hexSize.dp.toPx())

                            relativeLineTo((hexSize* sqrt(3f) /2f).dp.toPx(), (hexSize/2f).dp.toPx())
                            relativeLineTo(0.dp.toPx(), (hexSize).dp.toPx())
                            relativeLineTo((-hexSize* sqrt(3f) /2f).dp.toPx(), (hexSize/2f).dp.toPx())
                            relativeLineTo((-hexSize* sqrt(3f) /2f).dp.toPx(), (-hexSize/2f).dp.toPx())
                            relativeLineTo(0.dp.toPx(), (-hexSize).dp.toPx())
                            relativeLineTo((hexSize* sqrt(3f) /2f).dp.toPx(), (-hexSize/2f).dp.toPx())

                            close()
                        }

                        // Fill hexagons
                        drawPath(
                            path = hexPath,
                            color = colorArray[i]
                        )

                        // Draw border
                        //  drawPath (
                        //     path = path,
                        //     Color.Black,
                        //     style = Stroke(width = 3f)
                        // )
                    }
                }

                // Draw the borders
                for (hexID: String in clusterCenterIDs) {
                    val x: Int = hexID.split(":")[0].toInt()
                    val y: Int = hexID.split(":")[1].toInt()

                    // Calculate the path around a hexagon
                    val thinInnerPath = Path().apply {
                        moveTo(scaleX(x, hexSize).dp.toPx() + center.x, scaleY(y, hexSize).dp.toPx() + center.y)
                        relativeMoveTo(0f.dp.toPx(), -hexSize.dp.toPx())

                        // Each of these 3 lines is moving along the center hexagon, then adding a line outwards
                        relativeLineTo((hexSize* sqrt(3f) /2f).dp.toPx(), (hexSize/2f).dp.toPx())
                        relativeLineTo((hexSize* sqrt(3f) /2f).dp.toPx(), (-hexSize/2f).dp.toPx())
                        relativeMoveTo((-hexSize* sqrt(3f) /2f).dp.toPx(), (hexSize/2f).dp.toPx())

                        relativeLineTo(0.dp.toPx(), (hexSize).dp.toPx())
                        relativeLineTo((hexSize* sqrt(3f) /2f).dp.toPx(), (hexSize/2f).dp.toPx())
                        relativeMoveTo((-hexSize* sqrt(3f) /2f).dp.toPx(), (-hexSize/2f).dp.toPx())

                        relativeLineTo((-hexSize* sqrt(3f) /2f).dp.toPx(), (hexSize/2f).dp.toPx())
                        relativeLineTo(0.dp.toPx(), (hexSize).dp.toPx())
                        relativeMoveTo(0.dp.toPx(), (-hexSize).dp.toPx())

                        relativeLineTo((-hexSize* sqrt(3f) /2f).dp.toPx(), (-hexSize/2f).dp.toPx())
                        relativeLineTo((-hexSize* sqrt(3f) /2f).dp.toPx(), (hexSize/2f).dp.toPx())
                        relativeMoveTo((hexSize* sqrt(3f) /2f).dp.toPx(), (-hexSize/2f).dp.toPx())

                        relativeLineTo(0.dp.toPx(), (-hexSize).dp.toPx())
                        relativeLineTo((-hexSize* sqrt(3f) /2f).dp.toPx(), (-hexSize/2f).dp.toPx())
                        relativeMoveTo((hexSize* sqrt(3f) /2f).dp.toPx(), (hexSize/2f).dp.toPx())

                        relativeLineTo((hexSize* sqrt(3f) /2f).dp.toPx(), (-hexSize/2f).dp.toPx())
                        relativeLineTo(0.dp.toPx(), (-hexSize).dp.toPx())
                        relativeMoveTo(0.dp.toPx(), (hexSize).dp.toPx())

                        close()
                    }

                    val thickOuterPath = Path().apply {
                        moveTo(scaleX(x, hexSize).dp.toPx() + center.x, scaleY(y, hexSize).dp.toPx() + center.y)
                        relativeMoveTo(0f.dp.toPx(), (-hexSize*2).dp.toPx())

                        relativeLineTo((hexSize* sqrt(3f) /2f).dp.toPx(), (-hexSize/2f).dp.toPx()) // Up right
                        relativeLineTo((hexSize* sqrt(3f) /2f).dp.toPx(), (hexSize/2f).dp.toPx()) // Down right
                        relativeLineTo(0.dp.toPx(), (hexSize).dp.toPx()) // Down
                        relativeLineTo((hexSize* sqrt(3f) /2f).dp.toPx(), (hexSize/2f).dp.toPx()) // Down right
                        relativeLineTo(0.dp.toPx(), (hexSize).dp.toPx()) // Down
                        relativeLineTo((-hexSize* sqrt(3f) /2f).dp.toPx(), (hexSize/2f).dp.toPx()) // Down left
                        relativeLineTo(0.dp.toPx(), (hexSize).dp.toPx()) // Down
                        relativeLineTo((-hexSize* sqrt(3f) /2f).dp.toPx(), (hexSize/2f).dp.toPx()) // Down left
                        relativeLineTo((-hexSize* sqrt(3f) /2f).dp.toPx(), (-hexSize/2f).dp.toPx()) // Up left
                        relativeLineTo((-hexSize* sqrt(3f) /2f).dp.toPx(), (hexSize/2f).dp.toPx()) // Down left
                        relativeLineTo((-hexSize* sqrt(3f) /2f).dp.toPx(), (-hexSize/2f).dp.toPx()) // Up left
                        relativeLineTo(0.dp.toPx(), (-hexSize).dp.toPx()) // Up
                        relativeLineTo((-hexSize* sqrt(3f) /2f).dp.toPx(), (-hexSize/2f).dp.toPx()) // Up left
                        relativeLineTo(0.dp.toPx(), (-hexSize).dp.toPx()) // Up
                        relativeLineTo((hexSize* sqrt(3f) /2f).dp.toPx(), (-hexSize/2f).dp.toPx()) // Up right
                        relativeLineTo(0.dp.toPx(), (-hexSize).dp.toPx()) // Up
                        relativeLineTo((hexSize* sqrt(3f) /2f).dp.toPx(), (-hexSize/2f).dp.toPx()) // Up right
                        relativeLineTo((hexSize* sqrt(3f) /2f).dp.toPx(), (hexSize/2f).dp.toPx()) // Down right

                        close()
                    }


                    // Draw thin border
                    drawPath (
                        path = thinInnerPath,
                        Color.Black,
                        style = Stroke(width = 2.5f)
                    )

                    // Draw thick border
                    drawPath (
                        path = thickOuterPath,
                        Color.Black,
                        style = Stroke(width = 9f)
                    )
                }


                // Show text
                for (hexID: String in BoardModel.hexIDArray) {


                    val x: Int = hexID.split(":")[0].toInt()
                    val y: Int = hexID.split(":")[1].toInt()

                    val paint = Paint()
                    paint.textAlign = Paint.Align.CENTER
                    paint.textSize = hexSize*2
                    paint.color = hexTextColor[hexID]!!.value

                    if (hexChangeable[hexID]!!) {
                        paint.typeface = Typeface.DEFAULT
                    } else {
                        paint.typeface = Typeface.DEFAULT_BOLD
                    }

                    drawContext.canvas.nativeCanvas.drawText(
                        stateToText(board[hexID]!!.value),
                        scaleX(x, hexSize).dp.toPx() + center.x,
                        scaleY(y, hexSize).dp.toPx() + center.y + (paint.textSize/8).dp.toPx(),
                        paint
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            var seconds by remember { mutableStateOf(timeSpent) }
            LaunchedEffect(Unit) {
                while(true) {
                    delay(1000)
                    seconds++
                }
            }
            if (showTimer) {
                Text(text = "${seconds/60}:${"%02d".format(seconds%60)}", fontSize = 20.sp, modifier = Modifier.height(30.dp))
            } else {
                Spacer(modifier = Modifier.height(30.dp))
            }

            Row {
                HexButton(onClick = { backToMenu(completed, seconds) }, text = "Back To Menu")

                Spacer(modifier = Modifier.width(10.dp))
                if (showHint) {
                    HexButton(enabled = !completed, onClick = {
                        val (hexID, number) = boardModel.hint()
                        if (number == 0) {
                            hexTextColor[hexID]!!.value = errorColor
                        } else {
                            board[hexID]!!.value = number
                            boardModel.board[hexID] = board[hexID]!!.value

                            completed = boardModel.isBoardSolved()
                        }
                    }, text = "Hint")
                }
            }

        }
    }
}