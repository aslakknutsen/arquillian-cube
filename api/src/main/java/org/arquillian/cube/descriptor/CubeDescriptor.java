package org.arquillian.cube.descriptor;

import org.jboss.shrinkwrap.descriptor.api.Descriptor;

public interface CubeDescriptor extends Descriptor {

    String getName();
    
    String getImage();

    String[] getExposedPorts();
    
    String[] getPortBindings();
    
    <T> T await(Await<T> strategy);
    
    public interface Await<T> {
        
    }
    
    public static class Awaits {
        public static Polling polling() {
            return null;
        }
    }
    
    public static interface Polling extends Await<PollingDescriptor> {
        
    }

    public interface PollingDescriptor extends Descriptor {
        
        int getIterations();
        
    }
}
