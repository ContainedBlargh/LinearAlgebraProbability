import GaussJordan.ReductionResultType.*
import java.io.Serializable

object GaussJordan {

    abstract class Move : Serializable

    data class Scale(val scalar: Float, val row: Int) : Move()

    data class Swap(val r1: Int, val r2: Int) : Move()

    data class Add(val scalar: Float, val r1: Int, val r2: Int) : Move()

    fun applyMove(matrix: Matrixf, move: Move): Matrixf =
        when (move) {
            is Scale -> matrix.scaleRow(move.row, move.scalar)
            is Swap -> matrix.swapRows(move.r1, move.r2)
            is Add -> matrix.scaleAddRows(move.scalar, move.r1, move.r2)
            else -> throw NoWhenBranchMatchedException()
        }

    enum class ReductionResultType {
        ROW_ECHELON,
        REDUCED_ROW_ECHELON,
        PARAMETRIC
    }

    class ReductionResult(val matrix: Matrixf, val trace: List<Move>, val resultType: ReductionResultType)

    fun reduceMatrixToEchelon(matrix: Matrixf): ReductionResult {
        if (matrix.parallel().anyMatch { r -> r.all { it == 0f } }) {
            return ReductionResult(matrix, listOf(), PARAMETRIC)
        }
        if (matrix.isReducedEchelonForm()) {
            return ReductionResult(matrix, listOf(), REDUCED_ROW_ECHELON)
        }
        if (matrix.isEchelonForm()) {
            return ReductionResult(matrix, listOf(), ROW_ECHELON)
        }
        var m = matrix
        val moves = mutableListOf<Move>()
        for (i in 0 until m.rows.size) {
            val v = m[i, i]
            var ti = i
            var tv = v
            if (v == 0f) {
                ti = m.parallelIndexed()
                    .filter { (_, r2) -> r2[i].second != 0f }
                    .findFirst().map { it.first }
                    .orElseThrow { IllegalStateException("Cannot reduce matrix with an all-zero column") }
                tv = m[ti, i]
            }
            if (ti != i) {
                m = m.swapRows(i, ti)
                moves.add(Swap(i, ti))
            }
            if (tv != 1f) {
                val scalar = 1f / tv
                m = m.scaleRow(i, scalar)
                moves.add(Scale(scalar, i))
            }
            for (ri in i + 1 until m.rows.size) {
                if (m[ri, i] != 0f) {
                    val rv = m[ri, i]
                    m = m.scaleAddRows(-rv, i, ri)
                    moves.add(Add(-rv, i, ri))
                }
            }
        }
        if (m.parallel().anyMatch { r -> r.all { it == 0f } }) {
            return ReductionResult(m, moves, PARAMETRIC)
        }
        if (m.isReducedEchelonForm()) {
            return ReductionResult(m, moves, REDUCED_ROW_ECHELON)
        }
        if (m.isEchelonForm()) {
            return ReductionResult(m, moves, ROW_ECHELON)
        }
        throw IllegalStateException("Reduction failed.")
    }

    fun reduceMatrixToReducedEchelonForm(matrix: Matrixf): ReductionResult {
        if (matrix.parallel().anyMatch { r -> r.all { it == 0f } }) {
            return ReductionResult(matrix, listOf(), PARAMETRIC)
        }
        if (matrix.isReducedEchelonForm()) {
            return ReductionResult(matrix, listOf(), REDUCED_ROW_ECHELON)
        }
        val echelon = matrix.echelonForm()
        if (echelon.matrix.isReducedEchelonForm()) {
            return echelon
        }
        var m = echelon.matrix
        val moves = echelon.trace.toMutableList()
        for (i in 0 until m.rows.size) {
            val r = m[i]
            for (j in 0 until r.size) {
                val v = m[i, j]
                if (i != j && v != 0f) {
                    m = m.scaleAddRows(-v, j, i)
                    moves.add(Add(-v, j, i))
                }
            }
        }
        if (m.isReducedEchelonForm()) {
            return ReductionResult(m, moves.toList(), REDUCED_ROW_ECHELON)
        }
        throw IllegalStateException("Reduction failed.")
    }
}