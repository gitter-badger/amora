package backend
package indexer

import org.junit.Test

import backend.actors.IndexerMessage

class DeprecatedScalaSourceIndexerTest {

  import backend.TestUtils._

  case class Data(varName: String, value: String)

  def ask(modelName: String, data: Seq[(String, String)], query: String): Seq[Data] = {
    val indexer = new Indexer(modelName)
    val dataset = indexer.mkInMemoryDataset
    val res = indexer.writeDataset(dataset) { dataset ⇒
      indexer.withModel(dataset) { model ⇒
        val sindexer = new ScalaSourceIndexer(IgnoreLogger)
        sindexer.convertToHierarchy(data) match {
          case scala.util.Success(data) ⇒
            data foreach {
              case (filename, data) ⇒
                indexer.addFile(IndexerMessage.File(IndexerMessage.NoOrigin, filename, data))(model)
            }
          case scala.util.Failure(f) ⇒
            throw f
        }

        if (debugTests) {
          println(indexer.queryResultAsString("select * { ?s ?p ?o }", model))
          println(indexer.queryResultAsString(query, model))
        }
        indexer.flattenedQueryResult(query, model) { (v, q) ⇒
          val res = q.get(v)
          require(res != null, s"The variable `$v` does not exist in the result set.")
          Data(v, res.toString)
        }
      }
    }

    res.sortBy(d ⇒ (d.varName, d.value))
  }

  @Test
  def find_usages() = {
    val modelName = "http://test.model/"
    ask(modelName, Seq(
      "f1.scala" → """
        package a.b.c
        import d.e.f.Y
        class X {
          def m: Y = null
        }
      """,
      "f2.scala" → """
        package d.e.f
        class Y
      """), s"""
        PREFIX c:<$modelName>
        PREFIX s:<http://schema.org/>
        SELECT ?name WHERE {
          ?class c:attachment "class" .
          ?class s:name ?className .
          FILTER (str(?className) = "Y") .
          ?ref c:reference ?class .
          ?ref c:owner ?owner .
          ?owner s:name ?name .
        }
      """) === Seq(
        Data("name", "f"),
        Data("name", "m"))
  }

  @Test
  def owner_of_refs_in_if_expr() = {
    val modelName = "http://test.model/"
    ask(modelName, Seq(
      "<memory>" → """
        class X {
          val b1 = true
          val b2 = true
          val b3 = true
          def f = if (b1) b2 else b3
        }
      """), s"""
        PREFIX c:<$modelName>
        PREFIX s:<http://schema.org/>
        SELECT ?name WHERE {
          ?def s:name "f" .
          [c:owner ?def] s:name ?name .
        }
      """) === Seq("Boolean", "b1", "b2", "b3").map(Data("name", _))
  }
}
