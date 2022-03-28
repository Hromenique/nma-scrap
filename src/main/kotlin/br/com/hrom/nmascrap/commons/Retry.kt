package br.com.hrom.nmascrap.commons

fun <R> retryOnError(retryWorker: RetryWorker = BasicRetryWorker(), work: () -> R):R {
    return retryWorker.retry(work)
}

interface RetryWorker {
    fun <R> retry(work: () -> R): R
}

class BasicRetryWorker(private val maxAttempts: Int = 3, private val delay:Long = 1000): RetryWorker {

    override fun <R> retry(work: () -> R): R {
        var attempts = 0

        while (true) {
            try {
                return work.invoke()
            } catch (ex: Throwable) {
                attempts++
                if (attempts >= maxAttempts) {
                    throw ex;
                }
                Thread.sleep(delay)
            }
        }
    }
}
