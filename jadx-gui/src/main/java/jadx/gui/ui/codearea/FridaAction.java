package jadx.gui.ui.codearea;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.*;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.VariableNode;
import jadx.core.dex.nodes.VariableNode.VarKind;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JMethod;
import jadx.gui.treemodel.JNode;
import jadx.gui.utils.NLS;

import static javax.swing.KeyStroke.getKeyStroke;

public final class FridaAction extends JNodeMenuAction<JNode> {
	private static final Logger LOG = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
	private static final long serialVersionUID = 4692546569977976384L;
	private boolean isInitial = true;
	private String methodName;

	public FridaAction(CodeArea codeArea) {

		super(NLS.str("popup.frida") + " (f)", codeArea);
		LOG.info("triggered meee");
		KeyStroke key = getKeyStroke(KeyEvent.VK_F, 0);
		codeArea.getInputMap().put(key, "trigger frida");
		codeArea.getActionMap().put("trigger frida", new AbstractAction() {
			@Override

			public void actionPerformed(ActionEvent e) {
				node = codeArea.getNodeUnderCaret();
				copyFridaCode();
			}
		});
	}

	private void copyFridaCode() {

		if (node != null) {
			if (node instanceof JMethod) {
				;
				JMethod n = (JMethod) node;
				MethodNode methodNode = n.getJavaMethod().getMethodNode();
				MethodInfo mi = methodNode.getMethodInfo();
				methodName = mi.getName();
				if (methodName.equals("<init>")) {
					methodName = "$init";
				}
				String fullClassName = methodNode.getParentClass().getFullName();
				String className = methodNode.getParentClass().getShortName();

				LOG.info("node is jmethod");
				LOG.info(fullClassName + "." + methodName);

				// overload ?
				List<MethodNode> methods = methodNode.getParentClass().getMethods();

				List<MethodNode> filteredmethod = methods.stream().filter(m -> m.getName().equals(methodName))
						.filter(m -> m.getArgTypes().size() == mi.getArgumentsTypes().size()).collect(Collectors.toList());
				StringBuilder sb = new StringBuilder();
				String overloadStr = "";
				if (filteredmethod.size() > 1) {
					List<ArgType> methodArgs = mi.getArgumentsTypes();
					for (ArgType argType : methodArgs) {
						sb.append("'" + argType.toString() + "', ");
					}
					if (sb.length() > 2) {
						sb.setLength(sb.length() - 2);
					}
					overloadStr = sb.toString();

				}
				StringBuilder sb2 = new StringBuilder();
				String functionParameters = "";
				List<VariableNode> vl =
						methodNode.getVars().stream().filter(v -> v.getVarKind() == VarKind.ARG).collect(Collectors.toList());
				if (vl.size() > 0) {

					for (VariableNode var : vl) {
						sb2.append(var.getName() + ",");
					}
					if (sb2.length() > 1) {
						sb2.setLength(sb2.length() - 1);
					}
					functionParameters = sb2.toString();
				}
				// if so extract arguments
				// argument size

				String funcDefin = "";
				if (overloadStr != "") {
					funcDefin = String.format("%s.%s.overload(%s).implementation", className, methodName, overloadStr);
				} else {
					funcDefin = String.format("%s.%s.implementation", className, methodName);
				}
				String funcDefin2 = "";
				if (functionParameters != "") {
					funcDefin2 = String.format("%s = function(%s){\n}", funcDefin, functionParameters);
				} else {
					funcDefin2 = String.format("%s = function(){\n}", funcDefin);
				}
				String varDefin = "";
				if (isInitial) {
					varDefin = String.format("var %s = Java.use(\"%s\")\n%s", className, fullClassName, funcDefin2);
				} else {
					varDefin = funcDefin2;
				}
				LOG.info("frida code : " + varDefin);

				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

				StringSelection selection = new StringSelection(varDefin);
				clipboard.setContents(selection, selection);
			} else if (node instanceof JClass) {
				LOG.info("node is jclass");
				JClass jc = (JClass) node;
				LOG.info(jc.getCls().getClassNode().getClassInfo().getFullName());
			} else {
				LOG.info("node is something else");
			}

		}
		isInitial = false;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		node = codeArea.getNodeUnderCaret();
		copyFridaCode();
	}

	@Nullable
	@Override
	public JNode getNodeByOffset(int offset) {
		return codeArea.getJNodeAtOffset(offset);
	}
}
