package io.systemtest.container.cases.complex;

/**
 * Test support type for DefaultCustomerSegmentService.
 *
 * @author Mustapha Zouari
 */
final class DefaultCustomerSegmentService implements CustomerSegmentService {

  @Override
  public boolean isPremium(Long customerId) {
    return customerId % 2 == 0;
  }
}
