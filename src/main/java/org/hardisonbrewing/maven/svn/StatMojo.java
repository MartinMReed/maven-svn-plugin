/**
 * Copyright (c) 2013 Martin M Reed
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.hardisonbrewing.maven.svn;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;
import org.hardisonbrewing.maven.core.FileUtils;
import org.hardisonbrewing.maven.core.JoJoMojoImpl;
import org.hardisonbrewing.maven.core.cli.CommandLineService;

/**
 * @goal stat
 * @phase stat
 */
public final class StatMojo extends JoJoMojoImpl {

    /**
     * @parameter
     */
    private String[] includes;

    /**
     * @parameter
     */
    private String[] excludes;

    /**
     * @parameter expression="${maven.svn.revisionStart}"
     */
    private long revisionStart;

    /**
     * @parameter expression="${maven.svn.revisionEnd}"
     */
    private long revisionEnd;

    /**
     * @parameter property="threads" default-value="15" expression="${maven.svn.threads}"
     */
    private int threadCount;

    @Override
    public final void execute() throws MojoExecutionException, MojoFailureException {

        File baseDir = getProject().getBasedir();
        File[] files = FileUtils.listFilesRecursive( baseDir, includes, excludes );

        if ( revisionStart > 0 || revisionEnd > 0 ) {

            List<File> _files = new LinkedList<File>();
            for (File file : files) {
                _files.add( file );
            }

            FileDiff fileDiff = new FileDiff();

            CountThread[] threads = new CountThread[threadCount];

            for (int i = 0; i < threads.length; i++) {
                threads[i] = new CountThread( _files, revisionStart, revisionEnd, fileDiff );
                new Thread( threads[i] ).start();
            }

            for (CountThread thread : threads) {
                thread.waitUntilFinished();
            }

            getLog().info( "LOC added " + revisionStart + ":" + revisionEnd + " = " + fileDiff.linesAdded );
            getLog().info( "LOC removed since " + revisionStart + ":" + revisionEnd + " = " + fileDiff.linesRemoved );
            getLog().info( "LOC total since " + revisionStart + ":" + revisionEnd + " = " + ( fileDiff.linesAdded - fileDiff.linesRemoved ) );
        }
        else {

            long count = 0;

            for (File file : files) {
                try {
                    count += countLines( file );
                }
                catch (IOException e) {
                    throw new IllegalStateException( e );
                }
            }

            getLog().info( "LOC: " + count );
        }
    }

    private long countLines( File file ) throws IOException {

        BufferedReader reader = null;
        try {
            reader = new BufferedReader( new FileReader( file ) );
            long count = 0;
            String line = null;
            while (( line = reader.readLine() ) != null) {
                line = line.trim();
                if ( line.length() > 0 ) {
                    count++;
                }
            }
            return count;
        }
        finally {
            IOUtil.close( reader );
        }
    }

    private long firstRevision( File file ) {

        List<String> cmd = new LinkedList<String>();
        cmd.add( "svn" );
        cmd.add( "log" );
        cmd.add( "-r" );
        cmd.add( "1:HEAD" );
        cmd.add( "--limit" );
        cmd.add( "1" );
        cmd.add( file.getPath() );

        FirstRevisionStreamConsumer streamConsumer;

        try {
            streamConsumer = new FirstRevisionStreamConsumer();
            execute( cmd, streamConsumer, null );
        }
        catch (Exception e) {
            throw new IllegalStateException( "Failed on file: " + file.getPath(), e );
        }

        return streamConsumer.revision();
    }

    @Override
    protected Commandline buildCommandline( List<String> cmd ) {

        Commandline commandLine;

        try {
            commandLine = CommandLineService.build( cmd );
        }
        catch (CommandLineException e) {
            throw new IllegalStateException( e.getMessage() );
        }

        return commandLine;
    }

    private void countLinesDiff( File file, long revisionStart, long revisionEnd, FileDiff fileDiff ) {

        long firstRevision = firstRevision( file );
        revisionStart = Math.max( revisionStart, firstRevision );

        List<String> cmd = new LinkedList<String>();
        cmd.add( "svn" );
        cmd.add( "diff" );
        cmd.add( "-r" );
        if ( revisionEnd > 0 ) {
            cmd.add( revisionStart + ":" + revisionEnd );
        }
        else {
            cmd.add( Long.toString( revisionStart ) );
        }
        cmd.add( file.getPath() );

        DiffStreamConsumer streamConsumer = new DiffStreamConsumer();
        Commandline commandLine = buildCommandline( cmd );

        int exitValue;

        try {
            getLog().info( commandLine.toString() );
            exitValue = CommandLineService.execute( commandLine, streamConsumer, streamConsumer );
        }
        catch (CommandLineException e) {
            throw new IllegalStateException( e );
        }

        if ( exitValue != 0 && exitValue != 1 ) {
            throw new IllegalStateException( "Command exited with value[" + exitValue + "]" );
        }

        synchronized (fileDiff) {
            fileDiff.linesAdded += streamConsumer.plus;
            fileDiff.linesRemoved += streamConsumer.minus;
        }
    }

    public void setThreads( int threadCount ) {

        this.threadCount = threadCount;
    }

    private class DiffStreamConsumer implements StreamConsumer {

        private long plus;
        private long minus;

        @Override
        public void consumeLine( String line ) {

            line = line.trim();

            boolean plus = line.startsWith( "+" );
            boolean minus = line.startsWith( "-" );

            if ( plus || minus ) {
                line = line.substring( 1 );
                line = line.trim();
                if ( line.length() > 0 ) {
                    if ( plus ) {
                        this.plus++;
                    }
                    else if ( minus ) {
                        this.minus++;
                    }
                }
            }
        }
    }

    private class FirstRevisionStreamConsumer implements StreamConsumer {

        private long revision;

        @Override
        public void consumeLine( String line ) {

            if ( revision != 0 ) {
                return;
            }

            line = line.trim();
            String original = line;

            if ( line.startsWith( "r" ) ) {

                int indexOf = line.indexOf( '|' );
                if ( indexOf != -1 ) {

                    line = line.substring( 1, indexOf );
                    line = line.trim();

                    try {
                        revision = Long.parseLong( line );
                    }
                    catch (NumberFormatException e) {
                        System.err.println( "Unable to parse line: [" + original + "]" );
                        throw e;
                    }
                }
            }
        }

        public long revision() {

            return revision;
        }
    }

    private class FileDiff {

        public long linesAdded;
        public long linesRemoved;
    }

    private class CountThread implements Runnable {

        private final Object lock = new Object();

        private final List<File> files;
        private final long revisionStart;
        private final long revisionEnd;
        private final FileDiff fileDiff;

        private boolean finished;

        public CountThread(List<File> files, long revisionStart, long revisionEnd, FileDiff fileDiff) {

            this.files = files;
            this.revisionStart = revisionStart;
            this.revisionEnd = revisionEnd;
            this.fileDiff = fileDiff;
        }

        @Override
        public void run() {

            try {
                while (true) {
                    File file = null;
                    synchronized (files) {
                        if ( files.isEmpty() ) {
                            break;
                        }
                        file = files.remove( 0 );
                    }
                    countLinesDiff( file, revisionStart, revisionEnd, fileDiff );
                }
            }
            finally {
                finished = true;
                synchronized (lock) {
                    lock.notify();
                }
            }
        }

        private void waitUntilFinished() {

            if ( finished ) {
                return;
            }

            synchronized (lock) {
                while (!finished) {
                    try {
                        lock.wait();
                    }
                    catch (InterruptedException e) {
                        // do nothing
                    }
                }
            }
        }
    }
}
