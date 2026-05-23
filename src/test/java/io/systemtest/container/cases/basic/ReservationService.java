package io.systemtest.container.cases.basic;

import jakarta.inject.Inject;

/**
 * Test support type for ReservationService.
 *
 * @author Mustapha Zouari
 */
final class ReservationService {

  private final PaymentGateway paymentGateway;

  @Inject
  ReservationService(PaymentGateway paymentGateway) {
    this.paymentGateway = paymentGateway;
  }

  String reserve(String roomCode) {
    return roomCode + ":" + paymentGateway.authorize(roomCode);
  }
}
