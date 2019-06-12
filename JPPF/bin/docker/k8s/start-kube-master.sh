#! /bin/sh

set -E

#JOIN_CMD=$(sudo kubeadm init --apiserver-advertise-address=192.168.1.12 --pod-network-cidr=10.244.0.0/16 | grep -E "(kubeadm join|--discovery-token-ca-cert-hash)")
#echo "join command = sudo $JOIN_CMD"
sudo kubeadm init --apiserver-advertise-address=192.168.1.12 --pod-network-cidr=10.244.0.0/16
mkdir -p $HOME/.kube && sudo cp -f /etc/kubernetes/admin.conf $HOME/.kube/config && sudo chown $(id -u):$(id -g) $HOME/.kube/config
kubectl apply -f https://raw.githubusercontent.com/coreos/flannel/master/Documentation/kube-flannel.yml

# init helm and create tiller RBAC service account
sudo helm init
kubectl create serviceaccount --namespace kube-system tiller
kubectl create clusterrolebinding tiller-cluster-rule --clusterrole=cluster-admin --serviceaccount=kube-system:tiller
kubectl patch deploy --namespace kube-system tiller-deploy -p '{"spec":{"template":{"spec":{"serviceAccount":"tiller"}}}}'
