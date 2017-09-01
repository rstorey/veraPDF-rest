# See https://docs.docker.com/engine/userguide/eng-image/multistage-build/


FROM maven as builder
RUN mkdir /build
WORKDIR /build
# RUN git clone https://github.com/rstorey/dropwizard-swagger.git
RUN git clone https://github.com/rstorey/veraPDF-rest.git
RUN git clone https://github.com/bfosupport/pdfa-testsuite.git
RUN git clone https://github.com/veraPDF/veraPDF-corpus
# RUN cd /build/dropwizard-swagger && git checkout master && mvn clean install -DskipTests=true
RUN cd /build/veraPDF-rest && git checkout integration && mvn clean package


FROM openjdk:8-jre-alpine

ENV VERAPDF_REST_VERSION=0.1.0-SNAPSHOT

# Since this is a running network service we'll create an unprivileged account
# which will be used to perform the rest of the work and run the actual service:

# Debian:
# RUN useradd --system --user-group --home-dir=/opt/verapdf-rest verapdf-rest
# Alpine / Busybox:
RUN install -d -o root -g root -m 755 /opt && adduser -h /opt/verapdf-rest -S verapdf-rest
USER verapdf-rest
WORKDIR /opt/verapdf-rest

COPY --from=builder /build/veraPDF-rest/target/verapdf-rest-${VERAPDF_REST_VERSION}.jar /opt/verapdf-rest/
COPY --from=builder /build/pdfa-testsuite /opt/pdfa-testsuite
COPY --from=builder /build/veraPDF-corpus /opt/veraPDF-corpus
COPY --from=builder /build/veraPDF-rest/server.yml /opt/verapdf-rest/server.yml

EXPOSE 8080
ENTRYPOINT java -jar /opt/verapdf-rest/verapdf-rest-${VERAPDF_REST_VERSION}.jar server server.yml
