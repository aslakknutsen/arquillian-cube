package org.arquillian.cube.impl.descriptor;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.arquillian.cube.descriptor.CubeDescriptor;
import org.arquillian.cube.descriptor.CubeDescriptors;
import org.jboss.shrinkwrap.descriptor.api.DescriptorExportException;
import org.jboss.shrinkwrap.descriptor.api.DescriptorExporter;
import org.jboss.shrinkwrap.descriptor.spi.DescriptorImplBase;
import org.jboss.shrinkwrap.descriptor.spi.node.Node;

public class CubeDescriptorsImpl extends DescriptorImplBase<CubeDescriptor> implements CubeDescriptors {

    private Node root;
    
    public CubeDescriptorsImpl(String name) {
        this(name, new Node(name));
    }

    public CubeDescriptorsImpl(String name, Node root) {
        super(name);
        this.root = root;
    }

    @Override
    public CubeDescriptor getCubeDescriptor(String name) {
        Node node = root.getSingle(name);
        if(node == null) {
            return null;
        }
        return new CubeDescriptorImpl(name, node);
    }
    
    @Override
    public Collection<CubeDescriptor> getCubeDescriptors() {
        List<CubeDescriptor> cubes = new ArrayList<>();
        for(Node child : root.getChildren()) {
            cubes.add(new CubeDescriptorImpl(child.getName(), child));
        }
        return cubes;
    }
    
    public Node getRoot() {
        return root;
    }
    
    @Override
    public void exportTo(OutputStream output) throws DescriptorExportException, IllegalArgumentException {
        throw new UnsupportedOperationException("CubeDescriptor is not exportable");

//        if (output == null) {
//            throw new IllegalArgumentException("Can not export to null stream");
//        }
//        
    }

    @Override
    protected DescriptorExporter<CubeDescriptor> getExporter() {
        return null;
    }
}
