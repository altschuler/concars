class BarrierGate {
    public Semaphore lock;
    public int cars;

    public BarrierGate() {
        this.lock = new Semaphore(0);
        this.cars = 0;
    }

    public void releaseAll() {
        for (int i = 0; i < this.cars; i++)
            this.lock.V();

        this.reset();
    }

    public void reset() {
        this.cars = 0;
    }
}

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

    private int threshold;
    private Semaphore mutex;
    private BarrierGate arrivalGate, departureGate;

    public BarrierSemaphore() {
        this.active = false;

        this.mutex = new Semaphore(1);

        this.arrivalGate = new BarrierGate();
        this.departureGate = new BarrierGate();

        this.threshold = 9;
    }

    private void syncGate(BarrierGate gate) throws InterruptedException {
        this.mutex.P();

        if (this.active) {
            if (gate.cars + 1 < this.threshold) {
                gate.cars++;
                this.mutex.V();

                gate.lock.P(); // Wait for others
                return;
            } else {
                // Let everybody through the gate
                gate.releaseAll();
            }
        }

        this.mutex.V();
    }

    @Override
    public void sync(int no) throws InterruptedException {
        // Arrival gate
        syncGate(this.arrivalGate);

        // Departure gate
        syncGate(this.departureGate);
    }

    @Override
    public void on() {
        try {
            this.mutex.P();

            this.active = true;

            // we must reset number of cars in both .on and .off because if cars
            // are leaving after .off was called they will alter the state
            this.arrivalGate.reset();
            this.departureGate.reset();

            this.mutex.V();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void off() {
        try {
            this.mutex.P();

            // Release all the cars already at the gates
            this.arrivalGate.releaseAll();
            this.departureGate.releaseAll();

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