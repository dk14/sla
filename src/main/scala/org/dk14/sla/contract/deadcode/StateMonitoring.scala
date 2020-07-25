package org.dk14.sla.contract.deadcode

/*
  This requirement is ambigous:
  """SLA should be counted by intervals of 1/10 second (i.e. if RPS
  limit is reached, after 1/10 second ThrottlingService should allow
  10% more requests)"""
  
  1) It doesn't say if we should check SLA when allowing 10% requests
  2) It is not clear wether I should introduce blocked state vs throttled 
  3) It is not clear if I'm to 
     a) increase load incrementally by blocking/unblocking every 10%
     -or -
     b) use Idle state with rejectionRate and randomly reject 90%, 80% etc
     
     Those sum up to quite different traffic
     
  In any case - I'm doing blocking/unblocking above
  And here I just present state-machine for option (b), 
  although I'm not gonna use anywhere


 */

trait StateMonitoring:

  enum ActivityState:
    case Active
    case Throttled(rejectionRate: Int)
    case Blocked
  
  import ActivityState._
  
  //penalty strategy could differ
  def stateTransition(threshold: Int, activityLevel: Int, state: ActivityState) =
    state match
      case Active if activityLevel > threshold => Blocked
      case Active if activityLevel <= threshold => Active
      case Blocked if activityLevel > threshold => Blocked
      case Blocked if activityLevel <= threshold => Throttled(90)
      case Throttled(0) => Active
      case Throttled(n) if activityLevel > threshold => Throttled(math.max(100, n + 10))
      case Throttled(n) if activityLevel <= threshold => Throttled(math.min(0, n - 10))



