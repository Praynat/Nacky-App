# Base Ubuntu pour Codespaces
FROM mcr.microsoft.com/devcontainers/base:ubuntu

# Outils de base
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl git xz-utils unzip ca-certificates libglu1-mesa \
    && rm -rf /var/lib/apt/lists/*

# Installer Flutter (stable)
ENV FLUTTER_HOME=/opt/flutter
RUN git clone https://github.com/flutter/flutter.git -b stable $FLUTTER_HOME
ENV PATH="$FLUTTER_HOME/bin:$PATH"

# Pré-télécharger les artefacts de base (pas d’Android ici)
RUN flutter config --no-analytics && \
    flutter precache --universal && \
    flutter doctor -v

# Utilisateur non-root fourni par devcontainers
USER vscode
WORKDIR /workspaces