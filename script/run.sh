#!/usr/bin/bash

jre_path=/usr/lib/jvm/java/jre/lib

project=$PROJECT

libs=../libs

extra_cp=$(echo $project/libs/*.jar | tr ' ' ':')

java -Xms2048m -Xmx4096m -cp \
$libs/sootclasses.jar:../bin  \
com.hang.ld.MyMain -f j --d $project/sootOutput -p cg.spark enabled:true -w \
--process-path $project/src --cp \
$project/bin:$jre_path/rt.jar:$jre_path/jce.jar:$extra_cp --src-prec only-class --xml-attributes $@
