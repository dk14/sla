package mocks

import org.dk14.sla.contract.ActivityStats

class ActivityStatsMock extends ActivityStats:

  def incrementTime(): Unit = map = Map.empty[Option[String], Int]

  @volatile private var map = Map.empty[Option[String], Int]

  def countOverLastActivityWindow(token: Option[String]): Int =
    val result = map.get(token).getOrElse(0)
    map = map ++ Map(token -> (result + 1))
    result + 1

  def readCounter(token: Option[String]) = map.get(token).getOrElse(0)
  
  def close(): Unit = ()
