package io.systemtest.container.cases.basic;

import jakarta.inject.Inject;

/**
 * Test support type for ReservationEndpoint.
 *
 * @author Mustapha Zouari
 */
final class ReservationEndpoint {

  private final ReservationService reservationService;

  @Inject
  ReservationEndpoint(ReservationService reservationService) {
    this.reservationService = reservationService;
  }

  String reserve(String roomCode) {
    return reservationService.reserve(roomCode);
  }
}
