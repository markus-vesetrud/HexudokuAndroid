package com.example.hexudoku

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import kotlin.math.sqrt


val clusterCenterIDs = arrayOf("0:0", "-1:-3", "4:-2", "5:1", "1:3", "-4:2", "-5:-1")

@Composable
fun TutorialBoard(boardSize: Float, step: Int) {

    val board = mapOf(
        "-6:-2" to 2, "-4:0" to 3, "-2:2" to 5, "-6:0" to 4, "-4:2" to 6, "-2:-2" to 0, "-2:-4" to 0,
        "-2:0" to 5, "5:-1" to 0, "5:-3" to 0, "1:-3" to 1, "1:-1" to 0, "-5:-1" to 7, "0:-4" to 0,
        "1:1" to 7, "3:1" to 0, "1:3" to 4, "-1:-1" to 3, "5:1" to 0, "3:3" to 0, "-1:-3" to 0,
        "7:1" to 0, "4:-2" to 0, "-6:2" to 0, "0:-2" to 7, "-3:1" to 4, "-1:3" to 1, "-4:-2" to 1,
        "-5:1" to 0, "-3:3" to 0, "-1:1" to 0, "3:-1" to 2, "3:-3" to 7, "-7:-1" to 5, "0:0" to 1,
        "-3:-3" to 6, "2:0" to 6, "0:2" to 3, "-3:-1" to 6, "4:0" to 7, "2:2" to 0, "0:4" to 0,
        "6:0" to 2, "4:2" to 0, "2:4" to 0, "6:2" to 0, "6:-2" to 0, "-5:3" to 2, "2:-2" to 0
    )

    // hexSize is the length from the center of a hex to a corner
    // The same as the side length of the hex
    val hexSize: Float = (boardSize - 20f)/(sqrt(3f)*8f)
    val colorArray = colorArray()
    val textColor: Int = colorToHex(MaterialTheme.colors.onSurface)

    Canvas(modifier = androidx.compose.ui.Modifier
        .width(boardSize.dp)
        .height(boardSize.dp)
    ) {

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

        when (step) {
            2 -> {
                drawLine(
                    start = Offset(x= scaleX(-7.5f, hexSize).dp.toPx() + center.x, y = scaleY(-1f, hexSize).dp.toPx() + center.y),
                    end = Offset(x= scaleX(5.5f, hexSize).dp.toPx() + center.x, y = scaleY(-1f, hexSize).dp.toPx() + center.y),
                    color = Color.Red,
                    strokeWidth = 10f
                )
            }
            3-> {
                drawLine(
                    start = Offset(x= scaleX(-4.25f, hexSize).dp.toPx() + center.x, y = scaleY(-2.25f, hexSize).dp.toPx() + center.y),
                    end = Offset(x= scaleX(2.25f, hexSize).dp.toPx() + center.x, y = scaleY(4.25f, hexSize).dp.toPx() + center.y),
                    color = Color.Red,
                    strokeWidth = 10f
                )
            }
            4 -> {
                drawLine(
                    start = Offset(x= scaleX(-5.25f, hexSize).dp.toPx() + center.x, y = scaleY(3.25f, hexSize).dp.toPx() + center.y),
                    end = Offset(x= scaleX(1.25f, hexSize).dp.toPx() + center.x, y = scaleY(-3.25f, hexSize).dp.toPx() + center.y),
                    color = Color.Red,
                    strokeWidth = 10f
                )
            }
            else -> {

            }

        }

        // Show text
        for (hexID: String in BoardModel.hexIDArray) {


            val x: Int = hexID.split(":")[0].toInt()
            val y: Int = hexID.split(":")[1].toInt()

            val paint = Paint()
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = hexSize*2
            paint.color = textColor
            paint.typeface = Typeface.DEFAULT_BOLD

            drawContext.canvas.nativeCanvas.drawText(
                stateToText(board[hexID]!!),
                scaleX(x, hexSize).dp.toPx() + center.x,
                scaleY(y, hexSize).dp.toPx() + center.y + (paint.textSize/8).dp.toPx(),
                paint
            )
        }
    }
}