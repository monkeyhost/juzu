package org.juzu.impl.request;

import junit.framework.TestCase;
import org.juzu.application.ApplicationDescriptor;
import org.juzu.application.Phase;
import org.juzu.impl.spi.fs.disk.DiskFileSystem;
import org.juzu.test.CompilerHelper;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
public class RenderMethodTestCase extends TestCase
{

   @Override
   protected void setUp() throws Exception
   {
      final File root = new File(System.getProperty("test.resources"));
      DiskFileSystem fs = new DiskFileSystem(root, "request", "render");

      //
      CompilerHelper<File> compiler = new CompilerHelper<File>(fs);
      compiler.assertCompile();
      aClass = compiler.assertClass("request.render.A");
      compiler.assertClass("request.render.A_");

      //
      Class<?> appClass = compiler.assertClass("request.render.RenderApplication");
      descriptor = (ApplicationDescriptor)appClass.getDeclaredField("DESCRIPTOR").get(null);
   }

   /** . */
   private Class<?> aClass;

   /** . */
   private ApplicationDescriptor descriptor;

   public void testNoArg() throws Exception
   {
      ControllerMethod cm = descriptor.getControllerMethod(aClass, "noArg");
      assertEquals("noArg", cm.getName());
      assertEquals(Phase.RENDER, cm.getPhase());
      assertEquals(Collections.<ControllerParameter>emptyList(), cm.getArgumentParameters());
   }

   public void testStringArg() throws Exception
   {
      ControllerMethod cm = descriptor.getControllerMethod(aClass, "oneArg", String.class);
      assertEquals("oneArg", cm.getName());
      assertEquals(Phase.RENDER, cm.getPhase());
      assertEquals(Arrays.asList(new ControllerParameter("foo")), cm.getArgumentParameters());
   }
}
