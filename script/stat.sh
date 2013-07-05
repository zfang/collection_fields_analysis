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

otherFields=0
for i in `cat $@ | sed -rn 's/.*otherFields size: ([0-9]+)/\1/p'`; do
   otherFields=$(( $otherFields + $i ))
done
echo "Total number of otherFields fields: $otherFields"

unknownFields=0
for i in `cat $@ | sed -rn 's/.*unknownFields size: ([0-9]+)/\1/p'`; do
   unknownFields=$(( $unknownFields + $i ))
done
echo "Total number of unknownFields fields: $unknownFields"
