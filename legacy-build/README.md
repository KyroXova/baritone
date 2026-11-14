# Minecraft 1.7.10 development build

This build targets Forge 1.7.10 using the Baritone 26.3 source files in the
repository root. The port is incomplete: compiling the full source tree and
producing a working mod are not yet supported.

Run Gradle with JDK 21 and make a JDK 8 installation available to Gradle's
toolchain detection. From the repository root, run:

```text
gradlew -p legacy-build tasks
gradlew -p legacy-build compileJava
```

The `compileJava` task is used to identify remaining compatibility work. It is
expected to fail until the source and its dependencies have been ported.
The original 26.3 build remains available separately in the root.
