public abstract class Barrier {
	abstract public void sync(int no) throws InterruptedException;
	abstract public void on();
	abstract public void off();
	abstract public void setThreshold(int k);

    public boolean atBarrier(Pos pos, int no) {
        if (no < 5)
            return pos.row == 4 && pos.col > 2;
        else
            return pos.row == 5 && pos.col > 2;
    }
}

class BarrierSemaphore extends Barrier {
    private Boolean active;

    private int threshold, carsArriving, carsLeaving;

    private Semaphore mutex, arrivalLock, leavingLock;

    private int[] rounds;

    public BarrierSemaphore() {
        this.active = false;

        this.mutex = new Semaphore(1);

        this.arrivalLock = new Semaphore(0);
        this.leavingLock = new Semaphore(0);

        this.carsArriving = 0;
        this.carsLeaving = 0;

        this.threshold = 9;

        this.rounds = new int[9];
    }

	@Override
    public void sync(int no) throws InterruptedException {

        this.mutex.P();

        if (this.active) {

            // ARRIVING

            this.carsArriving++;

            if (this.carsArriving < this.threshold) {
                // Wait for others
                this.mutex.V();

                this.arrivalLock.P();
            } else {
                // Give access to everyone
                for (int i = 0; i < this.threshold - 1; i++) {
                    this.arrivalLock.V();
                }

                this.carsArriving = 0;
                this.mutex.V();
            }


            // LEAVING
            this.mutex.P();
            this.carsLeaving++;

            this.rounds[no]++;

            if (this.carsLeaving < this.threshold) {
                // Wait for others

                this.mutex.V();

                this.leavingLock.P();
            } else {
                // Give access to everyone
                for (int i = 0; i < this.threshold - 1; i++) {
                    this.leavingLock.V();
                }

                this.carsLeaving = 0;

                for (int r : this.rounds) {
                    System.out.println(r);
                }
                System.out.println();

                this.mutex.V();
            }

        } else {

            this.mutex.V();
        }
    }

	@Override
    public void on() {
        try {
            this.mutex.P();

            this.active = true;

            // we must reset number of cars in both .on and .off because if cars
            // are leaving after .off was called they will alter the state
            this.carsArriving = 0;
            this.carsLeaving = 0;

            for (int i = 0; i < this.rounds.length; i++) {
                this.rounds[i] = 0;
            }

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

            for (int i = 0; i < this.carsLeaving + this.carsArriving; i++) {
                this.leavingLock.V();
            }

            // resetting number of cars here because no remaining car should
            // give access to the others again after this
            this.carsArriving = 0;
            this.carsLeaving = 0;

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
		if(on) {
			cars++;
			if(cars >= threshold) {
				for(int i = 0; i < 9; i++)
					if(waiting[i]) {
						pass[i] = true;
						waiting[i] = false;
					}
				cars = 0;
				notifyAll();
			} else {
				waiting[no] = true;
				pass[no] = false;
				while(on && !pass[no]) wait();
			}
		}
	}

	@Override
	synchronized public void on() {
		on = true;
		cars = 0;
		for(int i = 0; i < 9; i++) {
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
		if(cars >= threshold) {
			for(int i = 0; i < 9; i++) {
				if(waiting[i]) {
					pass[i] = true;
					waiting[i] = false;
				}
			}
			cars = 0;
			notifyAll();
		}
	}
}