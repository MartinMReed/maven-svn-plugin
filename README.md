# Usage
A [Maven](http://maven.apache.org/download.html) plugin to support updating svn:externals to point to the SCM Url of a given dependency. The target use-case for this is to update the svn:externals property while performing a release. This ensures that the project build is reproducible.

# Build or Download
To build this you need to use [Maven](http://maven.apache.org/download.html) with the [hbc-maven-core](https://github.com/hardisonbrewing/hbc-maven-core) project. Alternatively you can pull the latest version of hbc-maven-core from [http://repo.hardisonbrewing.org](http://repo.hardisonbrewing.org) (see repository settings below).

# Pulling the latest version from Nexus
To pull the latest version of the plugin you will need to update your [remote repository](http://maven.apache.org/guides/introduction/introduction-to-repositories.html) settings under your `.m2/settings.xml`.

	<repositories>
		<repository>
			<id>hardisonbrewing-releases</id>
			<name>hardisonbrewing-releases</name>
			<url>http://repo.hardisonbrewing.org/content/repositories/releases/</url>
		</repository>
		<repository>
			<id>hardisonbrewing-snapshots</id>
			<name>hardisonbrewing-snapshots</name>
			<url>http://repo.hardisonbrewing.org/content/repositories/snapshots/</url>
		</repository>
	</repositories>

To download this plugin without building it manually, you can add the following remote plugin repository:

	<pluginRepositories>
		<pluginRepository>
			<id>hardisonbrewing-releases</id>
			<name>hardisonbrewing-releases</name>
			<url>http://repo.hardisonbrewing.org/content/repositories/releases/</url>
		</pluginRepository>
		<pluginRepository>
			<id>hardisonbrewing-snapshots</id>
			<name>hardisonbrewing-snapshots</name>
			<url>http://repo.hardisonbrewing.org/content/repositories/snapshots/</url>
		</pluginRepository>
	</pluginRepositories>

# Sample: Project POM
	
	<project xmlns="http://maven.apache.org/POM/4.0.0"
	  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	  xsi:schemaLocation="http://maven.apache.org/POM/4.0.0">
	  <modelVersion>4.0.0</modelVersion>
	  <groupId>net.hardisonbrewing</groupId>
	  <artifactId>komodododo</artifactId>
	  <version>0.0.1-SNAPSHOT</version>
	  <name>${project.artifactId}</name>
	  <packaging>pom</packaging>
	  <build>
	    <plugins>
	      <plugin>
	        <artifactId>maven-release-plugin</artifactId>
	        <extensions>true</extensions>
	        <configuration>
	          <preparationGoals>verify svn:prepare-externals</preparationGoals>
	          <completionGoals>svn:update-externals</completionGoals>
	        </configuration>
	      </plugin>
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
	    </plugins>
	  </build>
	</project>

# License
GNU Lesser General Public License, Version 3.0.