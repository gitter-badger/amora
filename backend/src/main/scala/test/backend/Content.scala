package test.backend

object Content {
  import scalatags.Text.all._
  import scalatags.Text.tags2

  def indexPage(cssDeps: Seq[String], jsDeps: Seq[String]): String = {
    "<!DOCTYPE html>" + html(
      head(
        meta(charset := "UTF-8"),
        tags2.title("First Scala.js steps"),
        for (d <- cssDeps) yield
          link(rel := "stylesheet", `type` := "text/css", href := d)
      ),
      body(
        for (d <- jsDeps) yield
          script(`type` := "text/javascript", src := d)
      )
    )
  }

}

