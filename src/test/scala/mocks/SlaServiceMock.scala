package mocks

import org.dk14.sla.contract.{Sla, SlaServiceCachedSync}

trait SlaServiceCachedSyncMock extends SlaServiceCachedSync:
  override def getCachedSlaByToken(token: String): Option[Sla] = Some(Sla("aaa", 100))

    
