#!/bin/sh

d=`dirname $0`

java -jar "$d/adapter.jar" java -jar "$d/practicum.jar" -t
