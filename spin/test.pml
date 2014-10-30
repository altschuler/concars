int alleyAccess = 1;
int numCars = 0;

int carsRegion = 1;
int top = 1;
int bottom = 1;

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
	:: _pid < 4  -> dir = -1;
	:: _pid >= 4 -> dir = 1;
	fi

loop:
// ENTER
	if
	:: dir == 1 -> P(bottom) 	// up car
	:: dir == -1 -> P(top) 		// down car
	fi

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
	fi

    if
	:: signum != dir ->
		P(alleyAccess);
	fi

    if
	:: signum != dir ->
		P(carsRegion);
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
	:: numCars == 0 -> V(alleyAccess);
	fi

	V(carsRegion);

    V(alleyAccess);
goto loop;
}