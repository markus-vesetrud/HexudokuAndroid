package com.example.hexudoku

import android.util.Log
import kotlin.random.Random
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

@OptIn(ExperimentalTime::class)
class BoardModel(board: Array<Int>?, solvedBoard: Array<Int>?, numbersToRemove: Int, seed: Int) {

    private val random: Random

    var board: Array<Int>
        private set

    private var solvedBoard: Array<Int>

    // This array lacks the 7 centre hexes, as they are filled separately
    private val almostOptimizedHexSequence = arrayOf(10, 9, 3, 2, 0, 4, 1, 11, 19, 12, 5, 6, 13, 20, 26, 32, 33, 27, 34, 41, 40, 39, 38, 45, 46, 48, 47, 44, 37, 29, 36, 43, 42, 35, 28, 22, 16, 15, 21, 14, 7, 8)
    // This array contains the random order in which the values of the above hexes are checked
    private val randomValueSequences: Array<List<Int>>


    init {
        this.random = Random(seed)

        this.board = board ?: Array(49) { 0 }
        this.solvedBoard = solvedBoard ?: Array(49) { 0 }
        this.randomValueSequences = Array(almostOptimizedHexSequence.size) { listOf(1,2,3,4,5,6,7).shuffled(this.random) }

        if (board == null) {

            var timeTaken = measureTime {
                fillMap()
            }
            Log.d("Time to fill board", timeTaken.toString(DurationUnit.SECONDS, decimals = 3))

            timeTaken = measureTime {
                removeXNumbers(numbersToRemove)
            }
            Log.d("Time to remove", timeTaken.toString(DurationUnit.SECONDS, decimals = 3))
            Log.d("board", this.board.toList().toString())

        }

    }

    // Finds the hexes with only one possible number and returns them
    private fun calculateNext(): List<Pair<Int, Int>> {
        val result = mutableListOf<Pair<Int, Int>>()
        for (hexID in 0..48) {
            // Checks that there are exactly one possible number in this cell, if so it is
            // added to the result

            // possibleNumbers has a bit representation that encodes the numbers that are possible
            // at this hex, if the bit on the x'th position from the left is a 1, then x is possible
            var possibleNumbers = 0
            for (number in 1..7) {
                if (checkPossible(hexID, number)) {
                    possibleNumbers = possibleNumbers or (0b1 shl (number))
                }
            }
            // This checks that possibleNumbers contain exactly one bit
            // if (possibleNumbers != 0 && ((possibleNumbers and (possibleNumbers-1)) == 0)) {}
            when (possibleNumbers) {
                0b00000010 -> result.add(Pair(hexID, 1))
                0b00000100 -> result.add(Pair(hexID, 2))
                0b00001000 -> result.add(Pair(hexID, 3))
                0b00010000 -> result.add(Pair(hexID, 4))
                0b00100000 -> result.add(Pair(hexID, 5))
                0b01000000 -> result.add(Pair(hexID, 6))
                0b10000000 -> result.add(Pair(hexID, 7))
            }
        }
        return result
    }

    // Checks whether the given number can be placed in the given hex
    private fun checkPossible(hexID: Int, number: Int): Boolean {
        if (board[hexID] != 0) {
            return false
        }
        for (otherHexID in constraints[hexID]) {
            if (board[otherHexID] == number) {
                return false
            }
        }
        return true
    }

