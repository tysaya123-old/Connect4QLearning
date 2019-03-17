#!/bin/bash
for i in {1..100}
do
   echo $(java Main -p2 RandomAI -p1 QLearnerAI -text -w 5 -h 4 -seed $i  > ./tests/$i.txt)
done
