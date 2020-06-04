resolvers += Resolver.sonatypeRepo("releases")

addSbtPlugin("io.github.davidgregory084" % "sbt-tpolecat"      % "0.1.12")
addSbtPlugin("org.scalameta"             % "sbt-scalafmt"      % "2.3.4")
addSbtPlugin("org.scalameta"             % "sbt-mdoc"          % "2.1.3")
addSbtPlugin("com.geirsson"              % "sbt-ci-release"    % "1.5.3")
addSbtPlugin("de.heikoseeberger"         % "sbt-header"        % "5.6.0")
addSbtPlugin("com.alejandrohdezma"       % "sbt-github-header" % "0.8.1")
addSbtPlugin("com.alejandrohdezma"       % "sbt-github-mdoc"   % "0.8.1")
