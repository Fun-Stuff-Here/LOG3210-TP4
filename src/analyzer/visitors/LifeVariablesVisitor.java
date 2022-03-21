package analyzer.visitors;

import analyzer.ast.*;

import java.io.PrintWriter;
import java.util.*;


/**
 * Created: 19-02-15
 * Last Changed: 19-10-20
 * Author: Félix Brunet & Doriane Olewicki
 * Modified by: Gérard Akkerhuis
 *
 * Description: Ce visiteur explore l'AST et génère un code intermédiaire.
 */

public class LifeVariablesVisitor implements ParserVisitor {

    //le m_writer est un Output_Stream connecter au fichier "result". c'est donc ce qui permet de print dans les fichiers
    //le code généré.
    private /*final*/ PrintWriter m_writer;

    public LifeVariablesVisitor(PrintWriter writer) { m_writer = writer; }

    /* UTIL POUR VARIABLES VIVES */
    public HashMap<String, StepStatus> allSteps = new HashMap<>();
    private HashSet<String> previous_step = new HashSet<>(); // dernier step qui est rencontré... sera la liste du/des STOP_NODE après parcours de tout l'arbre.

    /*Afin de pouvoir garder en memoire les variables a ajouter au REF*/
    private HashSet<String> current_ref_ids = new HashSet<>();

    private int step = 0;
    /*
    génère une nouvelle variable temporaire qu'il est possible de print
    À noté qu'il serait possible de rentrer en conflit avec un nom de variable définit dans le programme.
    Par simplicité, dans ce tp, nous ne concidérerons pas cette possibilité, mais il faudrait un générateur de nom de
    variable beaucoup plus robuste dans un vrai compilateur.
     */
    private String genStep() {
        return "_step" + step++;
    }

    @Override
    public Object visit(SimpleNode node, Object data) {
        return data;
    }

    @Override
    public Object visit(ASTProgram node, Object data)  {
        node.childrenAccept(this, data);
        compute_IN_OUT();

        // Impression déjà implémentée ici, vous pouvez changer cela si vous n'utilisez pas allSteps.
        for (int i = 0; i < step; i++) {
            m_writer.write("===== STEP " + i + " ===== \n" + allSteps.get("_step" + i).toString());
        }
        return null;
    }

    /*
    Code fournis pour remplir la table de symbole.
    Les déclarations ne sont plus utile dans le code à trois adresse.
    elle ne sont donc pas concervé.
     */
    @Override
    public Object visit(ASTDeclaration node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTBlock node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTStmt node, Object data) {
        // TODO: Définition des statements, cette fonction est importante pour l'identification des "step".
        StepStatus status = new StepStatus();
        String step_id = genStep();
        this.allSteps.put(step_id,status);
        this.add_pred(step_id);
        this.add_succ(step_id);

        this.previous_step.clear();
        this.previous_step.add(step_id);
        node.childrenAccept(this, step_id);
        return null;
    }

    /*
    le If Stmt doit vérifier s'il à trois enfants pour savoir s'il s'agit d'un "if-then" ou d'un "if-then-else".
     */
    @Override
    public Object visit(ASTIfStmt node, Object data) {
        // TODO: Cas IfStmt.
        //  Attention au cas de "if cond stmt" (sans else) qui est la difficulté ici...
        String if_start = (String) data;

        this.visit_child_for_def_ref(node,data,0,false);
        HashSet<String> true_path = this.visit_child_with_previous(node,data,if_start,1);

        if(node.jjtGetNumChildren()<=2) this.previous_step.add(if_start);
        else{
            HashSet<String> false_path = this.visit_child_with_previous(node,data,if_start,2);
            this.previous_step.addAll(false_path);
        }

        this.previous_step.addAll(true_path);
        return null;
    }


    @Override
    public Object visit(ASTWhileStmt node, Object data) {
        // TODO: Cas WhileStmt.
        //  Attention au cas de la condition qui est la difficulté ici...
        String while_start = (String) data;
        this.visit_child_with_previous(node,data,while_start,1);
        this.add_succ(while_start);
        this.add_pred(while_start);
        this.visit_child_for_def_ref(node,data,0,false);
        this.previous_step.clear();
        this.previous_step.add(while_start);
        return null;
    }


