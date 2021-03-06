package backend.schema

import java.net.URLEncoder

import research.converter.protocol._

trait Schema
final case class Project(name: String) extends Schema
final case class Artifact(owner: Project, organization: String, name: String, version: String) extends Schema
final case class File(owner: Schema, name: String) extends Schema
final case class Package(name: String, owner: Schema) extends Schema
final case class Class(name: String, owner: Schema) extends Schema
final case class AbstractClass(name: String, owner: Schema) extends Schema
final case class Object(name: String, owner: Schema) extends Schema
final case class Trait(name: String, owner: Schema) extends Schema
final case class Def(name: String, owner: Schema) extends Schema
final case class Val(name: String, owner: Schema) extends Schema
final case class Var(name: String, owner: Schema) extends Schema
final case class LazyVal(name: String, owner: Schema) extends Schema

object Schema {

  def mkShortId(s: Schema): String = s match {
    case Project(name) ⇒
      name
    case Artifact(owner, organization, name, version) ⇒
      s"${mkShortId(owner)}/$organization/$name/$version"
    case File(owner, name) ⇒
      s"${mkShortId(owner)}/$name"
    case Package(name, owner) ⇒
      s"${mkShortId(owner)}/$name"
    case Class(name, owner) ⇒
      s"${mkShortId(owner)}/$name"
    case AbstractClass(name, owner) ⇒
      s"${mkShortId(owner)}/$name"
    case Object(name, owner) ⇒
      s"${mkShortId(owner)}/$name"
    case Trait(name, owner) ⇒
      s"${mkShortId(owner)}/$name"
    case Def(name, owner) ⇒
      s"${mkShortId(owner)}/$name"
    case Val(name, owner) ⇒
      s"${mkShortId(owner)}/$name"
    case Var(name, owner) ⇒
      s"${mkShortId(owner)}/$name"
    case LazyVal(name, owner) ⇒
      s"${mkShortId(owner)}/$name"
  }

  def mkId(s: Schema): String = s match {
    case _: Project ⇒
      s"http://amora.center/kb/amora/Project/0.1/${mkShortId(s)}"
    case _: Artifact ⇒
      s"http://amora.center/kb/amora/Artifact/0.1/${mkShortId(s)}"
    case _: File ⇒
      s"http://amora.center/kb/amora/File/0.1/${mkShortId(s)}"
    case _: Package ⇒
      s"http://amora.center/kb/amora/Package/0.1/${mkShortId(s)}"
    case _: Class ⇒
      s"http://amora.center/kb/amora/Class/0.1/${mkShortId(s)}"
    case _: AbstractClass ⇒
      s"http://amora.center/kb/amora/AbstractClass/0.1/${mkShortId(s)}"
    case _: Object ⇒
      s"http://amora.center/kb/amora/Object/0.1/${mkShortId(s)}"
    case _: Trait ⇒
      s"http://amora.center/kb/amora/Trait/0.1/${mkShortId(s)}"
    case _: Def ⇒
      s"http://amora.center/kb/amora/Def/0.1/${mkShortId(s)}"
    case _: Val ⇒
      s"http://amora.center/kb/amora/Val/0.1/${mkShortId(s)}"
    case _: Var ⇒
      s"http://amora.center/kb/amora/Var/0.1/${mkShortId(s)}"
    case _: LazyVal ⇒
      s"http://amora.center/kb/amora/LazyVal/0.1/${mkShortId(s)}"
  }

  def mkDefn(s: Schema): String = s match {
    case _: Project ⇒
      s"http://amora.center/kb/amora/Schema/0.1/Project/0.1"
    case _: Artifact ⇒
      s"http://amora.center/kb/amora/Schema/0.1/Artifact/0.1"
    case _: File ⇒
      s"http://amora.center/kb/amora/Schema/0.1/File/0.1"
    case _: Package ⇒
      s"http://amora.center/kb/amora/Schema/0.1/Package/0.1"
    case _: Class ⇒
      s"http://amora.center/kb/amora/Schema/0.1/Class/0.1"
    case _: AbstractClass ⇒
      s"http://amora.center/kb/amora/Schema/0.1/AbstractClass/0.1"
    case _: Object ⇒
      s"http://amora.center/kb/amora/Schema/0.1/Object/0.1"
    case _: Trait ⇒
      s"http://amora.center/kb/amora/Schema/0.1/Trait/0.1"
    case _: Def ⇒
      s"http://amora.center/kb/amora/Schema/0.1/Def/0.1"
    case _: Val ⇒
      s"http://amora.center/kb/amora/Schema/0.1/Val/0.1"
    case _: Var ⇒
      s"http://amora.center/kb/amora/Schema/0.1/Var/0.1"
    case _: LazyVal ⇒
      s"http://amora.center/kb/amora/Schema/0.1/LazyVal/0.1"
  }

