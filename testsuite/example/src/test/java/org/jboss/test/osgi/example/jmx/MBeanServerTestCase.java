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
package org.jboss.test.osgi.example.jmx;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.osgi.repository.XRepository;
import org.jboss.osgi.testing.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.test.osgi.example.AbstractExampleTestCase;
import org.jboss.test.osgi.example.jmx.bundle.Foo;
import org.jboss.test.osgi.example.jmx.bundle.FooMBean;
import org.jboss.test.osgi.example.jmx.bundle.MBeanActivator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.resource.Resource;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.repository.Repository;
import org.osgi.util.tracker.ServiceTracker;

import javax.inject.Inject;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;

/**
 * A test that deployes a bundle that registeres an MBean
 *
 * @author thomas.diesler@jboss.com
 * @since 12-Feb-2009
 */
@RunWith(Arquillian.class)
public class MBeanServerTestCase extends AbstractExampleTestCase {

    @Inject
    public BundleContext context;

    @Inject
    public Bundle bundle;

    @Deployment
    public static JavaArchive createdeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "example-mbean");
        archive.addClasses(AbstractExampleTestCase.class, Foo.class, FooMBean.class, MBeanActivator.class);
        archive.setManifest(new Asset() {
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addBundleActivator(MBeanActivator.class);
                builder.addImportPackages(PackageAdmin.class, BundleActivator.class, ServiceTracker.class);
                builder.addImportPackages(MBeanServer.class);
                builder.addImportPackages(XRepository.class, Repository.class, Resource.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    public void testMBeanAccess() throws Exception {
        bundle.start();
        MBeanServer server = provideMBeanServer(context);
        ObjectName oname = ObjectName.getInstance(FooMBean.MBEAN_NAME);
        FooMBean foo = getMBeanProxy(server, oname, FooMBean.class);
        assertEquals("hello", foo.echo("hello"));
    }

    private MBeanServer provideMBeanServer(BundleContext context) throws BundleException {
        ServiceReference sref = context.getServiceReference(PackageAdmin.class.getName());
        PackageAdmin padmin = (PackageAdmin) context.getService(sref);
        if (padmin.getBundles("jboss-osgi-jmx", null) == null) {
            installSupportBundle(context, getCoordinates(APACHE_ARIES_UTIL)).start();
            installSupportBundle(context, getCoordinates(APACHE_ARIES_JMX)).start();
            installSupportBundle(context, getCoordinates(JBOSS_OSGI_JMX)).start();
        }
        sref = context.getServiceReference(MBeanServer.class.getName());
        return (MBeanServer) context.getService(sref);
    }

    private <T> T getMBeanProxy(MBeanServerConnection server, ObjectName name, Class<T> interf)
    {
        return (T) MBeanServerInvocationHandler.newProxyInstance(server, name, interf, false);
    }
}