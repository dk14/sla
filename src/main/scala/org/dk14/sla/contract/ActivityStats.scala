package org.dk14.sla.contract

trait ActivityStats:
  
  def countOverLastActivityWindow(token: Option[String]): Int

  def close(): Unit

  
class ActivityStatsImpl extends ActivityStats:
  
  import java.util.concurrent._
  import java.util.concurrent.atomic._
  
  private lazy val scheduler = Executors.newScheduledThreadPool(1);

  import java.util.concurrent.ScheduledFuture

  // GC might not be 100% happy collecting HashMap's every 100ms
  // but it wouldn't be too unhappy either
  private val reset: Runnable = new Runnable: 
    override def run (): Unit = 
      slaMap = createMap()
  
  private lazy val scheduleResetOnFirstCall: ScheduledFuture[_] = 
    scheduler.scheduleAtFixedRate (reset, 0, 100, TimeUnit.MILLISECONDS)

  private def createMap() = new ConcurrentHashMap[Option[String], AtomicInteger]

  @volatile private var slaMap = createMap()

  private def createCounter() = new AtomicInteger(0)

  def countOverLastActivityWindow(token: Option[String]): Int =
    scheduleResetOnFirstCall
    val zero = createCounter() //lazy val + computeIfAbscent could be faster
    val state = Option(slaMap.putIfAbsent(token, zero)).getOrElse(zero)
    state.incrementAndGet()

  def close(): Unit = scala.util.Try {
    scheduleResetOnFirstCall.cancel(true)
    scheduler.shutdown()
  }
