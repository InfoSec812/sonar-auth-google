#!/bin/bash

set -euo pipefail

function installTravisTools {
  mkdir ~/.local
  curl -sSL https://github.com/SonarSource/travis-utils/tarball/v21 | tar zx --strip-components 1 -C ~/.local
  source ~/.local/bin/install
}
installTravisTools

function strongEcho {
  echo ""
  echo "================ $1 ================="
}

# temporary build of SonarQube 5.4-SNAPSHOT. Will be removed when SNAPSHOT shared repository will be available
# or when dependency on sonar-plugin-api will be RELEASE.
build "SonarSource/sonarqube" "7f427416a8ebfe5b68e6edf6f31cd372e36076d2"

if [ "${TRAVIS_BRANCH}" == "master" ] && [ "$TRAVIS_PULL_REQUEST" == "false" ]; then
  strongEcho 'Build and analyze commit in master'
  # this commit is master must be built and analyzed (with upload of report)
  mvn org.jacoco:jacoco-maven-plugin:prepare-agent verify sonar:sonar \
    -Pcoverage-per-test \
    -Dmaven.test.redirectTestOutputToFile=false \
    -Dsonar.host.url=$SONAR_HOST_URL \
    -Dsonar.login=$SONAR_TOKEN \
    -B -e -V

elif [ "$TRAVIS_PULL_REQUEST" != "false" ] && [ -n "$GITHUB_TOKEN" ]; then
  # For security reasons environment variables are not available on the pull requests
  # coming from outside repositories
  # http://docs.travis-ci.com/user/pull-requests/#Security-Restrictions-when-testing-Pull-Requests
  # That's why the analysis does not need to be executed if the variable GITHUB_TOKEN is not defined.

  strongEcho 'Build and analyze pull request'
  # this pull request must be built and analyzed (without upload of report)
  mvn org.jacoco:jacoco-maven-plugin:prepare-agent verify sonar:sonar \
    -Pcoverage-per-test \
    -Dmaven.test.redirectTestOutputToFile=false \
    -Dsonar.analysis.mode=issues \
    -Dsonar.github.pullRequest=$TRAVIS_PULL_REQUEST \
    -Dsonar.github.repository=$TRAVIS_REPO_SLUG \
    -Dsonar.github.oauth=$GITHUB_TOKEN \
    -Dsonar.host.url=$SONAR_HOST_URL \
    -Dsonar.login=$SONAR_TOKEN \
    -B -e -V

else
  strongEcho 'Build, no analysis'
  # Build branch, without any analysis

  # No need for Maven goal "install" as the generated JAR file does not need to be installed
  # in Maven local repository
  mvn verify -Dmaven.test.redirectTestOutputToFile=false -B -e -V
fi
