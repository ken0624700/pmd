/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang.java.rule.performance;

import java.util.Objects;

import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.lang.java.ast.ASTArgumentList;
import net.sourceforge.pmd.lang.java.ast.ASTAssignmentOperator;
import net.sourceforge.pmd.lang.java.ast.ASTConditionalExpression;
import net.sourceforge.pmd.lang.java.ast.ASTDoStatement;
import net.sourceforge.pmd.lang.java.ast.ASTEqualityExpression;
import net.sourceforge.pmd.lang.java.ast.ASTFieldDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTForStatement;
import net.sourceforge.pmd.lang.java.ast.ASTFormalParameter;
import net.sourceforge.pmd.lang.java.ast.ASTName;
import net.sourceforge.pmd.lang.java.ast.ASTPrimaryExpression;
import net.sourceforge.pmd.lang.java.ast.ASTStatementExpression;
import net.sourceforge.pmd.lang.java.ast.ASTVariableDeclaratorId;
import net.sourceforge.pmd.lang.java.ast.ASTWhileStatement;
import net.sourceforge.pmd.lang.java.rule.AbstractJavaRule;
import net.sourceforge.pmd.lang.java.typeresolution.TypeHelper;
import net.sourceforge.pmd.lang.symboltable.NameOccurrence;

public class UseStringBufferForStringAppendsRule extends AbstractJavaRule {

    public UseStringBufferForStringAppendsRule() {
        addRuleChainVisit(ASTVariableDeclaratorId.class);
    }

    @Override
    public Object visit(ASTVariableDeclaratorId node, Object data) {
        if (!TypeHelper.isA(node, String.class) || node.isArray()
                || node.getNthParent(3) instanceof ASTForStatement) {
            return data;
        }

        for (NameOccurrence no : node.getUsages()) {
            Node name = no.getLocation();
            ASTStatementExpression statement = name.getFirstParentOfType(ASTStatementExpression.class);
            if (statement == null) {
                continue;
            }
            ASTArgumentList argList = name.getFirstParentOfType(ASTArgumentList.class);
            if (argList != null && argList.getFirstParentOfType(ASTStatementExpression.class) == statement) {
                // used in method call
                continue;
            }
            ASTEqualityExpression equality = name.getFirstParentOfType(ASTEqualityExpression.class);
            if (equality != null && equality.getFirstParentOfType(ASTStatementExpression.class) == statement) {
                // used in condition
                continue;
            }
            if ((node.getNthParent(2) instanceof ASTFieldDeclaration
                    || node.getParent() instanceof ASTFormalParameter)
                    && isNotWithinLoop(name)) {
                // ignore if the field or formal parameter is *not* used within loops
                continue;
            }
            ASTConditionalExpression conditional = name.getFirstParentOfType(ASTConditionalExpression.class);

            if (conditional != null) {
                Node thirdParent = name.getNthParent(3);
                Node fourthParent = name.getNthParent(4);
                if ((Objects.equals(thirdParent, conditional) || Objects.equals(fourthParent, conditional))
                        && conditional.getFirstParentOfType(ASTStatementExpression.class) == statement) {
                    // is used in ternary as only option (not appended to other
                    // string)
                    continue;
                }
            }
            if (statement.getNumChildren() > 0 && statement.getChild(0) instanceof ASTPrimaryExpression) {
                ASTName astName = statement.getChild(0).getFirstDescendantOfType(ASTName.class);
                if (astName != null) {
                    if (astName.equals(name)) {
                        ASTAssignmentOperator assignmentOperator = statement
                                .getFirstDescendantOfType(ASTAssignmentOperator.class);
                        if (assignmentOperator != null && assignmentOperator.isCompound()) {
                            addViolation(data, assignmentOperator);
                        }
                    } else if (astName.getImage().equals(name.getImage())) {
                        ASTAssignmentOperator assignmentOperator = statement
                                .getFirstDescendantOfType(ASTAssignmentOperator.class);
                        if (assignmentOperator != null && !assignmentOperator.isCompound()) {
                            addViolation(data, astName);
                        }
                    }
                }
            }
        }
        return data;
    }

    private boolean isNotWithinLoop(Node name) {
        return name.getFirstParentOfType(ASTForStatement.class) == null
                && name.getFirstParentOfType(ASTWhileStatement.class) == null
                && name.getFirstParentOfType(ASTDoStatement.class) == null;
    }
}
