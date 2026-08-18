package com.airport.ui;

/**
 * Panel her gösterildiğinde taze veri yüklemesi gereken controller'lar
 * bu arayüzü uygular.
 */
public interface Refreshable {
    /** Panel ekrana geldiğinde MainWindow tarafından çağrılır. */
    void onPanelShown();
}