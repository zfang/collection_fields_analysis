#!/bin/bash
project=$1
shift
PROJECT=../../$project/ ./run.sh $@ > ../${project}_collection_fields_result.txt
