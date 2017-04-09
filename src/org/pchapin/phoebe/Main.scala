package org.pchapin.phoebe

import org.antlr.v4.runtime._

case class ParseError(message: String) extends Exception(message)

object Main {

  /** Handle CLI invocation and execute business logic. */
  def main(args: Array[String]): Unit = {
    import scala.collection.JavaConverters._

    // Analyze the command line.
    if (args.length != 1) {
      println("Usage: java -jar Phoebe.jar source-file.pcd")
      System.exit(1)
    }

    // Create a stream that reads from the specified file.
    val input = new ANTLRFileStream(args(0))

    // Tokenize the input file.
    val lexer  = new PhoebeLexer(input)
    val tokens = new CommonTokenStream(lexer)
    tokens.fill()

    val tokenList = tokens.getTokens
    val scalaTokenList: List[Token] = tokenList.asScala.toList
    val parseTree = parse(scalaTokenList)
    println(PrettyPrint.pprint(parseTree))
  }

  /** Pop a token off the head of the given tokenList.
    *
    * Return tokenList, minus its head.
    *
    * Throw a parseError if tokenlist.head.getType doesn't equal tokenType.
    * tokenName is used when constructing the exception message. It should state
    * the name of tokenType. (This seems kludgy. There's probably a programmatic
    * way to get a token's name from its type.)
    */
  @throws(classOf[ParseError])
  def popToken(tokenList: List[Token], tokenType: Int, tokenName: String): List[Token] = {
    val token = tokenList.head
    if (token.getType != tokenType) {
      throw ParseError(
        s"(line ${token.getLine}, column ${token.getCharPositionInLine}) " +
        s"Expected token ${tokenName}, but got token ${token.getText}"
      )
    }
    tokenList.drop(1)
  }

