package org.arquillian.cube.descriptor;

import java.util.Collection;

import org.jboss.shrinkwrap.descriptor.api.Descriptor;

public interface CubeDescriptors extends Descriptor {

    Collection<CubeDescriptor> getCubeDescriptors();
    
    CubeDescriptor getCubeDescriptor(String name);
}
