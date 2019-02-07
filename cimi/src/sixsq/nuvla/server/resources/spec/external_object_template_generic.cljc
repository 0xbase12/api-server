(ns sixsq.nuvla.server.resources.spec.external-object-template-generic
  (:require
    [clojure.spec.alpha :as s]
    [sixsq.nuvla.server.resources.common.utils :as u]
    [sixsq.nuvla.server.resources.spec.common :as c]
    [sixsq.nuvla.server.resources.spec.external-object :as eo]
    [sixsq.nuvla.server.util.spec :as su]))


(def template-resource-keys-spec
  (u/remove-req eo/common-external-object-attrs #{::eo/state}))

;; Defines the contents of the generic template used in a create resource.
(s/def ::template
  (su/only-keys-maps c/template-attrs
                     template-resource-keys-spec))

(s/def ::external-object-create
  (su/only-keys-maps c/create-attrs
                     {:req-un [::template]}))
