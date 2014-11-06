import java.util.ArrayList;

public class Barrier {
    private Boolean active;
    private ArrayList<Semaphore> sems;
    private int threshold;

    private Semaphore barrierAccess;
    private Semaphore carLock;
    private int cars;

    public Barrier() {
        this.active = false;
        this.barrierAccess = new Semaphore(0);
        this.carLock = new Semaphore(1);
        this.cars = 0;

        this.threshold = 7;
    }

    public void sync(int no) throws InterruptedException {
        if (this.active) {

            this.carLock.P();
            this.cars++;
            this.carLock.V();

            if (this.cars < this.threshold) {
                // Wait for others
                this.barrierAccess.P();
            } else {
                // Give access to everyone
                for (int i = 0; i < this.threshold - 1; i++) {
                    this.barrierAccess.V();
                }

                this.carLock.P();
                this.cars = 0;
                this.carLock.V();
            }
        }
    }

    public void on() {
        this.active = true;
    }

    public void off() {
        try {
            for (int i = 0; i < this.cars; i++) {
                this.barrierAccess.V();
            }

            this.carLock.P();
            this.cars = 0;
            this.carLock.V();

            this.active = false;

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public boolean atBarrier(Pos pos, int carNumber) {
        if (carNumber < 5) {
            return pos.row == 4 && pos.col > 2;
        } else {
            return pos.row == 5 && pos.col > 2;
        }
    }
}
