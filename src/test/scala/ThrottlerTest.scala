
import java.util.concurrent.atomic.AtomicInteger

import org.junit.Test
import org.junit.Assert._
import org.dk14.sla.contract.{ActivityStats, ActivityStatsImpl, Sla, SlaService, ThrottlingServiceSync}
import mocks._

import scala.concurrent.{Future, ExecutionContext, Await}
import scala.concurrent.duration._

class ThrottlerTest:

  def createThrottlingMock(as: ActivityStats) = new ThrottlingServiceSync with SlaServiceCachedSyncMock {
    val graceRps: Int = 100
    val slaService = null
    val activityStats = as
  }

  @Test def test(): Unit =
    val as = new ActivityStatsMock
    val throttler = createThrottlingMock(as)
    val nonBlocked = (0 to 8).forall(_ => throttler.isRequestAllowed(Some("aaa")))
    assertTrue("First few requests not blocked", nonBlocked)
    (0 to 8).foreach(_ => throttler.isRequestAllowed(Some("aaa")))
    assertTrue("Requests are getting blocked after a while", !throttler.isRequestAllowed(Some("aaa")))
    as.incrementTime()
    val nonBlocked2 = (0 to 8).forall(_ => throttler.isRequestAllowed(Some("aaa")))
    assertTrue("round2: First few requests not blocked", nonBlocked2)
    (0 to 8).foreach(_ => throttler.isRequestAllowed(Some("aaa")))
    assertTrue("round2: Requests are getting blocked after a while", !throttler.isRequestAllowed(Some("aaa")))

  @Test def performance(): Unit =

    val k = 100 //rsp
    val n = 10 //users
    val m = 10000 //requests per user
    val tsleep = 1L
    val t = m * tsleep / 1000

    val throttler = new ThrottlingServiceSync:
      val graceRps: Int = 100
      val slaService = new SlaService:
        def getSlaByToken (token: String): Future[Sla] =
          Future.successful(Sla("aaa", k))
      val activityStats = new ActivityStatsImpl

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

    {
      implicit val ec = ExecutionContext.global
      Await.result(Future.sequence(result), Duration.Inf)
    }

    val target = k * n * t
    val count = cnt.get()
    val load = (m / t).toDouble / k

    assertTrue(s"No high load: ${load}x", load > 2.0)

    assertTrue(s"bandwith upper: $count < ${target * 1.4} ", count < target * 1.4)
    assertTrue(s"bandwith lower: $count > ${target * 0.6} ", count > target * 0.6)





