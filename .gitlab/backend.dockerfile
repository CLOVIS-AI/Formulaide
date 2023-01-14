FROM alpine:latest as builder

COPY backend.tar /home/server/server.tar
WORKDIR /home/server
RUN tar -xf server.tar
RUN mkdir -p extracted
RUN mv backend-*/* extracted

FROM alpine:latest

RUN apk add --no-cache openjdk17-jre-headless
COPY --from=builder /home/server/extracted /opt/formulaide

WORKDIR /opt/formulaide
EXPOSE 9000
ENTRYPOINT [ "/opt/formulaide/bin/backend" ]
