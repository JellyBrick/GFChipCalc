package main.util

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

object ThreadPoolManager {
    val threadPool: ExecutorService = Executors.newCachedThreadPool()
}