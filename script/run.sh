#!/bin/bash

dirname=$(dirname $0)

jre_path=/usr/lib/jvm/java/jre/lib

project=$PROJECT

libs=$dirname/../libs

extra_cp=$(echo $project/libs/*.jar | tr ' ' ':')

#main_class=com.zfang.cf.MyMain
main_class=com.hang.ld.MyMain

java -Xms2048m -Xmx4096m -cp \
$libs/sootclasses.jar:$dirname/../bin  \
$main_class -f j --d $project/sootOutput -p cg.spark enabled:true -w \
--process-path $project/src \
--cp $project/bin:$jre_path/rt.jar:$jre_path/jce.jar:$extra_cp --src-prec only-class --xml-attributes $@ \
| sed -rn 's/\[(.*)Analysis\] (.*)/[\1]: \2/p'
