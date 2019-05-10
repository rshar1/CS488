#!/usr/bin/env gnuplot

set term png
set output 'output64victims.png'
set datafile separator ','
set logscale y
set xrange [0:30]
set xlabel 'Seconds'
set ylabel 'Bytes/s (log scale)'
set title 'Maximum Traffic Induced Over Time (numvictims = 64)'
set key outside right center box 3
plot '14/64.csv' using 1:2 title "wscale = 14", \
     '8/64.csv' using 1:2 title "wscale = 8", \
     '4/64.csv' using 1:2 title "wscale = 4", \
     '2/64.csv' using 1:2 title "wscale = 2", \
     '1/64.csv' using 1:2 title "wscale = 1"
