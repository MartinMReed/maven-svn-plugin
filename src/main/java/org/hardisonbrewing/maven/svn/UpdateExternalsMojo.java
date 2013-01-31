package org.hardisonbrewing.maven.svn;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Scm;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;
import org.hardisonbrewing.maven.core.DependencyService;
import org.hardisonbrewing.maven.core.FileUtils;
import org.hardisonbrewing.maven.core.JoJoMojoImpl;
import org.hardisonbrewing.maven.core.ProjectService;
import org.hardisonbrewing.maven.core.TargetDirectoryService;
import org.hardisonbrewing.maven.core.cli.CommandLineService;

/**
 * @goal update-externals
 * @phase update-externals
 */
public final class UpdateExternalsMojo extends JoJoMojoImpl {

    /**
     * @parameter
     */
    private External[] externals;

    @Override
    public final void execute() throws MojoExecutionException, MojoFailureException {

        if ( externals == null ) {
            getLog().error( "Must specify <externals/> list!" );
            throw new IllegalStateException();
        }

        Properties properties = loadExternals();

        for (External external : externals) {
            try {
                updateExternal( properties, external );
            }
            catch (Exception e) {
                throw new IllegalStateException( e );
            }
        }

        try {
            writeExternals( properties );
        }
        catch (Exception e) {
            throw new IllegalStateException( e );
        }

        List<String> cmd = new LinkedList<String>();
        cmd.add( "svn" );
        cmd.add( "update" );
        execute( cmd );
    }

    @Override
    protected Commandline buildCommandline( List<String> cmd ) {

        try {
            return CommandLineService.build( cmd );
        }
        catch (CommandLineException e) {
            throw new IllegalStateException( e.getMessage() );
        }
    }

    private void updateExternal( Properties properties, External external ) throws Exception {

        Dependency dependency = external.dependency;

        if ( dependency == null ) {
            getLog().error( "Must specify external dependency!" );
            throw new IllegalStateException();
        }

        if ( external.path == null ) {
            getLog().error( "No path specified for external: " + dependency.getGroupId() + ":" + dependency.getArtifactId() );
            throw new IllegalStateException();
        }

        Artifact artifact = DependencyService.createResolvedArtifact( dependency );
        MavenProject project = ProjectService.getProject( artifact );
        Scm scm = project.getScm();

        if ( scm == null ) {
            getLog().error( "No SCM specified for " + dependency.getGroupId() + ":" + dependency.getArtifactId() );
            throw new IllegalStateException();
        }

        String url = scm.getUrl();

        if ( url == null || url.length() == 0 ) {
            getLog().error( "No SCM Url specified for " + dependency.getGroupId() + ":" + dependency.getArtifactId() );
            throw new IllegalStateException();
        }

        properties.put( external.path, url );
    }

    private void writeExternals( Properties properties ) throws Exception {

        String targetDirectoryPath = TargetDirectoryService.getTargetDirectoryPath();
        File file = new File( targetDirectoryPath, "svn.externals" );
        FileUtils.ensureParentExists( file );

        OutputStream outputStream = null;

        try {

            outputStream = new FileOutputStream( file );

            for (Object key : properties.keySet()) {
                String path = (String) key;
                String url = (String) properties.getProperty( path );
                outputStream.write( ( path + " " + url + "\n" ).getBytes() );
            }
        }
        finally {
            IOUtil.close( outputStream );
        }

        List<String> cmd = new LinkedList<String>();
        cmd.add( "svn" );
        cmd.add( "propset" );
        cmd.add( "svn:externals" );
        cmd.add( "-F" );
        cmd.add( file.getPath() );
        cmd.add( "." );
        execute( cmd );
    }

    private Properties loadExternals() {

        List<String> cmd = new LinkedList<String>();
        cmd.add( "svn" );
        cmd.add( "propget" );
        cmd.add( "svn:externals" );

        Properties properties = new Properties();
        PropertyStreamConsumer streamConsumer = new PropertyStreamConsumer( properties );
        execute( cmd, streamConsumer, null );

        return properties;
    }

    private static class PropertyStreamConsumer implements StreamConsumer {

        private final Properties properties;

        public PropertyStreamConsumer(Properties properties) {

            this.properties = properties;
        }

        @Override
        public void consumeLine( String line ) {

            line = line.trim();

            int indexOf = line.indexOf( ' ' );

            if ( indexOf == -1 ) {
                return;
            }

            String key = line.substring( 0, indexOf ).trim();
            String value = line.substring( indexOf + 1 ).trim();
            properties.put( key, value );
        }
    }
}
