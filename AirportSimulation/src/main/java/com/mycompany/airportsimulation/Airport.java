package com.mycompany.airportsimulation;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

public class Airport {
    private static final int TOTAL_GATES = 3;
    private static final int AIRPORT_CAPACITY = 3;

    private final Gate[] gates = new Gate[TOTAL_GATES];
    private final Object gateLock = new Object();
    private final ATC atc = new ATC(this);
    final Semaphore airportCapacity = new Semaphore(AIRPORT_CAPACITY, true);
    private final Object capacityLock = new Object();
    private final Queue<Airplane> emergencyLandingQueue = new ArrayDeque<>();
    private final Queue<Airplane> normalLandingQueue = new ArrayDeque<>();
    private final AtomicInteger currentGroundPlanes = new AtomicInteger(0);

    public Airport() {
        for (int i = 0; i < TOTAL_GATES; i++) {
            gates[i] = new Gate(i, this);
            gates[i].start();
        }
        atc.start();
    }

    public void signalReadyForGateOperations(Airplane plane) {
        for (Gate gate : gates) {
            if (gate.isAssignedTo(plane)) {
                gate.signalPlaneReady();
                break;
            }
        }
    }

    public void acquireAirportCapacity(Airplane plane, boolean isEmergency) throws InterruptedException {
        synchronized (capacityLock) {
            (isEmergency ? emergencyLandingQueue : normalLandingQueue).add(plane);
            while (true) {
                boolean atHead = isEmergency
                        ? emergencyLandingQueue.peek() == plane
                        : emergencyLandingQueue.isEmpty() && normalLandingQueue.peek() == plane;
                if (atHead && airportCapacity.availablePermits() > 0) {
                    airportCapacity.acquire();
                    (isEmergency ? emergencyLandingQueue : normalLandingQueue).remove();
                    break;
                }
                if (atHead && airportCapacity.availablePermits() == 0) {
                    System.out.println(plane.getName() + ": Waiting for ground space - airport at full capacity (3/3)");
                }
                capacityLock.wait();
            }
        }
    }

    public void releaseAirportCapacity(Airplane plane) {
        airportCapacity.release();
        synchronized (capacityLock) {
            capacityLock.notifyAll();
        }
    }

    public int incrementGroundPlanes() {
        return currentGroundPlanes.incrementAndGet();
    }

    public int decrementGroundPlanes() {
        return currentGroundPlanes.decrementAndGet();
    }

    public int getCurrentGroundPlanes() {
        return currentGroundPlanes.get();
    }

    public Gate[] getGates() {
        return gates;
    }

    public Object getGateLock() {
        return gateLock;
    }

    public ATC getATC() {
        return atc;
    }
}
