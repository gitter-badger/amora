package frontend.webui

import java.nio.ByteBuffer

import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.JSApp
import scala.scalajs.js.JSON

import org.scalajs.dom
import org.scalajs.dom.raw.CloseEvent
import org.scalajs.dom.raw.ErrorEvent
import org.scalajs.dom.raw.Event
import org.scalajs.dom.raw.MessageEvent
import org.scalajs.dom.raw.WebSocket

import frontend.webui.protocol.AuthorizationGranted
import frontend.webui.protocol.Response
import protocol.ConnectionSuccessful

object Main extends JSApp {

  implicit class AsDynamic[A](private val a: A) extends AnyVal {
    def jsg: js.Dynamic = a.asInstanceOf[js.Dynamic]
  }

  val ServerAddress = "localhost:9999"

  /** The socket to the server */
  private var ws: WebSocket = _
  /** The ID of the client which is assigned by the server after authorization. */
  private var id: String = _

  override def main(): Unit = {
    authorize()
  }

  def authorize() = {
    val ws = new WebSocket(websocketUri("auth-web"))
    ws.binaryType = "arraybuffer"
    ws.onopen = (e: Event) ⇒ {
      dom.console.info("Connection for client authorization opened")
    }
    ws.onerror = (e: ErrorEvent) ⇒ {
      dom.console.error(s"Couldn't create connection to server: ${JSON.stringify(e)}")
    }
    ws.onmessage = (e: MessageEvent) ⇒ {
      import boopickle.Default._
      val bytes = toByteBuffer(e.data)
      Unpickle[Response].fromBytes(bytes) match {
        case AuthorizationGranted(id) ⇒
          dom.console.info(s"Server assigned id `$id`.")
          this.id = id

          import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
          Future(setupWS())

        case msg ⇒
          dom.console.error(s"Unexpected message arrived: $msg")
      }
      ws.close()
      dom.console.info("Connection for client authorization closed")
    }
  }

  def setupWS() = {
    ws = new WebSocket(websocketUri(s"kbws?id=$id"))
    ws.binaryType = "arraybuffer"
    ws.onopen = (e: Event) ⇒ {
      dom.console.info("Connection for server communication opened")
    }
    ws.onerror = (e: ErrorEvent) ⇒ {
      dom.console.error(s"Couldn't create connection to server: ${JSON.stringify(e)}")
    }
    ws.onmessage = (e: MessageEvent) ⇒ {
      import boopickle.Default._
      val bytes = toByteBuffer(e.data)
      handleResponse(Unpickle[Response].fromBytes(bytes))
    }
    ws.onclose = (e: CloseEvent) ⇒ {
      val reason = if (e.reason.isEmpty) "" else s" Reason: ${e.reason}"
      dom.console.info(s"Connection for server communication closed.$reason")
      ws = null
    }
  }

  def handleResponse(response: Response) = response match {
    case ConnectionSuccessful ⇒
      dom.console.info(s"Connection to server established. Communication is now possible.")
    case msg ⇒
      dom.console.error(s"Unexpected message arrived: $msg")
  }

  private def websocketUri(path: String): String = {
    val wsProtocol = if (dom.document.location.protocol == "https:") "wss" else "ws"
    s"$wsProtocol://$ServerAddress/$path"
  }

  private def toByteBuffer(data: Any): ByteBuffer = {
    val ab = data.asInstanceOf[js.typedarray.ArrayBuffer]
    js.typedarray.TypedArrayBuffer.wrap(ab)
  }
}
