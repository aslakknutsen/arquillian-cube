package org.arquillian.cube.impl.containerless;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.arquillian.cube.impl.docker.DockerClientExecutor;
import org.arquillian.cube.impl.model.DockerCube;
import org.arquillian.cube.impl.util.IOUtil;
import org.arquillian.cube.spi.Binding;
import org.arquillian.cube.spi.Cube;
import org.arquillian.cube.spi.CubeRegistry;
import org.arquillian.cube.spi.event.CreateCube;
import org.arquillian.cube.spi.event.CubeControlEvent;
import org.arquillian.cube.spi.event.DestroyCube;
import org.arquillian.cube.spi.event.StartCube;
import org.arquillian.cube.spi.event.StopCube;
import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.arquillian.container.spi.client.protocol.ProtocolDescription;
import org.jboss.arquillian.container.spi.client.protocol.metadata.HTTPContext;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.core.api.Event;
import org.jboss.arquillian.core.api.Injector;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.GenericArchive;
import org.jboss.shrinkwrap.api.exporter.TarExporter;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.descriptor.api.Descriptor;
import org.jboss.shrinkwrap.descriptor.api.docker.DockerDescriptor;
import org.jboss.shrinkwrap.descriptor.api.docker.instruction.ExposeInstruction;

public class ContainerlessDockerDeployableContainer implements DeployableContainer<ContainerlessConfiguration> {

    private static final String DOCKERFILE_TEMPLATE = "DockerfileTemplate";
    private static final Logger log = Logger.getLogger(ContainerlessDockerDeployableContainer.class.getName());
    
    private ContainerlessConfiguration configuration;

    @Inject
    private Instance<CubeRegistry> cubeRegistryInstance;

    @Inject
    private Event<CubeControlEvent> controlEvent;

    // HACK, should not expose this
    @Inject
    private Instance<DockerClientExecutor> executor;

    @Inject
    private Instance<Injector> injector;
    
    @Override
    public Class<ContainerlessConfiguration> getConfigurationClass() {
        return ContainerlessConfiguration.class;
    }

