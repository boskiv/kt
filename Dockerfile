FROM hairyhenderson/gomplate:v2.6.0 as gomplate

FROM lachlanevenson/k8s-kubectl:v1.9.8 as kubectl

FROM golang:1.10 as build
WORKDIR /go/src/app
COPY *.go /go/src/app/
RUN go get
RUN go build -o /kt

FROM alpine:3.7 as app
RUN apk add -U ruby ruby-json
RUN gem install -N stackup:1.3.1
COPY --from=gomplate /gomplate /usr/bin/
COPY --from=kubectl /usr/local/bin/kubectl /usr/bin/
COPY --from=build /kt /usr/bin/
WORKDIR /app
ENTRYPOINT ["/usr/bin/kt"]
