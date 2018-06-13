FROM hairyhenderson/gomplate:v2.6.0 as gomplate

FROM lachlanevenson/k8s-kubectl:v1.9.8 as kubectl

FROM alpine:3.7 as app
RUN apk add -U bash
COPY --from=gomplate /gomplate /usr/bin/
COPY --from=kubectl /usr/local/bin/kubectl /usr/bin/
COPY ./kt /usr/bin/
WORKDIR /app
ENTRYPOINT ["/usr/bin/kt"]
