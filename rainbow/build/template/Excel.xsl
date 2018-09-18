<?xml version="1.0" encoding = "UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:r="http://rainbow/db/model">
	<xsl:output method="xml" indent="yes" encoding="UTF-8" />
	<xsl:template match="/">
 		<?mso-application progid="Excel.Sheet"?>
 		<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet"
    		xmlns:o="urn:schemas-microsoft-com:office:office"
    		xmlns:x="urn:schemas-microsoft-com:office:excel"
    		xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet"
    		xmlns:html="http://www.w3.org/TR/REC-html40">
		<xsl:apply-templates select="//r:entity" />
		</Workbook>
	</xsl:template>

	<xsl:template match="r:entity">
		<xsl:text disable-output-escaping="yes">&lt;Worksheet ss:Name=&quot;</xsl:text>
		<xsl:value-of select="r:name" />
		<xsl:text disable-output-escaping="yes">&quot;&gt;</xsl:text>
		<Table><Row>
		<xsl:apply-templates select="r:columns/r:column" />
		</Row></Table>
		<xsl:text disable-output-escaping="yes">&lt;/Worksheet&gt;</xsl:text>
	</xsl:template>

	<xsl:template match="r:column">
		<Cell>
			<xsl:text disable-output-escaping="yes">&lt;Data ss:Type="String"&gt;</xsl:text>
			<xsl:value-of select="r:cnName" />
			<xsl:text disable-output-escaping="yes">&lt;/Data&gt;</xsl:text>
		</Cell>
	</xsl:template>
</xsl:stylesheet>