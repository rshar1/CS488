#!/usr/bin/env gnuplot

set term png
set output 'output32victims.png'
set datafile separator ','
set logscale y
set xrange [0:30]
set xlabel 'Seconds'
set ylabel 'Bytes/s (log scale)'
set title 'Maximum Traffic Induced Over Time (numvictims = 32)'
set key outside right center box 3
plot '14/32.csv' using 1:2 title "wscale = 14", \
     '8/32.csv' using 1:2 title "wscale = 8", \
     '4/32.csv' using 1:2 title "wscale = 4", \
     '2/32.csv' using 1:2 title "wscale = 2", \
     '1/32.csv' using 1:2 title "wscale = 1"
