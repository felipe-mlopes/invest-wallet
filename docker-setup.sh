#!/bin/bash

# Atualizar repositórios e instalar Docker CLI
echo "Instalando o Docker CLI no container Jenkins..."

apt-get update && apt-get install -y \
    apt-transport-https \
    ca-certificates \
    curl \
    gnupg \
    lsb-release && \
    git \
    curl -fsSL https://download.docker.com/linux/debian/gpg | gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg && \
    echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/debian $(lsb_release -cs) stable" > /etc/apt/sources.list.d/docker.list && \
    apt-get update && \
    apt-get install -y docker-ce-cli && \
    rm -rf /var/lib/apt/lists/*

# Adicionando o usuário Jenkins ao grupo Docker
groupadd -g 999 docker || true
usermod -aG docker jenkins || true

# Garantir que o Jenkins tem permissões no volume
chown -R 1000:1000 /var/jenkins_home

echo "Docker CLI instalado com sucesso!"

# Garantir que o Jenkins inicie normalmente
exec "$@"
