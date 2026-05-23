package io.systemtest.container.cases.complex;

/**
 * Test support type for CustomerSegmentService.
 *
 * @author Mustapha Zouari
 */
interface CustomerSegmentService {

  boolean isPremium(Long customerId);
}