    /*
    private fun checkMoreThanOneBit(byte: Byte): Boolean {
        val num = byte.toInt()
        return (num and (num-1)) != 0
    }

    // New idea for solver
    // Implement the AC3 algorithm that reduces the domain of every variable
    // Start by filling the centre randomly and running AC3
    // (The worklist for this solver can then be modified by removing all items from the centre that is filled, but this is only a small speedup)
    // 1. Save the board on a stack
    // 2. Select a random variable with a domain larger than 1, and assign it a random value from its domain
    // 3. Run AC3, if it solves the board, end the algorithm, if not repeat from step 1
    // 4. If AC3 finds the problem is unsolvable, then backtrack to the top board on the stack,
    // and reduce the domain of the randomly selected hex to exclude the tried value

    private fun fillMap2(): Array<Int> {

        // The domain variable is a array of 49 bytes where each byte's digit signifies whether the
        // index of that digit is possible at that hex. So for example will domain[28] & 0b01000000u != 0u
        // mean that 1 is possible at that location

        var domain: Array<Byte> = Array(49) { 0b01111111 }
        val board: Array<Int> = Array(49) { 0 }
        val solvedBoard: Array<Int> = Array(49) { 0 }



        // Randomly set the centre
        val centreIndices = listOf(17, 18, 23, 24, 25, 30, 31).shuffled(this.random)
        for (i in centreIndices.indices) {
            domain[centreIndices[i]] = (0b1 shl (6-i)).toByte()
        }

        logDomain2(domain)


        val (_, firstReducedDomain) = arcConsistency3(domain)
        domain = firstReducedDomain


        fun recursiveFillMap2(inputDomain: Array<Byte>): Pair<Boolean, Array<Byte>> {
            val domain = inputDomain.copyOf()
            // logDomain2(domain)

            val notFixedIndices = mutableListOf<Int>()
            for (i in domain.indices) {
                if (checkMoreThanOneBit(domain[i])) {
                    notFixedIndices.add(i)
                }
            }

            if (notFixedIndices.isEmpty()) {
                // If every hex has a domain of exactly 1, then return
                return Pair(true, domain)
            }

            val selectedHex = notFixedIndices[this.random.nextInt(notFixedIndices.size)]
            val originalValue: Byte = domain[selectedHex]

            for (hexValue in 1..7) {
                val hexValueInt = (0b1 shl (7-hexValue))
                // Check if hexValue is in the domain of selectedHex
                if ((hexValueInt and domain[selectedHex].toInt()) != 0) {
                    // Temporarily set the domain of selectedHex to only be hexValue
                    // Log.d("note", "Setting $selectedHex to $hexValue")
                    domain[selectedHex] = hexValueInt.toByte()

                    val (successAC3, reducedDomainAC3) = arcConsistency3(domain)
                    if (successAC3) {

                        val (successRecursion, reducedDomainRecursion) = recursiveFillMap2(reducedDomainAC3)

                        if (successRecursion) {
                            return Pair(true, reducedDomainRecursion)
                        }
                    }
                    domain[selectedHex] = originalValue
                }
            }

            // Log.d("note", "Resetting $selectedHex")
            return Pair(false, domain)

        }

        val (successBuild, finalDomain) = recursiveFillMap2(domain)
        Log.d("domain from fill map", "success: $successBuild domain: ${logDomain2(finalDomain)}")

        for (i in finalDomain.indices) {
            when (finalDomain[i].toInt()) {
                0b00000001 -> solvedBoard[i] = 7
                0b00000010 -> solvedBoard[i] = 6
                0b00000100 -> solvedBoard[i] = 5
                0b00001000 -> solvedBoard[i] = 4
                0b00010000 -> solvedBoard[i] = 3
                0b00100000 -> solvedBoard[i] = 2
                0b01000000 -> solvedBoard[i] = 1
                else -> solvedBoard[i] = -1
            }
        }

        return solvedBoard

    }
    private fun logDomain2(domain: Array<Byte>) {
        val showDomain = List<MutableList<Int>>(49) { mutableListOf()}
        for (i in domain.indices) {
            for (j in 1..7) {
                if (((0b1 shl (7-j)) and domain[i].toInt()) != 0) {
                    showDomain[i].add(j)
                }
            }
        }
        Log.d("domain byte", showDomain.toString())
    }

    private fun arcReduce(x: Int, y: Int, domain: Array<Byte>): Boolean {
        // Assumes no domains are empty
        for (xValue in 1..7) {
            val xValueInt = (0b1 shl (7-xValue))
            // Check if xValue is in the domain of x AND that xValue is the only value in the domain of y
            // Checks if it is possible to find a yValue in the domain of y that can satisfy the condition that xValue != yValue
            // That is always true unless the domain of y contains only xValue
            if (((xValueInt and domain[x].toInt()) != 0) && (xValueInt == domain[y].toInt())) {
                // This updates the domain variable outside the function
                domain[x] = (domain[x].toInt() and xValueInt.inv()).toByte()
                // Normally arc reduce cannot return early, but in this case there will be at most 1 change
                return true
            }
        }
        return false
    }

    private fun arcConsistency3(inputDomain: Array<Byte>): Pair<Boolean, Array<Byte>> {

        val domain = inputDomain.copyOf()

        val worklist: MutableSet<Pair<Int, Int>> = mutableSetOf()
        for (i in constraints.indices) {
            for (j in constraints[i]) {
                worklist.add(Pair(i,j))
            }
        }

        while(worklist.isNotEmpty()) {
            val (x,y) = worklist.firstOrNull()!!
            worklist.remove(Pair(x, y))

            // The domain may be changed by this function
            val changed = arcReduce(x, y, domain)

            if (changed) {
                // If the domain is empty
                if (domain[x].toInt() == 0) {
                    // The problem is unsolvable, return false
                    return Pair(false, domain)
                } else {
                    // Find all z such that there is a relation (x,z) or (z,x)
                    // In this case the relations are symmetric and all the relations are captured in the constraints array
                    for (z in constraints[x]) {
                        if (z != y) {
                            // worklist is a set, so only unique items allowed
                            worklist.add(Pair(z, x))
                        }
                    }
                }
            }
        }

        return Pair(true, domain)

    }


    // This function will change the given domain, unlike the normal AC3 function I wrote
    private fun singleIterationAC3(domain: Array<Byte>, worklist: MutableSet<Pair<Int, Int>>):
            Pair<Boolean, MutableSet<Pair<Int, Int>>> {
        val futureWorklist = mutableSetOf<Pair<Int, Int>>()

        while(worklist.isNotEmpty()) {
            val (x,y) = worklist.firstOrNull()!!
            worklist.remove(Pair(x, y))

            val changed = arcReduce(x, y, domain)

            if (changed) {
                if (domain[x].toInt() == 0) {
                    // The problem is unsolvable, return false
                    return Pair(false, futureWorklist)
                } else {
                    // Find all z such that there is a relation (x,z) or (z,x)
                    // In this case the relations are symmetric and all the relations are captured in the constraints array
                    for (z in constraints[x]) {
                        if (z != y) {
                            // worklist is a set, so only unique items allowed
                            futureWorklist.add(Pair(z, x))
                        }
                    }
                }
            }
        }

        return Pair(true, futureWorklist)
    }

    // This is a faster variant of just running AC3, as this algorithm can stop before the entire board is filled
    private fun checkHexCanBeFilled(hexID: Int, startingDomain: Array<Byte>): Boolean {
        val domain = startingDomain.copyOf()

        var worklist: MutableSet<Pair<Int, Int>> = mutableSetOf()
        for (i in constraints.indices) {
            for (j in constraints[i]) {
                worklist.add(Pair(i,j))
            }
        }

        while (worklist.isNotEmpty()) {
            val (success, futureWorklist) = singleIterationAC3(domain, worklist)
            worklist = futureWorklist
            if (!success) {
                return false
            }
            // If there is exactly one element at hexID we can stop, as we are guaranteed the rest will be filled too
            if (domain[hexID].toInt() != 0 && !checkMoreThanOneBit(domain[hexID])) {
                return true
            }
        }
        return false

    }

    private fun domainIsFilled(domain: Array<MutableList<Int>>): Boolean {

        for (values in domain) {
            if (values.size != 1) {
                return false
            }
        }
        return true
    }

    private fun removeXNumbers2(x: Int, solvedBoard: Array<Int>): Array<Int> {

        val board = solvedBoard.copyOf()
        // Set up the domain to be 0b01000000 if the board has a 1 and 0b00000001 if the board has a 7
        val domain: Array<Byte> = Array(49) { i -> (0b1 shl (7-solvedBoard[i])).toByte() }


        var untestedHexes = (List(49) {i -> i}).shuffled(this.random).toMutableList()

        // Now remove numbers one by one
        // At this point 7 hexes has already been removed, or 0 has been removed if something went wrong
        // In either case domain and untestedHexes should have values that make sense
        for (i in 0 until x) {
            if (untestedHexes.isEmpty()) {
                break
            }
            val nextHex = untestedHexes.removeAt(0)
            // Reduce the domain
            domain[nextHex] = 0b01111111
            if (!checkHexCanBeFilled(nextHex, domain)) {
                // If the hex cannot be removed, then reset the domain
                domain[nextHex] = (0b1 shl (7-solvedBoard[nextHex])).toByte()
            }
            // Reduce the remaining no matter what
        }

        for (i in domain.indices) {
            when (domain[i].toInt()) {
                0b00000001 -> board[i] = 7
                0b00000010 -> board[i] = 6
                0b00000100 -> board[i] = 5
                0b00001000 -> board[i] = 4
                0b00010000 -> board[i] = 3
                0b00100000 -> board[i] = 2
                0b01000000 -> board[i] = 1
                else -> board[i] = -1
            }
        }

        return board

    }
    */

