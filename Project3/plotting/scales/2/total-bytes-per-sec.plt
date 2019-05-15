#!/usr/bin/env gnuplot

set term png
set output 'output2victims.png'
set datafile separator ','
set logscale y
set xrange [0:30]
set xlabel 'Seconds'
set ylabel 'Bytes/s (log scale)'
set title 'Maximum Traffic Induced Over Time (numvictims = 2)'
set key outside right center box 3
plot '14/2.csv' using 1:2 title "wscale = 14", \
     '8/2.csv' using 1:2 title "wscale = 8", \
     '4/2.csv' using 1:2 title "wscale = 4", \
     '2/2.csv' using 1:2 title "wscale = 2", \
     '1/2.csv' using 1:2 title "wscale = 1"