  /** Parse the tokens in a pseudo-code file.
    *
    * TODO: Avoid declarations of tokenList2, tokenList3, etc.
    */
  def parse(tokenList: List[Token]): TreeNode.StatementListNode = {
    import TreeNode._

    /** Parse the statement_list at the head of the given token list. */
    def statementList(tokenList: List[Token]): (StatementListNode, List[Token]) = {
      val (myStatement, tokenList2) = statement(tokenList)
      val (myStatementListTail, tokenList3) = statementListTail(tokenList2)
      val myStatementList = StatementList(myStatement, myStatementListTail)
      (myStatementList, tokenList3)
    }

    /** Parse the statement_list_tail at the head of the given token list. */
    def statementListTail(tokenList: List[Token]): (StatementListTailNode, List[Token]) = {
      // The logic in this method boils down to the following:
      //
      //   If the next token is a statement:
      //     consume tokens and create a FullStatementListTail
      //   else:
      //     create a NullStatementListTail
      //
      // One way to accomplish this is to call `statement(tokenList)` and take
      // the first logical branch if it succeeds, or take the second logical
      // branch if it fails. This is nice because it encapsulates knowledge
      // about "statement" nodes inside the statement() method.
      //
      // The actual approach by taken by this method is kind of awful: we look
      // at the next token, decide whether it looks like the beginning of a
      // "statement" node, and choose a way forward based on this. This approach
      // is awful because knowledge about "statement" nodes is no longer
      // encapsulated inside the statement() method.
      val nodeType = tokenList.head.getType
      if (List(
        PhoebeLexer.EP,
        PhoebeLexer.IF,
        PhoebeLexer.REPEAT,
        PhoebeLexer.RETURN,
        PhoebeLexer.WHILE
      ).contains(nodeType)) {
        val (myStatement, tokenList2) = statement(tokenList)
        val (myStatementListTail, tokenList3) = statementListTail(tokenList2)
        val myFullStatementListTail = FullStatementListTail(myStatement, myStatementListTail)
        (myFullStatementListTail, tokenList3)
      } else {
        (NullStatementListTail(), tokenList)
      }
    }

    /** Parse the statement at the head of the given token list. */
    @throws(classOf[ParseError])
    def statement(tokenList: List[Token]): (StatementNode, List[Token]) = {
      // What kind of statement are we dealing with?
      tokenList.head.getType match {
        case PhoebeLexer.EP =>
          val myEPStatement = EPStatement(EPNode(tokenList.head.getText))
          val tokenList2 = popToken(tokenList, PhoebeLexer.EP, "EP")
          (myEPStatement, tokenList2)
        case PhoebeLexer.IF =>
          val tokenList2 = popToken(tokenList, PhoebeLexer.IF, "IF")
          val (myConditionalExpr, tokenList3) = conditionalExpr(tokenList2)
          val tokenList4 = popToken(tokenList3, PhoebeLexer.THEN, "THEN")
          val (myStatementList, tokenList5) = statementList(tokenList4)
          tokenList5.head.getType match {
            case PhoebeLexer.END =>
              val tokenList6 = popToken(tokenList5, PhoebeLexer.END, "END")
              val myIfStatement = IfStatement(myConditionalExpr, myStatementList)
              (myIfStatement, tokenList6)
            case PhoebeLexer.ELSE =>
              val tokenList6 = popToken(tokenList5, PhoebeLexer.ELSE, "ELSE")
              val (mySecondStatementList, tokenList7) = statementList(tokenList6)
              val tokenList8 = popToken(tokenList7, PhoebeLexer.END, "END")
              val myIfElseStatement = IfElseStatement(myConditionalExpr, myStatementList, mySecondStatementList)
              (myIfElseStatement, tokenList8)
            case _ =>
              throw ParseError(
                s"(line ${tokenList5.head.getLine}, column ${tokenList5.head.getCharPositionInLine}) " +
                s"Expected token END or ELSE, but got token ${tokenList5.head.getText}"
              )
          }
        case PhoebeLexer.REPEAT =>
          val tokenList2 = popToken(tokenList, PhoebeLexer.REPEAT, "REPEAT")
          val (myStatementList, tokenList3) = statementList(tokenList2)
          val tokenList4 = popToken(tokenList3, PhoebeLexer.UNTIL, "UNTIL")
          val (myConditionalExpr, tokenList5) = conditionalExpr(tokenList4)
          val myRepeatStatement = RepeatStatement(myStatementList, myConditionalExpr)
          (myRepeatStatement, tokenList5)
        case PhoebeLexer.RETURN =>
          val tokenList2 = popToken(tokenList, PhoebeLexer.RETURN, "RETURN")
          val myReturnStatement = ReturnStatement
          (myReturnStatement, tokenList2)
        case PhoebeLexer.WHILE =>
          val tokenList2 = popToken(tokenList, PhoebeLexer.WHILE, "WHILE")
          val (myConditionalExpr, tokenList3) = conditionalExpr(tokenList2)
          val tokenList4 = popToken(tokenList3, PhoebeLexer.LOOP, "LOOP")
          val (myStatementList, tokenList5) = statementList(tokenList4)
          val tokenList6 = popToken(tokenList5, PhoebeLexer.END, "END")
          val myWhileStatement = WhileStatement(myConditionalExpr, myStatementList)
          (myWhileStatement, tokenList6)
        case _ =>
          throw ParseError(
            s"(line ${tokenList.head.getLine}, column ${tokenList.head.getCharPositionInLine}) " +
            s"Expected token EP, IF, REPEAT, RETURN or WHILE, but got token ${tokenList.head.getText}"
          )
      }
    }

    /** Parse the conditional expression at the head of the given token list. */
    def conditionalExpr(tokenList: List[Token]): (ConditionalExprNode, List[Token]) = {
      val (myAndExpr, tokenList2) = andExpr(tokenList)
      val (myConditionalExprTail, tokenList3) = conditionalExprTail(tokenList2)
      val myConditionalExpr = ConditionalExpr(myAndExpr, myConditionalExprTail)
      (myConditionalExpr, tokenList3)
    }

    /** Parse the conditional expression tail at the head of tokenList. */
    def conditionalExprTail(tokenList: List[Token]): (ConditionalExprTailNode, List[Token]) = {
      tokenList.head.getType match {
        case PhoebeLexer.OR =>
          val tokenList2 = popToken(tokenList, PhoebeLexer.OR, "OR")
          val (myAndExpr, tokenList3) = andExpr(tokenList2)
          val (myConditionalExprTail, tokenList4) = conditionalExprTail(tokenList3)
          val myFullConditionalExprTail = FullConditionalExprTail(myAndExpr, myConditionalExprTail)
          (myFullConditionalExprTail , tokenList4)
        case _ =>
          (NullConditionalExprTail(), tokenList)
      }
    }

    /** Parse the and expression at the head of the given token list. */
    def andExpr(tokenList: List[Token]): (AndExprNode, List[Token]) = {
      val (mySimpleExpr, tokenList2) = simpleExpr(tokenList)
      val (myAndExprTail, tokenList3) = andExprTail(tokenList2)
      val myAndExpr = AndExpr(mySimpleExpr, myAndExprTail)
      (myAndExpr, tokenList3)
    }

    /** Parse the and expression tail at the head of the given token list. */
    def andExprTail(tokenList: List[Token]): (AndExprTailNode, List[Token]) = {
      tokenList.head.getType match {
        case PhoebeLexer.AND =>
          val tokenList2 = popToken(tokenList, PhoebeLexer.AND, "AND")
          val (mySimpleExpr, tokenList3) = simpleExpr(tokenList2)
          val (myAndExprTail, tokenList4) = andExprTail(tokenList3)
          val myFullAndExprTail = FullAndExprTail(mySimpleExpr, myAndExprTail)
          (myFullAndExprTail, tokenList4)
        case _ =>
          (NullAndExprTail(), tokenList)
      }
    }

    /** Parse the simple expression at the head of the given token list. */
    def simpleExpr(tokenList: List[Token]): (SimpleExprNode, List[Token]) = {
      tokenList.head.getType match {
        case PhoebeLexer.NOT =>
          val tokenList2 = popToken(tokenList, PhoebeLexer.NOT, "NOT")
          val (mySimpleExpr, tokenList3) = simpleExpr(tokenList2)
          val myNotSimpleExpr = NotSimpleExpr(mySimpleExpr)
          (myNotSimpleExpr, tokenList3)
        case PhoebeLexer.LPAREN =>
          val tokenList2 = popToken(tokenList, PhoebeLexer.LPAREN, "LPAREN")
          val (myConditionalExpr, tokenList3) = conditionalExpr(tokenList2)
          val tokenList4 = popToken(tokenList3, PhoebeLexer.RPAREN, "RPAREN")
          val myParenSimpleExpr = ParenSimpleExpr(myConditionalExpr)
          (myParenSimpleExpr, tokenList4)
        case PhoebeLexer.EP =>
          val myEPSimpleExpr = EPSimpleExpr(EPNode(tokenList.head.getText))
          val tokenList2 = popToken(tokenList, PhoebeLexer.EP, "EP")
          (myEPSimpleExpr, tokenList2)
        case _ =>
          throw ParseError(
            s"(line ${tokenList.head.getLine}, column ${tokenList.head.getCharPositionInLine}) " +
            s"Expected token NOT, LPAREN or EP, but got token ${tokenList.head.getText}"
          )
      }
    }

    // Start things off by calling the start symbol.
    val (parseTree, remainingTokens) = statementList(tokenList)
    if (remainingTokens.isEmpty) {
      throw ParseError(
        "The parser incorrectly consumed the token stream's EOF token."
      )
    } else if (remainingTokens(0).getType != Token.EOF) {
      throw ParseError(
        s"The token stream still contains tokens after parsing: $remainingTokens"
      )
    }
    parseTree
  }
}

// vim:set ts=2 sw=2 et:
