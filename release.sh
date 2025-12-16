#!/bin/bash
set -e

./mvnw release:prepare && rm -f release.properties

git push origin main
git push origin --tags
