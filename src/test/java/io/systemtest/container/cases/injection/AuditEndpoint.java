package io.systemtest.container.cases.injection;

import jakarta.inject.Inject;

/**
 * Test support type for AuditEndpoint.
 *
 * @author Mustapha Zouari
 */
final class AuditEndpoint {

  @Inject private AuditTrail auditTrail;

  boolean isReady() {
    return auditTrail != null;
  }
}
