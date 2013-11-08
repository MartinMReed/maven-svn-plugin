# Usage
A [Maven](http://maven.apache.org/download.html) plugin to support:  
* Updating `svn:externals` to point to the SCM Url of a given dependency.
 * This can be used to update the `svn:externals` property while performing a release. This ensures that the project tag is reproducible.
* Print line count statistics for source files.
 * This can be the full line count in HEAD or the lines added since a given revision.

## Build or Download
Dependency Projects:
* [hbc-maven-core](https://github.com/hardisonbrewing/hbc-maven-core)

Nexus: [http://repo.hardisonbrewing.org](http://repo.hardisonbrewing.org)  
Continuous Integration: [Bamboo Status](http://bamboo.hardisonbrewing.org/browse/MVN-SVN)

# Updating svn:externals
During a release, add `svn:prepare-externals` to the `<preparationGoals/>`, and `svn:update-externals` to the `<completionGoals/>`.

The `svn:externals` are updated by taking the SCM url from the released POM of the dependency. The released POM will be pointing to the tag location, so this is what will be used.

```xml
<plugin>
  <artifactId>maven-release-plugin</artifactId>
  <extensions>true</extensions>
  <configuration>
    <preparationGoals>verify svn:prepare-externals</preparationGoals>
    <completionGoals>svn:update-externals</completionGoals>
  </configuration>
</plugin>
```

The dependency to update with should be listed under the plugin configuration.

```xml
<plugin>
  <groupId>org.hardisonbrewing</groupId>
  <artifactId>maven-svn-plugin</artifactId>
  <extensions>true</extensions>
  <configuration>
    <externals>
      <external>
        <path>core</path>
        <dependency>
          <groupId>net.hardisonbrewing</groupId>
          <artifactId>komodododo-core</artifactId>
          <version>1.0.45</version>
          <type>pom</type>
        </dependency>
      </external>
    </externals>
  </configuration>
</plugin>
```

# License
GNU Lesser General Public License, Version 3.0.
