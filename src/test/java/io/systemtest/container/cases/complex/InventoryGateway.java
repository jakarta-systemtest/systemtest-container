package io.systemtest.container.cases.complex;

/**
 * Test support type for InventoryGateway.
 *
 * @author Mustapha Zouari
 */
interface InventoryGateway {

  boolean isAvailable(String sku);
}