    /*
     * A very simple function that uses backtracking to create a new board
     * It is fast because
     * 1. Calls to random number generators are precomputed
     * 2. The first 7 hexes are precomputed and guaranteed to not need backtracking
     * 3. almostOptimizedHexSequence puts the hexes in a sequence such that the next hex will quite
     * often have very few possible numbers (employing a general heuristic for CSPs)
     * 4. Very little memory is required, as the same board can be reused since the method is so simple
     */
    private fun recursiveFillMap(currentHexIndex: Int): Boolean {
        val currentHexID = this.almostOptimizedHexSequence[currentHexIndex]
        for (number in this.randomValueSequences[currentHexIndex]) {
            if (checkPossible(currentHexID, number)) {
                // Try filling in this hex before moving on to the next function call
                board[currentHexID] = number
                // If the end of the almostOptimizedHexSequence is reached, or the next
                // function in line returns true, then return true
                if (currentHexIndex == 41 || recursiveFillMap(currentHexIndex + 1)) {
                    return true
                }
                // Reset the change made by this try
                board[currentHexID] = 0
            }
        }
        // All numbers have been tried for this hex and none of the worked, therefore the
        // change made in the preceding call did not work out
        return false
    }

    /*
     * Fills the board by precomputing the centre and calling the recursiveFillMap function
     */
    private fun fillMap() {
        board = Array(49) { 0 }
        // Randomly set the centre
        val centreIndices = listOf(17, 18, 23, 24, 25, 30, 31).shuffled(this.random)
        for (i in centreIndices.indices) {
            board[centreIndices[i]] = i+1
        }
        recursiveFillMap(0)
        solvedBoard = board.copyOf()
    }

