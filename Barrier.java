public abstract class Barrier {
    abstract public void sync(int no) throws InterruptedException;

    abstract public void on();

    abstract public void off();

    abstract public void setThreshold(int k);

    public boolean atBarrier(Pos pos, int no) {
        if (no < 5) {
            return pos.row == 4 && pos.col > 2;
        } else {
            return pos.row == 5 && pos.col > 2;
        }
    }
}

class BarrierSemaphore extends Barrier {
    private Boolean active;

    private int threshold, carsArriving, carsDeparturing;
    private Semaphore mutex, arrivalLock, departureLock;

    public BarrierSemaphore() {
        this.active = false;

        this.mutex = new Semaphore(1);

        this.arrivalLock = new Semaphore(0);
        this.departureLock = new Semaphore(0);

        this.carsArriving = 0;
        this.carsDeparturing = 0;

        this.threshold = 9;
    }

    private void syncGate(Boolean isArrivalGate) throws InterruptedException {
        this.mutex.P();
        Semaphore gateLock = isArrivalGate ? this.arrivalLock : this.departureLock;

        if (this.active) {
            if (isArrivalGate) this.carsArriving++;
            else               this.carsDeparturing++;

            int cars = isArrivalGate ? this.carsArriving : this.carsDeparturing;
            if (cars < this.threshold) {
                // Wait for others
                this.mutex.V();

                gateLock.P();
            } else {
                // Give access to everyone
                for (int i = 0; i < this.threshold - 1; i++) {
                    gateLock.V();
                }

                if (isArrivalGate) this.carsArriving = 0;
                else               this.carsDeparturing = 0;

                this.mutex.V();
            }
        } else {
            this.mutex.V();
        }
    }

    @Override
    public void sync(int no) throws InterruptedException {
        // Arrival gate
        syncGate(true);

        // Departure gate
        syncGate(false);
    }

    @Override
    public void on() {
        try {
            this.mutex.P();

            this.active = true;

            // we must reset number of cars in both .on and .off because if cars
            // are leaving after .off was called they will alter the state
            this.carsArriving = 0;
            this.carsDeparturing = 0;

            this.mutex.V();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void off() {
        try {

            this.mutex.P();

            for (int i = 0; i < this.carsArriving; i++) {
                this.arrivalLock.V();
            }

            for (int i = 0; i < this.carsDeparturing; i++) {
                this.departureLock.V();
            }

            // resetting number of cars here because no remaining car should
            // give access to the others again after this
            this.carsArriving = 0;
            this.carsDeparturing = 0;

            this.active = false;

            this.mutex.V();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setThreshold(int k) {
        // TODO Auto-generated method stub
    }

}

class BarrierMonitor extends Barrier {

    boolean on = false;
    boolean[] waiting = new boolean[9], pass = new boolean[9];
    int cars = 0, threshold = 9;

    @Override
    synchronized public void sync(int no) throws InterruptedException {
        if (on) {
            cars++;
            if (cars >= threshold) {
                for (int i = 0; i < 9; i++) {
                    if (waiting[i]) {
                        pass[i] = true;
                        waiting[i] = false;
                    }
                }
                cars = 0;
                notifyAll();
            } else {
                waiting[no] = true;
                pass[no] = false;
                while (on && !pass[no]) {
                    wait();
                }
            }
        }
    }

    @Override
    synchronized public void on() {
        on = true;
        cars = 0;
        for (int i = 0; i < 9; i++) {
            waiting[i] = false;
            pass[i] = true;
        }
    }

    @Override
    synchronized public void off() {
        on = false;
        notifyAll();
    }

    @Override
    synchronized public void setThreshold(int k) {
        threshold = k;
        if (cars >= threshold) {
            for (int i = 0; i < 9; i++) {
                if (waiting[i]) {
                    pass[i] = true;
                    waiting[i] = false;
                }
            }
            cars = 0;
            notifyAll();
        }
    }
}