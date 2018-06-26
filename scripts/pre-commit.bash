#!/usr/bin/env bash

set -e

GIT_DIR=$(git rev-parse --git-dir)
cd "$GIT_DIR/../"

echo "Running tests..."
mvn clean test
