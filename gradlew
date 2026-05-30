#!/bin/sh
# Gradle wrapper script
GRADLE_VERSION=8.3
GRADLE_DIST_URL=https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip

# Find JAVA_HOME
if [ -z "$JAVA_HOME" ]; then
  JAVA_CMD=java
else
  JAVA_CMD="$JAVA_HOME/bin/java"
fi

# Download gradle wrapper if not present
GRADLE_USER_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}"
GRADLE_WRAPPER_JAR="$GRADLE_USER_HOME/wrapper/dists/gradle-${GRADLE_VERSION}-bin/*/gradle-${GRADLE_VERSION}/bin/gradle"

exec "$JAVA_CMD" \
  -classpath "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" \
  org.gradle.wrapper.GradleWrapperMain \
  "$@"
