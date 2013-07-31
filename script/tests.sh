#!/bin/bash
dirname=$(dirname $0)

for i in {1..5}
do
   set -x
   PROJECT=$dirname/../test/ $dirname/run.sh --main-class Test${i} > $dirname/../test/sootOutput/Test${i}.output
   set +x
done
