import GaussJordan.ReductionResultType.REDUCED_ROW_ECHELON
import java.lang.StringBuilder
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.streams.toList

/**
 * Good times.
 */
class Matrixf(val rows: Array<FloatArray>) {
    val m = rows.size //number of rows
    val n = rows.first().size //number of columns

    private var echelonForm: GaussJordan.ReductionResult? = null
    private var reducedEchelonForm: GaussJordan.ReductionResult? = null

    fun parallel() = Arrays.stream(rows).parallel()
    fun parallelIndexed() = Arrays.stream(rows.mapIndexed { i, r ->
        Pair(i, r.mapIndexed { j, v -> Pair(j, v) }.toTypedArray())
    }.toTypedArray()).parallel()

    companion object {

        class MatrixfException(msg: String) : Exception(msg)

        fun init(n: Int, m: Int, initializer: (Int, Int) -> Float) =
            Matrixf(Array(m) { j -> FloatArray(n) { i -> initializer(i, j) } })

        fun from(n: Int, m: Int, vararg values: Float) =
            Matrixf(Array(m) { i -> values.slice(i * n until i * n + n).toFloatArray() })

        fun fromArray(n: Int, m: Int, values: FloatArray) =
            Matrixf(Array(m) { i -> values.slice(i * n until i * n + n).toFloatArray() })

        fun fromRows(vararg rows: FloatArray) = Matrixf(Array(rows.size, rows::get))

        fun fromRows(vararg rows: IntArray) =
            Matrixf(Array(rows.size) { i -> rows[i].map { it.toFloat() }.toFloatArray() })

        fun fromRowArray(rows: Array<FloatArray>) = Matrixf(rows)

        fun fromRowArray(rows: Array<IntArray>) =
            Matrixf(Array(rows.size) { i -> rows[i].map { it.toFloat() }.toFloatArray() })

        fun fromColumns(vararg columns: FloatArray) =
            Matrixf(Array(columns.first().size) { i -> FloatArray(columns.first().size) { j -> columns[j][i] } })

        fun fromColumns(vararg columns: IntArray) =
            Matrixf(Array(columns.first().size) { i -> FloatArray(columns.first().size) { j -> columns[j][i].toFloat() } })

        fun fromColumnArray(columns: Array<IntArray>) =
            Matrixf(Array(columns.first().size) { i -> FloatArray(columns.first().size) { j -> columns[j][i].toFloat() } })

        fun identity(n: Int, m: Int) = Matrixf((0 until n).map { i ->
            (0 until m).map { j -> if (i == j) 1.0f else 0.0f }.toFloatArray()
        }.toTypedArray())

        infix operator fun Float.times(matrix: Matrixf) = matrix.map { v -> this * v }
        infix operator fun Double.times(matrix: Matrixf) = matrix.map { v -> (this * v).toFloat() }
        infix operator fun Int.times(matrix: Matrixf) = matrix.map { v -> this * v }
        infix operator fun Long.times(matrix: Matrixf) = matrix.map { v -> this * v }
    }

    operator fun get(i: Int, j: Int) = rows[i][j]
    operator fun set(i: Int, j: Int, value: Float) = rows[i].set(j, value)

    operator fun get(i: Int) = row(i)
    operator fun set(i: Int, row: FloatArray) = rows.set(i, row)

    fun row(r: Int) = rows[r].clone()

    fun column(c: Int) = FloatArray(m) { i -> this[i, c] }

    fun map(mapper: (Float) -> Float): Matrixf =
        Matrixf(parallel().map { r -> r.map { v -> mapper(v) }.toFloatArray() }.toList().toTypedArray())

