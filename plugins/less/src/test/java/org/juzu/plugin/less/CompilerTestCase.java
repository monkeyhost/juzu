package org.juzu.plugin.less;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.juzu.plugin.less.impl.lesser.Compilation;
import org.juzu.plugin.less.impl.lesser.Failure;
import org.juzu.plugin.less.impl.lesser.JSR223Context;
import org.juzu.plugin.less.impl.lesser.LessError;
import org.juzu.plugin.less.impl.lesser.Lesser;
import org.juzu.plugin.less.impl.lesser.Result;
import org.juzu.plugin.less.impl.lesser.URLLessContext;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
@RunWith(Parameterized.class)
public class CompilerTestCase
{

   @Parameterized.Parameters
   public static Collection<Object[]> configs() throws Exception
   {
      return Arrays.asList(new Object[][]{{new Lesser(new JSR223Context())}});
   }

   /** . */
   private Lesser lesser;

   public CompilerTestCase(Lesser lesser)
   {
      this.lesser = lesser;
   }

   @Test
   public void testSimple() throws Exception
   {
      URLLessContext context = new URLLessContext(CompilerTestCase.class.getClassLoader().getResource("lesser/test/"));
      Compilation compilation = (Compilation)lesser.compile(context, "simple.less");
      Assert.assertEquals(".class {\n" +
         "  width: 2;\n" +
         "}\n", compilation.getValue());

      //
      compilation = (Compilation)lesser.compile(context, "simple.less", true);
      Assert.assertEquals(".class{width:2;}\n", compilation.getValue());
   }

   @Test
   public void testFail() throws Exception
   {
      URLLessContext context = new URLLessContext(CompilerTestCase.class.getClassLoader().getResource("lesser/test/"));
      Failure ret = (Failure)lesser.compile(context, "fail.less");
      LinkedList<LessError> errors = ret.getErrors();
      Assert.assertEquals(1, errors.size());;
      LessError error = errors.get(0);
      Assert.assertEquals(1, error.line);
      Assert.assertEquals(8, error.column);
      Assert.assertEquals(8, error.index);
   }

   @Test
   public void testCannotResolveImport() throws Exception
   {
      URLLessContext context = new URLLessContext(CompilerTestCase.class.getClassLoader().getResource("lesser/test/"));
      Failure failure = (Failure)lesser.compile(context, "cannotresolveimport.less");
      LinkedList<LessError> errors = failure.getErrors();
      Assert.assertEquals(1, errors.size());
      LessError error = errors.get(0);
      Assert.assertEquals(1, error.line);
      Assert.assertEquals(4, error.column);
      Assert.assertEquals(4, error.index);
   }

   @Test
   public void testSeveralErrors() throws Exception
   {
      URLLessContext context = new URLLessContext(CompilerTestCase.class.getClassLoader().getResource("lesser/test/"));
      Failure failure = (Failure)lesser.compile(context, "severalerrors1.less");
      LinkedList<LessError> errors = failure.getErrors();
      Assert.assertEquals(2, errors.size());
   }

   @Test
   public void testBootstrap() throws Exception
   {
      URLLessContext context = new URLLessContext(CompilerTestCase.class.getClassLoader().getResource("lesser/bootstrap/"));
      long time = - System.currentTimeMillis();
      Compilation compilation = (Compilation)lesser.compile(context, "bootstrap.less");
      time += System.currentTimeMillis();
      Assert.assertNotNull(compilation);
      System.out.println("parsed in " + time + "ms");
   }

   @Test
   public void testImport() throws Exception
   {
      URLLessContext context = new URLLessContext(CompilerTestCase.class.getClassLoader().getResource("lesser/test/"));
      Compilation compilation = (Compilation)lesser.compile(context, "importer.less");
      Assert.assertEquals("a {\n" +
         "  width: 2px;\n" +
         "}\n", compilation.getValue());
   }
}
