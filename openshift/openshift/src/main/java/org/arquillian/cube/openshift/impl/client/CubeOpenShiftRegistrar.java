package org.arquillian.cube.openshift.impl.client;

import java.util.List;

import org.arquillian.cube.openshift.impl.model.OpenShiftCube;
import org.arquillian.cube.spi.CubeRegistry;
import org.jboss.arquillian.core.api.annotation.Observes;

import io.fabric8.kubernetes.api.model.Pod;

public class CubeOpenShiftRegistrar {

    public void register(@Observes OpenShiftClient client, CubeRegistry registry, CubeOpenShiftConfiguration configuration) {
        Object model = configuration.getDefinitions();

        if (model instanceof List) {

        } else if (model instanceof Pod) {
            registry.addCube(new OpenShiftCube((Pod) model, client, configuration));
        }
    }
}
