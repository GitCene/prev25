<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:template match="imcgen">
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
	    <xsl:apply-templates select="frame"/>
            <xsl:apply-templates select="access"/>
	    <tr>
	      <xsl:apply-templates select="labels"/>
	    </tr>
	    <tr>
	      <xsl:apply-templates select="imc"/>
	    </tr>
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

<xsl:template match="frame">
  <tr>
    <td>
      <table width="100%" bgcolor="EECF00">
	<tr>
	  <td>
	    <font style="font-family:arial black" size="1">
	      FRAME:
	    </font>
	  </td>
	</tr>
	<tr>
	  <td>
	    <nobr>
	      label=<font style="font-family:courier new"><xsl:value-of select="@label"/></font>
	      depth=<xsl:value-of select="@depth"/> 
	      size=<xsl:value-of select="@size"/> 
	      locs=<xsl:value-of select="@locssize"/>
	      args=<xsl:value-of select="@argssize"/>
	      FP=<xsl:value-of select="@FP"/>
	      RV=<xsl:value-of select="@RV"/>
	    </nobr>
	  </td>
	</tr>
      </table>
    </td>
  </tr>
</xsl:template>

<xsl:template match="access">
  <tr>
    <td>
      <table width="100%" bgcolor="EECF00">
	<tr>
	  <td>
	    <font style="font-family:arial black" size="1">
	      ACCESS:
	    </font>
	  </td>
	</tr>
	<tr>
	  <td>
	    <nobr>
	      size=<xsl:value-of select="@size"/> 
	      <xsl:if test="@label!=''">
		label=<font style="font-family:courier new"><xsl:value-of select="@label"/></font>
	      </xsl:if>
	      <xsl:if test="@init!=''">
		init=<font style="font-family:courier new"><xsl:value-of select="@init"/></font>
	      </xsl:if>
	      <xsl:if test="@offset!=''">
		offset=<xsl:value-of select="@offset"/>
	      </xsl:if>
	      <xsl:if test="@depth!=''">
		depth=<xsl:value-of select="@depth"/>
	      </xsl:if>
	    </nobr>
	  </td>
	</tr>
      </table>
    </td>
  </tr>
</xsl:template>

<xsl:template match="imc">
  <td>
    <table width="100%">
      <tr>
	<td bgcolor="C7C232" colspan="1000">
	  <xsl:text disable-output-escaping="yes"><![CDATA[&nbsp;]]></xsl:text>
	  <xsl:value-of select="@instruction"/>
	  <xsl:text disable-output-escaping="yes"><![CDATA[&nbsp;]]></xsl:text>
	</td>
      </tr>
      <tr>
	<xsl:apply-templates select="imc"/>
      </tr>
    </table>
  </td>
</xsl:template>

<xsl:template match="labels">
  <td>
    <table width="100%">
      <tr>
	<td bgcolor="C7C232" colspan="1000">
	  <xsl:text disable-output-escaping="yes"><![CDATA[&nbsp;]]></xsl:text>
	  <xsl:if test="@prologue!=''">
	    prologue:<font style="font-family:courier new"><xsl:value-of select="@prologue"/></font>
	  </xsl:if>
	  <xsl:if test="@body!=''">
	    body:<font style="font-family:courier new"><xsl:value-of select="@body"/></font>
	  </xsl:if>
	  <xsl:if test="@epilogue!=''">
	    epilogue:<font style="font-family:courier new"><xsl:value-of select="@epilogue"/></font>
	  </xsl:if>
	  <xsl:text disable-output-escaping="yes"><![CDATA[&nbsp;]]></xsl:text>
	</td>
      </tr>
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
