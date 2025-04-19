FROM ghcr.io/sparfenyuk/mcp-proxy:latest

ENV VERSION=0.2.0
ENV ARCHIVE_NAME=github-mcp-server_Linux_x86_64.tar.gz

RUN wget https://github.com/github/github-mcp-server/releases/download/v$VERSION/$ARCHIVE_NAME -O /tmp/$ARCHIVE_NAME \
    && tar -xzvf /tmp/$ARCHIVE_NAME -C /opt \
    && rm /tmp/$ARCHIVE_NAME

RUN chmod +x /opt/github-mcp-server
