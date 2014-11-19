import java.util.HashSet;
import java.util.Set;

enum AlleyDir {
    UP, DOWN, NONE;
}

public abstract class Alley {

	protected int cars = 0;
    protected AlleyDir alleyDir = AlleyDir.NONE;

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

	protected AlleyDir noToDir(int no) {
		return no > 4 ? AlleyDir.DOWN : AlleyDir.UP;
	}

	public boolean inAlley(Pos p) {
        return points.contains(p);
	}

	abstract public void enter(int no) throws InterruptedException;

	abstract public void leave(int no) throws InterruptedException;

}


class AlleyMonitor extends Alley {

    synchronized public void enter(int no) throws InterruptedException {
        AlleyDir dir = noToDir(no);
        while(alleyDir != dir && alleyDir != AlleyDir.NONE) { wait(); }
        cars++;
        alleyDir = dir;
    }

    synchronized public void leave(int no) {
        cars--;
        if(cars == 0) { notifyAll(); alleyDir = AlleyDir.NONE; }
    }

}

class AlleySemaphore extends Alley {

    private Semaphore access = new Semaphore(1);
    private Semaphore carsRegion = new Semaphore(1);
    private Semaphore top = new Semaphore(1);
    private Semaphore bottom = new Semaphore(1);

    private Semaphore noToSem(int no) {
        return no > 4 ? top : bottom;
    }

    public void enter(int no) throws InterruptedException {
        noToSem(no).P();

        carsRegion.P();

        AlleyDir dir = noToDir(no);
        if (alleyDir != dir) {
            carsRegion.V();
            access.P();
            carsRegion.P();
        }

        alleyDir = dir;

        cars++;

        carsRegion.V();

        noToSem(no).V();
    }

    public void leave(int no) throws InterruptedException {
        carsRegion.P();

        cars--;

        if (cars == 0) {
            alleyDir = AlleyDir.NONE;
            access.V();
        }

        carsRegion.V();
    }
}