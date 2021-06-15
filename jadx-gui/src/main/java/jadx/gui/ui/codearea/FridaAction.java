package jadx.gui.ui.codearea;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.stream.Collectors;
import java.awt.datatransfer.StringSelection;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import javax.swing.*;
import org.jetbrains.annotations.Nullable;
import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.VariableNode;
import jadx.core.dex.nodes.VariableNode.VarKind;
import jadx.gui.treemodel.JNode;
import jadx.gui.utils.NLS;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static javax.swing.KeyStroke.getKeyStroke;

public final class FridaAction extends JNodeMenuAction<JNode> {
    private static final Logger LOG = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
	private static final long serialVersionUID = 4692546569977976384L;
    private boolean is_initial = true;
    private transient JTextField renameField;
    private String method_name;
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
            if( node instanceof JMethod){;
                JMethod n = (JMethod) node;
                MethodNode method_node = n.getJavaMethod().getMethodNode();
                MethodInfo mi = method_node.getMethodInfo();
                method_name = mi.getName();
                if(method_name.equals("<init>")){
                    method_name = "$init";
                }
                String full_class_name = method_node.getParentClass().getFullName();
                String class_name = method_node.getParentClass().getShortName();
                
                LOG.info("node is jmethod");
                LOG.info( full_class_name + "." + method_name);
                
                // overload ?
                List<MethodNode> methods =  method_node.getParentClass().getMethods();

                List<MethodNode> filteredmethod =  methods.stream().filter(m -> m.getName().equals(method_name)).filter(m-> m.getArgTypes().size() == mi.getArgumentsTypes().size()).collect(Collectors.toList());
                StringBuilder sb = new StringBuilder();
                String overload_str = "";
                if(filteredmethod.size()>1){
                    List<ArgType> method_argss = mi.getArgumentsTypes();
                    for (ArgType argType : method_argss) {
                        sb.append("'"+argType.toString()+"', ");
                    }
                    if(sb.length() > 2){
                        sb.setLength(sb.length()-2);
                    }
                    overload_str = sb.toString();
                    
                }
                StringBuilder sb2 = new StringBuilder();
                String function_parameters = "";
                List<VariableNode> vl = method_node.getVars().stream().filter(v->v.getVarKind()== VarKind.ARG).collect(Collectors.toList());
                if(vl.size()>0){
                    
                    for (VariableNode var : vl) {
                        sb2.append(var.getName()+",");
                    }
                    if(sb2.length() > 1){
                        sb2.setLength(sb2.length()-1);
                    }
                    function_parameters = sb2.toString();
                }
                //   if so extract arguments
                // argument size 

                
                String func_defin = "";
                if(overload_str != ""){
                    func_defin = String.format("%s.%s.overload(%s).implementation",class_name,method_name,overload_str);
                }
                else{
                    func_defin = String.format("%s.%s.implementation",class_name,method_name);
                }
                String func_defin2 = "";
                if(function_parameters != ""){
                    func_defin2 = String.format("%s = function(%s){\n}",func_defin,function_parameters);
                }
                else{
                    func_defin2 = String.format("%s = function(){\n}",func_defin);
                }
                String var_defin = "";
                if(is_initial){
                    var_defin = String.format("var %s = Java.use(\"%s\")\n%s",class_name,full_class_name,func_defin2);
                }
                else{
                    var_defin = func_defin2;
                }
                LOG.info("frida code : " + var_defin);
                
                

                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

                StringSelection selection = new StringSelection(var_defin);
                clipboard.setContents(selection, selection);
            }
            else if (node instanceof JClass){
                LOG.info("node is jclass");
                JClass jc = (JClass) node;
                LOG.info(jc.getCls().getClassNode().getClassInfo().getFullName());
            }
            else{
                LOG.info("node is something else");
            }
			
		}
        is_initial = false;
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
