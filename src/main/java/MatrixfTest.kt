import Matrixf.Companion.times
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.ThreadLocalRandom
import java.util.stream.IntStream
import kotlin.system.measureNanoTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class MatrixfTest {

    @BeforeEach
    private fun setUp() {

    }

    val identity = Matrixf.identity(4, 4)

    @Test
    fun printTest() {
        println(Matrixf.identity(4, 4).toString())
    }

    @Test
    fun fromColumnsTest() {
        println(Matrixf.fromColumns(floatArrayOf(1f, 2f), floatArrayOf(2f, 1f), floatArrayOf()))
    }

    @Test
    fun fromRowsTest() {
        println(Matrixf.fromRows(floatArrayOf(1f, 2f, 3f), floatArrayOf(2f, 3f, 1f), floatArrayOf(3f, 2f, 1f)))
    }

    @Test
    fun identityTimesIdentity() {
        assertEquals(identity, identity * identity)
    }

    @Test
    fun indexTimesIndex() {
        println(((3 * identity) * (3 * identity)) / 9)
    }

    @Test
    fun isEchelonFormReturnsTrueForIdentityTest() {
        val matrix = Matrixf.identity(4, 4)
        assertTrue(matrix.isEchelonForm())
    }

    @Test
    fun isReducedEchelonFormReturnsFalseForNonIdentity() {
        val matrix = Matrixf.identity(1000, 1000) * 2
        var isEchelonForm = true;
        println("isEchelonForm: ${measureNanoTime { isEchelonForm = matrix.isReducedEchelonForm() } / 1_000_000f}ms")
        assertFalse(isEchelonForm)
    }

    @Test
    fun echelonTest() {
        val matrix = Matrixf.fromColumns(floatArrayOf(1f, 2f, 3f), floatArrayOf(0f, 2f, 4f), floatArrayOf(1f, 3f, 0f))
        println(matrix)
        val echelon = matrix.echelonForm()
        println(echelon.matrix)
    }

    @Test
    fun echelonTest_chp1_ex2() {
        val matrix = Matrixf.fromRows(intArrayOf(0, 1, 3, 4), intArrayOf(-1, 2, 0, 3), intArrayOf(2, -3, 4, 1))
        println(matrix)
        val echelon = matrix.echelonForm()
        println(echelon.matrix)
        println("Trace length: ${echelon.trace.size}")
    }

    @Test
    fun determinantAndInverseTest() {
        val matrix = Matrixf.fromRows(intArrayOf(1, 1, 3), intArrayOf(1, 2, 4), intArrayOf(1, 1, 2))
        println("Original:")
        println(matrix)
        val echelon = matrix.echelonForm()
        println("Echelon form:")
        println(echelon.matrix)
        println("Determinant:\n${matrix.determinant()}")
        println("Trace length: ${echelon.trace.size}\n${echelon.trace.fold(StringBuilder()) { acc, move ->
            acc.appendln(
                move
            )
        }}")
        val invertedManually =
            matrix.reducedEchelonForm().trace.fold(Pair(matrix, Matrixf.identity(matrix.n, matrix.m))) { acc, move ->
                Pair(GaussJordan.applyMove(acc.first, move), GaussJordan.applyMove(acc.second, move))
            }
        println("Inverted manually:")
        println(invertedManually)
        println("Inverse:")
        val inverse = matrix.inverse()
        println(inverse)
        println("Original * Inverse")
        println(matrix * inverse!!)
        println("ID * Inverse")
        println(Matrixf.identity(matrix.n, matrix.m) * inverse!!)
    }

    @Test
    fun inverseTest() {
        val matrix = Matrixf.from(
            2, 2,
            -1f, 2f,
            -1f, 1f
        )
        println("Matrix:\n$matrix")
        val reduced = matrix.reducedEchelonForm()
        println("Reduced:\n${reduced.matrix}\nWith Steps:\n${reduced.trace}")
        val inverse = matrix.inverse()!!
        println("Inverse:\n$inverse")
        val matrixMulInverse = matrix * inverse
        println("Original times inverse:\n$matrixMulInverse")
        assertEquals(Matrixf.identity(2, 2), matrixMulInverse)
    }

    fun randomMatrixf(n: Int, m: Int): Matrixf {
        val random = ThreadLocalRandom.current()
        fun next() = random.nextGaussian().toFloat()
        return Matrixf.init(n, m) { _, _ -> next() }
    }

    fun randomReducibleMatrix(n: Int, m: Int): Matrixf {
        val random = ThreadLocalRandom.current()
        fun next() = random.nextGaussian().toFloat()
        var current = Matrixf.init(n, m) { _, _ -> next() }
        while (kotlin.runCatching { current.reducedEchelonForm() }.isFailure) {
        }
        return current.clone()
    }

    @Test
    fun reductionPerfTest() {
        val mio = 1_000_00f
        val n = 10
        val reductionTimes: MutableList<Long> = mutableListOf()
        val matrices = Array(n) { randomReducibleMatrix(n, n) }
        IntStream.range(0, n).parallel().forEach {
            reductionTimes.add(measureNanoTime {
                matrices[it].reducedEchelonForm()
            })
        }
        reductionTimes.sort()
        val message =
            "Size: ${n * n}\n" +
                    "Reduction Times:" +
                    "\nMax: ${reductionTimes.max()!! / mio}ms" +
                    "\nMin: ${reductionTimes.min()!! / mio}ms" +
                    "\nMedian: ${reductionTimes[reductionTimes.size / 2] / mio}ms" +
                    "\nAvg.: ${reductionTimes.average() / mio}ms"
        println(message)
    }

    @Test
    fun complexMultiplication() {
        val mio = 1_000_000f
        val n = 200
        val buildTimes: MutableList<Long> = mutableListOf()
        val mulTimes: MutableList<Long> = mutableListOf()
        IntStream.range(0, 100).parallel().forEach {
            randomMatrixf(n, n) * randomMatrixf(n, n)
        }
        IntStream.range(0, 100).parallel().forEach {
            var matrixf: Matrixf? = null
            buildTimes.add(measureNanoTime {
                matrixf = randomMatrixf(n, n)
            })
            mulTimes.add(measureNanoTime {
                matrixf!!.times(matrixf!!)
            })
        }
        mulTimes.sort()
        buildTimes.sort()
        val message =
            "Size: ${n * n}\n" +
                    "BuildTimes:" +
                    "\nMax: ${buildTimes.max()!! / mio}ms" +
                    "\nMin: ${buildTimes.min()!! / mio}ms" +
                    "\nMedian: ${buildTimes[buildTimes.size / 2] / mio}ms" +
                    "\nAvg.: ${buildTimes.average() / mio}ms" +
                    "\nMulTimes:\nMax: ${mulTimes.max()!! / mio}ms\nMin: ${mulTimes.min()!! / mio}ms" +
                    "\nMedian: ${mulTimes[mulTimes.size / 2] / mio}ms\nAvg: ${mulTimes.average() / mio}ms"
        println(message)
    }

    @Test
    fun adjoinTest() {
        val mat = Matrixf.fromRows(
            floatArrayOf(1f, 1f, 1f, 1f),
            floatArrayOf(2f, 3f, 0f, -1f),
            floatArrayOf(-3f, 4f, -1f, 1f),
            floatArrayOf(1f, 2f, -1f, 1f)
        )
        val mat2 = Matrixf.fromRows(
            floatArrayOf(1f, 1f, 1f, 1f),
            floatArrayOf(2f, 3f, 0f, -1f),
            floatArrayOf(-3f, 4f, -1f, 1f),
            floatArrayOf(1f, 2f, -1f, 1f)
        )
        val newRows = mat.rows.zip(mat2.rows).map { it.first + it.second }.toTypedArray()
        val expected = Matrixf.fromRowArray(newRows)
        val adj = mat.adjoin(mat2)
        println(adj)
        assertEquals(expected, adj)
    }

    @Test
    fun exc_1_1_55() {
        val mat = Matrixf.fromRows(
            floatArrayOf(1f, 1f, 1f, 1f),
            floatArrayOf(2f, 3f, 0f, -1f),
            floatArrayOf(-3f, 4f, -1f, 1f),
            floatArrayOf(1f, 2f, -1f, 1f)
        )
        val reduced = mat.reducedEchelonForm()
        val solVector = Vectorf.from(6f, 0f, 4f, 0f)
        val backtraced = reduced.backtrace(solVector)
        println(backtraced.second)
        println(backtraced.first)
    }

    @Test
    fun exc_1_3_1() {
        val mat = Matrixf.fromRows(
            floatArrayOf(1f, 2f, 4f),
            floatArrayOf(1f, 3f, 9f),
            floatArrayOf(1f, 4f, 16f)
        )
        val reduced = mat.reducedEchelonForm()
        val backtraced = reduced.backtrace(Vectorf.from(5f, 2f, 5f))
        println(backtraced.second)
        println(backtraced.first)
    }

    @Test
    fun exc_2_3_13() {
        val mat = Matrixf.fromRows(
            intArrayOf(1, 1, 1),
            intArrayOf(3, 5, 4),
            intArrayOf(3, 6, 5)
        )
        val inverse = mat.inverse()
        println(inverse)
    }

    @Test
    fun eigenvaluesTest() {
        val mat = Matrixf.from(2, 2, 2f, -2f, 1f, 5f)
        val lambdas = mat.eigenvalues()
        assertTrue("Computed eigenvalues are as expected") {
            lambdas.contains(4f) && lambdas.contains(3f)
        }
    }

    @Test
    fun eigenvectorsTest() {
        val mat = Matrixf.from(2, 2, 2f, -2f, 1f, 5f)
        val vectors = mat.eigenvectors()
        val expectedFirst = Vectorf.from(1, -1)
        val expectedLast = Vectorf.from(1, -2)
        assertTrue("Computed eigenvectors are as expected") {
            vectors.contains(expectedFirst) && vectors.contains(expectedLast)
        }
    }

    @Test
    fun eigenvectorTest2() {
        val mat = Matrixf.from(2, 2, 2f, 1f, 1f, 2f)
        val λ1 = 1f
        val λ2 = 3f
        val values = mat.eigenvalues()
        assertTrue() {
            values.contains(λ1) && values.contains(λ2)
        }
        val expectedFirst = Vectorf.from(1, -1)
        val expectedSecond = Vectorf.from(1f, 1f)
        val vectors = mat.eigenvectors()
        assertTrue("Computed eigenvectors contain expected") {
            vectors.contains(expectedFirst) && vectors.contains(expectedSecond)
        }
    }
}