  def mkSparqlUpdate(schemas: Seq[Schema]): String = {
    val sb = new StringBuilder

    def mk(s: Schema): String = s match {
      case Project(name) ⇒
        val id = mkId(s)
        val defn = mkDefn(s)
        sb.append(s"""|  <$id> a <$defn/> .
                      |  <$id> <$defn/name> "$name" .
        |""".stripMargin)
        id
      case Artifact(owner, organization, name, version) ⇒
        val oid = mk(owner)
        val id = mkId(s)
        val defn = mkDefn(s)
        sb.append(s"""|  <$id> a <$defn/> .
                      |  <$id> <$defn/owner> <$oid> .
                      |  <$id> <$defn/organization> "$organization" .
                      |  <$id> <$defn/name> "$name" .
                      |  <$id> <$defn/version> "$version" .
        |""".stripMargin)
        id
      case File(owner, fname) ⇒
        val oid = mk(owner)
        val id = mkId(s)
        val defn = mkDefn(s)
        sb.append(s"""|  <$id> a <$defn/> .
                      |  <$id> <$defn/owner> <$oid> .
                      |  <$id> <$defn/name> "$fname" .
        |""".stripMargin)
        id
      case Package(name, parent) ⇒
        val oid = mk(parent)
        val id = mkId(s)
        val defn = mkDefn(s)
        sb.append(s"""|  <$id> a <$defn/> .
                      |  <$id> <$defn/owner> <$oid> .
                      |  <$id> <$defn/name> "$name" .
        |""".stripMargin)
        id
      case Class(name, parent) ⇒
        val oid = mk(parent)
        val id = mkId(s)
        val defn = mkDefn(s)
        sb.append(s"""|  <$id> a <$defn/> .
                      |  <$id> <$defn/owner> <$oid> .
                      |  <$id> <$defn/name> "$name" .
        |""".stripMargin)
        id
      case Def(name, parent) ⇒
        val oid = mk(parent)
        val id = mkId(s)
        val defn = mkDefn(s)
        sb.append(s"""|  <$id> a <$defn/> .
                      |  <$id> <$defn/owner> <$oid> .
                      |  <$id> <$defn/name> "$name" .
        |""".stripMargin)
        id
      case Val(name, parent) ⇒
        val oid = mk(parent)
        val id = mkId(s)
        val defn = mkDefn(s)
        sb.append(s"""|  <$id> a <$defn/> .
                      |  <$id> <$defn/owner> <$oid> .
                      |  <$id> <$defn/name> "$name" .
        |""".stripMargin)
        id
      case Var(name, parent) ⇒
        val oid = mk(parent)
        val id = mkId(s)
        val defn = mkDefn(s)
        sb.append(s"""|  <$id> a <$defn/> .
                      |  <$id> <$defn/owner> <$oid> .
                      |  <$id> <$defn/name> "$name" .
        |""".stripMargin)
        id
      case LazyVal(name, parent) ⇒
        val oid = mk(parent)
        val id = mkId(s)
        val defn = mkDefn(s)
        sb.append(s"""|  <$id> a <$defn/> .
                      |  <$id> <$defn/owner> <$oid> .
                      |  <$id> <$defn/name> "$name" .
        |""".stripMargin)
        id
    }

    sb.append("INSERT DATA {\n")
    schemas foreach mk
    sb.append("}")
    sb.toString()
  }

}

object HierarchySchema {

