#!/usr/bin/env gnuplot

set term png
set output 'output8victims.png'
set datafile separator ','
set logscale y
set xrange [0:30]
set xlabel 'Seconds'
set ylabel 'Bytes/s (log scale)'
set title 'Maximum Traffic Induced Over Time (numvictims = 8)'
set key outside right center box 3
plot '14/8.csv' using 1:2 title "wscale = 14", \
     '8/8.csv' using 1:2 title "wscale = 8", \
     '4/8.csv' using 1:2 title "wscale = 4", \
     '2/8.csv' using 1:2 title "wscale = 2", \
     '1/8.csv' using 1:2 title "wscale = 1"
