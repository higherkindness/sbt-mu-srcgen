[comment]: <> (Don't edit this file!)
[comment]: <> (It is automatically updated after every release of https://github.com/47degrees/.github)
[comment]: <> (If you want to suggest a change, please open a PR or issue in that repository)

[![License](https://img.shields.io/badge/license-Apache%202-blue.svg)](https://raw.githubusercontent.com/higherkindness/mu-scala/master/LICENSE) 
[![Join the chat at https://gitter.im/47deg/mu](https://badges.gitter.im/47deg/mu.svg)](https://gitter.im/47deg/mu?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

# @NAME@

[mu-scala] is a purely functional library for building [RPC] endpoint-based services with support for [RPC] and [HTTP/2].

[@NAME@] brings the ability to generate [mu-scala] protocols, services, and clients in your Scala program.

## Documentation

For installing this plugin, add the following line to your `plugins.sbt` file:

```sbt mdoc:silent
addSbtPlugin("io.higherkindness" % "sbt-mu-srcgen" % "@VERSION@")
```

### NOTE

For any users using version `0.22.x` and below, the `SrcGenPlugin` is enabled on every module by default.  However, for everyone using 
version `0.23.x` and beyond, you'll need to manually enable the plugin for any module for which you want to 
auto-generate [mu-scala] code, like such:

```sbt mdoc:silent
.enablePlugins(SrcGenPlugin)
```

**This is a breaking change between the versions**, so be sure to make sure that you're choosing your modules to enable source generation
intentionally if you want to upgrade this library.

The full documentation is available at the [mu](https://higherkindness.io/mu-scala/guides/generate-sources-from-idl) site.

[RPC]: https://en.wikipedia.org/wiki/Remote_procedure_call
[HTTP/2]: https://http2.github.io/
[gRPC]: https://grpc.io/
[mu-scala]: https://higherkindness.github.io/mu-scala/

# Copyright

@NAME@ is designed and developed by @ORG_NAME@

Copyright (C)  @YEAR_RANGE@ @COPYRIGHT_OWNER@
