package org.arquillian.cube.impl.descriptor;

import java.util.Collection;

import org.arquillian.cube.descriptor.CubeDescriptor;
import org.arquillian.cube.descriptor.CubeDescriptor.Awaits;
import org.arquillian.cube.descriptor.CubeDescriptors;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.junit.Assert;
import org.junit.Test;

public class CubeDescriptorImplTest {

    public static final String DESCRIPTORS = 
            "            tomcat_default:\n" + 
            "              image: tutum/tomcat:7.0\n" + 
            "              exposedPorts: [8089/tcp]\n" + 
            "              await:\n" + 
            "                strategy: polling\n" + 
            "              portBindings: [8089/tcp, 8088/tcp, 8081->8080/tcp]\n" + 
            "            tomcat_dockerfile:\n" + 
            "              buildImage:\n" + 
            "                dockerfileLocation: src/test/resources/tomcat\n" + 
            "                noCache: true\n" + 
            "                remove: true\n" + 
            "              await:\n" + 
            "                strategy: polling\n" +
            "                iterations: 100\n" +
            "              env: [JAVA_OPTS=-Djava.rmi.server.hostname=  -Dcom.sun.management.jmxremote.rmi.port=8088 -Dcom.sun.management.jmxremote.port=8089 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false]\n" + 
            "              portBindings: [8089/tcp, 8088/tcp, 8081->8080/tcp]\n" + 
            "            wildfly:\n" + 
            "              buildImage:\n" + 
            "                dockerfileLocation: src/test/resources/wildfly\n" + 
            "                noCache: true\n" + 
            "                remove: true\n" + 
            "              exposedPorts: [8080/tcp, 9990/tcp]\n" + 
            "              await:\n" + 
            "                strategy: polling\n" + 
            "              portBindings: [8081->8080/tcp, 9991->9990/tcp]\n" + 
            "            wildfly_database:\n" + 
            "              extends: wildfly\n" + 
            "              links:\n" + 
            "                - database:database\n" + 
            "            wildfly2:\n" + 
            "              extends: wildfly\n" + 
            "              portBindings: [8082->8080/tcp, 9992->9990/tcp]\n" + 
            "            database:\n" + 
            "              image: zhilvis/h2-db\n" + 
            "              exposedPorts: [81/tcp, 1521/tcp]\n" + 
            "              await:\n" + 
            "                strategy: polling\n" + 
            "              portBindings: [1521/tcp, 8181->81/tcp]\n" + 
            "            database_manual:\n" + 
            "              extends: database\n" + 
            "              portBindings: [1522->1521/tcp, 8182->81/tcp]\n";

    public static final String DESCRIPTOR = 
            "            tomcat_default:\n" + 
            "              image: tutum/tomcat:7.0\n" + 
            "              exposedPorts: [81/tcp, 1521/tcp]\n" + 
            "              await:\n" + 
            "                strategy: polling\n" + 
            "              portBindings: [8089/tcp, 8088/tcp, 8081->8080/tcp]\n";

    @Test
    public void shouldBeAbleToGetDescriptorByName() {
        CubeDescriptors cubes = descriptors();
        CubeDescriptor cube = cubes.getCubeDescriptor("database");
        
        Assert.assertNotNull(cube);
        Assert.assertEquals("database", cube.getName());
    }

    @Test
    public void shouldBeAbleToGetAllDescriptors() {
        CubeDescriptors cubes = descriptors();
        Collection<CubeDescriptor> cube = cubes.getCubeDescriptors();
        
        Assert.assertEquals(7, cube.size());
    }
    
    @Test
    public void shouldBeAbleToGetCubeName() {
        CubeDescriptor cube = descriptor();
        
        Assert.assertEquals("tomcat_default", cube.getName());
    }

    @Test
    public void shouldBeAbleToGetImage() {
        CubeDescriptor cube = descriptor();
        
        Assert.assertEquals("tutum/tomcat:7.0", cube.getImage());
    }

    @Test
    public void shouldBeAbleToGetCubeExposedPorts() {
        CubeDescriptor cube = descriptor();
        
        Assert.assertEquals(2, cube.getExposedPorts().length);
        Assert.assertEquals("81/tcp", cube.getExposedPorts()[0]);
        Assert.assertEquals("1521/tcp", cube.getExposedPorts()[1]);
    }

    @Test
    public void shouldBeAbleToGetCubePortBindings() {
        CubeDescriptor cube = descriptor();
        
        Assert.assertEquals(3, cube.getPortBindings().length);
        Assert.assertEquals("8089/tcp", cube.getPortBindings()[0]);
        Assert.assertEquals("8088/tcp", cube.getPortBindings()[1]);
        Assert.assertEquals("8081->8080/tcp", cube.getPortBindings()[2]);
    }

    @Test
    public void shouldBeAbleToGetPollingStrategy() {
        CubeDescriptor cube = descriptor();
        
        cube.await(Awaits.polling()).getIterations()
    }

    private CubeDescriptors descriptors() {
        return Descriptors.importAs(CubeDescriptors.class).fromString(DESCRIPTORS);
    }

    private CubeDescriptor descriptor() {
        return Descriptors.importAs(CubeDescriptor.class).fromString(DESCRIPTOR);
    }
}
