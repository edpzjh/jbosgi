/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.test.osgi.example.webapp;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.osgi.deployment.interceptor.LifecycleInterceptorException;
import org.jboss.osgi.repository.XRepository;
import org.jboss.osgi.testing.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.test.osgi.example.AbstractExampleTestCase;
import org.jboss.test.osgi.example.webapp.bundle.EndpointServlet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.resource.Resource;
import org.osgi.service.repository.Repository;

import javax.inject.Inject;
import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import java.io.InputStream;
import java.util.jar.JarFile;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * A test that deployes a WAR bundle that contains no WEB-INF/web.xml
 *
 * @author thomas.diesler@jboss.com
 * @since 26-Oct-2009
 */
@RunWith(Arquillian.class)
public class WebAppNegativeTestCase extends AbstractExampleTestCase {

    @Inject
    public BundleContext context;

    @Inject
    public Bundle bundle;

    @Deployment
    public static WebArchive createdeployment() {
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, "example-webapp-negative");
        archive.addClasses(EndpointServlet.class, AbstractExampleTestCase.class);
        archive.addAsResource("webapp/message.txt", "message.txt");
        // [SHRINKWRAP-278] WebArchive.setManifest() results in WEB-INF/classes/META-INF/MANIFEST.MF
        archive.add(new Asset() {
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addManifestHeader(Constants.BUNDLE_CLASSPATH, ".,WEB-INF/classes");
                builder.addManifestHeader("Web-ContextPath", "example-webapp");
                builder.addImportPackages(LifecycleInterceptorException.class);
                builder.addImportPackages(HttpServlet.class, Servlet.class);
                builder.addImportPackages(XRepository.class, Repository.class, Resource.class);
                return builder.openStream();
            }
        }, JarFile.MANIFEST_NAME);
        return archive;
    }

    @Test
    public void testServletAccess() throws Exception {
        try {
            provideWebappSupport(context);
            bundle.start();
            fail("BundleException expected");
        } catch (BundleException ex) {
            Throwable cause = ex.getCause();
            assertTrue(cause instanceof LifecycleInterceptorException);
        }
    }

    private void provideHttpService(BundleContext context) throws BundleException {
        if (context.getServiceReference("org.osgi.service.http.HttpService") == null)
            installSupportBundle(context, getCoordinates(JBOSS_OSGI_HTTP)).start();
    }

    private void provideWebappSupport(BundleContext context) throws BundleException {
        provideHttpService(context);
        if (context.getServiceReference("org.jboss.osgi.webapp.WebAppService") == null) {
            installSupportBundle(context, getCoordinates(JBOSS_OSGI_WEBAPP)).start();
        }
    }
}
