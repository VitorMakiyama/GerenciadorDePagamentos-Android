package com.makiyamasoftware.gerenciadordepagamentos

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

// Reusable JUnit4 TestRule to override the Main dispatcher
class MainCoroutineRule @OptIn(ExperimentalCoroutinesApi::class) constructor(
    val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    /** This rule uses the StandardTestDispatcher, but could use the UnconfinedTestDispatcher
     Differences:
     * StandardTestDispatcher:
      When you start new coroutines on a StandardTestDispatcher, they are queued up on the underlying scheduler, to be run whenever the test thread
    is free to use. To let these new coroutines run, you need to yield the test thread (free it up for other coroutines to use). This queueing behavior
    gives you precise control over how new coroutines run during the test, and it resembles the scheduling of coroutines in production code.
      There are several ways to yield the test coroutine to let queued-up coroutines run. All of these calls let other coroutines run on the test thread before returning:
        * advanceUntilIdle: Runs all other coroutines on the scheduler until there is nothing left in the queue. This is a good default choice to let all pending
        coroutines run, and it will work in most test scenarios.
        * advanceTimeBy: Advances virtual time by the given amount and runs any coroutines scheduled to run before that point in virtual time.
        * runCurrent: Runs coroutines that are scheduled at the current virtual time.

     * UnconfinedTestDispatcher:
      When new coroutines are started on an UnconfinedTestDispatcher, they are started eagerly on the current thread. This means that they’ll
    start running immediately, without waiting for their coroutine builder to return. In many cases, this dispatching behavior results in simpler
    test code, as you don’t need to manually yield the test thread to let new coroutines run.
      However, this behavior is different from what you’ll see in production with non-test dispatchers. If your test focuses on concurrency, prefer
    using StandardTestDispatcher instead.*/
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}