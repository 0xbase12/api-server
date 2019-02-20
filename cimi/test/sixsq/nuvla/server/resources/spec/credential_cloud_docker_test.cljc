(ns sixsq.nuvla.server.resources.spec.credential-cloud-docker-test
  (:require
    [clojure.spec.alpha :as s]
    [clojure.test :refer :all]
    [sixsq.nuvla.server.resources.credential :as p]
    [sixsq.nuvla.server.resources.credential-template :as ct]
    [sixsq.nuvla.server.resources.credential-template-cloud :as ctc]
    [sixsq.nuvla.server.resources.credential-template-cloud-docker :as ctco]
    [sixsq.nuvla.server.resources.spec.credential-cloud-docker :as docker]
    [sixsq.nuvla.server.resources.spec.credential-template-cloud-docker :as docker-tpl]))

(def valid-acl ctc/resource-acl-default)

(deftest test-credential-cloud-docker-create-schema-check
  (let [root {:resource-type p/resource-type
              :template      {:key       "foo"
                              :secret    "bar"
                              :quota     7
                              :connector {:href "connector/xyz"}}}]
    (is (s/valid? ::docker/schema-create root))
    (is (s/valid? ::docker-tpl/template (:template root)))
    (doseq [k (into #{} (keys (dissoc root :resource-type)))]
      (is (not (s/valid? ::docker/schema-create (dissoc root k)))))))

(deftest test-credential-cloud-docker-schema-check
  (let [timestamp "1972-10-08T10:00:00.0Z"
        root {:id            (str ct/resource-type "/uuid")
              :resource-type p/resource-type
              :created       timestamp
              :updated       timestamp
              :acl           valid-acl
              :type          ctco/credential-type
              :method        ctco/method
              :key           "foo"
              :secret        "bar"
              :quota         7
              :connector     {:href "connector/xyz"}}]
    (is (s/valid? ::docker/schema root))
    (doseq [k (into #{} (keys root))]
      (is (not (s/valid? ::docker/schema (dissoc root k)))))))


