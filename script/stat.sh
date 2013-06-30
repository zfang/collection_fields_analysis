#!/bin/bash
nonAliasedFields=0
for i in `cat $@ | sed -rn 's/.* nonAliasedFields size: ([0-9]+)/\1/p'`; do
   nonAliasedFields=$(( $nonAliasedFields + $i ))
done
echo "Total number of nonAliasedFields fields: $nonAliasedFields"

aliasedFieldStore=0
for i in `cat $@ | sed -rn 's/.* aliasedFieldStore size: ([0-9]+)/\1/p'`; do
   aliasedFieldStore=$(( $aliasedFieldStore + $i ))
done
echo "Total number of aliasedFieldStore fields: $aliasedFieldStore"

unknownFields=0
for i in `cat $@ | sed -rn 's/.* unknownFields size: ([0-9]+)/\1/p'`; do
   unknownFields=$(( $unknownFields + $i ))
done
echo "Total number of unknownFields fields: $unknownFields"
