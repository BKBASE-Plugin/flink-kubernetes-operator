---
title: "Quick Start"
weight: 1
type: docs
aliases:
- /try-flink-kubernetes-operator/quick-start.html
---
<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

# Quick Start

This document provides a quick introduction to using the Flink Kubernetes Operator. Readers 
of this document will be able to deploy the Flink operator itself and an example Flink job to a local 
Kubernetes installation.

## Prerequisites

We assume that you have a local installations of the following:
1. [docker](https://docs.docker.com/)
2. [kubernetes](https://kubernetes.io/)
3. [helm](https://helm.sh/docs/intro/quickstart/)

So that the `docker`, `kubectl` and `helm` commands are available on your local system.

For docker we recommend that you have [Docker Desktop](https://www.docker.com/products/docker-desktop) installed 
and configured with at least 8GB of RAM.
For kubernetes [minikube](https://minikube.sigs.k8s.io/docs/start/) is our choice, at the time of writing this we are 
using version v1.21.5. You can start a cluster with the following command:

```bash
minikube start --kubernetes-version=v1.21.5
😄 minikube v1.25.1 on Darwin 12.1
🆕 Kubernetes 1.23.1 is now available. If you would like to upgrade, specify: --kubernetes-version=v1.23.1
✨ Using the docker driver based on existing profile
👍 Starting control plane node minikube in cluster minikube
🚜 Pulling base image ...
🏃 Updating the running docker "minikube" container ...
🐳 Preparing Kubernetes v1.21.5 on Docker 20.10.12 ...
▪ kubelet.housekeeping-interval=5m
🔎 Verifying Kubernetes components...
▪ Using image gcr.io/k8s-minikube/storage-provisioner:v5 (http://gcr.io/k8s-minikube/storage-provisioner:v5)
🌟 Enabled addons: storage-provisioner, default-storageclass
🏄 Done! kubectl is now configured to use "minikube" cluster and "default" namespace by default
```

We also recommend [k9s](https://k9scli.io/) as GUI for kubernetes, but it is optional for this quickstart guide.

## Building the operator image

You can build the docker image of the operator via the following command from the project root folder:
```bash
docker build . -t flink-operator
```
When using minikube you need to make this image available within the running minikube container:
```bash
minikube image load flink-operator
```

## Deploying the operator

Install the certificate manager to enable adding the webhook component (only neede once per Kubernetes cluster):
```bash
kubectl create -f https://github.com/jetstack/cert-manager/releases/download/v1.7.1/cert-manager.yaml
```
You can deploy the operator via the included helm chart:
```bash
helm install flink-operator helm/flink
```
You may verify your installation via `kubectl` and `helm`:
```bash
kubectl get pods
NAME READY STATUS RESTARTS AGE
flink-operator-fb5d46f94-ghd8b 2/2 Running 0 4m21s

helm list
NAME NAMESPACE REVISION UPDATED STATUS CHART APP VERSION
flink-operator default 1 2022-03-09 17 (tel:12022030917):39:55.461359 +0100 CET deployed flink-operator-0.1.0 1.0.13
```

## Submitting a Flink job

Once the operator is running as seen in the previous step you are ready to submit a Flink job:
```bash
kubectl create -f examples/basic.yaml
```
You may follow the logs of your job, after a successful startup (which can take on the order of a minute in a fresh environment, seconds afterwards) you can:

```bash
kubectl logs -f deploy/basic-example

2022-03-11 21:46:04,458 INFO  org.apache.flink.runtime.checkpoint.CheckpointCoordinator    [] - Triggering checkpoint 206 (type=CHECKPOINT) @ 1647035164458 for job a12c04ac7f5d8418d8ab27931bf517b7.
2022-03-11 21:46:04,465 INFO  org.apache.flink.runtime.checkpoint.CheckpointCoordinator    [] - Completed checkpoint 206 for job a12c04ac7f5d8418d8ab27931bf517b7 (28509 bytes, checkpointDuration=7 ms, finalizationTime=0 ms).
2022-03-11 21:46:06,458 INFO  org.apache.flink.runtime.checkpoint.CheckpointCoordinator    [] - Triggering checkpoint 207 (type=CHECKPOINT) @ 1647035166458 for job a12c04ac7f5d8418d8ab27931bf517b7.
2022-03-11 21:46:06,483 INFO  org.apache.flink.runtime.checkpoint.CheckpointCoordinator    [] - Completed checkpoint 207 for job a12c04ac7f5d8418d8ab27931bf517b7 (28725 bytes, checkpointDuration=25 ms, finalizationTime=0 ms).
```

To expose the Flink Dashboard you may add a port-forward rule or start a tunnel:

```bash
minikube tunnel
```

Now the Flink Dashboard is accessible at [localhost:8081](http://localhost:8081/).