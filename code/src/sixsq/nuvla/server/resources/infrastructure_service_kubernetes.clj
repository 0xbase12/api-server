(ns sixsq.nuvla.server.resources.infrastructure-service-kubernetes
  "
Information concerning a Kubernetes resource and the parameters necessary to
manage it.
"
  (:require
    [sixsq.nuvla.server.resources.common.utils :as u]
    [sixsq.nuvla.server.resources.infrastructure-service :as infra-service]
    [sixsq.nuvla.server.resources.infrastructure-service.utils :as is-utils]
    [sixsq.nuvla.server.resources.spec.infrastructure-service-template-kubernetes :as tpl-kubernetes]))


(def ^:const method "kubernetes")


;;
;; multimethods for create request validation
;;

(def create-validate-fn (u/create-spec-validation-fn ::tpl-kubernetes/schema-create))


(defmethod infra-service/create-validate-subtype method
  [resource]
  (create-validate-fn resource))


;;
;; transform template into service resource
;;

(defmethod infra-service/tpl->service method
  [{{:keys [href]} :service-credential :as resource}]
  (-> resource
      (dissoc resource :href :resource-metadata :endpoint :service-credential)
      (assoc :state "CREATED"
             :management-credential-id href)))


;;
;; post-add hook that creates a job that will deploy a kubernetes cluster
;;

(defmethod infra-service/post-add-hook method
  [{:keys [id] :as service} request]
  (is-utils/job-hook id request "start_infrastructure_service_kubernetes" "STARTING"))


(defmethod infra-service/post-delete-hook method
  [{:keys [id] :as service} request]
  (is-utils/job-hook id request "stop_infrastructure_service_kubernetes" "STOPPING"))
