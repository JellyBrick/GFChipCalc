package main.util

/**
 *
 * @author Bunnyspa
 * @param <E>
</E> */
class Ref<E>(var v: E) {
    override fun toString(): String {
        return v.toString()
    }
}