#!/bin/bash

dirname=$(dirname $0)

project=$1

shift

PROJECT=$dirname/../../$project/ $dirname/run.sh $@ > $dirname/../${project}_disjointness_result.txt
