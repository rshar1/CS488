#!/usr/bin/env gnuplot

set term png
set output 'output4victims.png'
set datafile separator ','
set logscale y
set xrange [0:30]
set xlabel 'Seconds'
set ylabel 'Bytes/s (log scale)'
set title 'Maximum Traffic Induced Over Time (numvictims = 4)'
set key outside right center box 3
plot '14/4.csv' using 1:2 title "wscale = 14", \
     '8/4.csv' using 1:2 title "wscale = 8", \
     '4/4.csv' using 1:2 title "wscale = 4", \
     '2/4.csv' using 1:2 title "wscale = 2", \
     '1/4.csv' using 1:2 title "wscale = 1"
