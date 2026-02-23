# k8s manifests for Jenkins + ArgoCD

This directory is designed for GitOps flow.

## Deployment flow
1. Jenkins builds and pushes backend/frontend images.
2. Jenkins updates tags in `k8s/kustomization.yaml` (`images[].newTag`) and commits.
3. ArgoCD watches this path and syncs to cluster.

## Why tags are in kustomization
- `Deployment` files keep only image repository name.
- Actual image tags are controlled in one place (`kustomization.yaml`).
- This is easier for automated commit/promotion per environment.

## Resource apply order (ArgoCD sync-wave)
- `-1`: Secret, PVC
- `0`: Deployment, Service
- `1`: Ingress

## Required replacements before production
- Replace `your-dockerhub-id` image repository names.
- Replace secret placeholders in `k8s/secrets/backend-secret.yaml`.
- Replace ingress host `maplog.local` with real domain.
- Ensure DB endpoint in secret matches actual DB service or managed DB.
