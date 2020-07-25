package org.dk14.sla.contract.deadcode.async

import org.dk14.sla.contract.{ActivityStats, SlaService, Sla}

trait ThrottlingServiceASync extends SlaServiceCachedASync :

  import scala.concurrent.Future
  import scala.concurrent.ExecutionContext.Implicits.global
  
  val graceRps: Int
  private val graceSla = Future.successful(Sla("", graceRps))

  val slaService: SlaService
  val activityStats: ActivityStats
  
  def isRequestAllowed (token: Option[String]): Future[Boolean] =
    for
      sla <- token map getCachedSlaByToken getOrElse graceSla
      activityLevel = activityStats.countOverLastActivityWindow(token)
    yield activityLevel > sla.rps / 10
