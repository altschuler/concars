//Prototype implementation of Car Control
//Mandatory assignment
//Course 02158 Concurrent Programming, DTU, Fall 2014

//Hans Henrik Løvengreen    Oct 6, 2014

import java.awt.Color;

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

	int basespeed = 50; // Rather: degree of slowness
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

	//SemFields semFields;
	Grid grid;
	Semaphore wait = new Semaphore(0);

	Alley alley;

    public Semaphore repairLock;
    public boolean removed = false;

    //public Car(int no, CarDisplayI cd, Gate g, SemFields semFields, Alley alley, Barrier barrier) {
    public Car(int no, CarDisplayI cd, Gate g, Grid grid, Alley alley, Barrier barrier) {
		this.no = no;
		this.cd = cd;
		this.mygate = g;
        this.startpos = cd.getStartPos(no);
		this.barpos = cd.getBarrierPos(no); // For later use

		//this.semFields = semFields;
		this.grid = grid;

		this.alley = alley;
        this.barrier = barrier;

        /*
        try {
			this.semFields.P(startpos);
		} catch (InterruptedException e) { } //It can't be interrupted this early.
		*/
        this.grid.setPos(startpos,true);

        this.repairLock = new Semaphore(0);

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

	public void repair() {
		cd.clear(curpos);
		removed = true;
		try {
			grid.setPos(curpos, false);
			//semFields.V(curpos);
			if (alley.inAlley(curpos))
				alley.leave(no);
			curpos = cd.getStartPos(no);
			grid.setPos(curpos, true);
			//semFields.P(curpos);
			this.repairLock.P();
			cd.mark(curpos, col, no);
		} catch (InterruptedException e) {}
		//This thread will never be interrupted, since
		//interruption only happens if removed == false.
	}

	public void run() {
		speed = chooseSpeed();
		curpos = startpos;
		cd.mark(curpos, col, no);
		while (true) {
			try {
				sleep(speed());
				//Gate
				if (atGate(curpos)) {
					mygate.pass();
					speed = chooseSpeed();
				}			
				newpos = nextPos(curpos);
				
				//Barrier
				if (barrier.atBarrier(curpos, no))
							barrier.sync(no);

				//Alley
				Boolean curIn = alley.inAlley(curpos),
						newIn = alley.inAlley(newpos);
				if (newIn && !curIn)
					alley.enter(no);
				else if (!newIn && curIn)
					alley.leave(no);
				//Translation
				grid.enter();
				try {
					if(grid.getPos(newpos)) {
						Semaphore other = grid.getSem(newpos);
						grid.setSem(newpos, wait);
						grid.exit();
						wait.P();
						grid.setSem(newpos, other);
						grid.exit();
					} else {
						grid.setPos(newpos, true);
						grid.exit();
					}
					//semFields.P(newpos);
					try {
						cd.clear(curpos);
						cd.mark(curpos, newpos, col, no);
						sleep(speed());
						cd.clear(curpos, newpos);
						cd.mark(newpos, col, no);

						try {
							grid.enter();
							Semaphore other = grid.getSem(curpos);
							if(other == null) {
								grid.setPos(curpos, false);
								curpos = newpos;
								grid.exit();
							} else {
								curpos = newpos;
								other.V();
							}
						} catch(InterruptedException e) { //interrupted while completing movement
							curpos = newpos;
							repair();
						}
					} catch (InterruptedException e) { //Translation sleep interrupt
						curpos = newpos;
						cd.clear(newpos);
						repair();
					}
				} catch(InterruptedException e) { //Interrupted while waiting
					repair();
				}
			} catch (InterruptedException e) { //Interrupted not during moving
				repair();
			}
		}
    }
}

public class CarControl implements CarControlI {

	public static final boolean USE_MONITORS = true;
	
	CarDisplayI cd; // Reference to GUI
	Car[] car; // Cars
	Gate[] gate; // Gates
	//SemFields semFields = new SemFields(); // Spaces
	Grid grid = new Grid();
	Alley alley; // Alley
    Barrier barrier;

    public CarControl(CarDisplayI cd) {
		this.cd = cd;
		car = new Car[9];
		gate = new Gate[9];

		if(USE_MONITORS) {
			alley = new AlleyMonitor();
        	barrier = new BarrierMonitor();
    	} else {
			alley = new AlleySemaphore();
			barrier = new BarrierSemaphore();
		}

		for (int no = 0; no < 9; no++) {
			gate[no] = new Gate();
			//car[no] = new Car(no, cd, gate[no], semFields, alley, barrier);
			car[no] = new Car(no, cd, gate[no], grid, alley, barrier);
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
		cd.println("Barrier threshold set to " + k);
		barrier.setThreshold(k);
	}

	public void removeCar(int no) {
		if(this.car[no].removed)
	        cd.println("Car no " + no + " is already in repair");
		else {
			this.car[no].interrupt();
			cd.println("Car no " + no + " is sent for repair");
		}
	}

	public void restoreCar(int no) {
		if(this.car[no].removed) {
	        this.car[no].repairLock.V();
	        this.car[no].removed = false;
	        cd.println("Car no " + no + " is back on track");
		} else
	        cd.println("Car no " + no + " is already on the track");
	}

	/* Speed settings for testing purposes */

	public void setSpeed(int no, int speed) {
		car[no].setSpeed(speed);
	}

	public void setVariation(int no, int var) {
		car[no].setVariation(var);
	}

}

class SemFields {
	
	private Semaphore[][] sems = new Semaphore[11][12];
	
	public SemFields() {
		for (int i = 0; i < 11; i++)
			for (int j = 0; j < 12; j++)
				this.sems[i][j] = new Semaphore(1);
	}
	
	public void P(Pos pos) throws InterruptedException {
		this.sems[pos.row][pos.col].P();
	}
	
	public void V(Pos pos) {
		this.sems[pos.row][pos.col].V();
	}

}

class Grid {

	private Semaphore mutex = new Semaphore(1);
	private boolean[][] spaces = new boolean[11][12];
	private Semaphore[][] sems = new Semaphore[11][12];

	public void enter() throws InterruptedException {
		mutex.P();
	}

	public void exit() {
		mutex.V();
	}

	public boolean getPos(Pos pos) {
		return spaces[pos.row][pos.col];
	}

	public void setPos(Pos pos, boolean val) {
		spaces[pos.row][pos.col] = val;
	}

	public Semaphore getSem(Pos pos) {
		return sems[pos.row][pos.col];
	}

	public void setSem(Pos pos, Semaphore sem) {
		sems[pos.row][pos.col] = sem;
	}

}