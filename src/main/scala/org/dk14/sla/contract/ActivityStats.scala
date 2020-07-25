package org.dk14.sla.contract

trait ActivityStats:

  //the activity window is 1/10th of a second, while SLA is per second
  def countOverLastActivityWindow(token: Option[String]): Int

  def close(): Unit
  
class ActivityStatsImpl extends ActivityStats:

//In imaginary case of activity spikes 
// with more than 1mil of unique users per second 
// we could use Caffeine instead, but unbounded Map should be good enough

  import java.util.concurrent._
  import java.util.concurrent.atomic._
  
  private lazy val scheduler = Executors.newScheduledThreadPool(1);

  import java.util.concurrent.ScheduledFuture

  // GC might not be 100% happy collecting HashMap's every 1/10th of a second
  // but it wouldn't be too unhappy either
  private val reset: Runnable = new Runnable: 
    override def run (): Unit = 
      slaMap = initMap()
  
  private lazy val scheduleResetOnFirstCall: ScheduledFuture[_] = 
    scheduler.scheduleAtFixedRate (reset, 0, 100, TimeUnit.MILLISECONDS)

  private def initMap() = new ConcurrentHashMap[Option[String], AtomicInteger]

  @volatile private var slaMap = initMap()

  private def initCounter() = new AtomicInteger(0)

  def countOverLastActivityWindow(token: Option[String]): Int =
    scheduleResetOnFirstCall
    slaMap.putIfAbsent(token, initCounter())
    val state = slaMap.get(token)
    state.incrementAndGet()

  def close(): Unit = scala.util.Try {
    scheduleResetOnFirstCall.cancel(true)
    scheduler.shutdown()
  }
