#!/bin/bash
nonAliasedFields=0
for i in `cat $@ | sed -rn 's/.* nonAliasedFields size: ([0-9]+)/\1/p'`; do
   nonAliasedFields=$(( $nonAliasedFields + $i ))
done
echo "Total number of nonAliasedFields fields: $nonAliasedFields"

mayAliasedFieldStore=0
for i in `cat $@ | sed -rn 's/.* mayAliasedFieldStore size: ([0-9]+)/\1/p'`; do
   mayAliasedFieldStore=$(( $mayAliasedFieldStore + $i ))
done
echo "Total number of mayAliasedFieldStore fields: $mayAliasedFieldStore"

unknownFields=0
for i in `cat $@ | sed -rn 's/.* unknownFields size: ([0-9]+)/\1/p'`; do
   unknownFields=$(( $unknownFields + $i ))
done
echo "Total number of unknownFields fields: $unknownFields"
