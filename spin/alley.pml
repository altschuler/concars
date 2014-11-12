#define DOWN 0
#define UP 1

bool alleyAcc = 1;
bool alleyCheck = 1;
bool topQ = 1;
bool botQ = 1;

bool alleyDir = 0;
bool alleyCars = 0;

byte topbotQCrit = 0;
byte alleyCheckCrit = 0

inline P(sem)
{
	atomic {
		sem != 0;
		sem--
	}
}

inline V(sem)
{
	atomic {
		sem++
	}
}

init
{
	atomic{
	run tester();
	run carUp();
	run carUp();
	run carDown();
	run carDown();
	}
}

proctype tester()
{
	assert(alleyCars <= 2);
	assert(topbotQCrit <= 2);
	assert(alleyCheckCrit <= 1)
}

proctype carUp()
{
do ::
	//Entering Alley
	P(botQ);

	topbotQCrit++;	//Critical Region
	topbotQCrit--;

	P(alleyCheck);

	alleyCheckCrit++;	//Critical Region
	alleyCheckCrit--;

	if
	:: alleyDir != UP; V(alleyCheck); P(alleyAcc); P(alleyCheck)
	:: alleyDir == UP; skip
	fi;
	alleyDir = UP;
	alleyCars++;

	V(alleyCheck);
	V(botQ);

	//Exiting Alley
	P(alleyCheck);

	alleyCheckCrit++;	//Critical Region
	alleyCheckCrit--;

	alleyCars--;
	if
	:: alleyCars == 0; V(alleyAcc)
	:: alleyCars != 0; skip
	fi;

	V(alleyCheck)
od
}

proctype carDown()
{
do ::
	//Entering Alley
	P(topQ);

	topbotQCrit++;	//Critical Region
	topbotQCrit--;

	P(alleyCheck);

	alleyCheckCrit++;	//Critical Region
	alleyCheckCrit--;

	if
	:: alleyDir != DOWN; V(alleyCheck); P(alleyAcc); P(alleyCheck)
	:: alleyDir == DOWN; skip
	fi;
	alleyDir = DOWN;
	alleyCars++;

	V(alleyCheck);
	V(topQ);

	//Exiting Alley
	P(alleyCheck);

	alleyCheckCrit++;	//Critical Region
	alleyCheckCrit--;

	alleyCars--;
	if
	:: alleyCars == 0; V(alleyAcc)
	:: else; skip
	fi;
	V(alleyCheck)
od
}

/* OLD - Variables have new names.
init
{
	atomic{
		run tester();
		run car(DOWN);
		run car(DOWN);
		run car(UP);
		run car(UP)
	}
}

proctype car(bool dir)
{
do ::
	//Enter Alley
	if
	:: dir == UP; atomic{bottom != 0; bottom--}
	:: dir == DOWN; atomic{top != 0; top--}
	fi;

	topbotCrit++;
	topbotCrit--;

	P(carsRegion);
	//Critical Region

	carsRegionCrit++;
	carsRegionCrit--;

	if
	:: alleyDir != dir; V(carsRegion); P(alleyAccess); P(carsRegion) //Temp leave critical region
	:: alleyDir == dir; skip
	fi;
	alleyDir = dir;
	numCars++;
	//Exit Critical Region
	V(carsRegion);

	if
	:: dir == UP; V(bottom)
	:: dir == DOWN; V(top)
	fi;

	//Exit Alley
	P(carsRegion);
	//Enter Critical Region

	carsRegionCrit++;
	carsRegionCrit--;

	numCars--;
	if
	:: numCars == 0; V(alleyAccess)
	:: numCars != 0; skip
	fi;

	//Exit Critical Region
	V(carsRegion);
	//end1:
od
}
*/