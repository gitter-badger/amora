package backend.indexer

import scala.util.Failure
import scala.util.Success

import org.junit.Test

import backend.TestUtils
import backend.actors.IndexerMessage._
import research.converter.protocol._

class ModelIndexerTest {
  import TestUtils._

  case class Data(varName: String, value: String)

  def ask(modelName: String, rawQuery: String, data: Indexable*): Seq[Seq[Data]] = {
    val query = rawQuery.replaceFirst("""\?MODEL\?""", modelName)
    val res = Indexer.withInMemoryDataset { dataset ⇒
      Indexer.withModel(dataset, modelName) { model ⇒
        data foreach (Indexer.add(modelName, model, _))

        if (debugTests) {
          Indexer.queryResultAsString(modelName, "select * { ?s ?p ?o }", model) foreach println
          Indexer.queryResultAsString(modelName, query, model) foreach println
        }

        Indexer.queryResult(modelName, query, model) { (v, q) ⇒
          val res = q.get(v)
          require(res != null, s"The variable `$v` does not exist in the result set.")
          Data(v, res.toString)
        }
      }.flatten
    }.flatten
    res match {
      case Success(res) ⇒
        res.map(_.sortBy(d ⇒ (d.varName, d.value)))
      case Failure(f) ⇒
        throw new RuntimeException("An error happened during the test.", f)
    }
  }

  def modelName = "http://test.model/"

  def mkDecl(name: String, owner: Decl, attachments: Attachment*) = {
    val decl = Decl(name, owner)
    decl.addAttachments(attachments: _*)
    decl
  }

  def mkPackage(name: String, owner: Decl) =
    mkDecl(name, owner, Attachment.Package)

  def mkClass(name: String, owner: Decl) =
    mkDecl(name, owner, Attachment.Class)

  @Test
  def single_project() = {
    val project = Project("project")

    ask(modelName, """
        PREFIX c:<?MODEL?>
        SELECT * WHERE {
          [a c:Project] c:name ?name .
        }
      """, project) === Seq(Seq(Data("name", "project")))
  }

  @Test
  def multiple_projects() = {
    val project1 = Project("p1")
    val project2 = Project("p2")

    ask(modelName, """
        PREFIX c:<?MODEL?>
        SELECT * WHERE {
          [a c:Project] c:name ?name .
        }
      """, project1, project2) === Seq(
          Seq(Data("name", "p1")),
          Seq(Data("name", "p2")))
  }

  @Test
  def single_artifact() = {
    val project = Project("project")
    val artifact = Artifact(project, "organization", "artifact", "v1")

    ask(modelName, """
        PREFIX c:<?MODEL?>
        SELECT * WHERE {
          [a c:Artifact] c:organization ?organization ; c:name ?name ; c:version ?version .
        }
      """, artifact) === Seq(
          Seq(Data("name", "artifact"), Data("organization", "organization"), Data("version", "v1")))
  }

  @Test
  def multiple_artifacts() = {
    val project = Project("project")
    val artifact1 = Artifact(project, "o1", "a1", "v1")
    val artifact2 = Artifact(project, "o2", "a2", "v2")

    ask(modelName, """
        PREFIX c:<?MODEL?>
        SELECT * WHERE {
          [a c:Artifact] c:organization ?organization ; c:name ?name ; c:version ?version .
        }
      """, artifact1, artifact2) === Seq(
          Seq(Data("name", "a1"), Data("organization", "o1"), Data("version", "v1")),
          Seq(Data("name", "a2"), Data("organization", "o2"), Data("version", "v2")))
  }

  @Test
  def multiple_artifacts_belong_to_same_project() = {
    val project = Project("project")
    val artifact1 = Artifact(project, "o1", "a1", "v1")
    val artifact2 = Artifact(project, "o2", "a2", "v2")

    ask(modelName, """
        PREFIX c:<?MODEL?>
        SELECT DISTINCT * WHERE {
          [a c:Artifact] c:owner [c:name ?name] .
        }
      """, artifact1, artifact2) === Seq(
          Seq(Data("name", "project")))
  }

