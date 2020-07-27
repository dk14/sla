Test assignment

Obviously, there are lots of throttlers and load-balancers (nginx, HAProxy so on) in the wild, so I tried to not spend much time re-inventing it.
Few years back, I already had to reinvent Guava as a test assignment: https://github.com/dk14/lifo-set.

Besides, developing a high quality throttler/balancer and testing it would take much more effort than appropriate for TA.

Notes:
- Just to have some fun I decided to use scala3 (aka dotty) - so reading the code might feel a bit unusual (especially without "end" keyword).
- I think, I made few mistakes in the code or tests - nothing is perfect.
- I've sacrificed some neglibible GC time assuming there aren't too many unique users in 100ms window
- Some parts of the TA were hinting at managing traffic with state-machines by using circuit breakers. 
Although it's easy to implement those - just dividing rps by 10 was enough (there are nuances - see code comments).


Assumptions:
- If you're not Facebook or Google - you won't have more than few mils of registered users
  - with no scalability requirements - I assume it's even much less 
  - in any case, throttler can be scaled horizontally by userId
- I block during caching because I consider "cache update" overheads negligible
  - Cache update task gets fork-join pool (with unbounded queue) by default which is often dangerous but okay in here with hourly updates and explicit service time-out. _Note: Scala's `Await.result` has "blocking" annotation by default, so I didn't have to repeat it_.
  - I don't block the throttling threads themselves of course. It's a bit nuanced why - see comments about refresh vs evict in the code.
  - If SlaService times-out - exception is propagated to Caffeine and staled value is not updated (according to their javadocs on CacheLoader)
