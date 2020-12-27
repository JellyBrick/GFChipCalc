package main.util

/**
 *
 * @author Bunnyspa
 */
class Rational {
    private val numerator: Int
    private val denominator: Int

    constructor(n: Int) {
        numerator = n
        denominator = 1
    }

    constructor(n: Int, d: Int) {
        numerator = n
        denominator = d
    }

    val intCeil: Int
        get() {
            var q = numerator / denominator
            if (numerator % denominator > 0) {
                q++
            }
            return q
        }
    val double: Double
        get() {
            val nDouble = numerator.toDouble()
            return nDouble / denominator
        }

    @JvmOverloads
    fun add(n: Int, d: Int = 1): Rational {
        val lcm = lcm(denominator, d)
        val n1 = numerator * (lcm / denominator)
        val n2 = n * (lcm / d)
        return reduce(n1 + n2, lcm)
    }

    fun add(r: Rational): Rational {
        return add(r.numerator, r.denominator)
    }

    @JvmOverloads
    fun mult(n: Int, d: Int = 1): Rational {
        return reduce(numerator * n, denominator * d)
    }

    fun mult(r: Rational): Rational {
        return mult(r.numerator, r.denominator)
    }

    operator fun div(d: Int): Rational {
        return mult(1, d)
    }

    operator fun div(r: Rational): Rational {
        return mult(r.denominator, r.numerator)
    }

    override fun toString(): String {
        return "$numerator/$denominator"
    }

    companion object {
        private fun reduce(n: Int, d: Int): Rational {
            val div = gcd(n, d)
            return Rational(n / div, d / div)
        }

        private fun gcd(a: Int, b: Int): Int {
            if (a == 0) {
                return b
            }
            if (b == 0) {
                return a
            }
            val r = a % b
            return gcd(b, r)
        }

        private fun lcm(a: Int, b: Int): Int {
            return a * b / gcd(a, b)
        }
    }
}