    private fun hexCanBeDetermined(hexID: Int): Boolean {
        val startingBoardCopy = board.copyOf()
        while (board[hexID] == 0) {
            val results = calculateNext()
            if (results.isEmpty()) {
                board = startingBoardCopy
                return false
            }
            for ((resultHexID, value) in results) {
                board[resultHexID] = value
            }
        }
        board = startingBoardCopy
        return true
    }

    private fun removeXNumbers(x: Int) {
        var remaining = x
        val randomIDArray = List(49) {i -> i}.shuffled(this.random).toMutableList()


        while (randomIDArray.isNotEmpty() && remaining > 0) {
            remaining--
            val hexID = randomIDArray.removeAt(0)
            val oldValue = board[hexID]
            board[hexID] = 0
            if (!hexCanBeDetermined(hexID)) {
                board[hexID] = oldValue
            }
        }
    }
    fun hint(): Pair<Int, Int> {

        // Check that no mistakes have been made
        for(hexID in 0..48) {
            if (board[hexID] != 0 && board[hexID] != solvedBoard[hexID]) {
                return Pair(hexID, 0)
            }
        }

        // If no mistakes then return one hex that is not yet filled in
        val results = calculateNext()
        return Pair(results[0].first, results[0].second)
    }

    internal fun isBoardSolved(): Boolean {
        for (hexID in 0..48) {
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
        /** This Array contains the position of the hexagons */
        internal val hexPositionArray = arrayOf(
            Pair(-2, -4),
            Pair(0, -4),
            Pair(-3, -3),
            Pair(-1, -3),
            Pair(1, -3),
            Pair(3, -3),
            Pair(5, -3),
            Pair(-6, -2),
            Pair(-4, -2),
            Pair(-2, -2),
            Pair(0, -2),
            Pair(2, -2),
            Pair(4, -2),
            Pair(6, -2),
            Pair(-7, -1),
            Pair(-5, -1),
            Pair(-3, -1),
            Pair(-1, -1),
            Pair(1, -1),
            Pair(3, -1),
            Pair(5, -1),
            Pair(-6, 0),
            Pair(-4, 0),
            Pair(-2, 0),
            Pair(0, 0),
            Pair(2, 0),
            Pair(4, 0),
            Pair(6, 0),
            Pair(-5, 1),
            Pair(-3, 1),
            Pair(-1, 1),
            Pair(1, 1),
            Pair(3, 1),
            Pair(5, 1),
            Pair(7, 1),
            Pair(-6, 2),
            Pair(-4, 2),
            Pair(-2, 2),
            Pair(0, 2),
            Pair(2, 2),
            Pair(4, 2),
            Pair(6, 2),
            Pair(-5, 3),
            Pair(-3, 3),
            Pair(-1, 3),
            Pair(1, 3),
            Pair(3, 3),
            Pair(0, 4),
            Pair(2, 4)
        )

        /**
         * An array where the array at index i contains all the indices of the hexagons that needs to be different to i
         *
         * This array was computed from the hexIDGroups array below
         */
        internal val constraints = arrayOf(
            arrayOf(1, 3, 10, 18, 25, 32, 40, 2, 8, 15, 21, 4, 9),
            arrayOf(0, 4, 11, 19, 26, 33, 41, 3, 9, 16, 22, 28, 35, 10, 2),
            arrayOf(3, 4, 5, 6, 9, 17, 24, 31, 39, 46, 0, 8, 15, 21, 1, 10),
            arrayOf(2, 4, 5, 6, 0, 10, 18, 25, 32, 40, 1, 9, 16, 22, 28, 35),
            arrayOf(2, 3, 5, 6, 1, 11, 19, 26, 33, 41, 10, 17, 23, 29, 36, 42, 0, 9),
            arrayOf(2, 3, 4, 6, 12, 20, 27, 34, 11, 18, 24, 30, 37, 43, 13, 19),
            arrayOf(2, 3, 4, 5, 13, 12, 19, 25, 31, 38, 44, 20, 11),
            arrayOf(8, 9, 10, 11, 12, 13, 15, 22, 29, 37, 44, 47, 14, 16, 21),
            arrayOf(7, 9, 10, 11, 12, 13, 16, 23, 30, 38, 45, 48, 0, 2, 15, 21, 22, 14),
            arrayOf(7, 8, 10, 11, 12, 13, 2, 17, 24, 31, 39, 46, 1, 3, 16, 22, 28, 35, 0, 4),
            arrayOf(7, 8, 9, 11, 12, 13, 0, 3, 18, 25, 32, 40, 4, 17, 23, 29, 36, 42, 1, 2),
            arrayOf(7, 8, 9, 10, 12, 13, 1, 4, 19, 26, 33, 41, 5, 18, 24, 30, 37, 43, 6, 20),
            arrayOf(7, 8, 9, 10, 11, 13, 5, 20, 27, 34, 6, 19, 25, 31, 38, 44),
            arrayOf(7, 8, 9, 10, 11, 12, 6, 20, 26, 32, 39, 45, 47, 5, 19),
            arrayOf(15, 16, 17, 18, 19, 20, 21, 28, 36, 43, 7, 8, 22),
            arrayOf(14, 16, 17, 18, 19, 20, 7, 22, 29, 37, 44, 47, 0, 2, 8, 21),
            arrayOf(14, 15, 17, 18, 19, 20, 8, 23, 30, 38, 45, 48, 1, 3, 9, 22, 28, 35, 21, 7),
            arrayOf(14, 15, 16, 18, 19, 20, 2, 9, 24, 31, 39, 46, 4, 10, 23, 29, 36, 42, 25, 30),
            arrayOf(14, 15, 16, 17, 19, 20, 0, 3, 10, 25, 32, 40, 5, 11, 24, 30, 37, 43, 31, 23),
            arrayOf(14, 15, 16, 17, 18, 20, 1, 4, 11, 26, 33, 41, 6, 12, 25, 31, 38, 44, 5, 13),
            arrayOf(14, 15, 16, 17, 18, 19, 5, 12, 27, 34, 13, 26, 32, 39, 45, 47, 6, 11),
            arrayOf(22, 23, 24, 25, 26, 27, 14, 28, 36, 43, 0, 2, 8, 15, 16, 7),
            arrayOf(21, 23, 24, 25, 26, 27, 7, 15, 29, 37, 44, 47, 1, 3, 9, 16, 28, 35, 8, 14),
            arrayOf(21, 22, 24, 25, 26, 27, 8, 16, 30, 38, 45, 48, 4, 10, 17, 29, 36, 42, 18, 31),
            arrayOf(21, 22, 23, 25, 26, 27, 2, 9, 17, 31, 39, 46, 5, 11, 18, 30, 37, 43),
            arrayOf(21, 22, 23, 24, 26, 27, 0, 3, 10, 18, 32, 40, 6, 12, 19, 31, 38, 44, 17, 30),
            arrayOf(21, 22, 23, 24, 25, 27, 1, 4, 11, 19, 33, 41, 13, 20, 32, 39, 45, 47, 34, 40),
            arrayOf(21, 22, 23, 24, 25, 26, 5, 12, 20, 34, 33, 40, 46, 48, 41, 32),
            arrayOf(29, 30, 31, 32, 33, 34, 14, 21, 36, 43, 1, 3, 9, 16, 22, 35, 37, 42),
            arrayOf(28, 30, 31, 32, 33, 34, 7, 15, 22, 37, 44, 47, 4, 10, 17, 23, 36, 42, 43, 35),
            arrayOf(28, 29, 31, 32, 33, 34, 8, 16, 23, 38, 45, 48, 5, 11, 18, 24, 37, 43, 17, 25),
            arrayOf(28, 29, 30, 32, 33, 34, 2, 9, 17, 24, 39, 46, 6, 12, 19, 25, 38, 44, 18, 23),
            arrayOf(28, 29, 30, 31, 33, 34, 0, 3, 10, 18, 25, 40, 13, 20, 26, 39, 45, 47, 27, 41),
            arrayOf(28, 29, 30, 31, 32, 34, 1, 4, 11, 19, 26, 41, 27, 40, 46, 48),
            arrayOf(28, 29, 30, 31, 32, 33, 5, 12, 20, 27, 41, 26, 40),
            arrayOf(36, 37, 38, 39, 40, 41, 42, 1, 3, 9, 16, 22, 28, 29, 43),
            arrayOf(35, 37, 38, 39, 40, 41, 14, 21, 28, 43, 4, 10, 17, 23, 29, 42),
            arrayOf(35, 36, 38, 39, 40, 41, 7, 15, 22, 29, 44, 47, 5, 11, 18, 24, 30, 43, 42, 28),
            arrayOf(35, 36, 37, 39, 40, 41, 8, 16, 23, 30, 45, 48, 6, 12, 19, 25, 31, 44, 46, 47),
            arrayOf(35, 36, 37, 38, 40, 41, 2, 9, 17, 24, 31, 46, 13, 20, 26, 32, 45, 47, 48, 44),
            arrayOf(35, 36, 37, 38, 39, 41, 0, 3, 10, 18, 25, 32, 27, 33, 46, 48, 26, 34),
            arrayOf(35, 36, 37, 38, 39, 40, 1, 4, 11, 19, 26, 33, 34, 27, 32),
            arrayOf(43, 44, 45, 46, 35, 4, 10, 17, 23, 29, 36, 37, 28),
            arrayOf(42, 44, 45, 46, 14, 21, 28, 36, 5, 11, 18, 24, 30, 37, 29, 35),
            arrayOf(42, 43, 45, 46, 7, 15, 22, 29, 37, 47, 6, 12, 19, 25, 31, 38, 39, 48),
            arrayOf(42, 43, 44, 46, 8, 16, 23, 30, 38, 48, 13, 20, 26, 32, 39, 47),
            arrayOf(42, 43, 44, 45, 2, 9, 17, 24, 31, 39, 27, 33, 40, 48, 47, 38),
            arrayOf(48, 7, 15, 22, 29, 37, 44, 13, 20, 26, 32, 39, 45, 46, 38),
            arrayOf(47, 8, 16, 23, 30, 38, 45, 27, 33, 40, 46, 39, 44)
        )

        /**
         * This array contains all the groups of hexagons where each number has to be
         * unique, in normal sudoku this is the rows and columns and 3x3 squares.
         *
         * hexIDGroups[0] is the clusters, hexIDGroups[1] is the rows, hexIDGroups[2 and 3] is the diagonals
         *
         * The rows and diagonals on the edges with only 2 hexes can be removed, since they must already be unique as they are always part
         * of the same cluster, but this data structure is not intended for checking uniqueness, for that use the array above.
         */
        internal val hexIDGroups = arrayOf(
            arrayOf(
                arrayOf(17, 18, 25, 31, 30, 23, 24),
                arrayOf(0, 1, 4, 10, 9, 2, 3),
                arrayOf(5, 6, 13, 20, 19, 11, 12),
                arrayOf(26, 27, 34, 41, 40, 32, 33),
                arrayOf(39, 46, 48, 47, 44, 38, 45),
                arrayOf(29, 37, 43, 42, 35, 28, 36),
                arrayOf(8, 16, 22, 21, 14, 7, 15)
            ),
            arrayOf(
                arrayOf(0, 1),
                arrayOf(2, 3, 4, 5, 6),
                arrayOf(7, 8, 9, 10, 11, 12, 13),
                arrayOf(14, 15, 16, 17, 18, 19, 20),
                arrayOf(21, 22, 23, 24, 25, 26, 27),
                arrayOf(28, 29, 30, 31, 32, 33, 34),
                arrayOf(35, 36, 37, 38, 39, 40, 41),
                arrayOf(42, 43, 44, 45, 46),
                arrayOf(47, 48)
            ),
            arrayOf(
                arrayOf(6, 13),
                arrayOf(5, 12, 20, 27, 34),
                arrayOf(1, 4, 11, 19, 26, 33, 41),
                arrayOf(0, 3, 10, 18, 25, 32, 40),
                arrayOf(2, 9, 17, 24, 31, 39, 46),
                arrayOf(8, 16, 23, 30, 38, 45, 48),
                arrayOf(7, 15, 22, 29, 37, 44, 47),
                arrayOf(14, 21, 28, 36, 43),
                arrayOf(35, 42)
            ),
            arrayOf(
                arrayOf(7, 14),
                arrayOf(0, 2, 8, 15, 21),
                arrayOf(1, 3, 9, 16, 22, 28, 35),
                arrayOf(4, 10, 17, 23, 29, 36, 42),
                arrayOf(5, 11, 18, 24, 30, 37, 43),
                arrayOf(6, 12, 19, 25, 31, 38, 44),
                arrayOf(13, 20, 26, 32, 39, 45, 47),
                arrayOf(27, 33, 40, 46, 48),
                arrayOf(34, 41)
            )
        )
    }
}