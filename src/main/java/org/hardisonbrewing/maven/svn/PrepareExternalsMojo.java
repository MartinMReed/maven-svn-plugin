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

import java.util.Properties;

import org.apache.maven.model.Scm;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.hardisonbrewing.maven.core.FileUtils;
import org.hardisonbrewing.maven.core.JoJoMojoImpl;

/**
 * @goal prepare-externals
 * @phase prepare-externals
 */
public final class PrepareExternalsMojo extends JoJoMojoImpl {

    @Override
    public final void execute() throws MojoExecutionException, MojoFailureException {

        String propertiesPath = PropertiesService.getExternalsPropertiesPath();
        FileUtils.ensureParentExists( propertiesPath );

        MavenProject project = getProject();
        Scm scm = project.getScm();

        Properties properties = new Properties();
        properties.put( PropertiesService.SCM_URL, scm.getUrl() );
        PropertiesService.storeProperties( properties, propertiesPath );
    }
}
