/**
 * This package is copied and pasted from:
 * https://gist.github.com/joost-de-vries/28ee59d1a03f3b7cde686f58e2ecd3f1
 *
 * We also could have done something nice like add a dependency to this project,
 * but this project relies entirely on IntelliJ's dependency and build tools
 * instead of more standard ones like Maven or SBT. This hack was found to be
 * easier than grappling with IntelliJ.
 */
package org.pchapin.phoebe

/**
 * Pretty print a Scala value similar to its source represention.
 *
 * Particularly useful for case classes.
 *
 * @param a - The value to pretty print.
 * @param indentSize - Number of spaces for each indent.
 * @param maxElementWidth - Largest element size before wrapping.
 * @param depth - Initial depth to pretty print indents.
 * @return
 */
object PrettyPrint {
  def pprint(a: Any, indentSize: Int = 2, maxElementWidth: Int = 30, depth: Int = 0): String = {
    val indent      = " " * depth * indentSize
    val fieldIndent = indent + (" " * indentSize)
    val thisDepth   = pprint(_: Any, indentSize, maxElementWidth, depth)
    val nextDepth   = pprint(_: Any, indentSize, maxElementWidth, depth + 1)
    a match {
      // Make Strings look similar to their literal form.
      case s: String =>
        val replaceMap = Seq(
          "\n" -> "\\n",
          "\r" -> "\\r",
          "\t" -> "\\t",
          "\"" -> "\\\""
        )
        '"' + replaceMap.foldLeft(s) { case (acc, (c, r)) => acc.replace(c, r) } + '"'
      // For an empty Seq just use its normal String representation.
      case xs: Seq[_] if xs.isEmpty => xs.toString()
      case xs: Seq[_]               =>
        // If the Seq is not too long, pretty print on one line.
        val resultOneLine = xs.map(nextDepth).toString()
        if (resultOneLine.length <= maxElementWidth) { resultOneLine } else {
          // Otherwise, build it with newlines and proper field indents.
          val result = xs.map(x => s"\n$fieldIndent${nextDepth(x)}").toString()
          result.substring(0, result.length - 1) + "\n" + indent + ")"
        }
      // Product should cover case classes.
      case p: Product => handleProduct(p, fieldIndent, maxElementWidth, indent, thisDepth, nextDepth)

      // If we haven't specialized this type, just use its toString.
      case _ => a.toString
    }
  }

  private def handleProduct(p: Product,
                            fieldIndent: String,
                            maxElementWidth: Int,
                            indent: String,
                            thisDepth: Any => String,
                            nextDepth: Any => String): String = {
    val prefix = p.productPrefix
    // We'll use reflection to get the constructor arg names and values.
    val cls    = p.getClass
    val fields = cls.getDeclaredFields.filterNot(_.isSynthetic).map(_.getName)
    val values = p.productIterator.toSeq
    // If we weren't able to match up fields/values, fall back to toString.
    if (fields.length != values.length) { p.toString } else {
      fields.zip(values).toList match {
        // If there are no fields, just use the normal String representation.
        case Nil => p.toString
        // If there is just one field, let's just print it as a wrapper.
        case (_, value) :: Nil => s"$prefix(${thisDepth(value)})"
        // If there is more than one field, build up the field names and values.
        case kvps =>
          val prettyFields = kvps.map { case (k, v) => s"$fieldIndent$k = ${nextDepth(v)}" }
          // If the result is not too long, pretty print on one line.
          val resultOneLine = s"$prefix(${prettyFields.mkString(", ")})"
          if (resultOneLine.length <= maxElementWidth) { resultOneLine } else {
            // Otherwise, build it with newlines and proper field indents.
            s"$prefix(\n${prettyFields.mkString(",\n")}\n$indent)"

          }
      }
    }
  }
}

// vim:set ts=2 sw=2 et:
