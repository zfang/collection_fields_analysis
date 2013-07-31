#!/bin/bash
dirname=$(dirname $0)

set -x
PROJECT=$dirname/../test/ $dirname/run.sh --main-class Test${1} > $dirname/../test/sootOutput/Test${1}.output
