package com.nikodoko.javaimports.environment.maven;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A simplified representation of a Maven POM, exposing only the dependencies. It can be extended
 * with other {@code FlatPom}s to enrich it.
 */
class FlatPom {
  private List<MavenDependency> dependencies;
  private Map<MavenDependency.Versionless, String> versionByManagedDependencies;
  private Properties properties;

  private FlatPom(
      List<MavenDependency> dependencies,
      List<MavenDependency> managedDependencies,
      Properties properties) {
    this.dependencies = dependencies;
    this.versionByManagedDependencies =
        managedDependencies.stream()
            .collect(Collectors.toMap(MavenDependency::hideVersion, d -> d.version));
    this.properties = properties;
    useManagedVersionWhenNeeded();
    substitutePropertiesWhenPossible();
  }

  private void useManagedVersionWhenNeeded() {
    dependencies =
        dependencies.stream()
            .map(
                d -> {
                  if (d.version != null) {
                    return d;
                  }

                  var managedVersion = versionByManagedDependencies.get(d.hideVersion());
                  return new MavenDependency(d.groupId, d.artifactId, managedVersion);
                })
            .collect(Collectors.toList());
  }

  private void substitutePropertiesWhenPossible() {
    dependencies =
        dependencies.stream()
            .map(
                d -> {
                  if (!PropertyKeyExtractor.isProperty(d.version)) {
                    return d;
                  }

                  return substitutePropertyIfPossible(d);
                })
            .collect(Collectors.toList());
  }

  private MavenDependency substitutePropertyIfPossible(MavenDependency dependency) {
    var version =
        properties.getProperty(
            PropertyKeyExtractor.extract(dependency.version), dependency.version);
    return new MavenDependency(dependency.groupId, dependency.artifactId, version);
  }

  /**
   * If the pom is already well defined, don't do anything. Otherwise, used the managed version and
   * resolve properties.
   */
  void merge(FlatPom other) {
    if (isWellDefined()) {
      return;
    }

    other.versionByManagedDependencies.forEach(
        (k, v) -> this.versionByManagedDependencies.putIfAbsent(k, v));
    // The other properties have lower priority so we put them as defaults
    var newProperties = new Properties(other.properties);
    properties.forEach((k, v) -> newProperties.setProperty((String) k, (String) v));
    this.properties = newProperties;
    useManagedVersionWhenNeeded();
    substitutePropertiesWhenPossible();
  }

  List<MavenDependency> dependencies() {
    return dependencies;
  }

  static Builder builder() {
    return new Builder();
  }

  /**
   * Returns {@code true} if all dependencies have a well defined version, i.e. a version that is
   * neither null nor a reference to a property.
   */
  boolean isWellDefined() {
    return dependencies.stream()
        .allMatch(d -> d.version != null && !PropertyKeyExtractor.isProperty(d.version));
  }

  private static class PropertyKeyExtractor {
    private static final Pattern PATTERN = Pattern.compile("\\$\\{(?<parameter>\\S+)\\}");

    static boolean isProperty(String property) {
      if (property == null) {
        return false;
      }

      var m = PATTERN.matcher(property);
      return m.matches();
    }

    static String extract(String property) {
      if (!isProperty(property)) {
        throw new IllegalArgumentException("Invalid property: " + property);
      }

      var m = PATTERN.matcher(property);
      m.matches();
      return m.group("parameter");
    }
  }

  static class Builder {
    private List<MavenDependency> dependencies = new ArrayList<>();
    private List<MavenDependency> managedDependencies = new ArrayList<>();
    private Properties properties = new Properties();

    Builder dependencies(List<MavenDependency> dependencies) {
      this.dependencies = dependencies;
      return this;
    }

    Builder managedDependencies(List<MavenDependency> managedDependencies) {
      this.managedDependencies = managedDependencies;
      return this;
    }

    Builder properties(Properties properties) {
      this.properties = properties;
      return this;
    }

    FlatPom build() {
      return new FlatPom(dependencies, managedDependencies, properties);
    }
  }
}
