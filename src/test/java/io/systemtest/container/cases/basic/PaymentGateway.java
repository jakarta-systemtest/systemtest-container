package io.systemtest.container.cases.basic;

/**
 * Test support type for PaymentGateway.
 *
 * @author Mustapha Zouari
 */
interface PaymentGateway {

  String authorize(String roomCode);
}
