import org.junit.jupiter.api.Test

internal class DiscreteRandomVariableTest {

    @Test
    fun dieTest() {
        val results = Array(6) {0}
        for (i in 0 until 100) {
            results[DiscreteRandomVariable.die.take() - 1]++
        }
        println(results.joinToString(prefix = "[", separator = ", ", postfix = "]"))
    }

    @Test
    fun weightedDieTest() {
        val results = Array(6) {0}
        for (i in 0 until 100) {
            results[DiscreteRandomVariable.weightedDie.take() - 1]++
        }
        println(results.joinToString(prefix = "[", separator = ", ", postfix = "]"))
    }

    @Test
    fun nCoinsTest() {
        val results = Array(2) {0}
        for (i in 0 until 100) {
            DiscreteRandomVariable.nCoins(10).take().forEach { if (it) { results[1]++ } else { results[0]++ }}
        }
        println("Heads: ${results[1]}, Tails: ${results[0]}")
    }
}