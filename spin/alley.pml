#define NONE 0
#define DOWN 1
#define UP 2

bool alleyAcc = 1;
bool alleyCheck = 1;
bool topQ = 1;
bool botQ = 1;

byte alleyDir = 0;
byte alleyCars = 0;

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
	run carUp();
	run carUp();
	run carDown();
	run carDown();
	run carDown();
	run carDown()
	}
}

proctype tester()
{
	assert(alleyCars <= 4);
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

	//In Alley
	assert(alleyDir == UP);

	//Exiting Alley
	P(alleyCheck);

	alleyCheckCrit++;	//Critical Region
	alleyCheckCrit--;

	alleyCars--;
	if
	:: alleyCars == 0; V(alleyAcc); alleyDir = NONE
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

	//In Alley
	assert(alleyDir == DOWN);

	//Exiting Alley
	P(alleyCheck);

	alleyCheckCrit++;	//Critical Region
	alleyCheckCrit--;

	alleyCars--;
	if
	:: alleyCars == 0; V(alleyAcc); alleyDir = NONE
	:: alleyCars != 0; skip
	fi;
	V(alleyCheck)
od
}