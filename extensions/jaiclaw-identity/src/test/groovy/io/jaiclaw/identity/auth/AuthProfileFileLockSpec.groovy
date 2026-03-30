package io.jaiclaw.identity.auth

import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Path
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Supplier

class AuthProfileFileLockSpec extends Specification {

    @TempDir
    Path tempDir

    def "withLock executes action and returns result"() {
        given:
        Path lockFile = tempDir.resolve("test.lock")

        when:
        String result = AuthProfileFileLock.withLock(lockFile, { "hello" } as Supplier<String>)

        then:
        result == "hello"
    }

    def "withLock void version executes action"() {
        given:
        Path lockFile = tempDir.resolve("test.lock")
        AtomicInteger counter = new AtomicInteger(0)

        when:
        AuthProfileFileLock.withLock(lockFile, { counter.incrementAndGet() } as Runnable)

        then:
        counter.get() == 1
    }

    def "withLock ensures mutual exclusion"() {
        given:
        Path lockFile = tempDir.resolve("mutex.lock")
        AtomicInteger concurrentCount = new AtomicInteger(0)
        AtomicInteger maxConcurrent = new AtomicInteger(0)
        int threadCount = 10
        CountDownLatch latch = new CountDownLatch(threadCount)

        when:
        List<Thread> threads = (1..threadCount).collect {
            Thread.startVirtualThread {
                AuthProfileFileLock.withLock(lockFile, {
                    int current = concurrentCount.incrementAndGet()
                    maxConcurrent.updateAndGet { int max -> Math.max(max, current) }
                    Thread.sleep(10)
                    concurrentCount.decrementAndGet()
                    latch.countDown()
                } as Runnable)
            }
        }
        latch.await()

        then:
        maxConcurrent.get() == 1
    }

    def "withLock propagates exceptions from action"() {
        given:
        Path lockFile = tempDir.resolve("error.lock")

        when:
        AuthProfileFileLock.withLock(lockFile, { throw new RuntimeException("test error") } as Supplier)

        then:
        RuntimeException e = thrown()
        e.message == "test error"
    }
}
