package org.hardisonbrewing.maven.svn;

import org.apache.maven.model.Dependency;

public class External {

    /**
     * @parameter
     */
    public String path;

    /**
     * @parameter
     */
    public Dependency dependency;
}
