import glob

dirs = ['./tests/']



for d in dirs:
	WIN = 0
	DRAW = 0
	LOSS = 0
	print("In "+d)
	fs = glob.glob(d+'*.txt')
	for fn in fs:
		f = open(fn)
		c = str.strip(f.readlines()[-1])
		if c =='Draw Game':
			DRAW+=1
		if c=='Player 1 won':
			WIN+=1
		if c=='Player 2 won':
			LOSS+=1
	print("Win: {}, Draw: {}, Loss: {}".format(WIN,DRAW,LOSS))