# annoy4s

[![Build Status](https://travis-ci.org/pishen/annoy4s.svg?branch=master)](https://travis-ci.org/pishen/annoy4s)
[![Download](https://api.bintray.com/packages/pishen/maven/annoy4s/images/download.svg)](https://bintray.com/pishen/maven/annoy4s/_latestVersion)

A JNA wrapper around [spotify/annoy](https://github.com/spotify/annoy) which calls the C++ library of annoy directly from Scala/JVM.

## Installation

Since the C++ library is platform-dependent, you'll have to compile the C++ part by yourself on your machine:

1. Clone this repository.
2. Check the values of `organization` and `version` in `build.sbt`, you may change it to the value you want, it's recommended to let `version` have the `-SNAPSHOT` suffix.
3. Run `compileNative` in sbt (Note that g++ installation is required).
4. Run `test` in sbt to see if the native library is successfully compiled.
5. Run `publishLocal` in sbt to install annoy4s on your machine.

Now you can add the library dependency as (organization and version may be different according to your settings):
```
libraryDependencies += "net.pishen" %% "annoy4s" % version
```

For people who don't want to publish the Scala part by themselves, a version without native library is available on Bintray:
```
resolvers += Resolver.bintrayRepo("pishen", "maven")

libraryDependencies += "net.pishen" %% "annoy4s" % "0.4.0"
```

You still have to generate the library file using `compileNative` as above. The `g++` command will indicate the output library file (`libannoy.so` or `libannoy.dylib`) location. Put this file on the [library search paths](http://java-native-access.github.io/jna/4.4.0/javadoc/com/sun/jna/NativeLibrary.html#library_search_paths) for JNA to find it.

## Usage

Create and query the index in memory mode:
```scala
import annoy4s._

val annoy = Annoy.create("./input_vectors", numOfTrees = 10, metric = Euclidean, verbose = true)

val result: Option[Seq[(Int, Float)]] = annoy.query(itemId, maxReturnSize = 30)
```

* The format of `./input_vectors` is `<item id> <vector>` for each line, here is an example:
```
3 0.2 -1.5 0.3
5 0.4 0.01 -0.5
0 1.1 0.9 -0.1
2 1.2 0.8 0.2
```
* `metric` could be `Euclidean` or `Angular`.
* `result` is a tuple list of id and distances, where the query item is itself contained.

To use the index in disk mode, one need to provide an `outputDir`:
```scala
val annoy = Annoy.create("./input_vectors", 10, outputDir = "./annoy_result/", Euclidean)

val result: Option[Seq[(Int, Float)]] = annoy.query(itemId, maxReturnSize = 30)

annoy.close()

// load an created index
val reloadedAnnoy = Annoy.load("./annoy_result/")

val reloadedResult: Option[Seq[(Int, Float)]] = reloadedAnnoy.query(itemId, 30)
```