    @Override
    public Object visit(ASTAssignStmt node, Object data) {
        // TODO: vous avez le cas "DEF" ici... conseil: c'est ici qu'il faut faire ça ;)
        this.visit_child_for_def_ref(node,data,0,true);
        this.visit_child_for_def_ref(node,data,1,false);
       return null;
    }



    @Override
    public Object visit(ASTExpr node, Object data){
        return node.jjtGetChild(0).jjtAccept(this, data);
    }

    @Override
    public Object visit(ASTAddExpr node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTMulExpr node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTUnaExpr node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTBoolExpr node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTCompExpr node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTNotExpr node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTGenValue node, Object data) {
        return node.jjtGetChild(0).jjtAccept(this, data);
    }

    @Override
    public Object visit(ASTBoolValue node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTIdentifier node, Object data) {
        // TODO: Ici on a accès au nom des variables
        this.current_ref_ids.add(node.getValue());
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTIntValue node, Object data) {
        return Integer.toString(node.getValue());
    }



    @Override
    public Object visit(ASTSwitchStmt node, Object data) {

        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTCaseStmt node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTDefaultStmt node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    /* UTILE POUR VARIABLES VIVES
     * Chaque Set représente un group utile pour l'algorithme.
     * Fonction "toString" utile pour l'impression finale de chaque step.
     */

    private class StepStatus {
        public HashSet<String> REF = new HashSet<String>();
        public HashSet<String> DEF = new HashSet<String>();
        public HashSet<String> IN  = new HashSet<String>();
        public HashSet<String> OUT = new HashSet<String>();

        public HashSet<String> SUCC  = new HashSet<String>();
        public HashSet<String> PRED  = new HashSet<String>();

        public String toString() {
            String buff = "";
            buff += "REF : " + set_ordered(REF) +"\n";
            buff += "DEF : " + set_ordered(DEF) +"\n";
            buff += "IN  : " + set_ordered(IN) +"\n";
            buff += "OUT : " + set_ordered(OUT) +"\n";

            buff += "SUCC: " + set_ordered(SUCC) +"\n";
            buff += "PRED: " + set_ordered(PRED) +"\n";
            buff += "\n";
            return buff;
        }

        public String set_ordered(HashSet<String> s) {
            List<String> list = new ArrayList<String>(s);
            Collections.sort(list);
            return list.toString();
        }
    }

    /*
     * Cette fonction devrait générer les champs IN et OUT.
     * C'est ici que vous appliquez l'algorithme de Variables Vives !
     *
     * Cfr. Algo du cours
     */
    private void compute_IN_OUT() {
        Stack<String> work_list = new Stack<>();
        work_list.addAll(previous_step);

        while (!work_list.empty()){
            String node = work_list.pop();
            StepStatus node_status = this.allSteps.get(node);
            for (String succ_node:node_status.SUCC) {
                StepStatus succ_node_status = this.allSteps.get(succ_node);
                node_status.OUT.addAll(succ_node_status.IN);
            }
            HashSet<String> OLD_IN = (HashSet<String>) node_status.IN.clone();
            node_status.IN = (HashSet<String>) node_status.OUT.clone();
            node_status.IN.removeAll(node_status.DEF);
            node_status.IN.addAll(node_status.REF);

            if(!node_status.IN.equals(OLD_IN)){
                for (String predNode: node_status.PRED)
                    work_list.push(predNode);
            }
        }
    }

    private void visit_child_for_def_ref(SimpleNode node, Object data, int child_index,boolean is_def){
        String step = (String) data;

        this.current_ref_ids.clear();
        node.jjtGetChild(child_index).jjtAccept(this,data);
        if (is_def) this.allSteps.get(step).DEF.addAll(this.current_ref_ids);
        else this.allSteps.get(step).REF.addAll(this.current_ref_ids);
        this.current_ref_ids.clear();
    }

    private void add_pred(String step_id){
        this.allSteps.get(step_id).PRED.addAll(this.previous_step);
    }

    private void add_succ(String step_id){
        for(String previous_step:this.previous_step) this.allSteps.get(previous_step).SUCC.add(step_id);
    }

    private HashSet<String> visit_child_with_previous(SimpleNode node, Object data, String previous_step, int child_index){
        this.previous_step.clear();
        this.previous_step.add(previous_step);
        node.jjtGetChild(child_index).jjtAccept(this, data);
        return (HashSet<String>) this.previous_step.clone();
    }


}
