# Gameon Quarkus Player

A recreation of the GameOn Player service using Quarkus, rather than JEE.

# Build instructions. 

`git clone https://github.com/BarDweller/gameon-quarkusplayer.git`

If you have graalvm etc locally.. then
`./mvnw clean package -Dnative`

If you don't, and you have Docker (or podman), you can use.. 
`./mvnw clean package -Dnative -Dquarkus.native.container-build=true`

Finally build the container using 
`docker build -t gameontext/quarkus-player:1.0 -f src/main/docker/Dockerfile.native .`

(or, if using RancherDesktop for testing.. )
`nerdctl build --namespace k8s.io -t gameontext/quarkus-player:1.0 -f src/main/docker/Dockerfile.native .`
(if testing locally with gameon running in kube remotely, 
you can use `kubectl port-forward service/couchdb 5984:5984 -n gameon-system` and a `COUCHDB_URL` of `http://localhost:5984`
retrieve the COUCHDB_USER & COUCHDB_PASSWORD from the gameon global-config ConfigMap in the gameon-system namespace )

# Config

This app requires the following env vars to be set. 

| Env var | Purpose |
|---------|---------|
|JWT_PUBLIC_CERT| The pem certificate as a multiline env var (eg, ----BEGIN CERTIFICATE--- etc.. etc etc) |
|COUCHDB_USER| userid to talk to couchdb |
|COUCHDB_PASSWORD| password to talk to couchdb |
|COUCHDB_SERVICE_URL|url to talk to couchdb|
|SYSTEM_ID|the id to allow access to sensitive data with (probably `dummy:dummy.AnonymousUser` if testing locally)|




