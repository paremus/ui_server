# The Paremus UI Server Repository

This repository contains core components for building a modular REST UI service. 

## Repository Contents

This repository contains:

#### _index

Generates OSGi index.xml containing all resources and dependencies needed to deploy.  It is used to resolve `com.paremus.ui.rest.app/app.bndrun` and is published so other projects can easily extend the UI.

#### com.paremus.ui.client

Provides a `Servlet` to serve the Javascript client. During development the location of the client can be specified by setting the system property `com.paremus.ui.client.dir` to the client directory.

For production deployment the Javascript client is embedded as a resource in this bundle. It is obtained from the artifact published by the `com.paremus.ui:js_client` project.

#### com.paremus.ui.metaconfig

Aggregates `Metatype` services from remote nodes to provide a distributed configuration service.

Provides a service to map framework UUID into hostnames (for friendly presentation in UI).

Provides basic host information (model, memory, OS, etc), using JNI library (https://github.com/oshi/oshi)

#### com.paremus.ui.rest

Core REST services

* API DTOs
* AbstractResource that handles query filters and pagination
* Login resource (default user/password is admin/admin)
* Watch support using Server Sent Events
  * UI is notified when watched resource changes
* Event resources - generic watchable events

#### com.paremus.ui.rest.app

App setup/configuration.

app.bndrun for stand-alone testing.

#### com.paremus.ui.rest.config

Distributed `config` resource implemented using `Metaconfig` service.

#### com.paremus.ui.rest.fake

Fake (test) resources so UI can be tested standalone. 

## How to extend this repository

This repository contains some "core" rest services but also many fake/test resources.

To build a real UI server, you should resolve your project against the `_index` of this repository, but exclude `com.paremus.ui.app` and `com.paremus.ui.rest.fake` from the resolution. Instead provide your own app/setup and real implementations of the fake (or other) resources.

The Javascript UI is dynamically configurable (you can control which resources it shows from the UI server config), but it is not currently modular. If you want the UI client to show new resources, you need to extend the current UI client and rebuild the whole Javascript client project.

## How to build this repository

This repository can be built using Maven 3.5.4 and Java 11. 

### Build profiles

By default the build will run with all tests, and lenient checks on copyright headers. To enable strict copyright checking (required for deployment) then the `strict-license-check` profile should be used, for example

    mvn -P strict-license-check clean install

If you make changes and do encounter licensing errors then the license headers can be regenerated using the `generate-licenses` profile

```
mvn -P generate-licenses process-sources
```
## end
