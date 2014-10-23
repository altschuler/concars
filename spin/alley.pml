int access = 1;
int carsRegion = 1;
int top = 1;
int bottom = 1;

int numCars = 0;

byte topbotCrit = 0;

inline P(sem)
{
	atomic {
		(sem !=0) -> sem--;
	}
} 

inline V(sem)
{
	atomic {
		sem++;
	}
} 

active [2] proctype car()
{
	int dir;
	if
	:: _pid < 1 -> dir = -1;
	:: _pid >= 1 -> dir = 1;
	fi

loop:
// ENTER
	if 
	:: dir == 1 -> P(bottom) 	// up car
	:: dir == -1 -> P(top) 		// down car
	fi

	topbotCrit++;
	assert(topbotCrit <= 2);		// critical section
	topbotCrit--;

	P(carsRegion);			// entering critical region for numCars
	int signum;
	if
	:: numCars < 0 -> signum = -1;
	:: numCars > 0 -> signum = 1;
	:: numCars == 0 -> signum = 0;
	fi

	if 
	:: signum != dir ->
		V(carsRegion); 
		:: P(access);
		:: P(carsRegion);
	fi
	
	numCars = numCars + dir;

	V(carsRegion);

	if 
	:: dir == 1 -> V(bottom) 	// up car
	:: dir == -1 -> V(top) 		// down car
	fi

// EXIT
	P(carsRegion);
	
	numCars = numCars - dir;
	if 
	:: numCars == 0 -> V(access);
	fi

	V(carsRegion);
	
goto loop;
}