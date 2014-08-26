/***********************************************************************
 * Copyright (c) 2014 Cambridge Semantics Incorporated.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Cambridge Semantics Incorporated - initial API and implementation
 ***********************************************************************/

package trigeditor.editors;

import java.awt.HeadlessException;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.TreeSet;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.openrdf.model.Statement;
import org.openrdf.rio.ParseErrorListener;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.helpers.RDFHandlerBase;
import org.openrdf.rio.trig.TriGParser;
import org.openrdf.rio.trig.TriGWriter;

import trigeditor.Activator;

/**
 * TriGEditor is a child class of TextEditor. It can be ran as an external
 * Eclipse application and used to edit TriG files. Main features include
 * validation, syntax highlighting, and prefix refactoring.
 * 
 * @author Natasha
 *
 */
public class TriGEditor extends TextEditor {

	private InputStream in;
	TriGParser t = new TriGParser();
	IEditorInput editorInput;
	private ColorManager colorManager;
	private PrefixRefactor refactorer;
	private String filePathSystem;
	private String docGet = "empty";
	private IWorkbenchPage pg;

	/**
	 * Constructor of the TriGEditor.
	 * 
	 * Initializes any variables necessary, setting the ColorManager,
	 * SourceViewer, EditorInput, and PrefixRefactorer.
	 * 
	 * @throws CoreException
	 */
	public TriGEditor() throws CoreException {
		super();
		colorManager = new ColorManager();
		setSourceViewerConfiguration(new TriGSourceViewerConfiguration(colorManager));

		editorInput = getEditorInput();
		if (editorInput != null) {
			if (editorInput instanceof IStorageEditorInput) {
				setDocumentProvider(new TriGDocumentProvider());
			} else if (editorInput instanceof IURIEditorInput) {
				setDocumentProvider(new TriGTextFileDocumentProvider());

			} else {
				setDocumentProvider(new TriGDocumentProvider());
			}
			super.doSetInput(editorInput);
		}

		refactorer = new PrefixRefactor();
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		super.init(site, input);
		if (input != null) {
			if (input instanceof IStorageEditorInput) {
				setDocumentProvider(new TriGDocumentProvider());
			} else if (input instanceof IURIEditorInput) {
				setDocumentProvider(new TriGTextFileDocumentProvider());

			} else {
				setDocumentProvider(new TriGDocumentProvider());
			}
			try{
				super.doSetInput(input);
			}catch(CoreException ce){
				ce.printStackTrace();
			}
		}
	}

	protected void createActions() {
		super.createActions();
	}

	public void dispose() {
		colorManager.dispose();
		super.dispose();
	}

