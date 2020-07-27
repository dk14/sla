Test assignment

Obviously, there are lots of throttlers and load-balancers (nginx, HAProxy so on) in the wild, so I tried to not spend much time re-inventing it.
Few years back, I already had to reinvent Guava as a test assignment: https://github.com/dk14/lifo-set.

Besides, developing and testing high quality throttler/balancer would take much more effort than appropriate for TA.

Notes:
- Just to have some fun, I decided to use scala3 (aka dotty) - so reading the code might feel a bit unusual (especially without "end" keyword).
- I think, I made few mistakes in the code/tests - nothing is perfect.
- Some parts of the TA were hinting at managing traffic with state-machines by using circuit breakers. 
Although it's easy to implement those - just dividing rps by 10 was good enough (there are nuances - see code comments).
- If I were to implement server app for this, I'd use something like [Blaze](https://github.com/http4s/blaze) for socket NIO + [Gatling](https://gatling.io/) for SLA testing.

Assumptions:
- If you're not Facebook or Google - you won't have more than few mils of registered users
  - with no scalability requirements - I assume it's even much less 
  - in any case, throttler can be scaled horizontally by userId if required
- I've sacrificed some neglibible GC time assuming there aren't too many unique users in 100ms window.
- I block threads during caching because I consider "cache update" overheads negligible
  - Cache update tasks get fork-join pool (with unbounded queue) by default which is often dangerous but actually okay in here with hourly updates and explicit sla-service time-out. _Note: Scala's `Await.result` has "blocking" annotation by default, so I didn't have to repeat it_.
  - I don't block the throttling threads themselves of course. It's a bit nuanced - see comments about refresh vs evict in the code.
- If SlaService times-out - exception is propagated to Caffeine and staled value is not updated (according to their javadocs on CacheLoader)
