package rainbow.ant.rdm;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.h2.tools.RunScript;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.io.CharStreams;
import com.google.common.io.Closeables;
import com.google.common.io.Files;

import rainbow.core.util.XmlBinder;
import rainbow.db.model.Column;
import rainbow.db.model.Entity;
import rainbow.db.model.Model;

public class Transform extends Task {

	private final static XmlBinder<Model> binder = Model.getXmlBinder();

	private static Map<Template, Transformer> transformerMap;

	/**
	 * RDM 文件集
	 */
	private FileSet fileset;

	/**
	 * 模版文件所在目录
	 */
	private File templateDir;

	/**
	 * 转换用模版描述
	 */
	private List<Template> templates = new LinkedList<Template>();

	/**
	 * 输出目录
	 */
	private File todir;

	/**
	 * 初始化数据描述
	 */
	private List<InitData> initdatas = new LinkedList<InitData>();

	public FileSet createFileset() {
		fileset = new FileSet();
		return fileset;
	}

	public void setTodir(File todir) {
		this.todir = todir;
	}

	public void setTemplateDir(File templateDir) {
		this.templateDir = templateDir;
	}

	public void addInitdata(InitData initdata) {
		initdatas.add(initdata);
	}

	public void addTemplate(Template template) {
		templates.add(template);
	}

	@Override
	public void execute() throws BuildException {
		check();
		readTemplate();
		Map<String, Map<String, Entity>> maps = readModelFiles();
		if (maps.isEmpty())
			throw new BuildException("no data model found");
		try {
			makeInitSql(maps);
			doOutputWork(maps);
		} catch (Exception e) {
			throw new BuildException(e, getLocation());
		}
	}

	/**
	 * 检查配置正确性
	 */
	private void check() {
		if (todir == null)
			throw new BuildException("The toDir attribute must be present", getLocation());
		if (!todir.isDirectory())
			throw new BuildException("The toDir attribute must be a directory", getLocation());
		if (!templateDir.isDirectory())
			throw new BuildException("The templateDir attribute must be a directory", getLocation());
		if (fileset == null)
			throw new BuildException("please tell me where are the rdm files using fileset!", getLocation());
		for (InitData initdata : initdatas) {
			File file = new File(getProject().getBaseDir(), initdata.getDir());
			if (!file.exists() || !file.isDirectory()) {
				throw new BuildException("init data dir not exist->" + initdata.getDir(), getLocation());
			}
		}
	}

	/**
	 * 读取转换模版配置
	 */
	private void readTemplate() {
		TransformerFactory tf = TransformerFactory.newInstance();
		transformerMap = new HashMap<Template, Transformer>(templates.size());
		for (Template template : templates) {
			if (template.getName() == null || template.getName().isEmpty())
				throw new BuildException("template filename not set");
			File file = new File(templateDir, template.getName() + ".xsl");
			if (file.exists() && file.isFile()) {
				try {
					Transformer t = tf.newTransformer(new StreamSource(file));
					for (Param param : template.getParams()) {
						t.setParameter(param.getName(), param.getValue());
					}
					transformerMap.put(template, t);
				} catch (TransformerConfigurationException e) {
					throw new BuildException("read template file failed->" + file.getName(), getLocation());
				}
			} else {
				throw new BuildException(
						"Template [" + template.getName() + "] not found under " + templateDir.getAbsolutePath());
			}
		}
	}

	/**
	 * 读取RDM文件
	 * 
	 * @return
	 */
	private Map<String, Map<String, Entity>> readModelFiles() {
		Map<String, Map<String, Entity>> maps = new TreeMap<String, Map<String, Entity>>();
		DirectoryScanner ds = null;
		ds = fileset.getDirectoryScanner(getProject());

		File fromDir = fileset.getDir(getProject());
		for (String s : ds.getIncludedFiles()) {
			File file = new File(fromDir, s);
			try {
				Model dbModel = binder.unmarshal(file);
				Map<String, Entity> entityMap = maps.get(dbModel.getName());
				if (entityMap == null) {
					entityMap = new TreeMap<String, Entity>();
					maps.put(dbModel.getName(), entityMap);
				}
				for (Entity entity : dbModel.getEntities()) {
					entityMap.put(entity.getName(), entity);
				}
			} catch (Exception e) {
				throw new BuildException(s, e);
			}
		}
		return maps;
	}

	/**
	 * 生成初始化数据sql
	 * 
	 * @param maps
	 * @throws Exception
	 */
	private void makeInitSql(Map<String, Map<String, Entity>> maps) throws Exception {
		for (final String model : maps.keySet()) {
			Map<String, Entity> entityMap = maps.get(model);
			FilenameFilter filter = new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					name = name.toLowerCase();
					if (name.endsWith(".sql") || name.endsWith(".xlsx")) {
						if (name.startsWith(model + ".") || (name.startsWith(model + "_")))
							return true;
					}
					return false;
				}
			};

