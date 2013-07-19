#!/bin/bash
nonAliasedFields=0
for i in `cat $@ | sed -rn 's/.*nonAliasedFields size: ([0-9]+)/\1/p'`; do
   nonAliasedFields=$(( $nonAliasedFields + $i ))
done
echo "Total number of nonAliasedFields fields: $nonAliasedFields"

finalAliasedFieldStore=0
for i in `cat $@ | sed -rn 's/.*finalAliasedFieldStore size: ([0-9]+)/\1/p'`; do
   finalAliasedFieldStore=$(( $finalAliasedFieldStore + $i ))
done
echo "Total number of finalAliasedFieldStore fields: $finalAliasedFieldStore"

externalFields=0
for i in `cat $@ | sed -rn 's/.*externalFields size: ([0-9]+)/\1/p'`;
do
   externalFields=$(( "xternalFields + $i ))
done
echo "Total number of externalFields fields: "xternalFields"

unknownFields=0
for i in `cat $@ | sed -rn 's/.*unknownFields size: ([0-9]+)/\1/p'`; do
   unknownFields=$(( $unknownFields + $i ))
done
echo "Total number of unknownFields fields: $unknownFields"
