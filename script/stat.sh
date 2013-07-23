#!/bin/bash
Methods=0
for i in `cat $@ | sed -rn 's/\[Main\]: At Method .*/1/p'`; do
   Methods=$(( $Methods + 1 ))
done
echo "Total number of methods: $Methods"

NONALIASED=0
for i in `cat $@ | sed -rn 's/\[Main\]: NONALIASED fields size: ([0-9]+)/\1/p'`; do
   NONALIASED=$(( $NONALIASED + $i ))
done
printf "Average number of NONALIASED fields: %.3f\n" $( bc -l <<< "$NONALIASED / $Methods" )

ALIASED=0
for i in `cat $@ | sed -rn 's/\[Main\]: ALIASED fields size: ([0-9]+)/\1/p'`; do
   ALIASED=$(( $ALIASED + $i ))
done
printf "Average number of ALIASED fields: %.3f\n" $( bc -l <<< "$ALIASED / $Methods" )

EXTERNAL=0
for i in `cat $@ | sed -rn 's/\[Main\]: EXTERNAL fields size: ([0-9]+)/\1/p'`; do
   EXTERNAL=$(( $EXTERNAL + $i ))
done
printf "Average number of EXTERNAL fields: %.3f\n" $( bc -l <<< "$EXTERNAL / $Methods" )

UNKNOWN=0
for i in `cat $@ | sed -rn 's/\[Main\]: UNKNOWN fields size: ([0-9]+)/\1/p'`; do
   UNKNOWN=$(( $UNKNOWN + $i ))
done
printf "Average number of UNKNOWN fields: %.3f\n" $( bc -l <<< "$UNKNOWN / $Methods" )
