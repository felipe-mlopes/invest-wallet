FROM jenkins/jenkins:lts-jdk17

# if we want to install via apt
USER root
RUN apt-get update && apt-get install -y ruby make more-thing-here

# drop back to the regular jenkins user - good practice
USER jenkins
