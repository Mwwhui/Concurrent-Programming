package com.mycompany.airportsimulation;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;

public class Gate extends Thread {
    private final int gateId;
    private final Airport airport;
    private final Object gateLock = new Object();
    private final Object readyLock = new Object();
    private Airplane currentPlane;
    private boolean occupied = false;
    private boolean planeReady = false;
    private final Semaphore refuelTruck = new Semaphore(1);

    public Gate(int gateId, Airport airport) {
        this.gateId = gateId;
        this.airport = airport;
        setName("Gate-" + gateId);
    }

    public void assignPlane(Airplane plane) {
        synchronized (gateLock) {
            this.currentPlane = plane;
            this.occupied = true;
            this.planeReady = false;
            gateLock.notify();
        }
    }

    public void signalPlaneReady() {
        synchronized (readyLock) {
            this.planeReady = true;
            readyLock.notify();
        }
    }

    public boolean isAssignedTo(Airplane plane) {
        synchronized (gateLock) {
            return currentPlane == plane;
        }
    }

    public void releasePlane() {
        synchronized (gateLock) {
            this.currentPlane = null;
            this.occupied = false;
            this.planeReady = false;
        }
    }

    public boolean isFree() {
        synchronized (gateLock) {
            return !occupied;
        }
    }

    @Override
    public void run() {
        while (true) {
            synchronized (gateLock) {
                while (currentPlane == null) {
                    try {
                        gateLock.wait();
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }

            synchronized (readyLock) {
                while (!planeReady) {
                    try {
                        readyLock.wait();
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }

            try {
                Airplane plane = currentPlane;
                
                System.out.println(plane.getName() + ": Docked at Gate-" + gateId + ".");
                System.out.println(plane.getName() + ": " + plane.getPassengerCount() + " passengers disembarking.");
                Thread.sleep(plane.getPassengerCount() / 15 * 1000);

                ExecutorService executor = Executors.newFixedThreadPool(2);
                
                Future<?> cleaningTask = executor.submit(() -> {
                    try {
                        System.out.println(plane.getName() + ": Cleaning...");
                        Thread.sleep(1500);
                        System.out.println(plane.getName() + ": Cleaning complete.");
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });

                Future<?> refuelingTask = executor.submit(() -> {
                    try {
                        refuelPlane(plane);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });

                cleaningTask.get();
                refuelingTask.get();
                executor.shutdown();

                int newPassengerCount = new Random().nextInt(50) + 1;
                plane.setDepartingPassengerCount(newPassengerCount);
                System.out.println(plane.getName() + ": " + newPassengerCount + " passengers boarding...");
                Thread.sleep(newPassengerCount / 20 * 1000);

                airport.getATC().releaseGate(gateId, plane);

                synchronized (plane) {
                    plane.notify();
                }

            } catch (Exception e) {
                System.out.println("Gate-" + gateId + ": Error - " + e.getMessage());
                Thread.currentThread().interrupt();
            }
        }
    }

    private void refuelPlane(Airplane plane) throws InterruptedException {
        System.out.println(plane.getName() + ": Waiting for refuel truck...");
        refuelTruck.acquire();
        System.out.println(plane.getName() + ": Refueling...");
        Thread.sleep(1500);
        System.out.println(plane.getName() + ": Refueling complete.");
        refuelTruck.release();
    }
}