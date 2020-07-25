package org.dk14.sla.contract

import scala.concurrent.Future

final case class Sla (user: String, rps: Int)

trait SlaService:

  def getSlaByToken (token:String): Future[Sla]

trait SlaServiceCachedSync:

  import com.github.blemale.scaffeine.{ LoadingCache, Scaffeine }
  import scala.concurrent.duration._
  import scala.concurrent._

  val slaService: SlaService

  private def await[T](f: Future[T]) = Await.result(f, 5.seconds)

  private val cache: LoadingCache[String, Sla] =
    Scaffeine()
      .recordStats()
      //this wouldn't block original thread while requesting update
      .refreshAfterWrite(1.hour) 
      .maximumSize(100000) //amount of registered users
      .build(slaService.getSlaByToken andThen await)

  //if value is not ready we'll treat user as unauthorized for few milliseconds
  //it's very very unlikely to get blocked because of this (it would have to combine with some other bug)
  def getCachedSlaByToken(token:String): Option[Sla] = Option(cache.get(token))
