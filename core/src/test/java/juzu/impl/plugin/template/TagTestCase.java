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

package juzu.impl.plugin.template;

import juzu.impl.common.Name;
import juzu.impl.compiler.CompilationError;
import juzu.impl.compiler.ProcessingContext;
import juzu.impl.inject.spi.InjectorProvider;
import juzu.impl.plugin.template.metamodel.TemplateMetaModel;
import juzu.impl.tags.DecorateTag;
import juzu.impl.template.spi.EmitContext;
import juzu.impl.template.spi.SimpleProcessContext;
import juzu.impl.template.spi.juzu.ast.ASTNode;
import juzu.impl.template.spi.juzu.compiler.EmitPhase;
import juzu.impl.template.spi.juzu.compiler.ProcessPhase;
import juzu.impl.template.spi.TemplateModel;
import juzu.impl.plugin.template.metadata.TemplateDescriptor;
import juzu.impl.common.Path;
import juzu.template.TagHandler;
import juzu.test.AbstractInjectTestCase;
import juzu.test.CompilerAssert;
import juzu.test.protocol.mock.MockApplication;
import juzu.test.protocol.mock.MockClient;
import juzu.test.protocol.mock.MockViewBridge;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
public class TagTestCase extends AbstractInjectTestCase {

  public TagTestCase(InjectorProvider di) {
    super(di);
  }

  @Test
  public void testDecorate() throws Exception {
    MockApplication<?> app = application("plugin.template.tag.decorate").init();

    //
    MockClient client = app.client();
    MockViewBridge render = client.render();
    String out = render.assertStringResponse();
    assertEquals("<foo>bar</foo>", out);
  }

  @Test
  public void testDecorateNested() throws Exception {
    MockApplication<?> app = application("plugin.template.tag.decoratenested").init();

    //
    MockClient client = app.client();
    MockViewBridge render = client.render();
    String out = render.assertStringResponse();
    assertEquals("<juu><foo>bar</foo></juu>", out);
  }

  @Test
  public void testDecorateExternal() throws Exception {
    CompilerAssert<File, File> simpleHelper = compiler("plugin.template.tag.decorate");
    simpleHelper.assertCompile();
    CompilerAssert<File, File> helper  = compiler("plugin.template.tag.decorateexternal");
    helper.addClassPath(simpleHelper.getClassOutput());
    helper.assertCompile();
    MockApplication<?> app = new MockApplication<File>(
        helper,
        di,
        Name.parse("plugin.template.tag.decorateexternal"));
    app.init();
    MockClient client = app.client();
    assertEquals("<foo>bar</foo>", client.render().assertStringResponse());
  }

  @Test
  public void testInclude() throws Exception {
    MockApplication<?> app = application("plugin.template.tag.include").init();

    //
    MockClient client = app.client();
    MockViewBridge render = client.render();
    String out = render.assertStringResponse();
    assertEquals("foo", out);
  }

  @Test
  public void testIncludeDouble() throws Exception {
    MockApplication<?> app = application("plugin.template.tag.includedouble").init();

    //
    MockClient client = app.client();
    MockViewBridge render = client.render();
    String out = render.assertStringResponse();
    assertEquals("foobar", out);
  }

  @Test
  public void testIncludeTwice() throws Exception {
    MockApplication<?> app = application("plugin.template.tag.includetwice").init();

    //
    MockClient client = app.client();
    MockViewBridge render = client.render();
    String out = render.assertStringResponse();
    assertEquals("foofoo", out);
  }

  @Test
  public void testIncludeCircular() throws Exception {
    List<CompilationError> errors = compiler("plugin.template.tag.includecircular").formalErrorReporting(true).failCompile();
    assertEquals(1, errors.size());
    CompilationError error = errors.get(0);
    assertEquals(TemplateMetaModel.TEMPLATE_CYCLE, error.getCode());
    assertEquals(Arrays.asList(
        "Path[/plugin/template/tag/includecircular/templates/index.gtmpl]",
        "/plugin/template/tag/includecircular/templates/index.gtmpl->/plugin/template/tag/includecircular/templates/foo.gtmpl"),
        error.getArguments());
  }

  @Test
  public void testIncludeExternal() throws Exception {
    CompilerAssert<File, File> simpleHelper = compiler("plugin.template.simple");
    simpleHelper.assertCompile();
    CompilerAssert<File, File> helper  = compiler("plugin.template.tag.includeexternal");
    helper.addClassPath(simpleHelper.getClassOutput());
    helper.assertCompile();
    MockApplication<?> app = new MockApplication<File>(
        helper,
        di,
        Name.parse("plugin.template.tag.includeexternal"));
    app.init();
    MockClient client = app.client();
    assertEquals("hello", client.render().assertStringResponse());
  }

  @Test
  public void testTitle() throws Exception {
    MockApplication<?> app = application("plugin.template.tag.title").init();

    //
    MockClient client = app.client();
    MockViewBridge render = client.render();
    String url = render.assertStringResponse();
    assertEquals("the_title", render.getTitle());
    render = (MockViewBridge)client.invoke(url);
    assertEquals("4", render.getTitle());
  }

  @Test
  public void testParam() throws Exception {
    if (getDI() != InjectorProvider.GUICE) {
      MockApplication<?> app = application("plugin.template.tag.param").init();

      //
      MockClient client = app.client();
      MockViewBridge render = client.render();
      String content = render.assertStringResponse();
      assertEquals("foo_value", content);
    }
  }

