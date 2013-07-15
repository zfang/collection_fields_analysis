#!/bin/bash
nonAliasedFields=0
for i in `cat $@ | sed -rn 's/.*nonAliasedFields size: ([0-9]+)/\1/p'`; do
   nonAliasedFields=$(( $nonAliasedFields + $i ))
done
echo "Total number of nonAliasedFields fields: $nonAliasedFields"

aliasedFields=0
for i in `cat $@ | sed -rn 's/.*aliasedFields size: ([0-9]+)/\1/p'`; do
   aliasedFields=$(( $aliasedFields + $i ))
done
echo "Total number of aliasedFields fields: $aliasedFields"

externalFields=0
for i in `cat $@ | sed -rn 's/.*externalFields size: ([0-9]+)/\1/p'`; do
   externalFields=$(( $externalFields + $i ))
done
echo "Total number of externalFields fields: $externalFields"

unknownFields=0
for i in `cat $@ | sed -rn 's/.*unknownFields size: ([0-9]+)/\1/p'`; do
   unknownFields=$(( $unknownFields + $i ))
done
echo "Total number of unknownFields fields: $unknownFields"
