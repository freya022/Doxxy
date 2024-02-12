import com.sun.source.doctree.*;
import com.sun.source.util.DocTreeScanner;
import com.sun.source.util.DocTrees;
import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementScanner9;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import java.io.PrintStream;
import java.util.Locale;
import java.util.Set;

/**
 * A doclet to demonstrate the use of {@link ElementScanner9}
 * <br>and {@link DocTreeScanner}.
 *
 * @version 1.0
 * @author Duke
 */
public class DocletTest implements Doclet {
	private static final boolean OK = true;

	private Reporter reporter;
	private DocTrees treeUtils;
	private Elements elementUtils;

	@Override
	public void init(Locale locale, Reporter reporter) {
		this.reporter = reporter;

		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String getName() {
		return getClass().getSimpleName();
	}

	@Override
	public Set<? extends Option> getSupportedOptions() {
		return Set.of();
	}

	@Override
	public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.latest();
	}

	@Override
	public boolean run(DocletEnvironment environment) {
		environment.getSpecifiedElements().forEach(e ->
				reporter.print(Diagnostic.Kind.NOTE,
						e,
						e.getKind().toString()));

		treeUtils = environment.getDocTrees();
		elementUtils = environment.getElementUtils();
		new ShowElements(System.out).scan(environment.getIncludedElements(), 0);

		return OK;
	}

	/**
	 * A scanner to display the structure of a series of elements
	 * and their documentation comments.
	 */
	class ShowElements extends ElementScanner9<Void, Integer> {
		private final PrintStream out;

		public ShowElements(PrintStream out) {
			this.out = out;
		}

		@Override
		public Void visitExecutable(ExecutableElement e, Integer depth) {
			DocCommentTree dcTree = treeUtils.getDocCommentTree(e);
			String indent = "  ".repeat(depth);
			if (dcTree != null) {
				out.println(indent + "| " + e.getKind() + " " + e);

				new ShowDocTrees(out).scan(dcTree, depth + 1);
			}

			return null;
		}

		@Override
		public Void visitType(TypeElement e, Integer depth) {
			DocCommentTree dcTree = treeUtils.getDocCommentTree(e);
			String indent = "  ".repeat(depth);
			if (dcTree != null) {
				out.println(indent + "| " + e.getKind() + " " + e);

				new ShowDocTrees(out).scan(dcTree, depth + 1);
			}

			return null;
		}

		@Override
		public Void scan(Element e, Integer depth) {
//			if (e.getKind() == ElementKind.CLASS) {
//				System.out.println("e.getSimpleName() = " + e);
//			}
//
//			DocCommentTree dcTree = treeUtils.getDocCommentTree(e);
//			String indent = "  ".repeat(depth);
//			if (dcTree != null) {
//				out.println(indent + "| " + e.getKind() + " " + e);
//
//				if (e.getKind() == ElementKind.CLASS) {
//					new ShowDocTrees(out).scan(dcTree, depth + 1);
//				} else if (e.getKind() == ElementKind.METHOD) {
//
//				}
//			}

			return super.scan(e, depth + 1);
		}
	}

	/**
	 * A scanner to display the structure of a documentation comment.
	 *
	 * {@link DocletTest mah string <b>bold</b>}
	 */
	class ShowDocTrees extends DocTreeScanner<Void, Integer> {
		private final PrintStream out;
		private final ClassDoclet doc = new ClassDoclet();

		//Make more ShowDocTrees classes when we need to fill methods / other than class

		private boolean hadNewLine = false;

		private void newLine(boolean force) {
			if (!hadNewLine || force) {
				out.println();

				hadNewLine = true;
			}
		}

		private void newLine() {
			newLine(true);
		}

		private void print(Object o) {
			out.print(o);

			hadNewLine = false;
		}

		public ShowDocTrees(PrintStream out) {
			this.out = out;
		}

		@Override
		public Void visitText(TextTree node, Integer integer) {
			print(node.getBody().replace("\n", ""));

			return null;
		}

		@Override
		public Void visitAuthor(AuthorTree node, Integer integer) {
			newLine(false);

			print("Author: ");

			scan(node.getName(), integer + 1);

			newLine();

			return null;
		}

		@Override
		public Void visitVersion(VersionTree node, Integer integer) {
			newLine(false);
			print("\nVersion: ");

			scan(node.getBody(), integer + 1);

			newLine();

			return null;
		}

		@Override
		public Void visitLink(LinkTree node, Integer integer) {
			print(node.getReference().getSignature());

			scan(node.getLabel(), integer + 1);

			return null;
		}

		@Override
		public Void visitStartElement(StartElementTree node, Integer integer) {
			switch (node.getName().toString()) {
				case "br" -> newLine();
				case "b" -> print("**");
			}

			return super.scan(node.getAttributes(), integer + 1);
		}

		@Override
		public Void visitEndElement(EndElementTree node, Integer integer) {
			switch (node.getName().toString()) {
				case "br" -> newLine();
				case "b" -> print("**");
			}

			return null;
		}
	}
}
