package org.pchapin.phoebe

/** Define classes corresponding to the grammar rules.
  *
  * For each rule in Phoebe's grammar, as described in pcode-grammar.md, one
  * class may be defined here. For example, the <code>statement_list</code>
  * grammar rule corresponds to class <code>StatementListNode</code> here.
  *
  * All non-terminal nodes have a corresponding class defined here. Terminal
  * nodes only only have a class defined here if they need customization.
  */
object TreeNode {

  abstract class Node

  // statement_list
  abstract class StatementListNode extends Node
  case class StatementList(statement: StatementNode, statement_list_tail: StatementListTailNode)
    extends StatementListNode

  // statement_list_tail
  abstract class StatementListTailNode extends Node
  case class FullStatementListTail(statement: StatementNode, statement_list_tail: StatementListTailNode)
    extends StatementListTailNode
  case class NullStatementListTail()
    extends StatementListTailNode

  // statement
  abstract class StatementNode extends Node
  case class EPStatement(ep: EPNode)
    extends StatementNode
  case object ReturnStatement
    extends StatementNode
  case class IfStatement(conditional_expr: ConditionalExprNode, statement_list: StatementListNode)
    extends StatementNode
  case class IfElseStatement(conditional_expr: ConditionalExprNode, if_statement_list: StatementListNode, else_statement_list: StatementListNode)
    extends StatementNode
  case class WhileStatement(conditional_expr: ConditionalExprNode, statement_list: StatementListNode)
    extends StatementNode
  case class RepeatStatement(statement_list: StatementListNode, conditional_expr: ConditionalExprNode)
    extends StatementNode

  // conditional_expr
  abstract class ConditionalExprNode extends Node
  case class ConditionalExpr(and_expr: AndExprNode, conditional_expr_tail: ConditionalExprTailNode)
    extends ConditionalExprNode

  // conditional_expr_tail
  abstract class ConditionalExprTailNode extends Node
  case class FullConditionalExprTail(and_expr: AndExprNode, conditional_expr_tail: ConditionalExprTailNode)
    extends ConditionalExprTailNode
  case class NullConditionalExprTail()
    extends ConditionalExprTailNode

  // and_expr
  abstract class AndExprNode extends Node
  case class AndExpr(simple_expr: SimpleExprNode, and_expr_tail: AndExprTailNode)
    extends AndExprNode

  // and_expr_tail
  abstract class AndExprTailNode extends Node
  case class FullAndExprTail(simple_expr: SimpleExprNode, and_expr_tail: AndExprTailNode)
    extends AndExprTailNode
  case class NullAndExprTail()
    extends AndExprTailNode

  // simple_expr
  abstract class SimpleExprNode extends Node
  case class NotSimpleExpr(simple_expr: SimpleExprNode)
    extends SimpleExprNode
  case class ParenSimpleExpr(conditional_expr: ConditionalExprNode)
    extends SimpleExprNode
  case class EPSimpleExpr(ep: EPNode)
    extends SimpleExprNode

  // terminal grammar rules
  case class EPNode(text: String) extends Node
}

// vim:set ts=2 sw=2 et:
