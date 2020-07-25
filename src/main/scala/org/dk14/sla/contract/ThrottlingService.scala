package org.dk14.sla.contract

trait ThrottlingServiceSync extends SlaServiceCachedSync: 

  val graceRps: Int
  val slaService: SlaService
  val activityStats: ActivityStats

  def isRequestAllowed (token: Option[String]): Boolean = 
    val rps = token 
        .flatMap (getCachedSlaByToken) 
        .map (_.rps) 
        .getOrElse(graceRps)
    val activityLevel = activityStats.countOverLastActivityWindow(token)
    activityLevel < rps / 10
  
  def close(): Unit = activityStats.close()

