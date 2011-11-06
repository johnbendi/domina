(ns domina.test
  (:use [domina :only [nodes single-node xpath children clone append
                       detach destroy destroy-children]])
  (:require [clojure.browser.repl :as repl]))

(repl/connect "http://localhost:9000/repl")

(js* "
  window.tryfn = function(f) {
    try {
      return f.call();
    } catch (e) {
      return e;
    }
  };")

(def tests (atom []))

(defn add-test [name testfn]
  (swap! tests conj [name testfn]))

(defn run-test [testfn]
  (js/tryfn testfn))

(defn run-tests []
  (map (fn [[name testfn]]
         [name (run-test testfn)])
       @tests))

(add-test "reset the page"
          #(do (destroy (xpath "//body/div"))
               (append (xpath "//body") "<div class='d1'></div>")
               (append (xpath "//body/div") "<p class='p1'>P1</p>")
               (append (xpath "//body/div") "<p class='p2'>P2</p>")
               (append (xpath "//body/div") "<p class='p3'>P3</p>")))

(add-test "basic xpath selection"
          #(assert (== 3 (count (nodes (xpath "//p"))))))

(add-test "basic xpath selection (single node)"
          #(assert (instance? js/Element (single-node (xpath "//p")))))

(add-test "xpath selection with class specification"
          #(assert (== 1 (count (nodes (xpath "//div[@class='d1']"))))))

(add-test "a relative xpath expression"
          #(assert (== 3 (count (nodes (-> (xpath "//body/div[@class='d1']")
                                           (xpath "p")))))))
(add-test "advanced xpath"
          #(assert (== 2 (count (nodes (xpath "//p[following-sibling::p[@class='p3']]"))))))

(add-test "child selection"
          #(assert (== 3 (count (children (xpath "//div[@class='d1']"))))))

(add-test "clone a single node"
          #(assert (== 1 (count (clone (single-node (xpath "//p")))))))

(add-test "clone multiple nodes"
          #(assert (== 3 (count (clone (nodes (xpath "//p")))))))


(add-test "append a single child to a single parent"
          #(do (append (xpath "//body") "<p class='appended1'>test</p>")
               (assert (== 1 (count (nodes (xpath "//body/p[@class='appended1']")))))))

(add-test "append multiple children to a single parent"
          #(do (append (xpath "//body")
                       "<p class='appended2'>test2-1</p><p class='appended2'>test2-2</p>")
               (assert (== 2 (count (nodes (xpath "//body/p[@class='appended2']")))))))

(add-test "append a single child to multiple parents"
          #(do (append (xpath "//body/div/p")
                       "<span>!!</span>")
               (assert (== 3 (count (nodes (xpath "//div/p/span")))))))

(add-test "append multiple children to multiple parents"
          #(do (append (xpath "//body/div/p")
                       "some <span class='foo'>more</span> text")
               (assert (== 3 (count (nodes (xpath "//div/p/span[@class='foo']")))))
               (assert (== 9 (count (nodes (xpath "//div/p/text()")))))))

(add-test "destroy a single node"
          #(do (destroy (xpath "//body/p[@class='appended1']"))
               (assert (== 0 (count (nodes (xpath "//body/p[@class='appended1']")))))))

(add-test "destroy multiple nodes"
          #(do (destroy (xpath "//body/p[@class='appended2']"))
               (assert (== 0 (count (nodes (xpath "//body/p[@class='appended2']")))))))

(add-test "detach and reattach a single node"
          #(let [n (detach (xpath "//p[@class='p3']"))]
             (assert (== 0 (count (nodes (xpath "//p[@class='p3']")))))
             (append (xpath "//div[@class='d1']") n)
             (assert (== 1 (count (nodes (xpath "//p[@class='p3']")))))))

(add-test "detach and reattach multiple nodes"
          #(let [n (detach (xpath "//div[@class='d1']/p"))]
             (assert (== 0 (count (nodes (xpath "//div[@class='d1']/p")))))
             (append (xpath "//div[@class='d1']") n)
             (assert (== 3 (count (nodes (xpath "//div[@class='d1']/p")))))))

(add-test "clear a node's contents"
          #(do (destroy-children (xpath "//div[@class='d1']"))
               (assert (== 1 (count (nodes (xpath "//div[@class='d1']")))))
               (assert (== 0 (count (nodes (xpath "//div[@class='d1']/*")))))))


(doseq [[name result] (run-tests)]
  (if (not (== result nil))
    (js/alert (str "Test \"" name "\" failed with error: [" result "]"))))

(js/alert "Tests completed.")

