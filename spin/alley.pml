int alleyAccess = 1;
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

active [4] proctype car()
{
	int dir;
	if
	:: (_pid < 2) -> dir = -1;
	:: (_pid >= 2) -> dir = 1;
    :: else -> skip;
	fi;

loop:
// ENTER
	if
	:: dir == 1 -> P(bottom); 	// up car
	:: dir == -1 -> P(top); 		// down car
	fi;

    printf("%u\n", _pid);

	//topbotCrit++;
	//assert(topbotCrit <= 2);		// critical section
	//topbotCrit--;

	P(carsRegion);			// entering critical region for numCars
	int signum;
	if
	:: numCars < 0 -> signum = -1;
	:: numCars > 0 -> signum = 1;
	:: numCars == 0 -> signum = 0;
    :: else -> skip;
	fi;

    if
	:: signum != dir ->
		V(carsRegion);
    :: else -> skip;
	fi;

    if
	:: signum != dir ->
		P(alleyAccess);
    :: else -> skip;
	fi;

    if
	:: signum != dir ->
		P(carsRegion);
    :: else -> skip;
	fi;

	numCars = numCars + dir;

	V(carsRegion);

	if
	:: dir == 1 -> V(bottom); 	// up car
	:: dir == -1 -> V(top); 		// down car
	fi;

// EXIT
	P(carsRegion);

	numCars = numCars - dir;
	if
	:: numCars == 0 -> V(alleyAccess);
    :: else -> skip;
	fi;

	V(carsRegion);

goto loop;
}