import EchelonFormatter.Outcome.*
import java.io.Serializable

object EchelonFormatter {

    private open class Move : Serializable

    private class Scale(val scalar: Float, val row: Int) : Move()

    private class Swap(val r1: Int, val r2: Int) : Move()

    private class Add(val scalar: Float, val r1: Int, val r2: Int) : Move()

    private fun moves(matrix: Matrixf): List<Move> {
        val moves = mutableListOf<Move>()
        matrix.values.forEachIndexed { i, r1 ->
            moves.add(Scale(1 / r1[i], i))
            matrix.values.forEachIndexed { j, r2 ->
                if (i != j) {
                    for (c in 0 until r1.size) {
                        when {
                            c == i && r1[c] == 1f -> return emptyList()
                            c == j && r2[c] == 1f -> return emptyList()
                            c == i && (r1[c] - r2[c]) == 1f -> moves.add(Add(-1f, i, j))
                            c == j && (r2[c] - r1[c]) == 1f -> moves.add(Add(-1f, j, i))
                            /*
                             * Take care of case where we have a value in a column that we don't want a value in.
                             * This means only the values below the diagonal. These must be empty.
                             */
                        }
                        moves.add(Add(r2[c] / r1[c], i, j))
                        moves.add(Add(r1[c] / r2[c], i, j))
                        moves.add(Swap(i, j))
                    }
                }
            }
        }
        return moves
    }

    class MatrixOutcome(val matrix: Matrixf, val outcome: Outcome)

    enum class Outcome {
        ECHELON,
        PARAMETRIC_SOLUTION_REQUIRED,
        NOT_DONE
    }

    private fun isTriangularized(matrix: Matrixf): Boolean =
        (0 until matrix.values[0].size)
            .all { i -> (i until matrix.values.size).all { j -> matrix[i, j] == 0f } }

    private fun isEchelonForm(matrix: Matrixf): Boolean =
        isTriangularized(matrix) && (0 until matrix.values.size).all { i -> matrix[i, i] == 1f }


    private fun isReducedEchelonForm(matrix: Matrixf): Outcome =
        when {
            matrix.values.map { r -> r.all { v -> v == 0f } }.filter { it }.count() > 1 -> PARAMETRIC_SOLUTION_REQUIRED
            isEchelonForm(matrix) && (0 until matrix.values[0].size)
                .all { i -> (0 until i).all { j -> matrix[i, j] == 0f } } -> ECHELON
            else -> NOT_DONE
        }

    private fun utlity(matrix: Matrixf, move: Move): Float {
        return Float.NEGATIVE_INFINITY
    }

    /*
     * SO, the idea here is to use a search algorithm to find the a row-reduced version of the matrix.
     * I want it to follow the way i usually think about gaussian elimination/reduction, but it should also be workable.
     * I used our expectiminimax for inspiration:
     * <a href="https://github.itu.dk/WingIT/othello-ai1/blob/master/src/OthelloAI1.java"/>
     */

    fun triangularizeMatrix(matrix: Matrixf): MatrixOutcome {
        return MatrixOutcome(matrix, NOT_DONE)
    }
}