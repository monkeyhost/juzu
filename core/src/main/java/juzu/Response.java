/*
 * Copyright 2013 eXo Platform SAS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package juzu;

import juzu.impl.common.Formatting;
import juzu.io.ChunkBuffer;
import juzu.io.Stream;
import juzu.io.Streamable;
import juzu.io.UndeclaredIOException;
import juzu.io.Chunk;
import juzu.request.Dispatch;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.AbstractMap;

/**
 * <p>A response object signalling to the framework the action to take after an interaction. This object is usually
 * returned after the invocation of a controller method and instructs the action to perform.</p>
 *
 * <h2>Action response</h2>
 *
 * <h3>Redirection response</h3>
 * <p>A <code>Response.Process.Action.Redirect</code> response instructs Juzu to make a redirection to a valid
 * URL after the interaction, this kind of response is created using the factory method {@link Response#redirect(String)}:
 * <code><pre>
 *    return Response.redirect("http://www.exoplatform.org");
 * </pre></code>
 * </p>
 *
 * <h3>Proceed to render phase</h3>
 * <p>A <code>Response.View</code> response instructs Juzu to proceed to the render phase of a valid view
 * controller, this kind of response can be created using an {@link juzu.request.ActionContext}, however the best
 * way is to use a controller companion class that carries method factories for creating render responses.</p>
 *
 * <p>Type safe {@link juzu.Response.View} factory method are generated for each view or resource controller
 * methods. The signature of an render factory is obtained by using the same signature of the controller method.</p> <p/>
 *
 * <code><pre>
 *    public class MyController {
 *
 *       &#064;Action
 *       public {@link juzu.Response.View} myAction() {
 *          return MyController_.myView("hello");
 *       }
 *
 *       &#064;View
 *       public void myView(String param) {
 *       }
 *    }
 * </pre></code>
 *
 * <h2>Mime response</h2>
 *
 * <p>Mime response are used by the {@link juzu.request.Phase#VIEW} and the {@link juzu.request.Phase#RESOURCE} phases.
 * Both contains a content to be streamed to the client but still they have some noticeable differences.<p/>
 *
 * <p>The {@link juzu.Response.Content} class is the base response class which will work well for the two phases.
 * However the {@link juzu.request.Phase#VIEW} can specify an optional title and the {@link juzu.request.Phase#RESOURCE}
 * can specify an optional status code for the user agent response.</p>
 *
 * <p>Responses are created using the {@link Response} factory methods such as</p>
 *
 * <ul>
 *   <li>{@link Response#ok} creates an ok response</li>
 *   <li>{@link Response#notFound} creates a not found response</li>
 * </ul>
 *
 * <p>Response can also created from {@link juzu.template.Template} directly:</p>
 *
 * <code><pre>
 *    public class MyController {
 *
 *       &#064;Inject &#064;Path("index.gtmpl") {@link juzu.template.Template} index;
 *
 *       &#064;View
 *       public {@link juzu.Response.Content} myView() {
 *          return index.ok();
 *       }
 *
 *       &#064;Inject &#064;Path("error.gtmpl")  {@link juzu.template.Template} error;
 *
 *       &#064;Resource
 *       public {@link juzu.Response.Content} myView() {
 *          return error.notFound();
 *       }
 *    }
 * </pre></code>
 *
 * <p>The {@link juzu.template.Template.Builder} can also create responses:</p>
 *
 * <code><pre>
 *    public class MyController {
 *
 *       &#064;Inject &#064;Path("index.gtmpl") index index;
 *
 *       &#064;View
 *       public {@link juzu.Response.Content} myView() {
 *          return index.with().label("hello").ok();
 *       }
 *    }
 * </pre></code>
 *
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 */
public abstract class Response {

  /** . */
  protected final PropertyMap properties;

  protected Response() {
    this.properties = new PropertyMap();
  }

  protected Response(PropertyMap properties) {
    this.properties = properties;
  }

  /**
   * Set a property, if the value is null, the property is removed.
   *
   * @param propertyType the property type
   * @param propertyValue the property value
   * @throws NullPointerException if the property type is null
   */
  public <T> Response with(PropertyType<T> propertyType, T propertyValue) throws NullPointerException {
    if (propertyType == null) {
      throw new NullPointerException("No null property type allowed");
    }
    properties.addValue(propertyType, propertyValue);
    return this;
  }

  /**
   * Removes a property.
   *
   * @param propertyType the property type
   * @throws NullPointerException if the property type is null
   */
  public <T> Response without(PropertyType<T> propertyType) throws NullPointerException {
    return with(propertyType, null);
  }

