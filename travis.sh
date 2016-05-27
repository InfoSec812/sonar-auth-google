#!/bin/bash

set -euo pipefail

if [ "${TRAVIS_BRANCH}" == "master" ] && [ "$TRAVIS_PULL_REQUEST" == "false" ]; then
  echo '======= Build, and analyze master, deploy to GitHub Releases'

  # Fetch all commit history so that SonarQube has exact blame information
  # for issue auto-assignment
  # This command can fail with "fatal: --unshallow on a complete repository does not make sense"
  # if there are not enough commits in the Git repository (even if Travis executed git clone --depth 50).
  # For this reason errors are ignored with "|| true"
  git fetch --unshallow || true

  export MAVEN_OPTS="-Xmx1536m -Xms128m"
  mvn cobertura:cobertura verify sonar:sonar \
      -Pcoverage-per-test \
      -Dmaven.test.redirectTestOutputToFile=false \
      -Dsonar.projectKey=$SONAR_PROJECT_KEY \
      -Dsonar.host.url=$SONAR_HOST_URL \
      -Dsonar.login=$SONAR_TOKEN \
      -B -e -V

elif [ "$TRAVIS_PULL_REQUEST" != "false" ] && [ -n "${GITHUB_TOKEN:-}" ]; then
  echo '======= Build and analyze pull request, no deploy'

  # No need for Maven phase "install" as the generated JAR files do not need to be installed
  # in Maven local repository. Phase "verify" is enough.

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

else
  echo '======= Build, no analysis, no deploy'

  # No need for Maven phase "install" as the generated JAR files do not need to be installed
  # in Maven local repository. Phase "verify" is enough.

  mvn verify \
      -Dmaven.test.redirectTestOutputToFile=false \
      -B -e -V
fi

echo "Generating release notes from git history"
git show -s --pretty=format:"%h - %<|(35)%an%s" $(git rev-list --tags --max-count=1) $(git show | grep "^commit" | awk '{print $2}') | tee target/RELEASE_NOTES
