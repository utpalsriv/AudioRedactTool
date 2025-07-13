# created by scone-gen 576de8b
FROM docker.repo.local.sfdc.net/sfci/docker-images/sfdc_rhel9_openjdk17/sfdc_rhel9_onejdk17_basic:55

ENV version=0.0.1

LABEL maintainer falcon-codelabs-support@salesforce.com

RUN groupadd --system --gid 7447 scone
RUN adduser --system --gid 7447 --uid 7447 --shell /bin/bash --home /home/scone scone

WORKDIR /home/scone
RUN chown -R scone:scone .
USER scone

EXPOSE 7442 15372
ENTRYPOINT ["./entrypoint.sh"]

COPY --chown=scone:scone target/codelab1-sampleapp-java-$version.jar service.jar
COPY --chown=scone:scone entrypoint.sh entrypoint.sh
