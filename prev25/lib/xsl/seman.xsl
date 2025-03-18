<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:template match="seman">
  <html>
    <style>
      table, tr, td {
      text-align: center;
      vertical-align: top;
      }
    </style>
    <body>
      <table>
	<tr>
	  <xsl:apply-templates select="astnode"/>
	</tr>
      </table>
    </body>
  </html>
</xsl:template>

<xsl:template match="astnode">
  <td>
    <table width="100%">
      <tr bgcolor="DDDDDD">
	<td colspan="1000">
	  <table width="100%">
	    <tr>
	      <td>
		<xsl:choose>
		  <xsl:when test="@none">
		    <xsl:text disable-output-escaping="yes"><![CDATA[&nbsp;]]></xsl:text>
		    <font style="font-family:arial black" color="F50A19">
		      AST
		    </font>
		    <xsl:text disable-output-escaping="yes"><![CDATA[&nbsp;]]></xsl:text>		    
		  </xsl:when>
		  <xsl:otherwise>
		    <nobr>
		      <xsl:text disable-output-escaping="yes"><![CDATA[&nbsp;]]></xsl:text>		    
		      <font style="font-size:75%">A<xsl:value-of select="@id"/>:</font><xsl:text> </xsl:text>
		      <font style="font-family:arial black">
			<xsl:value-of select="@label"/>
		      </font>
		      <xsl:if test="@name!=''">
			<xsl:text disable-output-escaping="yes"><![CDATA[&nbsp;]]></xsl:text>
			<font style="font-family:helvetica">
			  <xsl:value-of select="@name"/>
			</font>
		      </xsl:if>
		      <xsl:text disable-output-escaping="yes"><![CDATA[&nbsp;]]></xsl:text>
		    </nobr>
		    <xsl:if test="location">
		      <br/>
		      <xsl:apply-templates select="location"/>
		    </xsl:if>
		  </xsl:otherwise>
		</xsl:choose>
	      </td>
	    </tr>
	    <xsl:apply-templates select="defat"/>
	    <xsl:apply-templates select="istype"/>
	    <xsl:apply-templates select="oftype"/>
	    <xsl:apply-templates select="isconst"/>
	    <xsl:apply-templates select="isaddr"/>
	  </table>
	</td>
      </tr>
      <tr>
	<xsl:apply-templates select="astnode"/>
      </tr>
    </table>
  </td>
</xsl:template>

<xsl:template match="defat">
  <tr>
    <td>
      <font style="font-family:helvetica; font-size:75%">
	defAt:
      </font>
    </td>
  </tr>
  <xsl:choose>
    <xsl:when test="@none">
      <tr bgcolor="FFCF00">
	<td>
	  <nobr>
	    <xsl:text disable-output-escaping="yes"><![CDATA[&nbsp;]]></xsl:text>
	    <font style="font-family:arial black" color="FF0000">
	      DEF
	    </font>
	    <xsl:text disable-output-escaping="yes"><![CDATA[&nbsp;]]></xsl:text>
	  </nobr>
	</td>
      </tr>
    </xsl:when>
    <xsl:otherwise>
      <tr bgcolor="FFCF00">
	<td>
	  <nobr>
	    <xsl:text disable-output-escaping="yes"><![CDATA[&nbsp;]]></xsl:text>
	    <font style="font-size:75%">A<xsl:value-of select="@id"/>:</font><xsl:text> </xsl:text>
	    [<xsl:value-of select="@location"/>]
	    <xsl:text disable-output-escaping="yes"><![CDATA[&nbsp;]]></xsl:text>
	  </nobr>
	</td>
      </tr>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="istype">
  <tr>
    <td>
      <font style="font-family:helvetica; font-size:75%">
	isType:
      </font>
    </td>
  </tr>
  <xsl:choose>
    <xsl:when test="@none">
      <tr bgcolor="FFAF11">
	<td>
	  <xsl:text disable-output-escaping="yes"><![CDATA[&nbsp;]]></xsl:text>
	  <font style="font-family:arial black" color="FF0000">
	    TYPE
	  </font>
	  <xsl:text disable-output-escaping="yes"><![CDATA[&nbsp;]]></xsl:text>
	</td>
      </tr>
    </xsl:when>
    <xsl:otherwise>
      <tr>
	<xsl:apply-templates select="typnode"/>
      </tr>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="oftype">
  <tr>
    <td>
      <font style="font-family:helvetica; font-size:75%">
	ofType:
      </font>
    </td>
  </tr>
  <xsl:choose>
    <xsl:when test="@none">
       <tr bgcolor="FFAF11">
	 <td>
	   <xsl:text disable-output-escaping="yes"><![CDATA[&nbsp;]]></xsl:text>
	   <font style="font-family:arial black" color="FF0000">
	     TYPE
	   </font>
	   <xsl:text disable-output-escaping="yes"><![CDATA[&nbsp;]]></xsl:text>
	 </td>
       </tr>
    </xsl:when>
    <xsl:otherwise>
      <tr>
	<xsl:apply-templates select="typnode"/>
      </tr>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="isconst">
  <xsl:choose>
    <xsl:when test="@none">
      <tr>
	<td>
	  <font style="font-size:60%" color="FF0000">CONST</font>
	</td>
      </tr>
    </xsl:when>
    <xsl:when test="@true">
      <tr>
	<td>
	  <font style="font-size:60%">CONST</font>
	</td>
      </tr>
    </xsl:when>
  </xsl:choose>
