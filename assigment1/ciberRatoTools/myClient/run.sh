#!/bin/bash

challenge="1"
host="localhost"
robname="theAgent"
pos="0"
outfile="mapping.out"

while getopts "c:h:r:p:f:" op
do
    case $op in
        "c")
            challenge=$OPTARG
            ;;
        "h")
            host=$OPTARG
            ;;
        "r")
            robname=$OPTARG
            ;;
        "p")
            pos=$OPTARG
            ;;
        "f")
            outfile=$OPTARG
            ;;
        default)
            echo "ERROR in parameters"
            ;;
    esac
done

shift $(($OPTIND-1))

case $challenge in
    1)
        java ClientC1 -h "$host" -p "$pos" -r "$robname"
        ;;
    2)
        java ClientC2 -h "$host" -p "$pos" -r "$robname" -f "$outfile"
        ;;
    3)
        java ClientC3 -h "$host" -p "$pos" -r "$robname" -f "$outfile"
        ;;
esac
