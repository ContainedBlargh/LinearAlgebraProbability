import kotlin.math.acos
import kotlin.math.sqrt

class Vectorf(val values: FloatArray) {
    companion object {
        fun from(vararg floats: Float) = Vectorf(floats)
    }

    infix operator fun get(index: Int) = values[index]

    infix operator fun plus(other: Vectorf) = Vectorf(values.mapIndexed { i, v -> v + other[i] }.toFloatArray())

    infix operator fun minus(other: Vectorf) = Vectorf(values.mapIndexed { i, v -> v - other[i] }.toFloatArray())

    infix operator fun times(scalar: Float) = Vectorf(values.map { v -> v * scalar }.toFloatArray())

    infix operator fun times(matrix: Matrixf) = Matrixf.fromRows(values) * matrix

    fun magnitude() = sqrt(values.sum())

    infix operator fun times(other: Vectorf) = values.mapIndexed { i, v -> v * other[i] }.sum()

    fun angle(other: Vectorf): Float {
        val dot = this * other
        val magProduct = (magnitude() * other.magnitude())
        return acos(dot / magProduct)
    }

    infix fun x(other: Vectorf): Vectorf {
        fun folder(acc: Pair<Float, Boolean>, i: Int): Pair<Float, Boolean> {
            return Pair(acc.first * (if (acc.second) this[i] else other[i]), !acc.second)
        }
        return Vectorf(values.mapIndexed { i, _ ->
            val indices = (0 until values.size)
                .filter { index -> index != i }
            val ownAlternating = indices.fold(Pair(1.0f, true), ::folder).first
            val otherAlternating = indices.fold(Pair(1.0f, false), ::folder).first
            return@mapIndexed ownAlternating - otherAlternating
        }.toFloatArray())
    }

}