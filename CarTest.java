//Prototype implementation of Car Test class
//Mandatory assignment
//Course 02158 Concurrent Programming, DTU, Fall 2014

//Hans Henrik Løvengreen    Oct 6, 2014

public class CarTest extends Thread {

    CarTestingI cars;
    int testno;

    public CarTest(CarTestingI ct, int no) {
        cars = ct;
        testno = no;
    }

    /*
    * Set speed of all cars
    */
    private void setCarSpeed(int speed) {
        for (int i = 1; i < 9; i++)
            cars.setSpeed(i, speed);
    }

    public void run() {
        try {
            switch (testno) { 
                case 0:
                    // Demonstration of startAll/stopAll.
                    // Should let the cars go one round (unless very fast)
                    cars.startAll();
                    sleep(3000);
                    cars.stopAll();
                    break;

                case 1:
                    cars.println("Testing barrier works when toggled instantly");

                    cars.barrierOn();
                    cars.startAll();

                    sleep(4000);

                    cars.barrierOff();
                    cars.barrierOn();

                    sleep(4000);

                    cars.barrierOff();
                    cars.barrierOn();
                    cars.barrierOff();

                    cars.stopAll();
                    break;

                case 2:
                    // This test should show all cars waiting at the barrier one after another with a second in between.
                    // All waiting cars should make a round when the barrier opens.
                    cars.println("Testing barrier threshold (requires monitors)");

                    setCarSpeed(5); // speed things up

                    cars.barrierOn();

                    // start with lowest threshold
                    cars.barrierSet(2);

                    // Test thresholds value 2 through 6
                    for (int t = 3; t < 11; t++) {
                        cars.startCar(0);
                        sleep(1000);
                        cars.startCar(t - 2);
                        sleep(100); // let them through
                        cars.barrierSet(Math.min(t, 9));
                    }

                    cars.stopAll();

                    sleep(1000);

                    setCarSpeed(50); // reset speed
                    break;

                case 3:
                    // This test should show one slow cars with three very fast cars
                    // following right behind (on their common path). The fast cars
                    // should reach their gate before the slow one
                    cars.println("Testing collision prevention");
                    cars.setSpeed(8, 5);
                    cars.setSpeed(7, 5);
                    cars.setSpeed(6, 5);
                    cars.setSpeed(5, 100);

                    // Let the games begin
                    cars.startCar(5);

                    sleep(2000);

                    cars.startCar(8);
                    cars.startCar(7);
                    cars.startCar(6);

                    cars.stopAll();


                case 19:
                    // Demonstration of speed setting.
                    // Change speed to double of default values
                    cars.println("Doubling speeds");
                    for (int i = 1; i < 9; i++) {
                        cars.setSpeed(i,50);
                    };
                    break;

                default:
                    cars.println("Test " + testno + " not available");
            }

            cars.println("Test ended");

        } catch (Exception e) {
            System.err.println("Exception in test: "+e);
        }
    }

}



