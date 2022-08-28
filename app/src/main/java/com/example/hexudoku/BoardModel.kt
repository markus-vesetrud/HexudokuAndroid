package com.example.hexudoku

import kotlin.random.Random

class BoardModel(board: MutableMap<String, Int>?, solvedBoard: Map<String, Int>?, numbersToRemove: Int) {
    var board: MutableMap<String, Int>
        private set
    private var solvedBoard: Map<String, Int>

    private fun calculateNext(): Map<String, Int> {
        val result: MutableMap<String, Int> = HashMap()
        for (hexID in hexIDArray) {
            // Checks that there are exactly one possible number in this cell, if so it is
            // added to the result
            val possibleNumbers: MutableList<Int> = ArrayList(7)
            for (number in 1..7) {
                if (checkPossible(hexID, number)) {
                    possibleNumbers.add(number)
                }
            }
            if (possibleNumbers.size == 1) {
                result[hexID] = possibleNumbers[0]
            }
        }
        return result
    }

    private fun getGroup(hexID: String, groupIndex: Int): Array<String> {
        for (i in hexIDGroups[groupIndex].indices) {
            for (j in hexIDGroups[groupIndex][i].indices) {
                if (hexIDGroups[groupIndex][i][j] == hexID) {
                    return hexIDGroups[groupIndex][i]
                }
            }
        }
        throw IllegalStateException("Error in getGroup method")
    }

    private fun checkPossible(hexID: String, number: Int): Boolean {
        if (board[hexID] != 0) {
            return false
        }
        for (groupIndex in 0..3) {
            for (otherHexID in getGroup(hexID, groupIndex)) {
                if (board[otherHexID] == number) {
                    return false
                }
            }
        }
        return true
    }

    private fun getNextHexID(hexID: String): String? {
        for (i in 0 until hexIDArray.size - 1) {
            if (hexIDArray[i] == hexID) {
                return hexIDArray[i + 1]
            }
        }
        return null
    }

    private fun randomPermutationArray(): List<Int> {
        val choices: MutableList<Int> = mutableListOf(1,2,3,4,5,6,7)
        val result: MutableList<Int> = ArrayList(7)
        while (choices.isNotEmpty()) {
            result.add(choices.removeAt(Random.nextInt(choices.size)))
        }
        return result
    }

    private fun recursiveFillMap(currentHexID: String): Boolean {
        for (number in randomPermutationArray()) {
            if (checkPossible(currentHexID, number)) {
                board[currentHexID] = number
                val nextHexID = getNextHexID(currentHexID)
                // If the end of the hexIDArray is reached, or the next function in line returns
                // true, then return true
                if (nextHexID == null || recursiveFillMap(nextHexID)) {
                    return true
                }
                board[currentHexID] = 0
            }
        }
        return false
    }

    private fun fillMap() {
        for (hexID in hexIDArray) {
            board[hexID] = 0
        }
        recursiveFillMap(hexIDArray[0])
        solvedBoard = HashMap(board)
    }

    private fun boardIsFilled(): Boolean {
        for (hexID in hexIDArray) {
            if (board[hexID] == 0) {
                return false
            }
        }
        return true
    }

    private fun boardIsSolvable(): Boolean {
        val startingBoardCopy: MutableMap<String, Int> = HashMap(board)
        while (!boardIsFilled()) {
            val results = calculateNext()
            if (results.isEmpty()) {
                board = startingBoardCopy
                return false
            }
            for (result in results) {
                board[result.key] = result.value
            }
        }
        board = startingBoardCopy
        return true
    }

    private fun removeXNumbers(x: Int) {
        var remaining = x
        val temporaryHexIDArray = mutableListOf(*hexIDArray)
        val randomIDArray = mutableListOf<String>()
        while (temporaryHexIDArray.isNotEmpty()) {
            randomIDArray.add(temporaryHexIDArray.removeAt(Random.nextInt(temporaryHexIDArray.size)))
        }

        while (randomIDArray.isNotEmpty() && remaining > 0) {
            remaining--
            val hexID = randomIDArray.removeAt(0)
            val temporary = board[hexID]!!
            board[hexID] = 0
            if (!boardIsSolvable()) {
                board[hexID] = temporary
            }
        }
    }

    fun hint(): Pair<String, Int> {

        // Check that no mistakes have been made
        for(hexID in hexIDArray) {
            if (board[hexID] != 0 && board[hexID] != solvedBoard[hexID]) {
                return Pair(hexID, 0)
            }
        }

        // If no mistakes then return one hex that is not yet filled in
        val results = calculateNext()
        return Pair(results.entries.first().key, results.entries.first().value)
    }

    internal fun isBoardSolved(): Boolean {
        for (hexID in hexIDArray) {
            if (board[hexID] != solvedBoard[hexID]) {
                return false
            }
        }
        return true
    }

    /*
    internal fun checkBoardIsSolvedButNotIdenticalToFirstSolution(): Boolean {
        for (hexID in hexIDArray) {
            if (board[hexID] == 0) {
                return false
            }
        }
        val group = mutableListOf<Int>()
        for (i in hexIDGroups.indices) {
            for (j in hexIDGroups[i].indices) {
                group.clear()
                for (hexID in hexIDGroups[i][j]) {
                    if (group.contains(board[hexID])) {
                        return false
                    } else {
                        group.add(board[hexID]!!)
                    }
                }
            }
        }
        return true
    }
    */

