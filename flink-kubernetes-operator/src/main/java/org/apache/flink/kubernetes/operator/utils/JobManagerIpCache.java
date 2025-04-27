/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.kubernetes.operator.utils;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.fabric8.kubernetes.api.model.ListOptions;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/** Util to cache flink jobmanager info. */
public final class JobManagerIpCache {

    private static final Logger LOG = LoggerFactory.getLogger(JobManagerIpCache.class);
    private static final Cache<String, String> cache =
            CacheBuilder.newBuilder()
                    .recordStats()
                    .expireAfterAccess(1, TimeUnit.DAYS)
                    .maximumSize(50000)
                    .build();

    public static String get(String clusterId, String namespace) {
        String jmIp = cache.getIfPresent(clusterId);
        if (jmIp == null) {
            LOG.warn(
                    "Could not get jobmanager ip from cache for cluster {} in namespace {}",
                    clusterId,
                    namespace);
        }
        return jmIp;
    }

    public static void set(String clusterId, String jmIp) {
        cache.put(clusterId, jmIp);
    }

    public static void watchFlinkJobManagerPod(
            KubernetesClient kubernetesClient, Set<String> namespaces) {
        Map<String, String> labels = new HashMap<>();
        labels.put("type", "flink-native-kubernetes");
        labels.put("component", "jobmanager");
        labels.put("deployMode", "flink-k8s-operator");
        for (String namespace : namespaces) {
            LOG.info("List and watch jobmanager pod of namespace '{}'", namespace);
            ListOptions listOptions = new ListOptions();
            listOptions.setResourceVersion("0");

            kubernetesClient
                    .pods()
                    .inNamespace(namespace)
                    .withNewFilter()
                    .withLabels(labels)
                    .endFilter()
                    .withResourceVersion("0")
                    .watch(
                            listOptions,
                            new Watcher<Pod>() {
                                @Override
                                public void eventReceived(Action action, Pod pod) {
                                    String clusterId = pod.getMetadata().getLabels().get("app");
                                    switch (action) {
                                        case ADDED:
                                        case MODIFIED:
                                            String jmIp = pod.getStatus().getPodIP();
                                            if (jmIp != null) {
                                                LOG.info(
                                                        "Update jobmanager ip cache: action: {}, pod: {}, ip: {}",
                                                        action,
                                                        clusterId,
                                                        jmIp);
                                                set(clusterId, jmIp);
                                            }
                                            break;
                                        case DELETED:
                                            LOG.info(
                                                    "Remove jobmanager ip cache: action: {}, pod: {}",
                                                    action,
                                                    clusterId);
                                            cache.invalidate(clusterId);
                                        default:
                                            break;
                                    }
                                }

                                @Override
                                public void onClose(WatcherException cause) {
                                    LOG.info("jobmanager pod watch onClose: " + cause);
                                }
                            });
        }
    }
}
