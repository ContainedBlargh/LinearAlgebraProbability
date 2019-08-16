import kotlin.math.roundToInt

object Probability {
    private fun factorial(n: Int) = (1L..n.toLong()).reduce(Long::times)

    private fun binomial(n: Int, k: Int) = when {
        n < 0 || k < 0 -> throw IllegalArgumentException("negative numbers not allowed!")
        n == k -> 1L
        else -> {
            var ans = 1L
            for (i in n - k + 1L..n) ans *= i
            ans / factorial(k)
        }
    }

    infix fun Int.choose(k: Int): Long = binomial(this, k)
    fun Int.fac(): Long = factorial(this)
    fun Double.fac(): Long = factorial(this.roundToInt())
}