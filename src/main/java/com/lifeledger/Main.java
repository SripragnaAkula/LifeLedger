package com.lifeledger;

/**
 * Main entry point for LifeLedger.
 * Separating this from LifeLedgerApp avoids JavaFX module issues when running as a fat JAR.
 */
public class Main {
    public static void main(String[] args) {
        LifeLedgerApp.main(args);
    }
}
