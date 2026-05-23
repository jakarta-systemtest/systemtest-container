package io.systemtest.container.cases.complex;

/**
 * Test support type for NotificationPort.
 *
 * @author Mustapha Zouari
 */
interface NotificationPort {

  void notifyCustomer(Long customerId, String message);
}