</xsl:template>

<xsl:template match="isaddr">
  <xsl:choose>
    <xsl:when test="@none">
      <tr>
	<td>
	  <font style="font-size:60%" color="FF0000">ADDR</font>
	</td>
      </tr>
    </xsl:when>
    <xsl:when test="@true">
      <tr>
	<td>
	  <font style="font-size:60%">ADDR</font>
	</td>
      </tr>
    </xsl:when>
  </xsl:choose>
</xsl:template>

<xsl:template match="typnode">
  <td>
    <table width="100%">
      <tr bgcolor="FFAF11">
	<td colspan="1000">
	  <xsl:choose>
	    <xsl:when test="@none">
	      <xsl:text disable-output-escaping="yes"><![CDATA[&nbsp;]]></xsl:text>
	      <font style="font-family:arial black" color="FF0000">
		TYPE
	      </font>
	      <xsl:text disable-output-escaping="yes"><![CDATA[&nbsp;]]></xsl:text>
	    </xsl:when>
	    <xsl:otherwise>
	      <nobr>
		<xsl:text disable-output-escaping="yes"><![CDATA[&nbsp;]]></xsl:text>
		<xsl:if test="@id!=''">
		  <font style="font-size:75%">T<xsl:value-of select="@id"/>:</font><xsl:text> </xsl:text>
		  <xsl:value-of select="@label"/>
		</xsl:if>
		<!--
		<xsl:if test="@par!=''">
		  <xsl:text disable-output-escaping="yes"><![CDATA[&nbsp;]]></xsl:text>
		  <font style="font-family:helvetica; font-size:65%">
		    <xsl:value-of select="@par"/>
		  </font>
		  <xsl:text disable-output-escaping="yes"><![CDATA[&nbsp;]]></xsl:text>
		</xsl:if>
		<xsl:if test="@name!=''">
		  <xsl:text disable-output-escaping="yes"><![CDATA[&nbsp;]]></xsl:text>
		  <font style="font-family:helvetica">
		    Z<xsl:value-of select="@name"/>
		  </font>
		  </xsl:if>
		-->
		<xsl:text disable-output-escaping="yes"><![CDATA[&nbsp;]]></xsl:text>
	      </nobr>
	    </xsl:otherwise>
	  </xsl:choose>
	</td>
      </tr>
      <xsl:if test="typnode">
	<tr>
	  <xsl:apply-templates select="typnode"/>
	</tr>
      </xsl:if>
    </table>
  </td>
</xsl:template>

<xsl:template match="location">
  <xsl:choose>
    <xsl:when test="@none">
      <xsl:text disable-output-escaping="yes"><![CDATA[&nbsp;]]></xsl:text>
      <font style="font-family:helvetica" color="F50A19">
	LOCATION
      </font>
      <xsl:text disable-output-escaping="yes"><![CDATA[&nbsp;]]></xsl:text>
    </xsl:when>
    <xsl:when test="@loc='0.0 - 0.0'">
    </xsl:when>
    <xsl:otherwise>
      <nobr>
	<xsl:text disable-output-escaping="yes"><![CDATA[&nbsp;]]></xsl:text>
	<font style="font-family:helvetica; font-size:80%">
	  <xsl:value-of select="@loc"/>
	</font>
	<xsl:text disable-output-escaping="yes"><![CDATA[&nbsp;]]></xsl:text>
      </nobr>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

</xsl:stylesheet>