  def mkSparqlUpdate(schema: Schema, data: Seq[Hierarchy]): String = {
    val sb = new StringBuilder

    def mkTpe(decl: Decl) = {
      if (decl.attachments(Attachment.Lazy) && decl.attachments(Attachment.Val))
        "LazyVal"
      else if (decl.attachments(Attachment.Abstract) && decl.attachments(Attachment.Class))
        "AbstractClass"
      else
        decl.attachments.collectFirst {
          case Attachment.Class ⇒ "Class"
          case Attachment.Object ⇒ "Object"
          case Attachment.Trait ⇒ "Trait"
          case Attachment.Package ⇒ "Package"
          case Attachment.Def ⇒ "Def"
          case Attachment.Val ⇒ "Val"
          case Attachment.Var ⇒ "Var"
        }.getOrElse("Decl")
    }

    def mkFullPath(decl: Decl) = {
      def findArtifact(schema: Schema): Artifact = schema match {
        case a: Artifact ⇒ a
        case p: Package ⇒ findArtifact(p.owner)
        case f: File ⇒ findArtifact(f.owner)
        case _ ⇒ ???
      }
      val a = findArtifact(schema)
      val tpe = mkTpe(decl)
      s"http://amora.center/kb/amora/$tpe/0.1/${Schema.mkShortId(a)}/${mkShortPath(decl)}"
    }

    def mkOwnerPath(h: Hierarchy, owner: Decl) = {
      val isTopLevelDecl = {
        def isTopLevelDecl = h.attachments.exists(Set(Attachment.Class, Attachment.Trait, Attachment.Object))
        def isPkg = owner.attachments(Attachment.Package)
        isTopLevelDecl && isPkg
      }
      if (isTopLevelDecl)
        Schema.mkId(schema)
      else
        mkFullPath(owner)
    }

    def loop(h: Hierarchy): Unit = h match {
      case Root ⇒
      case decl @ Decl(name, owner) ⇒
        val tpe = mkTpe(decl)
        val path = mkFullPath(decl)
        val schemaPath = s"http://amora.center/kb/amora/Schema/0.1/$tpe/0.1"
        sb.append(s"  <$path> a <$schemaPath/> .\n")
        sb.append(s"""  <$path> <$schemaPath/name> "$name" .""" + "\n")

        if (h.attachments(Attachment.Param)) {
          sb.append(s"""  <$path> <$schemaPath/flag> "param" .""" + "\n")
        }
        if (h.attachments(Attachment.TypeParam)) {
          sb.append(s"""  <$path> <$schemaPath/flag> "tparam" .""" + "\n")
        }

        decl.attachments.collectFirst {
          case Attachment.JvmSignature(signature) ⇒
            sb.append(s"""  <$path> <$schemaPath/jvmSignature> "$signature" .""" + "\n")
        }

        decl.position match {
          case RangePosition(start, end) ⇒
            sb.append(s"  <$path> <$schemaPath/posStart> $start .\n")
            sb.append(s"  <$path> <$schemaPath/posEnd> $end .\n")
          case _ ⇒
        }

        owner match {
          case Root ⇒
            val ownerPath = Schema.mkDefn(schema)
            sb.append(s"  <$path> <$schemaPath/owner> <$ownerPath> .\n")
          case owner: Decl ⇒
            val ownerPath = mkOwnerPath(decl, owner)
            sb.append(s"  <$path> <$schemaPath/owner> <$ownerPath> .\n")

            loop(owner)
          case _: Ref ⇒
        }

      case ref @ Ref(name, refToDecl, owner, qualifier) ⇒
        val declPath = refToDecl match {
          case d: Decl ⇒ mkFullPath(d)
          // TODO replace this with a real implementation
          case _ ⇒ "???"
        }
        val path = s"$declPath/${Schema.mkShortId(schema)}${uniqueRef(ref.position)}"
        val schemaPath = s"http://amora.center/kb/amora/Schema/0.1/Ref/0.1"
        sb.append(s"  <$path> a <$schemaPath/> .\n")
        sb.append(s"""  <$path> <$schemaPath/name> "$name" .""" + "\n")
        sb.append(s"  <$path> <$schemaPath/refToDecl> <$declPath> .\n")

        ref.position match {
          case RangePosition(start, end) ⇒
            sb.append(s"  <$path> <$schemaPath/posStart> $start .\n")
            sb.append(s"  <$path> <$schemaPath/posEnd> $end .\n")
          case _ ⇒
        }

        owner match {
          case Root ⇒
            val ownerPath = Schema.mkDefn(schema)
            sb.append(s"  <$path> <$schemaPath/owner> <$ownerPath> .\n")
          case owner: Decl ⇒
            val ownerPath = mkOwnerPath(ref, owner)
            sb.append(s"  <$path> <$schemaPath/owner> <$ownerPath> .\n")

            loop(owner)
          case _: Ref ⇒
        }
    }

    sb.append("INSERT DATA {\n")
    data foreach loop
    sb.append("}")
    sb.toString
  }

  private def uniqueRef(pos: Position) = pos match {
    case RangePosition(start, _) ⇒
      s"/$start"
    case _ ⇒
      ""
  }

  def encode(str: String): String =
    URLEncoder.encode(str, "UTF-8")

  private def mkShortPath(decl: Decl) = {
    val Decl(name, owner) = decl
    val n = encode(name)
    val ownerPath = owner match {
      case Root ⇒
        ""
      case _: Decl ⇒
        encode(owner.asString).replace('.', '/')
      case _: Ref ⇒
        val path = encode(owner.asString).replace('.', '/')
        val h = uniqueRef(owner.position)
        s"$path/$h"
    }
    val sig = decl.attachments.collectFirst {
      case Attachment.JvmSignature(signature) ⇒ encode(signature)
    }.getOrElse("")
    val paramAtt = encode(decl.attachments.collectFirst {
      case Attachment.Param ⇒ "<param>"
      case Attachment.TypeParam ⇒ "<tparam>"
    }.getOrElse(""))
    val originPath = if (ownerPath.isEmpty) "" else ownerPath + "/"
    val fullPath = s"$originPath$paramAtt$n$sig"
    fullPath
  }
}
