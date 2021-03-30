import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Arrays;

// Main class
@SuppressWarnings({"NonAtomicOperationOnVolatileField", "SpellCheckingInspection", "FieldMayBeFinal", "IfStatementWithIdenticalBranches", "FieldCanBeLocal"})
public class dve_elevator2_1 {
    // The objects in the model
    private final SLCO_Class[] objects;

    // Lock class to handle locks of global variables
    private static class LockManager {
        // The locks
        private final ReentrantLock[] locks;

        LockManager(int noVariables) {
            locks = new ReentrantLock[noVariables];
            for(int i = 0; i < locks.length; i++) {
                locks[i] = new ReentrantLock(false);
            }
        }

        // Lock method
        void lock(LockRequest[] lock_ids, int start, int end) {
            int i = start;
            Arrays.sort(lock_ids, start, end);
            for (; i < end; i++) {
                if(lock_ids[i].isValid()) {
                    locks[lock_ids[i].getIdentity()].lock();
                }
            }
        }

        // Unlock method
        void unlock(LockRequest[] lock_ids, int end) {
            for (int i = 0; i < end; i++) {
                if(lock_ids[i].isValid()) {
                    locks[lock_ids[i].getIdentity()].unlock();
                }
            }
        }

        // Unlock method during exceptions
        void exception_unlock() {
            System.err.println("Exception encountered. Releasing all locks currently owned by " + Thread.currentThread().getName() + ".");
            for(ReentrantLock lock: locks) {
                while(lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        }
    }

    // A representation of a lock request that can be used to validate the validity thereof.
    private static class LockRequest implements Comparable<LockRequest> {
        private int identity;
        private boolean validity;

        boolean isValid() {
            return validity;
        }

        int getIdentity() {
            return identity;
        }

        void set(int identity, int offset, int length) {
            this.identity = identity + offset;
            this.validity = offset >= 0 && offset < length;
        }

        @Override
        public int compareTo(LockRequest request) {
            return this.identity - request.identity;
        }
    }

    // Template for SLCO classes
    interface SLCO_Class {
        void startThreads();
        void joinThreads();
    }

    // Representation of the SLCO class GlobalClass
    private static class GlobalClass implements SLCO_Class {
        // The threads
        private final cabinThread T_cabin;
        private final environmentThread T_environment;
        private final controllerThread T_controller;

        // Global variables
        private volatile byte v; // Lock id 0
        private volatile int t; // Lock id 1
        private volatile int p; // Lock id 2
        private final byte[] req; // Lock id 3

        // Define the states fot the state machine cabin
        interface GlobalClass_cabinThread_States {
            enum States {
                idle, mov, open
            }
        }

        // Representation of the SLCO state machine cabin
        class cabinThread extends Thread implements GlobalClass_cabinThread_States {
            // Current state
            private cabinThread.States currentState;

            // Counter of main while-loop iterations
            long transitionCounter;
            long successfulTransitionCounter;
            long transitionCounteridle;
            long successfulTransitionCounteridle;
            long transitionCountermov;
            long successfulTransitionCountermov;
            long transitionCounteropen;
            long successfulTransitionCounteropen;

            // The lock manager
            private final LockManager lockManager;

            // A list of lock ids that can be reused
            private final LockRequest[] lock_ids;

            cabinThread (LockManager lockManagerInstance) {
                lockManager = lockManagerInstance;
                lock_ids = new LockRequest[4];
                for (int i = 0; i < lock_ids.length; i++) {
                    lock_ids[i] = new LockRequest();
                }
                currentState = cabinThread.States.idle;
            }

            private void exec_idle() {
                transitionCounteridle++;
                // from idle to mov {v > 0}
                lock_ids[0].set(0, 0, 1); // Acquire v
                lockManager.lock(lock_ids, 0, 1);
                if(v <= 0) {
                    lockManager.unlock(lock_ids, 1);
                    return;
                }
                lockManager.unlock(lock_ids, 1);
                currentState = cabinThread.States.mov;
                successfulTransitionCounteridle++;
                successfulTransitionCounter++;
            }

            private void exec_mov() {
                transitionCountermov++;
                lock_ids[0].set(1, 0, 1); // Acquire t
                lock_ids[1].set(2, 0, 1); // Acquire p
                lockManager.lock(lock_ids, 0, 2);
                if (t < p) {
                    // from mov to mov {[t < p; p := p - 1]}
                    p = p - 1;
                    lockManager.unlock(lock_ids, 2);
                    currentState = cabinThread.States.mov;
                } else if(t > p) {
                    // from mov to mov {[t > p; p := p + 1]}
                    p = p + 1;
                    lockManager.unlock(lock_ids, 2);
                    currentState = cabinThread.States.mov;
                } else {
                    // from mov to open {t = p}
                    lockManager.unlock(lock_ids, 2);
                    currentState = cabinThread.States.open;
                }
                successfulTransitionCountermov++;
                successfulTransitionCounter++;
            }

            private void exec_open() {
                transitionCounteropen++;
                // from open to idle {[req[p] := 0; v := 0]}
                lock_ids[0].set(0, 0, 1); // Acquire v
                lock_ids[1].set(2, 0, 1); // Acquire p
                lockManager.lock(lock_ids, 0, 2);
                lock_ids[2].set(3, p, 4); // Acquire req[p]
                lockManager.lock(lock_ids, 2, 3);
                req[p] = (byte) (0);
                v = (byte) (0);
                lockManager.unlock(lock_ids, 3);
                currentState = cabinThread.States.idle;
                successfulTransitionCounteropen++;
                successfulTransitionCounter++;
            }

            // Execute method
            private void exec() {
                long startTime = System.nanoTime();
                long stopTime = System.currentTimeMillis() + 1000L * 10;
                while(stopTime > System.currentTimeMillis()) {
                    switch(currentState) {
                        case idle -> exec_idle();
                        case mov -> exec_mov();
                        case open -> exec_open();
                    }

                    // Increment counter
                    transitionCounter++;
                }
                long running_time = System.nanoTime() - startTime;
                System.out.println(
                    "GlobalClass.cabin;" +
                    successfulTransitionCounter + ";" +
                    transitionCounter + ";" +
                    (double)successfulTransitionCounter/transitionCounter + ";" +
                    running_time + ";"
                );
                System.out.println(
                    "GlobalClass.cabin.idle;" +
                    successfulTransitionCounteridle + ";" +
                    transitionCounteridle + ";" +
                    (double)successfulTransitionCounteridle/transitionCounteridle + ";" +
                    running_time + ";"
                );
                System.out.println(
                    "GlobalClass.cabin.mov;" +
                    successfulTransitionCountermov + ";" +
                    transitionCountermov + ";" +
                    (double)successfulTransitionCountermov/transitionCountermov + ";" +
                    running_time + ";"
                );
                System.out.println(
                    "GlobalClass.cabin.open;" +
                    successfulTransitionCounteropen + ";" +
                    transitionCounteropen + ";" +
                    (double)successfulTransitionCounteropen/transitionCounteropen + ";" +
                    running_time + ";"
                );
            }

            // Run method
            public void run() {
                try {
                    exec();
                } catch(Exception e) {
                    lockManager.exception_unlock();
                    throw e;
                }
            }
        }

        // Representation of the SLCO state machine environment
        class environmentThread extends Thread {
            // Random number generator to handle non-determinism
            private final Random random;

            // Counter of main while-loop iterations
            long transitionCounter;
            long successfulTransitionCounter;
            long transitionCounterread;
            long successfulTransitionCounterread;

            // The lock manager
            private final LockManager lockManager;

            // A list of lock ids that can be reused
            private final LockRequest[] lock_ids;

            environmentThread (LockManager lockManagerInstance) {
                random = new Random();
                lockManager = lockManagerInstance;
                lock_ids = new LockRequest[4];
                for (int i = 0; i < lock_ids.length; i++) {
                    lock_ids[i] = new LockRequest();
                }
            }

            private void exec_read() {
                transitionCounterread++;
                switch(random.nextInt(4)) {
                    case 0 -> {
                        // from read to read {[req[1] = 0; req[1] := 1]}
                        lock_ids[0].set(3, 1, 4); // Acquire req[1]
                        lockManager.lock(lock_ids, 0, 1);
                        if(req[1] != 0) {
                            lockManager.unlock(lock_ids, 1);
                            return;
                        }
                        req[1] = (byte) (1);
                        lockManager.unlock(lock_ids, 1);
                    }
                    case 1 -> {
                        // from read to read {[req[0] = 0; req[0] := 1]}
                        lock_ids[0].set(3, 0, 4); // Acquire req[0]
                        lockManager.lock(lock_ids, 0, 1);
                        if(req[0] != 0) {
                            lockManager.unlock(lock_ids, 1);
                            return;
                        }
                        req[0] = (byte) (1);
                        lockManager.unlock(lock_ids, 1);
                    }
                    case 2 -> {
                        // from read to read {[req[2] = 0; req[2] := 1]}
                        lock_ids[0].set(3, 2, 4); // Acquire req[2]
                        lockManager.lock(lock_ids, 0, 1);
                        if(req[2] != 0) {
                            lockManager.unlock(lock_ids, 1);
                            return;
                        }
                        req[2] = (byte) (1);
                        lockManager.unlock(lock_ids, 1);
                    }
                    case 3 -> {
                        // from read to read {[req[3] = 0; req[3] := 1]}
                        lock_ids[0].set(3, 3, 4); // Acquire req[3]
                        lockManager.lock(lock_ids, 0, 1);
                        if(req[3] != 0) {
                            lockManager.unlock(lock_ids, 1);
                            return;
                        }
                        req[3] = (byte) (1);
                        lockManager.unlock(lock_ids, 1);
                    }
                    default -> throw new RuntimeException("The default statement in a non-deterministic block should be unreachable!");
                }
                successfulTransitionCounterread++;
                successfulTransitionCounter++;
            }

            // Execute method
            private void exec() {
                long startTime = System.nanoTime();
                long stopTime = System.currentTimeMillis() + 1000L * 10;
                while(stopTime > System.currentTimeMillis()) {
                    exec_read();

                    // Increment counter
                    transitionCounter++;
                }
                long running_time = System.nanoTime() - startTime;
                System.out.println(
                    "GlobalClass.environment;" +
                    successfulTransitionCounter + ";" +
                    transitionCounter + ";" +
                    (double)successfulTransitionCounter/transitionCounter + ";" +
                    running_time + ";"
                );
                System.out.println(
                    "GlobalClass.environment.read;" +
                    successfulTransitionCounterread + ";" +
                    transitionCounterread + ";" +
                    (double)successfulTransitionCounterread/transitionCounterread + ";" +
                    running_time + ";"
                );
            }

            // Run method
            public void run() {
                try {
                    exec();
                } catch(Exception e) {
                    lockManager.exception_unlock();
                    throw e;
                }
            }
        }

        // Define the states fot the state machine controller
        interface GlobalClass_controllerThread_States {
            enum States {
                wait, work, done
            }
        }

        // Representation of the SLCO state machine controller
        class controllerThread extends Thread implements GlobalClass_controllerThread_States {
            // Current state
            private controllerThread.States currentState;

            // Counter of main while-loop iterations
            long transitionCounter;
            long successfulTransitionCounter;
            long transitionCounterwait;
            long successfulTransitionCounterwait;
            long transitionCounterwork;
            long successfulTransitionCounterwork;
            long transitionCounterdone;
            long successfulTransitionCounterdone;

            // Thread local variables
            private byte ldir = 0;

            // The lock manager
            private final LockManager lockManager;

            // A list of lock ids that can be reused
            private final LockRequest[] lock_ids;

            controllerThread (LockManager lockManagerInstance) {
                lockManager = lockManagerInstance;
                lock_ids = new LockRequest[3];
                for (int i = 0; i < lock_ids.length; i++) {
                    lock_ids[i] = new LockRequest();
                }
                currentState = controllerThread.States.wait;
            }

            private void exec_wait() {
                transitionCounterwait++;
                // from wait to work {[v = 0; t := t + (2 * ldir) - 1]}
                lock_ids[0].set(0, 0, 1); // Acquire v
                lock_ids[1].set(1, 0, 1); // Acquire t
                lockManager.lock(lock_ids, 0, 2);
                if(v != 0) {
                    lockManager.unlock(lock_ids, 2);
                    return;
                }
                t = t + (2 * ldir) - 1;
                lockManager.unlock(lock_ids, 2);
                currentState = controllerThread.States.work;
                successfulTransitionCounterwait++;
                successfulTransitionCounter++;
            }

            private void exec_work() {
                transitionCounterwork++;
                lock_ids[0].set(1, 0, 1); // Acquire t
                lockManager.lock(lock_ids, 0, 1);
                lock_ids[1].set(3, t, 4); // Acquire req[t]
                lockManager.lock(lock_ids, 1, 2);
                if (t >= 0 && t < 4 && req[t] == 1) {
                    // from work to done {t >= 0 and t < 4 and req[t] = 1}
                    lockManager.unlock(lock_ids, 2);
                    currentState = controllerThread.States.done;
                } else if(t < 0 || t == 4) {
                    // from work to wait {[t < 0 or t = 4; ldir := 1 - ldir]}
                    ldir = (byte) (1 - ldir);
                    lockManager.unlock(lock_ids, 2);
                    currentState = controllerThread.States.wait;
                } else if(t >= 0 && t < 4 && req[t] == 0) {
                    // from work to work {[t >= 0 and t < 4 and req[t] = 0; t := t + (2 * ldir) - 1]}
                    t = t + (2 * ldir) - 1;
                    lockManager.unlock(lock_ids, 2);
                    currentState = controllerThread.States.work;
                } else {
                    lockManager.unlock(lock_ids, 2);
                }
                successfulTransitionCounterwork++;
                successfulTransitionCounter++;
            }

            private void exec_done() {
                transitionCounterdone++;
                // from done to wait {[v := 1]}
                lock_ids[0].set(0, 0, 1); // Acquire v
                lockManager.lock(lock_ids, 0, 1);
                v = (byte) (1);
                lockManager.unlock(lock_ids, 1);
                currentState = controllerThread.States.wait;
                successfulTransitionCounterdone++;
                successfulTransitionCounter++;
            }

            // Execute method
            private void exec() {
                long startTime = System.nanoTime();
                long stopTime = System.currentTimeMillis() + 1000L * 10;
                while(stopTime > System.currentTimeMillis()) {
                    switch(currentState) {
                        case wait -> exec_wait();
                        case work -> exec_work();
                        case done -> exec_done();
                    }

                    // Increment counter
                    transitionCounter++;
                }
                long running_time = System.nanoTime() - startTime;
                System.out.println(
                    "GlobalClass.controller;" +
                    successfulTransitionCounter + ";" +
                    transitionCounter + ";" +
                    (double)successfulTransitionCounter/transitionCounter + ";" +
                    running_time + ";"
                );
                System.out.println(
                    "GlobalClass.controller.wait;" +
                    successfulTransitionCounterwait + ";" +
                    transitionCounterwait + ";" +
                    (double)successfulTransitionCounterwait/transitionCounterwait + ";" +
                    running_time + ";"
                );
                System.out.println(
                    "GlobalClass.controller.work;" +
                    successfulTransitionCounterwork + ";" +
                    transitionCounterwork + ";" +
                    (double)successfulTransitionCounterwork/transitionCounterwork + ";" +
                    running_time + ";"
                );
                System.out.println(
                    "GlobalClass.controller.done;" +
                    successfulTransitionCounterdone + ";" +
                    transitionCounterdone + ";" +
                    (double)successfulTransitionCounterdone/transitionCounterdone + ";" +
                    running_time + ";"
                );
            }

            // Run method
            public void run() {
                try {
                    exec();
                } catch(Exception e) {
                    lockManager.exception_unlock();
                    throw e;
                }
            }
        }

        GlobalClass(int p, byte[] req, int t, byte v) {
            // Create a lock manager.
            LockManager lockManager = new LockManager(7);

            // Instantiate global variables
            this.req = req;
            this.t = t;
            this.p = p;
            this.v = v;

            // Instantiate state machines
            T_cabin = new GlobalClass.cabinThread(lockManager);
            T_environment = new GlobalClass.environmentThread(lockManager);
            T_controller = new GlobalClass.controllerThread(lockManager);
        }

        // Start all threads
        public void startThreads() {
            T_cabin.start();
            T_environment.start();
            T_controller.start();
        }

        // Join all threads
        public void joinThreads() {
            while (true) {
                try {
                    T_cabin.join();
                    T_environment.join();
                    T_controller.join();
                    break;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    dve_elevator2_1() {
        //Instantiate the objects
        objects = new SLCO_Class[] {
            new GlobalClass(
                0,
                new byte[] {0, 0, 0, 0},
                0,
                (byte) 0
            ),
        };
    }

    // Start all threads
    private void startThreads() {
        for(SLCO_Class o : objects) {
            o.startThreads();
        }
    }

    // Join all threads
    private void joinThreads() {
        for(SLCO_Class o : objects) {
            o.joinThreads();
        }
    }

    // Run application
    public static void main(String[] args) {
        dve_elevator2_1 ap = new dve_elevator2_1();
        ap.startThreads();
        ap.joinThreads();
    }
}