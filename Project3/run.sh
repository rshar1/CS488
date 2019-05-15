#! /usr/bin/env bash
declare -a arr_victims=("1" "2" "4" "8" "16" "32" "64")
declare -a arr_wscale=("1" "2" "4" "8" "14")

# Launch the experiments
echo "Starting the experiments. It will take about 17 minutes."
echo  "Note : Ignore the '*** gave up after 3 retries' messages."

for i in "${arr_wscale[@]}"
do

  sudo ./simulate_optack.py -d results/$i/1 -n 1 -r 90 -t 30 -s $i
  echo "Experiment with 1 server finished"

  sudo ./simulate_optack.py -d results/$i/2 -n 2 -r 90 -t 30 -s $i
  echo "Experiment with 2 servers finished"

  sudo ./simulate_optack.py -d results/$i/4 -n 4 -r 90 -t 30 -s $i
  echo "Experiment with 4 servers finished"

  sudo ./simulate_optack.py -d results/$i/8 -n 8 -r 90 -t 30 -s $i
  echo "Experiment with 8 servers finished"

  sudo ./simulate_optack.py -d results/$i/16 -n 16 -r 90 -t 30 -s $i
  echo "Experiment with 16 servers finished"

  sudo ./simulate_optack.py -d results/$i/32 -n 32 -r 90 -t 30 -s $i
  echo "Experiment with 32 servers finished"

  sudo ./simulate_optack.py -d results/$i/64 -n 64 -r 80 -t 30 -s $i
  echo "Experiment with 64 servers finished"

  echo "Parsing wscale results"

  # Generates a unique graph for each wscale
  cd results/$i
  for nvictims in "${arr_victims[@]}"
  do
      cd "$nvictims"
      cd ../
      python ../../plotting/total-bytes-per-sec.py $nvictims/*.csv > $nvictims.csv
  done

  gnuplot ../../plotting/victims/$i/total-bytes-per-sec.plt
  cd ../..

done

cd results
for nvictims in "${arr_victims[@]}"
do
  gnuplot ../plotting/scales/$nvictims/total-bytes-per-sec.plt
done