    fun mapIndexed(mapper: (Int, Int, Float) -> Float) =
        Matrixf(parallelIndexed().map { (i, r) ->
            r.map { (j, v) -> mapper(i, j, v) }.toFloatArray()
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

    infix operator fun times(other: Matrixf): Matrixf {
        val out = identity(this.n, other.m)
        parallelIndexed().forEach { (i, r) ->
            for (j in 0 until other.m) {
                out[i, j] = 0f
                r.forEach { (k, v) ->
                    out[i, j] += v * other[k, j]
                }
            }
        }
        return out
    }

    infix operator fun times(vector: Vectorf) = this * fromColumns(vector.values)

    fun clone(): Matrixf =
        Matrixf((0 until rows.size).map { i -> rows[i].clone() }.toTypedArray())

    //GaussJordan be praised!!
    fun swapRows(r1: Int, r2: Int): Matrixf = clone().mapIndexed { i, j, v ->
        return@mapIndexed when (i) {
            r1 -> this[r2, j]
            r2 -> this[r1, j]
            else -> v
        }
    }

    fun scaleRow(row: Int, scalar: Float): Matrixf =
        clone().mapIndexed { i, _, v ->
            when (i) {
                row -> v * scalar
                else -> v
            }
        }

    fun scaleAddRows(r1s: Float, r1: Int, r2: Int): Matrixf =
        clone().mapIndexed { i, j, v ->
            when (i) {
                r2 -> (r1s * this[r1, j]) + v
                else -> v
            }
        }

    fun addRows(r1: Int, r2: Int): Matrixf = scaleAddRows(1f, r1, r2)

    fun echelonForm(): GaussJordan.ReductionResult {
        if (echelonForm == null) {
            echelonForm = GaussJordan.reduceMatrixToEchelon(this)
        }
        return echelonForm!!
    }

    fun isDiagonal(): Boolean {
        val result = AtomicBoolean(true)
        parallelIndexed().forEach { (i, r) ->
            r.forEach { (j, v) ->
                when {
                    !result.get() -> return@forEach
                    (i != j) && v != 0f -> result.compareAndExchange(true, false)
                }
            }
        }
        return result.get()
    }

    fun diagonal() = FloatArray(min(n, m)) {
        this[it, it]
    }

    fun isEchelonForm(): Boolean {
        val result = AtomicBoolean(true)
        parallelIndexed().forEach { (i, r) ->
            r.forEach { (j, v) ->
                when {
                    !result.get() -> return@forEach
                    i == j && v != 1f -> result.compareAndExchange(true, false)
                    i > j && v != 0f -> result.compareAndExchange(true, false)
                }
            }
        }
        return result.get()
    }

    fun reducedEchelonForm(): GaussJordan.ReductionResult {
        if (reducedEchelonForm == null) {
            reducedEchelonForm = GaussJordan.reduceMatrixToReducedEchelonForm(this)
        }
        return reducedEchelonForm!!
    }

    fun isReducedEchelonForm(): Boolean {
        val result = AtomicBoolean(true)
        parallelIndexed().forEach { (i, r) ->
            r.forEach { (j, v) ->
                when {
                    !result.get() -> return@forEach
                    i != j && v != 0f -> result.compareAndExchange(true, false)
                    i == j && v != 1f -> result.compareAndExchange(true, false)
                }
            }
        }
        return result.get()
    }

    fun determinant(): Float {
        if (n != m) {
            throw MatrixfException("Determinants for non-square matrices not defined.")
        }
        if (n == 2) {
            return (this[0, 0] * this[1, 1]) - (this[0, 1] * this[1, 0])
        }
        val ech = reducedEchelonForm()
        if (ech.matrix.parallel().anyMatch { r -> r.all { it == 0f } }) {
            return 0f
        }
        return 1f / ech.trace.foldRight(1f) { move, acc ->
            when (move) {
                is GaussJordan.Swap -> acc * -1
                is GaussJordan.Scale -> acc * move.scalar
                else -> acc
            }
        }
    }

    fun inverse(): Matrixf? {
        if (determinant() == 0f) {
            return null
        }
        val reduced = reducedEchelonForm()
        return when (reduced.resultType) {
            REDUCED_ROW_ECHELON -> reduced.trace.fold(identity(n, m)) { acc, move -> GaussJordan.applyMove(acc, move) }
            else -> null
        }
    }

    fun adjoin(matrix: Matrixf): Matrixf =
        fromRowArray(rows.zip(matrix.rows).map { it.first + it.second }.toTypedArray())

    fun adjoin(vector: Vectorf): Matrixf {
        val vecMat = fromArray(1, vector.size, vector.values)
        return adjoin(vecMat)
    }

    fun eigenvalues(): FloatArray {
        if (n != m) {
            throw MatrixfException("Cannot deduce eigenvalues for non-square matrices.")
        }
        if (isDiagonal()) {
            return diagonal()
        }
        if (n != 2) {
            throw MatrixfException("Can only deduce eigenvectors for 2 by 2 matrices.")
        }
        //2 x 2 matrix case:
        // (λ - m00) * (λ - m11) - m01 * m10
        // = λ^2 - λ*m11 - m00*λ + m00*m11 - m01 *m10
        // a = 1
        // b = -m11 - m00
        // c = m00*m11 - m01*m10
        //λ_1 = (-b + \sqrt(b^2-4*c))/2
        //λ_2 = (-b - \sqrt(b^2-4*c))/2
        //λ^2 - b*λ +
        val b = -this[1, 1] - this[0, 0]
        val c = this[0, 0] * this[1, 1] - this[0, 1] * this[1, 0]
        val d = (b * b) - (4f * c)
        val l1 = (-b + sqrt(d)) / 2
        val l2 = (-b - sqrt(d)) / 2
        return floatArrayOf(l1, l2)
    }

    fun eigenvectors(): List<Vectorf> {
        if (isDiagonal()) {
            return rows.map { r -> Vectorf(r.map { if (it != 0f) it / it else 0f }.toFloatArray()) }
        }
        if (n != m) {
            throw MatrixfException("Cannot deduce eigenvalues for non-square matrices.")
        }
        if (n > 2) {
            throw MatrixfException("Can only deduce eigenvectors for 2 by 2 matrices and *some* 3 by 3 matrices.")
        }
        fun deduceVector(l: Float): Vectorf {
            val characteristicMatrix = (l * identity(n, m)) - this
            val top = characteristicMatrix[0]
            val f = top.first()
            val s = top.last()
            val canScale = f > 1f && s > 1f && (s % f) == 0f && (f % s) == 0f
            val canInvert = f < 0 && s < 0
            return when {
                canScale && canInvert -> Vectorf.from(f / s, -s / f) * -1
                canScale -> Vectorf.from(f / s, -s / f)
                canInvert -> Vectorf.from(f, -s) * -1
                else -> Vectorf.from(f, -s)
            }
        }
        return eigenvalues().map(::deduceVector)
    }

    override fun toString(): String {
        val stringBuilder = StringBuilder()
        rows.fold(stringBuilder) { acc, row ->
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Matrixf) return false

        if (!rows.contentDeepEquals(other.rows)) return false
        if (m != other.m) return false
        if (n != other.n) return false

        return true
    }

    override fun hashCode(): Int {
        var result = rows.contentDeepHashCode()
        result = 31 * result + m
        result = 31 * result + n
        return result
    }
}