  /**
   * Set a boolean property to true.
   *
   * @param propertyType the property type
   * @throws NullPointerException if the property type is null
   */
  public Response with(PropertyType<Boolean> propertyType) throws NullPointerException {
    return with(propertyType, true);
  }

  /**
   * Set a boolean property to false.
   *
   * @param propertyType the property type
   * @throws NullPointerException if the property type is null
   */
  public Response withNo(PropertyType<Boolean> propertyType) throws NullPointerException {
    return with(propertyType, false);
  }

  public final PropertyMap getProperties() {
    return properties;
  }

  public Response withHeader(String name, String... value) {
    return with(PropertyType.HEADER, new AbstractMap.SimpleEntry<String, String[]>(name, value));
  }

  /**
   * A response instructing to execute a render phase of a controller method after the current interaction.
   */
  public static abstract class View extends Response implements Dispatch {

    @Override
    public <T> View with(PropertyType<T> propertyType, T propertyValue) throws NullPointerException {
      return (View)super.with(propertyType, propertyValue);
    }

    @Override
    public View withHeader(String name, String... value) {
      return (View)super.withHeader(name, value);
    }

    @Override
    public <T> View without(PropertyType<T> propertyType) throws NullPointerException {
      return (View)super.without(propertyType);
    }

    @Override
    public View with(PropertyType<Boolean> propertyType) throws NullPointerException {
      return (View)super.with(propertyType);
    }

    @Override
    public View withNo(PropertyType<Boolean> propertyType) throws NullPointerException {
      return (View)super.withNo(propertyType);
    }

    public abstract boolean equals(Object obj);

  }

  /**
   * A response instructing to execute an HTTP redirection after the current interaction.
   */
  public static class Redirect extends Response {

    /** . */
    private final String location;

    public Redirect(String location) {
      this.location = location;
    }

    public String getLocation() {
      return location;
    }

    @Override
    public <T> Redirect with(PropertyType<T> propertyType, T propertyValue) throws NullPointerException {
      return (Redirect)super.with(propertyType, propertyValue);
    }

    @Override
    public Redirect withHeader(String name, String... value) {
      return (Redirect)super.withHeader(name, value);
    }

    @Override
    public <T> Redirect without(PropertyType<T> propertyType) throws NullPointerException {
      return (Redirect)super.without(propertyType);
    }

    @Override
    public Redirect with(PropertyType<Boolean> propertyType) throws NullPointerException {
      return (Redirect)super.with(propertyType);
    }

    @Override
    public Redirect withNo(PropertyType<Boolean> propertyType) throws NullPointerException {
      return (Redirect)super.withNo(propertyType);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj instanceof Redirect) {
        Redirect that = (Redirect)obj;
        return location.equals(that.location);
      }
      return false;
    }

