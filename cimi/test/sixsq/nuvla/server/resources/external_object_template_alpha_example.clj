(ns sixsq.nuvla.server.resources.external-object-template-alpha-example
  (:require
    [clojure.spec.alpha :as s]
    [sixsq.nuvla.server.resources.common.utils :as u]
    [sixsq.nuvla.server.resources.external-object :as eo-resource]
    [sixsq.nuvla.server.resources.external-object-template :as eot]
    [sixsq.nuvla.server.resources.spec.common :as c]
    [sixsq.nuvla.server.resources.spec.external-object :as eo]
    [sixsq.nuvla.server.util.spec :as su]))

(def ^:const objectType "alpha")

;;
;; schemas
;;

(s/def :cimi.external-object.alpha/alphaKey pos-int?)

(def external-object-keys-spec
  (u/remove-req eo/common-external-object-attrs #{::eo/bucketName
                                                  ::eo/objectName
                                                  ::eo/objectStoreCred}))

(def external-object-alpha-keys-spec
  (su/merge-keys-specs [external-object-keys-spec
                        {:req-un [:cimi.external-object.alpha/alphaKey]}]))

(def resource-keys-spec
  (su/merge-keys-specs [c/common-attrs
                        external-object-alpha-keys-spec]))

(s/def :cimi/external-object.alpha
  (su/only-keys-maps resource-keys-spec))


(s/def :cimi.external-object-template.alpha/template
  (su/only-keys-maps c/template-attrs
                     (u/remove-req external-object-alpha-keys-spec #{::eo/state})))

(s/def :cimi/external-object-template.alpha-create
  (su/only-keys-maps c/create-attrs
                     {:req-un [:cimi.external-object-template.alpha/template]}))

;;
;; template resource
;;

(def ^:const resource-template
  {:objectType objectType
   :alphaKey   1001})


;;
;; initialization: register this external object template
;;

(defn initialize
  []
  (eot/register resource-template))

;;
;; multimethods for validation
;;

(def validate-fn (u/create-spec-validation-fn :cimi/external-object.alpha))
(defmethod eo-resource/validate-subtype objectType
  [resource]
  (validate-fn resource))

(def validate-fn (u/create-spec-validation-fn :cimi/external-object-template.alpha-create))
(defmethod eo-resource/create-validate-subtype objectType
  [resource]
  (validate-fn resource))

(def validate-fn (u/create-spec-validation-fn :cimi.external-object-template.alpha/template))
(defmethod eot/validate-subtype-template objectType
  [resource]
  (validate-fn resource))
