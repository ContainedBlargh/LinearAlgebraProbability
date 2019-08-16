import java.util.concurrent.ThreadLocalRandom

/**
 * A simple class for Discrete Random Variables.
 */
open class DiscreteRandomVariable(sampleSpace: IntRange, val probabilityMassFunction: (Int) -> Double) {

    private var pMax: Double = 0.0

    private val expansion = sampleSpace.map { it to probabilityMassFunction(it) }

    private val pmf = run {
        pMax = expansion.sumByDouble { it.second }
        if (pMax < 1.0 - 0.1 || pMax > 1.1) {
            throw IllegalArgumentException(
                "Cannot initialize discrete random variable with infeasible PMF, " +
                        "make sure that the probabilities given by the PMF sum to about 1."
            )
        }
        val accumulated = expansion.fold(Pair<Double, List<Pair<Double, Int>>>(0.0, emptyList())) { (a, l), (t, p) ->
            Pair(a + p, listOf(Pair(a + p, t)) + l)
        }.second.reversed()
        return@run { p: Double ->
            accumulated.first { it.first > p }.second
        }
    }

    val expectedValue = sampleSpace.map { it * probabilityMassFunction(it) }.sum()

    open fun take(): Int = pmf(random.nextDouble(pMax))


    /**
     * A companion object that supplies a variety of common discrete random variables.
     */
    companion object {
        private val random = ThreadLocalRandom.current()
        val coin: DiscreteRandomVariable = DiscreteRandomVariable(0..1) { 0.5 }
        val die: DiscreteRandomVariable = DiscreteRandomVariable(1..6) { 1.0 / 6.0 }
        val weightedDie: DiscreteRandomVariable = DiscreteRandomVariable(1..6) {
            when (it) {
                6 -> 4.0 / 9.0
                else -> 1.0 / 9.0
            }
        }
        private val nCoinsCache = mutableMapOf<Int, DiscreteRandomVariable>()
    }

}