    @Override
    public String toString() {
      return "Response.Redirect[location" + location + "]";
    }
  }

  public static class Status extends Response {

    /** . */
    private int code;

    public Status(int code) {
      this.code = code;
    }

    public Status(int code, PropertyMap properties) {
      super(properties);

      //
      this.code = code;
    }

    public final int getCode() {
      return code;
    }

    public Body body(Streamable s) {
      return new Body(code, properties, s);
    }

    public Body body(ChunkBuffer s) {
      return new Body(code, properties, s);
    }

    public Body body(CharSequence s) {
      return body(new ChunkBuffer().append(Chunk.create(s)).close());
    }

    public Body body(byte[] s) {
      return body(new ChunkBuffer().append(Chunk.create(s)).close());
    }

    public Body body(java.io.InputStream s) {
      return body(new ChunkBuffer().append(Chunk.create(s)).close());
    }

    public Content content(Streamable s) {
      return new Content(code, properties, s);
    }

    public Content content(CharSequence s) {
      return content(new ChunkBuffer().append(Chunk.create(s)).close());
    }

    public Content content(byte[] s) {
      return content(new ChunkBuffer().append(Chunk.create(s)).close());
    }

    public Content content(java.io.InputStream s) {
      return content(new ChunkBuffer().append(Chunk.create(s)).close());
    }

    @Override
    public Status withHeader(String name, String... value) {
      return (Status)super.withHeader(name, value);
    }

    @Override
    public Status withNo(PropertyType<Boolean> propertyType) throws NullPointerException {
      return (Status)super.withNo(propertyType);
    }

    @Override
    public Status with(PropertyType<Boolean> propertyType) throws NullPointerException {
      return (Status)super.with(propertyType);
    }

    @Override
    public <T> Status without(PropertyType<T> propertyType) throws NullPointerException {
      return (Status)super.without(propertyType);
    }

    @Override
    public <T> Status with(PropertyType<T> propertyType, T propertyValue) throws NullPointerException {
      return (Status)super.with(propertyType, propertyValue);
    }

    public Streamable streamable() {
      return new Streamable() {
        public void send(Stream stream) throws IllegalStateException {

          // Send properties
          for (PropertyType<?> propertyType : properties) {
            Iterable<?> values = properties.getValues(propertyType);
            if (values != null) {
              for (Object o : values) {
                stream.provide(new Chunk.Property(o, propertyType));
              }
            }
          }

          // Send real stream
          if (Status.this instanceof Response.Body) {
            ((Response.Body)Status.this).getData().send(stream);
          } else {
            stream.close(null);
          }
        }
      };
    }
  }

  public static class Body extends Status {

    /** . */
    private Streamable data;

    protected Body(int status, PropertyMap properties) {
      super(status, properties);

      //
      this.data = null;
    }

    protected Body(int status, Streamable data) {
      super(status);

      //
      this.data = data;
    }

    protected Body(int status, PropertyMap properties, Streamable data) {
      super(status, properties);

      //
      this.data = data;
    }

    public Streamable getData() {
      return data;
    }

    public String getMimeType() {
      return properties.getValue(PropertyType.MIME_TYPE);
    }

    public Charset getCharset() {
      return properties.getValue(PropertyType.ENCODING);
    }

    public Body withCharset(Charset charset) {
      return with(PropertyType.ENCODING, charset);
    }

    public Body withMimeType(String mimeType) {
      return with(PropertyType.MIME_TYPE, mimeType);
    }

    @Override
    public Body withHeader(String name, String... value) {
      return (Body)super.withHeader(name, value);
    }

    @Override
    public <T> Body with(PropertyType<T> propertyType, T propertyValue) throws NullPointerException {
      return (Body)super.with(propertyType, propertyValue);
    }

    @Override
    public <T> Body without(PropertyType<T> propertyType) throws NullPointerException {
      return (Body)super.without(propertyType);
    }

    @Override
    public Body with(PropertyType<Boolean> propertyType) throws NullPointerException {
      return (Body)super.with(propertyType);
    }

    @Override
    public Body withNo(PropertyType<Boolean> propertyType) throws NullPointerException {
      return (Body)super.withNo(propertyType);
    }
  }

  public static class Content extends Body {

    public Content(int status, PropertyMap properties, Streamable streamable) {
      super(status, properties, streamable);
    }

    public Content(int status, Streamable streamable) {
      super(status, streamable);
    }

    public Content(PropertyMap properties, Streamable streamable) {
      super(200, properties, streamable);
    }

    public Content(Streamable streamable) {
      super(200, streamable);
    }

    @Override
    public <T> Content with(PropertyType<T> propertyType, T propertyValue) throws NullPointerException {
      return (Content)super.with(propertyType, propertyValue);
    }

    @Override
    public <T> Content without(PropertyType<T> propertyType) throws NullPointerException {
      return (Content)super.without(propertyType);
    }

    @Override
    public Content with(PropertyType<Boolean> propertyType) throws NullPointerException {
      return (Content)super.with(propertyType);
    }

    @Override
    public Content withNo(PropertyType<Boolean> propertyType) throws NullPointerException {
      return (Content)super.withNo(propertyType);
    }

    @Override
    public Content withMimeType(String mimeType) {
      return (Content)super.withMimeType(mimeType);
    }

    @Override
    public Content withCharset(Charset charset) {
      return (Content)super.withCharset(charset);
    }

    @Override
    public Content withHeader(String name, String... value) {
      return (Content)super.withHeader(name, value);
    }

    public String getTitle() {
      return properties.getValue(PropertyType.TITLE);
    }

    public Content withTitle(String title) {
      return with(PropertyType.TITLE, title);
    }

    public Content withAssets(String... assets) throws NullPointerException {
      if (assets == null) {
        throw new NullPointerException("No null asset accepted");
      }
      for (String asset : assets) {
        with(PropertyType.ASSET, asset);
      }
      return this;
    }

    public Content withMetaTag(String name, String value) {
      with(PropertyType.META_TAG, new AbstractMap.SimpleEntry<String, String>(name, value));
      return this;
    }

    public Content withMetaHttpEquiv(String name, String value) {
      with(PropertyType.META_HTTP_EQUIV, new AbstractMap.SimpleEntry<String, String>(name, value));
      return this;
    }

    /**
     * Parse the header into an {@link Element} and set it on the response as an header tag. This method
     * expects well formed XML, the parsed Element will be translated into markup according to the
     * response content type when the response will be written to the document.
     *
     * @param header the header string to parse
     * @return this object
     * @throws ParserConfigurationException any ParserConfigurationException
     * @throws SAXException any SAXException
     */
    public Content withHeaderTag(String header) throws ParserConfigurationException, SAXException {
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = dbf.newDocumentBuilder();
      try {
        Document doc = builder.parse(new InputSource(new StringReader(header)));
        Element elt =  doc.getDocumentElement();
        return withHeaderTag(elt);
      }
      catch (IOException e) {
        // Let's save user from IOException at least
        throw new UndeclaredIOException(e);
      }
    }

    /**
     * Set the provided element on the response as an HTML header.
     *
     * @param header the header
     * @return this object
     */
    public Content withHeaderTag(Element header) {
      return with(PropertyType.HEADER_TAG, header);
    }

    @Override
    public String toString() {
      return "Response.Content[]";
    }
  }

  public static class Error extends Response {

    /** . */
    private final Throwable cause;

    /** . */
    private final String message;

    public Error(Throwable cause) {
      this(null, cause);
    }

    public Error(String message) {
      this(message, null);
    }

    private Error(String message, Throwable cause) {
      this.cause = cause;
      this.message = message;
    }

    public Throwable getCause() {
      return cause;
    }

    public String getMessage() {
      return message;
    }

    /**
     * @return the HTML formatted message, the default implementation returns the raw message
     */
    public String getHtmlMessage() {
      return message;
    }

    protected int getStatus() {
      return 500;
    }

    public Status asStatus(boolean verbose) {
      Response.Status response = Response.status(getStatus());
      if (verbose) {
        StringBuilder buffer = new StringBuilder();
        Formatting.renderStyleSheet(buffer);
        buffer.append("<div class=\"juzu\">");
        buffer.append("<h1>Oups something went wrong</h1>");
        // Use getCause as it can be overriden with subclasses
        Throwable c = getCause();
        if (c != null) {
          Formatting.renderThrowable(null, buffer, c);
        } else {
          // Use getMessage as it can be overriden with subclasses
          String m = getHtmlMessage();
          buffer.append(m);
        }
        buffer.append("</div>");
        response = response.content(buffer).withMimeType("text/html");
      }
      return response;
    }

    @Override
    public String toString() {
      return "Response.Error[" + (cause != null ? cause.getMessage() : "") + "]";
    }

    /**
     * A specific subclass for forbidden access.
     */
    public static class Forbidden extends Error {
      public Forbidden(Throwable cause) {
        super(cause);
      }

      public Forbidden(String message) {
        super(message);
      }

      public Forbidden(String message, Throwable cause) {
        super(message, cause);
      }

      @Override
      protected int getStatus() {
        return 403;
      }
    }
  }

  public static Response.Redirect redirect(String location) {
    return new Response.Redirect(location);
  }

  public static Status status(int code) {
    return new Status(code);
  }

  public static Status ok() {
    return status(200);
  }

  public static Status notFound() {
    return status(404);
  }

  public static Content ok(java.io.InputStream content) {
    return content(200, content);
  }

  public static Content ok(byte[] content) {
    return content(200, content);
  }

  public static Content ok(Readable content) {
    return content(200, content);
  }

  public static Content ok(CharSequence content) {
    return content(200, content);
  }

  public static Content notFound(byte[] content) {
    return content(404, content);
  }

  public static Content notFound(java.io.InputStream content) {
    return content(404, content);
  }

  public static Content notFound(Readable content) {
    return content(404, content);
  }

  public static Content notFound(CharSequence content) {
    return content(404, content);
  }

  public static Content content(int code, byte[] content) {
    return content(code, new ChunkBuffer().append(Chunk.create(content)).close());
  }

  public static Content content(int code, java.io.InputStream content) {
    return content(code, new ChunkBuffer().append(Chunk.create(content)).close());
  }

  public static Content content(int code, Readable content) {
    return content(code, new ChunkBuffer().append(Chunk.create(content)).close());
  }

  public static Content content(int code, CharSequence content) {
    return content(code, new ChunkBuffer().append(Chunk.create(content)).close());
  }

  public static Content content(int code, Streamable content) {
    return new Content(code, content);
  }

  public static Error error(Throwable t) {
    return new Error(t);
  }

  public static Error error(String msg) {
    return new Error(msg);
  }
}
