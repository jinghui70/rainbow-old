<?xml version="1.0" encoding = "UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:r="http://rainbow/db/model">
	<xsl:output method="html" indent="yes"
		cdata-section-elements="script" />
	<xsl:template match="/">
		<xsl:text disable-output-escaping="yes">&lt;!DOCTYPE html&gt;&#xA;</xsl:text>
		<html lang="zh-CN">
			<head>
				<style type="text/css">
					table {border:1px
					solid;border-collapse:collapse;font-family:Courier;font-size:12px}
					th {border:1px solid;align=center;}
					td {border:1px solid;padding:2px
					5px}
				</style>
			</head>
			<body>
			    <h1><xsl:value-of select="//r:name" /> 系统数据结构说明书</h1>
				<h2>数据表列表</h2>
				<table>
					<xsl:apply-templates select="//r:entity" mode="list" />
				</table>
				<h2>数据表详细信息</h2>
				<xsl:apply-templates select="//r:entity" mode="detail" />
			</body>
		</html>
	</xsl:template>

	<xsl:template match="r:entity" mode="list">
		<tr>
			<td>
				<xsl:value-of select="r:dbName" />
			</td>
			<td>
				<xsl:value-of select="r:cnName" />
			</td>
		</tr>
	</xsl:template>

	<xsl:template match="r:entity" mode="detail">
		<h3>
			<xsl:value-of select="r:cnName" />
			<xsl:text>（</xsl:text>
			<xsl:value-of select="r:dbName" />
			<xsl:text>）</xsl:text>
		</h3>
		<table>
			<tr>
				<th width="200px">字段名</th>
				<th width="200px">中文名</th>
				<th width="80px">数据类型</th>
				<th width="50px">长度</th>
				<th width="50px">精度</th>
				<th width="50px">主键</th>
				<th width="50px">非空</th>
			</tr>
			<xsl:apply-templates select="r:columns/r:column" />
		</table>
		<xsl:apply-templates select="r:indexes/r:index" />
	</xsl:template>

	<xsl:template match="r:column">
		<tr>
			<td>
				<xsl:value-of select="r:dbName" />
			</td>
			<td>
				<xsl:value-of select="r:cnName" />
			</td>
			<td align="center">
				<xsl:value-of select="r:type" />
			</td>
			<td align="right">
				<xsl:if test="r:length!=0">
					<xsl:value-of select="r:length" />
				</xsl:if>
			</td>
			<td align="right">
				<xsl:if test="r:precision!=0">
					<xsl:value-of select="r:precision" />
				</xsl:if>
			</td>
			<td align="center">
				<xsl:if test="r:key='true'">
					是
				</xsl:if>
			</td>
			<td align="center">
				<xsl:if test="r:mandatory='true'">
					是
				</xsl:if>
			</td>
		</tr>
		<xsl:if test="r:comment!=''">
			<tr>
				<td colspan="7">
					<strong>备注：</strong>
					<pre><xsl:value-of select="r:comment" /></pre>
				</td>
			</tr>
		</xsl:if>

	</xsl:template>

	<xsl:template match="r:index">
		<h3>
			<xsl:if test="r:unique='true'">
				唯一
			</xsl:if>
			<xsl:text>索引（</xsl:text>
			<xsl:value-of select="r:name" />
			<xsl:text>）</xsl:text>
		</h3>
		<table>
			<tr>
				<th>字段</th>
				<th>升序</th>
			</tr>
			<xsl:apply-templates select="r:inxColumns/r:inxColumn" />
		</table>
	</xsl:template>

	<xsl:template match="r:inxColumn">
		<tr>
			<td>
				<xsl:value-of select="r:name" />
			</td>
			<td>
				<xsl:if test="r:asc='true'">
					true
				</xsl:if>
			</td>
		</tr>
	</xsl:template>

</xsl:stylesheet>