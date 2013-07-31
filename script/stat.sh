#!/bin/bash
args=$@
tag="\[Main\]"

Methods=0
for i in `cat $args | sed -rn "s/$tag: At Method .*/1/p"`; do
   Methods=$(( $Methods + 1 ))
done
echo "Total number of methods: $Methods"

msg_format="Average number of %s fields: %.3f\n"

count_fields() {
   COUNT=0
   for i in `cat $args | sed -rn "s/$tag: $1 fields size: ([0-9]+)/\1/p"`; do
      COUNT=$(( $COUNT + $i ))
   done
   printf "$msg_format" "$1" $( bc -l <<< "$COUNT / $Methods" )
}

for i in IMMUTABLE ALIASED EXTERNAL UNKNOWN NULL NONALIASED
do
   count_fields $i
done
