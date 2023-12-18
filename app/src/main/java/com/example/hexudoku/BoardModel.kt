package com.example.hexudoku

import android.util.Log
import kotlin.math.min
import kotlin.random.Random
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

@OptIn(ExperimentalTime::class)
class BoardModel(board: MutableMap<String, Int>?, solvedBoard: Map<String, Int>?, numbersToRemove: Int, seed: Int) {
    var board: MutableMap<String, Int>
        private set
    private var solvedBoard: Map<String, Int>

    private val random: Random

    init {
        this.board = board ?: HashMap()
        this.solvedBoard = solvedBoard ?: HashMap()
        this.random = Random(seed)

        if (board == null) {
            var timeTaken = measureTime {
                fillMap()
            }
            Log.d("Time old fill", timeTaken.toString(DurationUnit.SECONDS, decimals = 3))
            timeTaken = measureTime {
                removeXNumbers(numbersToRemove)
            }
            Log.d("Time old remove", timeTaken.toString(DurationUnit.SECONDS, decimals = 3))

        }
        val solvedBoard2: Array<Int>
        var timeTaken = measureTime {
            solvedBoard2 = fillMap2()
        }
        Log.d("Time new fill", timeTaken.toString(DurationUnit.SECONDS, decimals = 3))

        timeTaken = measureTime {
            removeXNumbers2(numbersToRemove, solvedBoard2)
        }
        Log.d("Time new remove", timeTaken.toString(DurationUnit.SECONDS, decimals = 3))


    }

    // Finds the hexes with only one possible number and returns them
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

    // Gets the group the given hex is in with the given groupIndex
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

    // Checks whether the given number can be placed in the given hex
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

    // Gets the hexID after the given hexID in the hexIDArray
    private fun getNextHexID(hexID: String): String? {
        for (i in 0 until hexIDArray.size - 1) {
            if (hexIDArray[i] == hexID) {
                return hexIDArray[i + 1]
            }
        }
        return null
    }

    // Returns a random permutation of an array of 7 numbers
    private fun randomPermutationArray(): List<Int> {
        val choices: MutableList<Int> = mutableListOf(1,2,3,4,5,6,7)
        val result: MutableList<Int> = ArrayList(7)
        while (choices.isNotEmpty()) {
            result.add(choices.removeAt(Random.nextInt(choices.size)))
        }
        return result
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


        var domain: Array<MutableList<Int>> = Array(49) { mutableListOf(1,2,3,4,5,6,7) }
        val board: Array<Int> = Array(49) { 0 }
        val solvedBoard: Array<Int> = Array(49) { 0 }

        val centreIndices = arrayOf(17, 18, 23, 24, 25, 30, 31)

        // Randomly set the centre
        centreIndices.shuffle(this.random)
        for (i in centreIndices.indices) {
            domain[centreIndices[i]] = mutableListOf(i+1)
        }

        val (_, firstReducedDomain) = arcConsistency3(domain)
        domain = firstReducedDomain


        fun recursiveFillMap2(domain: Array<MutableList<Int>>): Pair<Boolean, Array<MutableList<Int>>> {

            val notFixedIndices = mutableListOf<Int>()
            for (i in domain.indices) {
                if (domain[i].size > 1) {
                    notFixedIndices.add(i)
                }
            }

            if (notFixedIndices.isEmpty()) {
                // If every hex has a domain of exactly 1, then return
                return Pair(true, domain)
            }

            val selectedHex = notFixedIndices[this.random.nextInt(notFixedIndices.size)]
            domain[selectedHex].shuffle(this.random)

            for (hexValue in domain[selectedHex]) {
                domain[selectedHex] = mutableListOf(hexValue)
                val (successAC3, reducedDomainAC3) = arcConsistency3(deepCopyDomain(domain))
                if (successAC3) {

                    val (successRecursion, reducedDomainRecursion) = recursiveFillMap2(deepCopyDomain(reducedDomainAC3))

                    if (successRecursion) {
                        return Pair(true, reducedDomainRecursion)
                    }
                }
            }

            return Pair(false, domain)

        }
        /*
        while (true) {
            backStack.addLast(deepCopyDomain(domain))
            Log.d("domain from fill map", "${domain.toList()}")

            // Find all hexIndices that has more than one option for its value
            notFixedIndices.clear()
            for (i in domain.indices) {
                if (domain[i].size > 1) {
                    notFixedIndices.add(i)
                }
            }
            if (notFixedIndices.isEmpty()) {
                // If every hex has a domain of exactly 1, then return
                break
            }
            // Redo this part
            // On the selected hex, we should loop through all the possible values
            // When one doesn't work, we try the next value
            // It can not work due to an immidiate issue with AC3, or due to back tracking
            // Use recursion
            selectedHex = notFixedIndices[this.random.nextInt(notFixedIndices.size)]
            selectedHexValue = domain[selectedHex][this.random.nextInt(domain[selectedHex].size)]

            domain[selectedHex] = mutableListOf(selectedHexValue)

            val (success, reducedDomain) = arcConsistency3(domain)
            if (!success) {
                // If the arc consistency algorithm returns false, then the last selected hex lead to that issue
                // Therefore we need to backtrack, so revert to the state at the beginning of the loop
                domain = backStack.removeLastOrNull()!!

                // Remember that in this state the selected hex and that value can not be selected again
                domain[selectedHex].remove(selectedHexValue)
            } else {
                domain = reducedDomain
            }

        }
        */

        val (successBuild, finalDomain) = recursiveFillMap2(domain)
        Log.d("domain from fill map", "success: $successBuild domain: ${finalDomain.toList()}")

        for (i in finalDomain.indices) {
            solvedBoard[i] = finalDomain[i][0]
        }
        return  solvedBoard
    }

