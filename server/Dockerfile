FROM alpine:latest

RUN apk add --no-cache openjdk17-jre-headless

COPY build/distributions/server-*.tar /home/server/server.tar
RUN cd /home/server/ && \
	tar -xf server.tar && \
	rm server.tar && \
	mv server-*/* . && \
	rmdir server-*

EXPOSE 8000

ENTRYPOINT [ "/home/server/bin/server" ]
