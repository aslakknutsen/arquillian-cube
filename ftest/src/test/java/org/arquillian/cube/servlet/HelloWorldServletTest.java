package org.arquillian.cube.servlet;

import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.arquillian.cube.Container;
import org.arquillian.cube.Cube;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.github.dockerjava.api.DockerClient;

@RunWith(Arquillian.class)
public class HelloWorldServletTest {

    @Deployment(testable=false)
    public static WebArchive create() {
        return ShrinkWrap.create(WebArchive.class, "hello.war").addClass(HelloWorldServlet.class);
    }
    
    @Container
    String containerId;
    
    @Cube
    DockerClient dockerClient;
    
    @Test
    public void should_parse_and_load_configuration_file() throws IOException {
        
        URL obj = new URL("http://localhost:8080/hello/HelloWorld");
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("GET");
        
        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();
 
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
 
        assertThat(response.toString(), is("Hello World"));
        
    }
    
    @Test
    public void should_enrich_test_with_container_id() {
        assertThat(containerId, notNullValue());
    }
    
    @Test
    public void should_enrich_test_with_docker_client() {
        assertThat(dockerClient, notNullValue());
    }
}
