FROM jenkins/jenkins:lts

USER root

# parametro DOCKER_GID do SO
ARG DOCKER_GID

ENV CLI_VERSION 18.06.3-ce

RUN apt-get update && \
    curl -fsSLO https://download.docker.com/linux/static/stable/x86_64/docker-${CLI_VERSION}.tgz && \
    tar xzvf docker-${CLI_VERSION}.tgz && \
    mv docker/docker /usr/local/bin && \
    rm -r docker docker-${CLI_VERSION}.tgz && \
    groupadd -g ${DOCKER_GID} docker && \
    usermod -aG docker jenkins && \
    apt-get clean

VOLUME /var/run/docker.sock

USER jenkins