    companion object {
        /** This Array contains all the possible ID's for hexagons  */
        internal val hexIDArray = arrayOf(
            "-2:-4",
            "0:-4",
            "-3:-3",
            "-1:-3",
            "1:-3",
            "3:-3",
            "5:-3",
            "-6:-2",
            "-4:-2",
            "-2:-2",
            "0:-2",
            "2:-2",
            "4:-2",
            "6:-2",
            "-7:-1",
            "-5:-1",
            "-3:-1",
            "-1:-1",
            "1:-1",
            "3:-1",
            "5:-1",
            "-6:0",
            "-4:0",
            "-2:0",
            "0:0",
            "2:0",
            "4:0",
            "6:0",
            "-5:1",
            "-3:1",
            "-1:1",
            "1:1",
            "3:1",
            "5:1",
            "7:1",
            "-6:2",
            "-4:2",
            "-2:2",
            "0:2",
            "2:2",
            "4:2",
            "6:2",
            "-5:3",
            "-3:3",
            "-1:3",
            "1:3",
            "3:3",
            "0:4",
            "2:4"
        )

        /**
         * This array contains all the groups of hexagons where each number has to be
         * unique, in normal sudoku this is the rows and columns and 3x3 squares.
         */
        internal val hexIDGroups = arrayOf(
            arrayOf(
                arrayOf("-2:-4", "0:-4"),
                arrayOf("-3:-3", "-1:-3", "1:-3", "3:-3", "5:-3"),
                arrayOf("-6:-2", "-4:-2", "-2:-2", "0:-2", "2:-2", "4:-2", "6:-2"),
                arrayOf("-7:-1", "-5:-1", "-3:-1", "-1:-1", "1:-1", "3:-1", "5:-1"),
                arrayOf("-6:0", "-4:0", "-2:0", "0:0", "2:0", "4:0", "6:0"),
                arrayOf("-5:1", "-3:1", "-1:1", "1:1", "3:1", "5:1", "7:1"),
                arrayOf("-6:2", "-4:2", "-2:2", "0:2", "2:2", "4:2", "6:2"),
                arrayOf("-5:3", "-3:3", "-1:3", "1:3", "3:3"),
                arrayOf("0:4", "2:4")
            ),
            arrayOf(
                arrayOf("5:-3", "6:-2"),
                arrayOf("3:-3", "4:-2", "5:-1", "6:0", "7:1"),
                arrayOf("0:-4", "1:-3", "2:-2", "3:-1", "4:0", "5:1", "6:2"),
                arrayOf("-2:-4", "-1:-3", "0:-2", "1:-1", "2:0", "3:1", "4:2"),
                arrayOf("-3:-3", "-2:-2", "-1:-1", "0:0", "1:1", "2:2", "3:3"),
                arrayOf("-4:-2", "-3:-1", "-2:0", "-1:1", "0:2", "1:3", "2:4"),
                arrayOf("-6:-2", "-5:-1", "-4:0", "-3:1", "-2:2", "-1:3", "0:4"),
                arrayOf("-7:-1", "-6:0", "-5:1", "-4:2", "-3:3"),
                arrayOf("-6:2", "-5:3")
            ),
            arrayOf(
                arrayOf("-6:-2", "-7:-1"),
                arrayOf("-2:-4", "-3:-3", "-4:-2", "-5:-1", "-6:0"),
                arrayOf("0:-4", "-1:-3", "-2:-2", "-3:-1", "-4:0", "-5:1", "-6:2"),
                arrayOf("1:-3", "0:-2", "-1:-1", "-2:0", "-3:1", "-4:2", "-5:3"),
                arrayOf("3:-3", "2:-2", "1:-1", "0:0", "-1:1", "-2:2", "-3:3"),
                arrayOf("5:-3", "4:-2", "3:-1", "2:0", "1:1", "0:2", "-1:3"),
                arrayOf("6:-2", "5:-1", "4:0", "3:1", "2:2", "1:3", "0:4"),
                arrayOf("6:0", "5:1", "4:2", "3:3", "2:4"),
                arrayOf("7:1", "6:2")
            ),
            arrayOf(
                arrayOf("-1:-1", "1:-1", "2:0", "1:1", "-1:1", "-2:0", "0:0"),
                arrayOf("-2:-4", "0:-4", "1:-3", "0:-2", "-2:-2", "-3:-3", "-1:-3"),
                arrayOf("3:-3", "5:-3", "6:-2", "5:-1", "3:-1", "2:-2", "4:-2"),
                arrayOf("4:0", "6:0", "7:1", "6:2", "4:2", "3:1", "5:1"),
                arrayOf("2:2", "3:3", "2:4", "0:4", "-1:3", "0:2", "1:3"),
                arrayOf("-3:1", "-2:2", "-3:3", "-5:3", "-6:2", "-5:1", "-4:2"),
                arrayOf("-4:-2", "-3:-1", "-4:0", "-6:0", "-7:-1", "-6:-2", "-5:-1")
            )
        )
    }

    init {
        this.board = board ?: HashMap()
        this.solvedBoard = solvedBoard ?: HashMap()

        if (board == null) {
            fillMap()
            removeXNumbers(numbersToRemove)
        }
    }
}