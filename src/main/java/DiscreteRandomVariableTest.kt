import org.junit.jupiter.api.Test
import java.util.*
import kotlin.streams.toList
import Probability.fac
import java.util.stream.IntStream
import kotlin.math.pow
import kotlin.test.assertEquals

typealias DRV = DiscreteRandomVariable

internal class DiscreteRandomVariableTest {

    @Test
    fun coinExpectedValueTest() {
        val expected = 0 * 0.5 + 1 * 0.5
        val actual = DRV.coin.expectedValue
        assertEquals(expected, actual)
    }

    @Test
    fun dieTest() {
        val results = Array(6) { 0 }
        for (i in 0 until 100) {
            results[DiscreteRandomVariable.die.take() - 1]++
        }
        println(results.joinToString(prefix = "[", separator = ", ", postfix = "]"))
    }

    @Test
    fun weightedDieTest() {
        val results = Array(6) { 0 }
        for (i in 0 until 100) {
            results[DiscreteRandomVariable.weightedDie.take() - 1]++
        }
        println(results.joinToString(prefix = "[", separator = ", ", postfix = "]"))
    }

    @Test
    fun exc_1_39() {
        //A lecture takes place only if k/n students show up. The probability of each student showing up is p.
        val tests = 10_000_000
        val k = 2
        val n = 6
        val p = 0.5
        val drv = DRV(0..1) {
            when (it) {
                1 -> p
                else -> 1 - p
            }
        }
        val attendance = Array(tests) { mutableMapOf(0 to 0, 1 to 0) }
        IntStream.range(0, tests).parallel().forEach { i ->
            for (s in 0 until n) {
                val r = drv.take()
                attendance[i][r] = attendance[i][r]!! + 1
            }
        }
        val results = Arrays.stream(attendance)
            .map { it[1]!! > k }
            .toList().requireNoNulls()
        val rate = results.sumByDouble { if (it) 1.0 else 0.0 } / (tests.toDouble())
        val nFac = n.fac()
        val pPowK = p.pow(k)
        val pC = 1.0 - p
        val pCPowNMinusK = pC.pow(n - k)
        val top = nFac * pPowK * pCPowNMinusK
        val kFac = k.fac()
        val nMinusKFac = (n - k).fac()
        val btm = kFac * nMinusKFac
        val estimatedRate = top / btm
        println("Test rate: $rate, Estimated rate: $estimatedRate")
    }
}