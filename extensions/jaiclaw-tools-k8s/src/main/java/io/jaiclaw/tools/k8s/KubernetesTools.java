package io.jaiclaw.tools.k8s;

import io.jaiclaw.core.tool.ToolCallback;
import io.jaiclaw.tools.ToolRegistry;
import io.jaiclaw.tools.exec.KubectlPolicyConfig;

import java.util.List;

/**
 * Factory for creating and registering all Kubernetes tools.
 */
public final class KubernetesTools {

    private KubernetesTools() {}

    public static List<ToolCallback> all(KubernetesClientProvider clientProvider) {
        return all(clientProvider, KubectlPolicyConfig.DEFAULT);
    }

    public static List<ToolCallback> all(KubernetesClientProvider clientProvider,
                                         KubectlPolicyConfig kubectlPolicyConfig) {
        return List.of(
                new ListNamespacesTool(clientProvider),
                new ListPodsTool(clientProvider),
                new GetPodLogsTool(clientProvider),
                new DescribeResourceTool(clientProvider),
                new ListEventsTool(clientProvider),
                new ListNodesTool(clientProvider),
                new ListDeploymentsTool(clientProvider),
                new GetResourceUsageTool(clientProvider),
                new KubectlExecTool(kubectlPolicyConfig)
        );
    }

    public static void registerAll(ToolRegistry registry, KubernetesClientProvider clientProvider) {
        registry.registerAll(all(clientProvider));
    }

    public static void registerAll(ToolRegistry registry, KubernetesClientProvider clientProvider,
                                   KubectlPolicyConfig kubectlPolicyConfig) {
        registry.registerAll(all(clientProvider, kubectlPolicyConfig));
    }
}
