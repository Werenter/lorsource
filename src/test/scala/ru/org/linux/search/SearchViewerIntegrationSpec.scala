/*
 * Copyright 1998-2016 Linux.org.ru
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package ru.org.linux.search

import com.sksamuel.elastic4s.ElasticClient
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.{ContextConfiguration, TestContextManager}

@ContextConfiguration(classes = Array(classOf[SearchIntegrationTestConfiguration]))
class SearchViewerIntegrationSpec  extends SpecificationWithJUnit {
  new TestContextManager(this.getClass).prepareTestInstance(this)

  @Autowired
  var indexService: ElasticsearchIndexService = _

  @Autowired
  var elastic: ElasticClient = _

  trait IndexFixture extends Scope {
    indexService.createIndexIfNeeded()
  }

  "SearchViewer" should {
    "make valid default search" in new IndexFixture {
      val response = new SearchViewer(new SearchRequest(), elastic).performSearch

      response.totalHits must be equalTo 0
    }
  }
}
