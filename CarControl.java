//Prototype implementation of Car Control
//Mandatory assignment
//Course 02158 Concurrent Programming, DTU, Fall 2014

//Hans Henrik Løvengreen    Oct 6, 2014

import java.awt.Color;
import java.util.HashSet;
import java.util.Set;

class Gate {

	Semaphore g = new Semaphore(0);
	Semaphore e = new Semaphore(1);
	boolean isopen = false;

	public void pass() throws InterruptedException {
		g.P();
		g.V();
	}

	public void open() {
		try {
			e.P();
		} catch (InterruptedException e) {
		}
		if (!isopen) {
			g.V();
			isopen = true;
		}
		e.V();
	}

	public void close() {
		try {
			e.P();
		} catch (InterruptedException e) {
		}
		if (isopen) {
			try {
				g.P();
			} catch (InterruptedException e) {
			}
			isopen = false;
		}
		e.V();
	}

}

class Car extends Thread {

	int basespeed = 100; // Rather: degree of slowness
	int variation = 50; // Percentage of base speed

	CarDisplayI cd; // GUI part

	int no; // Car number
	Pos startpos; // Startpositon (provided by GUI)
	Pos barpos; // Barrierpositon (provided by GUI)
	Color col; // Car color
	Gate mygate; // Gate at startposition
    Barrier barrier;

    int speed; // Current car speed
	Pos curpos; // Current position
	Pos newpos; // New position to go to

	SemFields semFields;

	Alley alley;

	public Car(int no, CarDisplayI cd, Gate g, SemFields semFields, Alley alley, Barrier barrier) {

		this.no = no;
		this.cd = cd;
		this.mygate = g;
        this.startpos = cd.getStartPos(no);
		this.barpos = cd.getBarrierPos(no); // For later use

		this.semFields = semFields;

		this.alley = alley;
        this.barrier = barrier;

        this.semFields.P(startpos);

        this.col = chooseColor();

		// do not change the special settings for car no. 0
		if (no == 0) {
			basespeed = 0;
			variation = 0;
			setPriority(Thread.MAX_PRIORITY);
		}
	}

	public synchronized void setSpeed(int speed) {
		if (no != 0 && speed >= 0) {
			basespeed = speed;
		} else
			cd.println("Illegal speed settings");
	}

	public synchronized void setVariation(int var) {
		if (no != 0 && 0 <= var && var <= 100) {
			variation = var;
		} else
			cd.println("Illegal variation settings");
	}

	synchronized int chooseSpeed() {
		double factor = (1.0D + (Math.random() - 0.5D) * 2 * variation / 100);
		return (int) Math.round(factor * basespeed);
	}

	private int speed() {
		// Slow down if requested
		final int slowfactor = 3;
		return speed * (cd.isSlow(curpos) ? slowfactor : 1);
	}

	Color chooseColor() {
		return Color.blue; // You can get any color, as longs as it's blue
	}

	Pos nextPos(Pos pos) {
		// Get my track from display
		return cd.nextPos(no, pos);
	}

	boolean atGate(Pos pos) {
		return pos.equals(startpos);
	}

	public void run() {
		try {

			speed = chooseSpeed();
			curpos = startpos;
			cd.mark(curpos, col, no);

			while (true) {
				sleep(speed());

				if (atGate(curpos)) {
					mygate.pass();
					speed = chooseSpeed();
				}

				newpos = nextPos(curpos);

                // Alley handling
                Boolean curIn = alley.inAlley(curpos);
                Boolean newIn = alley.inAlley(newpos);

				if (newIn && !curIn) {
                    alley.enter(no);
				} else if(!newIn && curIn) {
                    alley.leave(no);
				}

                // Barrier handling
                if (barrier.atBarrier(curpos, no)) {
                    System.out.println("At barrier");
                    barrier.sync(no);
                }

                // Move to new position
				semFields.P(newpos);

				cd.clear(curpos);
				cd.mark(curpos, newpos, col, no);
				sleep(speed());
				cd.clear(curpos, newpos);
				cd.mark(newpos, col, no);

				semFields.V(curpos);

				curpos = newpos;
			}

		} catch (Exception e) {
			cd.println("Exception in Car no. " + no);
			System.err.println("Exception in Car no. " + no + ":" + e);
			e.printStackTrace();
		}
	}

}

public class CarControl implements CarControlI {

	CarDisplayI cd; // Reference to GUI
	Car[] car; // Cars
	Gate[] gate; // Gates
	SemFields semFields; // Spaces
	Alley alley; // Alley
    Barrier barrier;

    public CarControl(CarDisplayI cd) {
		this.cd = cd;
		car = new Car[9];
		gate = new Gate[9];

		semFields = new SemFields(11, 12);

		alley = new Alley();
        barrier = new Barrier();

		for (int no = 0; no < 9; no++) {
			gate[no] = new Gate();
			car[no] = new Car(no, cd, gate[no], semFields, alley, barrier);
			car[no].start();
		}
	}

	public void startCar(int no) {
		gate[no].open();
	}

	public void stopCar(int no) {
		gate[no].close();
	}

	public void barrierOn() {
        barrier.on();
        cd.println("Barrier turned on");
	}

	public void barrierOff() {
        barrier.off();
        cd.println("Barrier turned off");
	}

	public void barrierSet(int k) {
		cd.println("Barrier threshold setting not implemented in this version");
		// This sleep is for illustrating how blocking affects the GUI
		// Remove when feature is properly implemented.
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
		}
	}

	public void removeCar(int no) {
		cd.println("Remove Car not implemented in this version");
	}

	public void restoreCar(int no) {
		cd.println("Restore Car not implemented in this version");
	}

	/* Speed settings for testing purposes */

	public void setSpeed(int no, int speed) {
		car[no].setSpeed(speed);
	}

	public void setVariation(int no, int var) {
		car[no].setVariation(var);
	}
}

class Alley {
	private int cars;
	private Set<Pos> points;

	private Semaphore access, carsRegion, top, bottom;

	public Alley() {
		access = new Semaphore(1);
		carsRegion = new Semaphore(1);
		top = new Semaphore(1);
		bottom = new Semaphore(1);

		points = new HashSet<Pos>() {{
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
	}

	private int no2int(int no) {
		return no > 4 ? -1 : 1;
	}

    private Semaphore no2sem(int no) {
        return no > 4 ? top : bottom;
    }

	public boolean inAlley(Pos p) {
        return points.contains(p);
	}

	public void enter(int no) throws InterruptedException {
		no2sem(no).P();

		carsRegion.P();

		if (Math.signum(cars) != no2int(no)) {
			carsRegion.V();
			access.P();
			carsRegion.P();
		}
		cars += no2int(no);

		carsRegion.V();

        no2sem(no).V();
	}

	public void leave(int no) throws InterruptedException {
		carsRegion.P();

		cars -= no2int(no);
		if (cars == 0)
			access.V();

		carsRegion.V();
	}
}

class SemFields {
    private Semaphore[][] sems;

    public SemFields(int row, int col) {
        this.sems = new Semaphore[row][col];

        for (int i = 0; i < 11; i++) {
            for (int j = 0; j < 12; j++) {
                this.sems[i][j] = new Semaphore(1);
            }
        }
    }

    public void P(Pos pos) {
        try {
            this.sems[pos.row][pos.col].P();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void V(Pos pos) {
        this.sems[pos.row][pos.col].V();
    }
}