#!/bin/bash
REPO_ROOT="$(git -C "$(dirname "${BASH_SOURCE[0]}")" rev-parse --show-toplevel)" || exit 1
cd "$REPO_ROOT" || exit 1

clear
echo "+++++++++++++++++++++++++++++++++++++++++++++"
echo "+++++++++++++++++++++++++++++++++++++++++++++"
echo "+++++++++++++++++++++++++++++++++++++++++++++"
echo "Testing Project (clean test + coverage report)"
echo "+++++++++++++++++++++++++++++++++++++++++++++"
echo "+++++++++++++++++++++++++++++++++++++++++++++"
echo "+++++++++++++++++++++++++++++++++++++++++++++"
mvn clean test jacoco:report
STATUS=$?
echo "Exit status: $STATUS"
exit $STATUS
