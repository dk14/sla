package org.dk14.sla.contract.deadcode.async

import org.dk14.sla.contract.{Sla, SlaService}

trait SlaServiceCachedASync:

  import com.github.blemale.scaffeine.{ AsyncLoadingCache, Scaffeine }
  import scala.concurrent.duration._
  import scala.concurrent._
  
  val slaService: SlaService
  
  val cache: AsyncLoadingCache[String, Sla] =
    Scaffeine()
      .recordStats()
      //this wouldn't block original thread while requesting update
      .refreshAfterWrite(1.hour)
      .maximumSize(100000) //amount of registered users
      .buildAsyncFuture(slaService.getSlaByToken)
  
  def getCachedSlaByToken(token: String): Future[Sla]