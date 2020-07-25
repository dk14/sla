
import org.junit.Test
import org.junit.Assert._

import org.dk14.sla.contract.ThrottlingServiceSync
import mocks._

class TestLogic:
  
  def createThrottlingMock = new ThrottlingServiceSync with SlaServiceCachedSyncMock {
    val graceRps: Int = 100
    val slaService = null
    val activityStats = new ActivityStatsMock()
  }
  
  @Test def test(): Unit =
    val throttler = createThrottlingMock
    val nonBlocked = (0 to 100).forall(_ => !throttler.isRequestAllowed(Some("aaa")))
    assertTrue("First few request not blocked", nonBlocked)
    //check blocked
    //move clock
    //check not blocked again
    //check blocked
  
