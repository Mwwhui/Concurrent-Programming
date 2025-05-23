package com.mycompany.airportsimulation;

import java.util.ArrayDeque;
import java.util.Queue;

public class ATC extends Thread {
    private final Queue<Airplane> emergencyLandingQueue = new ArrayDeque<>();
    private final Queue<Airplane> normalLandingQueue = new ArrayDeque<>();
    private final Queue<Airplane> takeoffQueue = new ArrayDeque<>();
    private final Object lock = new Object();
    private final Airport airport;
    private boolean runwayFree = true;

    public ATC(Airport airport) {
        this.airport = airport;
        setName("ATC");
    }

    @Override
    public void run() {
        while (true) {
            synchronized (lock) {
                if (runwayFree) {
                    Airplane plane = null;
                    String operation = "";
                    
                    if (!emergencyLandingQueue.isEmpty()) {
                        plane = emergencyLandingQueue.poll();
                        operation = "Emergency landing";
                        System.out.println("ATC: Emergency landing granted to " + plane.getName());
                    } else if (!normalLandingQueue.isEmpty()) {
                        plane = normalLandingQueue.poll();
                        operation = "Landing";
                        System.out.println("ATC: Landing granted to " + plane.getName());
                    } else if (!takeoffQueue.isEmpty()) {
                        plane = takeoffQueue.poll();
                        operation = "Takeoff";
                        System.out.println("ATC: Takeoff granted to " + plane.getName());
                    }

                    if (plane != null) {
                        runwayFree = false;
                        if (plane.isWaitingForLanding()) {
                            synchronized (plane.getLandingLock()) {
                                plane.getLandingLock().notify();
                            }
                        } else {
                            plane.setClearedForTakeoff(true);
                            synchronized (plane.getTakeoffLock()) {
                                plane.getTakeoffLock().notify();
                            }
                        }
                    }
                }
                try {
                    lock.wait(100);
                } catch (InterruptedException ignored) {}
            }
        }
    }

    public void requestLanding(Airplane plane, boolean isEmergency) {
        synchronized (lock) {
            if (isEmergency) {
                emergencyLandingQueue.add(plane);
            } else {
                normalLandingQueue.add(plane);
            }
            lock.notifyAll();
        }
    }

    public void requestTakeoff(Airplane plane) {
        synchronized (lock) {
            takeoffQueue.add(plane);
            lock.notifyAll();
        }
    }

    public void reportLanding(Airplane plane, int currentCount) {
        System.out.println("ATC: " + plane.getName() + " has landed. Current capacity " + currentCount + "/3");
    }

    public void signalRunwayVacated(Airplane plane) {
        synchronized (lock) {
            System.out.println("ATC: Runway vacated by " + plane.getName());
            runwayFree = true;
            if (!plane.isWaitingForLanding()) {
                plane.setClearedForTakeoff(false);
            }
            lock.notifyAll();
        }
    }

    public int requestGate(Airplane plane, boolean isEmergency) throws InterruptedException {
        synchronized (airport.getGateLock()) {
            while (true) {
                for (int i = 0; i < airport.getGates().length; i++) {
                    if (airport.getGates()[i].isFree()) {
                        System.out.println("ATC: Gate-" + i + " assigned for " + plane.getName());
                        airport.getGates()[i].assignPlane(plane);
                        return i;
                    }
                }
                System.out.println("ATC: No gates available for " + plane.getName() + ", waiting...");
                airport.getGateLock().wait();
            }
        }
    }

    public void releaseGate(int gateId, Airplane plane) {
        synchronized (airport.getGateLock()) {
            airport.getGates()[gateId].releasePlane();
            System.out.println("ATC: " + plane.getName() + " left Gate-" + gateId);
            airport.getGateLock().notifyAll();
        }
    }
}


