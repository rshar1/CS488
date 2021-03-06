We used some of Alexander Schaub's and Trey Deitch's scripts for our implementation. (https://bitbucket.org/AlexCid/opt-ack) We specifically used the following, with some modifications for our project: 
- run.sh 
- simulate_optack.py
- server directory
- plotting directory 

to help us reproduce the results.

As per Alexander Schaub's README, make sure to have gnuplot installed (sudo apt-get install gnuplot-nox):

sudo apt-get update
sudo apt-get install gnuplot-nox

Setup to reproduce:
- compile raw_tcp_socket.c with the pthread flag ( gcc -pthread raw_tcp_socket.c -o raw_tcp_socket )
- run run.sh (sudo ./run.sh)
- check the results directory for output.png

Note:
If getting no such file or directory error, try changing all the 
./simulate_optack.py 
to 
python simulate_optack.py

Our Presentation:
https://docs.google.com/presentation/d/1UR4IhMVLR1jX025LmtEQMKepjC7vfdvtxPBr7Fum8ho/edit?usp=sharing
