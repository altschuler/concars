import java.util.ArrayList;

public class Barrier {
    private Boolean active;
    private ArrayList<Semaphore> sems;

    private Semaphore barrierAccess;
    private Semaphore carLock;
    private int cars;

    public Barrier() {
        this.active = false;
        this.barrierAccess = new Semaphore(0);
        this.carLock = new Semaphore(1);
        this.cars = 0;
    }

    public void sync(int no) throws InterruptedException {
        if (this.active) {

            this.carLock.P();
            this.cars++;
            this.carLock.V();

            if (this.cars < 9) {
                System.out.println("Waiting for others " + no);
                this.barrierAccess.P();
            } else {
                System.out.println("Release " + this.cars);
                for (int i = 0; i < 8; i++) {
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
        this.active = false;
    }

    public boolean atBarrier(Pos pos, int carNumber) {
        if (carNumber < 5)
            return pos.row == 4 && pos.col > 2;
        else
            return pos.row == 5 && pos.col > 2;
    }
}
