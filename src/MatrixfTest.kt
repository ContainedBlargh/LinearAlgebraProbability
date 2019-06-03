import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import Matrixf.Companion.times
import java.util.concurrent.ThreadLocalRandom
import java.util.stream.IntStream
import kotlin.system.measureNanoTime

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
    fun indexTimesIndex() {
        println(((3 * identity) * (3 * identity)) / 9)
    }

    fun randomMatrixf(n: Int, m: Int): Matrixf {
        val random = ThreadLocalRandom.current()
        fun next() = random.nextGaussian().toFloat()
        return Matrixf.init(n, m) { i, j -> next() }
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
        IntStream.range(0, 1000).parallel().forEach {
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
            "BuildTimes:" +
                    "\nMax: ${buildTimes.max()!! / mio}" +
                    "\nMin: ${buildTimes.min()!! / mio}" +
                    "\nMedian: ${buildTimes[buildTimes.size / 2] / mio}" +
                    "\nAvg.: ${buildTimes.average() / mio}" +
                    "\nMulTimes:\nMax: ${mulTimes.max()!! / mio}\nMin: ${mulTimes.min()!! / mio}" +
                    "\nMedian: ${mulTimes[mulTimes.size / 2] / mio}\nAvg: ${mulTimes.average() / mio}"
        println(message)
    }
}