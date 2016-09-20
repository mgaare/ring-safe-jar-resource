# ring-safe-jar-resource

This is a drop-in replacement for Ring's `wrap-resource` middleware
that works around what appears to be a Java bug affecting Ring's
version.

The bug is triggered when creating resource responses for files inside
a jar, from an app running from that same jar (ie, running a
ring-based app from an uberjar).

Part of the codepath for creating resource responses is a call to
`.getLastModified` on the resource URL's connection object. Operations
on the connection object are meant to be safe, but this particular
operation is causing a file handle to be opened on the jar which
doesn't get closed until a GC run.

This library's replacement, `wrap-safe-jar-resource`, works around
this problem by not trying to read the last modified time from the
JAR, rather it returns a new Date object.

## Usage

``` clojure
(require '[mssngvwls.ring.middleware.safe-jar-resource :as resource])

(-> my-app-routes
    (resource/wrap-safe-jar-resource "public"))
```

## License

Copyright © 2016 Michael Gaare

Released under the MIT license, like Ring.