			for (InitData initdata : initdatas) {
				File[] files = new File(getProject().getBaseDir(), initdata.getDir()).listFiles(filter);
				if (files.length > 0) {
					File toFile = new File(todir, model + "." + initdata.getName());
					BufferedWriter writer = Files.newWriter(toFile, Charsets.UTF_8);
					for (File file : files) {
						System.out.println("init data from " + file.getName());
						if (file.getName().toLowerCase().endsWith(".sql")) {
							BufferedReader reader = Files.newReader(file, Charsets.UTF_8);
							CharStreams.copy(reader, writer);
							reader.close();
						} else {
							initDataFromExcel(file, entityMap, writer);
						}
					}
					writer.close();
				}
			}
		}
	}

	private void initDataFromExcel(File file, Map<String, Entity> entityMap, BufferedWriter writer)
			throws InvalidFormatException, IOException {
		InputStream is = new FileInputStream(file);
		try {
			Workbook wb = WorkbookFactory.create(is);
			for (int i = 0; i < wb.getNumberOfSheets(); i++) {
				Sheet sheet = wb.getSheetAt(i);
				String entityName = sheet.getSheetName();
				Entity entity = entityMap.get(entityName);
				if (entity == null) {
					System.out.println("entity not found->" + entityName);
				} else {
					System.out.println("load data of->" + entityName);
					StringBuilder sb = new StringBuilder();
					sb.append("INSERT INTO ").append(entity.getDbName()).append("(");
					Joiner.on(',').appendTo(sb, Lists.transform(entity.getColumns(), new Function<Column, String>() {
						@Override
						public String apply(Column input) {
							return input.getDbName();
						}
					}));
					sb.append(") VALUES(");
					String s = sb.toString();
					for (int rowInx = 1; rowInx <= sheet.getLastRowNum(); rowInx++) {
						writer.write(s);
						Row row = sheet.getRow(rowInx);
						int col = 0;
						for (Column column : entity.getColumns()) {
							try {
								if (col > 0)
									writer.write(',');
								Cell cell = row.getCell(col++);
								if (cell == null)
									writer.write("NULL");
								else
									writer.write(getValue(column, cell));
							} catch (Throwable e) {
								System.out.println(String.format("row[%d]col[%s]error", rowInx, column.getCnName()));
								Throwables.propagate(e);
							}
						}
						writer.write(");\n");
					}
				}
			}
		} finally {
			Closeables.closeQuietly(is);
		}
	}

	private String getValue(Column column, Cell cell) {
		switch (cell.getCellTypeEnum()) {
		case NUMERIC:
			switch (column.getType()) {
			case DATE:
				return String.format("'%tF'", cell.getDateCellValue());
			case TIME:
				return String.format("'%tT'", cell.getDateCellValue());
			case TIMESTAMP:
				return String.format("'%tF %tT'", cell.getDateCellValue(), cell.getDateCellValue());
			case INT:
			case SMALLINT:
				Double d = Double.valueOf(cell.getNumericCellValue());
				return Integer.toString(d.intValue());
			case LONG:
				d = Double.valueOf(cell.getNumericCellValue());
				return Long.toString(d.longValue());
			case DOUBLE:
			case NUMERIC:
				return Double.toString(cell.getNumericCellValue());
			case CHAR:
			case VARCHAR:
			case CLOB:
				String v = Double.toString(cell.getNumericCellValue());
				if (v.contains(".")) {
					if (v.contains(".")) {
						v = v.replaceAll("0+$", "");// 去掉多余的0
						v = v.replaceAll("[.]$", "");// 如最后一位是.则去掉
					}
				}
				return String.format("'%s'", v);
			default:
				return "NULL";
			}
		case BLANK:
			return "NULL";
		case STRING:
			return String.format("'%s'", cell.getStringCellValue());
		default:
			return "NULL";
		}
	}

	/**
	 * 转换
	 * 
	 * @param maps
	 * @throws Exception
	 */
	private void doOutputWork(Map<String, Map<String, Entity>> maps) throws Exception {
		for (String modelName : maps.keySet()) {
			Map<String, Entity> entityMap = maps.get(modelName);
			if (entityMap.isEmpty()) {
				System.out.println(String.format("model [%s] has no entity", modelName));
				continue;
			}
			List<Entity> entities = new ArrayList<Entity>(entityMap.size());
			entities.addAll(entityMap.values());
			Model model = new Model();
			model.setName(modelName);
			model.setEntities(entities);
			byte[] xml = binder.marshal(model);
			System.out.println(String.format("transform model [%s] ", modelName));
			saveFile(modelName, xml);
		}
	}

	private void saveFile(String modelName, byte[] modelXml) throws TransformerException, IOException, SQLException {
		for (Template template : templates) {
			System.out.println("processing template " + template.getName());
			Transformer t = transformerMap.get(template);
			String fileName = String.format("%s_%s.%s", modelName, template.getName(), template.getSuffix());
			File file = new File(todir, fileName);
			FileOutputStream fos = new FileOutputStream(file);
			t.transform(new StreamSource(new ByteArrayInputStream(modelXml)), new StreamResult(fos));
			fos.close();
			if ("H2".equals(template.getName()) && "sql".equals(template.getSuffix())) {
				String url = "jdbc:h2:" + todir + "/" + modelName;
				RunScript.execute(url, "sa", "", file.getAbsolutePath(), Charsets.UTF_8, false);
				System.out.println("dev db generated!->" + modelName);
				for (InitData initdata : initdatas) {
					if (initdata.isLoad()) {
						File initdataFile = new File(todir, modelName + "." + initdata.getName());
						if (initdataFile.exists()) {
							RunScript.execute(url, "sa", "", initdataFile.getAbsolutePath(), Charsets.UTF_8, false);
							System.out.println(initdataFile.getName() + " excuted!");
						}
					}
				}
			}
		}
	}

}