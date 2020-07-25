
import java.util.concurrent.atomic.AtomicInteger

import org.junit.Test
import org.junit.Assert._
import org.dk14.sla.contract.{ActivityStats, ActivityStatsImpl, Sla, SlaService, ThrottlingServiceSync}
import mocks._

import scala.concurrent.{Future, ExecutionContext, Await}
import scala.concurrent.duration._


class ThrottlerTest:
//It might make sense to disable parallel test run for JUnit to get better metrics

//-----------Behaviour---------------------

  def createThrottlingMock(as: ActivityStats) = new ThrottlingServiceSync with SlaServiceCachedSyncMock {
    val graceRps: Int = 100
    val slaService = null
    val activityStats = as
    val rps = 100
  }

  @Test def behaviour(): Unit =
    val as = new ActivityStatsMock
    val throttler = createThrottlingMock(as)

    val nonBlocked = (0 to 8).forall(_ => throttler.isRequestAllowed(Some("aaa")))
    assertTrue("First few requests should not be blocked", nonBlocked)

    (0 to 8).foreach(_ => throttler.isRequestAllowed(Some("aaa")))
    assertTrue("Requests should start getting blocked after a while", 
      !throttler.isRequestAllowed(Some("aaa")))
    
    as.incrementTime() //this is 100ms increment, that's why 100rps is 10rpms above

    val nonBlocked2 = (0 to 8).forall(_ => throttler.isRequestAllowed(Some("aaa")))
    assertTrue("round2: First few requests should not be blocked", nonBlocked2)

    (0 to 8).foreach(_ => throttler.isRequestAllowed(Some("aaa")))
    assertTrue("round2: Requests should start getting blocked after a while", 
      !throttler.isRequestAllowed(Some("aaa")))


//-----------Performance---------------------

  def createRealIshThrottler(k : Int) = new ThrottlingServiceSync:
    val graceRps: Int = 100
    val slaService = new SlaService:
      def getSlaByToken (token: String): Future[Sla] =
        Future {
          Thread.sleep(200) //simulate slow configuration service
          Sla("aaa", k)
        }(ExecutionContext.global)
    val activityStats = new ActivityStatsImpl

  val k = 100 //rsp
  val n = 8 //users, reasonable number depends on amount of cores in your system
  val m = 10000 //requests per user
  val tsleep = 1L
  val t = m * tsleep / 1000 //run-time in seconds

  /**
   * ∀k n t. stats = get_stats(run(k, n, t)) -> stats.allowed_requests < k * n * t +- eps
   */
  @Test def performance(): Unit =

    val throttler = createRealIshThrottler(k)

    val cnt = new AtomicInteger(0)

    val result = (0 to n).map { userId =>
      Future {
        (0 to m) foreach { _ =>
          if throttler.isRequestAllowed(Some(userId.toString))
          then cnt.incrementAndGet()
          Thread.sleep(tsleep)
        }
      }(ExecutionContext.global)
    }

    { //hiding implicit with braces
      implicit val ec = ExecutionContext.global
      Await.result(Future.sequence(result), Duration.Inf)
    }

    val target = k * n * t
    val count = cnt.get()
    val load = (m / t).toDouble / k

    assertTrue(s"No high load: ${load}x", load > 4.0) //sanity check

    //we assume null hypothesis with something around 9X% quantile, and p(x) ~ Normal(target)
    assertTrue(s"bandwith upper: $count < ${target * 1.7} ", count < target * 1.4)
    assertTrue(s"bandwith lower: $count > ${target * 0.3} ", count > target * 0.6)

  @Test def overhead(): Unit =

    val z = 10000
    val throttler = createRealIshThrottler(k)

    val start = System.currentTimeMillis()
    (0 to z).foreach(_ => throttler.isRequestAllowed(Some((z % 100).toString)))
    val end = System.currentTimeMillis()
    
    val overhead = (end - start).toDouble / z
    
    //this shows that throttling overhead is less than 0.05ms
    assertTrue(s"Overhead: $overhead ms", overhead < 0.05)


//-----------Logic--------------

  /**
   * We ensure that semantics of the throttler follow semantics of the specification here.
   * It's a bit boring and a bit unnecessary, given that we deal with concurrency.
   * Besides I use mocked version of activity stats.
   * 
   * In general I'd use property based checking like scala-test for this.
   * I'd also make state explicit (st, st') to avoid overthinking behavioral aspects.
   * 
   * ∀ user st. st' = check_allowed(user, st) -> count(user, st') = count(user, st) + 1
   * 
   * This can be shown by randomly sampling `user`, `st`.
   * We can also prove it by induction/simplification on `st`` (no need for induction on `user`)
   * 
   * Below, I just show the property by example:
   */
  @Test def spec() =

    val as = new ActivityStatsMock
    val throttler = createThrottlingMock(as)
    assertTrue("user1 req count is 0", 
      as.readCounter(Some("user1")) == 0)

    throttler.isRequestAllowed(Some("user1"))

    assertTrue(s"user1 req count is 1",
      as.readCounter(Some("user1")) == 1)

    //--------

    assertTrue("user2 req count is 0",
      as.readCounter(Some("user2")) == 0)

    throttler.isRequestAllowed(Some("user2"))

    assertTrue("user2 req count is 1",
      as.readCounter(Some("user2")) == 1)

    //--------

    assertTrue("nouser req count is 0",
      as.readCounter(None) == 0)
    
    throttler.isRequestAllowed(None)

    assertTrue("nouser req count is 1",
      as.readCounter(None) == 1)

    
    


