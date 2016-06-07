#!/bin/bash

set -euo pipefail

LATEST_TAG=$(git tag | tail -n 1)

if [ "$TRAVIS_PULL_REQUEST" == "false" ] && [ "$LATEST_TAG" == "$TRAVIS_BRANCH" ]; then
	echo "Setting Maven release values"
	mvn versions:set -DnewVersion=$(git tag | tail -n 1)
        mvn versions:commit
	echo "Generating release notes from git history"
	git show -s --pretty=format:"%h - %<|(35)%an%s" $(git rev-list --tags --max-count=1)...$(git show | grep "^commit" | awk '{print $2}') | tee target/RELEASE_NOTES
fi


# Fetch all commit history so that SonarQube has exact blame information
# for issue auto-assignment
# This command can fail with "fatal: --unshallow on a complete repository does not make sense"
# if there are not enough commits in the Git repository (even if Travis executed git clone --depth 50).
# For this reason errors are ignored with "|| true"
git fetch --unshallow || true

export MAVEN_OPTS="-Xmx1G -Xms128m"
mvn cobertura:cobertura verify sonar:sonar \
    -Dmaven.test.redirectTestOutputToFile=false \
    -Dsonar.projectKey=$SONAR_PROJECT_KEY \
    -Dsonar.analysis.mode=issues \
    -Dsonar.github.pullRequest=$TRAVIS_PULL_REQUEST \
    -Dsonar.github.repository=$TRAVIS_REPO_SLUG \
    -Dsonar.github.oauth=$GITHUB_TOKEN \
    -Dsonar.host.url=$SONAR_HOST_URL \
    -Dsonar.login=$SONAR_TOKEN \
    -B -e -V
