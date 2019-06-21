#! /bin/sh

set -e

# init the K8S cluster with pd cidr set for flannel
sudo kubeadm init --apiserver-advertise-address=192.168.1.12 --pod-network-cidr=10.244.0.0/16
mkdir -p $HOME/.kube && sudo cp -f /etc/kubernetes/admin.conf $HOME/.kube/config && sudo chown $(id -u):$(id -g) $HOME/.kube/config
# apply the flannel cni plugin
kubectl apply -f https://raw.githubusercontent.com/coreos/flannel/62e44c867a2846fefb68bd5f178daf4da3095ccb/Documentation/kube-flannel.yml
#kubectl apply -f kube-flannel.yml

# allow pod scheduling on master node
kubectl taint nodes --all node-role.kubernetes.io/master-

# init helm and create tiller RBAC service account
sudo helm init
kubectl create serviceaccount --namespace kube-system tiller
kubectl create clusterrolebinding tiller-cluster-rule --clusterrole=cluster-admin --serviceaccount=kube-system:tiller
kubectl patch deploy --namespace kube-system tiller-deploy -p '{"spec":{"template":{"spec":{"serviceAccount":"tiller"}}}}'

# create jppf namespace
kubectl create namespace jppf
