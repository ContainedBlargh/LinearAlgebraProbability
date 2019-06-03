import java.lang.StringBuilder
import java.util.*
import java.util.stream.Collectors
import java.util.stream.IntStream
import java.util.stream.Stream
import kotlin.streams.toList

/**
 * Good times.
 */
class Matrixf(val values: Array<FloatArray>) {
    val m = values.size
    val n = values.first().size

    private fun parallel() = Arrays.stream(values).parallel()
    private fun parallelIndexed() = Arrays.stream(values.mapIndexed { i, r ->
        r.mapIndexed { j, v -> Triple(i, j, v) }.toTypedArray()
    }.toTypedArray()).parallel()

    companion object {
        fun init(n: Int, m: Int, initializer: (Int, Int) -> Float) =
            Matrixf(Array(m) { i -> FloatArray(n) { j -> initializer(i, j) } })

        fun from(n: Int, m: Int, vararg values: Float) =
            Matrixf(Array(m) { i -> values.slice(i * n until i * n + n).toFloatArray() })

        fun fromArray(n: Int, m: Int, values: FloatArray) =
            Matrixf(Array(m) { i -> values.slice(i * n until i * n + n).toFloatArray() })

        fun fromRows(vararg rows: FloatArray) = Matrixf(Array(rows.size, rows::get))

        fun fromRowArray(rows: Array<FloatArray>) = Matrixf(rows)

        fun fromColumns(vararg columns: FloatArray) =
            Matrixf(Array(columns.first().size) { i -> FloatArray(columns.first().size) { j -> columns[j][i] } })

        fun fromColumnArray(columns: Array<FloatArray>) =
            Matrixf(Array(columns.first().size) { i -> FloatArray(columns.first().size) { j -> columns[j][i] } })

        fun identity(n: Int, m: Int) = Matrixf((0 until n).map { i ->
            (0 until m).map { j -> if (i == j) 1.0f else 0.0f }.toFloatArray()
        }.toTypedArray())

        infix operator fun Float.times(matrix: Matrixf) = matrix.map { v -> this * v }
        infix operator fun Double.times(matrix: Matrixf) = matrix.map { v -> (this * v).toFloat() }
        infix operator fun Int.times(matrix: Matrixf) = matrix.map { v -> this * v }
        infix operator fun Long.times(matrix: Matrixf) = matrix.map { v -> this * v }
    }

    operator fun get(i: Int, j: Int) = values[i][j]
    operator fun set(i: Int, j: Int, value: Float) = values[i].set(j, value)

    operator fun get(i: Int) = row(i)
    operator fun set(i: Int, row: FloatArray) = values.set(i, row)

    fun row(r: Int) = values[r].clone()

    fun column(c: Int) = FloatArray(m) { i -> this[i, c] }

    fun map(mapper: (Float) -> Float): Matrixf =
        Matrixf(parallel().map { r -> r.map { v -> mapper(v) }.toFloatArray() }.toList().toTypedArray())

    fun mapIndexed(mapper: (Int, Int, Float) -> Float) =
        Matrixf(parallelIndexed().map { r ->
            r.map { t -> mapper(t.first, t.second, t.third) }.toFloatArray()
        }.toList().toTypedArray())

    fun map2(other: Matrixf, mapper: (Float, Float) -> Float): Matrixf =
        mapIndexed { i, j, v -> mapper(v, other[i, j]) }

    infix operator fun plus(other: Matrixf) =
        map2(other) { v, u -> v + u }

    infix operator fun minus(other: Matrixf) =
        map2(other) { v, u -> v - u }

    infix operator fun times(scalar: Float) = map { v -> scalar * v }
    infix operator fun times(scalar: Double) = map { v -> (scalar * v).toFloat() }
    infix operator fun times(scalar: Int) = map { v -> scalar * v }
    infix operator fun times(scalar: Long) = map { v -> scalar * v }

    infix operator fun div(scalar: Float) = map { v -> (1.0f / scalar) * v }
    infix operator fun div(scalar: Double) = map { v -> ((1.0 / scalar) * v).toFloat() }
    infix operator fun div(scalar: Int) = map { v -> (1.0f / scalar) * v }
    infix operator fun div(scalar: Long) = map { v -> (1.0f / scalar) * v }

    operator fun unaryMinus() = map { v -> -1 * v }

    infix operator fun times(other: Matrixf) = mapIndexed { _, j, v -> (other.column(j).map { u -> v * u }.sum()) }
    infix operator fun times(vector: Vectorf) = this * fromColumns(vector.values)

    fun clone(): Matrixf =
        Matrixf((0 until values.size).map { i -> values[i].clone() }.toTypedArray())

    //EchelonFormatter be praised!!
    fun swapRows(r1: Int, r2: Int): Matrixf {
        val min = Math.min(r1, r2)
        val max = Math.min(r1, r2)
        val start = values.slice(0 until min)
        val middle = values.slice(min + 1 until max)
        val end = values.slice(max + 1 until values.size)
        val combined = listOf(start, listOf(row(max)), middle, listOf(row(min)), end).flatten()
        return Matrixf(combined.toTypedArray())
    }

    fun scaleRow(row: Int, scalar: Float): Matrixf {
        val clone = clone()
        clone.values[row] = (clone.values[row].map { v -> v * scalar }.toFloatArray())
        return clone
    }

    fun scaleAddRows(r1s: Float, r1: Int, r2: Int): Matrixf {
        val scaled = scaleRow(r1, r1s)
        scaled[r2] = scaled[r2].mapIndexed { i, v -> scaled[r1, i] + v }.toFloatArray()
        return scaled
    }

    fun addRows(r1: Int, r2: Int): Matrixf = scaleAddRows(1f, r1, r2)



    override fun toString(): String {
        val stringBuilder = StringBuilder()
        values.fold(stringBuilder) { acc, row ->
            acc.append(
                row.map { v -> String.format("%3.2f", v) }.joinToString(
                    separator = ", ",
                    prefix = "[",
                    postfix = "]\n"
                )
            )
            return@fold acc
        }
        return stringBuilder.toString().trimEnd()
    }
}