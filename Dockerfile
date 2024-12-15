FROM jenkins/jenkins:lts-jdk17

# Switch to root user for installations
USER root

# Install Maven
RUN apt-get update && \
    apt-get install -y maven && \
    mvn --version

# Switch back to jenkins user
USER jenkins