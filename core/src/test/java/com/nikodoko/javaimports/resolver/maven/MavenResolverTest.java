package com.nikodoko.javaimports.resolver.maven;

import static com.google.common.truth.Truth8.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.nikodoko.javaimports.Options;
import com.nikodoko.javaimports.parser.Import;
import com.nikodoko.javaimports.resolver.Resolver;
import com.nikodoko.javaimports.resolver.Resolvers;
import com.nikodoko.packagetest.Export;
import com.nikodoko.packagetest.Exported;
import com.nikodoko.packagetest.Module;
import com.nikodoko.packagetest.exporters.Kind;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class MavenResolverTest {
  Exported project;

  @AfterEach
  void cleanup() throws Exception {
    project.cleanup();
  }

  @Test
  void testThatTopLevelClassesAreFound() throws Exception {
    Module module =
        new Module(
            "test.module",
            ImmutableMap.of(
                "Main.java",
                "package test.module; public class Main {}",
                "second/Second.java",
                "package test.module.second; public class Second {}"));
    project = Export.of(Kind.MAVEN, ImmutableList.of(module));
    Path target = project.file(module.name(), "Main.java").get();

    Resolver resolver = Resolvers.basedOnEnvironment(target, Options.defaults());
    assertThat(resolver.find("Second")).hasValue(new Import("Second", "test.module.second", false));
  }

  @Test
  void testThatFileBeingResolvedIsNotFound() throws Exception {
    Module module =
        new Module(
            "test.module",
            ImmutableMap.of(
                "Main.java",
                "package test.module; public class Main {}",
                "second/Second.java",
                "package test.module.second; public class Second {}"));
    project = Export.of(Kind.MAVEN, ImmutableList.of(module));
    Path target = project.file(module.name(), "Main.java").get();

    Resolver resolver = Resolvers.basedOnEnvironment(target, Options.defaults());
    assertThat(resolver.find("Main")).isEmpty();
  }

  @Test
  void testThatFindPicksTheClosestImport() throws Exception {
    Module module =
        new Module(
            "test.module",
            ImmutableMap.of(
                "Main.java",
                "package test.module; public class Main {}",
                "other/second/Second.java",
                "package test.module.other.second; public class Second {}",
                "second/Second.java",
                "package test.module.second; public class Second {}"));
    project = Export.of(Kind.MAVEN, ImmutableList.of(module));
    Path target = project.file(module.name(), "Main.java").get();

    Resolver resolver = Resolvers.basedOnEnvironment(target, Options.defaults());
    assertThat(resolver.find("Second")).hasValue(new Import("Second", "test.module.second", false));
  }
}
