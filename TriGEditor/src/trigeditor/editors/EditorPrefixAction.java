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

import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;

/**
 * This class is responsible for adding the refactoring action to the toolbar for the TriGEditor.
 * 
 * @author Natasha
 *
 */
public class EditorPrefixAction implements IEditorActionDelegate{

	/**
	 * Creates a TriGEditor object in order to refactor prefixes when user performs the action
	 * by clicking the button in the editor's toolbar.
	 */
	@Override
	public void run(IAction arg0) {
		try {
			final IEditorPart editor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
			        .getActiveEditor();
			if(editor instanceof TriGEditor){
				((TriGEditor)editor).doRefactoring();
			}
			//t.firePropertyChange(IEditorPart.PROP_DIRTY);
		} catch (CoreException | IOException e) {
			e.printStackTrace();
		}
		
	}

	@Override
	public void selectionChanged(IAction arg0, ISelection arg1) {
		
	}

	@Override
	public void setActiveEditor(IAction arg0, IEditorPart arg1) {
		
	}

}
