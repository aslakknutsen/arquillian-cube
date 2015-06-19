package org.arquillian.cube.openshift.impl.client;

import static io.fabric8.kubernetes.api.KubernetesHelper.loadJson;
import static org.arquillian.cube.openshift.impl.client.ResourceUtil.waitForStart;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import io.fabric8.kubernetes.api.Kubernetes;
import io.fabric8.kubernetes.api.KubernetesExtensions;
import io.fabric8.kubernetes.api.KubernetesFactory;
import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.openshift.api.model.Build;
import io.fabric8.openshift.api.model.BuildConfig;
import io.fabric8.openshift.api.model.BuildConfigBuilder;
import io.fabric8.openshift.api.model.BuildRequest;
import io.fabric8.openshift.api.model.BuildRequestBuilder;
import io.fabric8.openshift.api.model.ImageStream;
import io.fabric8.openshift.api.model.ImageStreamBuilder;

public class OpenShiftClient {

    private KubernetesFactory factory;
    private String namespace;

    private Kubernetes kubernetes;
    private KubernetesExtensions kubernetesExtensions;
    private GitServer gitserver;
    private boolean keepAliveGitServer;

    public OpenShiftClient(KubernetesFactory factory, String namespace, boolean keepAliveGitServer) {
        this.factory = factory;
        this.namespace = namespace;
        this.keepAliveGitServer = keepAliveGitServer;
        this.gitserver = new GitServer(this.getClient(), namespace);
    }

    public List<Exception> clean(ResourceHolder holder) {

        List<Exception> exceptions = new ArrayList<Exception>();

        for (KubernetesResource resource : holder.getResources()) {
            try {
                if (resource instanceof Pod) {
                    Pod m = (Pod) resource;
                    getClient().deletePod(m.getMetadata().getName(), namespace);
                } else if (resource instanceof ImageStream) {
                    ImageStream m = (ImageStream) resource;
                    getClientExt().deleteImageStream(m.getMetadata().getName(), namespace);
                } else if (resource instanceof BuildConfig) {
                    BuildConfig m = (BuildConfig) resource;
                    getClientExt().deleteBuildConfig(m.getMetadata().getName(), namespace);
                } else if (resource instanceof Build) {
                    Build build = (Build) resource;
                    getClientExt().deleteBuild(build.getMetadata().getName(), namespace);
                }
            } catch (Exception e) {
                exceptions.add(e);
            }
        }
        return exceptions;
    }

	public ResourceHolder build(File folder, Pod template) throws Exception {
		URI repoUri = gitserver.push(folder, template.getMetadata().getName());

		ResourceHolder holder = new ResourceHolder();

		Map<String, String> defaultLabels = getDefaultLabels();

		String runID = template.getMetadata().getName();

		try {
			ImageStream is = new ImageStreamBuilder()
					.withNewMetadata()
						.withName(runID)
						.withLabels(defaultLabels)
						.endMetadata()
					.build();
			is = (ImageStream)KubernetesHelper.loadJson(getClientExt().createImageStream(is, namespace));
			holder.addResource(is);

			BuildConfig config = new BuildConfigBuilder()
					.withNewMetadata()
						.withName(runID)
						.withLabels(defaultLabels)
						.endMetadata()
					.withNewSpec()
						.withNewSource()
							.withNewGit("master", repoUri.toString())
							.withType("Git")
							.endSource()
						.withNewStrategy()
							.withType("Docker")
							.withNewDockerStrategy()
								.withNoCache(false)
								.endDockerStrategy()
							.endStrategy()
						.withNewOutput()
								.withNewTo()
									.withKind("ImageStreamTag")
									.withName(runID + ":latest")
									.endTo()
							.endOutput()
						.endSpec()
					.build();

			config = (BuildConfig)KubernetesHelper.loadJson(getClientExt().createBuildConfig(config, namespace));
			holder.addResource(config);

			BuildRequest br = new BuildRequestBuilder()
					.withNewMetadata()
						.withName(config.getMetadata().getName())
						.withLabels(defaultLabels)
						.endMetadata()
					.build();

			Build build = ResourceUtil.waitForComplete(
					getClientExt(),
					(Build)KubernetesHelper.loadJson(
							getClientExt().instantiateBuild(
									config.getMetadata().getName(), br, namespace)));

			holder.addResource(build);

			is = getClientExt().getImageStream(is.getMetadata().getName(), namespace);

			Pod service = new PodBuilder()
					.withNewMetadataLike(template.getMetadata())
						.withLabels(defaultLabels)
					.endMetadata()
				.withNewSpecLike(template.getSpec())
					.endSpec()
				.build();

			service.getSpec().getContainers().get(0).setImage(is.getStatus().getTags().get(0).getItems().get(0).getDockerImageReference());
			holder.setPod(service);
		} catch(Exception e) {
			holder.setException(e);
		}
		return holder;
	}

	public Pod createAndWait(Pod resource) throws Exception {
		return waitForStart(
				getClient(),
				(Pod)loadJson(
						getClient().createPod(resource, namespace)));
	}

	public void destroy(Pod resource) throws Exception {
		getClient().deletePod(resource.getMetadata().getName(), namespace);
	}

	public Pod update(Pod resource) throws Exception {
		return getClient().getPod(resource.getMetadata().getName(), namespace);
	}

	public void shutdown() throws Exception {
		if(!keepAliveGitServer) {
			gitserver.shutdown();
		}
	}

	public Kubernetes getClient() {
		if(kubernetes == null) {
			kubernetes = factory.createKubernetes();
		}
		return kubernetes;
	}

	public KubernetesExtensions getClientExt() {
		if(kubernetesExtensions == null) {
			kubernetesExtensions = factory.createKubernetesExtensions();
		}
		return kubernetesExtensions;
	}

	private Map<String, String> getDefaultLabels() {
		Map<String, String> labels = new HashMap<String, String>();
		labels.put("generatedby", "arquillian");
		return labels;
	}

    public class ResourceHolder {

        public Pod pod;
        public Set<KubernetesResource> resources;
        private Exception exception;

        public ResourceHolder() {
            this.resources = new HashSet<KubernetesResource>();
        }

        public void setException(Exception exception) {
            this.exception = exception;
        }

        public Exception getException() {
            return exception;
        }

        public void setPod(Pod pod) {
            this.pod = pod;
        }

        public void addResource(KubernetesResource resource) {
            this.resources.add(resource);
        }

        public Set<KubernetesResource> getResources() {
            return resources;
        }

        public Pod getPod() {
            return pod;
        }
    }
}
