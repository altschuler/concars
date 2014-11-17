import java.util.HashSet;
import java.util.Set;

public abstract class Alley {

	protected static final int UP = 1, DOWN = -1;
	protected int cars = 0, alleyDir = 0;
	@SuppressWarnings("serial")
	protected Set<Pos> points = new HashSet<Pos>() {{
									add(new Pos(1,0));
									add(new Pos(2,0));
									add(new Pos(3,0));
									add(new Pos(4,0));
									add(new Pos(5,0));
									add(new Pos(6,0));
									add(new Pos(7,0));
									add(new Pos(8,0));
									add(new Pos(9,0));
									add(new Pos(9,1));
									add(new Pos(9,2));
								}};

	protected int noToDir(int no) {
		return no > 4 ? DOWN : UP;
	}

	public boolean inAlley(Pos p) {
        return points.contains(p);
	}

	abstract public void enter(int no) throws InterruptedException;

	abstract public void leave(int no) throws InterruptedException;

}


class AlleyMonitor extends Alley {

    public AlleyMonitor() {
        super();
    }

    synchronized public void enter(int no) throws InterruptedException {
        int dir = noToDir(no);
        while(dir == -alleyDir) { wait(); }
        cars++;
        alleyDir = dir;
    }

    synchronized public void leave(int no) {
        cars--;
        if(cars == 0) { notifyAll(); alleyDir = 0; }
    }

}

class AlleySemaphore extends Alley {

    private Semaphore access = new Semaphore(1),
            carsRegion = new Semaphore(1),
            top = new Semaphore(1),
            bottom = new Semaphore(1);

    private Semaphore noToSem(int no) {
        return no > 4 ? top : bottom;
    }

    public void enter(int no) throws InterruptedException {
        noToSem(no).P();

        carsRegion.P();

        if (Math.signum(cars) != noToDir(no)) {
            carsRegion.V();
            access.P();
            carsRegion.P();
        }
        cars += noToDir(no);

        carsRegion.V();

        noToSem(no).V();
    }

    public void leave(int no) throws InterruptedException {
        carsRegion.P();

        cars -= noToDir(no);
        if (cars == 0)
            access.V();

        carsRegion.V();
    }

}