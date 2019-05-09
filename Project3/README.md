We used Alexander Schaub's (https://bitbucket.org/AlexCid/opt-ack)
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
