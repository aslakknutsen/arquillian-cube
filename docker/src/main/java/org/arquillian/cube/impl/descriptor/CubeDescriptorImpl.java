package org.arquillian.cube.impl.descriptor;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.arquillian.cube.descriptor.CubeDescriptor;
import org.jboss.shrinkwrap.descriptor.api.DescriptorExportException;
import org.jboss.shrinkwrap.descriptor.api.DescriptorExporter;
import org.jboss.shrinkwrap.descriptor.spi.DescriptorImplBase;
import org.jboss.shrinkwrap.descriptor.spi.node.Node;

public class CubeDescriptorImpl extends DescriptorImplBase<CubeDescriptor> implements CubeDescriptor {

    private Node root;
    
    public CubeDescriptorImpl(String name) {
        this(name, new Node(name));
    }

    public CubeDescriptorImpl(String name, Node root) {
        super(name);
        this.root = root;
    }

    public Node getRoot() {
        return root;
    }
    
    @Override
    public String getName() {
        return root.getName();
    }

    @Override
    public String getImage() {
        Node image = root.getSingle("image");
        if(image == null) {
            return null;
        }
        return image.getText();
    }

    @Override
    public String[] getExposedPorts() {
        List<String> result = new ArrayList<>();
        List<Node> children = root.get("exposedPorts/array");
        for(Node child : children) {
            result.add(child.getText());
        }
        return result.toArray(new String[] {});
    }
    
    @Override
    public String[] getPortBindings() {
        List<String> result = new ArrayList<>();
        List<Node> children = root.get("portBindings/array");
        for(Node child : children) {
            result.add(child.getText());
        }
        return result.toArray(new String[] {});
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