    @Override
    public void setup(ContainerlessConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void start() throws LifecycleException {
        // should be done at deployment time.
    }

    @Override
    public void stop() throws LifecycleException {
        // should be done at undeployment time.
    }

    @Override
    public ProtocolDescription getDefaultProtocol() {
        return new ProtocolDescription("Servlet 3.0");
    }

    @Override
    public ProtocolMetaData deploy(Archive<?> archive) throws DeploymentException {
        String containerlessDocker = this.configuration.getContainerlessDocker();
        final CubeRegistry cubeRegistry = cubeRegistryInstance.get();

        Cube cube = cubeRegistry.getCube(containerlessDocker);
        if (cube == null) {
            // Is there a way to ignore it? Or we should throw an exception?
            throw new IllegalArgumentException("No Containerless Docker container configured in extension with id "
                    + containerlessDocker);
        }
        Map<String, Object> cubeConfiguration = cube.configuration();
        //Only works with images using a Dockerfile.
        if (cubeConfiguration.containsKey("buildImage")) {
            Map<String, Object> params = asMap(cubeConfiguration, "buildImage");
            if (params.containsKey("dockerfileLocation")) {
                File location = new File((String) params.get("dockerfileLocation"));
                if (location.isDirectory()) {
                    //Because ShrinkWrap may create different jar files depending on what we are testing in this case
                    //we need a template which is the responsible to copy the jar to desired location
                    try {
                        createDockerfileFromTemplate(archive, location);
                        // fire events as usually.
                        controlEvent.fire(new CreateCube(cube));
                        controlEvent.fire(new StartCube(cube));
                        return createProtocolMetadata(cube);
                    } catch (FileNotFoundException e) {
                        throw new IllegalArgumentException("Containerless Docker container requires a file named "
                                + DOCKERFILE_TEMPLATE);
                    }
                } else {
                    throw new IllegalArgumentException(
                            "Dockerfile Template of containerless Docker container must be in a directory.");
                }
            } else {
                throw new IllegalArgumentException(
                        "Containerless Docker container should be built in Dockerfile, and dockerfileLocation property not found.");
            }
        } else {
            throw new IllegalArgumentException(
                    "Containerless Docker container should be built in Dockerfile, and buildImage property not found.");
        }
    }

    private void createDockerfileFromTemplate(Archive<?> archive, File location)
            throws FileNotFoundException {
        File templateDockerfile = new File(location, DOCKERFILE_TEMPLATE);
        String deployableFilename = archive.getName();
        Map<String, String> values = new HashMap<String, String>();
        values.put("deployableFilename", deployableFilename);
        String templateContent = IOUtil.asStringPreservingNewLines(new FileInputStream(templateDockerfile));
        //But because deployable file is created by shrinkwrap we need to replace the deploy file name to the one created.
        String dockerfileContent = IOUtil.replacePlaceholders(templateContent, values);
        File dockerfile = new File(location, "Dockerfile");
        if (dockerfile.exists()) {
            log.fine("Dockerfile file is already found in current build directory and is going to be renamed to Dockerfile.old.");
            dockerfile.renameTo(new File(location, "Dockerfile.old"));
            dockerfile = new File(location, "Dockerfile");
        }
        dockerfile.deleteOnExit();
        //The content is written to real Dockerfile which will be used during built time.
        IOUtil.toFile(dockerfileContent, dockerfile);
        File deployableOutputFile = new File(location, deployableFilename);
        deployableOutputFile.deleteOnExit();
        //file is saved to Dockerfile directory so can be copied inside image.
        if(archive instanceof GenericArchive) {
            //In case of generic archives a tgz exporter should be used so Docker can uncompress it automatically.
            archive.as(TarExporter.class).exportTo(deployableOutputFile, true);
        } else {
            archive.as(ZipExporter.class).exportTo(deployableOutputFile, true);
        }
    }

    private ProtocolMetaData createProtocolMetadata(Cube cube) {
        Binding bindings = cube.bindings();
        //ProtocolMetadataUpdater will reajust the port to the exposed ones.
        HTTPContext httpContext = new HTTPContext(bindings.getIP(), configuration.getEmbeddedPort());
        return new ProtocolMetaData().addContext(httpContext);
    }

    @Override
    public void undeploy(Archive<?> archive) throws DeploymentException {
        String containerlessDocker = this.configuration.getContainerlessDocker();
        final CubeRegistry cubeRegistry = cubeRegistryInstance.get();

        Cube cube = cubeRegistry.getCube(containerlessDocker);
        if (cube != null) {
            controlEvent.fire(new StopCube(cube));
            controlEvent.fire(new DestroyCube(cube));
        }
    }

    @Override
    public void deploy(Descriptor descriptor) throws DeploymentException {
        String name = descriptor.getDescriptorName();
        
        DockerDescriptor dockerDescriptor = null;
        if(!(descriptor instanceof DockerDescriptor)) {
            throw new DeploymentException(
                    "Only Descriptors of type DockerDescriptor are supported by this container. Tried to deploy " + descriptor.getClass());
        }
        
        dockerDescriptor = DockerDescriptor.class.cast(descriptor);
        
        Map<String, Object> configuration = new HashMap<>();
        Map<String, Object> buildImage = new HashMap<>();
        Map<String, Object> await = new HashMap<>();
        
        configuration.put("buildImage", buildImage);
        configuration.put("await", await);
        
        await.put("strategy", "polling");
        
        List<String> portBindings = new ArrayList<>();
        List<ExposeInstruction> allExposed = dockerDescriptor.getAllExpose();
        for(int i = 0; i < allExposed.size(); i++) {
            ExposeInstruction expose = allExposed.get(i);
            for(Integer p : expose.getPorts()) {
                portBindings.add(p + "->" + p + "/tcp");
            }
        }
        configuration.put("portBindings", portBindings);

        try {
            File dockerFile = exportDescriptorToTemp(descriptor);
            buildImage.put("dockerfileLocation", dockerFile.getParentFile().getAbsolutePath());
            buildImage.put("noCache", false);
            buildImage.put("remove", false);
            
            DockerCube cube = new DockerCube(name, configuration, executor.get());
            this.cubeRegistryInstance.get().addCube(injector.get().inject(cube));
            
            controlEvent.fire(new CreateCube(cube));
            controlEvent.fire(new StartCube(cube));
        }
        catch(Exception e) {
            throw new DeploymentException("Could not deploy descriptor", e);
        }
    }
    
    @Override
    public void undeploy(Descriptor descriptor) throws DeploymentException {
        Cube cube = this.cubeRegistryInstance.get().getCube(descriptor.getDescriptorName());
        if (cube != null) {
            controlEvent.fire(new StopCube(cube));
            controlEvent.fire(new DestroyCube(cube));
        }
    }

    private File exportDescriptorToTemp(Descriptor descriptor) throws Exception {
        File output = File.createTempFile("arq", "cube");
        output.delete();
        output.mkdirs();
        
        File dockerFile = new File(output, "Dockerfile");
        descriptor.exportTo(new FileOutputStream(dockerFile));
        return dockerFile;
    }

    @SuppressWarnings("unchecked")
    private static final Map<String, Object> asMap(Map<String, Object> map, String property) {
        return (Map<String, Object>) map.get(property);
    }
}
