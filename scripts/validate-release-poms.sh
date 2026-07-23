#!/usr/bin/env bash

set -euo pipefail

release_version="${1:?Expected the release version as the first argument}"

for pom in pom.xml thegreeklab-core/pom.xml; do
  sed -i "s#<revision>[^<]*</revision>#<revision>$release_version</revision>#" "$pom"
done

./mvnw -B -ntp -DskipTests process-resources

RELEASE_VERSION="$release_version" python3 - <<'PY'
import os
import xml.etree.ElementTree as element_tree

namespace = "{http://maven.apache.org/POM/4.0.0}"
expected = os.environ["RELEASE_VERSION"]
paths = [
    ".flattened-pom.xml",
    "thegreeklab-core/.flattened-pom.xml",
    "thegreeklab-visualization/.flattened-pom.xml",
]
poms = [element_tree.parse(path).getroot() for path in paths]
versions = [
    poms[0].find(namespace + "version").text,
    poms[1].find(namespace + "version").text,
    poms[2].find(namespace + "parent").find(namespace + "version").text,
]

assert versions == [expected] * 3, (
    f"Expected all published POM versions to be {expected}, got {versions}"
)
assert all("SNAPSHOT" not in open(path, encoding="utf-8").read() for path in paths), (
    "Flattened release POM contains SNAPSHOT"
)
PY
