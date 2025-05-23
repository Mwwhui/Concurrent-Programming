package com.mycompany.airportsimulation;

import java.util.Random;

public class Airplane extends Thread {
    private final String id;
    private final int initialPassengerCount;
    private int departingPassengerCount;
    private final Airport airport;
    private int gateId;
    private final boolean emergency;
    private long arrivalTime;
    private long waitingStartTime;
    private long waitingEndTime;
    private final Random rand = new Random();

    private final Object landingLock = new Object();
    private final Object takeoffLock = new Object();
    private boolean waitingForLanding = true;
    private boolean clearedForTakeoff = false;

    public Airplane(String id, Airport airport, boolean emergency) {
        this.id = id;
        this.airport = airport;
        this.emergency = emergency;
        this.initialPassengerCount = id.equals("1") ? 10 : rand.nextInt(40) + 10;
        setName("Plane-" + id + (emergency ? "-EMERGENCY" : ""));
    }

    public String getPlaneId() {
        return id;
    }

    public int getPassengerCount() {
        return initialPassengerCount;
    }

    public int getDepartingPassengerCount() {
        return departingPassengerCount;
    }

    public void setDepartingPassengerCount(int count) {
        this.departingPassengerCount = count;
    }

    public long getWaitingTime() {
        return waitingEndTime - waitingStartTime;
    }

    public boolean isEmergency() {
        return emergency;
    }

    public Airport getAirport() {
        return airport;
    }

    public Object getLandingLock() {
        return landingLock;
    }

    public Object getTakeoffLock() {
        return takeoffLock;
    }

    public boolean isWaitingForLanding() {
        return waitingForLanding;
    }

    public boolean isClearedForTakeoff() {
        return clearedForTakeoff;
    }

    public void setClearedForTakeoff(boolean cleared) {
        this.clearedForTakeoff = cleared;
    }

    @Override
    public void run() {
        try {
            arrivalTime = System.currentTimeMillis();
            System.out.println(getName() + ": Requesting landing" + (emergency ? " (EMERGENCY)" : "") + ".");
            waitingStartTime = System.currentTimeMillis();

            airport.acquireAirportCapacity(this, emergency);
            airport.getATC().requestLanding(this, emergency);
            
            synchronized (landingLock) {
                landingLock.wait();
            }

            System.out.println(getName() + ": Landing...");
            Thread.sleep(1000);
            int currentCount = airport.incrementGroundPlanes();
            airport.getATC().reportLanding(this, currentCount);

            gateId = airport.getATC().requestGate(this, emergency);
            airport.getATC().signalRunwayVacated(this);
            waitingEndTime = System.currentTimeMillis();
            waitingForLanding = false;

            System.out.println(getName() + ": Landed successfully after waiting " + getWaitingTime() + "ms.");
            System.out.println(getName() + ": Coasting to Gate-" + gateId + ".");
            Thread.sleep(500);

            airport.signalReadyForGateOperations(this);

            synchronized (this) {
                this.wait();
            }

            airport.getATC().requestTakeoff(this);
            synchronized (takeoffLock) {
                while (!isClearedForTakeoff()) {
                    takeoffLock.wait();
                }
            }

            System.out.println(getName() + ": Taking off...");
            Thread.sleep(1000);
            airport.getATC().signalRunwayVacated(this);
            airport.releaseAirportCapacity(this);
            airport.decrementGroundPlanes(); // ✅ FIX: Track ground count
            System.out.println(getName() + ": Successfully completed all operations.");

        } catch (InterruptedException e) {
            System.out.println(getName() + ": Interrupted - " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }
}
