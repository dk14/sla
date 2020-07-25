package org.dk14.sla.contract

trait ThrottlingServiceSync extends SlaServiceCachedSync: 

  val graceRps: Int
  val slaService: SlaService
  val activityStats: ActivityStats

  def isRequestAllowed(token: Option[String]): Boolean = 
    val rps = token 
        .flatMap (getCachedSlaByToken) 
        .map (_.rps) 
        .getOrElse(graceRps)
    val activityLevel = activityStats.countOverActivityWindow(token)
    //Ideally, we'd have to use RNG when rps < 10
    // if (rps < 10 && activityLevel = 1) p(allowed) ~ Bernoulli(rps.toDouble / 10)
    //simillar adjustments could be done to account for rounding
    //It's not too pragmatic though - so we won't do that:
    activityLevel < rps / 10 
  
  def close(): Unit = activityStats.close()

