#!/usr/bin/env bash

#
# This script is a wrapper to run the PhotoMosaic and ProcessDB Java programs.
#

# The directory with our source code
SRCDIR="$(dirname $0)/src"

if [[ $# -lt 1 ]]; then
	>&2 echo "Usage: $0 <program to run> [ args ... ]"
	>&2 echo "Programs available:"
	
	# Find the programs that have a main method
	for prog in $(grep -l 'public static void main' "$SRCDIR"/*); do
		prog_basename="$(basename $prog)"
		>&2 echo -e "\t${prog_basename%.*}"
	done
	exit 1
fi

# Run the specified program
java -cp "$SRCDIR" $@
