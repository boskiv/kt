FROM hairyhenderson/gomplate:v2.6.0 as gomplate

FROM lachlanevenson/k8s-kubectl:v1.11.4 as kubectl

FROM alpine:3.7 as app
COPY --from=gomplate /gomplate /usr/bin/
COPY --from=kubectl /usr/local/bin/kubectl /usr/bin/
RUN apk add -U bash ruby ruby-json jq python3
RUN gem install -N stackup:1.3.1 yaml2json:0.0.2
RUN pip3 install awscli==1.15.53 cfn-flip==1.0.3
COPY ./scripts/ /usr/bin/
COPY ./kt /usr/bin/
WORKDIR /app
ENTRYPOINT ["/usr/bin/kt"]
