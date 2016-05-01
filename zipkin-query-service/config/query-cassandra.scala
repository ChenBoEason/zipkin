/*
 * Copyright 2012 Twitter Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.twitter.app.App
import com.twitter.conversions.time._
import com.twitter.zipkin.builder.QueryServiceBuilder
import com.twitter.zipkin.cassandra.CassandraSpanStoreFactory

val serverPort = sys.env.get("QUERY_PORT").getOrElse("9411").toInt
val adminPort = sys.env.get("QUERY_ADMIN_PORT").getOrElse("9901").toInt
val logLevel = sys.env.get("QUERY_LOG_LEVEL").getOrElse("INFO")

object Factory extends App with CassandraSpanStoreFactory

Factory.keyspace.parse(sys.env.get("CASSANDRA_KEYSPACE").getOrElse("zipkin"))
Factory.ensureSchema.parse(sys.env.get("CASSANDRA_ENSURE_SCHEMA").getOrElse("true"))
Factory.cassandraDest.parse(sys.env.get("CASSANDRA_CONTACT_POINTS").getOrElse("localhost"))
Factory.cassandraSpanTtl.parse(sys.env.get("CASSANDRA_SPAN_TTL").map(_.+(".seconds")).getOrElse(7.days.toString))
Factory.cassandraIndexTtl.parse(sys.env.get("CASSANDRA_INDEX_TTL").map(_.+(".seconds")).getOrElse(3.days.toString))

val username = sys.env.get("CASSANDRA_USERNAME")
val password = sys.env.get("CASSANDRA_PASSWORD")

if (username.isDefined && password.isDefined) {
  Factory.cassandraUsername.parse(username.get)
  Factory.cassandraPassword.parse(password.get)
}

sys.env.get("CASSANDRA_LOCAL_DC").foreach(Factory.cassandraLocalDc.parse(_))
sys.env.get("CASSANDRA_MAX_CONNECTIONS").foreach(Factory.cassandraMaxConnections.parse(_))

val spanStore = Factory.newCassandraStore()
val dependencies = Factory.newCassandraDependencies()

QueryServiceBuilder(
  "0.0.0.0:" + serverPort,
  adminPort,
  logLevel,
  spanStore,
  dependencies
)