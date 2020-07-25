package mocks

import org.dk14.sla.contract.ActivityStats

class ActivityStatsMock extends ActivityStats:

  private var time: Int = 0

  def incrementTime(): Unit = time = time + 1

  def countOverLastActivityWindow(token: Option[String]): Int = ???
  
  def close(): Unit = ()
