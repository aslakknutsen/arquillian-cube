package org.arquillian.cube.impl.descriptor;

import java.io.InputStream;
import java.util.Collection;
import java.util.Map;

import org.jboss.shrinkwrap.descriptor.api.Descriptor;
import org.jboss.shrinkwrap.descriptor.api.DescriptorImporter;
import org.jboss.shrinkwrap.descriptor.spi.node.Node;
import org.jboss.shrinkwrap.descriptor.spi.node.NodeDescriptorImporterBase;
import org.jboss.shrinkwrap.descriptor.spi.node.NodeImporter;
import org.yaml.snakeyaml.Yaml;

public class YAMLImporter<T extends Descriptor> extends NodeDescriptorImporterBase<T>
    implements DescriptorImporter<T> { 

    public YAMLImporter(Class<T> endUserViewImplType, String descriptorName) throws IllegalArgumentException {
        super(endUserViewImplType, descriptorName);
    }

    private static class YAMLNodeImporter implements NodeImporter {
    
        @Override
        public Node importAsNode(InputStream stream, boolean close) throws IllegalArgumentException {
            try {
                Map<String, Object> data = (Map<String, Object>)new Yaml().load(stream);
                
                Node root = null;
                if(data.size() == 1) {
                    root = new Node(data.keySet().iterator().next());
                    data = (Map<String, Object>)data.values().iterator().next();
                } else {
                    root = new Node("root");
                }
                readRecursive(root, data);
                
                if(close) {
                    stream.close();
                }
                
                return root;
            } catch(Exception e) {
                throw new RuntimeException(e); 
            }
        }
    
        private void readRecursive(Node target, Map<String, Object> source) {
            for(Map.Entry<String, Object> sourceEntry : source.entrySet()) {
                String key = sourceEntry.getKey();
                Object value = sourceEntry.getValue();
                
                Node subTarget = target.createChild(key);
                if(value instanceof String || value instanceof Boolean || value instanceof Number) {
                    subTarget.text(value);
                }
                else if(value instanceof Collection) {
                    Collection<Object> children = (Collection<Object>)value;
                    for(Object child : children) {
                        Node subSubTarget = subTarget.createChild("array");
                        subSubTarget.text(child.toString());
                    }
                }
                else if(value instanceof Map) {
                    readRecursive(subTarget, (Map<String, Object>)value);
                }
            }
        }
    }

    @Override
    public NodeImporter getNodeImporter() {
        return new YAMLNodeImporter();
    }
}
