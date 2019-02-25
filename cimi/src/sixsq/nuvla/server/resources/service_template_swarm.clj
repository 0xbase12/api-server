(ns sixsq.nuvla.server.resources.service-template-swarm
  "
Template that requires information necessary to create a new Docker Swarm
cluster on a given cloud infrastructure.
"
  (:require
    [sixsq.nuvla.server.resources.common.utils :as u]
    [sixsq.nuvla.server.resources.resource-metadata :as md]
    [sixsq.nuvla.server.resources.service-template :as tpl]
    [sixsq.nuvla.server.resources.spec.service-template-swarm :as tpl-swarm]
    [sixsq.nuvla.server.util.metadata :as gen-md]))


(def ^:const resource-type (u/ns->type *ns*))


(def ^:const method "swarm")


(def template {:method             method

               :name               "create swarm service template"
               :description        "template to create docker swarm"
               :resourceMetadata   (str "resource-metadata/" tpl/resource-type "-" method)

               :type               "swarm"
               :cloud-service      {:href "service/change-me"}
               :service-credential {:href "credential/change-me"}

               :acl                tpl/resource-acl})


;;
;; initialization: register this template and provide metadata description
;;

(defn initialize
  []
  (tpl/register template)
  (md/register (gen-md/generate-metadata ::ns ::tpl/ns ::tpl-swarm/schema)))


;;
;; multimethods for validation
;;

(def validate-fn (u/create-spec-validation-fn ::tpl-swarm/schema))


(defmethod tpl/validate-subtype method
  [resource]
  (validate-fn resource))
