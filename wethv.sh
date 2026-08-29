clear
echo off
echo "+++++++++++++++++++++++++++++++++++++++++++++"
echo "+++++++++++++++++++++++++++++++++++++++++++++"
echo "+++++++++++++++++++++++++++++++++++++++++++++"
echo "Updating Project Version"
echo "+++++++++++++++++++++++++++++++++++++++++++++"
echo "+++++++++++++++++++++++++++++++++++++++++++++"
echo "+++++++++++++++++++++++++++++++++++++++++++++"

if [ -z "$1" ]; then
  echo "ERROR: New version required."
  echo "Usage: ./wethv.sh <new-version>"
  echo "Example: ./wethv.sh 1.17.3-SNAPSHOT"
  exit 1
fi

NEW_VERSION="$1"

echo "Step  1/10: Entering noakweather-platform directory"
cd noakweather-platform || { echo "ERROR: noakweather-platform directory not found"; exit 1; }

echo "Step  2/10: mvn versions:set -DnewVersion=$NEW_VERSION"
mvn versions:set -DnewVersion="$NEW_VERSION"
if [ $? -ne 0 ]; then
  echo "ERROR: mvn versions:set failed. Aborting before touching root pom.xml."
  exit 1
fi

echo "Step  3/10: Returning to parent directory"
cd ..

echo "Step  4/10: Updating root pom.xml version to $NEW_VERSION"
# Updates only the FIRST <version> tag in the root pom.xml, which is the
# project's own <version> element (appears before any module references).
# Does NOT touch noakweather-legacy, which is a separate module with its
# own independent version (0.0.5) that this script must never change.
#
# Uses '1,/pattern/' (not '0,/pattern/') for the address range: '0,' is a
# GNU sed extension not supported by BSD/macOS sed. '1,/pattern/' is the
# POSIX-portable equivalent and is safe here because the <version> tag is
# never on line 1 of a Maven pom.xml (line 1 is always the XML declaration
# or the opening <project> tag).
sed -i.bak "1,/<version>.*<\/version>/s/<version>.*<\/version>/<version>${NEW_VERSION}<\/version>/" pom.xml
SED_STATUS=$?
rm -f pom.xml.bak

if [ $SED_STATUS -ne 0 ]; then
  echo "ERROR: sed command failed (exit code $SED_STATUS) while updating pom.xml"
  exit 1
fi

# sed exits 0 even if the pattern was never found (no-op), so also verify
# the substitution actually took effect by reading back the first
# <version> tag and comparing it to what we expected to write. Uses awk
# (POSIX, consistent between BSD/macOS and GNU) rather than sed/grep -o,
# whose extended-regex behavior varies more across platforms.
ACTUAL_VERSION=$(awk -F'[<>]' '/<version>/{print $3; exit}' pom.xml)

if [ "$ACTUAL_VERSION" != "$NEW_VERSION" ]; then
  echo "ERROR: Version verification failed."
  echo "  Expected: $NEW_VERSION"
  echo "  Found:    $ACTUAL_VERSION"
  echo "root pom.xml may be in an inconsistent state. Review 'git diff pom.xml'"
  echo "before proceeding."
  exit 1
else
  echo "OK: root pom.xml version verified as $NEW_VERSION"
fi

echo "Step  5/10: git status"
git status

echo "Step  6/10: git diff --stat"
git diff --stat

echo "Step  7/10: Verifying noakweather-legacy/pom.xml was NOT modified"
if git diff --name-only | grep -q "^noakweather-legacy/pom.xml$"; then
  echo "WARNING: noakweather-legacy/pom.xml was modified!"
  echo "This module must stay at its own independent version (0.0.5)."
  echo "Review 'git diff noakweather-legacy/pom.xml' and revert if needed"
  echo "before proceeding to versions:commit."
  exit 1
else
  echo "OK: noakweather-legacy/pom.xml untouched."
fi

echo "Step  8/10: Entering noakweather-platform directory"
cd noakweather-platform || { echo "ERROR: noakweather-platform directory not found"; exit 1; }

echo "Step  9/10: mvn versions:commit"
mvn versions:commit
if [ $? -ne 0 ]; then
  echo "ERROR: mvn versions:commit failed"
  exit 1
fi

echo "Step 10/10: Returning to parent directory"
cd ..

echo "+++++++++++++++++++++++++++++++++++++++++++++"
echo "Version update complete: $NEW_VERSION"
echo "Review 'git diff --stat' above, then update CHANGELOG.md"
echo "before committing."
echo "+++++++++++++++++++++++++++++++++++++++++++++"
echo $?
