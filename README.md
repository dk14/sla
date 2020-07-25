#Test assignment

Obviously, there are lots of throttlers and load-balancers (nginx, HAProxy so on) in the wild, so I tried to not spend much time re-inventing it.
Few years back, I had to reinvent Guava as a test assignment already: https://github.com/dk14/lifo-set.

Besides, developing a high quality throttler/balancer and testing it would take a while and much more effort than appropriate for TA.

Notes:
- Just to have some fun I decided to use scala3 (aka dotty). 
- I didn't use quantiles and confidence intervals in the test, but generally using those would be a good practice.
- I likely made few mistakes in the code or tests, I think... idk.
- I've sacrificed some GC time assuming there aren't many unique users in 100ms window

Assumptions:
- If you're not facebook or google - you won't have more than few mil of registered users.
With no scalability requirements - I assume it's even much less. 
- so full-cache of their SLA should fit the memory.
- I block during caching because I consider "cache update" overheads negligible. That's given that I don't block the throttler itself of course (it's a bit nuanced - see comments in the code)


