FROM jenkins/jenkins:lts

USER root

# parametro DOCKER_GID do SO
ARG DOCKER_GID=998

ENV CLI_VERSION 18.06.3-ce
ENV JENKINS_OPTS="--httpPort=9090"
ENV MAVEN_VERSION 3.9.6

RUN apt-get update && \
    apt-get install -y maven git && \
    curl -fsSLO https://download.docker.com/linux/static/stable/x86_64/docker-${CLI_VERSION}.tgz && \
    tar xzvf docker-${CLI_VERSION}.tgz && \
    mv docker/docker /usr/local/bin && \
    rm -r docker docker-${CLI_VERSION}.tgz && \
    groupadd -g ${DOCKER_GID} docker || groupmod -g ${DOCKER_GID} docker && \
    usermod -aG docker jenkins && \
    chmod 666 /var/run/docker.sock || true

RUN touch /var/run/docker.sock && \
    chmod 666 /var/run/docker.sock && \
    chown root:docker /var/run/docker.sock

USER jenkins