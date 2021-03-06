package main.util;

import org.jetbrains.annotations.NotNull;

/**
 *
 * @author Bunnyspa
 */
public class Rational {

    private final int numerator, denominator;

    public Rational(int n) {
        this.numerator = n;
        this.denominator = 1;
    }

    public Rational(int n, int d) {
        this.numerator = n;
        this.denominator = d;
    }

    public int getIntFloor() {
        return numerator / denominator;
    }

    public int getIntCeil() {
        int q = numerator / denominator;
        if (numerator % denominator > 0) {
            q++;
        }
        return q;
    }

    public double getDouble() {
        return (double) numerator / denominator;
    }

    @NotNull
    public Rational add(int n, int d) {
        int lcm = lcm(denominator, d);
        int n1 = numerator * (lcm / denominator);
        int n2 = n * (lcm / d);
        return reduce(n1 + n2, lcm);
    }

    @NotNull
    public Rational add(int n) {
        return add(n, 1);
    }

    @NotNull
    public Rational add(@NotNull Rational r) {
        return add(r.numerator, r.denominator);
    }

    @NotNull
    public Rational mult(int n, int d) {
        return reduce(this.numerator * n, this.denominator * d);
    }

    @NotNull
    public Rational mult(int n) {
        return mult(n, 1);
    }

    @NotNull
    public Rational mult(@NotNull Rational r) {
        return mult(r.numerator, r.denominator);
    }

    @NotNull
    public Rational div(int d) {
        return mult(1, d);
    }

    @NotNull
    public Rational div(@NotNull Rational r) {
        return mult(r.denominator, r.numerator);
    }

    @NotNull
    private static Rational reduce(int n, int d) {
        int div = gcd(n, d);
        return new Rational(n / div, d / div);
    }

    private static int gcd(int a, int b) {
        if (a == 0) {
            return b;
        }
        if (b == 0) {
            return a;
        }
        int r = a % b;
        return gcd(b, r);
    }

    private static int lcm(int a, int b) {
        return a * b / gcd(a, b);
    }

    @NotNull
    @Override
    public String toString() {
        return numerator + "/" + denominator;
    }
}
