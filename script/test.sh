#!/bin/bash
for i in {1..4}
do
   set -x
   PROJECT=../test/ ./run.sh --main-class Test${i} > ../test/sootOutput/Test${i}.output
   set +x
done