  @Test
  public void testRecompileTemplate() throws Exception {
    MockApplication<?> app = application("plugin.template.tag.decorate").init();

    // Manufacture a template
    // to be removed later when we improve this
    // but for now it will be enough
    TemplateDescriptor desc = app.getLifeCycle().resolveBean(TemplatePlugin.class).getDescriptor().getTemplate("/plugin/template/tag/decorate/templates/index.gtmpl");
    assertNotNull(desc);
    TemplateModel<?> foo = new TemplateModel<ASTNode.Template>(
      new ASTNode.Template(),
      (Path.Absolute)Path.parse("/" + desc.getType().getName().replace('.', '/') + "/foo.gtmpl"),
      System.currentTimeMillis(),
      0);

    //
    HashMap<Path.Absolute, TemplateModel<?>> templates = new HashMap<Path.Absolute, TemplateModel<?>>();
    templates.put((Path.Absolute)Path.parse("/foo.gtmpl"), foo);
    ProcessPhase process = new ProcessPhase(new SimpleProcessContext(templates) {
      @Override
      public Path.Absolute resolveTemplate(Path path) {
        if (path.getCanonical().equals("index.gtmpl")) {
          return (Path.Absolute)Path.parse("/plugin/template/tag/decorate/templates/index.gtmpl");
        }
        else {
          return null;
        }
      }
    });
    Path.Absolute template = process.resolveTemplate(Path.parse("index.gtmpl"));
    assertNotNull(template);

    // Now emit the template
    EmitPhase emit = new EmitPhase(new EmitContext(){
      public TagHandler resolveTagHandler(String name) {
        if ("decorate".equals(name)) {
          return new DecorateTag();
        } else {
          return null;
        }
      }
      public void createResource(Path.Absolute path, CharSequence content) throws IOException {
        throw new UnsupportedOperationException();
      }
    });
  }

  @Test
  public void testNotFound() throws Exception {
    List<CompilationError> errors = compiler("plugin.template.tag.notfound").formalErrorReporting(true).failCompile();
    assertEquals(1, errors.size());
    CompilationError error = errors.get(0);
    assertTrue(error.getSource().endsWith("template/tag/notfound/A.java"));
    assertEquals(TemplateMetaModel.UNKNOWN_TAG, error.getCode());
    assertEquals(Arrays.asList("notfound"), error.getArguments());
  }

  @Test
  public void testSimpleRender() throws Exception {
    MockApplication<?> app = application("plugin.template.tag.simple.render").init();
    MockClient client = app.client();
    MockViewBridge render = client.render();
    String out = render.assertStringResponse();
    assertEquals("foothe_tagbar", out);
  }

  @Test
  public void testSimpleParameters() throws Exception {
    MockApplication<?> app = application("plugin.template.tag.simple.parameters").init();
    MockClient client = app.client();
    MockViewBridge render = client.render();
    String out = render.assertStringResponse();
    assertEquals("-[:]-[a:b]-", out);
  }

  @Test
  public void testSimpleBody() throws Exception {
    MockApplication<?> app = application("plugin.template.tag.simple.body").init();
    MockClient client = app.client();
    MockViewBridge render = client.render();
    String out = render.assertStringResponse();
    assertEquals("<foo>the_body</foo>", out);
  }

  @Test
  public void testSimpleNested() throws Exception {
    MockApplication<?> app = application("plugin.template.tag.simple.nested").init();
    MockClient client = app.client();
    MockViewBridge render = client.render();
    String out = render.assertStringResponse();
    assertEquals("<index><foo><bar>foo_content</bar></foo></index>", out);
  }

  @Test
  public void testSimpleInclude() throws Exception {
    MockApplication<?> app = application("plugin.template.tag.simple.include").init();
    MockClient client = app.client();
    MockViewBridge render = client.render();
    String out = render.assertStringResponse();
    assertEquals("pass", out);
  }

  @Test
  public void testSimpleTagReuse() throws Exception {
    CompilerAssert<File, File> simpleHelper = compiler("plugin.template.tag.simple.render");
    simpleHelper.assertCompile();
    File classes = simpleHelper.getClassOutput().getRoot();
    File services = new File(classes, "META-INF/services");
    assertTrue(services.mkdirs());
    File tagHandler = new File(services, "juzu.template.TagHandler");
    FileWriter writer = new FileWriter(tagHandler);
    writer.append("plugin.template.tag.simple.render.tags.foo").close();
    // Swap this classLoader for the test so we can load the tag at compile time
    ProcessingContext.baseCL = new URLClassLoader(new URL[]{classes.toURI().toURL()}, ProcessingContext.baseCL);
    try {
      CompilerAssert<File, File> helper  = compiler("plugin.template.tag.simple.reuse");
      helper.addClassPath(simpleHelper.getClassOutput());
      helper.assertCompile();
      MockApplication<?> app = new MockApplication<File>(
          helper,
          di,
          Name.parse("plugin.template.tag.simple.reuse"));
      app.init();
      MockClient client = app.client();
      MockViewBridge render = client.render();
      String out = render.assertStringResponse();
      assertEquals("foothe_tagbar", out);
    }
    finally {
      ProcessingContext.baseCL = ProcessingContext.baseCL.getParent();
    }
  }
}
