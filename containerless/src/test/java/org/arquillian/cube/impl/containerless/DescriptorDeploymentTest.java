package org.arquillian.cube.impl.containerless;

import java.net.Socket;

import org.arquillian.cube.impl.util.IOUtil;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.descriptor.api.Descriptor;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.docker.DockerDescriptor;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class DescriptorDeploymentTest {

    private static int PORT = 8080;
    
    @Deployment
    public static Descriptor createDockerfile() {
        return Descriptors.create(DockerDescriptor.class, "test-box")
                .from("busybox")
                .expose(PORT)
                .cmd("nc", "-ll", "-p", String.valueOf(PORT), "-e", "echo", "A");
    }
    
    @Test
    public void shouldStartContainer() throws Exception {
        Socket s = new Socket("localhost", PORT);
        
        try {
            String response = IOUtil.asString(s.getInputStream());
            Assert.assertEquals("A", response);
        }
        finally {
            s.close();
        }
    }
}
