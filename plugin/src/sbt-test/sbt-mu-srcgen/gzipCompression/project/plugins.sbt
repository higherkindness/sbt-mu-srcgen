resolvers ++= Resolver.sonatypeOssRepos("snapshots")
addSbtPlugin("io.higherkindness" %% "sbt-mu-srcgen" % sys.props("version"))
