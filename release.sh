#!/bin/bash
set -e

./mvnw release:prepare

git push origin main
git push origin --tags
