import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.model.ContentTypeRange
import akka.http.scaladsl.model.MediaType
import akka.http.scaladsl.model.MediaTypes.`application/json`
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshaller}
import akka.util.ByteString
import com.avsystem.commons.serialization.GenCodec
import com.avsystem.commons.serialization.json.{JsonStringInput, JsonStringOutput}

import scala.collection.immutable.Seq

object GenCodecSupport extends GenCodecSupport {

}

trait GenCodecSupport {

  def unmarshallerContentTypes: Seq[ContentTypeRange] =
    mediaTypes.map(ContentTypeRange.apply)

  def mediaTypes: Seq[MediaType.WithFixedCharset] =
    List(`application/json`)

  private val jsonStringUnmarshaller =
    Unmarshaller.byteStringUnmarshaller
      .forContentTypes(unmarshallerContentTypes: _*)
      .mapWithCharset {
        case (ByteString.empty, _) => throw Unmarshaller.NoContentException
        case (data, charset)       => data.decodeString(charset.nioCharset.name)
      }

  private val jsonStringMarshaller =
    Marshaller.oneOf(mediaTypes: _*)(Marshaller.stringMarshaller)

  /**
    * HTTP entity => `A`
    *
    * @tparam A type to decode
    * @return unmarshaller for `A`
    */
  implicit def unmarshaller[A: GenCodec]: FromEntityUnmarshaller[A] =
    jsonStringUnmarshaller.map(data => JsonStringInput.read[A](data))

  /**
    * `A` => HTTP entity
    *
    * @tparam A type to encode
    * @return marshaller for any `A` value
    */
  implicit def marshaller[A: GenCodec]: ToEntityMarshaller[A] =
    jsonStringMarshaller.compose(JsonStringOutput.write(_))
}