(ns sixsq.nuvla.server.resources.job
  "
The job resource represents an asynchronous task that will be queued and
processed at a later time. These are typically generated by other resources to
handle potentially long running tasks. These resources will respond with a 202
(accepted) response containing the identifier of the job.

This resource should not be confused with the callback resource which
represents a task that will be executed only when triggered by an external
request.
"
  (:require
    [clojure.tools.logging :as log]
    [sixsq.nuvla.auth.acl-resource :as a]
    [sixsq.nuvla.auth.utils :as auth]
    [sixsq.nuvla.db.impl :as db]
    [sixsq.nuvla.server.resources.common.crud :as crud]
    [sixsq.nuvla.server.resources.common.std-crud :as std-crud]
    [sixsq.nuvla.server.resources.common.utils :as u]
    [sixsq.nuvla.server.resources.job.utils :as ju]
    [sixsq.nuvla.server.resources.resource-metadata :as md]
    [sixsq.nuvla.server.resources.spec.job :as job]
    [sixsq.nuvla.server.util.metadata :as gen-md]))


(def ^:const resource-type (u/ns->type *ns*))


(def ^:const collection-type (u/ns->collection-type *ns*))


(def collection-acl {:query       ["group/nuvla-user"]
                     :add         ["group/nuvla-admin"]
                     :bulk-delete ["group/nuvla-admin"]})

;;
;; initialization
;;

(def resource-metadata (gen-md/generate-metadata ::ns ::job/schema))


(defn initialize
  []
  (std-crud/initialize resource-type ::job/schema)
  (md/register resource-metadata)
  (ju/create-job-queue))


;;
;; multimethods for validation and operations
;;

(def validate-fn (u/create-spec-validation-fn ::job/schema))


(defmethod crud/validate resource-type
  [resource]
  (validate-fn resource))


;;
;; use default ACL method
;;

(defmethod crud/add-acl resource-type
  [resource request]
  (a/add-acl resource request))


;;
;; CRUD operations
;;

(defn add-impl [{{:keys [priority] :or {priority 999} :as body} :body :as request}]
  (a/throw-cannot-add collection-acl request)
  (let [id             (u/new-resource-id resource-type)
        new-job        (-> body
                           u/strip-service-attrs
                           (assoc :resource-type resource-type)
                           (assoc :id id)
                           (assoc :state ju/state-queued)
                           u/update-timestamps
                           ju/job-cond->addition
                           (crud/add-acl request)
                           (crud/validate))
        response       (db/add resource-type new-job {})
        zookeeper-path (ju/add-job-to-queue id priority)]
    (log/debugf "Added %s, zookeeper path %s." id zookeeper-path)
    response))


(defmethod crud/add resource-type
  [request]
  (add-impl request))


(def retrieve-impl (std-crud/retrieve-fn resource-type))


(defmethod crud/retrieve resource-type
  [request]
  (retrieve-impl request))


(defn edit-impl
  [{{select :select} :cimi-params {uuid :uuid} :params body :body :as request}]
  (try
    (let [current                  (-> (str resource-type "/" uuid)
                                       (db/retrieve (assoc-in request [:cimi-params :select] nil))
                                       (a/throw-cannot-edit request))
          dissoc-keys              (-> (map keyword select)
                                       (set)
                                       (u/strip-select-from-mandatory-attrs))
          current-without-selected (apply dissoc current dissoc-keys)
          merged                   (merge current-without-selected body)]
      (-> merged
          (u/update-timestamps)
          (ju/job-cond->edition)
          (crud/validate)
          (db/edit request)))
    (catch Exception e
      (or (ex-data e) (throw e)))))


(defmethod crud/edit resource-type
  [request]
  (edit-impl request))


(def delete-impl (std-crud/delete-fn resource-type))


(defmethod crud/delete resource-type
  [request]
  (delete-impl request))


(def query-impl (std-crud/query-fn resource-type collection-acl collection-type))


(defmethod crud/query resource-type
  [request]
  (query-impl request))


(def bulk-delete-impl (std-crud/bulk-delete-fn resource-type collection-acl collection-type))


(defmethod crud/bulk-delete resource-type
  [request]
  (bulk-delete-impl request))


;;
;; provide an action that allows the job to be stoppable.
;;

(defmethod crud/set-operations resource-type
  [{:keys [id] :as resource} request]
  (let [stop-op (u/action-map id :stop)]
    (cond-> (crud/set-standard-operations resource request)
            (a/can-manage? resource request) (update-in [:operations] conj stop-op))))


(defmethod crud/do-action [resource-type "stop"]
  [{{uuid :uuid} :params :as request}]
  (try
    (-> (str resource-type "/" uuid)
        (db/retrieve request)
        (a/throw-cannot-edit request)
        (ju/stop)
        (db/edit request))
    (catch Exception e
      (or (ex-data e) (throw e)))))


;;
;; internal crud
;;

(defn create-job
  [target-resource action acl & {:keys [priority]}]
  (let [job-map        (cond-> {:action          action
                                :target-resource {:href target-resource}
                                :acl             acl}
                               priority (assoc :priority priority))
        create-request {:params      {:resource-name resource-type}
                        :body        job-map
                        :nuvla/authn auth/internal-identity}]
    (crud/add create-request)))
