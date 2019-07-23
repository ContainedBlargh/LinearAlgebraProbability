import java.util.concurrent.ThreadLocalRandom

/**
 * A simple class for Discrete Random Variables.
 */
open class DiscreteRandomVariable<T>(sampleSpace: Set<T>, probabilityMassFunction: (T) -> Double) {

    private var pMax: Double = 0.0

    private val pmf = run {
        val expansion = sampleSpace.map { it to probabilityMassFunction(it) }
        pMax = expansion.sumByDouble { it.second }
        if (pMax < 1.0 - 0.1 || pMax > 1.1) {
            throw IllegalArgumentException(
                "Cannot initialize discrete random variable with infeasible PMF, " +
                        "make sure that the probabilities given by the PMF sum to about 1."
            )
        }
        val accumulated = expansion.fold(Pair<Double, List<Pair<Double, T>>>(0.0, emptyList())) { (a, l), (t, p) ->
            Pair(a + p, listOf(Pair(a + p, t)) + l)
        }.second.reversed()
        return@run { p: Double ->
            accumulated.first { it.first > p }.second
        }
    }

    open fun take(): T = pmf(random.nextDouble(pMax))

    /**
     * A companion object that supplies a variety of common discrete random variables.
     */
    companion object {
        private val random = ThreadLocalRandom.current()
        val coin: DiscreteRandomVariable<Boolean> = DiscreteRandomVariable(setOf(true, false)) { 0.5 }
        val die: DiscreteRandomVariable<Int> = DiscreteRandomVariable((1..6).toSet()) { 1.0 / 6.0 }
        val weightedDie: DiscreteRandomVariable<Int> = DiscreteRandomVariable((1..6).toSet()) {
            when (it) {
                6 -> 4.0 / 9.0
                else -> 1.0 / 9.0
            }
        }
        private val nCoinsCache = mutableMapOf<Int, DiscreteRandomVariable<List<Boolean>>>()
        fun nCoins(n: Int): DiscreteRandomVariable<List<Boolean>> = nCoinsCache.computeIfAbsent(n) { i ->
            //I wish I could figure out a better solution.
            object : DiscreteRandomVariable<List<Boolean>>(setOf(listOf(true, false)), { 1.0 }) {
                val coins = List(i) { coin }
                override fun take(): List<Boolean> {
                    return coins.map { it.take() }
                }
            }
        }
    }

}