    private fun logDomain(domain: Array<MutableList<Int>>) {
        for (i in domain.indices) {
            Log.d(i.toString(), domain[i].toString())
        }
    }

    private fun deepCopyDomain(domain: Array<MutableList<Int>>): Array<MutableList<Int>> {
        val newDomain = Array<MutableList<Int>>(domain.size) { mutableListOf() }
        for (i in domain.indices) {
            newDomain[i].addAll(domain[i])
        }
        return newDomain

    }

    private fun arcReduce(x: Int, y: Int, domain: Array<MutableList<Int>>): Pair<Boolean, Array<MutableList<Int>>> {
        // Assumes no domains are empty
        for (xValue in domain[x]) {
            // Checks if it is possible to find a yValue in the domain of y that can satisfy the condition that xValue != yValue
            // That is always true unless the domain of y contains only xValue
            if (domain[y].size == 1 && domain[y][0] == xValue) {
                domain[x].remove(xValue)
                // Normally arc reduce cannot return early, but in this case there will be at most 1 change
                return Pair(true, domain)
            }
        }
        return Pair(false, domain)
    }

    private fun arcConsistency3(startingDomain: Array<MutableList<Int>>): Pair<Boolean, Array<MutableList<Int>>> {

        var domain = startingDomain
        val worklist: MutableSet<Pair<Int, Int>> = mutableSetOf()
        for (i in constraints.indices) {
            for (j in constraints[i]) {
                worklist.add(Pair(i,j))
            }
        }

        while(worklist.isNotEmpty()) {
            val (x,y) = worklist.firstOrNull()!!
            worklist.remove(Pair(x, y))

            val (changed, reducedDomain) = arcReduce(x, y, domain)

            if (changed) {
                domain = reducedDomain
                if (domain[x].isEmpty()) {
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

    private fun singleIterationAC3(startingDomain: Array<MutableList<Int>>, worklist: MutableSet<Pair<Int, Int>>):
            Triple<Boolean, Array<MutableList<Int>>, MutableSet<Pair<Int, Int>>> {
        var domain = startingDomain
        val futureWorklist = mutableSetOf<Pair<Int, Int>>()

        while(worklist.isNotEmpty()) {
            val (x,y) = worklist.firstOrNull()!!
            worklist.remove(Pair(x, y))

            val (changed, reducedDomain) = arcReduce(x, y, domain)

            if (changed) {
                domain = reducedDomain
                if (domain[x].isEmpty()) {
                    // The problem is unsolvable, return false
                    return Triple(false, domain, futureWorklist)
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

        return Triple(true, domain, futureWorklist)
    }

    // This is a faster variant of just running AC3, as this algorithm can stop before the entire board is filled
    private fun checkHexCanBeFilled(hexID: Int, startingDomain: Array<MutableList<Int>>): Boolean {
        var domain = startingDomain

        var worklist: MutableSet<Pair<Int, Int>> = mutableSetOf()
        for (i in constraints.indices) {
            for (j in constraints[i]) {
                worklist.add(Pair(i,j))
            }
        }

        while (worklist.isNotEmpty()) {
            val (success, newDomain, newWorklist) = singleIterationAC3(domain, worklist)
            domain = newDomain
            worklist = newWorklist

            if (!success) {
                return false
            }
            if (domain[hexID].size == 1) {
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
        val domain: Array<MutableList<Int>> = Array(49) { mutableListOf(1,2,3,4,5,6,7) }
        for (i in solvedBoard.indices) {
            domain[i] = mutableListOf(solvedBoard[i])
        }


        var untestedHexes = MutableList(49) {i -> i}
        untestedHexes.shuffle(this.random)
        var remaining = x

        // Start by removing at most 7 numbers, as removing at most 7 are guaranteed to not
        // cause problems (I think)
        // This is only for speeding it up byt doing hte first 7 at once
        for (i in 0 until min(x, 7)) {
            // Set the domain of those hexes to have every possible number
            // The elements in untestedHexes are not removed, in case this approach fails
            domain[untestedHexes[i]] = mutableListOf(1,2,3,4,5,6,7)
        }
        val (success, newDomain) = arcConsistency3(deepCopyDomain(domain))

        if (success && domainIsFilled(newDomain)) {
            // if we have removed everything we need to, return early
            if (x < 7) {
                for (i in domain.indices) {
                    if (domain[i].size == 1) {
                        board[i] = domain[i][0]
                    } else {
                        board[i] = 0
                    }
                }
                return board
            } else {
                // Remove the first 7 elements from untestedHexes we are already done with
                untestedHexes = untestedHexes.slice(7 until untestedHexes.size).toMutableList()
                remaining -= 7
            }
        } else {
            // Reset the domain
            for (i in solvedBoard.indices) {
                domain[i] = mutableListOf(solvedBoard[i])
            }
        }
        Log.d("domain", "${domain.toList()}")


        // Now remove numbers one by one
        // At this point 7 hexes has already been removed, or 0 has been removed if something went wrong
        // In either case domain and untestedHexes should have values that make sense
        while (remaining > 0) {
            val nextHex = untestedHexes.removeAt(0)
            // Reduce the domain
            domain[nextHex] = mutableListOf(1,2,3,4,5,6,7)
            val (success, newDomain) = arcConsistency3(deepCopyDomain(domain))

            if (!success || !checkHexCanBeFilled(nextHex, newDomain)) {
                // If the hex cannot be removed, then reset the domain
                domain[nextHex] = mutableListOf(solvedBoard[nextHex])
            }
            // Reduce the remaining no matter what
            remaining--
        }

        for (i in domain.indices) {
            if (domain[i].size == 1) {
                board[i] = domain[i][0]
            } else {
                board[i] = 0
            }
        }
        Log.d("Board", board.toList().toString())
        return board

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
         * An array where the array at index i contains all the indices of the hexagons that needs to be different to i
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
}