	/**
	 * This function allows for the TriG file that is being edited to be saved.
	 * Any changes to the file will prompt the user to save the changes by
	 * adding an asterik by the file's name. In addition, before the file is
	 * saved, the file will be parsed for errors. If errors are found, a message
	 * dialog will appear with the type of error, line number, and reason for
	 * error.
	 * 
	 * Parsing is done by the openRDF TriG Parser.
	 * 
	 */
	protected void editorSaved() {
		super.editorSaved();
		IFileEditorInput input = (IFileEditorInput) getEditorInput();
		final IFile res = input.getFile();
		try {
			res.deleteMarkers(null, true, 1);
		} catch (CoreException e2) {
			e2.printStackTrace();
		}
		TriGParser t = new TriGParser();
		RDFHandler r = new RDFHandlerBase();
		t.setRDFHandler(r);

		try {
			setInputStream();
			t.setStopAtFirstError(false);
			t.setParseErrorListener(new ParseErrorListener() {

				@Override
				public void warning(String message, int row, int column) {
					IMarker m;
					try {
						m = res.createMarker("trigeditor.editors.trigMarker");
						m.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);
						if (column != -1) {
							m.setAttribute(IMarker.CHAR_START, column);
							m.setAttribute(IMarker.CHAR_END, column + 1);
						}
						m.setAttribute(IMarker.MESSAGE, message);
						m.setAttribute(IMarker.LINE_NUMBER, row);
					} catch (CoreException e2) {
						// TODO Auto-generated catch block
						e2.printStackTrace();
					}
				}

				@Override
				public void fatalError(String message, int row, int column) {
					IMarker m;
					try {
						m = res.createMarker("trigeditor.editors.trigMarker");
						m.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
						if (column != -1) {
							m.setAttribute(IMarker.CHAR_START, column);
							m.setAttribute(IMarker.CHAR_END, column + 1);
						}
						m.setAttribute(IMarker.MESSAGE, message);
						m.setAttribute(IMarker.LINE_NUMBER, row);
					} catch (CoreException e2) {
						// TODO Auto-generated catch block
						e2.printStackTrace();
					}

				}

				@Override
				public void error(String message, int row, int column) {
					IMarker m;
					try {
						m = res.createMarker("trigeditor.editors.trigMarker");
						m.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
						if (column != -1) {
							m.setAttribute(IMarker.CHAR_START, column);
							m.setAttribute(IMarker.CHAR_END, column + 1);
						}
						m.setAttribute(IMarker.MESSAGE, message);
						m.setAttribute(IMarker.LINE_NUMBER, row);
					} catch (CoreException e2) {
						// TODO Auto-generated catch block
						e2.printStackTrace();
					}

				}
			});
			t.parse(in, "");

		} catch (RDFParseException e) {
			try {
				// Status status = new Status(IStatus.ERROR,
				// Activator.PLUGIN_ID, "Parsing error occurred.\n" + "Line: " +
				// e.getLineNumber() + "\n" + e.getMessage(), e);
				// ErrorDialog.openError(this.getEditorSite().getShell(),
				// "Error", "Exception occured saving!", status);
			} catch (HeadlessException e1) {
				e1.printStackTrace();
			}

		} catch (RDFHandlerException e) {
			Status status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, "RDFHandler exception thrown.\n" + e.getMessage(), e);
			ErrorDialog.openError(this.getEditorSite().getShell(), "Error", "Exception occured saving!", status);
		} catch (IOException e) {
			Status status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Failed or interrupted I/O operation.\n" + e.getMessage(), e);
			ErrorDialog.openError(this.getEditorSite().getShell(), "Error", "Exception occured saving!", status);
		}
	}

	public void format() {
		IDocumentProvider provider=getDocumentProvider();
		  final  IDocument doc = provider.getDocument(getEditorInput());
		String contents = doc.get();
		final StringWriter writer = new StringWriter();
		TriGParser t = new TriGParser();
		RDFHandler r = new RDFHandlerBase();
		t.setRDFHandler(r);
		final TreeSet<Statement> statements = new TreeSet<>(new Comparator<Statement>() {
			@Override
			public int compare(Statement o1, Statement o2) {
				if (o1.getContext() != null && o2.getContext() == null) {
					return 1;
				} else if (o1.getContext() == null && o2.getContext() != null) {
					return -1;
				} else if (o1.getContext() != null && o2.getContext() != null) {
					int comp = o1.getContext().toString().compareTo(o2.getContext().toString());
					if (comp != 0) {
						return comp;
					}
				}
				if (o1.getSubject() != null && o2.getSubject() != null) {
					int comp = o1.getSubject().toString().compareTo(o2.getSubject().toString());
					if (comp != 0) {
						return comp;
					}
				}
				if (o1.getPredicate() != null && o2.getPredicate() != null) {
					int comp = o1.getPredicate().toString().compareTo(o2.getPredicate().toString());
					if (comp != 0) {
						return comp;
					}
				}
				if (o1.getObject() != null && o2.getObject() != null) {
					int comp = o1.getObject().toString().compareTo(o2.getObject().toString());
					if (comp != 0) {
						return comp;
					}
				}
				return 0;
			}
		});
		try (StringReader reader = new StringReader(contents);) {
			final TriGWriter tw = new TriGWriter(writer);
			t.setRDFHandler(new RDFHandler() {

				@Override
				public void startRDF() throws RDFHandlerException {
					tw.startRDF();
				}

				@Override
				public void handleStatement(Statement arg0) throws RDFHandlerException {
					statements.add(arg0);
				}

				@Override
				public void handleNamespace(String arg0, String arg1) throws RDFHandlerException {
					tw.handleNamespace(arg0, arg1);
				}

				@Override
				public void handleComment(String arg0) throws RDFHandlerException {

				}

				@Override
				public void endRDF() throws RDFHandlerException {

				}
			});
			t.parse(reader, "");
			for (Statement stmt : statements) {
				tw.handleStatement(stmt);
			}
			tw.endRDF();
			writer.flush();
			doc.set(writer.toString());
		} catch (RDFParseException e) {
			try {
				// Status status = new Status(IStatus.ERROR,
				// Activator.PLUGIN_ID, "Parsing error occurred.\n" + "Line: " +
				// e.getLineNumber() + "\n" + e.getMessage(), e);
				// ErrorDialog.openError(this.getEditorSite().getShell(),
				// "Error", "Exception occured saving!", status);
			} catch (HeadlessException e1) {
				e1.printStackTrace();
			}

		} catch (RDFHandlerException e) {
			Status status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, "RDFHandler exception thrown.\n" + e.getMessage(), e);
			ErrorDialog.openError(this.getEditorSite().getShell(), "Error", "Exception occured saving!", status);
		} catch (IOException e) {
			Status status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Failed or interrupted I/O operation.\n" + e.getMessage(), e);
			ErrorDialog.openError(this.getEditorSite().getShell(), "Error", "Exception occured saving!", status);
		}
	}

	protected void firePropertyChange(int property) {
		super.firePropertyChange(property);
	}

	/**
	 * This helper function sets the input stream of the file being edited, by
	 * accessing the page/view open in the editor.
	 * 
	 * @return InputStream representing the contents open in the eidtor
	 * @throws FileNotFoundException
	 */
	private InputStream setInputStream() throws FileNotFoundException {
		in = new ByteArrayInputStream("Results in Null".getBytes(StandardCharsets.UTF_8));

		IWorkbench wb = PlatformUI.getWorkbench();
		IWorkbenchWindow wbWindow = wb.getActiveWorkbenchWindow();
		IWorkbenchPage page = wbWindow.getActivePage();
		pg = page;

		if (page != null) {
			IEditorPart editor = page.getActiveEditor();
			if (editor != null) {
				IEditorInput input = editor.getEditorInput();

				// for files already in the workbench
				if (input instanceof FileEditorInput) {
					IFileEditorInput fileEdit = (IFileEditorInput) input;
					IFile file = fileEdit.getFile();
					String filePath = file.getLocation().toString();
					if (filePath != null) {
						filePathSystem = filePath;
						in = new FileInputStream(filePath);
					}
				}
				// for system files outside the workbench
				else {
					FileStoreEditorInput fileEdit = (FileStoreEditorInput) input;
					URI fileURI = fileEdit.getURI();
					String filePath = fileURI.getPath();
					if (filePath != null) {
						filePathSystem = filePath;
						in = new FileInputStream(filePath);
					}
				}
			}
		}
		return in;
	}

	/**
	 * Converts the contents of an inputstream to a String object.
	 * 
	 * @param input
	 *            representing the InputStream
	 * @return String representation of the inputstream.
	 * @throws IOException
	 */
	private String streamToString(InputStream input) throws IOException {
		InputStreamReader isr = new InputStreamReader(input);
		StringBuilder sb = new StringBuilder();
		BufferedReader br = new BufferedReader(isr);
		String s = br.readLine();
		while (s != null) {
			sb.append(s);
			s = br.readLine();
		}

		return sb.toString();
	}

	/**
	 * Performs the prefix refactoring action of the file. If file is external
	 * to workspace, changes will not be shown in editor, unless editor view
	 * changes first.
	 * 
	 * @throws IOException
	 * @throws CoreException
	 */
	public void doRefactoring() throws IOException, CoreException {
		setInputStream();
		refactorer.refactorPrefixes(streamToString(setInputStream()), filePathSystem);

		if (!(editorInput instanceof IStorageEditorInput)) {
			Status status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, "To see effects in editor, for files not in the workspace," + " switch tab and switch back.\nClick 'OKAY' in new window.", null);
			ErrorDialog.openError(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), "Error", "Exception occured doing refactor", status);
		}
	}

}