  @org.junit.Ignore("Ignore until _root_ package is gone")
  @Test
  def files_with_same_name_of_different_artifacts() = {
    val project = Project("project")
    val artifact1 = Artifact(project, "organization", "name", "v1")
    val artifact2 = Artifact(project, "organization", "name", "v2")
    val file1 = File(artifact1, "a/b/c/Test.scala", Seq())
    val file2 = File(artifact2, "a/b/c/Test.scala", Seq())

    ask(modelName, """
        PREFIX c:<?MODEL?>
        SELECT * WHERE {
          [a c:File] c:name ?name; c:artifact [c:version ?version] .
        }
      """, artifact1, artifact2, file1, file2) === Seq(
          Seq(Data("name", "a/b/c/Test.scala"), Data("version", "v1")),
          Seq(Data("name", "a/b/c/Test.scala"), Data("version", "v2")))
  }

  @Test
  def the_owner_of_a_project_does_not_exist() = {
    val project = Project("p")

    ask(modelName, """
        PREFIX c:<?MODEL?>
        SELECT * WHERE {
          [a c:Project] c:owner ?owner .
        }
      """, project) === Seq()
  }

  @Test
  def the_owner_of_an_artifact_is_a_project() = {
    val project = Project("p")
    val artifact = Artifact(project, "o", "a", "v1")

    ask(modelName, """
        PREFIX c:<?MODEL?>
        SELECT * WHERE {
          [a c:Artifact] c:owner [c:name ?name] .
        }
      """, artifact) === Seq(Seq(Data("name", "p")))
  }

  @Test
  def the_owner_of_the_top_package_is_an_artifact() = {
    val project = Project("p")
    val artifact = Artifact(project, "o", "a", "v1")
    val file = File(artifact, "pkg/A.scala", Seq(mkPackage("pkg", Root)))

    ask(modelName, """
        PREFIX c:<?MODEL?>
        SELECT * WHERE {
          [a c:Package] c:owner [c:name ?name] .
        }
      """, artifact, file) === Seq(Seq(Data("name", "a")))
  }

  @Test
  def the_owner_of_a_non_top_package_is_a_package() = {
    val project = Project("p")
    val artifact = Artifact(project, "o", "a", "v1")
    val file = File(artifact, "pkg/A.scala", Seq(mkPackage("inner", mkPackage("pkg", Root))))

    ask(modelName, """
        PREFIX c:<?MODEL?>
        PREFIX s:<http://schema.org/>
        SELECT * WHERE {
          [c:owner [a c:Package]] s:name ?name .
        }
      """, artifact, file) === Seq(Seq(Data("name", "inner")))
  }

  @Test
  def the_owner_of_a_top_level_class_is_a_file() = {
    val project = Project("p")
    val artifact = Artifact(project, "o", "a", "v1")
    val file = File(artifact, "pkg/A.scala", Seq(mkClass("A", mkPackage("pkg", Root))))

    ask(modelName, """
        PREFIX c:<?MODEL?>
        PREFIX s:<http://schema.org/>
        SELECT * WHERE {
          [a c:Class] c:owner [c:name ?name] .
        }
      """, artifact, file) === Seq(Seq(Data("name", "pkg/A.scala")))
  }

  @Test
  def the_owner_of_a_class_in_the_default_package_is_a_file() = {
    val project = Project("p")
    val artifact = Artifact(project, "o", "n", "v1")
    val file = File(artifact, "A.scala", Seq(mkClass("A", Root)))

    ask(modelName, """
        PREFIX c:<?MODEL?>
        SELECT * WHERE {
          [a c:Class] c:owner [c:name ?name] .
        }
      """, artifact, file) === Seq(Seq(Data("name", "A.scala")))
  }
}