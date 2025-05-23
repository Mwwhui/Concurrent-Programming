// AirportSimulation.java
package com.mycompany.airportsimulation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class AirportSimulation {
    private static final AtomicInteger totalPlanes = new AtomicInteger(0);
    private static final AtomicInteger totalPassengers = new AtomicInteger(0);
    
    public static void main(String[] args) throws InterruptedException {
        Airport airport = new Airport();
        List<Airplane> planes = new ArrayList<>();

        System.out.println("=== Airport Simulation Start - Emergency Priority Scenario ===");
        System.out.println("Airport Configuration: 3 gates, max 3 planes on ground (including runway)");
        System.out.println("Scenario: Plane 1 operates and leaves (1 gate free), Planes 2 and 3 occupy 2 gates, Planes 4 and 5 wait, Plane 6 arrives as emergency\n");

        // Create 6 planes - only plane 6 is emergency
        for (int i = 1; i <= 6; i++) {
            boolean isEmergency = (i == 6);
            Airplane plane = new Airplane(String.valueOf(i), airport, isEmergency);
            planes.add(plane);
        }

        // Phase 1: Start Planes 1, 2, and 3
        System.out.println("--- Phase 1: Plane 1 operates, Planes 2 and 3 occupy two gates ---");
        planes.get(0).start(); // Plane 1
        Thread.sleep(800);
        planes.get(1).start(); // Plane 2
        Thread.sleep(5000);
        planes.get(2).start(); // Plane 3
        Thread.sleep(600);

        // Phase 2: Planes 4 and 5 request landing and wait
        System.out.println("\n--- Phase 2: Planes 4 and 5 request landing and wait ---");
        planes.get(3).start(); // Plane 4
        Thread.sleep(500);
        planes.get(4).start(); // Plane 5
        Thread.sleep(200);

        // Phase 3: Emergency Plane 6 arrives
        System.out.println("\n--- Phase 3: EMERGENCY PLANE 6 arrives and gets landing priority ---");
        planes.get(5).start(); // Plane 6 - EMERGENCY

        // Wait for all planes to complete with timeout
        for (Airplane p : planes) {
            try {
                p.join(10000); // Wait up to 10 seconds
                if (p.isAlive()) {
                    System.err.println("Plane-" + p.getName() + " did not complete within timeout.");
                } else {
                    recordPlaneStats(p);
                }
            } catch (InterruptedException e) {
                System.err.println("Main thread interrupted while waiting for Plane-" + p.getName());
            }
        }

        // Print Statistics
        System.out.println("\n=== Final Airport Simulation Statistics ===");
        System.out.println("Total planes served: " + totalPlanes.get());
        System.out.println("Total passengers handled (landed + boarded): " + totalPassengers.get());

        List<Long> waitTimes = new ArrayList<>();
        for (Airplane p : planes) {
            if (!p.isAlive()) {
                waitTimes.add(p.getWaitingTime());
            }
        }

        long totalWait = 0;
        long minWait = Long.MAX_VALUE;
        long maxWait = Long.MIN_VALUE;
        for (Long time : waitTimes) {
            totalWait += time;
            minWait = Math.min(minWait, time);
            maxWait = Math.max(maxWait, time);
        }

        long avgWait = waitTimes.isEmpty() ? 0 : totalWait / waitTimes.size();
        minWait = waitTimes.isEmpty() ? 0 : minWait;
        maxWait = waitTimes.isEmpty() ? 0 : maxWait;

        System.out.println("Average wait time before landing: " + avgWait + " ms");
        System.out.println("Minimum wait time: " + minWait + " ms");
        System.out.println("Maximum wait time: " + maxWait + " ms");
        System.out.println("=== END ===");
    }

    public static void recordPlaneStats(Airplane plane) {
        totalPlanes.incrementAndGet();
        totalPassengers.addAndGet(plane.getPassengerCount() + plane.getDepartingPassengerCount());
    }
}
